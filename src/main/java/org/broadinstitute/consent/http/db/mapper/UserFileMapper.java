package org.broadinstitute.consent.http.db.mapper;


import java.sql.ResultSet;
import java.sql.SQLException;

import org.broadinstitute.consent.http.enumeration.UserFileCategory;
import org.broadinstitute.consent.http.models.UserFile;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class UserFileMapper implements RowMapper<UserFile>, RowMapperHelper {
    @Override
    public UserFile map(ResultSet r, StatementContext statementContext) throws SQLException {
        UserFile file = new UserFile();
        if (hasColumn(r, "file_name")) {
            file.setFileName(r.getString("file_name"));
        }

        if (hasColumn(r, "user_file_id")) {
            file.setUserFileId(r.getInt("user_file_id"));
        }

        if (hasColumn(r, "entity_id")) {
            file.setEntityId(r.getString("entity_id"));
        }

        if (hasColumn(r, "bucket_name")) {
            file.setBucketName(r.getString("bucket_name"));
        }

        if (hasColumn(r, "blob_name")) {
            file.setBlobName(r.getString("blob_name"));
        }

        if (hasColumn(r, "category")) {
            try {
                file.setCategory(UserFileCategory.findValue(r.getString("category")));
            } catch(Exception e) {
                file.setCategory(null);
            }
        }

        if (hasColumn(r, "media_type")) {
            file.setMediaType(r.getString("media_type"));
        }

        if (hasColumn(r, "create_user_id")) {
            file.setCreateUserId(r.getInt("create_user_id"));
        }

        if (hasColumn(r, "create_date")) {
            file.setCreateDate(r.getDate("create_date"));
        }

        if (hasColumn(r, "deleted")) {
            file.setDeleted(r.getBoolean("deleted"));
        }

        return file;
    }
}
