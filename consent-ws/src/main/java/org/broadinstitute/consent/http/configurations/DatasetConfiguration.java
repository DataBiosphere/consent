package org.broadinstitute.consent.http.configurations;

import javax.validation.constraints.NotNull;

public class DatasetConfiguration {

    @NotNull
    public String duos1;

    @NotNull
    public String duos2;

    public String getDuos1() {
        return duos1;
    }

    public void setDuos1(String duos1) {
        this.duos1 = duos1;
    }

    public String getDuos2() {
        return duos2;
    }

    public void setDuos2(String duos2) {
        this.duos2 = duos2;
    }
}
