package org.broadinstitute.consent.http.models.draft;

public enum NihICsSupportingStudy {

  NCI("NCI"),
  NEI("NEI"),
  NHLBI("NHLBI"),
  NHGRI("NHGRI"),
  NIA("NIA"),
  NIAAA("NIAAA"),
  NIAID("NIAID"),
  NIAMS("NIAMS"),
  NIBIB("NIBIB"),
  NICHD("NICHD"),
  NIDCD("NIDCD"),
  NIDCR("NIDCR"),
  NIDDK("NIDDK"),
  NIDA("NIDA"),
  NIEHS("NIEHS"),
  NIGMS("NIGMS"),
  NIMH("NIMH"),
  NIMHD("NIMHD"),
  NINDS("NINDS"),
  NINR("NINR"),
  NLM("NLM"),
  CC("CC"),
  CIT("CIT"),
  CSR("CSR"),
  FIC("FIC"),
  NCATS("NCATS"),
  NCCIH("NCCIH");
  private final String value;

  NihICsSupportingStudy(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return this.value;
  }

  public String value() {
    return this.value;
  }

}
