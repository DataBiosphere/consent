package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.LibraryCardService;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.feature.InstitutionAndLibraryCardEnforcement;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;

@ExtendWith(MockitoExtension.class)
class LibraryCardResourceTest {

  private final AuthUser authUser = new AuthUser("test@test.com");
  private final User adminUser =
      new User(
          1,
          authUser.getEmail(),
          "Admin",
          new Date(),
          Collections.singletonList(UserRoles.Admin()));
  private final DuosUser duosAdminUser = new DuosUser(authUser, adminUser);
  private final User lcUser =
      new User(
          2,
          "lc_user@gmail.com",
          "Researcher",
          new Date(),
          Collections.singletonList(UserRoles.Researcher()));
  private final User soUser =
      new User(
          3,
          "so_user@gmail.com",
          "Signing Official",
          new Date(),
          Collections.singletonList(UserRoles.SigningOfficial()));

  private LibraryCardResource resource;

  @Mock private UserService userService;
  @Mock private LibraryCardService libraryCardService;
  @Mock private InstitutionAndLibraryCardEnforcement institutionAndLibraryCardEnforcement;

  private LibraryCard mockLibraryCardSetup() {
    LibraryCard mockCard = new LibraryCard();
    mockCard.setUserId(2);
    mockCard.setCreateUserId(1);
    mockCard.setUserEmail(lcUser.getEmail());
    return mockCard;
  }

  @BeforeEach
  void initResource() {
    resource =
        new LibraryCardResource(
            userService, libraryCardService, institutionAndLibraryCardEnforcement);
  }

  private UnableToExecuteStatementException generateUniqueViolationException() {
    PSQLState uniqueViolationEnum = PSQLState.UNIQUE_VIOLATION;
    PSQLException uniqueViolationException = new PSQLException("Error", uniqueViolationEnum);
    return new UnableToExecuteStatementException(uniqueViolationException, null);
  }

  @Test
  void testGetLibraryCardsAsAdmin() {
    List<LibraryCard> libraryCards = Collections.singletonList(mockLibraryCardSetup());
    when(libraryCardService.findAllLibraryCards()).thenReturn(libraryCards);
    try (Response response = resource.getLibraryCards(duosAdminUser)) {
      String json = response.getEntity().toString();
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
      assertNotNull(json);
    }
  }

  @Test
  void testGetLibraryCardsById() {
    LibraryCard card = mockLibraryCardSetup();
    when(libraryCardService.findLibraryCardById(anyInt())).thenReturn(card);
    try (Response response = resource.getLibraryCardById(duosAdminUser, 1)) {
      String json = response.getEntity().toString();
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
      assertNotNull(json);
    }
  }

