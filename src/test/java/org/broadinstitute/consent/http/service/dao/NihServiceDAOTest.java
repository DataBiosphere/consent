package org.broadinstitute.consent.http.service.dao;

import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.enumeration.UserFields;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.NIHUserAccount;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserProperty;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NihServiceDAOTest extends DAOTestHelper {

    private NihServiceDAO serviceDAO;

    @Before
    public void setUp() {
        serviceDAO = new NihServiceDAO(jdbi);
    }

    @Test
    public void testUpdateUserNihStatus_existing() throws Exception {
        // create a user
        User user = createUser();
        // Create ERA Account Props
        UserProperty prop1 = new UserProperty(
            user.getUserId(),
                UserFields.ERA_STATUS.getValue(),
                Boolean.TRUE.toString()
        );
        UserProperty prop2 = new UserProperty(
            user.getUserId(),
                UserFields.ERA_EXPIRATION_DATE.getValue(),
                new Date().toString()
        );
        String commonsId = "COMMONS_ID";
        userDAO.updateEraCommonsId(user.getUserId(), commonsId);
        userPropertyDAO.insertAll(List.of(prop1, prop2));
        // Create Library Card
        Integer institutionId = institutionDAO.insertInstitution("name", "name", "email", "url", 1, "url", "url", "file", "type", user.getUserId(), new Date());
        libraryCardDAO.insertLibraryCard(user.getUserId(), institutionId, commonsId, user.getDisplayName(), user.getEmail(), user.getUserId(), new Date());

        // Build a new NIHUserAccount to update
        NIHUserAccount userAccount = new NIHUserAccount();
        userAccount.setStatus(true);
        userAccount.setNihUsername("NEW_ID");
        userAccount.setEraExpiration("new expiration");
        serviceDAO.updateUserNihStatus(user, userAccount);

        // assert that props are updated to the new values
        List<UserProperty> updatedProps = userPropertyDAO.findUserPropertiesByUserIdAndPropertyKeys(
            user.getUserId(),
            List.of(UserFields.ERA_STATUS.getValue(), UserFields.ERA_EXPIRATION_DATE.getValue()));

        Optional<UserProperty> statusProp = updatedProps
            .stream()
            .filter(userProperty -> userProperty.getPropertyKey().equals(UserFields.ERA_STATUS.getValue()))
            .findFirst();
        assertTrue(statusProp.isPresent());
        assertEquals(statusProp.get().getPropertyValue(), userAccount.getStatus().toString());

        Optional<UserProperty> expirationProp = updatedProps
            .stream()
            .filter(userProperty -> userProperty.getPropertyKey().equals(UserFields.ERA_EXPIRATION_DATE.getValue()))
            .findFirst();
        assertTrue(expirationProp.isPresent());
        assertEquals(expirationProp.get().getPropertyValue(), userAccount.getEraExpiration());

        // assert that era commons user id is updated appropriately
        User updatedUser = userDAO.findUserById(user.getUserId());
        assertEquals(updatedUser.getEraCommonsId(), userAccount.getNihUsername());

        List<LibraryCard> cards = libraryCardDAO.findLibraryCardsByUserId(user.getUserId());
        assertFalse(cards.isEmpty());
        assertEquals(cards.get(0).getEraCommonsId(), userAccount.getNihUsername());
    }

    @Test
    public void testUpdateUserNihStatus_new() throws Exception {
        // create a user
        User user = createUser();

        // Build a new NIHUserAccount to update
        NIHUserAccount userAccount = new NIHUserAccount();
        userAccount.setStatus(true);
        userAccount.setNihUsername("NEW_ID");
        userAccount.setEraExpiration("new expiration");
        serviceDAO.updateUserNihStatus(user, userAccount);

        // assert that props are updated to the new values
        List<UserProperty> updatedProps = userPropertyDAO.findUserPropertiesByUserIdAndPropertyKeys(
            user.getUserId(),
            List.of(UserFields.ERA_STATUS.getValue(), UserFields.ERA_EXPIRATION_DATE.getValue()));

        Optional<UserProperty> statusProp = updatedProps
            .stream()
            .filter(userProperty -> userProperty.getPropertyKey().equals(UserFields.ERA_STATUS.getValue()))
            .findFirst();
        assertTrue(statusProp.isPresent());
        assertEquals(statusProp.get().getPropertyValue(), userAccount.getStatus().toString());

        Optional<UserProperty> expirationProp = updatedProps
            .stream()
            .filter(userProperty -> userProperty.getPropertyKey().equals(UserFields.ERA_EXPIRATION_DATE.getValue()))
            .findFirst();
        assertTrue(expirationProp.isPresent());
        assertEquals(expirationProp.get().getPropertyValue(), userAccount.getEraExpiration());

        // assert that era commons user id is updated appropriately
        User updatedUser = userDAO.findUserById(user.getUserId());
        assertEquals(updatedUser.getEraCommonsId(), userAccount.getNihUsername());

        // ensure that we did not make any LC updates
        List<LibraryCard> cards = libraryCardDAO.findLibraryCardsByUserId(user.getUserId());
        assertTrue(cards.isEmpty());
    }

}
