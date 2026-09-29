package com.gpudb;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;

import com.gpudb.protocol.ShowSystemPropertiesResponse;

/**
 * Turns a cluster's system properties into the addresses a client will use.
 *
 * <p>Everything here is a <b>pure function of its arguments</b>: a properties
 * map, a hostname pattern, a list of candidates.  Nothing contacts a server,
 * and nothing reads connection state.  That is the whole point of the class
 * boundary -- address resolution has a parse phase and a select phase that can
 * be exercised exhaustively with no cluster, and a probe phase that cannot.
 * The probe phase stays on {@link GPUdbBase}, where the connection lives.
 */
final class AddressResolution {

    /** Static utilities only. */
    private AddressResolution() {
    }


    // ------------------------------------------------------------------
    // The property names this class reads
    // ------------------------------------------------------------------

    static final String SYSTEM_PROPERTIES_RESPONSE_ENABLE_HTTPD        = "conf.enable_httpd_proxy";
    static final String SYSTEM_PROPERTIES_RESPONSE_ENABLE_MH           = "conf.enable_worker_http_servers";
    static final String SYSTEM_PROPERTIES_RESPONSE_NUM_HOSTS           = "conf.number_of_hosts";
    static final String SYSTEM_PROPERTIES_RESPONSE_HEAD_NODE_URLS      = "conf.ha_ring_head_nodes_full";
    // A semicolon-separated list of rank URL entries for the cluster; each
    // entry is either an admin-configured URL or a list of host interface URLs.
    static final String SYSTEM_PROPERTIES_RESPONSE_SERVER_URLS         = "conf.worker_http_server_urls";
    // A semicolon-separated list of rank URLs for the cluster; each entry is
    // generated using the host's configured internal address.
    static final String SYSTEM_PROPERTIES_RESPONSE_SERVER_URLS_PRIVATE = "conf.worker_http_server_urls_private";
    // The host interface addresses each rank's host carries.
    static final String SYSTEM_PROPERTIES_RESPONSE_SERVER_IPS           = "conf.worker_http_server_ips";
    // List of ports intended to be matched with entries in
    // SYSTEM_PROPERTIES_RESPONSE_SERVER_IPS.
    static final String SYSTEM_PROPERTIES_RESPONSE_SERVER_PORTS         = "conf.worker_http_server_ports";
    // Scheme of a rank's own listener.
    static final String SYSTEM_PROPERTIES_RESPONSE_USE_HTTPS            = "conf.use_https";
    static final String SYSTEM_PROPERTIES_RESPONSE_TRUE                = "TRUE";


    /**
     * The host-manager path for a head node URL that is being reached through
     * httpd: the head node's own path with its <i>last segment replaced</i> by
     * {@code gpudb-host-manager}.
     *
     * @param headNodeUrl  the head node URL to derive from
     *
     * @return  the path component for the host manager URL
     */
    static String hostManagerPathFor( URL headNodeUrl ) {
        String path      = headNodeUrl.getPath();
        int    lastSlash = path.lastIndexOf( '/' );

        return (lastSlash < 0)
               ? "/gpudb-host-manager"
               : path.substring( 0, lastSlash + 1 ) + "gpudb-host-manager";
    }


    /**
     * Retrieves the HTTPD proxy enabled configuration from the given system
     * properties of a cluster.
     * 
     * @param systemProperties  A map containing all cluster system properties.
     * @return  whether or not HTTPD proxy is enabled for the cluster.
     */
    static boolean getIsHttpdEnabled(Map<String, String> systemProperties) {
        
        boolean isHttpdEnabled = false;

        // Is HTTPD being used (helps in figuring out the host manager URL
        String enableHttpd = systemProperties.get( SYSTEM_PROPERTIES_RESPONSE_ENABLE_HTTPD );

        // Figure out if we're using HTTPD
        if ( (enableHttpd != null)
            && (enableHttpd.compareToIgnoreCase( SYSTEM_PROPERTIES_RESPONSE_TRUE ) == 0 ) ) {
            isHttpdEnabled = true;
        }

        return isHttpdEnabled;
    }


    /**
     * Retrieves the multi-head I/O enabled configuration from the given system
     * properties of a cluster.
     * 
     * @param systemProperties  A map containing all cluster system properties.
     * @return  whether or not multi-head I/O is enabled for the cluster.
     */
    static boolean getIsMultiHeadEnabled(Map<String, String> systemProperties) {
        
        boolean isMultiHeadEnabled = false;

        String enableWorkerHttp = systemProperties.get( SYSTEM_PROPERTIES_RESPONSE_ENABLE_MH );

        if ( (enableWorkerHttp != null)
            && (enableWorkerHttp.compareToIgnoreCase( SYSTEM_PROPERTIES_RESPONSE_TRUE ) == 0 ) ) {
            isMultiHeadEnabled = true;
        }

        return isMultiHeadEnabled;
    }


