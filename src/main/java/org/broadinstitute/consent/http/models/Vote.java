package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.Gson;

import java.util.Date;
import java.util.List;

@JsonInclude(Include.NON_NULL)
public class Vote {

  public static final String QUERY_FIELDS_WITH_V_PREFIX =
      "v.voteid as v_vote_id, "
          + " v.vote as v_vote, "
          + " v.dacuserid as v_dac_user_id, "
          + " v.rationale as v_rationale, "
          + " v.electionid as v_election_id, "
          + "v.createdate as v_create_date, "
          + " v.updatedate as v_update_date, "
          + " v.type as v_type ";

    @JsonProperty
    private Integer voteId;

    @JsonProperty
    private Boolean vote;

    @JsonProperty
    private Integer dacUserId;

    @JsonProperty
    private Date createDate;

    @JsonProperty
    private Date updateDate;

    @JsonProperty
    private Integer electionId;

    @JsonProperty
    private String rationale;

    @JsonProperty
    private String type;

    @JsonProperty
    private Boolean isReminderSent;

    @JsonProperty
    private Boolean hasConcerns;

    @JsonProperty
    private String displayName;

    public Vote() {
    }

    public Vote(Integer voteId, Boolean vote, Integer dacUserId, Date createDate, Date updateDate,
                Integer electionId, String rationale, String type, Boolean isReminderSent, Boolean hasConcerns) {
        this.voteId = voteId;
        this.vote = vote;
        this.dacUserId = dacUserId;
        this.createDate = createDate;
        this.updateDate = updateDate;
        this.electionId = electionId;
        this.rationale = rationale;
        this.type = type;
        this.isReminderSent = isReminderSent;
        this.hasConcerns = hasConcerns;
    }

    public Integer getVoteId() {
        return voteId;
    }

    public void setVoteId(Integer voteId) {
        this.voteId = voteId;
    }

    public Boolean getVote() {
        return vote;
    }

    public void setVote(Boolean vote) {
        this.vote = vote;
    }

    public Integer getDacUserId() {
        return dacUserId;
    }

    public void setDacUserId(Integer dacUserId) {
        this.dacUserId = dacUserId;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }

    public Integer getElectionId() {
        return electionId;
    }

    public void setElectionId(Integer electionId) {
        this.electionId = electionId;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getIsReminderSent() {
        return isReminderSent;
    }

    public void setIsReminderSent(Boolean isReminderSent) {
        this.isReminderSent = isReminderSent;
    }

    public Boolean getHasConcerns() {
        return hasConcerns;
    }

    public void setHasConcerns(Boolean hasConcerns) {
        this.hasConcerns = hasConcerns;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }



    public static class VoteUpdate {
        private Boolean vote;
        private String rationale;
        private List<Integer> voteIds;

        public VoteUpdate() {

        }

        public VoteUpdate(Boolean vote, String rationale, List<Integer> voteIds) {
            this.vote = vote;
            this.rationale = rationale;
            this.voteIds = voteIds;
        }

        public Boolean getVote() {
            return vote;
        }

        public void setVote(Boolean vote) {
            this.vote = vote;
        }

        public String getRationale() {
            return rationale;
        }

        public void setRationale(String rationale) {
            this.rationale = rationale;
        }

        public List<Integer> getVoteIds() {
            return voteIds;
        }

        public void setVoteIds(List<Integer> voteIds) {
            this.voteIds = voteIds;
        }
    }

    public static class RationaleUpdate {
        private List<Integer> voteIds;
        private String rationale;

        public RationaleUpdate() {
        }

        public List<Integer> getVoteIds() {
            return voteIds;
        }

        public void setVoteIds(List<Integer> voteIds) {
            this.voteIds = voteIds;
        }

        public String getRationale() {
            return rationale;
        }

        public void setRationale(String rationale) {
            this.rationale = rationale;
        }
    }
}