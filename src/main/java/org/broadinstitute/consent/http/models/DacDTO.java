package org.broadinstitute.consent.http.models;

import java.util.List;

public class DacDTO {
    private Dac dac;
    private List<DACUser> chairpersons;
    private List<DACUser> members;

    public DacDTO(Dac dac, List<DACUser> chairpersons, List<DACUser> members) {
        this.dac = dac;
        this.chairpersons = chairpersons;
        this.members = members;
    }

    public Dac getDac() {
        return dac;
    }

    public void setDac(Dac dac) {
        this.dac = dac;
    }

    public List<DACUser> getChairpersons() {
        return chairpersons;
    }

    public void setChairpersons(List<DACUser> chairpersons) {
        this.chairpersons = chairpersons;
    }

    public List<DACUser> getMembers() {
        return members;
    }

    public void setMembers(List<DACUser> members) {
        this.members = members;
    }

}
