package org.broadinstitute.consent.http.models;

import java.util.Calendar;
import java.util.Objects;

/**
 * Utility Interface Trait for writing out Summary Details 
 */
public interface SummaryDetail {

  String headers();
  
  @Override
  String toString();

  default String delimiterCheck(String delimitedString) {
    String textDelimiter = "\"";
    if (!delimitedString.isBlank()) {
      return textDelimiter + delimitedString.replaceAll(textDelimiter, "'") + textDelimiter;
    } else {
      return "";
    }
  }

  default String booleanToString(Boolean b) {
    if (Objects.nonNull(b)) {
      return b ? "YES" : "NO";
    }
    return "-";
  }

  default String formatLongToDate(long time) {
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(time);
    int day = cal.get(Calendar.DAY_OF_MONTH);
    int month = cal.get(Calendar.MONTH) + 1;
    int year = cal.get(Calendar.YEAR);
    return String.format("%d/%d/%d", month, day, year);
  }

  default String nullToString(String b) {
    return Objects.nonNull(b) && !b.isEmpty() ? b : "-";
  }
}
