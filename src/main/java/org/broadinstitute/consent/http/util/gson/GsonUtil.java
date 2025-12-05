package org.broadinstitute.consent.http.util.gson;

import com.google.cloud.storage.BlobId;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import org.broadinstitute.consent.http.models.IntellectualProperty;
import org.broadinstitute.consent.http.models.Presentation;
import org.broadinstitute.consent.http.models.Publication;
import org.broadinstitute.consent.http.util.gson.deserializer.IntellectualPropertyDeserializer;
import org.broadinstitute.consent.http.util.gson.deserializer.PresentationDeserializer;
import org.broadinstitute.consent.http.util.gson.deserializer.PublicationDeserializer;

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
        .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
        .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
        .registerTypeAdapter(BlobId.class, new BlobIdTypeAdapter())
        .registerTypeAdapter(Date.class, new DateTypeAdapter())
        .registerTypeAdapter(Timestamp.class, new TimestampTypeAdapter())
        .registerTypeHierarchyAdapter(Throwable.class, new ThrowableTypeAdapter())
        .registerTypeAdapter(IntellectualProperty.class, new IntellectualPropertyDeserializer())
        .registerTypeAdapter(Publication.class, new PublicationDeserializer())
        .registerTypeAdapter(Presentation.class, new PresentationDeserializer());
  }
}
