package me.darragh.ptaltsapi.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * A provider class for a singleton instance of Gson.
 *
 * @author darraghd493
 * @since 1.0.0
 */
public class GsonProvider {
    private static final Gson GSON = new GsonBuilder()
            .create();

    public static Gson get() {
        return GSON;
    }
}
