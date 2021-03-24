package org.broadinstitute.consent.http.util;

import com.google.gson.Gson;

import java.util.Date;
import java.util.List;
import java.util.Collections;

import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.junit.Before;
import org.junit.Test;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InstitutionUtilTest {
  private final List<UserRole> adminRoles = Collections.singletonList(new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName()));
  private final List<UserRole> researcherRoles = Collections.singletonList(new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName()));
  private final User adminUser = new User(1, "Admin", "Display Name", new Date(), adminRoles, "Admin");
  private final User researcherUser = new User(1, "Researcher", "Display Name", new Date(), researcherRoles, "Researcher");

  private InstitutionUtil util;

  private Institution initMockInstitution() {
    Institution mockInstitution = new Institution();
    mockInstitution.setName("Test Name");
    mockInstitution.setCreateDate(new Date());
    mockInstitution.setCreateUser(1);
    mockInstitution.setUpdateDate(new Date());
    mockInstitution.setUpdateUser(1);
    mockInstitution.setId(1);
    return mockInstitution;
  }

  @Before
  public void setUp() {
    util = new InstitutionUtil();
  }

  @Test
  public void testCheckIfAdmin() {
    Boolean adminResult = util.checkIfAdmin(adminUser);
    Boolean researcherResult = util.checkIfAdmin(researcherUser);
    assertTrue(adminResult);
    assertFalse(researcherResult);
  }

  @Test
  public void testGsonBuilderAdmin() {
    Institution mockInstitution = initMockInstitution();
    Gson builder = util.getGsonBuilder(true);
    String json = builder.toJson(mockInstitution);
    Institution deserialized = new Gson().fromJson(json, Institution.class);
    assertEquals(mockInstitution.getName(), deserialized.getName());
    assertEquals(mockInstitution.getCreateUser(), deserialized.getCreateUser());
    assertEquals(mockInstitution.getUpdateUser(), deserialized.getUpdateUser());
    assertEquals(mockInstitution.getCreateDate().toString(), deserialized.getCreateDate().toString());
    assertEquals(mockInstitution.getUpdateDate().toString(), deserialized.getUpdateDate().toString());
    assertEquals(mockInstitution.getId(), deserialized.getId());
  }

  @Test
  public void testGsonBuilderNonAdmin() {
    Institution mockInstitution = initMockInstitution();
    Gson builder = util.getGsonBuilder(false);
    String json = builder.toJson(mockInstitution);
    assertEquals("{\"id\":1,\"name\":\"Test Name\"}", json);
  }
}
