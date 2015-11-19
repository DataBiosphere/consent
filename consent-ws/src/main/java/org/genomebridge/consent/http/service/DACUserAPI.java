package org.genomebridge.consent.http.service;

import org.genomebridge.consent.http.models.DACUser;

import javax.ws.rs.NotFoundException;
import java.util.Collection;
import java.util.List;

public interface DACUserAPI {

    DACUser createDACUser(DACUser dacUser) throws IllegalArgumentException;

    DACUser describeDACUserByEmail(String email) throws NotFoundException;

    DACUser describeChairpersonUser() throws NotFoundException;

    Collection<DACUser> describeAdminUsers() throws NotFoundException;

    DACUser describeDACUserById(Integer id) throws IllegalArgumentException;

    DACUser updateDACUserById(DACUser rec, Integer userId) throws IllegalArgumentException, NotFoundException;

    void deleteDACUser(String email) throws IllegalArgumentException, NotFoundException;

    void updateExistentChairPersonToAlumni(Integer dacUserID);

    Collection<DACUser> describeUsers();

    Collection<String> describeUsersEmails(List<Integer> dacUserIds);
}
