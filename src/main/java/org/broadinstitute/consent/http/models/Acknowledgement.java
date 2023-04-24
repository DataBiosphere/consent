package org.broadinstitute.consent.http.models;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Objects;

public class Acknowledgment {

    private String ackKey;
    private Integer userId;
    private Timestamp firstAcknowledged;
    private Timestamp lastAcknowledged;

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Date getFirstAcknowledged() { return firstAcknowledged; }

    public void setFirstAcknowledged(Timestamp firstAcknowledged) {
        this.firstAcknowledged = firstAcknowledged;
    }

    public Date getLastAcknowledged() {
        return lastAcknowledged;
    }

    public void setLastAcknowledged(Timestamp lastAcknowledged) {
        this.lastAcknowledged = lastAcknowledged;
    }

    public String getAckKey() {
        return ackKey;
    }

    public void setAckKey(String ackKey) {
        this.ackKey = ackKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Acknowledgment ack = (Acknowledgment) o;
        return (Objects.equals(this.getAckKey(), ack.getAckKey()) &&
                Objects.equals(this.getUserId(), ack.getUserId()) &&
                this.getLastAcknowledged().getTime() == (ack.getLastAcknowledged().getTime()) &&
                this.getFirstAcknowledged().getTime() == (ack.getFirstAcknowledged()).getTime());
    }
}
