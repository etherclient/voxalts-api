package me.darragh.ptaltsapi;

import com.google.gson.Gson;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import me.darragh.ptaltsapi.exception.RequestException;
import me.darragh.ptaltsapi.gson.GsonProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * A generic SSE client implementation.
 *
 * @param <T> The type of event to be received.
 * @author darraghd493
 * @since 1.0.2
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
class AbstractSseClient<T> {
    protected static final Gson GSON = GsonProvider.get();

    private final @NotNull HttpClient httpClient;
    private final @NotNull Class<T> eventClass;

    protected @NotNull AutoCloseable subscribe(@NotNull HttpRequest request, @NotNull Consumer<T> onEvent, @Nullable Consumer<Throwable> onError, @NotNull String threadName) throws RequestException {
        HttpResponse<InputStream> response;
        try {
            response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RequestException("Error establishing SSE connection", e);
        }

        if (response.statusCode() != 200) {
            throw new RequestException("Failed to establish SSE connection. Status: " + response.statusCode());
        }

        @SuppressWarnings("resource")
        InputStream bodyStream = response.body();
        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, threadName);
            t.setDaemon(true);
            return t;
        });
        AtomicBoolean closed = new AtomicBoolean(false);
        executor.submit(() -> this.listen(bodyStream, onEvent, onError, closed));

        return () -> {
            if (closed.compareAndSet(false, true)) {
                try {
                    bodyStream.close();
                } catch (IOException ignored) {
                }
                executor.shutdownNow();
            }
        };
    }

    private void listen(@NotNull InputStream bodyStream,
                        @NotNull Consumer<T> onEvent,
                        @Nullable Consumer<Throwable> onError,
                        @NotNull AtomicBoolean closed) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(bodyStream))) {
            String line;
            StringBuilder dataBuffer = new StringBuilder();
            while (!closed.get() && (line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    this.dispatch(dataBuffer, onEvent);
                    dataBuffer.setLength(0);
                    continue;
                }

                if (line.startsWith("data:")) {
                    dataBuffer.append(line.substring("data:".length()).trim()).append('\n');
                }
            }
        } catch (Exception e) {
            if (!closed.get() && onError != null) {
                onError.accept(e);
            }
        } finally {
            closed.set(true);
        }
    }

    private void dispatch(@NotNull StringBuilder dataBuffer, @NotNull Consumer<T> onEvent) {
        String payload = dataBuffer.toString().trim();
        if (payload.isEmpty()) {
            return;
        }
        try {
            T event = this.GSON.fromJson(payload, this.eventClass);
            onEvent.accept(event);
        } catch (Exception ignored) {
        }
    }
}

