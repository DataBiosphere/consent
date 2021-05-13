package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.db.LibraryCardDAO;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.LibraryCard;

import javax.ws.rs.NotFoundException;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class LibraryCardService {

    private final LibraryCardDAO libraryCardDAO;
    private final InstitutionDAO institutionDAO;

    public LibraryCardService(LibraryCardDAO libraryCardDAO, InstitutionDAO institutionDAO) {
        this.libraryCardDAO = libraryCardDAO;
        this.institutionDAO = institutionDAO;
    }

    public LibraryCard createLibraryCard(LibraryCard libraryCard, Integer userId) {
        checkUserId(userId);
        checkForValidInstitution(libraryCard.getInstitutionId());

        Date createDate = new Date();
        Integer id = this.libraryCardDAO.insertLibraryCard(
                libraryCard.getUserId(),
                libraryCard.getInstitutionId(),
                libraryCard.getEraCommonsId(),
                libraryCard.getUserName(),
                libraryCard.getUserEmail(),
                userId,
                createDate
        );
        return this.libraryCardDAO.findLibraryCardById(id);
    }

    public LibraryCard updateLibraryCard(LibraryCard libraryCard, Integer id, Integer userId) {
        LibraryCard updateCard = this.libraryCardDAO.findLibraryCardById(id);
        throwIfNull(updateCard);
        checkUserId(userId);
        checkForValidInstitution(libraryCard.getInstitutionId());

        Date updateDate = new Date();
        this.libraryCardDAO.updateLibraryCardById(
                id,
                libraryCard.getUserId(),
                libraryCard.getInstitutionId(),
                libraryCard.getEraCommonsId(),
                libraryCard.getUserName(),
                libraryCard.getUserEmail(),
                userId,
                updateDate
        );
        return this.libraryCardDAO.findLibraryCardById(id);
    }

    public void deleteLibraryCardById(Integer id) {
        LibraryCard libraryCard = this.libraryCardDAO.findLibraryCardById(id);
        throwIfNull(libraryCard);
        this.libraryCardDAO.deleteLibraryCardById(id);
    }

    public List<LibraryCard> findAllLibraryCards() {
        return this.libraryCardDAO.findAllLibraryCards();
    }

    public List<LibraryCard> findLibraryCardsByUserId(Integer userId) {
        return this.libraryCardDAO.findLibraryCardsByUserId(userId);
    }

    public List<LibraryCard> findLibraryCardsByInstitutionId(Integer institutionId) {
        return this.libraryCardDAO.findLibraryCardsByInstitutionId(institutionId);
    }

    public LibraryCard findLibraryCardById(Integer libraryCardId) {
        LibraryCard libraryCard = this.libraryCardDAO.findLibraryCardById(libraryCardId);
        throwIfNull(libraryCard);
        return libraryCard;
    }

    private void checkForValidInstitution(Integer institutionId) {
        checkInstitutionId(institutionId);
        Institution institution = this.institutionDAO.findInstitutionById(institutionId);

        if (Objects.isNull(institution)) {
            throw new IllegalArgumentException("Invalid Institution Id");
        }
    }

    private void checkInstitutionId(Integer institutionId) {
        if (Objects.isNull(institutionId)){
            throw new IllegalArgumentException("Institution ID is a required parameter");
        }
    }

    private void checkUserId(Integer userId) {
        if (Objects.isNull(userId)) {
            throw new IllegalArgumentException("User ID is a required parameter");
        }
    }

    private void throwIfNull(LibraryCard libraryCard) {
        if (Objects.isNull(libraryCard)) {
            throw new NotFoundException("LibraryCard not found.");
        }
    }
}
