package org.broadinstitute.consent.http.service;

import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.db.LibraryCardDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.exceptions.ConsentConflictException;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.util.Collections;

import javax.ws.rs.BadRequestException;
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
    // Test LC create with userId and email
    public void testCreateLibraryCardFullUserDetails() throws Exception {
        initService();
        Institution institution = testInstitution();
        User user = testUser(institution.getId());
        user.setEmail("testemail");
        when(userDAO.findUserById(anyInt())).thenReturn(user);
        when(institutionDAO.findInstitutionById(anyInt())).thenReturn(institution);
        when(libraryCardDAO.findLibraryCardsByUserId(anyInt())).thenReturn(Collections.emptyList());

        //last two calls in the function, no need to test within this service test file
        when(libraryCardDAO.insertLibraryCard(anyInt(), anyInt(), any(), any(), any(), anyInt(), any())).thenReturn(1);
        when(libraryCardDAO.findLibraryCardById(anyInt())).thenReturn(new LibraryCard());

        LibraryCard payload = testLibraryCard(institution.getId(), user.getDacUserId());
        payload.setUserEmail(user.getEmail());
        service.createLibraryCard(payload);
    }

    @Test
    //Test LC create with only user email (no userId)
    public void testCreateLibraryCardPartialUserDetailsEmail() throws Exception {
        initService();
        Institution institution = testInstitution();
        User user = testUser(institution.getId());
        user.setDacUserId(null);
        user.setEmail("testemail");

        when(userDAO.findUserById(anyInt())).thenReturn(user);
        when(institutionDAO.findInstitutionById(anyInt())).thenReturn(institution);
        when(libraryCardDAO.findLibraryCardsByUserId(anyInt())).thenReturn(Collections.emptyList());

        // last two calls in the function, no need to test within this service test file
        when(libraryCardDAO.insertLibraryCard(anyInt(), anyInt(), any(), any(), any(), anyInt(), any()))
                        .thenReturn(1);
        when(libraryCardDAO.findLibraryCardById(anyInt())).thenReturn(new LibraryCard());

        LibraryCard payload = testLibraryCard(institution.getId(), user.getDacUserId());
        payload.setUserEmail(user.getEmail());
        service.createLibraryCard(payload);
    }

    @Test
    //Test LC create with only user id (no email)
    public void testCreateLibraryCardPartialUserDetailsId() throws Exception {
        initService();
        Institution institution = testInstitution();
        User user = testUser(institution.getId());
        user.setEmail(null);

        when(userDAO.findUserById(anyInt())).thenReturn(user);
        when(institutionDAO.findInstitutionById(anyInt())).thenReturn(institution);
        when(libraryCardDAO.findLibraryCardsByUserId(anyInt())).thenReturn(Collections.emptyList());

        // last two calls in the function, no need to test within this service test file
        when(libraryCardDAO.insertLibraryCard(anyInt(), anyInt(), any(), any(), any(), anyInt(), any()))
                        .thenReturn(1);
        when(libraryCardDAO.findLibraryCardById(anyInt())).thenReturn(new LibraryCard());

        LibraryCard payload = testLibraryCard(institution.getId(), user.getDacUserId());
        service.createLibraryCard(payload);
    }

    @Test(expected = ConsentConflictException.class)
    //Negative test, checks if error is thrown if payload email and userId don't match up to those on user record
    public void testCreateLibraryCardIncorrectUserIdAndEmail() throws Exception {
        initService();
        Institution institution = testInstitution();
        User user = testUser(institution.getId());
        user.setDacUserId(1);
        user.setEmail("testemail");

        when(userDAO.findUserById(anyInt())).thenReturn(user);
        when(institutionDAO.findInstitutionById(anyInt())).thenReturn(institution);
        when(libraryCardDAO.findLibraryCardsByUserId(anyInt())).thenReturn(Collections.emptyList());

        when(libraryCardDAO.insertLibraryCard(anyInt(), anyInt(), any(), any(), any(), anyInt(), any()))
                        .thenReturn(1);
        when(libraryCardDAO.findLibraryCardById(anyInt())).thenReturn(new LibraryCard());

        LibraryCard payload = testLibraryCard(institution.getId(), user.getDacUserId());
        payload.setUserEmail("differentemail");
        service.createLibraryCard(payload);
    }

    @Test(expected = ConsentConflictException.class)
    //Negative test, checks to see if error thrown if card already exists on user id and institution id
    public void testCreateLibraryCardAlreadyExistsOnUserId() throws Exception{
        initService();
        Institution institution = testInstitution();
        User user = testUser(institution.getId());
        LibraryCard savedCard = testLibraryCard(institution.getId(), user.getDacUserId());
        LibraryCard payload = savedCard;

        when(institutionDAO.findInstitutionById(anyInt())).thenReturn(institution);
        when(libraryCardDAO.findLibraryCardsByUserId(anyInt())).thenReturn(Collections.singletonList(savedCard));
        service.createLibraryCard(payload);
    }

    @Test(expected = ConsentConflictException.class)
    // Negative test, checks to see if error thrown if card already exists on user email and institution id
    public void testCreateLibraryCardAlreadyExistsOnUserEmail() throws Exception {
        initService();
        Institution institution = testInstitution();
        User user = testUser(institution.getId());
        user.setEmail("testemail");
        LibraryCard savedCard = testLibraryCard(institution.getId(), null);
        savedCard.setUserEmail(user.getEmail());

        LibraryCard payload = savedCard;

        when(institutionDAO.findInstitutionById(anyInt())).thenReturn(institution);
        when(libraryCardDAO.findAllLibraryCardsByUserEmail(any())).thenReturn(Collections.singletonList(savedCard));
        service.createLibraryCard(payload);
    }

    @Test(expected = BadRequestException.class)
    //Negative test, checks to see if error is thrown if email and userId are not provided
    public void testCreateLibraryCardNoUserDetails() throws Exception {
        initService();
        Institution institution = testInstitution();
        LibraryCard payload = testLibraryCard(institution.getId(), null);
        when(institutionDAO.findInstitutionById(anyInt())).thenReturn(institution);

        service.createLibraryCard(payload);
    }

    @Test(expected = IllegalArgumentException.class)
    //Negative test, checks if error is thrown on null institutionId
    public void testCreateLibraryCard_InvalidInstitution() throws Exception {
        User user = testUser(1);
        LibraryCard libraryCard = testLibraryCard(1, user.getDacUserId());

        when(libraryCardDAO.findLibraryCardById(libraryCard.getId()))
                .thenReturn(libraryCard);
        when(institutionDAO.findInstitutionById(libraryCard.getInstitutionId()))
                .thenReturn(null);
        when(userDAO.findUserById(user.getDacUserId()))
                .thenReturn(user);

        initService();
        service.createLibraryCard(libraryCard);
    }

    @Test(expected = NotFoundException.class)
    //Negative test, checks to see if error is thrown on null payload
    public void testCreateLibraryCardNullPayload() throws Exception {
        initService();
        service.createLibraryCard(null);
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