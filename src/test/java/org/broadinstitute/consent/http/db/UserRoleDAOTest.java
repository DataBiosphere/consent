package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Role;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UserRoleDAOTest extends DAOTestHelper {

    @Test
    public void testFindRolesByUserId() {
        User user = createUserWithRole(UserRoles.RESEARCHER.getRoleId());

        List<UserRole> roles = userRoleDAO.findRolesByUserId(user.getUserId());
        Assertions.assertEquals(1, roles.size());
    }

    @Test
    public void testFindRolesByUserEmail() {
        User user = createUserWithRole(UserRoles.RESEARCHER.getRoleId());

        List<String> roles = userRoleDAO.findRoleNamesByUserEmail(user.getEmail());
        Assertions.assertEquals(1, roles.size());
    }

    @Test
    public void testFindRolesByUserMixedCaseEmail() {
        User user = createUserWithRole(UserRoles.ADMIN.getRoleId());
        user.setEmail(randomizeCase(user.getEmail()));
        List<String> roles = userRoleDAO.findRoleNamesByUserEmail(user.getEmail());
        Assertions.assertEquals(1, roles.size());
    }

    private String randomizeCase(String string) {
        Random random = new Random();
        return Arrays
                .stream(string.split(""))
                .map(l -> random.nextBoolean() ? l.toUpperCase(Locale.ROOT) : l.toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(""));
    }

    @Test
    public void testFindRoles() {
        List<Role> roles = userRoleDAO.findRoles();
        Assertions.assertFalse(roles.isEmpty());
    }

    @Test
    public void testFindRoleIdByName() {
        List<String> roleNames = userRoleDAO.
                findRoles().
                stream().
                map(Role::getName).
                collect(Collectors.toList());
        roleNames.forEach(r -> Assertions.assertNotNull(userRoleDAO.findRoleIdByName(r)));
        Assertions.assertNull(userRoleDAO.findRoleIdByName("Not a real role"));
    }

    @Test
    public void testInsertUserRoles() {
        User user = createUserWithRole(UserRoles.RESEARCHER.getRoleId());
        UserRole r = new UserRole();
        r.setRoleId(UserRoles.MEMBER.getRoleId());
        userRoleDAO.insertUserRoles(Collections.singletonList(r), user.getUserId());
        Optional<User> updatedUser = userDAO.findUsersWithRoles(Collections.singletonList(user.getUserId())).stream().findFirst();
        assertTrue(updatedUser.isPresent());
        Assertions.assertEquals(2, updatedUser.get().getRoles().size());
    }

    @Test
    public void testUpdateUserRoles() {
        User user = createUserWithRole(UserRoles.MEMBER.getRoleId());
        List<UserRole> currentRoles = userRoleDAO.findRolesByUserId(user.getUserId());
        userRoleDAO.updateUserRoles(UserRoles.CHAIRPERSON.getRoleId(), user.getUserId(), UserRoles.MEMBER.getRoleId());
        List<UserRole> newRoles = userRoleDAO.findRolesByUserId(user.getUserId());
        Assertions.assertFalse(currentRoles.get(0).getRoleId().equals(newRoles.get(0).getRoleId()));
    }

    @Test
    public void testRemoveUserRoles() {
        User user = createUserWithRole(UserRoles.RESEARCHER.getRoleId());
        List<UserRole> currentRoles = userRoleDAO.findRolesByUserId(user.getUserId());
        Assertions.assertFalse(currentRoles.isEmpty());
        List<Integer> roleIds = userRoleDAO.findRoles().stream().map(Role::getRoleId).collect(Collectors.toList());
        userRoleDAO.removeUserRoles(user.getUserId(), roleIds);
        List<UserRole> newRoles = userRoleDAO.findRolesByUserId(user.getUserId());
        assertTrue(newRoles.isEmpty());
    }

    @Test
    public void testInsertSingleUserRole() {
        // No-op: tested in set up and tear down
    }

    @Test
    public void testRemoveSingleUserRole() {
        User user = createUserWithRole(UserRoles.RESEARCHER.getRoleId());
        List<UserRole> userRoles = userRoleDAO.findRolesByUserId(user.getUserId());
        Assertions.assertFalse(userRoles.isEmpty());
        List<Role> roles = userRoleDAO.findRoles();
        roles.forEach(r ->
                userRoleDAO.removeSingleUserRole(user.getUserId(), r.getRoleId())
        );

        List<UserRole> newUserRoles = userRoleDAO.findRolesByUserId(user.getUserId());
        assertTrue(newUserRoles.isEmpty());
    }

    @Test
    public void testFindRoleByNameAndUser() {
        User user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        Integer roleId = userRoleDAO.findRoleByNameAndUser(UserRoles.CHAIRPERSON.getRoleName(), user.getUserId());
        Assertions.assertNotNull(roleId);

        Integer invalidRoleId = userRoleDAO.findRoleByNameAndUser(UserRoles.MEMBER.getRoleName(), user.getUserId());
        Assertions.assertNull(invalidRoleId);
    }

    @Test
    public void testFindRoleByUserIdAndRoleId() {
        User user = createUserWithRole(UserRoles.DATAOWNER.getRoleId());
        UserRole userRole = userRoleDAO.findRoleByUserIdAndRoleId(user.getUserId(), UserRoles.DATAOWNER.getRoleId());
        Assertions.assertNotNull(userRole);
    }

}
