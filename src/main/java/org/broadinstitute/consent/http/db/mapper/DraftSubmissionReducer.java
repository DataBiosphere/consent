package org.broadinstitute.consent.http.db.mapper;

import java.util.Map;
import java.util.UUID;
import org.broadinstitute.consent.http.models.DraftSubmissionInterface;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;

public class DraftSubmissionReducer implements
    LinkedHashMapRowReducer<UUID, DraftSubmissionInterface>, RowMapperHelper {

  @Override
  public void accumulate(Map<UUID, DraftSubmissionInterface> map, RowView rowView) {
    DraftSubmissionInterface draftSubmission = map.computeIfAbsent(
        rowView.getColumn("uuid", UUID.class),
        id -> rowView.getRow(DraftSubmissionInterface.class));

    if (hasNonZeroColumn(rowView, "fso_file_storage_object_id")) {
      FileStorageObject fileStorageObject = rowView.getRow(FileStorageObject.class);
      draftSubmission.addStoredFile(fileStorageObject);
    }
  }

}
