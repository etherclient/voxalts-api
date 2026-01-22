package me.darragh.ptaltsapi;

import com.google.gson.annotations.SerializedName;
import me.darragh.ptaltsapi.exception.RequestException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.function.Consumer;

/**
 * A client for subscribing to PTAlts restock events.
 *
 * @author darraghd493
 * @since 1.0.2
 */
public class PtAltsRestockSseClient extends AbstractSseClient<PtAltsRestockSseClient.RestockEvent> {
    public static final String RESTOCK_EVENTS_URL = PtAltsApiClient.BASE_URL + "/restock-events";

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMinutes(3))
            .build();

    PtAltsRestockSseClient() {
        super(HTTP_CLIENT, RestockEvent.class);
    }

    /**
     * Starts listening to restock events.
     *
     * @param onEvent The consumer to handle incoming restock events.
     * @param onError The consumer to handle errors (nullable).
     * @return An {@link  AutoCloseable} that can be used to stop listening to events.
     * @throws RequestException If there is an error connecting to the restock stream.
     */
    public @NotNull AutoCloseable subscribe(@NotNull Consumer<RestockEvent> onEvent, @Nullable Consumer<Throwable> onError) throws RequestException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(RESTOCK_EVENTS_URL))
                .header("Accept", "text/event-stream")
                .GET().build();

        return this.subscribe(request, onEvent, onError, "restock-sse-listener");
    }

    public record RestockEvent(
            @SerializedName("event") @NotNull String event,// we don't know what other possible event types are available, however it appears to only be restock
            @SerializedName("stock_type") @NotNull String stockType,
            @SerializedName("added_count") int addedCount,
            @SerializedName("new_total") int newTotal,
            @SerializedName("timestamp") @NotNull String timestamp
    ) {
    }
}
