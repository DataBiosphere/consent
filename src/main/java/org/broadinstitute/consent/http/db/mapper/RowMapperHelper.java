package org.broadinstitute.consent.http.db.mapper;

import com.google.gson.JsonSyntaxException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.text.StringEscapeUtils;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.jdbi.v3.core.result.RowView;

public interface RowMapperHelper extends ConsentLogger {

  @SuppressWarnings({"rawtypes", "unchecked"})
  /*
   * Utility method to check if a column exists in the row view or not.
   *
   * @param rowView The RowView
   * @param columnName The column name
   * @param clazz The class that corresponds to the column
   * @return True if the column is in the results, false otherwise
   */
  default boolean hasColumn(RowView rowView, String columnName, Class clazz) {
    try {
      return rowView.getColumn(columnName, clazz) != null;
    } catch (Exception e) {
      logDebug("RowView does not contain column " + columnName);
      return false;
    }
  }

  /*
   * Utility method to check if a column exists and has a non-zero value.
   *
   * @param rowView The RowView
   * @param columnName The column name
   * @return True if the column has non-zero results, false otherwise
   */
  default boolean hasNonZeroColumn(RowView rowView, String columnName) {
    try {
      return rowView.getColumn(columnName, Integer.class) != null && rowView.getColumn(columnName, Integer.class) > 0;
    } catch (Exception e) {
      logDebug("RowView does not contain column " + columnName);
      return false;
    }
  }

  /*
   * Utility method to check if a column exists in the row view or not.
   *
   * @param rowView The RowView
   * @param columnName The column name
   * @param clazz The class that corresponds to the column
   * @return Optional of requested class if the column is in the results, empty otherwise
   */
  default <T> Optional<T> hasOptionalColumn(RowView rowView, String columnName, Class<T> clazz) {
    try {
      return Optional.of(rowView.getColumn(columnName, clazz));
    } catch (Exception e) {
      logDebug(String.format("RowView does not contain column %s", columnName));
      return Optional.empty();
    }
  }

  /**
   * Utility method to check if a column exists in the result set or not.
   *
   * @param rs         The ResultSet
   * @param columnName The column name
   * @return True if column name exists, false otherwise
   * @throws SQLException The exception
   */
  default boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
    ResultSetMetaData rsmd = rs.getMetaData();
    int columns = rsmd.getColumnCount();
    for (int x = 1; x <= columns; x++) {
      // postgres -> case insensitive columns
      if (columnName.equalsIgnoreCase(rsmd.getColumnName(x))) {
        return true;
      }
    }
    return false;
  }

  /*
   * Utility method to check if a column exists and has a non-zero value.
   *
   * @param rowView The RowView
   * @param columnName The column name
   * @return True if the column has non-zero results, false otherwise
   */
  default boolean hasNonZeroColumn(ResultSet rs, String columnName) throws SQLException {
    ResultSetMetaData rsmd = rs.getMetaData();
    int columns = rsmd.getColumnCount();
    for (int x = 1; x <= columns; x++) {
      // postgres -> case insensitive columns
      if (columnName.equalsIgnoreCase(rsmd.getColumnName(x))) {
        return rs.getInt(columnName) > 0;
      }
    }
    return false;
  }


  static String unescapeJava(String value) {
    return StringEscapeUtils.unescapeJava(StringEscapeUtils.unescapeJava(value));
  }

  default DataAccessRequestData translate(String darDataString) {
    DataAccessRequestData data = null;
    if (Objects.nonNull(darDataString)) {
      // Handle nested quotes
      String quoteFixedDataString = darDataString.replaceAll("\\\\\"", "'");
      // Inserted json data ends up double-escaped via standard jdbi insert.
      String escapedDataString = unescapeJava(quoteFixedDataString);
      try {
        data = DataAccessRequestData.fromString(escapedDataString);
      } catch (JsonSyntaxException | NullPointerException e) {
        String message = "Unable to parse Data Access Request; error: " + e.getMessage();
        logDebug(message);
        throw e;
      }
    }
    return data;
  }

}
