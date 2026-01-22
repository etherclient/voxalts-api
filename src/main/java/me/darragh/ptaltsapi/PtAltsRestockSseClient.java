package me.darragh.ptaltsapi;

import com.google.gson.annotations.SerializedName;
import lombok.Value;
import me.darragh.ptaltsapi.exception.RequestException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * A client for subscribing to PTAlts restock events.
 *
 * @author darraghd493
 * @since 1.0.2
 */
public class PtAltsRestockSseClient extends AbstractSseClient<PtAltsRestockSseClient.RestockEvent> {
    public static final String RESTOCK_EVENTS_URL = PtAltsApiClient.BASE_URL + "/restock-events";

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.MINUTES)
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
        Request request = new Request.Builder()
                .url(RESTOCK_EVENTS_URL)
                .header("Accept", "text/event-stream")
                .get()
                .build();

        return this.subscribe(request, onEvent, onError, "restock-sse-listener");
    }

    @Value
    public class RestockEvent {
        @SerializedName("event") @NotNull String event; // we don't know what other possible event types are available, however it appears to only be restock
        @SerializedName("stock_type") @NotNull String stockType;
        @SerializedName("added_count") int addedCount;
        @SerializedName("new_total") int newTotal;
        @SerializedName("timestamp") @NotNull String timestamp;
    }
}
