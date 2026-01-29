package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.JsonObject;
import jakarta.ws.rs.core.Response;
import java.util.stream.Stream;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.enumeration.OntologyType;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.OntologyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OntologyResourceTest extends AbstractTestHelper {

  @Mock private OntologyService ontologyService;

  private final AuthUser authUser = new AuthUser("test@test.com");

  private OntologyResource resource;

  @Test
  void testIndexOntologyTermsSuccess() throws Exception {
    User admin = new User();
    admin.setAdminRole();
    DuosUser duosUser = new DuosUser(authUser, admin);

    doNothing().when(ontologyService).indexOntology(any(), any());

    resource = new OntologyResource(ontologyService);
    try (Response response = resource.indexOntologyTerms(duosUser, OntologyType.DOID.name())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testIndexOntologyTermsInvalidType() {
    User admin = new User();
    admin.setAdminRole();
    DuosUser duosUser = new DuosUser(authUser, admin);

    resource = new OntologyResource(ontologyService);
    try (Response response = resource.indexOntologyTerms(duosUser, "INVALID_TYPE")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testIndexOntologyTermsException() throws Exception {
    User admin = new User();
    admin.setAdminRole();
    DuosUser duosUser = new DuosUser(authUser, admin);

    doThrow(new RuntimeException()).when(ontologyService).indexOntology(any(), any());

    resource = new OntologyResource(ontologyService);
    try (Response response = resource.indexOntologyTerms(duosUser, OntologyType.DOID.name())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_SERVER_ERROR, response.getStatus());
    }
  }

  @Test
  void testSearchTermSuccess() {
    JsonObject term1 = new JsonObject();
    term1.addProperty("id", "DOID_1234");
    JsonObject term2 = new JsonObject();
    term2.addProperty("id", "DOID_5678");

    when(ontologyService.findByTermIds("DOID_1234,DOID_5678")).thenReturn(Stream.of(term1, term2));

    resource = new OntologyResource(ontologyService);
    try (Response response = resource.searchTerm("DOID_1234,DOID_5678")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testSearchTermException() {
    when(ontologyService.findByTermIds(any())).thenThrow(new RuntimeException());

    resource = new OntologyResource(ontologyService);
    try (Response response = resource.searchTerm("DOID_1234")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_SERVER_ERROR, response.getStatus());
    }
  }
}
