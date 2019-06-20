package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.cloudstore.GCSStore;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.service.AbstractConsentAPI;
import org.broadinstitute.consent.http.service.AbstractDataAccessRequestAPI;
import org.broadinstitute.consent.http.service.AbstractDataSetAPI;
import org.broadinstitute.consent.http.service.AbstractEmailNotifierAPI;
import org.broadinstitute.consent.http.service.AbstractMatchProcessAPI;
import org.broadinstitute.consent.http.service.AbstractTranslateService;
import org.broadinstitute.consent.http.service.ConsentAPI;
import org.broadinstitute.consent.http.service.DataAccessRequestAPI;
import org.broadinstitute.consent.http.service.DataSetAPI;
import org.broadinstitute.consent.http.service.ElectionAPI;
import org.broadinstitute.consent.http.service.users.DACUserAPI;
import org.broadinstitute.consent.http.service.validate.AbstractUseRestrictionValidatorAPI;
import org.broadinstitute.consent.http.util.DarConstants;
import org.broadinstitute.consent.http.util.DarUtil;
import org.bson.Document;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.NotFoundException;
import java.util.Collections;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        AbstractDataAccessRequestAPI.class,
        AbstractConsentAPI.class,
        AbstractMatchProcessAPI.class,
        AbstractEmailNotifierAPI.class,
        AbstractUseRestrictionValidatorAPI.class,
        AbstractTranslateService.class,
        AbstractDataSetAPI.class
})
public class DataAccessRequestResourceUnitTest {

    @Mock
    DACUserAPI dacUserAPI;

    @Mock
    ElectionAPI electionAPI;

    @Mock
    GCSStore store;

    @Mock
    ConsentAPI consentAPI;

    @Mock
    DataAccessRequestAPI dataAccessRequestAPI;

    @Mock
    DataSetAPI dataSetAPI;

    private DataAccessRequestResource resource;
    private Document dar;
    private String darId;
    private DataSet dataSet;

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(AbstractDataAccessRequestAPI.class);
        PowerMockito.mockStatic(AbstractConsentAPI.class);
        PowerMockito.mockStatic(AbstractMatchProcessAPI.class);
        PowerMockito.mockStatic(AbstractEmailNotifierAPI.class);
        PowerMockito.mockStatic(AbstractUseRestrictionValidatorAPI.class);
        PowerMockito.mockStatic(AbstractTranslateService.class);
        PowerMockito.mockStatic(AbstractDataSetAPI.class);


    }

    /**
     * Positive case where a DAR references a numeric dataset id
     */
    @Test
    public void testDescribeConsentForDAR_case1() throws Exception {
        dar = DataRequestSamplesHolder.getSampleDar();
        darId = DarUtil.getObjectIdFromDocument(dar).toHexString();
        when(dataAccessRequestAPI.describeDataAccessRequestFieldsById(any(), any())).thenReturn(dar);
        when(consentAPI.getConsentFromDatasetID(any())).thenReturn(new Consent());
        when(AbstractDataAccessRequestAPI.getInstance()).thenReturn(dataAccessRequestAPI);
        when(AbstractConsentAPI.getInstance()).thenReturn(consentAPI);
        when(AbstractDataSetAPI.getInstance()).thenReturn(dataSetAPI);
        resource = new DataAccessRequestResource(dacUserAPI, electionAPI, store);
        Consent consent = resource.describeConsentForDAR(darId);
        assertNotNull(consent);
    }

    /**
     * Positive case where a DAR references a string dataset id
     */
    @Test
    public void testDescribeConsentForDAR_case2() throws Exception {
        dar = DataRequestSamplesHolder.getSampleDar();
        dar.put(DarConstants.DATASET_ID, Collections.singletonList("SC-12345"));
        darId = DarUtil.getObjectIdFromDocument(dar).toHexString();
        dataSet = new DataSet();
        dataSet.setDataSetId(1);
        when(dataAccessRequestAPI.describeDataAccessRequestFieldsById(any(), any())).thenReturn(dar);
        when(consentAPI.getConsentFromDatasetID(any())).thenReturn(new Consent());
        when(dataSetAPI.findDataSetByObjectId(any())).thenReturn(dataSet);
        when(AbstractDataAccessRequestAPI.getInstance()).thenReturn(dataAccessRequestAPI);
        when(AbstractConsentAPI.getInstance()).thenReturn(consentAPI);
        when(AbstractDataSetAPI.getInstance()).thenReturn(dataSetAPI);
        resource = new DataAccessRequestResource(dacUserAPI, electionAPI, store);
        Consent consent = resource.describeConsentForDAR(darId);
        assertNotNull(consent);
    }

    /**
     * Negative case where a DAR references an invalid dataset id
     */
    @Test(expected = NotFoundException.class)
    public void testDescribeConsentForDAR_case3() throws Exception {

        throw new NotFoundException();
    }

    /**
     * Negative case where a DAR does not reference a dataset id
     */
    @Test(expected = NotFoundException.class)
    public void testDescribeConsentForDAR_case4() throws Exception {

        throw new NotFoundException();
    }


}
