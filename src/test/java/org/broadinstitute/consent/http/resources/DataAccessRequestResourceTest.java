package org.broadinstitute.consent.http.resources;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.UUID;
import javax.ws.rs.NotFoundException;
import org.broadinstitute.consent.http.cloudstore.GCSStore;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.service.AbstractConsentAPI;
import org.broadinstitute.consent.http.service.AbstractDataAccessRequestAPI;
import org.broadinstitute.consent.http.service.AbstractDataSetAPI;
import org.broadinstitute.consent.http.service.AbstractElectionAPI;
import org.broadinstitute.consent.http.service.AbstractMatchProcessAPI;
import org.broadinstitute.consent.http.service.AbstractTranslateService;
import org.broadinstitute.consent.http.service.ConsentAPI;
import org.broadinstitute.consent.http.service.CounterService;
import org.broadinstitute.consent.http.service.DataAccessRequestAPI;
import org.broadinstitute.consent.http.service.DataAccessRequestService;
import org.broadinstitute.consent.http.service.DataSetAPI;
import org.broadinstitute.consent.http.service.ElectionAPI;
import org.broadinstitute.consent.http.service.EmailNotifierService;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.users.AbstractDACUserAPI;
import org.broadinstitute.consent.http.service.users.DACUserAPI;
import org.broadinstitute.consent.http.service.validate.AbstractUseRestrictionValidatorAPI;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        AbstractDataAccessRequestAPI.class,
        AbstractConsentAPI.class,
        AbstractMatchProcessAPI.class,
        AbstractUseRestrictionValidatorAPI.class,
        AbstractTranslateService.class,
        AbstractDataSetAPI.class,
        AbstractDACUserAPI.class,
        AbstractElectionAPI.class
})
public class DataAccessRequestResourceTest {

    @Mock
    DataAccessRequestService dataAccessRequestService;
    @Mock
    DACUserAPI dacUserAPI;
    @Mock
    ElectionAPI electionAPI;
    @Mock
    CounterService counterService;
    @Mock
    EmailNotifierService emailNotifierService;
    @Mock
    GCSStore store;
    @Mock
    ConsentAPI consentAPI;
    @Mock
    DataAccessRequestAPI dataAccessRequestAPI;
    @Mock
    DataSetAPI dataSetAPI;
    @Mock
    UserService userService;

    private DataAccessRequestResource resource;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(AbstractDataAccessRequestAPI.class);
        PowerMockito.mockStatic(AbstractConsentAPI.class);
        PowerMockito.mockStatic(AbstractMatchProcessAPI.class);
        PowerMockito.mockStatic(AbstractUseRestrictionValidatorAPI.class);
        PowerMockito.mockStatic(AbstractTranslateService.class);
        PowerMockito.mockStatic(AbstractDataSetAPI.class);
        PowerMockito.mockStatic(AbstractDACUserAPI.class);
        PowerMockito.mockStatic(AbstractElectionAPI.class);
    }

    /**
     * Positive case where a DAR references a numeric dataset id
     */
    @Test
    public void testDescribeConsentForDarCase1() {
        DataAccessRequest dar = generateDataAccessRequest();
        when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);
        when(consentAPI.getConsentFromDatasetID(any())).thenReturn(new Consent());
        when(AbstractDataAccessRequestAPI.getInstance()).thenReturn(dataAccessRequestAPI);
        when(AbstractConsentAPI.getInstance()).thenReturn(consentAPI);
        when(AbstractDataSetAPI.getInstance()).thenReturn(dataSetAPI);
        when(AbstractDACUserAPI.getInstance()).thenReturn(dacUserAPI);
        when(AbstractElectionAPI.getInstance()).thenReturn(electionAPI);
        resource = new DataAccessRequestResource(counterService, dataAccessRequestService, emailNotifierService, store, userService);
        Consent consent = resource.describeConsentForDAR(dar.getReferenceId());
        assertNotNull(consent);
    }

    /**
     * Positive case where a DAR references a string dataset id
     */
    @Test
    public void testDescribeConsentForDarCase2() throws Exception {
        DataAccessRequest dar = generateDataAccessRequest();
        when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);
        DataSet dataSet = new DataSet();
        dataSet.setDataSetId(1);
        when(consentAPI.getConsentFromDatasetID(any())).thenReturn(new Consent());
        when(dataSetAPI.findDataSetByObjectId(any())).thenReturn(dataSet);
        when(AbstractDataAccessRequestAPI.getInstance()).thenReturn(dataAccessRequestAPI);
        when(AbstractConsentAPI.getInstance()).thenReturn(consentAPI);
        when(AbstractDataSetAPI.getInstance()).thenReturn(dataSetAPI);
        when(AbstractDACUserAPI.getInstance()).thenReturn(dacUserAPI);
        when(AbstractElectionAPI.getInstance()).thenReturn(electionAPI);
        resource = new DataAccessRequestResource(counterService, dataAccessRequestService, emailNotifierService, store, userService);
        Consent consent = resource.describeConsentForDAR(dar.getReferenceId());
        assertNotNull(consent);
    }

    /**
     * Negative case where a DAR references an invalid dataset id
     */
    @Test(expected = NotFoundException.class)
    public void testDescribeConsentForDarCase3() throws Exception {
        DataAccessRequest dar = generateDataAccessRequest();
        when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);
        when(consentAPI.getConsentFromDatasetID(any())).thenReturn(null);
        when(AbstractDataAccessRequestAPI.getInstance()).thenReturn(dataAccessRequestAPI);
        when(AbstractConsentAPI.getInstance()).thenReturn(consentAPI);
        when(AbstractDACUserAPI.getInstance()).thenReturn(dacUserAPI);
        when(AbstractElectionAPI.getInstance()).thenReturn(electionAPI);
        resource = new DataAccessRequestResource(counterService, dataAccessRequestService, emailNotifierService, store, userService);
        resource.describeConsentForDAR(dar.getReferenceId());
    }

    /**
     * Negative case where a DAR does not reference a dataset id
     */
    @Test(expected = NotFoundException.class)
    public void testDescribeConsentForDarCase4() throws Exception {
        DataAccessRequest dar = generateDataAccessRequest();
        dar.getData().setDatasetId(null);
        when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);
        when(AbstractDataAccessRequestAPI.getInstance()).thenReturn(dataAccessRequestAPI);
        when(AbstractDACUserAPI.getInstance()).thenReturn(dacUserAPI);
        when(AbstractElectionAPI.getInstance()).thenReturn(electionAPI);
        resource = new DataAccessRequestResource(counterService, dataAccessRequestService, emailNotifierService, store, userService);
        resource.describeConsentForDAR(dar.getReferenceId());
    }

    private DataAccessRequest generateDataAccessRequest() {
        DataAccessRequest dar = new DataAccessRequest();
        DataAccessRequestData data = new DataAccessRequestData();
        dar.setReferenceId(UUID.randomUUID().toString());
        data.setReferenceId(dar.getReferenceId());
        data.setDatasetId(Arrays.asList(1, 2));
        dar.setData(data);
        return dar;
    }

}
