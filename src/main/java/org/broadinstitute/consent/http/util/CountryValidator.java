package org.broadinstitute.consent.http.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public class CountryValidator {
  private static final String FILEPATH = "assets/ISO-3166-countries.json";
  private Set<String> countries;
  public CountryValidator() {
    try (InputStream is = getClass().getClassLoader().getResourceAsStream(FILEPATH)) {
      if (is != null) {
        Type type = new TypeToken<Set<String>>() {}.getType();
        countries = new Gson().fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), type);
      }
    } catch (JsonParseException | IOException e) {
      countries = new HashSet<>();
      throw new IllegalStateException(e);
    }
  }

  public boolean isInCountryList(String country) {
    return countries.contains(country);
  }

}
