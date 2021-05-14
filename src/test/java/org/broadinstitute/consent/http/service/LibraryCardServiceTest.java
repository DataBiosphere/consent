package org.broadinstitute.consent.http.service;

import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.db.LibraryCardDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import javax.ws.rs.NotFoundException;

public class LibraryCardServiceTest {

    private LibraryCardService service;

    @Mock
    private InstitutionDAO institutionDAO;
    @Mock
    private LibraryCardDAO libraryCardDAO;
    @Mock
    private UserDAO userDAO;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    private void initService() {
        this.service = new LibraryCardService(libraryCardDAO, institutionDAO, userDAO);
    }

    @Test
    public void testCreateLibraryCard() {
        Institution institution = testInstitution();
        LibraryCard libraryCard = testLibraryCard(institution.getId(), null);
        when(libraryCardDAO.insertLibraryCard(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(libraryCard.getId());
        when(libraryCardDAO.findLibraryCardById(libraryCard.getId()))
                .thenReturn(libraryCard);
        when(institutionDAO.findInstitutionById(libraryCard.getInstitutionId()))
                .thenReturn(institution);
        initService();

        LibraryCard resultCard = service.createLibraryCard(libraryCard, 1);
        assertNotNull(resultCard);
        assertEquals(resultCard.getId(), libraryCard.getId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateLibraryCard_InvalidInstitution() {
        User user = testUser(1);
        LibraryCard libraryCard = testLibraryCard(1, user.getDacUserId());

        when(libraryCardDAO.findLibraryCardById(libraryCard.getId()))
                .thenReturn(libraryCard);
        when(institutionDAO.findInstitutionById(libraryCard.getInstitutionId()))
                .thenReturn(null);
        when(userDAO.findUserById(user.getDacUserId()))
                .thenReturn(user);

        initService();
        service.createLibraryCard(libraryCard, 1);
    }
    @Test(expected = IllegalArgumentException.class)
    public void testCreateLibraryCard_InvalidUser() {
        LibraryCard libraryCard = testLibraryCard(1, 1);

        when(libraryCardDAO.findLibraryCardById(libraryCard.getId()))
                .thenReturn(libraryCard);
        when(userDAO.findUserById(any()))
                .thenReturn(null);

        initService();
        service.createLibraryCard(libraryCard, 1);
    }


    @Test
    public void testUpdateLibraryCard() {
        Institution institution = testInstitution();
        User user = testUser(institution.getId());
        LibraryCard libraryCard = testLibraryCard(institution.getId(), user.getDacUserId());
        when(institutionDAO.findInstitutionById(libraryCard.getInstitutionId()))
                .thenReturn(institution);
        when(libraryCardDAO.findLibraryCardById(libraryCard.getId()))
                .thenReturn(libraryCard);
        when(userDAO.findUserById(user.getDacUserId()))
                .thenReturn(user);
        doNothing().when(libraryCardDAO).updateLibraryCardById(any(), any(), any(), any(), any(), any(), any(), any());

        initService();
        LibraryCard resultCard = service.updateLibraryCard(libraryCard, libraryCard.getId(), 1);
        assertNotNull(resultCard);
        assertEquals(resultCard.getId(), libraryCard.getId());
    }

    @Test(expected = NotFoundException.class)
    public void testUpdateLibraryCard_NotFound() {
        Institution institution = testInstitution();
        User user = testUser(institution.getId());
        LibraryCard libraryCard = testLibraryCard(institution.getId(), user.getDacUserId());
        when(institutionDAO.findInstitutionById(libraryCard.getInstitutionId()))
                .thenReturn(institution);
        when(userDAO.findUserById(user.getDacUserId()))
                .thenReturn(user);
        when(libraryCardDAO.findLibraryCardById(libraryCard.getId()))
                .thenReturn(null);

        initService();
        service.updateLibraryCard(libraryCard, libraryCard.getId(), 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateLibraryCard_InvalidInstitution() {
        User user = testUser(1);
        LibraryCard libraryCard = testLibraryCard(1, user.getDacUserId());
        when(institutionDAO.findInstitutionById(libraryCard.getInstitutionId()))
                .thenReturn(null);
        when(userDAO.findUserById(user.getDacUserId()))
                .thenReturn(user);
        when(libraryCardDAO.findLibraryCardById(libraryCard.getId()))
                .thenReturn(libraryCard);

        initService();
        service.updateLibraryCard(libraryCard, libraryCard.getId(), 1);
    }

    @Test(expected = NotFoundException.class)
    public void testDeleteLibraryCard_NotFound() {
        Institution institution = testInstitution();
        User user = testUser(institution.getId());
        LibraryCard libraryCard = testLibraryCard(institution.getId(), user.getDacUserId());
        when(libraryCardDAO.findLibraryCardById(libraryCard.getId()))
                .thenReturn(null);
        doNothing().when(libraryCardDAO).deleteLibraryCardById(any());

        initService();
        service.deleteLibraryCardById(libraryCard.getId());
    }

    @Test(expected = NotFoundException.class)
    public void testFindLibraryCardById_NotFound() {
        when(libraryCardDAO.findLibraryCardById(any()))
                .thenReturn(null);
        initService();
        service.findLibraryCardById(1);
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

    private User testUser(Integer institutionId) {
        User user = new User();
        user.setDacUserId(RandomUtils.nextInt(1, 10));
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
}