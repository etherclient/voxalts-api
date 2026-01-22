package me.darragh.ptaltsapi.exception;

import org.jetbrains.annotations.NotNull;

/**
 * Exception thrown when there is an error with a request to the VoxAlts API.
 *
 * @author darraghd493
 * @since 1.0.0
 */
public class RequestException extends RuntimeException {
    public RequestException(@NotNull String message) {
        super(message);
    }

    public RequestException(@NotNull String message, @NotNull Throwable cause) {
        super(message, cause);
    }
}
