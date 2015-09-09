package org.genomebridge.consent.http.service;

import org.genomebridge.consent.http.db.ResearchPurposeDAO;
import org.genomebridge.consent.http.models.ResearchPurpose;

import javax.ws.rs.NotFoundException;
import java.util.List;


/**
 * Implementation class for ResearchPurposeAPI on top of ResearchPurposeDAO database
 * support.
 */
public class DatabaseResearchPurposeAPI  extends AbstractResearchPurposeAPI {

    private ResearchPurposeDAO purposeDAO;


    public static void initInstance(ResearchPurposeDAO purposeDAO) {
        ResearchPurposeAPIHolder.setInstance(new DatabaseResearchPurposeAPI(purposeDAO));
    }


    private DatabaseResearchPurposeAPI(ResearchPurposeDAO purposeDAO) {
        this.purposeDAO = purposeDAO;
    }

    public ResearchPurpose createResearchPurpose(ResearchPurpose rec) throws IllegalArgumentException{
        return null;
    }

    public ResearchPurpose updateResearchPurpose(ResearchPurpose rec, Integer id) throws IllegalArgumentException, NotFoundException{
        return null;
    }

    public ResearchPurpose describeResearchPurpose(Integer id) throws NotFoundException{
        return null;
    }

    public List<ResearchPurpose> describeResearchPurposes(List<Integer> id) throws NotFoundException {
        return null;
    }

    public void deleteResearchPurpose(Integer id) throws IllegalArgumentException, NotFoundException{

    }





}
