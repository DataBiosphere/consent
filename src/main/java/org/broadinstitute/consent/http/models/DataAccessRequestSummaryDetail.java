package org.broadinstitute.consent.http.models;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.enumeration.HeaderSummary;
import org.broadinstitute.consent.http.enumeration.VoteType;

public class DataAccessRequestSummaryDetail implements SummaryDetail {

  private DataAccessRequest dar;
  private Election accessElection;
  private List<Vote> accessVotes;
  private List<Vote> rpVotes;
  private List<Vote> consentVotes;
  private Match matchObject;
  private List<User> dacMembers;
  private User darUser;
  private Integer maxNumberOfDACMembers;

  public DataAccessRequestSummaryDetail(
      DataAccessRequest dar,
      Election accessElection,
      List<Vote> accessVotes,
      List<Vote> rpVotes,
      List<Vote> consentVotes,
      Match matchObject,
      List<User> dacMembers,
      User darUser,
      Integer maxNumberOfDACMembers) {

    setDar(dar);
    setAccessElection(accessElection);
    setAccessVotes(accessVotes);
    setRpVotes(rpVotes);
    setConsentVotes(consentVotes);
    setMatchObject(matchObject);
    setDacMembers(dacMembers);
    setDarUser(darUser);
    setMaxNumberOfDACMembers(maxNumberOfDACMembers);
  }

  private static final String TAB = "\t";
  private static final String MANUAL_REVIEW = "Manual Review";

  public String headers() {
    StringBuilder builder = new StringBuilder();
    builder
        .append(HeaderSummary.DATA_REQUEST_ID.getValue())
        .append(TAB)
        .append(HeaderSummary.DATE.getValue())
        .append(TAB)
        .append(HeaderSummary.CHAIRPERSON.getValue())
        .append(TAB)
        .append(HeaderSummary.FINAL_DECISION.getValue())
        .append(TAB)
        .append(HeaderSummary.FINAL_DECISION_RATIONALE.getValue())
        .append(TAB)
        .append(HeaderSummary.VAULT_DECISION.getValue())
        .append(TAB)
        .append(HeaderSummary.VAULT_VS_DAC_AGREEMENT.getValue())
        .append(TAB)
        .append(HeaderSummary.CHAIRPERSON_FEEDBACK.getValue())
        .append(TAB)
        .append(HeaderSummary.RESEARCHER.getValue())
        .append(TAB)
        .append(HeaderSummary.PROJECT_TITLE.getValue())
        .append(TAB)
        .append(HeaderSummary.DATASET_ID.getValue())
        .append(TAB)
        .append(HeaderSummary.DATA_ACCESS_SUBM_DATE.getValue())
        .append(TAB);
    IntStream.rangeClosed(1, getMaxNumberOfDACMembers())
        .forEach(i -> builder.append(HeaderSummary.DAC_MEMBERS.getValue()).append(TAB));
    builder
        .append(HeaderSummary.REQUIRE_MANUAL_REVIEW.getValue())
        .append(TAB)
        .append(HeaderSummary.FINAL_DECISION_DAR.getValue())
        .append(TAB)
        .append(HeaderSummary.FINAL_RATIONALE_DAR.getValue())
        .append(TAB)
        .append(HeaderSummary.FINAL_DECISION_RP.getValue())
        .append(TAB)
        .append(HeaderSummary.FINAL_RATIONALE_RP.getValue())
        .append(TAB)
        .append(HeaderSummary.FINAL_DECISION_DUL.getValue())
        .append(TAB)
        .append(HeaderSummary.FINAL_RATIONALE_DUL.getValue());
    return builder.toString();
  }

