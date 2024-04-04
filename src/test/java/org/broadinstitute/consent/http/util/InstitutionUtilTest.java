package org.broadinstitute.consent.http.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InstitutionUtilTest {

  private final List<UserRole> adminRoles = Collections.singletonList(
      new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName()));
  private final List<UserRole> researcherRoles = Collections.singletonList(
      new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName()));
  private final User adminUser = new User(1, "Admin", "Display Name", new Date(), adminRoles);
  private final User researcherUser = new User(1, "Researcher", "Display Name", new Date(),
      researcherRoles);

  private InstitutionUtil util;

  private Institution initMockInstitution() {
    Institution mockInstitution = new Institution();
    mockInstitution.setName("Test Name");
    mockInstitution.setCreateDate(new Date());
    mockInstitution.setCreateUserId(1);
    mockInstitution.setUpdateDate(new Date());
    mockInstitution.setUpdateUserId(1);
    mockInstitution.setId(1);
    return mockInstitution;
  }

  @BeforeEach
  void setUp() {
    util = new InstitutionUtil();
  }

  @Test
  void testCheckIfAdminAdmin() {
    assertTrue(util.checkIfAdmin(adminUser));
    assertFalse(util.checkIfAdmin(researcherUser));
    assertFalse(util.checkIfAdmin(new User()));
  }

  @Test
  void testGsonBuilderAdmin() {
    Institution mockInstitution = initMockInstitution();
    Gson builder = util.getGsonBuilder(true);
    String json = builder.toJson(mockInstitution);
    Institution deserialized = new Gson().fromJson(json, Institution.class);
    assertEquals(mockInstitution.getName(), deserialized.getName());
    assertEquals(mockInstitution.getCreateUserId(), deserialized.getCreateUserId());
    assertEquals(mockInstitution.getUpdateUserId(), deserialized.getUpdateUserId());
    assertEquals(mockInstitution.getCreateDate().toString(),
        deserialized.getCreateDate().toString());
    assertEquals(mockInstitution.getUpdateDate().toString(),
        deserialized.getUpdateDate().toString());
    assertEquals(mockInstitution.getId(), deserialized.getId());
  }

  @Test
  void testGsonBuilderNonAdmin() {
    Institution mockInstitution = initMockInstitution();
    Gson builder = util.getGsonBuilder(false);
    String json = builder.toJson(mockInstitution);
    assertEquals("{\"id\":1,\"name\":\"Test Name\"}", json);
  }
}
