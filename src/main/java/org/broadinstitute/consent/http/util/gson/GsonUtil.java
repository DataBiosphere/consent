package org.broadinstitute.consent.http.util.gson;

import com.google.cloud.storage.BlobId;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;

public class GsonUtil {

  private static Gson instance;

  private static final Gson REGISTRATION_INSTANCE =
      gsonBuilderWithAdapters()
          .serializeNulls()
          .registerTypeAdapterFactory(new JsonValueEnumTypeAdapterFactory())
          .create();

  public static Gson getInstance() {
    if (Objects.isNull(instance)) {
      instance = buildGson();
    }
    return instance;
  }

  public static Gson buildGson() {
    return gsonBuilderWithAdapters().create();
  }

  /**
   * Writes registration documents: nulls included, and enums as their Jackson {@code @JsonValue}.
   */
  public static Gson getRegistrationInstance() {
    return REGISTRATION_INSTANCE;
  }

  public static GsonBuilder gsonBuilderWithAdapters() {
    return new GsonBuilder()
        .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
        .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
        .registerTypeAdapter(BlobId.class, new BlobIdTypeAdapter())
        .registerTypeAdapter(Date.class, new DateTypeAdapter())
        .registerTypeAdapter(Timestamp.class, new TimestampTypeAdapter())
        .registerTypeHierarchyAdapter(Throwable.class, new ThrowableTypeAdapter());
  }
}
