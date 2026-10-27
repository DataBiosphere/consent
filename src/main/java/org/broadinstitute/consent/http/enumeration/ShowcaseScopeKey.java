package org.broadinstitute.consent.http.enumeration;

/**
 * The {@code scope} vocabulary for Data Showcase lookups. Each key filters on one database column.
 * A scope with several keys matches rows that satisfy every key; the values of one key are
 * alternatives.
 */
public enum ShowcaseScopeKey {
  /** DAC names, matched exactly against {@code dac.name}. */
  DAC("dac", "dac.name", "string");

  private final String jsonName;
  private final String column;
  private final String valueType;

  ShowcaseScopeKey(String jsonName, String column, String valueType) {
    this.jsonName = jsonName;
    this.column = column;
    this.valueType = valueType;
  }

  public String getJsonName() {
    return jsonName;
  }

  public String getColumn() {
    return column;
  }

  public String getValueType() {
    return valueType;
  }
}
