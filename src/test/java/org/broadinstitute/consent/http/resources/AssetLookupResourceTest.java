package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.broadinstitute.consent.http.enumeration.AssetType;
import org.broadinstitute.consent.http.models.AssetLookup;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.AssetLookupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssetLookupResourceTest {

  @Mock private AssetLookupService assetLookupService;

  private AssetLookupResource resource;
  private DuosUser duosUser;

  @BeforeEach
  void setUp() {
    resource = new AssetLookupResource(assetLookupService);
    duosUser = new DuosUser(new AuthUser("user@example.org"), new User());
  }

  @Test
  void testLookupReturnsWhatTheIdentifierNames() {
    AssetLookup lookup = AssetLookup.removed(AssetType.MODEL);
    when(assetLookupService.lookup(eq("DUOS-M-000045"), any())).thenReturn(lookup);

    Response response = resource.lookup(duosUser, "DUOS-M-000045");

    assertEquals(200, response.getStatus());
    assertEquals(lookup, response.getEntity());
  }

  @Test
  void testAnUnknownIdentifierIsNotFound() {
    when(assetLookupService.lookup(eq("DUOS-M-45"), any()))
        .thenThrow(new NotFoundException("Unknown DUOS identifier."));

    assertEquals(404, resource.lookup(duosUser, "DUOS-M-45").getStatus());
  }

  @Test
  void testAnySignedInUserMayLookUpAnIdentifier() throws NoSuchMethodException {
    assertNotNull(
        AssetLookupResource.class
            .getMethod("lookup", DuosUser.class, String.class)
            .getAnnotation(PermitAll.class));
  }
}
