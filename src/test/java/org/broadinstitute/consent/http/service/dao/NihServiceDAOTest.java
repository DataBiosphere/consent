package org.broadinstitute.consent.http.service.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.enumeration.UserFields;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.NIHUserAccount;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserProperty;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NihServiceDAOTest extends DAOTestHelper {

  private NihServiceDAO serviceDAO;

  @BeforeEach
  void setUp() {
    serviceDAO = new NihServiceDAO(jdbi);
  }

  @Test
  void testUpdateUserNihStatus_existing() {
    // create a user
    User user = createUser();
    // Create ERA Account Props
    UserProperty prop1 =
        new UserProperty(
            user.getUserId(), UserFields.ERA_STATUS.getValue(), Boolean.TRUE.toString());
    UserProperty prop2 =
        new UserProperty(
            user.getUserId(), UserFields.ERA_EXPIRATION_DATE.getValue(), new Date().toString());
    String commonsId = "COMMONS_ID";
    userDAO.updateEraCommonsId(user.getUserId(), commonsId);
    userPropertyDAO.insertAll(List.of(prop1, prop2));
    // Create Library Card
    libraryCardDAO.insertLibraryCard(
        user.getUserId(), user.getDisplayName(), user.getEmail(), user.getUserId(), new Date());

    // Build a new NIHUserAccount to update
    NIHUserAccount userAccount = new NIHUserAccount();
    userAccount.setStatus(true);
    userAccount.setNihUsername("NEW_ID");
    userAccount.setEraExpiration("new expiration");
    serviceDAO.updateUserNihStatus(user, userAccount);

    // assert that props are updated to the new values
    List<UserProperty> updatedProps =
        userPropertyDAO.findUserPropertiesByUserIdAndPropertyKeys(
            user.getUserId(),
            List.of(UserFields.ERA_STATUS.getValue(), UserFields.ERA_EXPIRATION_DATE.getValue()));

    Optional<UserProperty> statusProp =
        updatedProps.stream()
            .filter(
                userProperty ->
                    userProperty.getPropertyKey().equals(UserFields.ERA_STATUS.getValue()))
            .findFirst();
    assertTrue(statusProp.isPresent());
    assertEquals(statusProp.get().getPropertyValue(), userAccount.getStatus().toString());

    Optional<UserProperty> expirationProp =
        updatedProps.stream()
            .filter(
                userProperty ->
                    userProperty.getPropertyKey().equals(UserFields.ERA_EXPIRATION_DATE.getValue()))
            .findFirst();
    assertTrue(expirationProp.isPresent());
    assertEquals(expirationProp.get().getPropertyValue(), userAccount.getEraExpiration());

    // assert that era commons user id is updated appropriately
    User updatedUser = userDAO.findUserById(user.getUserId());
    assertEquals(updatedUser.getEraCommonsId(), userAccount.getNihUsername());

    LibraryCard card = libraryCardDAO.findLibraryCardByUserId(user.getUserId());
    assertNotNull(card);
  }

  @Test
  void testUpdateUserNihStatus_new() {
    // create a user
    User user = createUser();

    // Build a new NIHUserAccount to update
    NIHUserAccount userAccount = new NIHUserAccount();
    userAccount.setStatus(true);
    userAccount.setNihUsername("NEW_ID");
    userAccount.setEraExpiration("new expiration");
    serviceDAO.updateUserNihStatus(user, userAccount);

    // assert that props are updated to the new values
    List<UserProperty> updatedProps =
        userPropertyDAO.findUserPropertiesByUserIdAndPropertyKeys(
            user.getUserId(),
            List.of(UserFields.ERA_STATUS.getValue(), UserFields.ERA_EXPIRATION_DATE.getValue()));

    Optional<UserProperty> statusProp =
        updatedProps.stream()
            .filter(
                userProperty ->
                    userProperty.getPropertyKey().equals(UserFields.ERA_STATUS.getValue()))
            .findFirst();
    assertTrue(statusProp.isPresent());
    assertEquals(statusProp.get().getPropertyValue(), userAccount.getStatus().toString());

    Optional<UserProperty> expirationProp =
        updatedProps.stream()
            .filter(
                userProperty ->
                    userProperty.getPropertyKey().equals(UserFields.ERA_EXPIRATION_DATE.getValue()))
            .findFirst();
    assertTrue(expirationProp.isPresent());
    assertEquals(expirationProp.get().getPropertyValue(), userAccount.getEraExpiration());

    // assert that era commons user id is updated appropriately
    User updatedUser = userDAO.findUserById(user.getUserId());
    assertEquals(updatedUser.getEraCommonsId(), userAccount.getNihUsername());

    // ensure that we did not make any LC updates
    LibraryCard card = libraryCardDAO.findLibraryCardByUserId(user.getUserId());
    assertNull(card);
  }

  @Test
  void testUpdateUserNihStatus_nullAccount() {
    User user = createUser();
    assertThrows(IllegalArgumentException.class, () -> serviceDAO.updateUserNihStatus(user, null));
  }

  @Test
  void testUpdateUserNihStatus_jdbiError() {
    // superclass jdbi is not a mock, we need to mock it locally to simulate an exception
    Jdbi jdbi = mock(Jdbi.class);
    serviceDAO = new NihServiceDAO(jdbi);
    doThrow(new Exception()).when(jdbi).useTransaction(any());
    User user = createUser();
    NIHUserAccount userAccount = new NIHUserAccount();
    userAccount.setStatus(true);
    userAccount.setNihUsername("NEW_ID");
    userAccount.setEraExpiration("new expiration");
    assertThrows(Exception.class, () -> serviceDAO.updateUserNihStatus(user, userAccount));
  }

  @Test
  void testDeleteNihAccountById() {
    User user = createUser();
    UserProperty prop1 =
        new UserProperty(
            user.getUserId(), UserFields.ERA_STATUS.getValue(), Boolean.TRUE.toString());
    UserProperty prop2 =
        new UserProperty(
            user.getUserId(), UserFields.ERA_EXPIRATION_DATE.getValue(), new Date().toString());
    String commonsId = "COMMONS_ID";
    userDAO.updateEraCommonsId(user.getUserId(), commonsId);
    userPropertyDAO.insertAll(List.of(prop1, prop2));

    serviceDAO.deleteNihAccountById(user.getUserId());

    // assert that props are deleted
    List<UserProperty> updatedProps =
        userPropertyDAO.findUserPropertiesByUserIdAndPropertyKeys(
            user.getUserId(),
            List.of(UserFields.ERA_STATUS.getValue(), UserFields.ERA_EXPIRATION_DATE.getValue()));
    assertTrue(updatedProps.isEmpty());

    // assert that era commons id is null
    User updatedUser = userDAO.findUserById(user.getUserId());
    assertNull(updatedUser.getEraCommonsId());
  }

  @Test
  void testDeleteNihStatus_jdbiError() {
    // superclass jdbi is not a mock, we need to mock it locally to simulate an exception
    Jdbi jdbi = mock(Jdbi.class);
    serviceDAO = new NihServiceDAO(jdbi);
    doThrow(new Exception()).when(jdbi).useTransaction(any());
    User user = createUser();
    assertThrows(Exception.class, () -> serviceDAO.deleteNihAccountById(user.getUserId()));
  }
}
