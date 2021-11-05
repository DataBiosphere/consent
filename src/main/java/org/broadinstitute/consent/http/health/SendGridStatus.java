package org.broadinstitute.consent.http.health;

import com.codahale.metrics.health.HealthCheck.Result;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

public class SendGridStatus {
    private Object page;
    private Object status;

    public Object getPage() {
        return page;
    }

    public void setPage(Object page) {
        this.page = page;
    }

    public Object getStatus() {
        return status;
    }

    public void setStatus(String indicator, String description) {
        Map<String, String> statusMap = new HashMap<>();
        statusMap.put("indicator", indicator);
        statusMap.put("description", description);
        status = new Gson().toJson(statusMap);
    }

    private Map<String, String> getStatusMap() {
        return new Gson().fromJson(getStatus().toString(), Map.class);
    }

    public boolean isOk() {
        return getStatusMap().get("indicator").equalsIgnoreCase("none");
    }

    public Result getResult() {
        Result result;

        if (isOk()) {
            result = Result.builder()
                    .withDetail("page", getPage())
                    .withDetail("status", getStatus())
                    .healthy()
                    .build();
        } else {
            result = Result.unhealthy("SendGrid status is unhealthy: " + getStatusMap().get("description"));
        }

        return result;
    }
}