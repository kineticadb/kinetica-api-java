package com.gpudb.filesystem.ingest;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum FileType {

    @JsonProperty("delimited_text")
    DELIMITED_TEXT("delimited_text"),
    @JsonProperty("parquet")
    PARQUET("parquet"),
    @JsonProperty("json")
    JSON("json"),
    @JsonProperty("shapefile")
    SHAPEFILE("shapefile");

    private final String text;

    FileType(String text) {
        this.text = text;
    }

    public String getText() {
        return this.text;
    }

    public String defaultPartitionType() {
        return DELIMITED_TEXT.getText();
    }

}
