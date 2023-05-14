package org.broadinstitute.consent.http.db.mapper;

import org.broadinstitute.consent.http.models.FileStorageObject;
import org.jdbi.v3.core.mapper.RowMapper;

public class FileStorageObjectMapperWithFSOPrefix extends FileStorageObjectMapper implements
    RowMapper<FileStorageObject>, RowMapperHelper {

  @Override
  public String getPrefix() {
    return "fso_";
  }
}
