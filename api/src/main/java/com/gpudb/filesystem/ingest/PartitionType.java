package com.gpudb.filesystem.ingest;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum PartitionType {

    @JsonProperty("range")
    RANGE("range"),
    @JsonProperty("interval")
    INTERVAL("interval"),
    @JsonProperty("list")
    LIST("list"),
    @JsonProperty("hash")
    HASH("hash");

    private final String text;

    PartitionType(String text) {
        this.text = text;
    }

    public String getText() {
        return this.text;
    }

    public String toString() { return this.text; }

    public String defaultPartitionType() {
        return RANGE.getText();
    }

}
