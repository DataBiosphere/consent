package org.broadinstitute.consent.http.configurations;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MailConfiguration {

    @NotNull
    private boolean activateEmailNotifications;

    @NotNull
    private String googleAccount;

    @NotNull
    private String sendGridApiKey;

    @NotNull
    private String sendGridStatusUrl;

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

    public String getSendGridStatusUrl() {
        return sendGridStatusUrl;
    }

    public void setSendGridStatusUrl(String sendGridStatusUrl) {
        this.sendGridStatusUrl = sendGridStatusUrl;
    }
}
