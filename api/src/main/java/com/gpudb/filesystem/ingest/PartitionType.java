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
    HASH("hash"),
    @JsonProperty("series")
    SERIES("series");

    private final String text;

    PartitionType(String text) {
        this.text = text;
    }

    /**
     * Returns the name of this partition type, in lowercase.  The server
     * documents the values in uppercase, which is how
     * {@link TableCreationOptions#setPartitionType(PartitionType)} sends them.
     *
     * @return  the lowercase name of this partition type
     */
    public String getText() {
        return this.text;
    }

    public String toString() { return this.text; }

    public String defaultPartitionType() {
        return RANGE.getText();
    }

}
