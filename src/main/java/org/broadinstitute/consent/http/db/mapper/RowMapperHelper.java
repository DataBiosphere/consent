package org.broadinstitute.consent.http.db.mapper;

import com.google.gson.JsonSyntaxException;
import org.apache.commons.text.StringEscapeUtils;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.jdbi.v3.core.result.RowView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Objects;

public interface RowMapperHelper {

    Logger log = LoggerFactory.getLogger(RowMapperHelper.class);

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
            rowView.getColumn(columnName, clazz);
            return true;
        } catch (Exception e) {
            log.debug("RowView does not contain column " + columnName);
            return false;
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
                log.error(message);
                throw e;
            }
        }
        return data;
    }

}
