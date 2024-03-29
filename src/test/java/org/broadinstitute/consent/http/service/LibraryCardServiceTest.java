package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.util.Collections;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.db.LibraryCardDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.exceptions.ConsentConflictException;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class LibraryCardServiceTest {

  private LibraryCardService service;

  @Mock
  private InstitutionDAO institutionDAO;
  @Mock
  private LibraryCardDAO libraryCardDAO;
  @Mock
  private UserDAO userDAO;

  @BeforeEach
  public void setUp() {
    openMocks(this);
  }

  private void initService() {
    this.service = new LibraryCardService(libraryCardDAO, institutionDAO, userDAO);
  }

  @Test
  // Test LC create with userId and email
  public void testCreateLibraryCardFullUserDetails() {
    initService();
    Institution institution = testInstitution();
    User user = testUser(institution.getId());
    User adminUser = createUserWithRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
    user.setEmail("testemail");
    when(userDAO.findUserById(anyInt())).thenReturn(user);
    when(institutionDAO.findInstitutionById(anyInt())).thenReturn(institution);
    when(libraryCardDAO.findLibraryCardsByUserId(anyInt())).thenReturn(Collections.emptyList());

    //last two calls in the function, no need to test within this service test file
    when(libraryCardDAO.insertLibraryCard(anyInt(), anyInt(), any(), any(), any(), anyInt(),
        any())).thenReturn(1);
    when(libraryCardDAO.findLibraryCardById(anyInt())).thenReturn(new LibraryCard());

    LibraryCard payload = testLibraryCard(institution.getId(), user.getUserId());
    payload.setUserEmail(user.getEmail());
    service.createLibraryCard(payload, adminUser);
  }

  @Test
  //Test LC create with only user email (no userId)
  public void testCreateLibraryCardPartialUserDetailsEmail() {
    initService();
    Institution institution = testInstitution();
    User user = testUser(institution.getId());
    User adminUser = createUserWithRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
    user.setUserId(null);
    user.setEmail("testemail");

    when(userDAO.findUserById(anyInt())).thenReturn(user);
    when(institutionDAO.findInstitutionById(anyInt())).thenReturn(institution);
    when(libraryCardDAO.findLibraryCardsByUserId(anyInt())).thenReturn(Collections.emptyList());

    // last two calls in the function, no need to test within this service test file
    when(libraryCardDAO.insertLibraryCard(anyInt(), anyInt(), any(), any(), any(), anyInt(), any()))
        .thenReturn(1);
    when(libraryCardDAO.findLibraryCardById(anyInt())).thenReturn(new LibraryCard());

    LibraryCard payload = testLibraryCard(institution.getId(), user.getUserId());
    payload.setUserEmail(user.getEmail());
    service.createLibraryCard(payload, adminUser);
  }

  @Test
  //Test LC create with only user id (no email)
  public void testCreateLibraryCardPartialUserDetailsId() {
    initService();
    Institution institution = testInstitution();
    User user = testUser(institution.getId());
    User adminUser = createUserWithRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
    user.setEmail(null);

    when(userDAO.findUserById(anyInt())).thenReturn(user);
    when(institutionDAO.findInstitutionById(anyInt())).thenReturn(institution);
    when(libraryCardDAO.findLibraryCardsByUserId(anyInt())).thenReturn(Collections.emptyList());

    // last two calls in the function, no need to test within this service test file
    when(libraryCardDAO.insertLibraryCard(anyInt(), anyInt(), any(), any(), any(), anyInt(), any()))
        .thenReturn(1);
    when(libraryCardDAO.findLibraryCardById(anyInt())).thenReturn(new LibraryCard());

    LibraryCard payload = testLibraryCard(institution.getId(), user.getUserId());
    service.createLibraryCard(payload, adminUser);
  }

  @Test
  public void testCreateLibraryCardAsSO() {
    initService();
    Institution institution = testInstitution();
    User soUser = createUserWithRole(UserRoles.SIGNINGOFFICIAL.getRoleId(),
        UserRoles.SIGNINGOFFICIAL.getRoleName());
    soUser.setInstitutionId(institution.getId());

    when(userDAO.findUserById(anyInt())).thenReturn(soUser);
    when(institutionDAO.findInstitutionById(anyInt())).thenReturn(institution);
    when(libraryCardDAO.findLibraryCardsByUserId(anyInt())).thenReturn(Collections.emptyList());

    // last two calls in the function, no need to test within this service test file
    when(libraryCardDAO.insertLibraryCard(anyInt(), anyInt(), any(), any(), any(), anyInt(), any()))
        .thenReturn(1);
    when(libraryCardDAO.findLibraryCardById(anyInt())).thenReturn(new LibraryCard());

    LibraryCard payload = testLibraryCard(institution.getId(), soUser.getUserId());
    payload.setUserEmail("testemail");
    service.createLibraryCard(payload, soUser);
  }

  @Test
  //Negative test, checks if error is thrown if payload email and userId don't match up to those on user record
  public void testCreateLibraryCardIncorrectUserIdAndEmail() {
    initService();
    Institution institution = testInstitution();
    User user = testUser(institution.getId());
    User adminUser = createUserWithRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
    user.setUserId(1);
    user.setEmail("testemail");

    when(userDAO.findUserById(anyInt())).thenReturn(user);
    when(institutionDAO.findInstitutionById(anyInt())).thenReturn(institution);
    when(libraryCardDAO.findLibraryCardsByUserId(anyInt())).thenReturn(Collections.emptyList());

    when(libraryCardDAO.insertLibraryCard(anyInt(), anyInt(), any(), any(), any(), anyInt(), any()))
        .thenReturn(1);
    when(libraryCardDAO.findLibraryCardById(anyInt())).thenReturn(new LibraryCard());

    LibraryCard payload = testLibraryCard(institution.getId(), user.getUserId());
    payload.setUserEmail("differentemail");
    assertThrows(ConsentConflictException.class, () -> {
      service.createLibraryCard(payload, adminUser);
    });
  }

  @Test
  //Negative test, checks to see if error thrown if card already exists on user id and institution id
  public void testCreateLibraryCardAlreadyExistsOnUserId() {
    initService();
    Institution institution = testInstitution();
    User user = testUser(institution.getId());
    User adminUser = createUserWithRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
    LibraryCard savedCard = testLibraryCard(institution.getId(), user.getUserId());
    LibraryCard payload = savedCard;

    when(institutionDAO.findInstitutionById(anyInt())).thenReturn(institution);
    when(libraryCardDAO.findLibraryCardsByUserId(anyInt())).thenReturn(
        Collections.singletonList(savedCard));
    assertThrows(ConsentConflictException.class, () -> {
      service.createLibraryCard(payload, adminUser);
    });
  }

  @Test
  // Negative test, checks to see if error thrown if card already exists on user email and institution id
  public void testCreateLibraryCardAlreadyExistsOnUserEmail() {
    initService();
    Institution institution = testInstitution();
    User user = testUser(institution.getId());
    User adminUser = createUserWithRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
    user.setEmail("testemail");
    LibraryCard savedCard = testLibraryCard(institution.getId(), null);
    savedCard.setUserEmail(user.getEmail());

    LibraryCard payload = savedCard;

    when(institutionDAO.findInstitutionById(anyInt())).thenReturn(institution);
    when(libraryCardDAO.findAllLibraryCardsByUserEmail(any())).thenReturn(
        Collections.singletonList(savedCard));
    assertThrows(ConsentConflictException.class, () -> {
      service.createLibraryCard(payload, adminUser);
    });
  }

  @Test
  //Negative test, checks to see if error is thrown if email and userId are not provided
  public void testCreateLibraryCardNoUserDetails() {
    User adminUser = createUserWithRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
    initService();
    Institution institution = testInstitution();
    LibraryCard payload = testLibraryCard(institution.getId(), null);
    when(institutionDAO.findInstitutionById(anyInt())).thenReturn(institution);

    assertThrows(BadRequestException.class, () -> {
      service.createLibraryCard(payload, adminUser);
    });
  }

  @Test
  //Negative test, checks if error is thrown on null institutionId
  public void testCreateLibraryCard_InvalidInstitution() {
    User user = testUser(1);
    User adminUser = createUserWithRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
    LibraryCard libraryCard = testLibraryCard(1, user.getUserId());

    when(libraryCardDAO.findLibraryCardById(libraryCard.getId()))
        .thenReturn(libraryCard);
    when(institutionDAO.findInstitutionById(libraryCard.getInstitutionId()))
        .thenReturn(null);
    when(userDAO.findUserById(user.getUserId()))
        .thenReturn(user);

    initService();
    assertThrows(IllegalArgumentException.class, () -> {
      service.createLibraryCard(libraryCard, adminUser);
    });
  }

  @Test
  //Negative test, checks to see if error is thrown on null payload
  public void testCreateLibraryCardNullPayload() {
    User adminUser = createUserWithRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
    initService();
    assertThrows(NotFoundException.class, () -> {
      service.createLibraryCard(null, adminUser);
    });
  }

  @Test
  public void testCreateLibraryCard_InvalidInstitutionId() {
    User soUser = createUserWithRole(UserRoles.SIGNINGOFFICIAL.getRoleId(),
        UserRoles.SIGNINGOFFICIAL.getRoleName());
    soUser.setInstitutionId(1);
    LibraryCard card = testLibraryCard(2, 2);
    card.setInstitutionId(2);
    initService();
    assertThrows(BadRequestException.class, () -> {
      service.createLibraryCard(card, soUser);
    });
  }

  @Test
  public void testUpdateLibraryCard() {
    Institution institution = testInstitution();
    User user = testUser(institution.getId());
    LibraryCard libraryCard = testLibraryCard(institution.getId(), user.getUserId());
    when(institutionDAO.findInstitutionById(libraryCard.getInstitutionId()))
        .thenReturn(institution);
    when(libraryCardDAO.findLibraryCardById(libraryCard.getId()))
        .thenReturn(libraryCard);
    when(userDAO.findUserById(user.getUserId()))
        .thenReturn(user);
    doNothing().when(libraryCardDAO)
        .updateLibraryCardById(any(), any(), any(), any(), any(), any(), any(), any());

    initService();
    LibraryCard resultCard = service.updateLibraryCard(libraryCard, libraryCard.getId(), 1);
    assertNotNull(resultCard);
    assertEquals(resultCard.getId(), libraryCard.getId());
  }

  @Test
  public void testUpdateLibraryCard_NotFound() {
    Institution institution = testInstitution();
    User user = testUser(institution.getId());
    LibraryCard libraryCard = testLibraryCard(institution.getId(), user.getUserId());
    when(institutionDAO.findInstitutionById(libraryCard.getInstitutionId()))
        .thenReturn(institution);
    when(userDAO.findUserById(user.getUserId()))
        .thenReturn(user);
    when(libraryCardDAO.findLibraryCardById(libraryCard.getId()))
        .thenReturn(null);

    initService();
    assertThrows(NotFoundException.class, () -> {
      service.updateLibraryCard(libraryCard, libraryCard.getId(), 1);
    });
  }

  @Test
  public void testUpdateLibraryCard_InvalidInstitution() {
    User user = testUser(1);
    LibraryCard libraryCard = testLibraryCard(1, user.getUserId());
    when(institutionDAO.findInstitutionById(libraryCard.getInstitutionId()))
        .thenReturn(null);
    when(userDAO.findUserById(user.getUserId()))
        .thenReturn(user);
    when(libraryCardDAO.findLibraryCardById(libraryCard.getId()))
        .thenReturn(libraryCard);

    initService();
    assertThrows(IllegalArgumentException.class, () -> {
      service.updateLibraryCard(libraryCard, libraryCard.getId(), 1);
    });
  }

  @Test
  public void testDeleteLibraryCard_NotFound() {
    Institution institution = testInstitution();
    User user = testUser(institution.getId());
    UserRole admin = new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
    user.addRole(admin);
    LibraryCard libraryCard = testLibraryCard(institution.getId(), user.getUserId());
    when(libraryCardDAO.findLibraryCardById(libraryCard.getId()))
        .thenReturn(null);
    doNothing().when(libraryCardDAO).deleteLibraryCardById(any());

    initService();
    assertThrows(NotFoundException.class, () -> {
      service.deleteLibraryCardById(libraryCard.getId());
    });
  }

  @Test
  public void testFindLibraryCardById_NotFound() {
    when(libraryCardDAO.findLibraryCardById(any()))
        .thenReturn(null);
    initService();
    assertThrows(NotFoundException.class, () -> {
      service.findLibraryCardById(1);
    });
  }

  @Test
  public void testFindLibraryCardById() {
    LibraryCard libraryCard = testLibraryCard(1, 1);
    when(libraryCardDAO.findLibraryCardById(libraryCard.getId()))
        .thenReturn(libraryCard);
    initService();
    LibraryCard result = service.findLibraryCardById(libraryCard.getId());
    assertNotNull(result);
    assertEquals(result.getId(), libraryCard.getId());
  }

  @Test
  public void testAddDaaToLibraryCard() {
    doNothing().when(libraryCardDAO).createLibraryCardDaaRelation(any(), any());

    LibraryCard libraryCard = testLibraryCard(1, 1);
    initService();
    assertDoesNotThrow(() -> service.addDaaToLibraryCard(libraryCard.getId(), 1));
  }

  @Test
  public void testRemoveDaaFromLibraryCard() {
    doNothing().when(libraryCardDAO).deleteLibraryCardDaaRelation(any(), any());

    LibraryCard libraryCard = testLibraryCard(1, 1);
    initService();
    assertDoesNotThrow(() -> service.removeDaaFromLibraryCard(libraryCard.getId(), 1));
  }


  private User testUser(Integer institutionId) {
    User user = new User();
    user.setUserId(RandomUtils.nextInt(1, 10));
    user.setInstitutionId(institutionId);
    return user;
  }

  private LibraryCard testLibraryCard(Integer institutionId, Integer userId) {
    LibraryCard libraryCard = new LibraryCard();
    libraryCard.setId(RandomUtils.nextInt(1, 10));
    libraryCard.setInstitutionId(institutionId);
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

  ;
}