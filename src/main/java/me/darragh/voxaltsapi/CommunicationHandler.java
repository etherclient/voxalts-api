package me.darragh.voxaltsapi;

import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import lombok.RequiredArgsConstructor;
import me.darragh.voxaltsapi.exception.RequestException;
import me.darragh.voxaltsapi.gson.GsonProvider;
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
 * Handles communication with the VoxAlts API.
 *
 * @author darraghd493
 * @since 10/12/2025
 */
@RequiredArgsConstructor
public class CommunicationHandler {
    public static final String BASE_URL = "http://api.voxalts.store/api";
    public static final String STOCK_URL = BASE_URL + "/stock";
    public static final String BALANCE_URL = BASE_URL + "/balance";
    public static final String PRICES_URL = BASE_URL + "/prices";
    public static final String PURCHASE_URL = BASE_URL + "/purchase";

    private final String apiKey;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
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
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(STOCK_URL))
                .header("X-API-Key", this.apiKey)
                .GET()
                .build();

        try {
            HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RequestException("Failed to fetch stock. Status: " + response.statusCode());
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
     * @return The balance response containing the balance and Discord ID.
     * @throws RequestException If there is an error during the request.
     */
    public BalanceResponse getBalance() throws RequestException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BALANCE_URL))
                .header("X-API-Key", this.apiKey)
                .GET()
                .build();

        try {
            HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RequestException("Failed to fetch balance. Status: " + response.statusCode());
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
    public Map<String, Integer> getPrices() throws RequestException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(PRICES_URL))
                .header("X-API-Key", this.apiKey)
                .GET()
                .build();

        try {
            HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RequestException("Failed to fetch prices. Status: " + response.statusCode());
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
     * @return The purchase response containing details of the purchase.
     * @throws RequestException If there is an error during the request.
     */
    public PurchaseResponse requestPurchase(PurchaseRequest purchaseRequest) throws RequestException {
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
                throw new RequestException("Failed to complete purchase. Status: " + response.statusCode());
            }

            return GsonProvider.get().fromJson(response.body(), PurchaseResponse.class);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RequestException("Error completing purchase", e);
        }
    }
    //endregion

    //region Gson Records
    public record BalanceResponse(
            @SerializedName("balance") int balance,
            @SerializedName("user_id") String discordId
    ) {
    }

    public record PurchaseResponse(
            @SerializedName("accounts") String[] accounts, // composed of 'results' - need special handling
            @SerializedName("charged") int charged,
            @SerializedName("new_balance") int newBalance,
            @SerializedName("quantity") int quantity,
            @SerializedName("requested") int requested,
            @SerializedName("success") boolean success,
            @SerializedName("type") String type,
            @SerializedName("error") @Nullable String error,
            @SerializedName("verification") @NotNull VerificationInformation verification

    ) {
    }

    public record VerificationInformation(
            @SerializedName("checked") int checked,
            @SerializedName("elapsed_seconds") double elapsedSeconds,
            @SerializedName("moved_to_banned") int movedToBanned,
            @SerializedName("moved_to_unbanned") int movedToUnbanned
    ) {
    }

    public record PurchaseRequest(
            @SerializedName("type") String type,
            @SerializedName("quantity") int quantity
    ) {
    }
    //endregion
}
