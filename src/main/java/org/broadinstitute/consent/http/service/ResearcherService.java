package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import java.util.List;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.UserPropertyDAO;
import org.broadinstitute.consent.http.enumeration.UserFields;
import org.broadinstitute.consent.http.models.UserProperty;


public class ResearcherService {

  private final UserPropertyDAO userPropertyDAO;
  private final UserDAO userDAO;

  @Inject
  public ResearcherService(UserPropertyDAO userPropertyDAO, UserDAO userDAO) {
    this.userPropertyDAO = userPropertyDAO;
    this.userDAO = userDAO;
  }

  public void deleteResearcherSpecificProperties(List<UserProperty> properties) {
    userPropertyDAO.deletePropertiesByUserAndKey(properties);
  }

  private void validateUser(Integer userId) {
    if (userDAO.findUserById(userId) == null) {
      throw new NotFoundException("User with id: " + userId + " does not exists");
    }
  }

  public List<UserProperty> describeUserProperties(Integer userId) {
    validateUser(userId);
    return userPropertyDAO.findUserPropertiesByUserIdAndPropertyKeys(userId,
        UserFields.getValues());
  }

}
