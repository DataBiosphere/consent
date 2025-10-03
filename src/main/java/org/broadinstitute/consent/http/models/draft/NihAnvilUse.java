package org.broadinstitute.consent.http.models.draft;

public enum NihAnvilUse {

  I_AM_NHGRI_FUNDED_AND_I_HAVE_A_DB_GA_P_PHS_ID_ALREADY(
      "I am NHGRI funded and I have a dbGaP PHS ID already"),
  I_AM_NHGRI_FUNDED_AND_I_DO_NOT_HAVE_A_DB_GA_P_PHS_ID(
      "I am NHGRI funded and I do not have a dbGaP PHS ID"),
  I_AM_NOT_NHGRI_FUNDED_BUT_I_AM_SEEKING_TO_SUBMIT_DATA_TO_AN_VIL(
      "I am not NHGRI funded but I am seeking to submit data to AnVIL"),
  I_AM_NOT_NHGRI_FUNDED_AND_DO_NOT_PLAN_TO_STORE_DATA_IN_AN_VIL(
      "I am not NHGRI funded and do not plan to store data in AnVIL");
  private final String value;

  @Override
  public String toString() {
    return this.value;
  }

  NihAnvilUse(String value) {
    this.value = value;
  }

}
