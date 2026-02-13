package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.enumeration.OntologyType;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.OntologyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OntologyResourceTest extends AbstractTestHelper {

  @Mock private OntologyService ontologyService;
  @Mock private StreamingOutput mockStreamingOutput;

  private final AuthUser authUser = new AuthUser("test@test.com");

  private OntologyResource resource;

  @Test
  void testIndexOntologyTermsSuccess() {
    User admin = new User();
    admin.setAdminRole();
    DuosUser duosUser = new DuosUser(authUser, admin);

    doNothing().when(ontologyService).indexOntology(duosUser.getUser(), OntologyType.DOID);

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
  void testIndexOntologyTermsException() {
    User admin = new User();
    admin.setAdminRole();
    DuosUser duosUser = new DuosUser(authUser, admin);

    doThrow(new RuntimeException())
        .when(ontologyService)
        .indexOntology(duosUser.getUser(), OntologyType.DOID);

    resource = new OntologyResource(ontologyService);
    try (Response response = resource.indexOntologyTerms(duosUser, OntologyType.DOID.name())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_SERVER_ERROR, response.getStatus());
    }
  }

  @Test
  void testDeleteOntologyTermsSuccess() {
    User admin = new User();
    admin.setAdminRole();
    DuosUser duosUser = new DuosUser(authUser, admin);

    doNothing().when(ontologyService).deleteOntologyTerms(OntologyType.DOID);

    resource = new OntologyResource(ontologyService);
    try (Response response = resource.deleteOntologyTerms(duosUser, OntologyType.DOID.name())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testDeleteOntologyTermsInvalidType() {
    User admin = new User();
    admin.setAdminRole();
    DuosUser duosUser = new DuosUser(authUser, admin);

    resource = new OntologyResource(ontologyService);
    try (Response response = resource.deleteOntologyTerms(duosUser, "INVALID_TYPE")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testDeleteOntologyTermsException() {
    User admin = new User();
    admin.setAdminRole();
    DuosUser duosUser = new DuosUser(authUser, admin);

    doThrow(new RuntimeException()).when(ontologyService).deleteOntologyTerms(OntologyType.DOID);

    resource = new OntologyResource(ontologyService);
    try (Response response = resource.deleteOntologyTerms(duosUser, OntologyType.DOID.name())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_SERVER_ERROR, response.getStatus());
    }
  }

  @Test
  void testSearchByTermIdsSuccess() {
    when(ontologyService.findByTermIds("DOID_1234,DOID_5678".split(",")))
        .thenReturn(mockStreamingOutput);
    resource = new OntologyResource(ontologyService);
    try (Response response = resource.searchByTermIds("DOID_1234,DOID_5678")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testSearchByTermIdsException() {
    when(ontologyService.findByTermIds(new String[] {"DOID_1234"}))
        .thenThrow(new RuntimeException());

    resource = new OntologyResource(ontologyService);
    try (Response response = resource.searchByTermIds("DOID_1234")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_SERVER_ERROR, response.getStatus());
    }
  }

  @Test
  void testAutocompleteSuccessAllValuesPopulated() {
    when(ontologyService.findByQuery("cancer", OntologyType.DOID, 10))
        .thenReturn(mockStreamingOutput);
    resource = new OntologyResource(ontologyService);
    try (Response response = resource.autocomplete("cancer", OntologyType.DOID.name(), 10)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testAutocompleteSuccessNullType() {
    when(ontologyService.findByQuery("cancer", null, 10)).thenReturn(mockStreamingOutput);
    resource = new OntologyResource(ontologyService);
    try (Response response = resource.autocomplete("cancer", null, 10)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testAutocompleteSuccessBlankType() {
    when(ontologyService.findByQuery("cancer", null, 10)).thenReturn(mockStreamingOutput);
    resource = new OntologyResource(ontologyService);
    try (Response response = resource.autocomplete("cancer", "", 10)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testAutocompleteSuccessNullTypeAndCount() {
    when(ontologyService.findByQuery("cancer", null, null)).thenReturn(mockStreamingOutput);
    resource = new OntologyResource(ontologyService);
    try (Response response = resource.autocomplete("cancer", null, null)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   ", "\t", "\n"})
  void testAutocompleteInvalidQuery(String q) {
    resource = new OntologyResource(ontologyService);
    try (Response response = resource.autocomplete(q, null, null)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testAutocompleteNullQuery() {
    resource = new OntologyResource(ontologyService);
    try (Response response = resource.autocomplete(null, null, null)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testAutocompleteException() {
    when(ontologyService.findByQuery("cancer", null, null)).thenThrow(new RuntimeException());
    resource = new OntologyResource(ontologyService);
    try (Response response = resource.autocomplete("cancer", null, null)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_SERVER_ERROR, response.getStatus());
    }
  }
}
