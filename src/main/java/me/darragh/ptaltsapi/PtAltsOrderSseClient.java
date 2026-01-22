package me.darragh.ptaltsapi;

import com.google.gson.annotations.SerializedName;
import lombok.Value;
import me.darragh.ptaltsapi.exception.RequestException;
import okhttp3.*;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * A client for handling PTAlts order-streaming.
 * This is used to transmit orders easily across clients/services, i.e., from the official dashboard to a client.
 *
 * @author darraghd493
 * @since 1.0.2
 */
public class PtAltsOrderSseClient extends AbstractSseClient<PtAltsOrderSseClient.TokenEvent> {
    public static final String CONNECTED_EVENT_TYPE = "connected";
    public static final String HEARTBEAT_EVENT_TYPE = "heartbeat";
    public static final String TOKEN_EVENT_TYPE = "token";

    public static final String BASE_URL = PtAltsApiClient.BASE_URL + "/client";
    public static final String CLIENT_STATUS_URL = BASE_URL + "/status";
    public static final String CLIENT_LISTEN_URL = BASE_URL + "/listen/%s"; // needs formatting with client_id
    public static final String CLIENT_PUSH_TOKEN_URL = BASE_URL + "/push-token";

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.MINUTES)
            .build();

    private final @NotNull String apiKey;
    private final @NotNull String clientName;

    PtAltsOrderSseClient(@NotNull String apiKey, @NotNull String clientName) {
        super(HTTP_CLIENT, TokenEvent.class);
        this.apiKey = apiKey;
        this.clientName = clientName;
    }

    //region Methods
    //region Endpoints
    /**
     * Updates a client status.
     *
     * @param status The status of the client.
     * @return The {@link ClientStatusResponse} containing all client information.
     * @throws RequestException If there is an error during the request.
     */
    public @NotNull ClientStatusResponse updateStatus(@NotNull @MagicConstant(stringValues = {"online", "offline"}) String status) throws RequestException {
        ClientStatusRequest request = new ClientStatusRequest(this.clientName, status);
        String requestBody = GSON.toJson(request);

        Request httpRequest = new Request.Builder()
                .url(CLIENT_STATUS_URL)
                .header("X-API-Key", this.apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                .build();

        try (Response response = HTTP_CLIENT.newCall(httpRequest).execute()) {
            if (response.code() != 200) {
                throw new RequestException("Failed to update client status.\nStatus: " + response.code() + "\nError message: " + extractErrorMessage(response));
            }

            return GSON.fromJson(response.body().string(), ClientStatusResponse.class);
        } catch (IOException e) {
            throw new RequestException("Error registering client", e);
        }
    }

    /**
     * Fetches the status of all registered clients for the authenticated user.
     *
     * @return The {@link ClientsStatusResponse} containing all required user information.
     * @throws RequestException If there is an error during the request.
     */
    public @NotNull ClientsStatusResponse getClientsStatus() throws RequestException {
        Request request = new Request.Builder()
                .url(CLIENT_STATUS_URL)
                .header("X-API-Key", this.apiKey)
                .get()
                .build();

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (response.code() != 200) {
                throw new RequestException("Failed to fetch clients status.\nStatus: " + response.code() + "\nError message: " + extractErrorMessage(response));
            }

            return GSON.fromJson(response.body().string(), ClientsStatusResponse.class);
        } catch (IOException e) {
            throw new RequestException("Error fetching clients status", e);
        }
    }

    /**
     * Pushes an order to a client.
     *
     * @param clientId The client ID to push the token to.
     * @param orderId The order ID containing the token.
     * @return The {@link PushTokenResponse} containing details of the push operation.
     * @throws RequestException If there is an error during the request.
     */
    public @NotNull PushTokenResponse pushOrder(@NotNull String clientId, @NotNull String orderId) throws RequestException {
        PushTokenRequest request = new PushTokenRequest(clientId, orderId);
        String requestBody = GSON.toJson(request);

        Request httpRequest = new Request.Builder()
                .url(CLIENT_PUSH_TOKEN_URL)
                .header("X-API-Key", this.apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                .build();

        try (Response response = HTTP_CLIENT.newCall(httpRequest).execute()) {
            if (response.code() != 200) {
                throw new RequestException("Failed to push token to client.\nStatus: " + response.code() + "\nError message: " + extractErrorMessage(response));
            }

            return GSON.fromJson(response.body().string(), PushTokenResponse.class);
        } catch (IOException e) {
            throw new RequestException("Error pushing token to client", e);
        }
    }
    //endregion

    /**
     * Starts listening to order-streaming events.
     *
     * @param clientId The client ID to listen to - provided by {@link #updateStatus(String)}.
     * @param onEvent The consumer to handle incoming token events.
     * @param onError The consumer to handle errors (nullable).
     * @return An {@link  AutoCloseable} that can be used to stop listening to events.
     * @throws RequestException If there is an error during the request or streaming.
     */
    public @NotNull AutoCloseable subscribe(@NotNull String clientId, @NotNull Consumer<TokenEvent> onEvent, @Nullable Consumer<Throwable> onError) throws RequestException {
        String url = String.format(CLIENT_LISTEN_URL, clientId);
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        return this.subscribe(request, onEvent, onError, "token-sse-listener");
    }
    //endregion

    private static @NotNull String extractErrorMessage(@NotNull Response response) {
        ErrorResponse errorResponse = null;
        try {
            errorResponse = GSON.fromJson(response.body().string(), ErrorResponse.class);
        } catch (Exception ignored) {
        }
        return errorResponse == null ? "null" : errorResponse.getErrorMessage();
    }

    //region Gson Objects
    @Value
    public static class ClientStatusRequest {
        @SerializedName("client") @NotNull String clientName;
        @SerializedName("status") @NotNull String status;
    }

    @Value
    public static class ClientStatusResponse {
        @SerializedName("success") boolean success;
        @SerializedName("user_id") @NotNull String userId;
        @SerializedName("client") @NotNull String clientName;
        @SerializedName("status") @NotNull String status;
        @SerializedName("client_id") @NotNull String clientId;
    }

    @Value
    public static class ClientInfo {
        @SerializedName("status") @NotNull String status;
        @SerializedName("client_id") @NotNull String clientId;
        @SerializedName("last_seen") @NotNull String lastSeen;
    }

    @Value
    public static class ClientsStatusResponse {
        @SerializedName("user_id") @NotNull String userId;
        @SerializedName("clients") @NotNull Map<String, ClientInfo> clients;
    }

    @Value
    public static class PushTokenRequest {
        @SerializedName("client_id") @NotNull String clientId;
        @SerializedName("order_id") @NotNull String orderId;
    }

    @Value
    public static class PushTokenResponse {
        @SerializedName("success") boolean success;
        @SerializedName("tokens_sent") int tokensSent;
        @SerializedName("client_id") @NotNull String clientId;
        @SerializedName("order_id") @NotNull String orderId;
    }

    @Value
    public static class TokenEvent {
        @SerializedName("event") @NotNull String event;
        @SerializedName("client_id") @Nullable String clientId;
        @SerializedName("order_id") @Nullable String orderId;
        @SerializedName("token") @Nullable String token;
        @SerializedName("timestamp") @Nullable String timestamp;

        public boolean isConnected() {
            return CONNECTED_EVENT_TYPE.equalsIgnoreCase(this.event);
        }

        public boolean isHeartbeat() {
            return HEARTBEAT_EVENT_TYPE.equalsIgnoreCase(this.event);
        }

        public boolean isToken() {
            return TOKEN_EVENT_TYPE.equalsIgnoreCase(this.event);
        }
    }

    @Value
    private static class ErrorResponse {
        @SerializedName("error") @NotNull String errorMessage;
    }
    //endregion
}

