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

    /**
     * Returns the mode the server uses when none is given: {@link #ACCURACY}.
     *
     * @return  the server's default type inference mode
     */
    public static TypeInferenceMode getDefault() {
        return ACCURACY;
    }

    /**
     * Returns the text of {@link #SPEED}, which is not the server's default;
     * that is {@link #ACCURACY}.  Misnamed, as it has nothing to do with
     * partitioning.
     *
     * @deprecated  Use {@link #getDefault()}, which returns the server's
     *              default.
     * @return  "speed"
     */
    @Deprecated
    public String defaultPartitionType() {
        return SPEED.getText();
    }

}
