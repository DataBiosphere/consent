package org.broadinstitute.consent.http.service.validate;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import com.google.gson.Gson;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import org.apache.commons.collections.CollectionUtils;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.models.dto.UseRestrictionDTO;
import org.broadinstitute.consent.http.models.validate.ValidateResponse;
import org.broadinstitute.consent.http.service.AbstractDataAccessRequestAPI;
import org.broadinstitute.consent.http.service.DataAccessRequestAPI;
import org.broadinstitute.consent.http.util.DarConstants;
import org.bson.Document;
import org.glassfish.jersey.client.ClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UseRestrictionValidator extends AbstractUseRestrictionValidatorAPI{

    private Client client;
    private ConsentDAO consentDAO;
    private DataAccessRequestAPI dataAccessAPI;
    private static final Logger logger = LoggerFactory.getLogger(UseRestrictionValidator.class);
    private String validateUrl;

    public static void initInstance(Client client, ServicesConfiguration config, ConsentDAO consentDAO) {
        AbstractUseRestrictionValidatorAPI.UseRestrictionValidatorAPIHolder.setInstance(new UseRestrictionValidator(client, config, consentDAO));
    }

    private UseRestrictionValidator(Client client, ServicesConfiguration config, ConsentDAO consentDAO){
        this.client = ClientBuilder.newClient();
        this.dataAccessAPI = AbstractDataAccessRequestAPI.getInstance();
        client.property(ClientProperties.CONNECT_TIMEOUT, 10000);
        client.property(ClientProperties.READ_TIMEOUT,    10000);
        this.validateUrl = config.getValidateUseRestrictionURL();
        this.consentDAO = consentDAO;
    }

    public void validateUseRestriction(String useRestriction) throws IllegalArgumentException {
        Response res = client.target(validateUrl).request("application/json").post(Entity.json(useRestriction));
        if (res.getStatus() == Response.Status.OK.getStatusCode()) {
            ValidateResponse entity = res.readEntity(ValidateResponse.class);
            if (!entity.isValid()) {
                throw new IllegalArgumentException(entity.getErrors().toString());
            }
        }else{
            throw new IllegalArgumentException("There was an error posting the use restriction" + (res.readEntity(ValidateResponse.class)).getErrors().toString());
        }
    }

    @Override
    public void setClient(Client client) {
        this.client = client;
    }

    @Override
    public void validateConsentUseRestriction(){
        List<UseRestrictionDTO> useRestrictionDTOList = consentDAO.findConsentUseRestrictions();
        List<UseRestrictionDTO> invalidUseRestrictionDTOList = new ArrayList<>();
        List<UseRestrictionDTO> validUseRestrictionDTOList = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(useRestrictionDTOList)){
            useRestrictionDTOList.forEach(us -> {
                try{
                    validateUseRestriction(us.getUseRestriction().toString());
                    validUseRestrictionDTOList.add(us);
                }catch (IllegalArgumentException iae){
                    invalidUseRestrictionDTOList.add(us);
                }
            });
        }
        logger.info("invalid consent use restrictions: " + invalidUseRestrictionDTOList.size());
        updateConsentValidUseRestriction(invalidUseRestrictionDTOList, false);
        logger.info("valid consent use restrictions: "+ validUseRestrictionDTOList.size());
        updateConsentValidUseRestriction(validUseRestrictionDTOList, true);
    }

    @Override
    public void validateDARUseRestriction(){
        FindIterable<Document> darUseRestriction = dataAccessAPI.findDARUseRestrictions();
        List<Document> invalidUseRestrictionList = new ArrayList<>();
        List<Document> validUseRestrictionList = new ArrayList<>();
        if(darUseRestriction != null){
            darUseRestriction.forEach((Block<Document>) dar -> {
                try {
                    String restriction = new Gson().toJson(dar.get(DarConstants.RESTRICTION, Map.class));
                    validateUseRestriction(restriction);
                    validUseRestrictionList.add(dar);
                }catch(Exception e){
                    invalidUseRestrictionList.add(dar);
                }
            });
        }
        logger.info("invalid dar use restrictions: " + invalidUseRestrictionList.size());
        updateDARValidUseRestriction(invalidUseRestrictionList, false);
        logger.info("valid dar use restrictions: " + validUseRestrictionList.size());
        updateDARValidUseRestriction(validUseRestrictionList, true);
    }

    private void updateDARValidUseRestriction(List<Document> useRestrictionList, Boolean validUseRestriction){
        List<String> darCodes = useRestrictionList.stream().map(sc -> sc.getString(DarConstants.DAR_CODE)).collect(Collectors.toList());
        dataAccessAPI.updateDARUseRestrictionValidation(darCodes, validUseRestriction);
   }

    private void updateConsentValidUseRestriction(List<UseRestrictionDTO> useRestrictionDTOList, Boolean validUseRestriction) {
        if(CollectionUtils.isNotEmpty(useRestrictionDTOList)){
            List<String> consentIdList = useRestrictionDTOList.stream().map(sc -> sc.getId()).collect(Collectors.toList());
            consentDAO.updateConsentValidUseRestriction(consentIdList, validUseRestriction);
        }
    }
}