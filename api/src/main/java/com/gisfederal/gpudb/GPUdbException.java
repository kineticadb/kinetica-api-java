package com.gisfederal.gpudb;

public final class GPUdbException extends Exception {
    private static final long serialVersionUID = 1L;

    public GPUdbException() {
        super();
    }

    public GPUdbException(String message) {
        super(message);
    }

    public GPUdbException(String message, Throwable cause) {
        super(message, cause);
    }

    public GPUdbException(Throwable cause) {
        super(cause);
    }
}