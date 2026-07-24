package org.broadinstitute.consent.http.db.mapper;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.broadinstitute.consent.http.util.gson.GsonUtil;

public class DataUseParser implements ConsentLogger {

  private final Gson gson = GsonUtil.gsonBuilderWithAdapters().create();
  private final ConcurrentMap<String, DataUse> dataUseCache = new ConcurrentHashMap<>();

  public DataUse parseDataUse(String dataUseString) {
    if (null == dataUseString || dataUseString.isEmpty()) {
      return null;
    }
    return dataUseCache.computeIfAbsent(
        dataUseString,
        s -> {
          try {
            return gson.fromJson(dataUseString, DataUse.class);
          } catch (Exception _) {
            String hashPrefix = Hashing.sha256().hashString(s, UTF_8).toString().substring(0, 12);
            logWarn(
                "Unable to parse data use string (length=%d, sha256Prefix=%s)"
                    .formatted(s.length(), hashPrefix));
          }
          return null;
        });
  }
}
