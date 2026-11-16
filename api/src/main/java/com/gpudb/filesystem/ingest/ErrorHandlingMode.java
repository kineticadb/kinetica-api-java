package com.gpudb.filesystem.ingest;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ErrorHandlingMode {

    @JsonProperty("permissive")
    PERMISSIVE("permissive"),
    /** Deprecated on the server:  an alias for {@link #SKIP}. */
    @JsonProperty("ignore_bad_records")
    IGNORE_BAD_RECORDS("ignore_bad_records"),
    @JsonProperty("abort")
    ABORT("abort"),
    /** Malformed records are skipped. */
    @JsonProperty("skip")
    SKIP("skip");

    private final String text;

    ErrorHandlingMode(String text) {
        this.text = text;
    }

    public String getText() {
        return this.text;
    }

    /**
     * Returns the mode the server uses when none is given: {@link #ABORT}.
     *
     * @return  the server's default error handling mode
     */
    public static ErrorHandlingMode getDefault() {
        return ABORT;
    }

    /**
     * Returns the text of {@link #PERMISSIVE}, which is not the server's
     * default; that is {@link #ABORT}.
     *
     * @deprecated  Use {@link #getDefault()}, which returns the server's
     *              default.
     * @return  "permissive"
     */
    @Deprecated
    public String defaultMode() {
        return PERMISSIVE.getText();
    }

}