  @Test
  void testGetLibraryCardsByIdThrowsNotFoundException() {
    when(libraryCardService.findLibraryCardById(anyInt())).thenThrow(new NotFoundException());
    try (Response response = resource.getLibraryCardById(duosAdminUser, 1)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testGetLibraryCardByInstitutionId() {
    List<LibraryCard> cards = Collections.singletonList(mockLibraryCardSetup());
    when(libraryCardService.findLibraryCardsByInstitutionId(anyInt())).thenReturn(cards);
    try (Response response = resource.getLibraryCardsByInstitutionId(duosAdminUser, 1)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testGetLibraryCardByInstitutionIdThrowsNotFoundException() {
    when(libraryCardService.findLibraryCardsByInstitutionId(anyInt()))
        .thenThrow(new NotFoundException());
    try (Response response = resource.getLibraryCardsByInstitutionId(duosAdminUser, 1)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testCreateLibraryCard() {
    LibraryCard mockCard = mockLibraryCardSetup();
    String payload = GsonUtil.getInstance().toJson(mockCard);
    when(libraryCardService.createLibraryCard(any(LibraryCard.class), any(User.class)))
        .thenReturn(mockCard);
    try (Response response = resource.createLibraryCard(duosAdminUser, payload)) {
      String json = response.getEntity().toString();
      assertEquals(HttpStatusCodes.STATUS_CODE_CREATED, response.getStatus());
      assertNotNull(json);
    }
  }

  @Test
  void testCreateLibraryCardThrowsIllegalArgumentException() {
    LibraryCard mockCard = mockLibraryCardSetup();
    String payload = GsonUtil.getInstance().toJson(mockCard);
    when(libraryCardService.createLibraryCard(any(LibraryCard.class), any(User.class)))
        .thenThrow(new IllegalArgumentException());
    try (Response response = resource.createLibraryCard(duosAdminUser, payload)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testCreateLibraryCardThrowsConflictException() {
    UnableToExecuteStatementException exception = generateUniqueViolationException();
    String json = GsonUtil.getInstance().toJson(mockLibraryCardSetup());
    when(libraryCardService.createLibraryCard(any(LibraryCard.class), any(User.class)))
        .thenThrow(exception);
    try (Response response = resource.createLibraryCard(duosAdminUser, json)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_CONFLICT, response.getStatus());
    }
  }

  @Test
  void testCreateLibraryCardThrowsBadRequestException() {
    BadRequestException exception = new BadRequestException();
    String json = GsonUtil.getInstance().toJson(mockLibraryCardSetup());
    when(libraryCardService.createLibraryCard(any(LibraryCard.class), any(User.class)))
        .thenThrow(exception);
    try (Response response = resource.createLibraryCard(duosAdminUser, json)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testCreateLibraruCardThrowsNotFoundException() {
    NotFoundException exception = new NotFoundException();
    String json = GsonUtil.getInstance().toJson(mockLibraryCardSetup());
    when(libraryCardService.createLibraryCard(any(LibraryCard.class), any(User.class)))
        .thenThrow(exception);
    try (Response response = resource.createLibraryCard(duosAdminUser, json)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testDeleteLibraryCard() {
    LibraryCard card = mockLibraryCardSetup();
    card.setId(1);
    when(libraryCardService.findLibraryCardById(anyInt())).thenReturn(card);
    try (Response response = resource.deleteLibraryCard(duosAdminUser, card.getId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NO_CONTENT, response.getStatus());
      verify(libraryCardService).deleteLibraryCardById(card.getId());
    }
  }

  @Test
  void testDeleteLibraryCardUserNotFound() {
    LibraryCard card = mockLibraryCardSetup();
    card.setId(1);
    card.setUserId(null);
    when(userService.findUserById(null)).thenThrow(new NotFoundException());
    when(libraryCardService.findLibraryCardById(anyInt())).thenReturn(card);
    try (Response response = resource.deleteLibraryCard(duosAdminUser, card.getId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NO_CONTENT, response.getStatus());
      verify(libraryCardService).deleteLibraryCardById(card.getId());
    }
  }

  @Test
  void testDeleteLibraryCardThrowsNotFoundException() {
    LibraryCard card = mockLibraryCardSetup();
    when(libraryCardService.findLibraryCardById(anyInt())).thenReturn(card);
    doThrow(new NotFoundException()).when(libraryCardService).deleteLibraryCardById(anyInt());
    try (Response response = resource.deleteLibraryCard(duosAdminUser, 1)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testDeleteLibraryCardThrowsForbiddenException() {
    LibraryCard card = mockLibraryCardSetup();
    Institution soInstitution = new Institution();
    soInstitution.setId(1);
    soUser.setInstitution(soInstitution);
    soUser.setInstitutionId(soInstitution.getId());
    DuosUser soDuosUser = new DuosUser(authUser, soUser);

    Institution lcUserInstitution = new Institution();
    lcUserInstitution.setId(2);
    lcUser.setInstitution(lcUserInstitution);
    lcUser.setInstitutionId(lcUserInstitution.getId());

    when(libraryCardService.findLibraryCardById(anyInt())).thenReturn(card);
    when(userService.findUserById(card.getUserId())).thenReturn(lcUser);

    try (Response response = resource.deleteLibraryCard(soDuosUser, 1)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    }
  }

  @Test
  void testFindLibraryCardDaaAuditsByUserId_ValidSO() {
    when(userService.findUserById(lcUser.getUserId())).thenReturn(lcUser);
    doNothing()
        .when(institutionAndLibraryCardEnforcement)
        .validateEmailsFromSameInstitution(soUser.getEmail(), lcUser.getEmail());
    when(libraryCardService.findLibraryCardDaaAuditsByUserId(lcUser.getUserId()))
        .thenReturn(List.of());
    DuosUser soDuosUser = new DuosUser(authUser, soUser);

    try (Response response =
        resource.findLibraryCardDaaAuditsByUserId(soDuosUser, lcUser.getUserId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testFindLibraryCardDaaAuditsByUserId_ValidAdmin() {
    when(userService.findUserById(lcUser.getUserId())).thenReturn(lcUser);
    when(libraryCardService.findLibraryCardDaaAuditsByUserId(lcUser.getUserId()))
        .thenReturn(List.of());

    try (Response response =
        resource.findLibraryCardDaaAuditsByUserId(duosAdminUser, lcUser.getUserId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
      verify(institutionAndLibraryCardEnforcement, never())
          .validateEmailsFromSameInstitution(any(), any());
    }
  }

  @Test
  void testFindLibraryCardDaaAuditsByUserId_InvalidSO() {
    soUser.setEmail("some.other.email@other.domain.com");
    when(userService.findUserById(lcUser.getUserId())).thenReturn(lcUser);
    doThrow(new ForbiddenException())
        .when(institutionAndLibraryCardEnforcement)
        .validateEmailsFromSameInstitution(soUser.getEmail(), lcUser.getEmail());
    DuosUser soDuosUser = new DuosUser(authUser, soUser);

    try (Response response =
        resource.findLibraryCardDaaAuditsByUserId(soDuosUser, lcUser.getUserId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    }
  }

  @Test
  void testFindLibraryCardDaaAuditsByUserId_UserNotFound() {
    soUser.setEmail("some.other.email@other.domain.com");
    doThrow(new NotFoundException()).when(userService).findUserById(lcUser.getUserId());
    DuosUser soDuosUser = new DuosUser(authUser, soUser);

    try (Response response =
        resource.findLibraryCardDaaAuditsByUserId(soDuosUser, lcUser.getUserId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }
}
