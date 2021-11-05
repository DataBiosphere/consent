package org.broadinstitute.consent.http.health;

import com.codahale.metrics.health.HealthCheck.Result;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

public class SendGridStatus {
    private Object page;
    private StatusObject status;

    public Object getPage() {
        return page;
    }

    public void setPage(Object page) {
        this.page = page;
    }

    public String getStatus() {
        return new Gson().toJson(status);
    }

    public void setStatus(String indicator, String description) {
        status = new StatusObject();
        status.setIndicator(indicator);
        status.setDescription(description);
    }

    public Result getResult() {
        Result result;

        if (status.getIndicator().equalsIgnoreCase("none")) {
            result = Result.builder()
                    .withDetail("page", getPage())
                    .withDetail("status", getStatus())
                    .healthy()
                    .build();
        } else {
            result = Result.unhealthy("SendGrid status is unhealthy: " + status.getDescription());
        }

        return result;
    }

    private static class StatusObject {
        private String indicator;
        private String description;

        public String getIndicator() {
            return indicator;
        }

        public void setIndicator(String indicator) {
            this.indicator = indicator;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}