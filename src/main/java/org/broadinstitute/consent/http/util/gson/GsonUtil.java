package org.broadinstitute.consent.http.util.gson;

import com.google.cloud.storage.BlobId;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;

public class GsonUtil {

  private static Gson instance;

  public static Gson getInstance() {
    if (Objects.isNull(instance)) {
      instance = buildGson();
    }
    return instance;
  }

  public static Gson buildGson() {
    return gsonBuilderWithAdapters().create();
  }

  public static Gson buildGsonNullSerializer() {
    return gsonBuilderWithAdapters().serializeNulls().create();
  }

  public static GsonBuilder gsonBuilderWithAdapters() {
    return new GsonBuilder()
        .registerTypeAdapter(
            Instant.class,
            new InstantTypeAdapter())
        .registerTypeAdapter(
            BlobId.class,
            new BlobIdTypeAdapter())
        .registerTypeAdapter(
            Date.class,
            new DateTypeAdapter())
        .registerTypeAdapter(
            Timestamp.class,
            new TimestampTypeAdapter())
        .registerTypeHierarchyAdapter(
            Throwable.class,
            new ThrowableTypeAdapter());
  }
}
