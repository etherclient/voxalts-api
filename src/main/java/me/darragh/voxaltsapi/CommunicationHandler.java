package me.darragh.voxaltsapi;

import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import me.darragh.voxaltsapi.exception.RequestException;
import me.darragh.voxaltsapi.gson.GsonProvider;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Map;

/**
 * Handles communication with the VoxAlts API.
 *
 * @author darraghd493
 * @since 10/12/2025
 */
@RequiredArgsConstructor
public class CommunicationHandler {
    @SuppressWarnings("HttpUrlsUsage")
    public static final String BASE_URL = "http://api.voxalts.store/api";
    public static final String STOCK_URL = BASE_URL + "/stock";
    public static final String BALANCE_URL = BASE_URL + "/balance";
    public static final String PRICES_URL = BASE_URL + "/prices";
    public static final String PURCHASE_URL = BASE_URL + "/purchase";

    private final String apiKey;
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofMinutes(3))
            .build();

    private static final Type MAP_STRING_INTEGER_TYPE = new TypeToken<Map<String, Integer>>() {}.getType();

    //region Methods
    /**
     * Fetches the current stock from the VoxAlts API.
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

        try (Response response = this.httpClient.newCall(request).execute()) {
            if (response.code() != 200) {
                throw new RequestException("Failed to fetch stock. Status: " + response.code());
            }

            // We don't always know what items will be in stock, so we use a generic int-to-string map
            return GsonProvider.get().fromJson(response.body().string(), MAP_STRING_INTEGER_TYPE);
        } catch (IOException e) {
            Thread.currentThread().interrupt();
            throw new RequestException("Error fetching stock", e);
        }
    }

    /**
     * Fetches the user's balance from the VoxAlts API.
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

        try (Response response = this.httpClient.newCall(request).execute()) {
            if (response.code() != 200) {
                throw new RequestException("Failed to fetch balance. Status: " + response.code());
            }

            return GsonProvider.get().fromJson(response.body().string(), BalanceResponse.class);
        } catch (IOException e) {
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
    public Map<String, Integer> getPrices() throws RequestException {
        Request request = new Request.Builder()
                .url(PRICES_URL)
                .header("X-API-Key", this.apiKey)
                .get()
                .build();

        try (Response response = this.httpClient.newCall(request).execute()) {
            if (response.code() != 200) {
                throw new RequestException("Failed to fetch prices. Status: " + response.code());
            }

            return GsonProvider.get().fromJson(response.body().string(), MAP_STRING_INTEGER_TYPE);
        } catch (IOException e) {
            Thread.currentThread().interrupt();
            throw new RequestException("Error fetching prices", e);
        }
    }

    /**
     * Attempts to purchase an item from the VoxAlts API.
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
                .post(RequestBody.create(requestBody.getBytes()))
                .build();

        try (Response response = this.httpClient.newCall(request).execute()) {
            if (response.code() != 200 && response.code() != 400) { // special handling for 400 - it is sent i.e., if verification fails
                throw new RequestException("Failed to complete purchase. Status: " + response.code());
            }

            return GsonProvider.get().fromJson(response.body().string(), PurchaseResponse.class);
        } catch (IOException e) {
            Thread.currentThread().interrupt();
            throw new RequestException("Error completing purchase", e);
        }
    }
    //endregion

    //region Gson Objects
    @Value
    public static class BalanceResponse {
        @SerializedName("balance") int balance;
        @SerializedName("user_id") String discordId;
    }

    @Value
    public static class PurchaseResponse {
            @SerializedName("accounts") String[] accounts; // composed of 'results' - need special handling
            @SerializedName("charged") int charged;
            @SerializedName("new_balance") int newBalance;
            @SerializedName("quantity") int quantity;
            @SerializedName("requested") int requested;
            @SerializedName("success") boolean success;
            @SerializedName("type") String type;
            @SerializedName("error") @Nullable String error;
            @SerializedName("verification") @NotNull VerificationInformation verification;
    }

    @Value
    public static class VerificationInformation {
            @SerializedName("checked") int checked;
            @SerializedName("elapsed_seconds") double elapsedSeconds;
            @SerializedName("moved_to_banned") int movedToBanned;
            @SerializedName("moved_to_unbanned") int movedToUnbanned;
    }

    @Value
    public static class PurchaseRequest {
            @SerializedName("type") String type;
            @SerializedName("quantity") int quantity;
    }
    //endregion
}
