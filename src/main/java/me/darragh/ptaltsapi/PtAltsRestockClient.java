package me.darragh.ptaltsapi;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import lombok.Value;
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
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * A lightweight SSE client to handle restock events from the PTAlts API.
 *
 * @author darraghd493
 * @since 22/01/2026
 */
public class PtAltsRestockClient {
    public static final String RESTOCK_EVENTS_URL = PtAltsApiClient.BASE_URL + "/restock-events";

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.MINUTES)
            .build();

    private static final Gson GSON = GsonProvider.get();

    /**
     * Starts listening to restock events.
     *
     * @param onEvent The consumer to handle incoming restock events.
     * @param onError The consumer to handle errors (nullable).
     * @return An {@link AutoCloseable} that can be used to stop listening to events.
     * @throws RequestException If there is an error connecting to the restock stream.
     */
    public @NotNull AutoCloseable subscribe(@NotNull Consumer<RestockEvent> onEvent, @Nullable Consumer<Throwable> onError) throws RequestException {
        Objects.requireNonNull(onEvent, "onEvent");

        Request request = new Request.Builder()
                .url(RESTOCK_EVENTS_URL)
                .header("Accept", "text/event-stream")
                .get()
                .build();

        final Response response;
        try {
            response = httpClient.newCall(request).execute();
        } catch (IOException e) {
            throw new RequestException("Error connecting to restock stream", e);
        }

        if (response.code() != 200) {
            closeQuietly(response);
            throw new RequestException("Failed to open restock stream. Status: " + response.code());
        }

        final InputStream bodyStream = response.body().byteStream();
        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "restock-sse-listener");
            t.setDaemon(true);
            return t;
        });
        AtomicBoolean closed = new AtomicBoolean(false);

        executor.submit(() -> listen(bodyStream, onEvent, onError, closed));

        return () -> {
            if (closed.compareAndSet(false, true)) {
                closeQuietly(bodyStream);
                closeQuietly(response);
                executor.shutdownNow();
            }
        };
    }

    private void listen(@NotNull InputStream bodyStream, @NotNull Consumer<RestockEvent> onEvent, @Nullable Consumer<Throwable> onError, @NotNull AtomicBoolean closed) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(bodyStream))) {
            String line;
            StringBuilder dataBuffer = new StringBuilder();
            while (!closed.get() && (line = reader.readLine()) != null) {
                if (line.isEmpty()) { // dispatch accumulated event
                    dispatch(dataBuffer, onEvent);
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

    private void dispatch(@NotNull StringBuilder dataBuffer, @NotNull Consumer<RestockEvent> onEvent) {
        String payload = dataBuffer.toString().trim();
        if (payload.isEmpty()) {
            return;
        }
        try {
            RestockEvent event = GSON.fromJson(payload, RestockEvent.class);
            onEvent.accept(event);
        } catch (Exception ignored) {
            // ignore malformed events
        }
    }

    private void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignored) {
        }
    }

    @Value
    public static class RestockEvent {
        @SerializedName("event") @NotNull String event;
        @SerializedName("stock_type") @NotNull String stockType;
        @SerializedName("added_count") int addedCount;
        @SerializedName("new_total") int newTotal;
        @SerializedName("timestamp") @NotNull String timestamp;
    }
}

