package com.gpudb;

/**
 * An exception that occurred during a GPUdb-related operation.
 */
public class GPUdbException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new {@link GPUdbException} with the specified message.
     *
     * @param message  the message
     */
    public GPUdbException(String message) {
        super(message);
    }

    /**
     * Creates a new {@link GPUdbException} with the specified message and
     * cause.
     *
     * @param message  the message
     * @param cause    the cause
     */
    public GPUdbException(String message, Throwable cause) {
        super(message, cause);
    }
}