package org.broadinstitute.consent.http.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.Gson;
import java.util.Collections;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Date;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.LibraryCardService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;

public class LibraryCardResourceTest {
  private final AuthUser authUser = new AuthUser("test@test.com");
  private final List<UserRole> adminRoles = Collections.singletonList(
    new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName())
  );
  private final User user = new User(1, authUser.getName(), "Display Name", new Date(), adminRoles, authUser.getName());
  private LibraryCardResource resource;

  @Mock private UserService userService;
  @Mock private LibraryCardService libraryCardService;
  @Mock private UriInfo info;
  @Mock private UriBuilder builder;
  @Mock private User mockUser;

  private LibraryCard mockLibraryCardSetup() {
    LibraryCard mockCard = new LibraryCard();
    mockCard.setUserId(2);
    mockCard.setCreateUserId(1);
    return mockCard;
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
  };

  @Before
  public void setUp(){
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testGetLibraryCardsAsAdmin() {
    List<LibraryCard> libraryCards = Collections.singletonList(mockLibraryCardSetup());
    when(libraryCardService.findAllLibraryCards()).thenReturn(libraryCards);
    initResource();
    Response response = resource.getLibraryCards(authUser);
    String json = response.getEntity().toString();
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    assertNotNull(json);
  }

  @Test
  public void testGetLibraryCardsById() {
    LibraryCard card = mockLibraryCardSetup();
    when(libraryCardService.findLibraryCardById(anyInt())).thenReturn(card);
    initResource();
    Response response = resource.getLibraryCardById(authUser, 1);
    String json = response.getEntity().toString();
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    assertNotNull(json);
  }

  @Test
  public void testGetLibraryCardsByIdThrowsNotFoundException() {
    when(libraryCardService.findLibraryCardById(anyInt())).thenThrow(new NotFoundException());
    initResource();
    Response response = resource.getLibraryCardById(authUser, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  public void testGetLibraryCardByInstitutionId() {
    List<LibraryCard> cards = Collections.singletonList(mockLibraryCardSetup());
    when(libraryCardService.findLibraryCardsByInstitutionId(anyInt())).thenReturn(cards);
    initResource();
    Response response = resource.getLibraryCardsByInstitutionId(authUser, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  public void testGetLibraryCardByInstitutionIdThrowsNotFoundException() {
    when(libraryCardService.findLibraryCardsByInstitutionId(anyInt())).thenThrow(new NotFoundException());
    initResource();
    Response response = resource.getLibraryCardsByInstitutionId(authUser, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  public void testCreateLibraryCard() {
    LibraryCard mockCard = mockLibraryCardSetup();
    String payload = new Gson().toJson(mockCard);
    when(userService.findUserByEmail(anyString())).thenReturn(user);
    when(libraryCardService.createLibraryCard(any(LibraryCard.class), anyInt())).thenReturn(mockCard);
    initResource();
    Response response = resource.createLibraryCard(authUser, payload);
    String json = response.getEntity().toString();
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    assertNotNull(json);
  }

  @Test
  public void testCreateLibraryCardThrowsIllegalArgumentException() {
    LibraryCard mockCard = mockLibraryCardSetup();
    String payload = new Gson().toJson(mockCard);
    when(userService.findUserByEmail(anyString())).thenReturn(user);
    when(libraryCardService.createLibraryCard(any(LibraryCard.class), anyInt())).thenThrow(new IllegalArgumentException());
    initResource();
    Response response = resource.createLibraryCard(authUser, payload);
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  public void testCreateLibraryCardThrowsConflictException() {
    UnableToExecuteStatementException exception = generateUniqueViolationException();
    String json = new Gson().toJson(mockLibraryCardSetup());
    when(userService.findUserByEmail(anyString())).thenReturn(user);
    when(libraryCardService.createLibraryCard(any(LibraryCard.class), anyInt())).thenThrow(exception);
    initResource();
    Response response = resource.createLibraryCard(authUser, json);
    assertEquals(HttpStatusCodes.STATUS_CODE_CONFLICT, response.getStatus());
  }

  @Test
  public void testUpdateLibraryCard() {
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
  public void testUpdateLibraryCardThrowsIllegalArgumentException() {
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
  public void testUpdateLibraryCardThrowsNotFoundException() {
    when(userService.findUserByEmail(anyString())).thenReturn(user);
    when(libraryCardService.updateLibraryCard(any(LibraryCard.class), anyInt(), anyInt()))
      .thenThrow(new NotFoundException());
    String payload = new Gson().toJson(mockLibraryCardSetup());
    initResource();
    Response response = resource.updateLibraryCard(authUser, 1, payload);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  public void testUpdateLibraryCardThrowsUniqueViolation() {
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
  public void deleteLibraryCard() {
    initResource();
    Response response = resource.deleteLibraryCard(authUser, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_NO_CONTENT, response.getStatus());
  }

  @Test
  public void deleteLibraryCardThrowsNotFoundException() {
    doThrow(new NotFoundException()).when(libraryCardService).deleteLibraryCardById(anyInt());
    initResource();
    Response response = resource.deleteLibraryCard(authUser, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }
}
