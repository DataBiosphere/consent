package org.broadinstitute.consent.http.util.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.time.Instant;

public class GsonUtil {
    public static Gson buildGson() {
        return gsonBuilderWithAdapters().create();
    }

    public static GsonBuilder gsonBuilderWithAdapters() {
        return new GsonBuilder()
                .registerTypeAdapter(
                        Instant.class,
                        new InstantTypeAdapter());
    }
}
