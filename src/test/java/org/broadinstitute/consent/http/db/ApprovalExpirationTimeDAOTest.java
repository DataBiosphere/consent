package org.broadinstitute.consent.http.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import org.broadinstitute.consent.http.models.ApprovalExpirationTime;
import org.broadinstitute.consent.http.models.User;
import org.junit.Test;

public class ApprovalExpirationTimeDAOTest extends DAOTestHelper {

  private final Integer DAYS = 7;

  @Test
  public void testInsert() {
    User user = createUser();
    Integer aetId = approvalExpirationTimeDAO.insertApprovalExpirationTime(new Date(), DAYS, user.getDacUserId());
    assertTrue(aetId > 0);
  }

  @Test
  public void testGet() {
    ApprovalExpirationTime aet = approvalExpirationTimeDAO.findApprovalExpirationTime();
    assertNull(aet);
  }

  @Test
  public void testGetById() {
    User user = createUser();
    Integer aetId = approvalExpirationTimeDAO.insertApprovalExpirationTime(new Date(), DAYS, user.getDacUserId());
    ApprovalExpirationTime aet = approvalExpirationTimeDAO.findApprovalExpirationTimeById(aetId);
    assertEquals(aetId, aet.getId());
  }

  @Test
  public void testUpdateById() {
    User user = createUser();
    Integer updatedDays = 100;
    Integer aetId =
        approvalExpirationTimeDAO.insertApprovalExpirationTime(new Date(), DAYS, user.getDacUserId());
    approvalExpirationTimeDAO.updateApprovalExpirationTime(aetId, updatedDays, new Date(), user.getDacUserId());
    ApprovalExpirationTime aet = approvalExpirationTimeDAO.findApprovalExpirationTimeById(aetId);
    assertEquals(aetId, aet.getId());
    assertEquals(updatedDays, aet.getAmountOfDays());
  }

  @Test
  public void testDeleteById() {
    User user = createUser();
    Integer aetId = approvalExpirationTimeDAO.insertApprovalExpirationTime(new Date(), DAYS, user.getDacUserId());
    approvalExpirationTimeDAO.deleteApprovalExpirationTime(aetId);
    ApprovalExpirationTime aet = approvalExpirationTimeDAO.findApprovalExpirationTimeById(aetId);
    assertNull(aet);
  }
}
