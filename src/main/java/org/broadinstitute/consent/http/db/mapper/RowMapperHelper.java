package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Objects;

import com.google.gson.JsonSyntaxException;
import org.apache.commons.text.StringEscapeUtils;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface RowMapperHelper {

  Logger log = LoggerFactory.getLogger(RowMapperHelper.class);

  /**
   * Utility method to check if a column exists in the result set or not.
   *
   * @param rs The ResultSet
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

  static String unescapeJava(String value) {
    return StringEscapeUtils.unescapeJava(StringEscapeUtils.unescapeJava(value));
  }

  static DataAccessRequestData translate(String darDataString) {
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
        log.error(message);
        throw e;
      }
    }
    return data;
  }

}
