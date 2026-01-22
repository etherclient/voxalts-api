package me.darragh.ptaltsapi;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A wrapper client for all provides client functionality of the PTAlts API.
 *
 * @author darraghd493
 * @since 1.0.2
 */
public abstract class PtAltsClient {
    @Getter
    private final  PtAltsApiClient apiClient;
    @Getter
    private final PtAltsRestockSseClient restockClient;
    @Getter
    private final PtAltsOrderSseClient tokenClient;
    @Getter
    private final PtAltsEventWrapper eventWrapper;

    private @Nullable AutoCloseable restockSubscription;
    private @Nullable AutoCloseable tokenStreamingSubscription;

    public PtAltsClient(@NotNull String apiKey, @NotNull String clientName) {
        this.apiClient = new PtAltsApiClient(apiKey);
        this.restockClient = new PtAltsRestockSseClient();
        this.tokenClient = new PtAltsOrderSseClient(apiKey, clientName);
        this.eventWrapper = new PtAltsEventWrapper();
    }

    /**
     * Launches all SSE connections to start receiving events.
     */
    public void launchConnections() {
        PtAltsOrderSseClient.ClientStatusResponse tokenClientStatusResponse = this.tokenClient.updateStatus("online");
        this.restockSubscription = this.restockClient.subscribe(this.eventWrapper.getOnRestockEvent(), this::handleConnectionException);
        this.tokenStreamingSubscription = this.tokenClient.subscribe(tokenClientStatusResponse.clientId(), this.eventWrapper.getOnTokenEvent(), this::handleConnectionException);
    }

    /**
     * Shuts down all SSE connections.
     *
     * @throws Exception If an error occurs while closing the connections.
     */
    public void shutdownConnections() throws Exception {
        if (this.restockSubscription != null) {
            this.restockSubscription.close();
        }
        if (this.tokenStreamingSubscription != null) {
            this.tokenClient.updateStatus("offline");
            this.tokenStreamingSubscription.close();
        }
    }

    /**
     * Handles exceptions that occur during SSE event processing.
     *
     * @param throwable The exception that occurred.
     */
    public abstract void handleConnectionException(@NotNull Throwable throwable);
}
