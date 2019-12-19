package org.broadinstitute.consent.http.service;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.enumeration.ResearcherFields;
import org.broadinstitute.consent.http.models.NIHUserAccount;
import org.broadinstitute.consent.http.models.ResearcherProperty;
import org.broadinstitute.consent.http.service.users.handler.ResearcherService;

import javax.ws.rs.BadRequestException;
import java.util.List;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class NihServiceAPI implements NihAuthApi {

    private ResearcherService researcherService;

    public NihServiceAPI(ResearcherService researcherService) {
        this.researcherService = researcherService;
    }

    @Override
    public List<ResearcherProperty> authenticateNih(NIHUserAccount nihAccount, Integer userId){
        if (StringUtils.isNotEmpty(nihAccount.getNihUsername()) && !nihAccount.getNihUsername().isEmpty()) {
            nihAccount.setEraExpiration(generateEraExpirationDates());
            nihAccount.setStatus(true);
            return researcherService.updateResearcher(nihAccount.getNihMap(), userId, false);
        } else {
            throw new BadRequestException("Invalid NIH UserName for user : " + userId);
        }
    }

    @Override
    public void deleteNihAccountById(Integer userId) {
        List<ResearcherProperty> properties = new ArrayList<>();
        properties.add(new ResearcherProperty(userId, ResearcherFields.ERA_EXPIRATION_DATE.getValue()));
        properties.add(new ResearcherProperty(userId, ResearcherFields.ERA_STATUS.getValue()));
        properties.add(new ResearcherProperty(userId, ResearcherFields.ERA_USERNAME.getValue()));
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
