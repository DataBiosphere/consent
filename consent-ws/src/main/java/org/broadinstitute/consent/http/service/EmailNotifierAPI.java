package org.broadinstitute.consent.http.service;

import freemarker.template.TemplateException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.mail.MessagingException;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Vote;
import org.bson.Document;

public interface EmailNotifierAPI {

    void sendCollectMessage(Integer electionId) throws MessagingException, IOException, TemplateException;

    void sendReminderMessage(Integer voteId) throws MessagingException, IOException, TemplateException;

    void sendNewCaseMessageToList(List<Vote> votes, Election election) throws MessagingException, IOException, TemplateException;

    void sendNewDARRequestMessage(String dataAccessRequestId) throws MessagingException, IOException, TemplateException;

    void sendDisabledDatasetsMessage(DACUser user, List<String> disabledDatasets, String dataAcessRequestId) throws MessagingException, IOException, TemplateException;

    void sendCancelDARRequestMessage(List<DACUser> userAddress, String dataAcessRequestId) throws MessagingException, IOException, TemplateException;

    void sendNeedsPIApprovalMessage(Map<DACUser, List<DataSet>> dataOwnersDataSets, Document access, Integer amountOfTime) throws MessagingException, IOException, TemplateException;

    void sendAdminFlaggedDarApproved(String darCode, List<DACUser> admins, Map<DACUser, List<DataSet>> dataOwnersDataSets) throws MessagingException, IOException, TemplateException;

    void sendClosedDataSetElectionsMessage(List<Election> elections) throws MessagingException, IOException, TemplateException;

    void sendUserDelegateResponsibilitiesMessage(DACUser newUser, Integer oldUser, String newRole, List<Vote> delegatedVotes) throws MessagingException, IOException, TemplateException;

    void sendNewResearcherCreatedMessage(Integer researcherId, String action) throws IOException, TemplateException, MessagingException;
}
