package org.broadinstitute.consent.http.util;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.ws.rs.BadRequestException;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.DomainValidator;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.util.gson.GsonUtil;

public class InstitutionUtil implements ConsentLogger {

  private final GsonBuilder gson;

  public InstitutionUtil() {
    this.gson = GsonUtil.gsonBuilderWithAdapters();
  }

  // Gson builder and exclusion strategy helpers
  // Opting to not null values, null has the implication of an absence of value
  // Whereas the absence of a field can mean an absence of value OR an omission of data

  public Gson getGsonBuilder(Boolean isAdmin) {
    ExclusionStrategy strategy = getSerializationExclusionStrategy(isAdmin);
    return gson.addSerializationExclusionStrategy(strategy).create();
  }

  private ExclusionStrategy getSerializationExclusionStrategy(Boolean isAdmin) {
    return new ExclusionStrategy() {
      @Override
      public boolean shouldSkipField(FieldAttributes field) {
        String fieldName = field.getName();

        return !isAdmin && !(fieldName.equals("id")
            || fieldName.equals("name")
            || fieldName.equals("signingOfficials")
            || fieldName.equals("displayName")
            || fieldName.equals("userId")
            || fieldName.equals("email")
            || fieldName.equals("itDirectorName")
            || fieldName.equals("itDirectorEmail")
            || fieldName.equals("institutionUrl")
            || fieldName.equals("dunsNumber")
            || fieldName.equals("orgChartUrl")
            || fieldName.equals("verificationUrl")
            || fieldName.equals("verificationFilename")
            || fieldName.equals("organizationType")
            || fieldName.equals("domains"));
      }

      // NOTE: shouldSkipClass is mandatory when creating an ExclusionStrategy
      // No reason to skip class (only dealing with Institution), so you can just
      // return false here
      @Override
      public boolean shouldSkipClass(Class<?> c) {
        return false;
      }
    };
  }

  /**
   * Canonicalizes an institution name by normalizing quotes and handling UTF-8 characters.
   * - Replaces curly quotes with straight quotes
   * - Replaces double quotes with single quotes
   * - Handles UTF-8 characters properly
   * - Name is required (cannot be null or blank)
   *
   * @param name The institution name to canonicalize
   * @return The canonicalized name, or null if input is invalid
   */
  public static String canonicalizeInstitutionName(String name) {
    // Validate that name is not null or blank
    if (StringUtils.isBlank(name)) {
      return null;
    }

    String canonicalized = name.trim();

    // Replace curly/smart quotes with straight quotes
    canonicalized = canonicalized
        .replace("\u201C", "'")  // Left double quotation mark
        .replace("\u201D", "'")  // Right double quotation mark
        .replace("\u2018", "'")  // Left single quotation mark
        .replace("\u2019", "'")  // Right single quotation mark
        .replace("\u201A", "'")  // Single low-9 quotation mark
        .replace("\u201E", "'"); // Double low-9 quotation mark

    // Replace double quotes with single quotes
    canonicalized = canonicalized.replace("\"", "'");

    return canonicalized;
  }

  /**
   * Validates that a given domain is valid
   *
   * @param domain The domain string to validate
   * @return true if the domain is valid, false otherwise
   */
  protected static boolean isValidInstitutionDomain(String domain) {
    // Validate that the domain is not null or empty
    if (StringUtils.isBlank(domain)) {
      return false;
    }

    // Validate the domain format
    DomainValidator validator = DomainValidator.getInstance();
    return validator.isValid(domain);
  }

  /**
   * Validates all domains in this institution's domain list and returns invalid ones.
   *
   * @param institution The institution to validate domains for.
   * @return List of invalid domains.
   */
  public static List<String> getInvalidInstitutionDomains(Institution institution) {
    //TODO: also check for duplicates in the domain list
    return institution.getDomains().stream()
        .filter(domain -> !isValidInstitutionDomain(domain))
        .collect(java.util.stream.Collectors.toList());
  }

  public static void validateInstitutionDomains(Institution institution) {
    List<String> invalidDomains = getInvalidInstitutionDomains(institution);
    if (!invalidDomains.isEmpty()) {
      throw new BadRequestException(
          "Invalid domain(s) provided for institution: " + String.join(", ", invalidDomains));
    }
  }
}
