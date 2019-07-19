package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
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
    private UserRoleDAO userRoleDAO;

    @Inject
    public DacService(DacDAO dacDAO, UserRoleDAO userRoleDAO) {
        this.dacDAO = dacDAO;
        this.userRoleDAO = userRoleDAO;
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
        DACUser dacUser = dacDAO.findUserById(id);
        if (dacUser != null) {
            dacUser.setRoles(userRoleDAO.findRolesByUserId(dacUser.getDacUserId()));
        }
        return dacUser;
    }

    public List<DACUser> findMembersByDacId(Integer dacId) {
        return dacDAO.findMembersByDacId(dacId);
    }

    public DACUser addDacMember(Role role, DACUser user, Dac dac) {
        dacDAO.addDacMember(role.getRoleId(), user.getDacUserId(), dac.getDacId());
        return dacDAO.findUserById(user.getDacUserId());
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

}
