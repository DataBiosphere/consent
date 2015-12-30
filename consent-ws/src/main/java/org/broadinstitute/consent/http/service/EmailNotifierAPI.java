package org.broadinstitute.consent.http.service;

import freemarker.template.TemplateException;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Vote;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.List;

public interface EmailNotifierAPI {

    void sendCollectMessage(Integer electionId) throws MessagingException, IOException, TemplateException;

    void sendReminderMessage(Integer voteId) throws MessagingException, IOException, TemplateException;

    void sendNewCaseMessageToList(List<Vote> votes, Election election) throws MessagingException, IOException, TemplateException;

    void sendNewDARRequestMessage(String dataAccessRequestId) throws MessagingException, IOException, TemplateException;

    void sendDisabledDatasetsMessage(DACUser user, List<String> disabledDatasets, String dataAcessRequestId) throws MessagingException, IOException, TemplateException;

    void sendCancelDARRequestMessage(List<DACUser> userAddress, String dataAcessRequestId) throws MessagingException, IOException, TemplateException;

}
