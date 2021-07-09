package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.db.LibraryCardDAO;
import org.broadinstitute.consent.http.enumeration.UserFields;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.NIHUserAccount;
import org.broadinstitute.consent.http.models.UserProperty;

import javax.ws.rs.BadRequestException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class NihService {

    private ResearcherService researcherService;
    private LibraryCardDAO libraryCardDAO;

    @Inject
    public NihService(ResearcherService researcherService, LibraryCardDAO libraryCardDAO) {
        this.researcherService = researcherService;
        this.libraryCardDAO = libraryCardDAO;
    }

    public List<UserProperty> authenticateNih(NIHUserAccount nihAccount, AuthUser authUser, Integer userId) throws BadRequestException {
        if (StringUtils.isNotEmpty(nihAccount.getNihUsername()) && !nihAccount.getNihUsername().isEmpty()) {
            nihAccount.setEraExpiration(generateEraExpirationDates());
            nihAccount.setStatus(true);
            //now that eraCommons is not being updated in properties we need to update it separately
            List<UserProperty> updatedProps = researcherService.updateProperties(nihAccount.getNihMap(), authUser, false);
            if (Objects.nonNull(nihAccount.getNihUsername())) {
                //only updates eraCommons on LC not on user
                libraryCardDAO.updateEraCommonsForUser(userId, nihAccount.getNihUsername());
                //add new DAO call here to update eraCommons user
            }
            return updatedProps;
        } else {
            throw new BadRequestException("Invalid NIH UserName for user : " + authUser.getName());
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
