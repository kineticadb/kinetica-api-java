package com.gpudb.filesystem.ingest;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum TypeInferenceMode {

    @JsonProperty("accuracy")
    ACCURACY("accuracy"),
    @JsonProperty("speed")
    SPEED("speed");

    private final String text;

    TypeInferenceMode(String text) {
        this.text = text;
    }

    public String getText() {
        return this.text;
    }

    public String defaultPartitionType() {
        return SPEED.getText();
    }

}
