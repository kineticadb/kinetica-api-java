package com.gpudb.filesystem.ingest;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ErrorHandlingMode {

    @JsonProperty("permissive")
    PERMISSIVE("permissive"),
    @JsonProperty("ignore_bad_records")
    IGNORE_BAD_RECORDS("ignore_bad_records"),
    @JsonProperty("abort")
    ABORT("abort");

    private final String text;

    ErrorHandlingMode(String text) {
        this.text = text;
    }

    public String getText() {
        return this.text;
    }

    public String defaultMode() {
        return PERMISSIVE.getText();
    }

}
