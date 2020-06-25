package org.broadinstitute.consent.http.resources;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import java.util.Collections;
import java.util.List;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DacBuilder;
import org.broadinstitute.consent.http.service.DacService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DacResourceTest {

    @Mock
    private DacService dacService;

    private DacResource dacResource;

    private AuthUser authUser = new AuthUser("test@test.com");

    private Gson gson = new Gson();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        dacResource = new DacResource(dacService);
    }

    @Test
    public void testFindAll_success_1() {
        when(dacService.findAll()).thenReturn(Collections.emptyList());

        Response response = dacResource.findAll();
        Assert.assertEquals(200, response.getStatus());
        List dacs = ((List) response.getEntity());
        Assert.assertTrue(dacs.isEmpty());
    }

    @Test
    public void testFindAll_success_2() {
        Dac dac = new DacBuilder()
                .setName("name")
                .setDescription("description")
                .build();
        when(dacService.findAll()).thenReturn(Collections.singletonList(dac));
        when(dacService.findAllDacsWithMembers()).thenReturn(Collections.singletonList(dac));

        Response response = dacResource.findAll();
        Assert.assertEquals(200, response.getStatus());
        List dacs = ((List) response.getEntity());
        Assert.assertEquals(1, dacs.size());
    }

    @Test
    public void testCreateDac_success() throws Exception {
        Dac dac = new DacBuilder()
                .setName("name")
                .setDescription("description")
                .build();
        when(dacService.createDac(any(), any())).thenReturn(1);
        when(dacService.findById(1)).thenReturn(dac);

        Response response = dacResource.createDac(authUser, gson.toJson(dac));
        Assert.assertEquals(200, response.getStatus());
    }

    @Test(expected = BadRequestException.class)
    public void testCreateDac_badRequest_1() throws Exception {
        dacResource.createDac(authUser, null);
    }

    @Test(expected = BadRequestException.class)
    public void testCreateDac_badRequest_2() throws Exception {
        Dac dac = new DacBuilder()
                .setName(null)
                .setDescription("description")
                .build();
        when(dacService.createDac(any(), any())).thenReturn(1);
        when(dacService.findById(1)).thenReturn(dac);

        dacResource.createDac(authUser, gson.toJson(dac));
    }

    @Test(expected = BadRequestException.class)
    public void testCreateDac_badRequest_3() throws Exception {
        Dac dac = new DacBuilder()
                .setName("name")
                .setDescription(null)
                .build();
        when(dacService.createDac(any(), any())).thenReturn(1);
        when(dacService.findById(1)).thenReturn(dac);

        dacResource.createDac(authUser, gson.toJson(dac));
    }


    @Test
    public void testUpdateDac_success() {
        Dac dac = new DacBuilder()
                .setDacId(1)
                .setName("name")
                .setDescription("description")
                .build();
        doNothing().when(dacService).updateDac(isA(String.class), isA(String.class), isA(Integer.class));
        when(dacService.findById(1)).thenReturn(dac);

        Response response = dacResource.updateDac(authUser, gson.toJson(dac));
        Assert.assertEquals(200, response.getStatus());
    }

    @Test(expected = BadRequestException.class)
    public void testUpdateDac_badRequest_1() {
        dacResource.updateDac(authUser, null);
    }

    @Test(expected = BadRequestException.class)
    public void testUpdateDac_badRequest_2() {
        Dac dac = new DacBuilder()
                .setDacId(null)
                .setName("name")
                .setDescription("description")
                .build();
        dacResource.updateDac(authUser, gson.toJson(dac));
    }

    @Test(expected = BadRequestException.class)
    public void testUpdateDac_badRequest_3() {
        Dac dac = new DacBuilder()
                .setDacId(1)
                .setName(null)
                .setDescription("description")
                .build();
        dacResource.updateDac(authUser, gson.toJson(dac));
    }

    @Test(expected = BadRequestException.class)
    public void testUpdateDac_badRequest_4() {
        Dac dac = new DacBuilder()
                .setDacId(1)
                .setName("name")
                .setDescription(null)
                .build();
        dacResource.updateDac(authUser, gson.toJson(dac));
    }

    @Test
    public void testFindById_success() {
        Dac dac = new DacBuilder()
                .setDacId(1)
                .setName("name")
                .setDescription("description")
                .build();
        when(dacService.findById(1)).thenReturn(dac);

        Response response = dacResource.findById(dac.getDacId());
        Assert.assertEquals(200, response.getStatus());
    }

    @Test(expected = NotFoundException.class)
    public void testFindById_failure() {
        when(dacService.findById(1)).thenReturn(null);

        dacResource.findById(1);
    }

    @Test
    public void testDeleteDac_success() {
        Dac dac = new DacBuilder()
                .setDacId(1)
                .setName("name")
                .setDescription("description")
                .build();
        when(dacService.findById(1)).thenReturn(dac);

        Response response = dacResource.deleteDac(dac.getDacId());
        Assert.assertEquals(200, response.getStatus());

    }

    @Test(expected = NotFoundException.class)
    public void testDeleteDac_failure() {
        when(dacService.findById(1)).thenReturn(null);

        dacResource.deleteDac(1);
    }

}
