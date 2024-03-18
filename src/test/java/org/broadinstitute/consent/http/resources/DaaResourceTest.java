package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.http.HttpStatus;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.service.DaaService;
import org.broadinstitute.consent.http.service.DacService;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DaaResourceTest {

  @Mock
  private DaaService daaService;
  @Mock
  private DacService dacService;
  @Mock
  private UserService userService;

  private final AuthUser authUser = new AuthUser("test@test.com");

  private DaaResource resource;

  @Test
  void testCreateDaaForDac_AdminCase() {
    UriInfo info = mock(UriInfo.class);
    UriBuilder builder = mock(UriBuilder.class);
    when(info.getBaseUriBuilder()).thenReturn(builder);
    when(builder.replacePath(any())).thenReturn(builder);
    Dac dac = new Dac();
    dac.setDacId(RandomUtils.nextInt(10, 100));
    User admin = new User();
    UserRole role = (new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName()));
    admin.setRoles(List.of(role));
    FormDataContentDisposition fileDetail = mock(FormDataContentDisposition.class);

    when(dacService.findById(any())).thenReturn(dac);
    when(userService.findUserByEmail(any())).thenReturn(admin);
    when(daaService.createDaaWithFso(any(), any(), any(), any())).thenReturn(new DataAccessAgreement());

    resource = new DaaResource(daaService, dacService, userService);
    Response response = resource.createDaaForDac(info, authUser, dac.getDacId(), IOUtils.toInputStream("test", "UTF-8"), fileDetail);
    assert response.getStatus() == HttpStatus.SC_CREATED;
  }

  @Test
  void testCreateDaaForDac_ChairCase() {
    UriInfo info = mock(UriInfo.class);
    UriBuilder builder = mock(UriBuilder.class);
    when(info.getBaseUriBuilder()).thenReturn(builder);
    when(builder.replacePath(any())).thenReturn(builder);
    Dac dac = new Dac();
    dac.setDacId(RandomUtils.nextInt(10, 100));
    User admin = new User();
    UserRole role = (new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName()));
    role.setDacId(dac.getDacId());
    admin.setRoles(List.of(role));
    FormDataContentDisposition fileDetail = mock(FormDataContentDisposition.class);

    when(dacService.findById(any())).thenReturn(dac);
    when(userService.findUserByEmail(any())).thenReturn(admin);
    when(daaService.createDaaWithFso(any(), any(), any(), any())).thenReturn(new DataAccessAgreement());

    resource = new DaaResource(daaService, dacService, userService);
    Response response = resource.createDaaForDac(info, authUser, dac.getDacId(), IOUtils.toInputStream("test", "UTF-8"), fileDetail);
    assert response.getStatus() == HttpStatus.SC_CREATED;
  }

  @Test
  void testCreateDaaForDac_InvalidChairCase() {
    UriInfo info = mock(UriInfo.class);
    Dac dac = new Dac();
    dac.setDacId(RandomUtils.nextInt(10, 100));
    User admin = new User();
    UserRole role = (new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName()));
    role.setDacId(1); // Note that this will not be the DAC we provide for DAA creation
    admin.setRoles(List.of(role));
    FormDataContentDisposition fileDetail = mock(FormDataContentDisposition.class);

    when(dacService.findById(any())).thenReturn(dac);
    when(userService.findUserByEmail(any())).thenReturn(admin);

    resource = new DaaResource(daaService, dacService, userService);
    Response response = resource.createDaaForDac(info, authUser, dac.getDacId(), IOUtils.toInputStream("test", "UTF-8"), fileDetail);
    assert response.getStatus() == HttpStatus.SC_FORBIDDEN;
  }

  @Test
  void testFindDaaByDaaId() {
    int expectedDaaId = RandomUtils.nextInt(10, 100);
    DataAccessAgreement expectedDaa = new DataAccessAgreement();
    expectedDaa.setDaaId(expectedDaaId);
    when(daaService.findById(expectedDaaId)).thenReturn(expectedDaa);

    resource = new DaaResource(daaService, dacService, userService);

    Response response = resource.findById(expectedDaaId);
    assert response.getStatus() == HttpStatus.SC_OK;
    assertEquals(expectedDaa, response.getEntity());
  }

  // can't find id
  @Test
  void testFindDaaByDaaIdInvalidId() {
    int invalidId = RandomUtils.nextInt(10, 100);
    when(daaService.findById(invalidId)).thenThrow(new NotFoundException());
    resource = new DaaResource(daaService, dacService, userService);

    Response response = resource.findById(invalidId);
    assert response.getStatus() == HttpStatus.SC_NOT_FOUND;
  }

}
