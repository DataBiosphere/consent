package org.broadinstitute.consent.http.resources;

import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.models.HelpReport;
import org.broadinstitute.consent.http.service.AbstractHelpReportAPI;
import org.broadinstitute.consent.http.service.EmailNotifierService;
import org.broadinstitute.consent.http.service.HelpReportAPI;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        AbstractHelpReportAPI.class
})
public class HelpReportResourceTest {

    @Mock
    private HelpReportAPI helpReportAPI;
    @Mock
    private EmailNotifierService emailNotifierService;
    @Mock
    UriInfo info;
    @Mock
    UriBuilder builder;

    private HelpReportResource resource;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(AbstractHelpReportAPI.class);
    }

    private void initResource() {
        when(AbstractHelpReportAPI.getInstance()).thenReturn(helpReportAPI);
        when(builder.path(anyString())).thenReturn(builder);
        when(builder.build()).thenReturn(URI.create("https://test.domain.org/some/path"));
        when(info.getRequestUriBuilder()).thenReturn(builder);
        resource = new HelpReportResource(emailNotifierService);
    }

    @Test
    public void createHelpReport() throws Exception {
        HelpReport report = new HelpReport();
        report.setUserId(RandomUtils.nextInt(1, 10));
        report.setSubject(RandomStringUtils.random(20));
        report.setDescription(RandomStringUtils.random(100));
        //            emailApi.sendNewRequestHelpMessage(helpReport);
        when(helpReportAPI.create(any())).thenReturn(report);
        doNothing().when(emailNotifierService).sendNewRequestHelpMessage(any());
        initResource();
        Response response = resource.createdHelpReport(info, report);
        Assert.assertEquals(201, response.getStatus());
    }

}