    /**
     * Whether a parsed address can actually be connected to.
     *
     * <p>{@link URL} accepts strings that parse cleanly but name no host:
     * {@code http://:9191} yields an empty host and would otherwise be stored
     * as a live worker.  Such an entry fails only at first use, and presents as
     * the rank being down rather than as an address problem -- so the operator
     * investigates a healthy rank and never suspects the parse.
     *
     * @param url  the parsed address, or {@code null}
     *
     * @return  whether the address names a host that can be connected to
     */
    static boolean hasUsableHost( URL url ) {
        return (url != null)
                && (url.getHost() != null)
                && !url.getHost().isEmpty();
    }


    /**
     * Whether a host, as bare text, names something that can be connected to.
     *
     * <p>The string-level counterpart of {@link #hasUsableHost(URL)}, for the
     * one parser whose input may be a bare host name with no scheme and so
     * cannot be handed to {@link URL} at all.  It rejects the same two shapes:
     * an empty entry, and one that carries a port but no host.
     *
     * @param host  the host text, already stripped of any scheme
     *
     * @return  whether the text names a host
     */
    static boolean hasUsableHostText( String host ) {
        return (host != null)
                && !host.isEmpty()
                && !host.startsWith( ":" );
    }


    /**
     * Extracts the host component of an advertised address, whatever form it
     * arrives in.
     *
     * <p>Exists so that every resolver matches the hostname regex against the
     * same thing.  The properties do not agree on form: some carry whole URLs,
     * one carries bare addresses, and an operator-set value may carry a port or
     * a path.  Reducing all of them to a host through {@link URL} gives one
     * answer, including the brackets {@code getHost()} reports for an IPv6
     * literal -- which is where a hand-rolled string split diverges.
     *
     * <p>A scheme is synthesized when the text has none, since {@link URL}
     * requires one and a bare host name is a legitimate value here.
     *
     * @param address  the advertised address text
     *
     * @return  the host component, or {@code null} if the text cannot be parsed
     *          as an address at all
     */
    static String hostOf( String address ) {
        if ( address == null )
            return null;

        String withScheme = address.contains( "://" ) ? address : ("http://" + address);
        try {
            return new URL( withScheme ).getHost();
        } catch ( MalformedURLException ex ) {
            // Not an address; the caller treats a null host as unusable
            return null;
        }
    }


    /**
     * Names a rank for a user-facing message.
     *
     * @param rankIndex  index into the server's rank list, where 0 is the head
     *
     * @return  a phrase naming that rank
     */
    static String rankLabel( int rankIndex ) {
        return (rankIndex == 0) ? "the head rank" : ("worker rank " + rankIndex);
    }


    /**
     * Applies the hostname regex to one candidate address.
     *
     * <p>The match is a <b>prefix</b> match: the pattern is anchored at the
     * start of the candidate and unanchored at the end, so {@code 172\.17\.}
     * selects every address in that range without a trailing {@code .*}.  This
     * is {@link java.util.regex.Matcher#lookingAt lookingAt} rather than
     * {@link java.util.regex.Matcher#matches matches}.
     *
     * <p>Note the consequence: a pattern is <i>not</i> implicitly anchored at
     * the end, so {@code 10\.0\.0\.1} also selects {@code 10.0.0.10} and
     * {@code 10.0.0.123}.  A pattern meant to name exactly one address must say
     * so, with a trailing {@code $}.
     *
     * <p>Every code path that resolves addresses matches through here, so the
     * semantics cannot differ between them.
     *
     * @param regex      the user-given pattern; never {@code null} here --
     *                   callers test for a null pattern first, since no pattern
     *                   means "take the first address" rather than "match all"
     * @param candidate  the address text to test
     *
     * @return  whether the candidate is selected by the pattern
     */
    static boolean hostnameRegexMatches( Pattern regex, String candidate ) {
        return regex.matcher( candidate ).lookingAt();
    }


    /**
     * The caller-given URLs whose host the hostname regex accepts.
     *
     * <p>Asked when the regex matches nothing the server advertises, to decide
     * whether the connection can continue head-node-only rather than fail.  The
     * regex states which address space this caller can reach; a URL of theirs
     * inside that space, which has already answered, is somewhere routing
     * everything contradicts nothing they asked for.  A URL outside it is not:
     * routing through an address the caller's own filter excludes would ignore
     * the option while appearing to honor it.
     *
     * <p>Matched on the host component alone, as everywhere else, and with no
     * pattern set every URL is accepted -- there is then no filter to
     * contradict.
     *
     * @param urls           the URLs the caller supplied
     * @param hostnameRegex  the pattern, or null where none is set
     *
     * @return those the pattern accepts, in the order given; empty if none
     */
    static List<URL> urlsAcceptedByHostnameRegex( List<URL> urls, Pattern hostnameRegex ) {

        if ( (hostnameRegex == null) || hostnameRegex.pattern().isEmpty() )
            return urls;

        final List<URL> accepted = new ArrayList<>();

        for ( URL url : urls ) {
            if ( hasUsableHost( url ) && hostnameRegexMatches( hostnameRegex, url.getHost() ) )
                accepted.add( url );
        }

        return accepted;
    }  // end urlsAcceptedByHostnameRegex


