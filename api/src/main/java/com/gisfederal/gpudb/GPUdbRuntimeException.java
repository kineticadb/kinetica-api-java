package com.gisfederal.gpudb;

public final class GPUdbRuntimeException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public GPUdbRuntimeException() {
        super();
    }

    public GPUdbRuntimeException(String message) {
        super(message);
    }

    public GPUdbRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public GPUdbRuntimeException(Throwable cause) {
        super(cause);
    }
}