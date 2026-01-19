package me.darragh.ptaltsapi.exception;

/**
 * Exception thrown when there is an error with a request to the VoxAlts API.
 *
 * @author darraghd493
 * @since 1.0.0
 */
public class RequestException extends RuntimeException {
    public RequestException(String message) {
        super(message);
    }

    public RequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
