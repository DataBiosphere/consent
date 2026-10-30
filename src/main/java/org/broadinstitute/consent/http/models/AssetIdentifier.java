package org.broadinstitute.consent.http.models;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.enumeration.AssetType;

/**
 * A DUOS asset identifier: {@code DUOS-000123} for a dataset, {@code DUOS-M-000045} for a model,
 * and so on. The number is zero-padded to six digits and grows past them.
 */
public record AssetIdentifier(AssetType type, int number) {

  private static final String PREFIX = "DUOS-";
  private static final Pattern FORM = Pattern.compile("DUOS-(?:([A-Z])-)?(\\d{6,10})");

  public AssetIdentifier {
    Objects.requireNonNull(type, "type");
    if (number < 0) {
      throw new IllegalArgumentException("An asset number cannot be negative.");
    }
  }

  public static String format(AssetType type, int number) {
    return new AssetIdentifier(type, number).format();
  }

  public String format() {
    String letter = type.letter().map(l -> l + "-").orElse("");
    return PREFIX + letter + StringUtils.leftPad(Integer.toString(number), 6, "0");
  }

  /** Accepts only the form {@link #format} produces, so one asset has exactly one identifier. */
  public static AssetIdentifier parse(String text) {
    Matcher matcher = FORM.matcher(Objects.requireNonNullElse(text, ""));
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Not a DUOS asset identifier.");
    }
    AssetType type =
        matcher.group(1) == null
            ? AssetType.DATASET
            : AssetType.fromLetter(matcher.group(1))
                .orElseThrow(() -> new IllegalArgumentException("Unknown asset type letter."));
    long number = Long.parseLong(matcher.group(2));
    if (number > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("Asset number is out of range.");
    }
    AssetIdentifier identifier = new AssetIdentifier(type, (int) number);
    if (!identifier.format().equals(text)) {
      throw new IllegalArgumentException("Asset identifier is not in its canonical form.");
    }
    return identifier;
  }
}
