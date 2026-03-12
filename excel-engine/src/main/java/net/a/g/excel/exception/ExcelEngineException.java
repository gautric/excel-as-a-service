package net.a.g.excel.exception;

/**
 * Base exception class for all Excel engine related errors.
 * This exception serves as the parent for all specific Excel operation exceptions.
 */
public class ExcelEngineException extends RuntimeException {

    /**
     * Creates a new exception with a message.
     *
     * @param message the error message
     */
    public ExcelEngineException(String message) {
        super(message);
    }

    /**
     * Creates a new exception with a message and cause.
     *
     * @param message the error message
     * @param cause the underlying cause
     */
    public ExcelEngineException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new exception with a cause.
     *
     * @param cause the underlying cause
     */
    public ExcelEngineException(Throwable cause) {
        super(cause);
    }
}
