package org.broadinstitute.consent.http.models.support;

import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.enumeration.SupportRequestType;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserUpdateFields;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SupportTicketFactory {

    private InstitutionDAO institutionDAO;
    private UserDAO userDAO;

    public SupportTicketFactory(InstitutionDAO institutionDAO, UserDAO userDAO) {
        this.institutionDAO = institutionDAO;
        this.userDAO = userDAO;
    }

    public SupportTicket createInstitutionSOSupportTicket(UserUpdateFields userUpdateFields, User user, String url) {
        String suggestedInstitution = userUpdateFields.getSuggestedInstitution();
        Integer selectedInstitutionId = userUpdateFields.getInstitutionId();
        String suggestedSigningOfficial = userUpdateFields.getSuggestedSigningOfficial();
        Integer selectedSigningOfficialId = userUpdateFields.getSelectedSigningOfficialId();

        //Generate subject and description of ticket based on provided fields
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
                url);
    }

    private static void addSuggestedInstitutionMessages(List<String> subjectItems, List<String> descriptionItems, String suggestedInstitution) {
        subjectItems.add("New Institution Request");
        descriptionItems.add("- requested a new institution: " + suggestedInstitution);
    }

    private static void addSelectedInstitutionMessages(List<String> subjectItems, List<String> descriptionItems, Integer selectedInstitutionId) {
        Institution selectedInstitution = institutionDAO.findInstitutionById(selectedInstitutionId);
        subjectItems.add("Institution Selection");
        descriptionItems.add(
                Objects.nonNull(selectedInstitution)
                        ? "- selected an existing institution: " + selectedInstitution.getName()
                        : "- attempted to select institution with id " + selectedInstitutionId + " (not found)"
        );
    }

    private static void addSuggestedSigningOfficialMessages(List<String> subjectItems, List<String> descriptionItems, String suggestedSigningOfficial) {
        subjectItems.add("New Signing Official Request");
        descriptionItems.add("- requested a new signing official: " + suggestedSigningOfficial);
    }

    private static void addSelectedSigningOfficialMessages(List<String> subjectItems, List<String> descriptionItems, Integer selectedSigningOfficialId) {
        User selectedSigningOfficial = userDAO.findUserById(selectedSigningOfficialId);
        subjectItems.add("Signing Official Selection");
        descriptionItems.add(
                Objects.nonNull(selectedSigningOfficial)
                        ? String.format("- selected an existing signing official: %s, %s",
                        selectedSigningOfficial.getDisplayName(),
                        selectedSigningOfficial.getEmail())
                        : "- attempted to select signing official with id " + selectedSigningOfficialId + " (not found)"
        );
    }

}
