package org.broadinstitute.consent.http.service.dao;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import java.sql.SQLException;
import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.models.DraftSubmission;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.DraftSubmissionService;
import org.broadinstitute.consent.http.service.FileStorageObjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DraftSubmissionServiceTest extends DAOTestHelper {

  DraftSubmissionService draftSubmissionService;

  FileStorageObjectService  fileStorageObjectService;

  @BeforeEach
  public void setUp() {
    draftSubmissionService = new DraftSubmissionService(jdbi, draftSubmissionDAO, fileStorageObjectService);
  }

  @Test
  public void testCreateDraftSubmission() throws SQLException {
    User user = createUser();
    DraftSubmission draft = new DraftSubmission("{}", null, user);
    draftSubmissionService.insertDraftSubmission(draft);
    assertThat(draftSubmissionDAO.findDraftSubmissionsByUserId(user.getUserId()), hasSize(1));
  }

}
