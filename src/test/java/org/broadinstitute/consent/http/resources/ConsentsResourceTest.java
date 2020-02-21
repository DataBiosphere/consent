package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.enumeration.AssociationType;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.ConsentBuilder;
import org.broadinstitute.consent.http.service.ConsentService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class ConsentsResourceTest {

    @Mock
    ConsentService service;

    private ConsentsResource resource;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    private void initResource() {
        resource = new ConsentsResource(service);
    }

    @Test
    public void testFindByIds() {
        String searchId = UUID.randomUUID().toString();
        Consent c = generateConsent(searchId);
        when(service.findByConsentIds(any())).thenReturn(Collections.singletonList(c));
        initResource();
        Response response = resource.findByIds(searchId);
        Assert.assertEquals(200, response.getStatus());
    }

    @Test(expected = NotFoundException.class)
    public void testFindByIds_NotFound_Case1() {
        initResource();
        resource.findByIds("");
    }

    @Test(expected = NotFoundException.class)
    public void testFindByIds_NotFound_Case2() {
        String searchId = UUID.randomUUID().toString();
        when(service.findByConsentIds(any())).thenReturn(Collections.emptyList());
        initResource();
        resource.findByIds(searchId);
    }

    @Test(expected = NotFoundException.class)
    public void testFindByIds_NotFound_Case3() {
        String searchId = UUID.randomUUID().toString();
        String consentId = UUID.randomUUID().toString();
        Consent c = generateConsent(consentId);
        when(service.findByConsentIds(any())).thenReturn(Collections.singletonList(c));
        initResource();
        resource.findByIds(searchId);
    }

    // Test the case where only some of the requested consent ids are real consents
    @Test(expected = NotFoundException.class)
    public void testFindByIds_NotFound_Case4() {
        String searchId1 = UUID.randomUUID().toString();
        String searchId2 = UUID.randomUUID().toString();
        Consent c = generateConsent(searchId1);
        when(service.findByConsentIds(any())).thenReturn(Collections.singletonList(c));
        initResource();
        resource.findByIds(searchId1 + "," + searchId2);
    }

    @Test
    public void testFindByAssociationType() {
        when(service.findConsentsByAssociationType(any())).
                thenReturn(Collections.singletonList(generateConsent(UUID.randomUUID().toString())));
        initResource();
        Response response = resource.findByAssociationType(AssociationType.SAMPLESET.getValue());
        Assert.assertEquals(200, response.getStatus());
    }

    @Test(expected = NotFoundException.class)
    public void testFindByAssociationType_NotFound() {
        when(service.findConsentsByAssociationType(any())).thenReturn(Collections.emptyList());
        initResource();
        resource.findByAssociationType(AssociationType.SAMPLESET.getValue());
    }


    private Consent generateConsent(String consentId) {
        return new ConsentBuilder().
                setConsentId(consentId).
                build();
    }

}
