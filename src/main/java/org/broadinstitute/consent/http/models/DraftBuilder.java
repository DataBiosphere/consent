package org.broadinstitute.consent.http.models;

import org.broadinstitute.consent.http.enumeration.DraftType;

public class DraftBuilder {

  public static DraftInterface from(DraftType draftType) {
    //Expecting new draft types to be implemented later, leaving as switch for pattern.
    return switch (draftType) {
      case STUDY_DATASET_SUBMISSION_V1 -> new DraftStudyDataset();
    };
  }
}
