package org.broadinstitute.consent.http.models;

import java.util.Set;

public class BannedCountriesList {
  // For the list of countries banned, see
  // https://www.ecfr.gov/current/title-28/chapter-I/part-202/subpart-F/section-202.601
  private BannedCountriesList() {}

  public static final Set<String> bannedCountriesISO3166 =
      Set.of(
          "china",
          "cuba",
          "iran, (islamic republic of)",
          "Korea (the democratic people's republic of)",
          "russian federation (the)",
          "venezuela, (bolivarian republic of)");
  public static final Set<String> bannedCountriesCFR =
      Set.of("china", "cuba", "iran", "north korea", "russia", "venezuela");
}
