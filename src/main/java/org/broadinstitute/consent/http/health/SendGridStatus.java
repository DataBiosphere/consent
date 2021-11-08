package org.broadinstitute.consent.http.health;

import com.codahale.metrics.health.HealthCheck.Result;

public class SendGridStatus {
    private Object page;
    private StatusObject status;

    public Object getPage() {
        return page;
    }

    public void setPage(Object page) {
        this.page = page;
    }

    public StatusObject getStatus() {
        return status;
    }

    public void setStatus(StatusObject status) {
        this.status = status;
    }

    public Result getResult() {
        Result result;

        if (status.getIndicator() == Indicator.none) {
            result = Result.builder()
                    .withDetail("page", page)
                    .withDetail("status", status)
                    .healthy()
                    .build();
        } else {
            result = Result.unhealthy("SendGrid status is unhealthy: " + status.getDescription());
        }

        return result;
    }

    enum Indicator {
        none, minor, major, critical
    }

    static class StatusObject {
        private Indicator indicator;
        private String description;

        public StatusObject(Indicator indicator, String description) {
            this.indicator = indicator;
            this.description = description;
        }

        public Indicator getIndicator() {
            return indicator;
        }

        public void setIndicator(Indicator indicator) {
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