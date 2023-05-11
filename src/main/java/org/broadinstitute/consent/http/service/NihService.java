package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.enumeration.UserFields;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.NIHUserAccount;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserProperty;
import org.broadinstitute.consent.http.service.dao.NihServiceDAO;
import org.broadinstitute.consent.http.util.ConsentLogger;

public class NihService implements ConsentLogger {

    private final ResearcherService researcherService;
    private final UserDAO userDAO;
    private final NihServiceDAO serviceDAO;

    @Inject
    public NihService(ResearcherService researcherService, UserDAO userDAO, NihServiceDAO serviceDAO) {
        this.researcherService = researcherService;
        this.userDAO = userDAO;
        this.serviceDAO = serviceDAO;
    }

    public void validateNihUserAccount(NIHUserAccount nihAccount, AuthUser authUser) throws BadRequestException {
        if (Objects.isNull(nihAccount) || Objects.isNull(nihAccount.getEraExpiration())) {
            logWarn("Invalid NIH Account for user: " + authUser.getEmail());
            throw new BadRequestException("Invalid NIH Authentication for user : " + authUser.getEmail());
        }
    }

    public List<UserProperty> authenticateNih(NIHUserAccount nihAccount, AuthUser authUser, Integer userId) throws BadRequestException {
        // fail fast
        validateNihUserAccount(nihAccount, authUser);
        User user = userDAO.findUserById(userId);
        if (Objects.isNull(user)) {
            throw new NotFoundException("User not found: " + authUser.getEmail());
        }
        if (StringUtils.isNotEmpty(nihAccount.getNihUsername()) && !nihAccount.getNihUsername().isEmpty()) {
            nihAccount.setEraExpiration(generateEraExpirationDates());
            nihAccount.setStatus(true);
            try {
                serviceDAO.updateUserNihStatus(user, nihAccount);
            } catch (IllegalArgumentException e) {
                logException(e);
            }
            return researcherService.describeUserProperties(userId);
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
        Date expires = c.getTime();
        return String.valueOf(expires.getTime());
    }

}
