package org.genomebridge.consent.http.service;

import org.genomebridge.consent.http.models.DataSet;

import java.util.ArrayList;
import java.util.List;

public class ParseResult {

    List<DataSet> dataSets;
    List<String> errors;

    public ParseResult(List<DataSet> dataSets, List<String> errors) {
        this.dataSets = new ArrayList<>();
        this.errors = new ArrayList<>();
    }

    public List<DataSet> getDataSets() {
        return dataSets;
    }

    public void setDataSets(List<DataSet> dataSets) {
        this.dataSets = dataSets;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}