    /**
     * Given system properties and a hostname regex, extract the head and worker
     * rank root URLs.  If distributed I/O is disabled on the server, no URLs
     * are found in the server's list, or none of the URLs match the given
     * regex, an empty list will be returned.
     *
     * @param systemProperties  A map containing all relevant system properties.
     * @param hostnameRegex     The regex to match the URLs against; if null,
     *                          then use the first element of the list, if the
     *                          system properties has multiple URLs for a given
     *                          rank.
     *
     * @return a list of URLs, where the first entry is the rank-0 URL.  If no
     *         worker URLs are found or match the regex, the list will be empty.
     */
    static List<URL> extractRankUrls( Map<String, String> systemProperties, Pattern hostnameRegex )
        throws GPUdbBase.GPUdbHostnameRegexFailureException, GPUdbException {

        return selectRankUrls( extractRankCandidateUrls( systemProperties ), hostnameRegex );
    }  // end extractRankUrls


    /**
     * Split one comma-separated alternate list into the addresses that are
     * usable, in advertised order.
     *
     * <p>An unusable alternate is skipped rather than fatal.
     *
     * @param alternateList  one rank's or cluster's comma-separated addresses
     * @param what           what these address are, for logging: "rank URL"
     *
     * @return the usable addresses, in advertised order; empty if none is
     */
    static List<URL> parseUsableAlternateUrls( String alternateList, String what ) {

        final List<URL> usable = new ArrayList<>();

        for ( String urlString : alternateList.split( "," ) ) {
            URL url;

            try {
                url = new URL( urlString );
            } catch ( MalformedURLException ex ) {
                GPUdbLogger.debug_with_info( "Skipping unusable " + what + " <" + urlString + ">: " + ex.getMessage() );
                continue;
            }

            if ( !hasUsableHost( url ) ) {
                GPUdbLogger.debug_with_info( "Skipping " + what + " naming no host <" + urlString + ">" );
                continue;
            }

            usable.add( url );
        }

        return usable;
    }  // end parseUsableAlternateUrls


    /**
     * Retain the candidates whose host the regex selects, in the order given.
     *
     * <p>A <b>filter</b>, not a selector: every match is kept.  A null or empty
     * pattern selects everything.
     *
     * <p>Matching is against the host component alone, never the scheme, port
     * or path, and every resolver matches that same target.  Note the match is
     * anchored only at the start, so a pattern is not implicitly exact.
     *
     * @param candidates     the usable addresses to filter
     * @param hostnameRegex  the pattern, or null to retain everything
     * @param what           what these addresses are, for logging
     *
     * @return the retained addresses, in the order given
     */
    static List<URL> filterUrlsByHostnameRegex( List<URL> candidates, Pattern hostnameRegex, String what ) {

        if ( (hostnameRegex == null) || hostnameRegex.pattern().isEmpty() )
            return candidates;

        final List<URL> retained = new ArrayList<>();

        for ( URL url : candidates ) {
            if ( hostnameRegexMatches( hostnameRegex, url.getHost() ) ) {
                GPUdbLogger.debug_with_info( "Keeping matching " + what + ": " + url );
                retained.add( url );
            } else {
                GPUdbLogger.debug_with_info( "Skipping non-matching " + what + ": " + url );
            }
        }

        return retained;
    }  // end filterUrlsByHostnameRegex


    /**
     * Split a {@code ';'}-separated per-rank property into its slot fields.
     *
     * <p>Every per-rank address property is emitted over the <b>padded</b> rank
     * slot count, so a removed rank is an empty field with its separators
     * intact and the field index is the rank number.  Note
     * {@code String.split(";")} discards trailing empty strings, so a removed
     * <i>last</i> rank compacts away -- harmless, because dropping a trailing
     * entry shifts no surviving index.
     *
     * @param systemProperties  the properties map
     * @param key               the property to split
     *
     * @return the slot fields, empty if the property is absent or empty
     */
    static String[] extractRankSlotFields( Map<String, String> systemProperties, String key ) {
        String value = systemProperties.get( key );
        return ( (value == null) || value.isEmpty() ) ? new String[0] : value.split( ";" );
    }


    /**
     * The field for a rank slot, or {@code ""} where the property does not
     * reach that far.  A property may be shorter than another when its own
     * trailing slots were empty, so an index is never assumed to be present.
     */
    static String getRankSlotField( String[] fields, int slot ) {
        return (slot < fields.length) ? fields[ slot ] : "";
    }


