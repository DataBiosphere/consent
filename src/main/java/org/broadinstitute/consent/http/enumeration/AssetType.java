package org.broadinstitute.consent.http.enumeration;

import java.util.Arrays;
import java.util.Optional;

/** A requestable asset type and the letter its DUOS identifier carries. */
public enum AssetType {
  // Datasets keep their letterless DUOS-000123 form. D and S already mean a dataset database id and
  // a study id in duos-ui links, so no type may use them.
  DATASET(null),
  MODEL("M"),
  WORKSPACE("W"),
  BIOSPECIMEN("B");

  private final String letter;

  AssetType(String letter) {
    this.letter = letter;
  }

  public Optional<String> letter() {
    return Optional.ofNullable(letter);
  }

  public static Optional<AssetType> fromLetter(String letter) {
    return Arrays.stream(values())
        .filter(t -> t.letter != null && t.letter.equals(letter))
        .findFirst();
  }
}
