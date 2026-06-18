package org.broadinstitute.consent.http.util;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import java.util.List;
import org.apache.commons.validator.routines.DomainValidator;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.util.gson.GsonUtil;

public class InstitutionUtil implements ConsentLogger {

  private final GsonBuilder gson;

  @Inject
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

        return !isAdmin
            && !(fieldName.equals("id")
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
   * Canonicalizes an institution name by normalizing quotes and trimming whitespace.
   *
   * @param name The institution name to canonicalize
   * @return The canonicalized name
   */
  public static String canonicalizeInstitutionName(String name) {
    String canonicalized = name.trim();

    // Replace the following characters with a single quote:
    // u201C (Left double quotation)
    // u201D (Right double quotation)
    // u2018 (Left single quotation)
    // u2019 (Right single quotation)
    // u201A (Single low-9 quotation)
    // u201E (Double low-9 quotation)
    // "     (Straight double quotation)
    canonicalized = canonicalized.replaceAll("[\u201C\u201D\u2018\u2019\u201A\u201E\"]", "'");

    return canonicalized;
  }

  /**
   * Validates all domains in this institution's domain list and returns invalid ones.
   *
   * @param institution The institution to validate domains for.
   * @return List of invalid domains.
   */
  protected static List<String> getInvalidInstitutionDomains(Institution institution) {
    if (institution.getDomains() == null) {
      return List.of();
    }

    DomainValidator validator = DomainValidator.getInstance();

    return institution.getDomains().stream().filter(domain -> !validator.isValid(domain)).toList();
  }

  public static void validateInstitutionDomains(Institution institution) {
    List<String> invalidDomains = getInvalidInstitutionDomains(institution);
    if (!invalidDomains.isEmpty()) {
      throw new BadRequestException(
          "Invalid domain(s) provided for institution: " + String.join(", ", invalidDomains));
    }
  }
}
