package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.models.DataSet;

import java.util.ArrayList;
import java.util.List;

public class ParseResult {

    List<DataSet> dataSets;
    List<String> errors;

    public ParseResult() {
        this.dataSets = new ArrayList<>();
        this.errors = new ArrayList<>();
    }

    public List<DataSet> getDatasets() {
        return dataSets;
    }

    public void setDatasets(List<DataSet> dataSets) {
        this.dataSets = dataSets;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}
