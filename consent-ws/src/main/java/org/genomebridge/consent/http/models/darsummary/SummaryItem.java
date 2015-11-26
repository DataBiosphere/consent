package org.genomebridge.consent.http.models.darsummary;

public class SummaryItem {

    private String title;
    private boolean manualReview = false;
    private String description;

    public SummaryItem(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public SummaryItem(String description, boolean manualReview) {
        this.description = description;
        this.manualReview = manualReview;
    }

    public boolean isManualReview() {
        return manualReview;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
