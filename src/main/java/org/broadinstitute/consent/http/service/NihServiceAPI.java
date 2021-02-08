package org.broadinstitute.consent.http.service;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.enumeration.ResearcherFields;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.NIHUserAccount;
import org.broadinstitute.consent.http.models.UserProperty;
import org.broadinstitute.consent.http.service.users.handler.ResearcherService;

import javax.ws.rs.BadRequestException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class NihServiceAPI implements NihAuthApi {

    private ResearcherService researcherService;

    public NihServiceAPI(ResearcherService researcherService) {
        this.researcherService = researcherService;
    }

    @Override
    public List<UserProperty> authenticateNih(NIHUserAccount nihAccount, AuthUser user) {
        if (StringUtils.isNotEmpty(nihAccount.getNihUsername()) && !nihAccount.getNihUsername().isEmpty()) {
            nihAccount.setEraExpiration(generateEraExpirationDates());
            nihAccount.setStatus(true);
            return researcherService.updateProperties(nihAccount.getNihMap(), user, false);
        } else {
            throw new BadRequestException("Invalid NIH UserName for user : " + user.getName());
        }
    }

    @Override
    public void deleteNihAccountById(Integer userId) {
        List<UserProperty> properties = new ArrayList<>();
        properties.add(new UserProperty(userId, ResearcherFields.ERA_EXPIRATION_DATE.getValue()));
        properties.add(new UserProperty(userId, ResearcherFields.ERA_STATUS.getValue()));
        properties.add(new UserProperty(userId, ResearcherFields.ERA_USERNAME.getValue()));
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
