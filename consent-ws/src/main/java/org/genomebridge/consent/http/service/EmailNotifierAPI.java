package org.genomebridge.consent.http.service;

import freemarker.template.TemplateException;
import org.genomebridge.consent.http.models.Election;
import org.genomebridge.consent.http.models.Vote;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.List;

public interface EmailNotifierAPI {

    void sendCollectMessage(Integer electionId) throws MessagingException, IOException, TemplateException;

    void sendReminderMessage(Integer voteId) throws MessagingException, IOException, TemplateException;

    void sendNewCaseMessage(Integer voteId) throws MessagingException, IOException, TemplateException;

    void sendNewCaseMessageToList(List<Vote> votes, Election election) throws MessagingException, IOException, TemplateException;
}