    /**
     * Build the direct-form candidates for one rank from its host's enumerated
     * interface addresses.
     *
     * <p>These arrive as bare addresses -- no scheme, no port -- so the client
     * supplies both.  The scheme is the rank's own
     * ({@code conf.use_https}), <b>not</b> the one dressing the advertised
     * URLs: where an httpd proxy fronts the cluster those carry the proxy's
     * scheme, port and {@code /gpudb-N} path, and an address built from this
     * list reaches the rank's own listener instead.
     *
     * <p>An IPv6 literal is skipped rather than emitted unbracketed, which
     * would parse as a different host and port.  That costs a candidate and
     * never produces a wrong one; bracketing it properly is deferred.
     *
     * @param ipsField   one rank's comma-separated bare addresses
     * @param portField  that rank's port
     * @param useHttps   whether the rank's own listener speaks https
     *
     * @return the built candidates, in the order the addresses were given
     */
    static List<URL> buildEnumeratedCandidateUrls( String ipsField, String portField, boolean useHttps ) {

        final List<URL> built = new ArrayList<>();

        if ( ipsField.isEmpty() || portField.isEmpty() )
            return built;

        final String scheme = useHttps ? "https" : "http";

        for ( String address : ipsField.split( "," ) ) {
            if ( address.isEmpty() )
                continue;

            // Deferred: an IPv6 literal needs bracketing to be a valid
            // authority.  Whether one can reach this list at all is unconfirmed.
            if ( address.indexOf( ':' ) >= 0 ) {
                GPUdbLogger.debug_with_info( "Skipping enumerated address needing IPv6 bracketing <" + address + ">" );
                continue;
            }

            String urlString = scheme + "://" + address + ":" + portField;

            try {
                URL url = new URL( urlString );
                if ( hasUsableHost( url ) )
                    built.add( url );
            } catch ( MalformedURLException ex ) {
                GPUdbLogger.debug_with_info( "Skipping unbuildable enumerated address <" + urlString + ">: " + ex.getMessage() );
            }
        }

        return built;
    }  // end buildEnumeratedCandidateUrls


    /**
     * Concatenate one rank's candidates in intent order and drop duplicates.
     *
     * <p>Configured addresses precede discovered ones, and intent is a property
     * of the <b>entry</b> rather than of the property it arrived in: the
     * advertised list holds operator-supplied addresses in some deployments and
     * the host's interface enumeration in others, so the intent is discovered
     * by rule.  If the public addresses are configured, they are used first,
     * then the private addresses and then the enumerated interfaces last; if
     * the public addresses are not configured, they are the enumerated
     * interfaces, so the private addresses go first and then the enumerated
     * ones.
     *
     * @return the rank's candidates, deduplicated, highest intent first
     */
    static List<URL> orderRankCandidateUrls( List<URL> advertised, List<URL> privateUrls, List<URL> enumerated ) {

        final Set<String> enumeratedHosts = new HashSet<>();
        for ( URL url : enumerated )
            enumeratedHosts.add( url.getHost() );

        final List<URL> configuredAdvertised = new ArrayList<>();
        final List<URL> discoveredAdvertised = new ArrayList<>();

        for ( URL url : advertised ) {
            if ( enumeratedHosts.contains( url.getHost() ) )
                discoveredAdvertised.add( url );
            else
                configuredAdvertised.add( url );
        }

        final List<URL> ordered = new ArrayList<>();
        final Set<String> seen  = new LinkedHashSet<>();

        for ( List<URL> tier : Arrays.asList( configuredAdvertised, privateUrls,
                                              discoveredAdvertised, enumerated ) ) {
            for ( URL url : tier ) {
                if ( seen.add( url.toString() ) )
                    ordered.add( url );
            }
        }

        return ordered;
    }  // end orderRankCandidateUrls


    /**
     * The host names/IPs that appear under more than one host, and so identify
     * none of them.
     *
     * <p>A working address may be reused for a host's other ranks only because
     * every rank binds on every one of its host's interfaces, which makes a
     * reachable address a property of the host.  That reasoning needs the
     * address to <b>name</b> one host, and an enumerated interface list does
     * not guarantee it: a container bridge gateway is the same string on every
     * machine in a fleet.
     *
     * @param candidateUrls  per-rank candidate URLs, null for a removed rank
     * @param rankHost       rank slot to host index, -1 where not grouped
     *
     * @return the host names/IPs that must not be adopted as preferred
     */
    static Set<String> getNonIdentifyingHosts( List<List<URL>> candidateUrls, int[] rankHost ) {

        final Map<String, Integer> firstHostByAddress = new HashMap<>();
        final Set<String> nonIdentifying = new HashSet<>();

        for ( int rank = 0; rank < candidateUrls.size(); ++rank ) {

            final List<URL> candidates = candidateUrls.get( rank );
            if ( candidates == null )
                continue;   // slot kept for a removed rank

            final int host = (rank < rankHost.length) ? rankHost[ rank ] : -1;
            if ( host < 0 )
                continue;   // ungrouped: it says nothing about any host

            for ( URL candidate : candidates ) {
                final String address = candidate.getHost();
                final Integer seen = firstHostByAddress.putIfAbsent( address, host );

                if ( (seen != null) && (seen.intValue() != host) )
                    nonIdentifying.add( address );
            }
        }

        if ( !nonIdentifying.isEmpty() )
            GPUdbLogger.debug_with_info(
                    "Addresses advertised under more than one host, and so usable only for"
                    + " the rank that named them: " + nonIdentifying );

        return nonIdentifying;
    }  // end getNonIdentifyingHosts


