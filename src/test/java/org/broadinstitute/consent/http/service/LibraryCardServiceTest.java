package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import freemarker.template.TemplateException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.io.IOException;
import java.util.List;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.db.LibraryCardDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.exceptions.ConsentConflictException;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LibraryCardServiceTest extends AbstractTestHelper {

  private LibraryCardService service;

  @Mock private InstitutionDAO institutionDAO;
  @Mock private LibraryCardDAO libraryCardDAO;
  @Mock private InstitutionService institutionService;
  @Mock private UserDAO userDAO;
  @Mock private EmailService emailService;

  @BeforeEach
  void initService() {
    service =
        new LibraryCardService(
            libraryCardDAO, institutionDAO, institutionService, userDAO, emailService);
  }

  @Test
  // Test SO LC create with userId and email success case
  void testCreateLibraryCardFullUserDetailsAsSOSameInstitution()
      throws TemplateException, IOException {
    Institution institution = testInstitution();
    User user = testUser(institution.getId());
    user.setEmail("testemail");
    User soUser =
        createUserWithRole(
            UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName());
    soUser.setInstitutionId(institution.getId());
    when(userDAO.findUserById(user.getUserId())).thenReturn(user);
    when(userDAO.findUserByEmail(user.getEmail())).thenReturn(user);
    when(institutionDAO.findInstitutionById(institution.getId())).thenReturn(institution);
    when(institutionService.findInstitutionForEmail(user.getEmail())).thenReturn(institution);
    doThrow(IOException.class).when(emailService).sendNewLibraryCardIssuedMessage(user);
    LibraryCard payload = testLibraryCard(user.getUserId());
    payload.setUserEmail(user.getEmail());
    payload.setUserName("username");
    payload.setCreateUserId(randomInt(1, 10));

    // last two calls in the function, no need to test within this service test file
    LibraryCard createdCard = new LibraryCard();
    when(libraryCardDAO.findLibraryCardById(anyInt())).thenReturn(createdCard);

    assertEquals(createdCard, service.createLibraryCard(payload, soUser));
    verify(libraryCardDAO)
        .insertLibraryCard(
            eq(user.getUserId()),
            eq(payload.getUserName()),
            eq(user.getEmail()),
            eq(payload.getCreateUserId()),
            any());
  }

  @Test
  // Test SO LC create with userId and email but in different institutions
  void testCreateLibraryCardFullUserDetailsAsSODifferentInstitution() {
    Institution institution = testInstitution();
    User user = testUser(institution.getId());
    user.setEmail("testemail");
    User soUser =
        createUserWithRole(
            UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName());
    // Signing officials should not create LCs for users outside their institution.
    soUser.setInstitutionId(institution.getId() + 1);
    when(userDAO.findUserById(user.getUserId())).thenReturn(user);
    when(institutionDAO.findInstitutionById(soUser.getInstitutionId()))
        .thenReturn(new Institution());
    when(institutionService.findInstitutionForEmail(user.getEmail())).thenReturn(institution);

    LibraryCard payload = testLibraryCard(user.getUserId());
    payload.setUserEmail(user.getEmail());
    payload.setUserName("username");
    payload.setCreateUserId(randomInt(1, 10));

    assertThrows(BadRequestException.class, () -> service.createLibraryCard(payload, soUser));
  }

  @Test
  // Test LC create with only user email (no userId)
  void testCreateLibraryCardPartialUserDetailsEmailThrowsBadRequest() {
    Institution institution = testInstitution();
    User user = testUser(institution.getId());
    user.setUserId(null);
    user.setEmail("testemail");

    User signingOfficialUser =
        createUserWithRole(
            UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName());

    LibraryCard payload = testLibraryCard(user.getUserId());
    payload.setUserEmail(user.getEmail());
    assertThrows(
        BadRequestException.class, () -> service.createLibraryCard(payload, signingOfficialUser));
  }

  @Test
  // Test LC create with only user id (no email)
  void testCreateLibraryCardPartialUserDetailsIdThrowsBadRequest() {
    Institution institution = testInstitution();
    User user = testUser(institution.getId());
    user.setEmail(null);
    user.setEmail("testemail");

    User signingOfficialUser =
        createUserWithRole(
            UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName());

    LibraryCard payload = testLibraryCard(user.getUserId());
    assertThrows(
        BadRequestException.class, () -> service.createLibraryCard(payload, signingOfficialUser));
  }

  @Test
  void testCreateLibraryCardAsSO() {
    Institution institution = testInstitution();
    User soUser =
        createUserWithRole(
            UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName());
    soUser.setInstitutionId(institution.getId());
    soUser.setEmail("testemail");

    when(userDAO.findUserById(anyInt())).thenReturn(soUser);
    when(institutionDAO.findInstitutionById(anyInt())).thenReturn(institution);
    when(institutionService.findInstitutionForEmail("testemail")).thenReturn(institution);
    when(libraryCardDAO.findLibraryCardByUserId(anyInt())).thenReturn(null);

    // last two calls in the function, no need to test within this service test file
    when(libraryCardDAO.insertLibraryCard(anyInt(), eq(null), anyString(), eq(null), any()))
        .thenReturn(1);
    when(libraryCardDAO.findLibraryCardById(anyInt())).thenReturn(new LibraryCard());

    LibraryCard payload = testLibraryCard(soUser.getUserId());
    payload.setUserEmail("testemail");
    service.createLibraryCard(payload, soUser);
  }

  @Test
  // Negative test, checks if error is thrown if payload email and userId don't match up to those on
  // user record
  void testCreateLibraryCardIncorrectUserIdAndEmail() {
    Institution institution = testInstitution();
    User user = testUser(institution.getId());
    User signingOfficialUser =
        createUserWithRole(
            UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName());
    user.setUserId(1);
    user.setEmail("testemail");

    when(userDAO.findUserById(anyInt())).thenReturn(user);
    when(libraryCardDAO.findLibraryCardByUserId(anyInt())).thenReturn(null);

    LibraryCard payload = testLibraryCard(user.getUserId());
    payload.setUserEmail("differentemail");
    assertThrows(
        ConsentConflictException.class,
        () -> service.createLibraryCard(payload, signingOfficialUser));
  }

  @Test
  // Negative test, checks to see if error thrown if card already exists on user id and institution
  // id
  void testCreateLibraryCardAlreadyExistsOnUserId() {
    Institution institution = testInstitution();
    User user = testUser(institution.getId());
    User signingOfficialUser =
        createUserWithRole(
            UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName());
    LibraryCard savedCard = testLibraryCard(user.getUserId());
    LibraryCard payload = savedCard;

    when(libraryCardDAO.findLibraryCardByUserId(anyInt())).thenReturn(savedCard);
    assertThrows(
        ConsentConflictException.class,
        () -> service.createLibraryCard(payload, signingOfficialUser));
  }

  @Test
  // Negative test, checks to see if error thrown if card already exists on user email and
  // institution id
  void testCreateLibraryCardAlreadyExistsOnUserEmail() {
    Institution institution = testInstitution();
    User user = testUser(institution.getId());
    User signingOfficialUser =
        createUserWithRole(
            UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName());
    user.setEmail("testemail");
    LibraryCard savedCard = testLibraryCard(null);
    savedCard.setUserEmail(user.getEmail());

    LibraryCard payload = savedCard;

    when(libraryCardDAO.findLibraryCardByUserEmail(any())).thenReturn(savedCard);
    assertThrows(
        ConsentConflictException.class,
        () -> {
          service.createLibraryCard(payload, signingOfficialUser);
        });
  }

  @Test
  // Negative test, checks to see if error is thrown if email and userId are not provided
  void testCreateLibraryCardNoUserDetails() {
    User signingOfficialUser =
        createUserWithRole(
            UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName());
    LibraryCard payload = testLibraryCard(null);

    assertThrows(
        BadRequestException.class,
        () -> {
          service.createLibraryCard(payload, signingOfficialUser);
        });
  }

  @Test
  // Negative test, checks if error is thrown on null institutionId
  void testCreateLibraryCard_InvalidInstitution() {
    User user = testUser(1);
    User signingOfficialUser =
        createUserWithRole(
            UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName());
    LibraryCard libraryCard = testLibraryCard(user.getUserId());

    assertThrows(
        BadRequestException.class,
        () -> service.createLibraryCard(libraryCard, signingOfficialUser));
  }

  @Test
  // Negative test, checks to see if error is thrown on null payload
  void testCreateLibraryCardNullPayload() {
    User signingOfficialUser =
        createUserWithRole(
            UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName());
    assertThrows(
        NotFoundException.class, () -> service.createLibraryCard(null, signingOfficialUser));
  }

  @Test
  void testCreateLibraryCard_InvalidInstitutionId() {
    User soUser =
        createUserWithRole(
            UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName());
    soUser.setInstitutionId(1);
    LibraryCard card = testLibraryCard(2);
    assertThrows(BadRequestException.class, () -> service.createLibraryCard(card, soUser));
  }

  @Test
  void testCreateLibraryCard_InstitutionMismatch() {
    User soUser =
        createUserWithRole(
            UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName());
    soUser.setInstitutionId(1);
    LibraryCard card = testLibraryCard(2);
    assertThrows(
        BadRequestException.class,
        () -> {
          service.createLibraryCard(card, soUser);
        });
  }

  @Test
  void testDeleteLibraryCard_NotFound() {
    Institution institution = testInstitution();
    User user = testUser(institution.getId());
    user.setAdminRole();
    LibraryCard libraryCard = testLibraryCard(user.getUserId());

    assertThrows(NotFoundException.class, () -> service.deleteLibraryCardById(libraryCard.getId()));
  }

  @Test
  void testFindLibraryCardById_NotFound() {
    when(libraryCardDAO.findLibraryCardById(any())).thenReturn(null);
    assertThrows(
        NotFoundException.class,
        () -> {
          service.findLibraryCardById(1);
        });
  }

  @Test
  void testFindLibraryCardById() {
    LibraryCard libraryCard = testLibraryCard(1);
    when(libraryCardDAO.findLibraryCardById(libraryCard.getId())).thenReturn(libraryCard);
    LibraryCard result = service.findLibraryCardById(libraryCard.getId());
    assertNotNull(result);
    assertEquals(result.getId(), libraryCard.getId());
  }

  @Test
  void testFindLibraryCardDaaById_NotFound() {
    assertThrows(NotFoundException.class, () -> service.findLibraryCardWithDaasById(1));
  }

  @Test
  void testFindLibraryCardByIdDaa() {
    LibraryCard libraryCard = testLibraryCard(1);
    DataAccessAgreement daa1 = new DataAccessAgreement();
    int daaId1 = randomInt(1, 10);
    int daaId2 = randomInt(1, 10);
    daa1.setDaaId(daaId1);
    DataAccessAgreement daa2 = new DataAccessAgreement();
    daa2.setDaaId(daaId2);
    libraryCard.addDaaObject(daa1);
    libraryCard.addDaaObject(daa2);
    when(libraryCardDAO.findLibraryCardDaaById(libraryCard.getId())).thenReturn(libraryCard);
    LibraryCard result = service.findLibraryCardWithDaasById(libraryCard.getId());
    assertNotNull(result);
    assertEquals(result.getId(), libraryCard.getId());
    assertEquals(result.getDaas(), List.of(daa1, daa2));
    assertEquals(result.getDaas().get(0).getDaaId(), daaId1);
    assertEquals(result.getDaas().get(1).getDaaId(), daaId2);
  }

  @Test
  void testAddDaaToLibraryCard() {
    doNothing().when(libraryCardDAO).createLibraryCardDaaRelation(any(), any());

    LibraryCard libraryCard = testLibraryCard(1);
    assertDoesNotThrow(() -> service.addDaaToLibraryCard(libraryCard.getId(), 1));
  }

  @Test
  void testRemoveDaaFromLibraryCard() {
    doNothing().when(libraryCardDAO).deleteLibraryCardDaaRelation(any(), any());

    LibraryCard libraryCard = testLibraryCard(1);
    assertDoesNotThrow(() -> service.removeDaaFromLibraryCard(libraryCard.getId(), 1));
  }

  @Test
  void testAddDaaToUserLibraryCard() {
    User user = testUser(1);
    user.setRoles(
        List.of(
            new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName())));
    User signingOfficial =
        createUserWithRole(
            UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName());
    signingOfficial.setInstitutionId(1);
    Integer userId = user.getUserId();
    when(libraryCardDAO.findLibraryCardByUserId(user.getUserId()))
        .thenReturn(testLibraryCard(userId));
    doNothing().when(libraryCardDAO).createLibraryCardDaaRelation(any(), any());
    LibraryCard card = service.addDaaToUserLibraryCard(user, signingOfficial, 1);
    assertNotNull(card);
  }

  @Test
  void testAddDaaToUserLibraryCardNoMatchingInstitutions() {
    User user = testUser(1);
    user.setRoles(
        List.of(
            new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName())));
    User signingOfficial =
        createUserWithRole(
            UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName());
    signingOfficial.setInstitutionId(4);
    assertThrows(
        BadRequestException.class, () -> service.addDaaToUserLibraryCard(user, signingOfficial, 1));
  }

  @Test
  void testAddDaaToUserLibraryCardWithNoLibraryCards() {
    Institution institution = testInstitution();
    User user = testUser(institution.getId());
    user.setRoles(
        List.of(
            new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName())));
    User signingOfficial =
        createUserWithRole(
            UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName());
    signingOfficial.setInstitutionId(institution.getId());
    Integer userId = user.getUserId();
    LibraryCard payload = testLibraryCard(user.getUserId());
    payload.setUserEmail("testemail");
    // There are two calls to findLibraryCardsByUserId for checks before creation
    when(libraryCardDAO.findLibraryCardByUserId(userId))
        .thenReturn(null)
        .thenReturn(null)
        .thenReturn(payload);
    when(institutionDAO.findInstitutionById(institution.getId())).thenReturn(institution);
    when(institutionService.findInstitutionForEmail(any())).thenReturn(institution);
    when(userDAO.findUserById(user.getUserId())).thenReturn(user);
    when(libraryCardDAO.insertLibraryCard(anyInt(), any(), any(), anyInt(), any())).thenReturn(1);
    when(libraryCardDAO.findLibraryCardById(anyInt())).thenReturn(new LibraryCard());

    LibraryCard card = service.addDaaToUserLibraryCard(user, signingOfficial, 1);
    assertNotNull(card);
  }

  @Test
  void testRemoveDaaFromUserLibraryCards() {
    User user = testUser(1);
    Integer userId = user.getUserId();
    when(libraryCardDAO.findLibraryCardByUserId(user.getUserId()))
        .thenReturn(testLibraryCard(userId));
    doNothing().when(libraryCardDAO).deleteLibraryCardDaaRelation(any(), any());
    LibraryCard card = service.removeDaaFromUserLibraryCard(user, 1);
    // The above deletion only affects the lc-daa join table and does not remove library cards
    assertNotNull(card);
    assertTrue(card.getDaaIds().isEmpty());
  }

  @Test
  void testRemoveDaaFromUserLibraryCardsNoMatchingInstitutions() {
    User user = testUser(1);
    Integer userId = user.getUserId();
    when(libraryCardDAO.findLibraryCardByUserId(user.getUserId()))
        .thenReturn(testLibraryCard(userId));
    LibraryCard card = service.removeDaaFromUserLibraryCard(user, 1);
    // DAA removal should not delete library cards
    assertNotNull(card);
    assertTrue(card.getDaaIds().isEmpty());
  }

  @Test
  void testCreateLibraryCardForSigningOfficial() throws TemplateException, IOException {
    Institution institution = testInstitution();
    User user =
        createUserWithRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName());
    user.setInstitutionId(institution.getId());
    User signingOfficial =
        createUserWithRole(
            UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName());
    signingOfficial.setInstitutionId(institution.getId());
    user.setEmail("testemail");
    LibraryCard newLc = new LibraryCard();
    newLc.setId(1);

    when(userDAO.findUserById(anyInt())).thenReturn(user);
    when(userDAO.findUserByEmail(user.getEmail())).thenReturn(user);
    when(institutionDAO.findInstitutionById(anyInt())).thenReturn(institution);
    when(institutionService.findInstitutionForEmail(user.getEmail())).thenReturn(institution);
    when(libraryCardDAO.findLibraryCardByUserId(anyInt())).thenReturn(null);

    when(libraryCardDAO.insertLibraryCard(anyInt(), any(), any(), anyInt(), any())).thenReturn(1);
    when(libraryCardDAO.findLibraryCardById(anyInt())).thenReturn(newLc);

    LibraryCard card = service.createLibraryCardForSigningOfficial(user, signingOfficial);
    assertNotNull(card);
    assertEquals(card.getId(), newLc.getId());
    verify(emailService).sendNewLibraryCardIssuedMessage(user);
  }

  @Test
  void testRemoveDaaFromUserLibraryCardByInstitutionNoLibraryCards() {
    User user = testUser(1);
    Integer userId = user.getUserId();
    when(libraryCardDAO.findLibraryCardByUserId(userId)).thenReturn(null);
    LibraryCard card = service.removeDaaFromUserLibraryCard(user, 1);
    assertNull(card);
  }

  private User testUser(Integer institutionId) {
    User user = new User();
    user.setUserId(randomInt(1, 10));
    user.setEmail("testemail");
    user.setInstitutionId(institutionId);
    return user;
  }

  private LibraryCard testLibraryCard(Integer userId) {
    LibraryCard libraryCard = new LibraryCard();
    libraryCard.setId(randomInt(1, 10));
    libraryCard.setUserId(userId);

    return libraryCard;
  }

  private Institution testInstitution() {
    Institution institution = new Institution();
    institution.setId(randomInt(1, 10));
    institution.setName("Test Institution");

    return institution;
  }

  private User createUserWithRole(Integer id, String name) {
    Institution institution = testInstitution();
    User user = testUser(institution.getId());
    user.addRole(new UserRole(id, name));
    return user;
  }
}
