package org.broadinstitute.consent.http.job;

import org.broadinstitute.consent.http.service.validate.UseRestrictionValidatorAPI;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

public class UseRestrictionValidationJobTest {

    @Mock
    private UseRestrictionValidatorAPI useRestrictionValidatorAPI;

    UseRestrictionValidationJob useRestrictionValidationJob;


    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        useRestrictionValidationJob = new UseRestrictionValidationJob();
        useRestrictionValidationJob.setUseRestrictionValidator(useRestrictionValidatorAPI);
    }

    @Test
    public void testVerifyCloseElectionsEmpty() throws Exception {
        useRestrictionValidationJob.doJob();
        verify(useRestrictionValidatorAPI,times(1)).validateConsentUseRestriction();
        verify(useRestrictionValidatorAPI,times(1)).validateDARUseRestriction();
    }

}