    /**
     * Order a rank's candidate URLs with those naming a single host first, the
     * ambiguous ones following in their established order.
     *
     * <p>Applied where the address chosen becomes the one a cluster is
     * <b>addressed by</b> from then on, which is a stronger commitment than
     * using an address for one rank: the head node's address carries every
     * subsequent request.  Adopting one that is the same string on every machine
     * in a fleet -- a container bridge gateway -- would point the whole
     * connection somewhere that may well be the client's own machine.
     *
     * <p>Ambiguous candidates are deprioritized, never dropped.
     *
     * <p>Shares a shape with {@link #orderUrlsByPreferredHostFirst} and
     * deliberately not an implementation: they partition on different
     * predicates for different reasons, and a reordering carries no judgment
     * that two copies could answer differently.  What must stay single is a
     * *decision* -- see {@link #getRunningRankUrl} -- not every loop that
     * resembles another.
     */
    static List<URL> orderUrlsByUniqueHostsFirst( List<URL> candidates, Set<String> nonIdentifying ) {

        if ( nonIdentifying.isEmpty() )
            return candidates;

        final List<URL> identifying = new ArrayList<>();
        final List<URL> ambiguous   = new ArrayList<>();

        for ( URL candidate : candidates ) {
            if ( nonIdentifying.contains( candidate.getHost() ) )
                ambiguous.add( candidate );
            else
                identifying.add( candidate );
        }

        if ( !ambiguous.isEmpty() )
            GPUdbLogger.debug_with_info( "Head rank addresses naming more than one host, tried last: " + ambiguous );

        identifying.addAll( ambiguous );
        return identifying;
    }


    /**
     * Order a rank's candidate URLs with those on the host's preferred address
     * first, every other candidate following in its established order.
     *
     * <p>Matched on the host alone: the preferred address was proven by a
     * different rank, so it carries that rank's port and path and cannot be
     * compared whole.
     */
    static List<URL> orderUrlsByPreferredHostFirst( List<URL> candidates, String preferredHost ) {

        if ( preferredHost == null )
            return candidates;

        final List<URL> preferred = new ArrayList<>();
        final List<URL> rest      = new ArrayList<>();

        for ( URL candidate : candidates ) {
            if ( preferredHost.equals( candidate.getHost() ) )
                preferred.add( candidate );
            else
                rest.add( candidate );
        }

        preferred.addAll( rest );
        return preferred;
    }


    /**
     * Map each rank slot to the host it runs on.
     *
     * <p>The join is {@code conf.rank<r>_ip_address == conf.host<h>_address},
     * on the raw configured address on both sides -- no parsing, no protocol
     * stripping, no normalization, and no branching on whether a public address
     * or proxy is configured.  Both sides are the same configured value, which
     * is what makes a plain equality correct here.
     *
     * <p>Grouping ranks by host is what lets an address proven to reach one rank
     * be tried first for that host's other ranks: the addresses belong to the
     * host, and ranks sharing one share its list because it <i>is</i> that
     * host's list.
     *
     * @param systemProperties  the properties map
     * @param rankSlots         how many rank slots to map
     *
     * @return {@code host[i]} is rank {@code i}'s host index, or {@code -1}
     *         where the rank is removed or its host cannot be identified
     */
    static int[] extractRankHostMapping( Map<String, String> systemProperties, int rankSlots ) {

        final int[] rankHost = new int[ rankSlots ];
        Arrays.fill( rankHost, -1 );

        int hostCount;
        try {
            hostCount = Integer.parseInt( systemProperties.get( SYSTEM_PROPERTIES_RESPONSE_NUM_HOSTS ) );
        } catch ( NumberFormatException | NullPointerException ex ) {
            GPUdbLogger.debug_with_info( "No usable host count; ranks will not be grouped by host" );
            return rankHost;
        }

        // host address -> host index.  A host with no address contributes
        // nothing rather than matching every rank that also has none.
        final Map<String, Integer> hostIndexByAddress = new HashMap<>();
        for ( int h = 0; h < hostCount; ++h ) {
            String address = systemProperties.get( String.format( "conf.host%d_address", h ) );
            if ( (address != null) && !address.isEmpty() )
                hostIndexByAddress.put( address, h );
        }

        for ( int r = 0; r < rankSlots; ++r ) {
            String address = systemProperties.get( String.format( "conf.rank%d_ip_address", r ) );

            // A removed rank is emitted as an empty value, on the same padded
            // slot count as the address properties.
            if ( (address == null) || address.isEmpty() )
                continue;

            Integer host = hostIndexByAddress.get( address );
            if ( host == null ) {
                GPUdbLogger.debug_with_info( "No host matches the address of " + rankLabel( r ) + " <" + address + ">; it will not be grouped" );
                continue;
            }

            rankHost[ r ] = host;
        }

        return rankHost;
    }  // end extractRankHostMapping


