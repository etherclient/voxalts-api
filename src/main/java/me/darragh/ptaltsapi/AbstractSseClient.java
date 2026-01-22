package me.darragh.ptaltsapi;

import com.google.gson.Gson;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import me.darragh.ptaltsapi.exception.RequestException;
import me.darragh.ptaltsapi.gson.GsonProvider;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

    private final @NotNull OkHttpClient httpClient;
    private final @NotNull Class<T> eventClass;

    protected @NotNull AutoCloseable subscribe(@NotNull Request request, @NotNull Consumer<T> onEvent, @Nullable Consumer<Throwable> onError, @NotNull String threadName) throws RequestException {
        Response response;
        try {
            response = this.httpClient.newCall(request).execute();
        } catch (IOException e) {
            Thread.currentThread().interrupt();
            throw new RequestException("Error establishing SSE connection", e);
        }

        if (response.code() != 200) {
            closeQuietly(response);
            throw new RequestException("Failed to establish SSE connection. Status: " + response.code());
        }

        InputStream bodyStream = response.body().byteStream();
        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, threadName);
            t.setDaemon(true);
            return t;
        });
        AtomicBoolean closed = new AtomicBoolean(false);
        executor.submit(() -> this.listen(bodyStream, onEvent, onError, closed));

        return () -> {
            if (closed.compareAndSet(false, true)) {
                closeQuietly(bodyStream);
                closeQuietly(response);
                executor.shutdownNow();
            }
        };
    }

    private void listen(@NotNull InputStream bodyStream, @NotNull Consumer<T> onEvent, @Nullable Consumer<Throwable> onError, @NotNull AtomicBoolean closed) {
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
            T event = GSON.fromJson(payload, this.eventClass);
            onEvent.accept(event);
        } catch (Exception ignored) {
        }
    }

    private static void closeQuietly(@Nullable InputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static void closeQuietly(@Nullable Response response) {
        if (response != null) {
            try {
                response.close();
            } catch (Exception ignored) {
            }
        }
    }
}

