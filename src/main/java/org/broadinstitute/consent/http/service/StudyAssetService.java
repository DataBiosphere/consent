package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import java.util.List;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.StudyAssets;
import org.broadinstitute.consent.http.models.User;

public class StudyAssetService {

  private final DatasetService datasetService;
  private final StudyAssets studyAssets = new StudyAssets();

  @Inject
  public StudyAssetService(DatasetService datasetService) {
    this.datasetService = datasetService;
  }

  /**
   * Enforces the same read access StudyResource applies to the study itself: a study that is not
   * publicly visible is readable only by its creator, custodians, and admins. The shared gate reads
   * the study's own details, which carries the properties the asset lists live in.
   */
  private Study requireStudy(Integer studyId, User user) {
    return datasetService.requireReadableStudy(studyId, user);
  }

  /** Returns the registration assets of one type recorded for the study. */
  public List<Object> getAssetsByType(Integer studyId, User user, String key) {
    return studyAssets.findAssetList(requireStudy(studyId, user).getProperties(), key);
  }
}
