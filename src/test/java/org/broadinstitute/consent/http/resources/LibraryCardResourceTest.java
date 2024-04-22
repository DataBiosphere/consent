package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.Gson;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.service.LibraryCardService;
import org.broadinstitute.consent.http.service.UserService;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;

@ExtendWith(MockitoExtension.class)
public class LibraryCardResourceTest {

  private final AuthUser authUser = new AuthUser("test@test.com");
  private final List<UserRole> adminRoles = Collections.singletonList(UserRoles.AdminRole());
  private final User user = new User(1, authUser.getEmail(), "Display Name", new Date(),
      adminRoles);
  private final User lcUser = new User(2, "testuser@gmail.com", "Test User", new Date(),
      Collections.singletonList(UserRoles.ResearcherRole()));

  private LibraryCardResource resource;

  @Mock
  private UserService userService;
  @Mock
  private LibraryCardService libraryCardService;

  private LibraryCard mockLibraryCardSetup() {
    LibraryCard mockCard = new LibraryCard();
    mockCard.setUserId(2);
    mockCard.setCreateUserId(1);
    mockCard.setUserEmail(lcUser.getEmail());
    return mockCard;
  }

  private User mockSOUser() {
    User mockUser = new User(2, "testuser@gmail.com", "Test User", new Date(),
        Collections.singletonList(UserRoles.SigningOfficialRole()));
    return mockUser;
  }

  private void initResource() {
    resource = new LibraryCardResource(userService, libraryCardService);
  }

  private UnableToExecuteStatementException generateUniqueViolationException() {
    PSQLState uniqueViolationEnum = PSQLState.UNIQUE_VIOLATION;
    PSQLException uniqueViolationException = new PSQLException(
        "Error", uniqueViolationEnum
    );
    return new UnableToExecuteStatementException(uniqueViolationException, null);
  }

  @Test
  void testGetLibraryCardsAsAdmin() {
    List<LibraryCard> libraryCards = Collections.singletonList(mockLibraryCardSetup());
    when(libraryCardService.findAllLibraryCards()).thenReturn(libraryCards);
    initResource();
    Response response = resource.getLibraryCards(authUser);
    String json = response.getEntity().toString();
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    assertNotNull(json);
  }

  @Test
  void testGetLibraryCardsById() {
    LibraryCard card = mockLibraryCardSetup();
    when(libraryCardService.findLibraryCardById(anyInt())).thenReturn(card);
    initResource();
    Response response = resource.getLibraryCardById(authUser, 1);
    String json = response.getEntity().toString();
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    assertNotNull(json);
  }

