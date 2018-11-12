package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.enumeration.ResearcherFields;
import org.broadinstitute.consent.http.models.NIHUserAccount;
import org.broadinstitute.consent.http.models.ResearcherProperty;
import org.broadinstitute.consent.http.service.users.handler.ResearcherAPI;

import java.util.List;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class NihServiceAPI implements NihAuthApi {

//    private NihConfiguration nihConfiguration;
    private ResearcherAPI researcherAPI;

    public NihServiceAPI(/*NihConfiguration nihConfiguration,*/ ResearcherAPI researcherApi) {
        this.researcherAPI = researcherApi;
//        this.nihConfiguration = nihConfiguration;
    }

    @Override
    public List<ResearcherProperty> authenticateNih(NIHUserAccount nihAccount, Integer userId) {
        nihAccount.setEraExpiration(generateEraExpirationDates());
        return researcherAPI.updateResearcher(nihAccount.getNihMap(), userId, false);
    }

    @Override
    public void deleteNihAccountById(Integer userId) {
        List<ResearcherProperty> properties = new ArrayList<>();
        properties.add(new ResearcherProperty(userId, ResearcherFields.ERA_EXPIRATION_DATE.getValue()));
        properties.add(new ResearcherProperty(userId, ResearcherFields.ERA_STATUS.getValue()));
        properties.add(new ResearcherProperty(userId, ResearcherFields.ERA_USERNAME.getValue()));

        researcherAPI.deleteResearcherSpecificProperties(properties);
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
