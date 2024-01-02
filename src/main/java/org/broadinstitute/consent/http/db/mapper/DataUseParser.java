package org.broadinstitute.consent.http.db.mapper;

import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.broadinstitute.consent.http.util.gson.GsonUtil;

public class DataUseParser implements ConsentLogger {

  private final Gson gson = GsonUtil.gsonBuilderWithAdapters().create();
  private final Map<String, DataUse> dataUseCache = new HashMap<>();

  public DataUse parseDataUse(String dataUseString) {
    dataUseCache.computeIfAbsent(dataUseString, s -> {
      try {
        return gson.fromJson(dataUseString, DataUse.class);
      } catch (Exception e) {
        logWarn(String.format("Unable to parse data use string: '%s'", dataUseString));
      }
      return null;
    });
    return dataUseCache.get(dataUseString);
  }

}