    /**
     * Phase 1 of address resolution: turn the server's rank address encoding
     * into per-rank candidate lists.
     *
     * <p>The returned list is indexed by <b>rank slot</b>, so entry 0 is the
     * head rank.  Three states are distinguished and the difference matters to
     * the caller:
     *
     * <ul>
     *   <li>{@code null} -- the rank was removed from the cluster.  The slot is
     *       kept so that the list's indices stay equal to rank numbers; that is
     *       the numbering the shard routing table refers to, so compacting here
     *       would silently misroute every rank above the hole.</li>
     *   <li>an <b>empty</b> list -- the rank advertised addresses but none was
     *       usable.</li>
     *   <li>a non-empty list -- the usable addresses, in advertised order.</li>
     * </ul>
     *
     * @param systemProperties  a map containing all relevant system properties
     *
     * @return per-rank candidate addresses, empty where the server advertises
     *         no rank addresses at all
     */
    static List<List<URL>> extractRankCandidateUrls( Map<String, String> systemProperties ) {

        final List<List<URL>> candidates = new ArrayList<>();

        if ( !getIsMultiHeadEnabled( systemProperties ) ) {
            GPUdbLogger.debug_with_info( "Distributed I/O not enabled on server; skipping rank URL retrieval." );
            return candidates;
        }

        String propertyVal = systemProperties.get( SYSTEM_PROPERTIES_RESPONSE_SERVER_URLS );
        if ( (propertyVal == null) || propertyVal.isEmpty() ) {
            GPUdbLogger.debug_with_info( String.format( "No entry for <%s> in %s response", SYSTEM_PROPERTIES_RESPONSE_SERVER_URLS, GPUdbBase.ENDPOINT_SHOW_SYSTEM_PROPERTIES ) );
            return candidates;
        }

        GPUdbLogger.debug_with_info( String.format( "Known rank URLs <%s> from server: %s", SYSTEM_PROPERTIES_RESPONSE_SERVER_URLS, propertyVal ) );

        // The other three address properties.  All are emitted together under
        // the same server setting, so any of them being absent is an older
        // server rather than a different configuration; an absent one simply
        // contributes no candidates.
        final String[] advertisedFields = propertyVal.split( ";" );
        final String[] privateFields    = extractRankSlotFields( systemProperties, SYSTEM_PROPERTIES_RESPONSE_SERVER_URLS_PRIVATE );
        final String[] ipsFields        = extractRankSlotFields( systemProperties, SYSTEM_PROPERTIES_RESPONSE_SERVER_IPS );
        final String[] portFields       = extractRankSlotFields( systemProperties, SYSTEM_PROPERTIES_RESPONSE_SERVER_PORTS );

        final boolean useHttps = SYSTEM_PROPERTIES_RESPONSE_TRUE.equalsIgnoreCase( systemProperties.get( SYSTEM_PROPERTIES_RESPONSE_USE_HTTPS ) );

        for ( int i = 0; i < advertisedFields.length; ++i ) {

            // Handle removed ranks.  Keep an empty slot for the rank so that
            // this list's indices stay aligned with the rank numbering.  The
            // advertised list is the authority for which slots are vacant; the
            // server empties every per-rank property for a removed rank, so the
            // others agree by construction.
            if ( advertisedFields[i].isEmpty() ) {
                candidates.add( null );
                continue;
            }

            List<URL> advertised = parseUsableAlternateUrls( advertisedFields[i], "rank URL" );

            List<URL> privateUrls = parseUsableAlternateUrls(getRankSlotField( privateFields, i ), "private rank URL" );

            List<URL> enumerated = buildEnumeratedCandidateUrls(
                    getRankSlotField( ipsFields, i ),
                    getRankSlotField( portFields, i ),
                    useHttps
            );

            candidates.add( orderRankCandidateUrls( advertised, privateUrls, enumerated ) );
        }

        return candidates;
    }  // end extractRankCandidateUrls


    /**
     * Phase 2 of address resolution: reduce each rank's candidates to the one
     * address the client will use.
     *
     * @param candidates     per-rank candidates from {@link #extractRankCandidateUrls}
     * @param hostnameRegex  the regex to match candidates against; if null, the
     *                       first candidate of each rank is taken
     *
     * @return one URL per rank slot, with a null placeholder for a removed rank
     *
     * @throws GPUdbBase.GPUdbHostnameRegexFailureException  if a rank had usable addresses
     *                                             and the regex rejected all of them
     * @throws GPUdbException                      if a rank advertised nothing usable
     */
    static List<URL> selectRankUrls( List<List<URL>> candidates, Pattern hostnameRegex )
        throws GPUdbBase.GPUdbHostnameRegexFailureException, GPUdbException {

        final List<URL> rankURLs = new ArrayList<>();

        if ( (hostnameRegex != null) && !hostnameRegex.pattern().isEmpty() )
            GPUdbLogger.debug_with_info( "Selecting rank URLs against user-given regex: " + hostnameRegex.pattern() );

        for ( List<URL> retained : retainMatchingRankCandidateUrls( candidates, hostnameRegex ) ) {

            // The slot kept for a rank removed from the cluster
            if ( retained == null ) {
                rankURLs.add( null );
                continue;
            }

            // Before any probe has run, the first survivor is the one used:
            // routing needs a concrete address from the moment a connection is
            // established, which precedes the first capability probe.  The rest
            // are retained so that probing can re-select.
            GPUdbLogger.debug_with_info( "Keeping rank URL: " + retained.get( 0 ) );
            rankURLs.add( retained.get( 0 ) );
        }

        return rankURLs;
    }  // end selectRankUrls


