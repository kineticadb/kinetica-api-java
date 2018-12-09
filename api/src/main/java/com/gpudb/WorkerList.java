package com.gpudb;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A list of worker URLs to use for multi-head operations.
 */
public class WorkerList extends ArrayList<URL> {
    private static final long serialVersionUID = 1L;

    private boolean isMultiHeadEnabled;
    private Pattern ipRegex;

    /**
     * Creates an empty {@link WorkerList} that can be populated manually
     * with worker URLs to support multi-head operations. Note that worker URLs
     * must be added in rank order, starting with rank 1, and all worker
     * ranks must be included; otherwise operations may fail for certain
     * data types.
     */
    public WorkerList() {
    }

    /**
     * Creates a {@link WorkerList} and automatically populates it with the
     * worker URLs from GPUdb to support multi-head operations. (If the
     * specified GPUdb instance has multi-head operations disabled, the worker
     * list will be empty and multi-head operations will not be used.) Note that
     * in some cases, workers may be configured to use more than one IP
     * address, not all of which may be accessible to the client; this
     * constructor uses the first IP returned by the server for each worker.
     * To override this behavior, use one of the alternate constructors that
     * accepts an {@link #WorkerList(GPUdb, Pattern) IP regex} or an {@link
     * #WorkerList(GPUdb, String) IP prefix}.
     *
     * @param gpudb    the {@link GPUdb} instance from which to obtain the
     *                 worker URLs
     *
     * @throws GPUdbException if an error occurs during the request for
     * worker URLs
     */
    public WorkerList(GPUdb gpudb) throws GPUdbException {
        this(gpudb, (Pattern)null);
    }

    /**
     * Creates a {@link WorkerList} and automatically populates it with the
     * worker URLs from GPUdb to support multi-head operations. (If the
     * specified GPUdb instance has multi-head operations disabled, the worker
     * list will be empty and multi-head operations will not be used.) Note that
     * in some cases, workers may be configured to use more than one IP
     * address, not all of which may be accessible to the client; the
     * optional {@code ipRegex} parameter can be used in such cases to
     * filter for an IP range that is accessible, e.g., a regex of
     * {@code "192\.168\..*"} will use worker IP addresses in the 192.168.*
     * range.
     *
     * @param gpudb    the {@link GPUdb} instance from which to obtain the
     *                 worker URLs
     * @param ipRegex  optional IP regex to match
     *
     * @throws GPUdbException if an error occurs during the request for
     * worker URLs or no IP addresses matching the IP regex could be found
     * for one or more workers
     */
    public WorkerList(GPUdb gpudb, Pattern ipRegex) throws GPUdbException {

        this.ipRegex = ipRegex;
        
        Map<String, String> systemProperties = gpudb.showSystemProperties(GPUdb.options()).getPropertyMap();

        String s = systemProperties.get("conf.enable_worker_http_servers");

        if (s == null) {
            throw new GPUdbException("Missing value for conf.enable_worker_http_servers.");
        }

        // Check if multi-head I/O is enabled
        if (s.equals("FALSE")) {
            return;
        }
        this.isMultiHeadEnabled = true;

        if (gpudb.getURLs().size() > 1) {
            throw new GPUdbException("Multi-head ingest not supported with failover URLs.");
        }

        s = systemProperties.get("conf.worker_http_server_urls");

        if (s != null) {
            String[] urlLists = s.split(";");

            for (int i = 1; i < urlLists.length; i++) {

                // Handle removed ranks
                if ( urlLists[i].isEmpty() ) {
                    add( null );
                    continue;
                }
                
                String[] urls = urlLists[i].split(",");
                boolean found = false;

                for (String urlString : urls) {
                    URL url;
                    boolean match;

                    try {
                        url = new URL(urlString);
                    } catch (MalformedURLException ex) {
                        throw new GPUdbException(ex.getMessage(), ex);
                    }

                    if (this.ipRegex != null) {
                        match = this.ipRegex.matcher(url.getHost()).matches();
                    } else {
                        match = true;
                    }

                    if (match) {
                        add(url);
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    throw new GPUdbException("No matching IP found for worker " + i + ".");
                }
            }
        } else {
            s = systemProperties.get("conf.worker_http_server_ips");

            if (s == null) {
                throw new GPUdbException("Missing value for conf.worker_http_server_ips.");
            }

            String[] ipLists = s.split(";");

            s = systemProperties.get("conf.worker_http_server_ports");

            if (s == null) {
                throw new GPUdbException("Missing value for conf.worker_http_server_ports.");
            }

            String[] ports = s.split(";");

            if (ipLists.length != ports.length) {
                throw new GPUdbException("Inconsistent number of values for conf.worker_http_server_ips and conf.worker_http_server_ports.");
            }

            String protocol = gpudb.getURL().getProtocol();

            for (int i = 1; i < ipLists.length; i++) {
                // Handle removed ranks
                if ( ipLists[i].isEmpty() ) {
                    add( null );
                    continue;
                }

                String[] ips = ipLists[i].split(",");
                boolean found = false;

                for (String ip : ips) {
                    boolean match;

                    if (this.ipRegex != null) {
                        match = this.ipRegex.matcher(ip).matches();
                    } else {
                        match = true;
                    }

                    if (match) {
                        try {
                            add(new URL(protocol + "://" + ip + ":" + ports[i]));
                        } catch (MalformedURLException ex) {
                            throw new GPUdbException(ex.getMessage(), ex);
                        }

                        found = true;
                        break;
                    }
                }

                if (!found) {
                    throw new GPUdbException("No matching IP found for worker " + i + ".");
                }
            }
        }

        if (isEmpty()) {
            throw new GPUdbException("No worker HTTP servers found.");
        }
    }

    /**
     * Creates a {@link WorkerList} and automatically populates it with the
     * worker URLs from GPUdb to support multi-head operations. (If the
     * specified GPUdb instance has multi-head operations disabled, the worker
     * list will be empty and multi-head operations will not be used.) Note that
     * in some cases, workers may be configured to use more than one IP
     * address, not all of which may be accessible to the client; the
     * optional {@code ipprefix} parameter can be used in such cases to
     * filter for an IP range that is accessible, e.g., a prefix of
     * {@code "192.168."} will use worker IP addresses in the 192.168.*
     * range.
     *
     * @param gpudb     the {@link GPUdb} instance from which to obtain the
     *                  worker URLs
     * @param ipPrefix  optional IP prefix to match
     *
     * @throws GPUdbException if an error occurs during the request for
     * worker URLs or no IP addresses matching the IP prefix could be found
     * for one or more workers
     */
    public WorkerList(GPUdb gpudb, String ipPrefix) throws GPUdbException {
        this(gpudb, (ipPrefix == null) ? null
                : Pattern.compile(Pattern.quote(ipPrefix) + ".*"));
    }


    /**
     * @return  the IP regular expression used to create this worker list.
     *
     */
    public Pattern getIpRegex() {
        return this.ipRegex;
    }


    /**
     * return  a boolean indicating whether multi-head I/O is enabled at the
     * server.
     */
    public boolean isMultiHeadEnabled() {
        return this.isMultiHeadEnabled;
    }
}
