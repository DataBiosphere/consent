package org.broadinstitute.consent.http.models;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;
import org.broadinstitute.consent.http.enumeration.HeaderSummary;
import org.broadinstitute.consent.http.enumeration.VoteType;

public class ConsentSummaryDetail implements SummaryDetail {

  private Election election;
  private Consent consent;
  private List<Vote> electionVotes;
  private Collection<User> electionUsers;
  private Vote chairPersonVote;
  private User chairPerson;
  private Integer maxNumberOfElectionUsers;

  public ConsentSummaryDetail(
      Election election,
      Consent consent,
      List<Vote> electionVotes,
      Collection<User> electionUsers,
      Vote chairPersonVote,
      User chairPerson,
      Integer maxNumberOfElectionUsers) {
    setElection(election);
    setConsent(consent);
    setElectionVotes(electionVotes);
    setElectionUsers(electionUsers);
    setChairPersonVote(chairPersonVote);
    setChairPerson(chairPerson);
    setMaxNumberOfElectionUsers(maxNumberOfElectionUsers);
  }

  private static final String TAB = "\t";

  public String headers() {
    StringBuilder builder = new StringBuilder();
    builder
        .append(HeaderSummary.CONSENT.getValue())
        .append(TAB)
        .append(HeaderSummary.VERSION.getValue())
        .append(TAB)
        .append(HeaderSummary.STATUS.getValue())
        .append(TAB)
        .append(HeaderSummary.ARCHIVED.getValue())
        .append(TAB)
        .append(HeaderSummary.STRUCT_LIMITATIONS.getValue())
        .append(TAB)
        .append(HeaderSummary.DATE.getValue())
        .append(TAB)
        .append(HeaderSummary.CHAIRPERSON.getValue())
        .append(TAB)
        .append(HeaderSummary.FINAL_DECISION.getValue())
        .append(TAB)
        .append(HeaderSummary.FINAL_DECISION_RATIONALE.getValue())
        .append(TAB);
    IntStream.rangeClosed(1, getMaxNumberOfElectionUsers())
        .forEach(
            i ->
                builder
                    .append(HeaderSummary.USER.getValue())
                    .append(TAB)
                    .append(HeaderSummary.VOTE.getValue())
                    .append(TAB)
                    .append(HeaderSummary.RATIONALE.getValue())
                    .append(TAB));
    builder
        .append(HeaderSummary.USER.getValue())
        .append(TAB)
        .append(HeaderSummary.VOTE.getValue())
        .append(TAB)
        .append(HeaderSummary.RATIONALE.getValue())
        .append(System.lineSeparator());
    return builder.toString();
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    if (Objects.nonNull(getConsent())) {
      builder.append(delimiterCheck(getConsent().getName()));
    }
    builder.append(TAB);
    builder.append(getElection().getVersion());
    builder.append(TAB);
    builder.append(getElection().getStatus());
    builder.append(TAB);
    builder.append(booleanToString(getElection().getArchived()));
    builder.append(TAB);
    builder.append(delimiterCheck(getConsent().getTranslatedUseRestriction()));
    builder.append(TAB);
    builder.append(formatLongToDate(getConsent().getCreateDate().getTime()));
    builder.append(TAB);
    if (Objects.nonNull(getChairPerson())) {
      builder.append(nullToString(getChairPerson().getDisplayName()));
    }
    builder.append(TAB);
    if (Objects.nonNull(getChairPersonVote())) {
      builder.append(booleanToString(getChairPersonVote().getVote()));
    }
    builder.append(TAB);
    if (Objects.nonNull(getChairPersonVote())) {
      builder.append(nullToString(getChairPersonVote().getRationale()));
    }
    builder.append(TAB);
    getElectionVotes().stream()
        .filter(ev -> ev.getType().equals(VoteType.DAC.getValue()))
        .forEach(
            v -> {
              Optional<User> user =
                  getElectionUsers().stream()
                      .filter(u -> v.getDacUserId().equals(u.getDacUserId()))
                      .findFirst();
              user.ifPresent(value -> builder.append(value.getDisplayName()));
              builder.append(TAB);
              builder.append(booleanToString(v.getVote()));
              builder.append(TAB);
              builder.append(nullToString(v.getRationale()));
              builder.append(TAB);
              // Append extra tabs for the case where there are more election users in other rows
              // than there are election users for this row.
              builder.append(
                  TAB.repeat(
                      Math.max(0, (getMaxNumberOfElectionUsers() - getElectionUsers().size()))));
            });
    return builder.toString();
  }

  private Election getElection() {
    return election;
  }

  private void setElection(Election election) {
    this.election = election;
  }

  private Consent getConsent() {
    return consent;
  }

  private void setConsent(Consent consent) {
    this.consent = consent;
  }

  private List<Vote> getElectionVotes() {
    return electionVotes;
  }

  private void setElectionVotes(List<Vote> electionVotes) {
    this.electionVotes = electionVotes;
  }

  private Collection<User> getElectionUsers() {
    return electionUsers;
  }

  private void setElectionUsers(Collection<User> electionUsers) {
    this.electionUsers = electionUsers;
  }

  private Vote getChairPersonVote() {
    return chairPersonVote;
  }

  private void setChairPersonVote(Vote chairPersonVote) {
    this.chairPersonVote = chairPersonVote;
  }

  private User getChairPerson() {
    return chairPerson;
  }

  private void setChairPerson(User chairPerson) {
    this.chairPerson = chairPerson;
  }

  private Integer getMaxNumberOfElectionUsers() {
    return maxNumberOfElectionUsers;
  }

  private void setMaxNumberOfElectionUsers(Integer maxNumberOfElectionUsers) {
    this.maxNumberOfElectionUsers = maxNumberOfElectionUsers;
  }
}
