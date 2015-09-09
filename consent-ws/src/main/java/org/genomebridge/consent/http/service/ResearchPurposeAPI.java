package org.genomebridge.consent.http.service;

import org.genomebridge.consent.http.models.ResearchPurpose;

import javax.ws.rs.NotFoundException;
import java.util.List;

public interface ResearchPurposeAPI {

    ResearchPurpose createResearchPurpose(ResearchPurpose rec) throws IllegalArgumentException;

    ResearchPurpose updateResearchPurpose(ResearchPurpose rec, Integer id) throws IllegalArgumentException, NotFoundException;

    ResearchPurpose describeResearchPurpose(Integer id) throws NotFoundException;

    List<ResearchPurpose> describeResearchPurposes(List<Integer> id) throws NotFoundException;

    void deleteResearchPurpose(Integer id) throws IllegalArgumentException, NotFoundException;

}