  @Override
  public String toString() {
    List<String> dataSetUUIds =
        getDar().getData().getDatasetIds().stream()
            .map(DataSet::parseAliasToIdentifier)
            .collect(Collectors.toUnmodifiableList());
    Optional<Vote> chairPersonRPVote =
        getRpVotes().stream()
            .filter(v -> v.getType().equalsIgnoreCase(VoteType.CHAIRPERSON.getValue()))
            .filter(v -> Objects.nonNull(v.getVote()))
            .findFirst();
    Optional<Vote> chairPersonAccessVote =
        getAccessVotes().stream()
            .filter(v -> v.getType().equalsIgnoreCase(VoteType.CHAIRPERSON.getValue()))
            .filter(v -> Objects.nonNull(v.getVote()))
            .findFirst();
    Optional<Vote> chairPersonConsentVote =
        getConsentVotes().stream()
            .filter(v -> v.getType().equalsIgnoreCase(VoteType.CHAIRPERSON.getValue()))
            .filter(v -> Objects.nonNull(v.getVote()))
            .findFirst();
    Optional<Vote> finalVote =
        getAccessVotes().stream()
            .filter(v -> v.getType().equalsIgnoreCase(VoteType.FINAL.getValue()))
            .filter(v -> Objects.nonNull(v.getVote()))
            .findFirst();
    Optional<User> chairpersonUser =
        finalVote.flatMap(
            vote ->
                getDacMembers().stream()
                    .filter(u -> u.getDacUserId().equals(vote.getDacUserId()))
                    .findFirst());
    Boolean agreement =
        (finalVote.isPresent() && Objects.nonNull(getMatchObject()))
            ? finalVote.get().getVote().equals(getMatchObject().getMatch())
            : null;

    StringBuilder builder = new StringBuilder();
    builder.append(getDar().getData().getDarCode()).append(TAB);
    builder.append(formatLongToDate(getAccessElection().getCreateDate().getTime())).append(TAB);
    if (chairpersonUser.isPresent()) {
      builder.append(chairpersonUser.get().getDisplayName()).append(TAB);
    } else {
      builder.append(nullToString(null)).append(TAB);
    }
    appendVoteDetails(finalVote, builder);
    if (Objects.nonNull(getMatchObject())) {
      builder.append(booleanToString(getMatchObject().getMatch())).append(TAB);
    } else {
      builder.append(MANUAL_REVIEW + TAB);
    }
    Vote agreementVote = new Vote();
    agreementVote.setVote(agreement);
    agreementVote.setRationale("");
    appendVoteDetails(Optional.of(agreementVote), builder);
    builder.append(getDarUser().getDisplayName()).append(TAB);
    builder.append(getDar().getData().getProjectTitle()).append(TAB);
    builder.append(StringUtils.join(dataSetUUIds, ",")).append(TAB);
    builder.append(formatLongToDate(getDar().getSortDate().getTime())).append(TAB);
    for (User user : getDacMembers()) {
      builder.append(user.getDisplayName()).append(TAB);
    }
    // Append extra tabs for the case where there are more DAC members in other rows
    // than there are DAC members for this row.
    builder.append(TAB.repeat(Math.max(0, (getMaxNumberOfDACMembers() - getDacMembers().size()))));
    builder
        .append(booleanToString(Objects.isNull(getDar().getData().getRestriction())))
        .append(TAB);

    appendVoteDetails(chairPersonAccessVote, builder);
    appendVoteDetails(chairPersonRPVote, builder);
    appendVoteDetails(chairPersonConsentVote, builder);
    return builder.toString();
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private void appendVoteDetails(Optional<Vote> vote, StringBuilder builder) {
    if (vote.isPresent()) {
      builder.append(booleanToString(vote.get().getVote())).append(TAB);
      builder.append(nullToString(vote.get().getRationale())).append(TAB);
    } else {
      builder.append(nullToString(null)).append(TAB);
      builder.append(nullToString(null)).append(TAB);
    }
  }

  private DataAccessRequest getDar() {
    return dar;
  }

  private void setDar(DataAccessRequest dar) {
    this.dar = dar;
  }

  private Election getAccessElection() {
    return accessElection;
  }

  private void setAccessElection(Election accessElection) {
    this.accessElection = accessElection;
  }

  private List<Vote> getAccessVotes() {
    return accessVotes;
  }

  private void setAccessVotes(List<Vote> accessVotes) {
    this.accessVotes = accessVotes;
  }

  private List<Vote> getRpVotes() {
    return rpVotes;
  }

  private void setRpVotes(List<Vote> rpVotes) {
    this.rpVotes = rpVotes;
  }

  private List<Vote> getConsentVotes() {
    return consentVotes;
  }

  private void setConsentVotes(List<Vote> consentVotes) {
    this.consentVotes = consentVotes;
  }

  private List<User> getDacMembers() {
    return dacMembers;
  }

  private void setDacMembers(List<User> dacMembers) {
    this.dacMembers = dacMembers;
  }

  private User getDarUser() {
    return darUser;
  }

  private void setDarUser(User darUser) {
    this.darUser = darUser;
  }

  private Match getMatchObject() {
    return matchObject;
  }

  private void setMatchObject(Match matchObject) {
    this.matchObject = matchObject;
  }

  private Integer getMaxNumberOfDACMembers() {
    return maxNumberOfDACMembers;
  }

  private void setMaxNumberOfDACMembers(Integer maxNumberOfDACMembers) {
    this.maxNumberOfDACMembers = maxNumberOfDACMembers;
  }
}
