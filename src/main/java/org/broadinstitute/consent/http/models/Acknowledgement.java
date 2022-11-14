package org.broadinstitute.consent.http.models;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Objects;

public class Acknowledgement {

    private String ack_key;
    private Integer userId;
    private Timestamp first_acknowledged;
    private Timestamp last_acknowledged;

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Date getFirst_acknowledged() { return first_acknowledged; }

    public void setFirst_acknowledged(Timestamp first_acknowledged) {
        this.first_acknowledged = first_acknowledged;
    }

    public Date getLast_acknowledged() {
        return last_acknowledged;
    }

    public void setLast_acknowledged(Timestamp last_acknowledged) {
        this.last_acknowledged = last_acknowledged;
    }

    public String getAck_key() {
        return ack_key;
    }

    public void setAck_key(String ack_key) {
        this.ack_key = ack_key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Acknowledgement ack = (Acknowledgement) o;
        return (Objects.equals(this.getAck_key(), ack.getAck_key()) &&
                Objects.equals(this.getUserId(), ack.getUserId()) &&
                this.getLast_acknowledged().getTime() == (ack.getLast_acknowledged().getTime()) &&
                this.getFirst_acknowledged().getTime() == (ack.getFirst_acknowledged()).getTime());
    }
}
