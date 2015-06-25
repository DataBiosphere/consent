package org.genomebridge.consent.http.service;

import org.genomebridge.consent.http.models.Election;

import com.sun.jersey.api.NotFoundException;

public interface ElectionAPI {

   public Election createElection(Election rec, String referenceId, Boolean isConsent) throws IllegalArgumentException;
   
   public Election updateElectionById(Election rec, Integer electionId) throws IllegalArgumentException, NotFoundException;
   
   public Election describeConsentElection(String consentId) throws  NotFoundException;
   
   public Election describeDataRequestElection(Integer requestId) throws  NotFoundException;
   
   public void deleteElection(String referenceId) throws IllegalArgumentException, NotFoundException;
   
}