    /**
     * Apply the hostname regex to every rank's candidates, if specified,
     * keeping all matches.  Then, ensure that every rank has at least one
     * candidate URL.
     *
     * <p>A rank with no usable address at all and a rank whose usable addresses
     * the regex rejected are different outcomes: the first leaves the client
     * able to degrade, the second can only degrade if the user-given URL also
     * matches the hostname regex.
     *
     * @param candidates     per-rank candidates, with null for a removed rank
     * @param hostnameRegex  the pattern, or null to retain everything
     *
     * @return per-rank retained candidates, with null for a removed rank; every
     *         non-null entry is non-empty
     *
     * @throws GPUdbBase.GPUdbHostnameRegexFailureException  if a rank had usable addresses
     *                                             and the regex rejected all of them
     * @throws GPUdbException                      if a rank advertised nothing usable
     */
    static List<List<URL>> retainMatchingRankCandidateUrls( List<List<URL>> candidates, Pattern hostnameRegex )
        throws GPUdbBase.GPUdbHostnameRegexFailureException, GPUdbException {

        final List<List<URL>> retainedByRank = new ArrayList<>();

        if ( (hostnameRegex != null) && !hostnameRegex.pattern().isEmpty() )
            GPUdbLogger.debug_with_info( "Selecting rank URLs against user-given regex: " + hostnameRegex.pattern() );

        for ( int i = 0; i < candidates.size(); ++i ) {
            final List<URL> rankCandidates = candidates.get( i );

            if ( rankCandidates == null ) {
                retainedByRank.add( null );
                continue;
            }

            // Nothing usable was advertised for this rank, whether or not a
            // regex was given.  Reported as an ordinary failure so that the
            // connection degrades rather than being refused for a cause the
            // regex did not create.
            if ( rankCandidates.isEmpty() )
                throw new GPUdbException( "No valid IP/hostname found for " + rankLabel( i ) );

            final List<URL> retained = filterUrlsByHostnameRegex( rankCandidates, hostnameRegex, "rank URL" );

            // Every candidate reaching the filter was usable, so an empty result
            // means the regex is genuinely why nothing was found.
            if ( retained.isEmpty() )
                throw new GPUdbBase.GPUdbHostnameRegexFailureException( "No valid matching IP/hostname found for " + rankLabel( i ) );

            retainedByRank.add( retained );
        }

        return retainedByRank;
    }  // end retainMatchingRankCandidateUrls


    /**
     * Given system properties, extract the hostnames or IP addresses of all the
     * physical nodes (machines) used in the cluster, whether or not there are
     * active ranks running on any of them.  Each string will contain the protocol
     * and the hostname or the IP address, e.g. "http://abcd.com",
     * "https://123.4.5.6". This method will strip the protocol part off the 
     * hostnames/IP addresses received from the server.
     *
     * @param systemProperties  A map containing all relevant system properties.
     * @param hostnameRegex     The regex to match the URLs again; if null, then
     *                          use the first element of the list, if the system
     *                          properties has multiple URLs for a given rank.
     *
     * @return a list of hostnames or IP addresses without the protocol part.
     *         These are not full URLs.
     */
    static Set<String> getHostNamesFromSystemProperties( Map<String, String> systemProperties, Pattern hostnameRegex )
        throws GPUdbBase.GPUdbHostnameRegexFailureException, GPUdbException {

        GPUdbLogger.debug_with_info(String.format(
                "Extracting server-known host names from system properties%s",
                hostnameRegex == null ? "" : " using user-given regex: " + hostnameRegex));
        
        // Get the total number of hosts/machines in the cluster
        String numHostsStr = systemProperties.get( SYSTEM_PROPERTIES_RESPONSE_NUM_HOSTS );
        if (numHostsStr == null) {
            throw new GPUdbException( "Missing value for " + SYSTEM_PROPERTIES_RESPONSE_NUM_HOSTS );
        }
        int numHosts;
        try {
            numHosts = Integer.parseInt( numHostsStr, 10 );
        } catch ( NumberFormatException ex ) {
            throw new GPUdbException( String.format(
                    "Unparsable entry for '%s' (%s); need an integer",
                    SYSTEM_PROPERTIES_RESPONSE_NUM_HOSTS, numHostsStr));
        }


        // Extract the hostnames from the system properties
        Set<String> clusterHostnames = new HashSet<>();
        for (int i = 0; i < numHosts; ++i) {
            // Each hostname is listed individually in the system properties
            // as 'conf.host<i>_public_urls'
            String hostnameKey = String.format("conf.host%s_public_urls", i);

            String hostnameStr = systemProperties.get( hostnameKey );
            if (hostnameStr == null) {
                throw new GPUdbException( String.format("Missing value for %sth hostname '%s'",
                        i, hostnameKey));
            }

            // Each host can have multiple hostnames associated with it.
            // Collect the usable ones first, so that "advertised nothing usable"
            // and "the regex rejected what was advertised" stay distinguishable.
            final List<String> usableHosts = new ArrayList<>();

            for ( String hostname : hostnameStr.split(",") ) {

                // The hostname might have the protocol; strip that out
                String[] splitHostname = hostname.split( "://" );
                String host = (splitHostname.length > 1) ? splitHostname[ 1 ]
                                                         : splitHostname[ 0 ];

                // Match against the HOST only, per the rule that every resolver
                // matches the same thing.  `host` here still carries any port and
                // path, because that is what this parser stores and its public
                // getter has always returned; only the match target changes.
                if ( !hasUsableHostText( host ) || !hasUsableHostText( hostOf( host ) ) ) {
                    GPUdbLogger.debug_with_info("Skipping host entry naming no host <"
                                                + hostname + ">");
                    continue;
                }

                usableHosts.add( host );
            }

            // Nothing usable was advertised for this host, whether or not a
            // regex was given.
            if ( usableHosts.isEmpty() )
                throw new GPUdbException("No matching hostname found for host #" + i + ".");

            String selected = null;

            for ( String host : usableHosts ) {
                if (hostnameRegex == null) {
                    // No regex given, so take the first one
                    GPUdbLogger.debug_with_info("Keeping hostname: " + host);
                    selected = host;
                    break;
                }

                if ( hostnameRegexMatches( hostnameRegex, hostOf( host ) ) ) {
                    GPUdbLogger.debug_with_info("Keeping matching hostname: " + host);
                    selected = host;
                    break;
                }

                GPUdbLogger.debug_with_info("Skipping non-matching hostname: " + host);
            }

            // Every hostname reaching the loop was usable, so nothing selected
            // means the regex is genuinely why.
            if ( selected == null )
                throw new GPUdbBase.GPUdbHostnameRegexFailureException(String.format(
                        "No matching hostname found for host #%s (given hostname regex %s)",
                        i, hostnameRegex));

            clusterHostnames.add( selected );
        }

        return clusterHostnames;
    }  // getHostNamesFromSystemProperties


