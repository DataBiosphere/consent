package org.broadinstitute.consent.http.models;

import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

public class UserUpdateFieldsTest {

    private static final List<Integer> ALL_ROLE_IDS = Stream.of(UserRoles.values()).map(UserRoles::getRoleId).collect(Collectors.toList());
    private static final List<Integer> NON_IGNORABLE_ROLES = ALL_ROLE_IDS.stream().filter(id -> !UserUpdateFields.IGNORE_ROLE_IDS.contains(id)).collect(Collectors.toList());

    @Test
    public void testGetRoleIdsToAdd_Case_1() {
        UserUpdateFields fields = new UserUpdateFields();
        fields.setUserRoleIds(ALL_ROLE_IDS);
        // If the user has all role ids, and we're updating the user to have all roles,
        // then roles to add should be empty
        List<Integer> roleIdsToAdd = fields.getRoleIdsToAdd(ALL_ROLE_IDS);
        assertTrue(roleIdsToAdd.isEmpty());
    }

    @Test
    public void testGetRoleIdsToAdd_Case_2() {
        UserUpdateFields fields = new UserUpdateFields();
        fields.setUserRoleIds(ALL_ROLE_IDS);
        // If the user has no role ids, then roles to add = all non-ignorable roles
        List<Integer> roleIdsToAdd = fields.getRoleIdsToAdd(List.of());
        assertEquals(NON_IGNORABLE_ROLES, roleIdsToAdd);
    }

    @Test
    public void testGetRoleIdsToAdd_Case_3() {
        UserUpdateFields fields = new UserUpdateFields();
        fields.setUserRoleIds(List.of(100, 200, 300, 400));
        // Role ids outside the range of existing roles should not be added
        List<Integer> roleIdsToAdd = fields.getRoleIdsToAdd(List.of());
        assertTrue(roleIdsToAdd.isEmpty());
    }

    @Test
    public void testGetRoleIdsToRemove_Case_1() {
        UserUpdateFields fields = new UserUpdateFields();
        fields.setUserRoleIds(ALL_ROLE_IDS);
        // If the user has all role ids, and we're updating the user to have all roles,
        // then roles to remove should be empty
        List<Integer> roleIdsToRemove = fields.getRoleIdsToRemove(ALL_ROLE_IDS);
        assertTrue(roleIdsToRemove.isEmpty());
    }

    @Test
    public void testGetRoleIdsToRemove_Case_2() {
        UserUpdateFields fields = new UserUpdateFields();
        fields.setUserRoleIds(List.of());
        // If the user has all role ids, and we're updating the user to have NO roles,
        // then roles to remove should not be empty
        List<Integer> roleIdsToRemove = fields.getRoleIdsToRemove(ALL_ROLE_IDS);
        assertFalse(roleIdsToRemove.isEmpty());
        // We can never remove the ignorable roles, so they should not be in the list
        roleIdsToRemove.forEach(id -> {
            assertFalse(UserUpdateFields.IGNORE_ROLE_IDS.contains(id));
        });
        // We can also never remove the Researcher role from a user
        assertFalse(roleIdsToRemove.contains(UserRoles.RESEARCHER.getRoleId()));
    }

    @Test
    public void testGetRoleIdsToRemove_Case_3() {
        UserUpdateFields fields = new UserUpdateFields();
        fields.setUserRoleIds(ALL_ROLE_IDS);
        List<Integer> invalidRoleIds = List.of(100, 200, 300, 400);
        fields.getUserRoleIds().addAll(invalidRoleIds);
        // Role ids outside the range of existing roles should not be removed
        List<Integer> roleIdsToRemove = fields.getRoleIdsToRemove(ALL_ROLE_IDS);
        assertTrue(roleIdsToRemove.isEmpty());
    }

}
