package org.genomebridge.consent.http.service;

import freemarker.template.TemplateException;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.List;

public interface EmailNotifierAPI {

    void sendCollectMessage(Integer electionId) throws MessagingException, IOException, TemplateException;

    void sendReminderMessage(Integer voteId) throws MessagingException, IOException, TemplateException;

    void sendNewCaseMessage(Integer voteId) throws MessagingException, IOException, TemplateException;

    void sendNewCaseMessageToList(List<Integer> votes) throws MessagingException, IOException, TemplateException;
}
