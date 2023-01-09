package com.gpudb.util.url;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.stream.Collectors;

public class UrlUtils {

    public static String constructEndpointUrl( String rootUrl, Map<String, String> params ) {
        return String.format("%s?%s", rootUrl, constructQueryParams(params));
    }

    public static String constructQueryParams( Map<String, String> parameters ) {
        return parameters.entrySet().stream()
                .map(p -> String.format("%s=%s", urlEncodeUTF8(p.getKey()), urlEncodeUTF8(p.getValue())))
                .reduce((p1, p2) -> String.format("%s&%s", p1, p2))
                .orElse("");
    }

    public static String urlEncodeUTF8(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    public static String urlEncodeUTF8(Map<?,?> map) {
        String sb = map.entrySet().stream().map(entry -> String.format("%s=%s",
                urlEncodeUTF8(entry.getKey().toString()),
                urlEncodeUTF8(entry.getValue().toString())
        )).collect(Collectors.joining("&"));
        return sb;
    }

}
