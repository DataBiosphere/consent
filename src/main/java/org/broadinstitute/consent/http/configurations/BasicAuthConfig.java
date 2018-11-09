package org.broadinstitute.consent.http.configurations;

import org.broadinstitute.consent.http.authentication.BasicUser;

import javax.validation.constraints.NotNull;
import java.util.List;

public class BasicAuthConfig {


    @NotNull
    public List<BasicUser> users;

    public List<BasicUser> getUsers() {
        return users;
    }

    public void setUsers(List<BasicUser> users) {
        this.users = users;
    }
}
