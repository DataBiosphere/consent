package org.broadinstitute.consent.http.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.JsonParseException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

public class CountryValidator {
  private static final String FILEPATH = "/src/main/resources/assets/ISO-3166-countries.json";
  private Set<String> countries;
  public CountryValidator() {
    try (FileReader reader = new FileReader(System.getProperty ("user.dir") + FILEPATH)) {
      Type type = new TypeToken<Set<String>>() {}.getType();
      countries = new Gson().fromJson(reader, type);
    } catch (JsonParseException | IOException e) {
      countries = new HashSet<>();
      throw new RuntimeException(e);
    }
  }

  public boolean isInCountryList(String country) {
    return countries.contains(country);
  }

}
