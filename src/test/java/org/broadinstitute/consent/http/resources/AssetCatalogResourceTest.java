package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Set;
import org.broadinstitute.consent.http.exceptions.ConsentConflictException;
import org.broadinstitute.consent.http.models.AssetCatalogCheck;
import org.broadinstitute.consent.http.models.AssetCatalogCheck.Findings;
import org.broadinstitute.consent.http.models.AssetCatalogRepair;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.AssetCatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssetCatalogResourceTest {

  @Mock private AssetCatalogService assetCatalogService;

  private AssetCatalogResource resource;
  private DuosUser duosUser;

  @BeforeEach
  void setUp() {
    resource = new AssetCatalogResource(assetCatalogService);
    duosUser = new DuosUser(new AuthUser("admin@example.org"), new User());
  }

  @Test
  void testCheckReturnsTheFindings() {
    Findings none = new Findings(0, List.of());
    AssetCatalogCheck check = new AssetCatalogCheck(none, none, none, none, none, none);
    when(assetCatalogService.check()).thenReturn(check);

    Response response = resource.check(duosUser);

    assertEquals(200, response.getStatus());
    assertEquals(check, response.getEntity());
  }

  @Test
  void testCheckMapsAFailureToServerError() {
    when(assetCatalogService.check()).thenThrow(new IllegalStateException("failed"));

    assertEquals(500, resource.check(duosUser).getStatus());
  }

  @Test
  void testRepairReturnsWhatItChanged() {
    AssetCatalogRepair repair = new AssetCatalogRepair(2, 1, 1, 0, null);
    when(assetCatalogService.repair(duosUser.getUser())).thenReturn(repair);

    Response response = resource.repair(duosUser);

    assertEquals(200, response.getStatus());
    assertEquals(repair, response.getEntity());
  }

  @Test
  void testRepairWhileAnotherRunsIsAConflict() {
    when(assetCatalogService.repair(duosUser.getUser()))
        .thenThrow(new ConsentConflictException("running"));

    assertEquals(409, resource.repair(duosUser).getStatus());
  }

  @Test
  void testEveryEndpointIsAdminOnly() {
    EndpointRoles.assertEveryEndpointAdmits(AssetCatalogResource.class, Set.of(Resource.ADMIN));
  }
}
