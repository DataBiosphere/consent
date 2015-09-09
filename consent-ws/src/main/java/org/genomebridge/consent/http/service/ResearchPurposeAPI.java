package org.genomebridge.consent.http.service;

import org.bson.Document;
import org.genomebridge.consent.http.models.ResearchPurpose;

import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.util.List;

public interface ResearchPurposeAPI {

    Document createResearchPurpose(ResearchPurpose rec) throws IllegalArgumentException;

    Document updateResearchPurpose(ResearchPurpose rec, String id) throws IllegalArgumentException, NotFoundException;

    Document describeResearchPurpose(String id) throws NotFoundException, IOException;

    List<Document> describeResearchPurposes(String[] id) throws NotFoundException;

    void deleteResearchPurpose(String id) throws IllegalArgumentException, NotFoundException;

}
