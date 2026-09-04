package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import java.util.List;
import org.broadinstitute.consent.http.db.StudyDAO;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.StudyAssets;
import org.broadinstitute.consent.http.models.User;
import org.jdbi.v3.core.Jdbi;

public class StudyAssetService {

  private final StudyDAO studyDAO;
  private final DatasetService datasetService;
  private final StudyAssets studyAssets = new StudyAssets();

  @Inject
  public StudyAssetService(Jdbi jdbi, DatasetService datasetService) {
    this.studyDAO = jdbi.onDemand(StudyDAO.class);
    this.datasetService = datasetService;
  }

  /**
   * Loads the study and enforces the same read access that StudyResource applies to the study
   * itself: a study that is not publicly visible is readable only by its creator, custodians, and
   * admins.
   */
  private Study requireStudy(Integer studyId, User user) {
    Study study = studyDAO.findStudyById(studyId);
    if (study == null) {
      throw new NotFoundException("Study not found");
    }
    return datasetService.verifyStudyVisibilityAccess(study, user);
  }

  /** Returns the registration assets of one type recorded for the study. */
  public List<Object> getAssetsByType(Integer studyId, User user, String key) {
    return studyAssets.findAssetList(requireStudy(studyId, user).getProperties(), key);
  }
}
