package org.broadinstitute.consent.http.util;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import org.broadinstitute.consent.http.models.DataAccessRequest;

public class CountryValidator {
  private static final String FILEPATH = "assets/ISO-3166-countries.json";
  private Set<String> countries;

  // For the list of countries banned, see
  // https://www.ecfr.gov/current/title-28/chapter-I/part-202/subpart-F/section-202.601
  @VisibleForTesting
  protected static final Set<String> bannedCountriesISO3166 =
      Set.of(
          "china",
          "cuba",
          "hong kong",
          "iran (islamic republic of)",
          "korea (the democratic people's republic of)",
          "macao",
          "russian federation (the)",
          "venezuela (bolivarian republic of)");

  private static final Set<String> bannedCountriesCFR =
      Set.of(
          "china",
          "cuba",
          "hong kong",
          "iran",
          "macao",
          "macau",
          "north korea",
          "russia",
          "venezuela");

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

  public static boolean containsBannedCountry(DataAccessRequest dar) {
    return dar.getData().getLabAndInternalCollaborators().stream()
            .anyMatch(collaborator -> isInBannedCountryList(collaborator.countryOfOperation()))
        || isInBannedCountryList(dar.getData().getPiCountryOfOperation());
  }

  private static boolean isInBannedCountryList(String countryOfOperation) {
    if (countryOfOperation == null) return false;
    return bannedCountriesCFR.contains(countryOfOperation.trim().toLowerCase())
        || bannedCountriesISO3166.contains(countryOfOperation.trim().toLowerCase());
  }
}
