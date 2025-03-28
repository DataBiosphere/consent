package org.broadinstitute.consent.http.rules;

import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GeneralResearchUseV1Test {

  @Test
  void testCompareSuccess() {
    Dataset dataset = new Dataset();
    dataset.setDataUse(new DataUseBuilder().setGeneralUse(true).build());

    DataAccessRequestData data = new DataAccessRequestData();
    data.setHmb(true);
    DataAccessRequest dataAccessRequest = new DataAccessRequest();
    dataAccessRequest.setData(data);

    compare(dataset, dataAccessRequest, true);
  }

  @Test
  void testCompareDatasetNotGRU() {
    Dataset dataset = new Dataset();
    dataset.setDataUse(new DataUseBuilder().setHmbResearch(true).build());

    DataAccessRequestData data = new DataAccessRequestData();
    data.setHmb(true);
    DataAccessRequest dataAccessRequest = new DataAccessRequest();
    dataAccessRequest.setData(data);

    compare(dataset, dataAccessRequest, false);
  }

  @Test
  void testCompareDARNotHmb_1() {
    Dataset dataset = new Dataset();
    dataset.setDataUse(new DataUseBuilder().setGeneralUse(true).build());

    DataAccessRequestData data = new DataAccessRequestData();
    data.setDiseases(true);
    DataAccessRequest dataAccessRequest = new DataAccessRequest();
    dataAccessRequest.setData(data);

    compare(dataset, dataAccessRequest, false);
  }

  @Test
  void testCompareDARNotHmb_2() {
    Dataset dataset = new Dataset();
    dataset.setDataUse(new DataUseBuilder().setGeneralUse(true).build());

    DataAccessRequestData data = new DataAccessRequestData();
    data.setOther(true);
    DataAccessRequest dataAccessRequest = new DataAccessRequest();
    dataAccessRequest.setData(data);

    compare(dataset, dataAccessRequest, false);
  }

  @Test
  void testCompareDARNotHmb_3() {
    Dataset dataset = new Dataset();
    dataset.setDataUse(new DataUseBuilder().setGeneralUse(true).build());

    DataAccessRequestData data = new DataAccessRequestData();
    data.setOtherText("Other Condition");
    DataAccessRequest dataAccessRequest = new DataAccessRequest();
    dataAccessRequest.setData(data);

    compare(dataset, dataAccessRequest, false);
  }

  @Test
  void testCompareDARNotHmb_4() {
    Dataset dataset = new Dataset();
    dataset.setDataUse(new DataUseBuilder().setGeneralUse(true).build());

    DataAccessRequestData data = new DataAccessRequestData();
    data.setControls(true);
    DataAccessRequest dataAccessRequest = new DataAccessRequest();
    dataAccessRequest.setData(data);

    compare(dataset, dataAccessRequest, false);
  }

  @Test
  void testCompareDARNotHmb_5() {
    Dataset dataset = new Dataset();
    dataset.setDataUse(new DataUseBuilder().setGeneralUse(true).build());

    DataAccessRequestData data = new DataAccessRequestData();
    data.setPopulation(true);
    DataAccessRequest dataAccessRequest = new DataAccessRequest();
    dataAccessRequest.setData(data);

    compare(dataset, dataAccessRequest, false);
  }

  @Test
  void testCompareDARNotHmb_6() {
    Dataset dataset = new Dataset();
    dataset.setDataUse(new DataUseBuilder().setGeneralUse(true).build());

    DataAccessRequestData data = new DataAccessRequestData();
    data.setForProfit(true);
    DataAccessRequest dataAccessRequest = new DataAccessRequest();
    dataAccessRequest.setData(data);

    compare(dataset, dataAccessRequest, false);
  }

  @Test
  void testCompareDARNotHmb_7() {
    Dataset dataset = new Dataset();
    dataset.setDataUse(new DataUseBuilder().setGeneralUse(true).build());

    DataAccessRequestData data = new DataAccessRequestData();
    data.setPediatric(true);
    DataAccessRequest dataAccessRequest = new DataAccessRequest();
    dataAccessRequest.setData(data);

    compare(dataset, dataAccessRequest, false);
  }

  @Test
  void testCompareDARNotHmb_8() {
    Dataset dataset = new Dataset();
    dataset.setDataUse(new DataUseBuilder().setGeneralUse(true).build());

    DataAccessRequestData data = new DataAccessRequestData();
    data.setVulnerablePopulation(true);
    DataAccessRequest dataAccessRequest = new DataAccessRequest();
    dataAccessRequest.setData(data);

    compare(dataset, dataAccessRequest, false);
  }

  @Test
  void testCompareDARNotHmb_9() {
    Dataset dataset = new Dataset();
    dataset.setDataUse(new DataUseBuilder().setGeneralUse(true).build());

    DataAccessRequestData data = new DataAccessRequestData();
    data.setIllegalBehavior(true);
    DataAccessRequest dataAccessRequest = new DataAccessRequest();
    dataAccessRequest.setData(data);

    compare(dataset, dataAccessRequest, false);
  }

  @Test
  void testCompareDARNotHmb_10() {
    Dataset dataset = new Dataset();
    dataset.setDataUse(new DataUseBuilder().setGeneralUse(true).build());

    DataAccessRequestData data = new DataAccessRequestData();
    data.setSexualDiseases(true);
    DataAccessRequest dataAccessRequest = new DataAccessRequest();
    dataAccessRequest.setData(data);

    compare(dataset, dataAccessRequest, false);
  }

  @Test
  void testCompareDARNotHmb_11() {
    Dataset dataset = new Dataset();
    dataset.setDataUse(new DataUseBuilder().setGeneralUse(true).build());

    DataAccessRequestData data = new DataAccessRequestData();
    data.setPsychiatricTraits(true);
    DataAccessRequest dataAccessRequest = new DataAccessRequest();
    dataAccessRequest.setData(data);

    compare(dataset, dataAccessRequest, false);
  }

  @Test
  void testCompareDARNotHmb_12() {
    Dataset dataset = new Dataset();
    dataset.setDataUse(new DataUseBuilder().setGeneralUse(true).build());

    DataAccessRequestData data = new DataAccessRequestData();
    data.setNotHealth(true);
    DataAccessRequest dataAccessRequest = new DataAccessRequest();
    dataAccessRequest.setData(data);

    compare(dataset, dataAccessRequest, false);
  }

  @Test
  void testCompareDARNotHmb_13() {
    Dataset dataset = new Dataset();
    dataset.setDataUse(new DataUseBuilder().setGeneralUse(true).build());

    DataAccessRequestData data = new DataAccessRequestData();
    data.setStigmatizedDiseases(true);
    DataAccessRequest dataAccessRequest = new DataAccessRequest();
    dataAccessRequest.setData(data);

    compare(dataset, dataAccessRequest, false);
  }

  @Test
  void testCompareDARNotHmb_14() {
    Dataset dataset = new Dataset();
    dataset.setDataUse(new DataUseBuilder().setGeneralUse(true).build());

    DataAccessRequestData data = new DataAccessRequestData();
    data.setAddiction(true);
    DataAccessRequest dataAccessRequest = new DataAccessRequest();
    dataAccessRequest.setData(data);

    compare(dataset, dataAccessRequest, false);
  }

  private void compare(Dataset dataset, DataAccessRequest dataAccessRequest, boolean expected) {
    GeneralResearchUseV1 rule = new GeneralResearchUseV1();
    boolean result = rule.compare(dataset, dataAccessRequest);
    Assertions.assertEquals(expected, result);
  }

}
