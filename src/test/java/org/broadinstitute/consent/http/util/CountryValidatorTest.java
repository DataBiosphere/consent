package org.broadinstitute.consent.http.util;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.broadinstitute.consent.http.models.Collaborator;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.junit.jupiter.api.Test;

class CountryValidatorTest {
  private final CountryValidator countryValidator = new CountryValidator();

  @Test
  void testIsInCountryListInvalid() {
    assertFalse(countryValidator.isInCountryList("US"));
  }

  @Test
  void testIsInCountryListValid() {
    assertTrue(countryValidator.isInCountryList("United States of America (the)"));
  }

  @Test
  void testIsInBannedCountryList_PI() {
    DataAccessRequest dar = new DataAccessRequest();
    DataAccessRequestData darData = new DataAccessRequestData();
    darData.setPiCountryOfOperation("Hong Kong");
    dar.setData(darData);
    assertTrue(CountryValidator.containsBannedCountry(dar));
  }

  @Test
  void testIsInBannedCountry_lab_staff() {
    DataAccessRequest dar = new DataAccessRequest();
    DataAccessRequestData darData = new DataAccessRequestData();
    darData.setLabCollaborators(
        List.of(new Collaborator(true, "test@test.com", "test", "Test", "Test", "12345", "Cuba")));
    dar.setData(darData);
    assertTrue(CountryValidator.containsBannedCountry(dar));
  }

  @Test
  void testIsInBannedCountry_internal_collaborator() {
    DataAccessRequest dar = new DataAccessRequest();
    DataAccessRequestData darData = new DataAccessRequestData();
    darData.setInternalCollaborators(
        List.of(
            new Collaborator(
                true,
                "test@test.com",
                "test",
                "Test",
                "Test",
                "12345",
                "Russian Federation (the)")));
    dar.setData(darData);
    assertTrue(CountryValidator.containsBannedCountry(dar));
  }

  @Test
  void testIsNotInBannedCountry_internal_collaborator() {
    DataAccessRequest dar = new DataAccessRequest();
    DataAccessRequestData darData = new DataAccessRequestData();
    darData.setInternalCollaborators(
        List.of(
            new Collaborator(true, "test@test.com", "test", "Test", "Test", "12345", "Genovia")));
    dar.setData(darData);
    assertFalse(CountryValidator.containsBannedCountry(dar));
  }

  @Test
  void testIsNotInBannedCountry_lab_staff() {
    DataAccessRequest dar = new DataAccessRequest();
    DataAccessRequestData darData = new DataAccessRequestData();
    darData.setLabCollaborators(
        List.of(
            new Collaborator(true, "test@test.com", "test", "Test", "Test", "12345", "Genovia")));
    dar.setData(darData);
    assertFalse(CountryValidator.containsBannedCountry(dar));
  }

  @Test
  void testIsNotInBannedCountry_PI() {
    DataAccessRequest dar = new DataAccessRequest();
    DataAccessRequestData darData = new DataAccessRequestData();
    darData.setPiCountryOfOperation("Genovia");
    dar.setData(darData);
    assertFalse(CountryValidator.containsBannedCountry(dar));
  }

  @Test
  void testBannedCountryListIsValid() throws IOException {
    String fileContents =
        Files.readString(Path.of("src/main/resources/assets/ISO-3166-countries.json"));
    CountryValidator.bannedCountriesISO3166.forEach(
        country -> {
          assertThat(fileContents.toLowerCase(), containsString(country));
        });
  }
}
