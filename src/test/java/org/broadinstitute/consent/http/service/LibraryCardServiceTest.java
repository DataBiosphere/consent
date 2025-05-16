package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.RandomUtils;
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

  @Mock
  private InstitutionDAO institutionDAO;
  @Mock
  private LibraryCardDAO libraryCardDAO;
  @Mock
  private InstitutionService institutionService;
  @Mock
  private UserDAO userDAO;

  @BeforeEach
  void initService() {
    service = new LibraryCardService(libraryCardDAO, institutionDAO, institutionService, userDAO);
  }

  @Test
  // Test Admin LC create with userId and email
  void testCreateLibraryCardFullUserDetailsAsAdmin() {
    Institution institution = testInstitution();
    User user = testUser(institution.getId());
    user.setEmail("testemail");
    User adminUser = createUserWithRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
    when(userDAO.findUserById(user.getUserId())).thenReturn(user);

    LibraryCard payload = testLibraryCard(user.getUserId());
    payload.setUserEmail(user.getEmail());
    payload.setUserName("username");
    payload.setCreateUserId(RandomUtils.nextInt(1, 10));

    //last two calls in the function, no need to test within this service test file
    LibraryCard createdCard = new LibraryCard();
    when(libraryCardDAO.findLibraryCardById(anyInt())).thenReturn(createdCard);

    assertEquals(createdCard, service.createLibraryCard(payload, adminUser));
    verify(libraryCardDAO).insertLibraryCard(eq(user.getUserId()),
        eq(payload.getUserName()), eq(user.getEmail()),
        eq(payload.getCreateUserId()), any());
  }

  @Test
  // Test SO LC create with userId and email success case
  void testCreateLibraryCardFullUserDetailsAsSOSameInstitution() {
    Institution institution = testInstitution();
    User user = testUser(institution.getId());
    user.setEmail("testemail");
    User soUser = createUserWithRole(UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName());
    soUser.setInstitutionId(institution.getId());
    when(userDAO.findUserById(user.getUserId())).thenReturn(user);
    when(institutionDAO.findInstitutionById(institution.getId())).thenReturn(institution);
    when(institutionService.findInstitutionForEmail(user.getEmail())).thenReturn(institution);

    LibraryCard payload = testLibraryCard(user.getUserId());
    payload.setUserEmail(user.getEmail());
    payload.setUserName("username");
    payload.setCreateUserId(randomInt(1, 10));

    //last two calls in the function, no need to test within this service test file
    LibraryCard createdCard = new LibraryCard();
    when(libraryCardDAO.findLibraryCardById(anyInt())).thenReturn(createdCard);

    assertEquals(createdCard, service.createLibraryCard(payload, soUser));
    verify(libraryCardDAO).insertLibraryCard(eq(user.getUserId()),
        eq(payload.getUserName()), eq(user.getEmail()),
        eq(payload.getCreateUserId()), any());
  }

  @Test
  // Test SO LC create with userId and email but in different institutions
  void testCreateLibraryCardFullUserDetailsAsSODifferentInstitution() {
    Institution institution = testInstitution();
    User user = testUser(institution.getId());
    user.setEmail("testemail");
    User soUser = createUserWithRole(UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName());
    // Signing officials should not create LCs for users outside their institution.
    soUser.setInstitutionId(institution.getId()+1);
    when(userDAO.findUserById(user.getUserId())).thenReturn(user);
    when(institutionDAO.findInstitutionById(soUser.getInstitutionId())).thenReturn(new Institution());
    when(institutionService.findInstitutionForEmail(user.getEmail())).thenReturn(institution);

    LibraryCard payload = testLibraryCard(user.getUserId());
    payload.setUserEmail(user.getEmail());
    payload.setUserName("username");
    payload.setCreateUserId(randomInt(1, 10));

    assertThrows(BadRequestException.class, () -> service.createLibraryCard(payload, soUser));
  }

  @Test
  //Test LC create with only user email (no userId)
  void testCreateLibraryCardPartialUserDetailsEmail() {
    Institution institution = testInstitution();
    User user = testUser(institution.getId());
    User adminUser = createUserWithRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
    user.setUserId(null);
    user.setEmail("testemail");

    // last two calls in the function, no need to test within this service test file
    when(libraryCardDAO.findLibraryCardById(anyInt())).thenReturn(new LibraryCard());

    LibraryCard payload = testLibraryCard(user.getUserId());
    payload.setUserEmail(user.getEmail());
    service.createLibraryCard(payload, adminUser);
    verify(libraryCardDAO).insertLibraryCard(eq(null), eq(null), any(), eq(null), any());
  }

  @Test
  //Test LC create with only user id (no email)
  void testCreateLibraryCardPartialUserDetailsId() {
    Institution institution = testInstitution();
    User user = testUser(institution.getId());
    user.setEmail("testemail");
    User adminUser = createUserWithRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());

    when(userDAO.findUserById(user.getUserId())).thenReturn(user);
    when(libraryCardDAO.findLibraryCardsByUserId(user.getUserId())).thenReturn(List.of());

    LibraryCard payload = testLibraryCard(user.getUserId());
    int cardId = 1;
    when(libraryCardDAO.insertLibraryCard(anyInt(), eq(null), any(), eq(null), any()))
        .thenReturn(cardId);
    LibraryCard newCard = new LibraryCard();
    when(libraryCardDAO.findLibraryCardById(cardId)).thenReturn(newCard);

    assertEquals(newCard, service.createLibraryCard(payload, adminUser));
  }

  @Test
  void stubTest() {
    Institution institution = testInstitution();
    User user = testUser(institution.getId());
    user.setEmail("testemail");
    when(userDAO.findUserById(user.getUserId())).thenReturn(user);
    LibraryCard libraryCard = testLibraryCard(user.getUserId());
    libraryCard.setCreateUserId(RandomUtils.nextInt(1, 10));
    when(libraryCardDAO.insertLibraryCard(eq(user.getUserId()), eq(null), eq(user.getEmail()),
        eq(libraryCard.getCreateUserId()), any())).thenReturn(123);
    service.createLibraryCard(libraryCard,
        createUserWithRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName()));
  }

  @Test
  void testCreateLibraryCardAsSO() {
    Institution institution = testInstitution();
    User soUser = createUserWithRole(UserRoles.SIGNINGOFFICIAL.getRoleId(),
        UserRoles.SIGNINGOFFICIAL.getRoleName());
    soUser.setInstitutionId(institution.getId());
    soUser.setEmail("testemail");

    when(userDAO.findUserById(anyInt())).thenReturn(soUser);
    when(institutionDAO.findInstitutionById(anyInt())).thenReturn(institution);
    when(institutionService.findInstitutionForEmail("testemail")).thenReturn(institution);
    when(libraryCardDAO.findLibraryCardsByUserId(anyInt())).thenReturn(Collections.emptyList());

    // last two calls in the function, no need to test within this service test file
    when(libraryCardDAO.insertLibraryCard(anyInt(), eq(null), anyString(), eq(null), any())).thenReturn(1);
    when(libraryCardDAO.findLibraryCardById(anyInt())).thenReturn(new LibraryCard());

    LibraryCard payload = testLibraryCard(soUser.getUserId());
    payload.setUserEmail("testemail");
    service.createLibraryCard(payload, soUser);
  }

  @Test
  //Negative test, checks if error is thrown if payload email and userId don't match up to those on user record
  void testCreateLibraryCardIncorrectUserIdAndEmail() {
    Institution institution = testInstitution();
    User user = testUser(institution.getId());
    User adminUser = createUserWithRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
    user.setUserId(1);
    user.setEmail("testemail");

    when(userDAO.findUserById(anyInt())).thenReturn(user);
    when(libraryCardDAO.findLibraryCardsByUserId(anyInt())).thenReturn(Collections.emptyList());

    LibraryCard payload = testLibraryCard(user.getUserId());
    payload.setUserEmail("differentemail");
    assertThrows(ConsentConflictException.class, () -> service.createLibraryCard(payload, adminUser));
  }

  @Test
  //Negative test, checks to see if error thrown if card already exists on user id and institution id
  void testCreateLibraryCardAlreadyExistsOnUserId() {
    Institution institution = testInstitution();
    User user = testUser(institution.getId());
    User adminUser = createUserWithRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
    LibraryCard savedCard = testLibraryCard(user.getUserId());
    LibraryCard payload = savedCard;

    when(libraryCardDAO.findLibraryCardsByUserId(anyInt())).thenReturn(
        Collections.singletonList(savedCard));
    assertThrows(ConsentConflictException.class, () -> service.createLibraryCard(payload, adminUser));
  }

  @Test
  // Negative test, checks to see if error thrown if card already exists on user email and institution id
  void testCreateLibraryCardAlreadyExistsOnUserEmail() {
    Institution institution = testInstitution();
    User user = testUser(institution.getId());
    User adminUser = createUserWithRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
    user.setEmail("testemail");
    LibraryCard savedCard = testLibraryCard(null);
    savedCard.setUserEmail(user.getEmail());

    LibraryCard payload = savedCard;

    when(libraryCardDAO.findAllLibraryCardsByUserEmail(any())).thenReturn(
        Collections.singletonList(savedCard));
    assertThrows(ConsentConflictException.class, () -> {
      service.createLibraryCard(payload, adminUser);
    });
  }

  @Test
  //Negative test, checks to see if error is thrown if email and userId are not provided
  void testCreateLibraryCardNoUserDetails() {
    User adminUser = createUserWithRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
    LibraryCard payload = testLibraryCard(null);

    assertThrows(BadRequestException.class, () -> {
      service.createLibraryCard(payload, adminUser);
    });
  }

  @Test
  //Negative test, checks if error is thrown on null institutionId
  void testCreateLibraryCard_InvalidInstitution() {
    User user = testUser(1);
    User adminUser = createUserWithRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
    LibraryCard libraryCard = testLibraryCard(user.getUserId());

    assertThrows(BadRequestException.class, () -> service.createLibraryCard(libraryCard, adminUser));
  }

  @Test
  //Negative test, checks to see if error is thrown on null payload
  void testCreateLibraryCardNullPayload() {
    User adminUser = createUserWithRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
    assertThrows(NotFoundException.class, () -> service.createLibraryCard(null, adminUser));
  }

  @Test
  void testCreateLibraryCard_InvalidInstitutionId() {
    User soUser = createUserWithRole(UserRoles.SIGNINGOFFICIAL.getRoleId(),
        UserRoles.SIGNINGOFFICIAL.getRoleName());
    soUser.setInstitutionId(1);
    LibraryCard card = testLibraryCard(2);
    assertThrows(BadRequestException.class, () -> service.createLibraryCard(card, soUser));
  }

  @Test
  void testCreateLibraryCard_InstitutionMismatch() {
    User soUser = createUserWithRole(UserRoles.SIGNINGOFFICIAL.getRoleId(),
        UserRoles.SIGNINGOFFICIAL.getRoleName());
    soUser.setInstitutionId(1);
    LibraryCard card = testLibraryCard(2);
    assertThrows(BadRequestException.class, () -> {
      service.createLibraryCard(card, soUser);
    });
  }

  @Test
  void testUpdateLibraryCard() {
    Institution institution = testInstitution();
    User user = testUser(institution.getId());
    LibraryCard libraryCard = testLibraryCard(user.getUserId());
    when(libraryCardDAO.findLibraryCardById(libraryCard.getId())).thenReturn(libraryCard);
    when(userDAO.findUserById(user.getUserId())).thenReturn(user);
    doNothing().when(libraryCardDAO)
        .updateLibraryCardById(any(), any(), any(), any(), any(), any());

    LibraryCard resultCard = service.updateLibraryCard(libraryCard, libraryCard.getId(), 1);
    assertNotNull(resultCard);
    assertEquals(resultCard.getId(), libraryCard.getId());
  }

  @Test
  void testUpdateLibraryCard_NotFound() {
    Institution institution = testInstitution();
    User user = testUser(institution.getId());
    LibraryCard libraryCard = testLibraryCard(user.getUserId());

    assertThrows(NotFoundException.class, () -> {
      service.updateLibraryCard(libraryCard, libraryCard.getId(), 1);
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
    when(libraryCardDAO.findLibraryCardById(any()))
        .thenReturn(null);
    assertThrows(NotFoundException.class, () -> {
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
    int daaId1 = RandomUtils.nextInt(1, 10);
    int daaId2 = RandomUtils.nextInt(1, 10);
    daa1.setDaaId(daaId1);
    DataAccessAgreement daa2 = new DataAccessAgreement();
    daa2.setDaaId(daaId2);
    libraryCard.addDaaObject(daa1);
    libraryCard.addDaaObject(daa2);
    when(libraryCardDAO.findLibraryCardDaaById(libraryCard.getId()))
        .thenReturn(libraryCard);
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
  void testAddDaaToUserLibraryCardByInstitution() {
    User user = testUser(1);
    user.setRoles(List.of(new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName())));
    User signingOfficial = createUserWithRole(UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName());
    signingOfficial.setInstitutionId(1);
    Integer userId = user.getUserId();
    List<LibraryCard> libraryCards = List.of(
        testLibraryCard(userId),
        testLibraryCard(userId)
    );
    when(libraryCardDAO.findLibraryCardsByUserId(user.getUserId()))
        .thenReturn(libraryCards);
    doNothing().when(libraryCardDAO).createLibraryCardDaaRelation(any(), any());
    List<LibraryCard> cards = service.addDaaToUserLibraryCardByInstitution(user, signingOfficial, 1);
    assertEquals(2, cards.size());
  }

  @Test
  void testAddDaaToUserLibraryCardByInstitutionNoMatchingInstitutions() {
    User user = testUser(1);
    user.setRoles(List.of(new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName())));
    User signingOfficial = createUserWithRole(UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName());
    signingOfficial.setInstitutionId(4);
    assertThrows(BadRequestException.class, () -> service.addDaaToUserLibraryCardByInstitution(user, signingOfficial, 1));
  }

  @Test
  void testAddDaaToUserLibraryCardWithNoLibraryCards() {
    Institution institution = testInstitution();
    User user = testUser(institution.getId());
    user.setRoles(List.of(new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName())));
    User signingOfficial = createUserWithRole(UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName());
    signingOfficial.setInstitutionId(institution.getId());
    Integer userId = user.getUserId();
    LibraryCard payload = testLibraryCard(user.getUserId());
    payload.setUserEmail("testemail");
    // There are two calls to findLibraryCardsByUserId for checks before creation
    when(libraryCardDAO.findLibraryCardsByUserId(userId))
        .thenReturn(List.of())
        .thenReturn(List.of())
        .thenReturn(List.of(payload));
    when(institutionDAO.findInstitutionById(institution.getId())).thenReturn(institution);
    when(institutionService.findInstitutionForEmail(any())).thenReturn(institution);
    when(userDAO.findUserById(user.getUserId())).thenReturn(user);
    when(libraryCardDAO.insertLibraryCard(anyInt(), any(), any(), anyInt(), any()))
        .thenReturn(1);
    when(libraryCardDAO.findLibraryCardById(anyInt())).thenReturn(new LibraryCard());

    List<LibraryCard> cards = service.addDaaToUserLibraryCardByInstitution(user, signingOfficial, 1);
    assertEquals(1, cards.size());
  }

  @Test
  void testAddDaaToUserLibraryCardByInstitutionSigningOfficialNoInstitution() {
    User user = testUser(1);
    user.setRoles(List.of(new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName())));
    User signingOfficial = new User();
    signingOfficial.setRoles(List.of(new UserRole(UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName())));
    assertThrows(BadRequestException.class, () -> service.addDaaToUserLibraryCardByInstitution(user, signingOfficial, 1));
  }

  @Test
  void testRemoveDaaFromUserLibraryCards() {
    User user = testUser(1);
    Integer userId = user.getUserId();
    List<LibraryCard> libraryCards = List.of(
        testLibraryCard(userId),
        testLibraryCard(userId),
        testLibraryCard(userId),
        testLibraryCard(userId)
    );
    when(libraryCardDAO.findLibraryCardsByUserId(user.getUserId()))
        .thenReturn(libraryCards);
    doNothing().when(libraryCardDAO).deleteLibraryCardDaaRelation(any(), any());
    List<LibraryCard> cards = service.removeDaaFromUserLibraryCards(user, 1);
    // The above deletion only affects the lc-daa join table and does not remove library cards
    assertEquals(libraryCards.size(), cards.size());
  }

  @Test
  void testRemoveDaaFromUserLibraryCardsNoMatchingInstitutions() {
    User user = testUser(1);
    Integer userId = user.getUserId();
    List<LibraryCard> libraryCards = List.of(
        testLibraryCard(userId),
        testLibraryCard(userId),
        testLibraryCard(userId),
        testLibraryCard(userId)
    );
    when(libraryCardDAO.findLibraryCardsByUserId(user.getUserId())).thenReturn(libraryCards);
    List<LibraryCard> cards = service.removeDaaFromUserLibraryCards(user, 1);
    // DAA removal should not delete library cards
    assertEquals(libraryCards.size(), cards.size());
  }

  @Test
  void testCreateLibraryCardForSigningOfficial() {
    Institution institution = testInstitution();
    User user = createUserWithRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName());
    user.setInstitutionId(institution.getId());
    User signingOfficial = createUserWithRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
    signingOfficial.setInstitutionId(institution.getId());
    user.setEmail("testemail");
    LibraryCard newLc = new LibraryCard();
    newLc.setId(1);

    when(userDAO.findUserById(anyInt())).thenReturn(user);
    when(institutionDAO.findInstitutionById(anyInt())).thenReturn(institution);
    when(institutionService.findInstitutionForEmail(user.getEmail())).thenReturn(institution);
    when(libraryCardDAO.findLibraryCardsByUserId(anyInt())).thenReturn(Collections.emptyList());

    when(libraryCardDAO.insertLibraryCard(anyInt(), any(), any(), anyInt(),
        any())).thenReturn(1);
    when(libraryCardDAO.findLibraryCardById(anyInt())).thenReturn(newLc);

    LibraryCard card = service.createLibraryCardForSigningOfficial(user, signingOfficial);
    assertNotNull(card);
    assertEquals(card.getId(), newLc.getId());
  }

  @Test
  void testRemoveDaaFromUserLibraryCardByInstitutionNoLibraryCards() {
    User user = testUser(1);
    Integer userId = user.getUserId();
    when(libraryCardDAO.findLibraryCardsByUserId(userId))
        .thenReturn(Collections.emptyList());
    List<LibraryCard> cards = service.removeDaaFromUserLibraryCards(user,1);
    assertEquals(0, cards.size());
  }

  private User testUser(Integer institutionId) {
    User user = new User();
    user.setUserId(RandomUtils.nextInt(1, 10));
    user.setInstitutionId(institutionId);
    return user;
  }

  private LibraryCard testLibraryCard(Integer userId) {
    LibraryCard libraryCard = new LibraryCard();
    libraryCard.setId(RandomUtils.nextInt(1, 10));
    libraryCard.setUserId(userId);

    return libraryCard;
  }

  private Institution testInstitution() {
    Institution institution = new Institution();
    institution.setId(RandomUtils.nextInt(1, 10));
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