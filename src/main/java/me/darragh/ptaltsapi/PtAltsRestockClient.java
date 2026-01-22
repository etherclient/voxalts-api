package me.darragh.ptaltsapi;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import me.darragh.ptaltsapi.exception.RequestException;
import me.darragh.ptaltsapi.gson.GsonProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMinutes(3))
            .build();

    private static final Gson GSON = GsonProvider.get();

    /**
     * Starts listening to restock events.
     *
     * @param onEvent The consumer to handle incoming restock events.
     * @param onError The consumer to handle errors (nullable).
     * @return An {@link  AutoCloseable} that can be used to stop listening to events.
     * @throws RequestException If there is an error connecting to the restock stream.
     */
    public @NotNull AutoCloseable subscribe(@NotNull Consumer<RestockEvent> onEvent, @Nullable Consumer<Throwable> onError) throws RequestException {
        Objects.requireNonNull(onEvent, "onEvent");

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(RESTOCK_EVENTS_URL))
                .header("Accept", "text/event-stream")
                .GET();

        HttpRequest request = builder.build();
        HttpResponse<InputStream> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RequestException("Error connecting to restock stream", e);
        }

        if (response.statusCode() != 200) {
            throw new RequestException("Failed to open restock stream. Status: " + response.statusCode());
        }

        @SuppressWarnings("resource") // stream is owned by returned AutoCloseable
        InputStream bodyStream = response.body();
        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "restock-sse-listener");
            t.setDaemon(true);
            return t;
        });
        AtomicBoolean closed = new AtomicBoolean(false);

        executor.submit(() -> listen(bodyStream, onEvent, onError, closed));

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
            // uh oh!
            // TODO: log?
        }
    }

    public record RestockEvent(
            @SerializedName("event") @NotNull String event,
            @SerializedName("stock_type") @NotNull String stockType,
            @SerializedName("added_count") int addedCount,
            @SerializedName("new_total") int newTotal,
            @SerializedName("timestamp") @NotNull String timestamp
    ) {
    }
}
