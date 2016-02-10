package org.broadinstitute.consent.http.models.dselectionmodal;

public class DARDataSetElectionModal {

    private String datasetId;
    private String datasetName;
    private String doName;
    private String doEmail;
    private boolean  doVote;
    private boolean  hasConcerns;
    private String  doComment;

    public DARDataSetElectionModal(String datasetId, String datasetName, String doName, String doEmail, boolean doVote, boolean hasConcerns,String doComment) {
        this.datasetId = datasetId;
        this.datasetName = datasetName;
        this.doName = doName;
        this.doEmail = doEmail;
        this.doVote = doVote;
        this.hasConcerns = hasConcerns;
        this.doComment = doComment;
    }

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public void setDatasetName(String datasetName) {
        this.datasetName = datasetName;
    }

    public String getDoName() {
        return doName;
    }

    public void setDoName(String doName) {
        this.doName = doName;
    }

    public String getDoEmail() {
        return doEmail;
    }

    public void setDoEmail(String doEmail) {
        this.doEmail = doEmail;
    }

    public boolean isDoVote() {
        return doVote;
    }

    public void setDoVote(boolean doVote) {
        this.doVote = doVote;
    }

    public String getDoComment() {
        return doComment;
    }

    public void setDoComment(String doComment) {
        this.doComment = doComment;
    }

    public boolean isHasConcerns() {
        return hasConcerns;
    }

    public void setHasConcerns(boolean hasConcerns) {
        this.hasConcerns = hasConcerns;
    }
}
