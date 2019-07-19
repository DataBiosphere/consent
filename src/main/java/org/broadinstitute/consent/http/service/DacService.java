package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.Role;
import org.broadinstitute.consent.http.models.UserRole;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class DacService {

    private DacDAO dacDAO;

    @Inject
    public DacService(DacDAO dacDAO) {
        this.dacDAO = dacDAO;
    }

    public List<Dac> findAll() {
        return dacDAO.findAll();
    }

    public Dac findById(Integer dacId) {
        return dacDAO.findById(dacId);
    }

    public Integer createDac(String name, String description) {
        Date createDate = new Date();
        return dacDAO.createDac(name, description, createDate);
    }

    public void updateDac(String name, String description, Integer dacId) {
        Date updateDate = new Date();
        dacDAO.updateDac(name, description, updateDate, dacId);
    }

    public void deleteDac(Integer dacId) {
        dacDAO.deleteDacMembers(dacId);
        dacDAO.deleteDac(dacId);
    }

    public DACUser findUserById(Integer id) throws IllegalArgumentException {
        return populatedUserById(id);
    }

    public List<DACUser> findMembersByDacId(Integer dacId) {
        List<DACUser> dacUsers = dacDAO.findMembersByDacId(dacId);
        dacUsers.forEach(u -> u.setRoles(dacDAO.findUserRolesForUser(u.getDacUserId())));
        return dacUsers;
    }

    public DACUser addDacMember(Role role, DACUser user, Dac dac) {
        dacDAO.addDacMember(role.getRoleId(), user.getDacUserId(), dac.getDacId());
        return populatedUserById(user.getDacUserId());
    }

    public void removeDacMember(Role role, DACUser user, Dac dac) {
        List<UserRole> dacRoles = user.
                getRoles().
                stream().
                filter(r -> r.getDacId() != null).
                filter(r -> r.getDacId().equals(dac.getDacId())).
                filter(r -> r.getRoleId().equals(role.getRoleId())).
                collect(Collectors.toList());
        dacRoles.forEach(userRole -> dacDAO.removeDacMember(userRole.getUserRoleId()));
    }

    public Role getChairpersonRole() {
        return dacDAO.getRoleById(UserRoles.CHAIRPERSON.getRoleId());
    }

    public Role getMemberRole() {
        return dacDAO.getRoleById(UserRoles.MEMBER.getRoleId());
    }

    private DACUser populatedUserById(Integer userId) {
        DACUser user = dacDAO.findUserById(userId);
        user.setRoles(dacDAO.findUserRolesForUser(userId));
        return user;
    }

}
