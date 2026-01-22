package me.darragh.ptaltsapi;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import me.darragh.ptaltsapi.exception.RequestException;
import me.darragh.ptaltsapi.gson.GsonProvider;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Handles communication with the PTAlts API.
 *
 * @author darraghd493
 * @since 1.0.0
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

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.MINUTES)
            .build();
    private static final Type MAP_STRING_INTEGER_TYPE = new TypeToken<Map<String, Integer>>() {}.getType();
    private static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json");
    private static final Gson GSON = GsonProvider.get();

    private final @NotNull String apiKey;
    //region Methods
    /**
     * Fetches the current status from the VoxAlts API.
     *
     * @return The {@link StatusResponse} containing the number of members.
     * @throws RequestException If there is an error during the request.
     */
    public StatusResponse getStatus() throws RequestException {
        // doesn't need authentication
        Request request = new Request.Builder()
                .url(STATUS_URL)
                .get()
                .build();

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (response.code() != 200) {
                throw new RequestException("Failed to fetch status.\nStatus: " + response.code() + "\nError message: " + extractErrorMessage(response));
            }

            return GsonProvider.get().fromJson(response.body().string(), StatusResponse.class);
        } catch (IOException e) {
            Thread.currentThread().interrupt();
            throw new RequestException("Error fetching status", e);
        }
    }

    /**
     * Fetches the current stock from the PTAlts API.
     *
     * @return The stock as a map of item names to their quantities.
     * @throws RequestException If there is an error during the request.
     */
    public Map<String, Integer> getStock() throws RequestException {
        Request request = new Request.Builder()
                .url(STOCK_URL)
                .header("X-API-Key", this.apiKey)
                .get()
                .build();

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (response.code() != 200) {
                throw new RequestException("Failed to fetch stock.\nStatus: " + response.code() + "\nError message: " + extractErrorMessage(response));
            }

            // We don't always know what items will be in stock, so we use a generic int-to-string map
            return GsonProvider.get().fromJson(response.body().string(), MAP_STRING_INTEGER_TYPE);
        } catch (IOException e) {
            Thread.currentThread().interrupt();
            throw new RequestException("Error fetching stock", e);
        }
    }

    /**
     * Fetches the user's balance from the PTAlts API.
     *
     * @return The balance response containing the balance and Discord ID.
     * @throws RequestException If there is an error during the request.
     */
    public BalanceResponse getBalance() throws RequestException {
        Request request = new Request.Builder()
                .url(BALANCE_URL)
                .header("X-API-Key", this.apiKey)
                .get()
                .build();

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (response.code() != 200) {
                throw new RequestException("Failed to fetch balance.\nStatus: " + response.code() + "\nError message: " + extractErrorMessage(response));
            }

            return GsonProvider.get().fromJson(response.body().string(), BalanceResponse.class);
        } catch (IOException e) {
            Thread.currentThread().interrupt();
            throw new RequestException("Error fetching balance", e);
        }
    }

    /**
     * Retrieves the prices of items from the PTAlts API.
     *
     * @return A map of item names to their prices.
     * @throws RequestException If there is an error during the request.
     */
    public Map<String, Integer> getPrices() throws RequestException {
        Request request = new Request.Builder()
                .url(PRICES_URL)
                .header("X-API-Key", this.apiKey)
                .get()
                .build();

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (response.code() != 200) {
                throw new RequestException("Failed to fetch prices.\nStatus: " + response.code() + "\nError message: " + extractErrorMessage(response));
            }

            return GsonProvider.get().fromJson(response.body().string(), MAP_STRING_INTEGER_TYPE);
        } catch (IOException e) {
            Thread.currentThread().interrupt();
            throw new RequestException("Error fetching prices", e);
        }
    }

    /**
     * Attempts to purchase an item from the PTAlts API.
     *
     * @param purchaseRequest The purchase request containing item type and quantity.
     * @return The purchase response containing details of the purchase.
     * @throws RequestException If there is an error during the request.
     */
    public PurchaseResponse requestPurchase(PurchaseRequest purchaseRequest) throws RequestException {
        String requestBody = GsonProvider.get().toJson(purchaseRequest);
        Request request = new Request.Builder()
                .url(PURCHASE_URL)
                .header("X-API-Key", this.apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "*/*") // ensure we accept all response types
                .post(RequestBody.create(requestBody, MEDIA_TYPE_JSON))
                .build();

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (response.code() != 200 && response.code() != 400) { // special handling for 400 - it is sent i.e., if verification fails
                throw new RequestException("Failed to complete purchase.\nStatus: " + response.code() + "\nError message: " + extractErrorMessage(response));
            }

            return GsonProvider.get().fromJson(response.body().string(), PurchaseResponse.class);
        } catch (IOException e) {
            Thread.currentThread().interrupt();
            throw new RequestException("Error completing purchase", e);
        }
    }

    /**
     * Redeems a token key from the VoxAlts API.
     *
     * @param redeemRequest The redeem request containing the code and optional referral.
     * @return The {@link RedeemResponse} containing details of the redemption.
     * @throws RequestException If there is an error during the request.
     */
    public RedeemResponse redeemToken(RedeemRequest redeemRequest) throws RequestException {
        String requestBody = GsonProvider.get().toJson(redeemRequest);
        Request request = new Request.Builder()
                .url(REDEEM_URL)
                .header("X-API-Key", this.apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "*/*")
                .post(RequestBody.create(requestBody, MEDIA_TYPE_JSON))
                .build();

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (response.code() != 200) {
                throw new RequestException("Failed to redeem token.\nStatus: " + response.code() + "\nError message: " + extractErrorMessage(response));
            }

            return GsonProvider.get().fromJson(response.body().string(), RedeemResponse.class);
        } catch (IOException e) {
            Thread.currentThread().interrupt();
            throw new RequestException("Error redeeming token", e);
        }
    }

    /**
     * Fetches the user's order history from the VoxAlts API.
     *
     * @return The {@link OrderHistoryResponse} containing a list of orders.
     * @throws RequestException If there is an error during the request.
     */
    public OrderHistoryResponse getOrderHistory() throws RequestException {
        Request request = new Request.Builder()
                .url(ORDERS_URL)
                .header("X-API-Key", this.apiKey)
                .get()
                .build();

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (response.code() != 200) {
                throw new RequestException("Failed to fetch order history.\nStatus: " + response.code() + "\nError message: " + extractErrorMessage(response));
            }

            return GsonProvider.get().fromJson(response.body().string(), OrderHistoryResponse.class);
        } catch (IOException e) {
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
    public OrderDetailsResponse getOrderDetails(String orderId) throws RequestException {
        String url = String.format(ORDER_URL, orderId);
        Request request = new Request.Builder()
                .url(url)
                .header("X-API-Key", this.apiKey)
                .get()
                .build();

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (response.code() != 200) {
                throw new RequestException("Failed to fetch order details.\nStatus: " + response.code() + "\nError message: " + extractErrorMessage(response));
            }

            return GsonProvider.get().fromJson(response.body().string(), OrderDetailsResponse.class);
        } catch (IOException e) {
            Thread.currentThread().interrupt();
            throw new RequestException("Error fetching order details", e);
        }
    }
    //endregion

    private String extractErrorMessage(Response response) {
        try {
            ErrorResponse errorResponse = GSON.fromJson(response.body().string(), ErrorResponse.class);
            return errorResponse == null ? "null" : errorResponse.getErrorMessage();
        } catch (Exception ignored) {
            return "null";
        }
    }

    //region Gson Objects
    @Value
    public static class StatusResponse {
        @SerializedName("members") int members;
    }

    @Value
    public static class BalanceResponse {
        @SerializedName("balance") int balance;
        @SerializedName("user_id") @NotNull String discordId;
    }

    @Value
    public static class PurchaseResponse {
        @SerializedName("success") boolean success;
        @SerializedName("order_id") @NotNull String orderId;
        @SerializedName("type") @NotNull String type;
        @SerializedName("quantity") int quantity;
        @SerializedName("cost") int cost;
        @SerializedName("new_balance") int newBalance;
        @SerializedName("accounts") @NotNull String[] accounts; // composed of 'results' - need special handling
    }

    @Value
    public static class PurchaseRequest {
        @SerializedName("type") String type;
        @SerializedName("quantity") int quantity;
    }

    @Value
    public static class RedeemRequest {
        @SerializedName("code") String code;
        @SerializedName("referral") @Nullable String referral;
    }

    @Value
    public static class RedeemResponse {
        @SerializedName("success") boolean success;
        @SerializedName("tokens_added") int tokensAdded;
        @SerializedName("old_balance") int oldBalance;
        @SerializedName("new_balance") int newBalance;
        @SerializedName("referral_bonus") int referralBonus;
        @SerializedName("total_with_bonus") int totalWithBonus;
    }

    @Value
    public static class OrderHistoryResponse {
        @SerializedName("orders") @NotNull OrderHistory[] orders;
    }

    @Value
    public static class OrderHistory {
        @SerializedName("orderId") @NotNull String orderId;
        @SerializedName("type") @NotNull String type;
        @SerializedName("quantity") int quantity;
        @SerializedName("cost") int cost;
        @SerializedName("createdAt") @NotNull String createdAt;
        @SerializedName("status") @NotNull String status;
    }

    @Value
    public static class OrderDetailsResponse {
        @SerializedName("orderId") @NotNull String orderId;
        @SerializedName("type") @NotNull String type;
        @SerializedName("quantity") int quantity;
        @SerializedName("cost") int cost;
        @SerializedName("createdAt") @NotNull String createdAt;
        @SerializedName("status") @NotNull String status;
        @SerializedName("accounts") @NotNull String[] accounts; // composed of 'results' - need special handling
    }

    @Value
    public static class ErrorResponse {
        @SerializedName("error") @NotNull String errorMessage;
    }
    //endregion
}
