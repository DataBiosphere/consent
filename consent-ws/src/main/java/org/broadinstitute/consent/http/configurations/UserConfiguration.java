package org.broadinstitute.consent.http.configurations;

import javax.validation.constraints.NotNull;

public class UserConfiguration {

    @NotNull
    public String user;

    @NotNull
    public String password;

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
