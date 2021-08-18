package org.broadinstitute.consent.http.configurations;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MailConfiguration {

    @NotNull
    boolean activateEmailNotifications;

    @NotNull
    String googleAccount;

    @NotNull
    String sendGridApiKey;

    public boolean isActivateEmailNotifications() {
        return activateEmailNotifications;
    }

    public void setActivateEmailNotifications(boolean activateEmailNotifications) {
        this.activateEmailNotifications = activateEmailNotifications;
    }

    public String getGoogleAccount() {
        return googleAccount;
    }

    public void setGoogleAccount(String googleAccount) {
        this.googleAccount = googleAccount;
    }

    public String getSendGridApiKey() {
        return sendGridApiKey;
    }

    public void setSendGridApiKey(String sendGridApiKey) {
        this.sendGridApiKey = sendGridApiKey;
    }
}