    /**
     * Given system properties, extract the head node URLs for the
     * high-availability cluster.
     *
     * @return a list of full URLs for each of the head node in the
     * high availability cluster, if any is set up.
     */
    static List<URL> getHARingHeadNodeURLs( Map<String, String> systemProperties,
                                                   Pattern hostnameRegex )
        throws GPUdbBase.GPUdbHostnameRegexFailureException, GPUdbException {

        List<URL> haRingHeadNodeURLs = new ArrayList<>();

        // First, find out if the database has a high-availability ring set up
        String is_ha_enabled_str = systemProperties.get( ShowSystemPropertiesResponse.PropertyMap.CONF_ENABLE_HA );

        // Only attempt to parse the HA ring node addresses if HA is enabled
        if ( (is_ha_enabled_str != null) && (is_ha_enabled_str.compareToIgnoreCase( ShowSystemPropertiesResponse.PropertyMap.TRUE ) == 0 ) ) {

            // Parse the HA ring head node addresses, if any
            String ha_ring_head_nodes_str = systemProperties.get( SYSTEM_PROPERTIES_RESPONSE_HEAD_NODE_URLS );
            if ( (ha_ring_head_nodes_str != null) && !ha_ring_head_nodes_str.isEmpty() ) {

                String[] haRingHeadNodeUrlLists = ha_ring_head_nodes_str.split(";");

                // Parse each entry (corresponds to a cluster)
                for (int i = 0; i < haRingHeadNodeUrlLists.length; ++i) {

                    // Each cluster's head node can have multiple URLs
                    // associated with it
                    final List<URL> clusterCandidates = parseUsableAlternateUrls( haRingHeadNodeUrlLists[i], "head node URL" );

                    // Nothing usable was advertised for this cluster, whether or
                    // not a regex was given.
                    if ( clusterCandidates.isEmpty() )
                        throw new GPUdbException("No matching IP/hostname found for cluster with head node URLs " + haRingHeadNodeUrlLists[i] );

                    final List<URL> retained = filterUrlsByHostnameRegex( clusterCandidates, hostnameRegex, "head node URL" );

                    // Every candidate reaching the filter was usable, so an empty
                    // result means the regex is genuinely why nothing was found.
                    if ( retained.isEmpty() )
                        throw new GPUdbBase.GPUdbHostnameRegexFailureException(String.format(
                                "No matching IP/hostname found for cluster with head node URLs %s (given hostname regex %s)",
                                haRingHeadNodeUrlLists[i], hostnameRegex));

                    GPUdbLogger.debug_with_info( "Keeping head node URL: " + retained.get( 0 ) );
                    haRingHeadNodeURLs.add( retained.get( 0 ) );
                }   // end for
            }   // end if
        }   // nothing to do if this property isn't returned or is empty

        return haRingHeadNodeURLs;
    }  // getHARingHeadNodeURLs


}
