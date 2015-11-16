package org.genomebridge.consent.http.configurations;

import javax.validation.constraints.NotNull;

public class MailConfiguration {

    @NotNull
    public String host;

    @NotNull
    public boolean activateEmailNotifications;

    @NotNull
    public String smtpPort;

    @NotNull
    public String smtpAuth;

    @NotNull
    public String googleAccount;

    @NotNull
    public String accountPassword;

    @NotNull
    public String smtpStartTlsEnable;

    public boolean isActivateEmailNotifications() {
        return activateEmailNotifications;
    }

    public void setActivateEmailNotifications(boolean activateEmailNotifications) {
        this.activateEmailNotifications = activateEmailNotifications;
    }

    public String getSmtpPort() {
        return smtpPort;
    }

    public void setSmtpPort(String smtpPort) {
        this.smtpPort = smtpPort;
    }

    public String getSmtpAuth() {
        return smtpAuth;
    }

    public void setSmtpAuth(String smtpAuth) {
        this.smtpAuth = smtpAuth;
    }

    public String getSmtpStartTlsEnable() {
        return smtpStartTlsEnable;
    }

    public void setSmtpStartTlsEnable(String smtpStartTlsEnable) {
        this.smtpStartTlsEnable = smtpStartTlsEnable;
    }

    public String getGoogleAccount() {
        return googleAccount;
    }

    public void setGoogleAccount(String googleAccount) {
        this.googleAccount = googleAccount;
    }

    public String getAccountPassword() {
        return accountPassword;
    }

    public void setAccountPassword(String accountPassword) {
        this.accountPassword = accountPassword;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }
}
