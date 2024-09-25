package org.broadinstitute.consent.http.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.broadinstitute.consent.http.db.DraftSubmissionDAO;
import org.broadinstitute.consent.http.models.DraftSubmission;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.jdbi.v3.core.Jdbi;

import com.google.inject.Inject;

public class DraftSubmissionService implements ConsentLogger {
    private final Jdbi jdbi;
    private final DraftSubmissionDAO draftSubmissionDAO;
    private final FileStorageObjectService fileStorageObjectService;
    
    @Inject
    public DraftSubmissionService(Jdbi jdbi, DraftSubmissionDAO draftSubmissionDAO, FileStorageObjectService fileStorageObjectService) {
        this.jdbi = jdbi;
        this.draftSubmissionDAO = draftSubmissionDAO;
        this.fileStorageObjectService = fileStorageObjectService;
    }

    public void insertDraftSubmission(DraftSubmission draft) throws SQLException {
        jdbi.useHandle(handle -> {
            handle.getConnection().setAutoCommit(false);
            try {
              List<FileStorageObject> uploadedFiles = new ArrayList<>();
              Integer draftId = draftSubmissionDAO
                  .insert(draft.getName(), draft.getCreateDate().toInstant(), draft.getCreateUser().getUserId(), draft.getJson(), draft.getUUID(), draft.getClass().getName());
              if (draft.getFiles().isPresent()){
                draft.getFiles().get().forEach((String key, FormDataBodyPart file) -> {
                  //todo: upload the files and insert them keyed to the draftId.

                });
              }
            } catch (Exception e) {
              handle.rollback();
            }
            handle.commit();
        });
    }
}