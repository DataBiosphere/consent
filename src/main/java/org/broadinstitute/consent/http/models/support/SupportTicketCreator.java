package org.broadinstitute.consent.http.models.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.enumeration.SupportRequestType;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserUpdateFields;

/**
 * Creates types of SupportTickets with the proper structure to send requests via Zendesk
 */
public class SupportTicketCreator {

  private final InstitutionDAO institutionDAO;
  private final UserDAO userDAO;
  private final ServicesConfiguration configuration;

  public SupportTicketCreator(InstitutionDAO institutionDAO, UserDAO userDAO,
      ServicesConfiguration configuration) {
    this.institutionDAO = institutionDAO;
    this.userDAO = userDAO;
    this.configuration = configuration;
  }

  /**
   * Generates a support ticket for a user selecting an existing or requesting an unfamiliar
   * institution and/or signing official
   *
   * @param userUpdateFields A UserUpdateFields object containing update information for the user
   * @param user             The user requesting the institution and/or signing official
   * @return A support ticket detailing the requested user fields
   */
  public SupportTicket createInstitutionSOSupportTicket(UserUpdateFields userUpdateFields,
      User user) {
    String suggestedInstitution = userUpdateFields.getSuggestedInstitution();
    Integer selectedInstitutionId = userUpdateFields.getInstitutionId();
    String suggestedSigningOfficial = userUpdateFields.getSuggestedSigningOfficial();
    Integer selectedSigningOfficialId = userUpdateFields.getSelectedSigningOfficialId();

    // Build up subject and description of ticket; using lists of strings to accommodate any combination of UserUpdateFields
    List<String> subjectItems = new ArrayList<>();
    List<String> descriptionItems = new ArrayList<>();
    if (Objects.nonNull(suggestedInstitution)) {
      addSuggestedInstitutionMessages(subjectItems, descriptionItems, suggestedInstitution);
    }
    if (Objects.nonNull(selectedInstitutionId)) {
      addSelectedInstitutionMessages(subjectItems, descriptionItems, selectedInstitutionId);
    }
    if (Objects.nonNull(suggestedSigningOfficial)) {
      addSuggestedSigningOfficialMessages(subjectItems, descriptionItems, suggestedSigningOfficial);
    }
    if (Objects.nonNull(selectedSigningOfficialId)) {
      addSelectedSigningOfficialMessages(subjectItems, descriptionItems, selectedSigningOfficialId);
    }

    //Append items for ticket subject and description
    String subject = String.format("%s user updates: %s",
        user.getDisplayName(),
        String.join(", ", subjectItems));
    String description = String.format("User %s [%s] has:\n%s",
        user.getDisplayName(),
        user.getEmail(),
        String.join("\n", descriptionItems));

    return new SupportTicket(
        user.getDisplayName(),
        SupportRequestType.TASK,
        user.getEmail(),
        subject,
        description,
        configuration.postSupportRequestUrl());
  }

  private void addSuggestedInstitutionMessages(List<String> subjectItems,
      List<String> descriptionItems, String suggestedInstitution) {
    subjectItems.add("New Institution Request");
    descriptionItems.add("- requested a new institution: " + suggestedInstitution);
  }

  private void addSelectedInstitutionMessages(List<String> subjectItems,
      List<String> descriptionItems, Integer selectedInstitutionId) {
    Institution selectedInstitution = institutionDAO.findInstitutionById(selectedInstitutionId);
    subjectItems.add("Institution Selection");
    if (Objects.isNull(selectedInstitution)) {
      descriptionItems.add(
          "- attempted to select institution with id " + selectedInstitutionId + " (not found)"
      );
    }
  }

  private void addSuggestedSigningOfficialMessages(List<String> subjectItems,
      List<String> descriptionItems, String suggestedSigningOfficial) {
    subjectItems.add("New Signing Official Request");
    descriptionItems.add("- requested a new signing official: " + suggestedSigningOfficial);
  }

  private void addSelectedSigningOfficialMessages(List<String> subjectItems,
      List<String> descriptionItems, Integer selectedSigningOfficialId) {
    User selectedSigningOfficial = userDAO.findUserById(selectedSigningOfficialId);
    subjectItems.add("Signing Official Selection");
    descriptionItems.add(
        Objects.nonNull(selectedSigningOfficial)
            ? String.format("- selected an existing signing official: %s, %s",
            selectedSigningOfficial.getDisplayName(),
            selectedSigningOfficial.getEmail())
            : "- attempted to select signing official with id " + selectedSigningOfficialId
                + " (not found)"
    );
  }
}
