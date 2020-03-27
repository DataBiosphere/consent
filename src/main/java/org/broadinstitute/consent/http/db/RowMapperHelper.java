package org.broadinstitute.consent.http.db;

import org.apache.commons.lang3.StringUtils;

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

    /**
     * JDBI3 does some string escaping when storing @Json data in postgres so it requires a bit of
     * processing to make it Gson-friendly:
     * <ul>
     * <li>Removes surrounding " literals
     * <li>Removes all of the prepended \" literals surrounding string keys and values
     * <li>Replaces \\" literals with single quotes
     * </p>
     *
     * @param json Unescaped jsonb data from postgres
     * @return Escaped json
     */
    default String escapeStoredJson(String json) {
        return StringUtils.
                stripEnd(StringUtils.stripStart(json, "\""), "\"").
                replace("\\\"", "\"").
                replace("\\\\\"", "'");
    }


}
