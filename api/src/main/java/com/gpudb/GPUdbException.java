package com.gpudb;

/**
 * An exception that occurred during a GPUdb-related operation.
 */
public class GPUdbException extends Exception {
    private static final long serialVersionUID = 1L;

    // Indicates if a connection failure caused this exception
    protected boolean hadConnectionFailure = false;

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


    /**
     * Creates a new {@link GPUdbException} with the specified message, a
     * cause, and a boolean flag indicating if a connection failure caused this
     * exception.
     *
     * @param message            the message
     * @param cause              the cause
     * @param connectionFailure  boolean flag indicating if there was a connection
     *                           failure which caused this exception
     */
    public GPUdbException(String message, Throwable cause, boolean connectionFailure) {
        super(message, cause);
        hadConnectionFailure = connectionFailure;
    }


    /**
     * Creates a new {@link GPUdbException} with the specified message and a
     * boolean flag indicating if a connection failure caused this exception.
     *
     * @param message            the message
     * @param connectionFailure  boolean flag indicating if there was a connection
     *                           failure which caused this exception
     */
    public GPUdbException(String message, boolean connectionFailure) {
        super(message);
        hadConnectionFailure = connectionFailure;
    }


    /**
     * Returns if a connection failure caused this exception.
     */
    public boolean hadConnectionFailure() {
        return hadConnectionFailure;
    }
}