  @Test
  void testGetLibraryCardsByIdThrowsNotFoundException() {
    when(libraryCardService.findLibraryCardById(anyInt())).thenThrow(new NotFoundException());
    initResource();
    Response response = resource.getLibraryCardById(authUser, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testGetLibraryCardByInstitutionId() {
    List<LibraryCard> cards = Collections.singletonList(mockLibraryCardSetup());
    when(libraryCardService.findLibraryCardsByInstitutionId(anyInt())).thenReturn(cards);
    initResource();
    Response response = resource.getLibraryCardsByInstitutionId(authUser, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testGetLibraryCardByInstitutionIdThrowsNotFoundException() {
    when(libraryCardService.findLibraryCardsByInstitutionId(anyInt())).thenThrow(
        new NotFoundException());
    initResource();
    Response response = resource.getLibraryCardsByInstitutionId(authUser, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testCreateLibraryCard() throws Exception {
    LibraryCard mockCard = mockLibraryCardSetup();
    String payload = new Gson().toJson(mockCard);
    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
    when(libraryCardService.createLibraryCard(any(LibraryCard.class), any(User.class))).thenReturn(
        mockCard);
    initResource();
    Response response = resource.createLibraryCard(authUser, payload);
    String json = response.getEntity().toString();
    assertEquals(HttpStatusCodes.STATUS_CODE_CREATED, response.getStatus());
    assertNotNull(json);
  }

  @Test
  void testCreateLibraryCardThrowsIllegalArgumentException() throws Exception {
    LibraryCard mockCard = mockLibraryCardSetup();
    String payload = new Gson().toJson(mockCard);
    when(userService.findUserByEmail(anyString())).thenReturn(user);
    when(libraryCardService.createLibraryCard(any(LibraryCard.class), any(User.class))).thenThrow(
        new IllegalArgumentException());
    initResource();
    Response response = resource.createLibraryCard(authUser, payload);
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  void testCreateLibraryCardThrowsConflictException() throws Exception {
    UnableToExecuteStatementException exception = generateUniqueViolationException();
    String json = new Gson().toJson(mockLibraryCardSetup());
    when(userService.findUserByEmail(anyString())).thenReturn(user);
    when(libraryCardService.createLibraryCard(any(LibraryCard.class), any(User.class))).thenThrow(
        exception);
    initResource();
    Response response = resource.createLibraryCard(authUser, json);
    assertEquals(HttpStatusCodes.STATUS_CODE_CONFLICT, response.getStatus());
  }

  @Test
  void testCreateLibraryCardThrowsBadRequestException() throws Exception {
    BadRequestException exception = new BadRequestException();
    String json = new Gson().toJson(mockLibraryCardSetup());
    when(userService.findUserByEmail(anyString())).thenReturn(user);
    when(libraryCardService.createLibraryCard(any(LibraryCard.class), any(User.class))).thenThrow(
        exception);
    initResource();
    Response response = resource.createLibraryCard(authUser, json);
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  void testCreateLibraruCardThrowsNotFoundException() throws Exception {
    NotFoundException exception = new NotFoundException();
    String json = new Gson().toJson(mockLibraryCardSetup());
    when(userService.findUserByEmail(anyString())).thenReturn(user);
    when(libraryCardService.createLibraryCard(any(LibraryCard.class), any(User.class))).thenThrow(
        exception);
    initResource();
    Response response = resource.createLibraryCard(authUser, json);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testUpdateLibraryCard() {
    LibraryCard mockCard = mockLibraryCardSetup();
    String payload = new Gson().toJson(mockCard);
    when(userService.findUserByEmail(anyString())).thenReturn(user);
    when(libraryCardService.updateLibraryCard(any(LibraryCard.class), anyInt(), anyInt()))
        .thenReturn(mockCard);
    initResource();
    Response response = resource.updateLibraryCard(authUser, 1, payload);
    String json = response.getEntity().toString();
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    assertNotNull(json);
  }

  @Test
  void testUpdateLibraryCardThrowsIllegalArgumentException() {
    LibraryCard mockCard = mockLibraryCardSetup();
    String payload = new Gson().toJson(mockCard);
    when(userService.findUserByEmail(anyString())).thenReturn(user);
    when(libraryCardService.updateLibraryCard(any(LibraryCard.class), anyInt(), anyInt()))
        .thenThrow(new IllegalArgumentException());
    initResource();
    Response response = resource.updateLibraryCard(authUser, 1, payload);
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  void testUpdateLibraryCardThrowsNotFoundException() {
    when(userService.findUserByEmail(anyString())).thenReturn(user);
    when(libraryCardService.updateLibraryCard(any(LibraryCard.class), anyInt(), anyInt()))
        .thenThrow(new NotFoundException());
    String payload = new Gson().toJson(mockLibraryCardSetup());
    initResource();
    Response response = resource.updateLibraryCard(authUser, 1, payload);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testUpdateLibraryCardThrowsUniqueViolation() {
    UnableToExecuteStatementException exception = generateUniqueViolationException();
    when(userService.findUserByEmail(anyString())).thenReturn(user);
    when(libraryCardService.updateLibraryCard(any(LibraryCard.class), anyInt(), anyInt()))
        .thenThrow(exception);
    String payload = new Gson().toJson(mockLibraryCardSetup());
    initResource();
    Response response = resource.updateLibraryCard(authUser, 1, payload);
    assertEquals(HttpStatusCodes.STATUS_CODE_CONFLICT, response.getStatus());
  }

  @Test
  void deleteLibraryCard() {
    LibraryCard card = mockLibraryCardSetup();
    when(userService.findUserByEmail(anyString())).thenReturn(user);
    when(libraryCardService.findLibraryCardById(anyInt())).thenReturn(card);
    initResource();

    Response response = resource.deleteLibraryCard(authUser, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_NO_CONTENT, response.getStatus());
  }

  @Test
  void deleteLibraryCardThrowsNotFoundException() {
    LibraryCard card = mockLibraryCardSetup();
    when(userService.findUserByEmail(anyString())).thenReturn(user);
    when(libraryCardService.findLibraryCardById(anyInt())).thenReturn(card);
    doThrow(new NotFoundException()).when(libraryCardService).deleteLibraryCardById(anyInt());
    initResource();

    Response response = resource.deleteLibraryCard(authUser, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void deleteLibraryCardThrowsForbiddenException() {
    LibraryCard card = mockLibraryCardSetup();
    User soUser = mockSOUser();
    soUser.setInstitutionId(1);
    card.setInstitutionId(2);

    when(userService.findUserByEmail(anyString())).thenReturn(soUser);
    when(libraryCardService.findLibraryCardById(anyInt())).thenReturn(card);

    initResource();
    Response response = resource.deleteLibraryCard(authUser, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
  }
}
