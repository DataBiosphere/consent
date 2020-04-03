package org.broadinstitute.consent.http.db;

import org.apache.commons.text.StringEscapeUtils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public interface RowMapperHelper {

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

    default String unescapeJava(String value) {
        return StringEscapeUtils.unescapeJava(
                StringEscapeUtils.unescapeJava(value));
    }

}
