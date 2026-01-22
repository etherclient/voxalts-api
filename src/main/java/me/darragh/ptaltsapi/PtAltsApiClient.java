package me.darragh.ptaltsapi;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import lombok.RequiredArgsConstructor;
import me.darragh.ptaltsapi.exception.RequestException;
import me.darragh.ptaltsapi.gson.GsonProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Handles communication with the PTAlts API.
 *
 * @author darraghd493
 * @since 10/12/2025
 */
@RequiredArgsConstructor
public class PtAltsApiClient {
    public static final String BASE_URL = "https://api.voxalts.store/api";
    public static final String STATUS_URL = BASE_URL + "/status";
    public static final String STOCK_URL = BASE_URL + "/stock";
    public static final String BALANCE_URL = BASE_URL + "/balance";
    public static final String PRICES_URL = BASE_URL + "/prices";
    public static final String PURCHASE_URL = BASE_URL + "/purchase";
    public static final String REDEEM_URL = BASE_URL + "/redeem";
    public static final String ORDERS_URL = BASE_URL + "/orders";
    public static final String ORDER_URL = BASE_URL + "/order/%s"; // needs formatting with order ID

    private final @NotNull String apiKey;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMinutes(3))
            .build();

    private static final Type MAP_STRING_INTEGER_TYPE = new TypeToken<Map<String, Integer>>() {}.getType();

    private static final Gson GSON = GsonProvider.get();

    //region Methods
    /**
     * Fetches the current status from the VoxAlts API.
     *
     * @return The {@link StatusResponse} containing the number of members.
     * @throws RequestException If there is an error during the request.
     */
    public @NotNull StatusResponse getStatus() throws RequestException {
        // doesn't need authentication
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(STATUS_URL))
                .GET()
                .build();

        try {
            HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RequestException("Failed to fetch status.\nStatus: %s\nError message: %s".formatted(response.statusCode(), extractErrorMessage(response)));
            }

            return GsonProvider.get().fromJson(response.body(), StatusResponse.class);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RequestException("Error fetching status", e);
        }
    }

    /**
     * Fetches the current stock from the VoxAlts API.
     *
     * @return A map of item names to their quantities.
     * @throws RequestException If there is an error during the request.
     */
    public @NotNull Map<String, Integer> getStock() throws RequestException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(STOCK_URL))
                .header("X-API-Key", this.apiKey)
                .GET()
                .build();

        try {
            HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RequestException("Failed to fetch stock.\nStatus: %s\nError message: %s".formatted(response.statusCode(), extractErrorMessage(response)));
            }

            // We don't always know what items will be in stock, so we use a generic int-to-string map
            return GsonProvider.get().fromJson(response.body(), MAP_STRING_INTEGER_TYPE);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RequestException("Error fetching stock", e);
        }
    }

    /**
     * Fetches the user's balance from the VoxAlts API.
     *
     * @return The {@link BalanceResponse} containing the balance and Discord ID.
     * @throws RequestException If there is an error during the request.
     */
    public @NotNull BalanceResponse getBalance() throws RequestException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BALANCE_URL))
                .header("X-API-Key", this.apiKey)
                .GET()
                .build();

        try {
            HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RequestException("Failed to fetch balance.\nStatus: %s\nError message: %s".formatted(response.statusCode(), extractErrorMessage(response)));
            }

            return GsonProvider.get().fromJson(response.body(), BalanceResponse.class);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RequestException("Error fetching balance", e);
        }
    }

    /**
     * Retrieves the prices of items from the VoxAlts API.
     *
     * @return A map of item names to their prices.
     * @throws RequestException If there is an error during the request.
     */
    public @NotNull Map<String, Integer> getPrices() throws RequestException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(PRICES_URL))
                .header("X-API-Key", this.apiKey)
                .GET()
                .build();

        try {
            HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RequestException("Failed to fetch prices.\nStatus: %s\nError message: %s".formatted(response.statusCode(), extractErrorMessage(response)));
            }

            return GsonProvider.get().fromJson(response.body(), MAP_STRING_INTEGER_TYPE);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RequestException("Error fetching prices", e);
        }
    }

    /**
     * Attempts to purchase an item from the VoxAlts API.
     *
     * @param purchaseRequest The purchase request containing item type and quantity.
     * @return The {@link PurchaseResponse} containing details of the purchase.
     * @throws RequestException If there is an error during the request.
     */
    public @NotNull PurchaseResponse requestPurchase(@NotNull PurchaseRequest purchaseRequest) throws RequestException {
        String requestBody = GsonProvider.get().toJson(purchaseRequest);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(PURCHASE_URL))
                .header("X-API-Key", this.apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "*/*") // ensure we accept all response types
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200 && response.statusCode() != 400) { // special handling for 400 - it is sent i.e., if verification fails
                throw new RequestException("Failed to complete purchase.\nStatus: %s\nError message: %s".formatted(response.statusCode(), extractErrorMessage(response)));
            }

            return GsonProvider.get().fromJson(response.body(), PurchaseResponse.class);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RequestException("Error completing purchase", e);
        }
    }

    /**
     * Redeems a token key from the VoxAlts API.
     *
     * @param redeemRequest The redeem request containing the code and optional referral.
     * @return The {@link RedeemRequest} containing details of the redemption.
     * @throws RequestException If there is an error during the request.
     */
    public @NotNull RedeemResponse redeemToken(@NotNull RedeemRequest redeemRequest) throws RequestException {
        String requestBody = GsonProvider.get().toJson(redeemRequest);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(REDEEM_URL))
                .header("X-API-Key", this.apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "*/*") // ensure we accept all response types
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RequestException("Failed to redeem token.\nStatus: %s\nError message: %s".formatted(response.statusCode(), extractErrorMessage(response)));
            }

            return GsonProvider.get().fromJson(response.body(), RedeemResponse.class);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RequestException("Error redeeming token", e);
        }
    }

    /**
     * Fetches the user's order history from the VoxAlts API.
     *
     * @see OrderHistory
     * @return The {@link OrderHistoryResponse} containing a list of orders.
     * @throws RequestException If there is an error during the request.
     */
    public @NotNull OrderHistoryResponse getOrderHistory() throws RequestException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ORDERS_URL))
                .header("X-API-Key", this.apiKey)
                .GET()
                .build();

        try {
            HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RequestException("Failed to fetch order history.\nStatus: %s\nError message: %s".formatted(response.statusCode(), extractErrorMessage(response)));
            }

            return GsonProvider.get().fromJson(response.body(), OrderHistoryResponse.class);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RequestException("Error fetching order history", e);
        }
    }

    /**
     * Fetches details of a specific order from the VoxAlts API, including accounts.
     *
     * @param orderId The ID of the order to fetch.
     * @return The {@link OrderDetailsResponse} containing order information and accounts.
     * @throws RequestException If there is an error during the request.
     */
    public @NotNull OrderDetailsResponse getOrderDetails(@NotNull String orderId) throws RequestException {
        String url = ORDER_URL.formatted(orderId);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("X-API-Key", this.apiKey)
                .GET()
                .build();

        try {
            HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RequestException("Failed to fetch order details.\nStatus: %s\nError message: %s".formatted(response.statusCode(), extractErrorMessage(response)));
            }

            return GsonProvider.get().fromJson(response.body(), OrderDetailsResponse.class);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RequestException("Error fetching order details", e);
        }
    }
    //endregion

    private static @NotNull String extractErrorMessage(@NotNull HttpResponse<String> response) {
        ErrorResponse errorResponse = null;
        try {
            errorResponse = GSON.fromJson(response.body(), ErrorResponse.class);
        } catch (Exception ignored) {
        }
        return errorResponse == null ? "null" : errorResponse.errorMessage();
    }

    //region Gson Records
    public record StatusResponse(
            @SerializedName("members") int members
    ) {
    }

    public record BalanceResponse(
            @SerializedName("balance") int balance,
            @SerializedName("user_id") @NotNull String discordId
    ) {
    }

    public record PurchaseResponse(
            @SerializedName("success") boolean success,
            @SerializedName("order_id") @NotNull String orderId,
            @SerializedName("type") @NotNull String type,
            @SerializedName("quantity") int quantity,
            @SerializedName("cost") int cost,
            @SerializedName("new_balance") int newBalance,
            @SerializedName("accounts") @NotNull String[] accounts // composed of 'results' - need special handling
    ) {
    }

    public record PurchaseRequest(
            @SerializedName("type") @NotNull String type,
            @SerializedName("quantity") int quantity
    ) {
    }

    public record RedeemRequest(
            @SerializedName("code") @NotNull String code,
            @SerializedName("referral") @Nullable String referral
    ) {
    }

    public record RedeemResponse(
            @SerializedName("success") boolean success,
            @SerializedName("tokens_added") int tokensAdded,
            @SerializedName("old_balance") int oldBalance,
            @SerializedName("new_balance") int newBalance,
            @SerializedName("referral_bonus") int referralBonus,
            @SerializedName("total_with_bonus") int totalWithBonus
    ) {
    }

    public record OrderHistoryResponse(
            @SerializedName("orders") @NotNull OrderHistory[] orders
    ) {
    }

    public record OrderHistory(
            @SerializedName("orderId") @NotNull String orderId,
            @SerializedName("type") @NotNull String type,
            @SerializedName("quantity") int quantity,
            @SerializedName("cost") int cost,
            @SerializedName("createdAt") @NotNull String createdAt,
            @SerializedName("status") @NotNull String status
    ) {
    }

    public record OrderDetailsResponse(
            @SerializedName("orderId") @NotNull String orderId,
            @SerializedName("type") @NotNull String type,
            @SerializedName("quantity") int quantity,
            @SerializedName("cost") int cost,
            @SerializedName("createdAt") @NotNull String createdAt,
            @SerializedName("status") @NotNull String status,
            @SerializedName("accounts") @NotNull String[] accounts // composed of 'results' - need special handling
    ) {
    }

    private record ErrorResponse(
            @SerializedName("error") @NotNull String errorMessage
    ) {
    }
    //endregion
}
