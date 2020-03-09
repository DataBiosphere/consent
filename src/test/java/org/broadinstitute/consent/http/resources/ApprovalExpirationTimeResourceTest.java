package org.broadinstitute.consent.http.resources;

import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.models.ApprovalExpirationTime;
import org.broadinstitute.consent.http.service.AbstractApprovalExpirationTimeAPI;
import org.broadinstitute.consent.http.service.ApprovalExpirationTimeAPI;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        AbstractApprovalExpirationTimeAPI.class
})
public class ApprovalExpirationTimeResourceTest {

    @Mock
    private ApprovalExpirationTimeAPI approvalExpirationTimeAPI;

    @Mock
    private UriInfo uriInfo;

    @Mock
    UriBuilder uriBuilder;

    private ApprovalExpirationTimeResource resource;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(AbstractApprovalExpirationTimeAPI.class);
    }

    private void initResource() {
        when(AbstractApprovalExpirationTimeAPI.getInstance()).thenReturn(approvalExpirationTimeAPI);
        resource = new ApprovalExpirationTimeResource();
    }

    @Test
    public void testCreateApprovalExpirationTime() throws Exception {
        ApprovalExpirationTime approvalExpirationTime = generateApprovalExpirationTime();
        when(approvalExpirationTimeAPI.create(any())).thenReturn(approvalExpirationTime);
        when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(Mockito.anyString())).thenReturn(uriBuilder);
        when(uriBuilder.build(anyString())).thenReturn(new URI("/api/approvalExpirationTime/" + RandomUtils.nextInt(1, 100)));
        initResource();

        Response response = resource.createdApprovalExpirationTime(uriInfo, approvalExpirationTime);
        assertEquals(201, response.getStatus());
    }

    @Test
    public void testDescribeApprovalExpirationTime() {
        ApprovalExpirationTime approvalExpirationTime = generateApprovalExpirationTime();
        when(approvalExpirationTimeAPI.findApprovalExpirationTime()).thenReturn(approvalExpirationTime);
        initResource();

        Response response = resource.describeApprovalExpirationTime();
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testDescribe() {
        ApprovalExpirationTime approvalExpirationTime = generateApprovalExpirationTime();
        when(approvalExpirationTimeAPI.findApprovalExpirationTimeById(any())).thenReturn(approvalExpirationTime);
        initResource();

        Response response = resource.describe(RandomUtils.nextInt(1, 10));
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testUpdate() throws Exception {
        ApprovalExpirationTime approvalExpirationTime = generateApprovalExpirationTime();
        when(approvalExpirationTimeAPI.update(any(), any())).thenReturn(approvalExpirationTime);
        when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(Mockito.anyString())).thenReturn(uriBuilder);
        when(uriBuilder.build(anyString())).thenReturn(new URI("/api/approvalExpirationTime/" + RandomUtils.nextInt(1, 100)));
        initResource();

        Response response = resource.update(uriInfo, approvalExpirationTime, RandomUtils.nextInt(1, 10));
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testDelete() {
        doNothing().when(approvalExpirationTimeAPI).deleteApprovalExpirationTime(any());
        initResource();

        Response response = resource.delete(RandomUtils.nextInt(1, 10));
        assertEquals(200, response.getStatus());
    }

    private ApprovalExpirationTime generateApprovalExpirationTime() {
        ApprovalExpirationTime approvalExpirationTime = new ApprovalExpirationTime();
        approvalExpirationTime.setAmountOfDays(7);
        approvalExpirationTime.setUserId(3333);
        return approvalExpirationTime;
    }

}
