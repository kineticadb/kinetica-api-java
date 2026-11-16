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
    SHAPEFILE("shapefile"),
    @JsonProperty("avro")
    AVRO("avro"),
    @JsonProperty("gdb")
    GDB("gdb");

    private final String text;

    FileType(String text) {
        this.text = text;
    }

    public String getText() {
        return this.text;
    }

    /**
     * Returns the file type the server assumes when none is given and none
     * can be told from the file's extension: {@link #DELIMITED_TEXT}.
     *
     * @return  the server's default file type
     */
    public static FileType getDefault() {
        return DELIMITED_TEXT;
    }

    /**
     * Returns the text of the default file type; misnamed, as it has nothing
     * to do with partitioning.
     *
     * @deprecated  Use {@link #getDefault()}.
     * @return  "delimited_text"
     */
    @Deprecated
    public String defaultPartitionType() {
        return DELIMITED_TEXT.getText();
    }

}
