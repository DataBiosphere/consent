package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.db.LibraryCardDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.enumeration.UserFields;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.NIHUserAccount;
import org.broadinstitute.consent.http.models.UserProperty;

import javax.ws.rs.BadRequestException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class NihService {

    private final ResearcherService researcherService;
    private final LibraryCardDAO libraryCardDAO;
    private final UserDAO userDAO;

    @Inject
    public NihService(ResearcherService researcherService, LibraryCardDAO libraryCardDAO, UserDAO userDAO) {
        this.researcherService = researcherService;
        this.libraryCardDAO = libraryCardDAO;
        this.userDAO = userDAO;
    }

    public List<UserProperty> authenticateNih(NIHUserAccount nihAccount, AuthUser authUser, Integer userId) throws BadRequestException {
        if (StringUtils.isNotEmpty(nihAccount.getLinkedNihUsername()) && !nihAccount.getLinkedNihUsername().isEmpty()) {
            nihAccount.setLinkExpireTime(generateEraExpirationDates());
            nihAccount.setStatus(true);
            List<UserProperty> updatedProps = researcherService.updateProperties(nihAccount.getNihMap(), authUser, false);
            libraryCardDAO.updateEraCommonsForUser(userId, nihAccount.getLinkedNihUsername());
            userDAO.updateEraCommonsId(userId, nihAccount.getLinkedNihUsername());
            return updatedProps;
        } else {
            throw new BadRequestException("Invalid NIH UserName for user : " + authUser.getEmail());
        }
    }

    public void deleteNihAccountById(Integer userId) {
        List<UserProperty> properties = new ArrayList<>();
        properties.add(new UserProperty(userId, UserFields.ERA_EXPIRATION_DATE.getValue()));
        properties.add(new UserProperty(userId, UserFields.ERA_STATUS.getValue()));
        researcherService.deleteResearcherSpecificProperties(properties);
    }


    private String generateEraExpirationDates() {
        Date currentDate = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(currentDate);
        c.add(Calendar.DATE, 30);
        Date expires= c.getTime();
        return String.valueOf(expires.getTime());
    }

}
