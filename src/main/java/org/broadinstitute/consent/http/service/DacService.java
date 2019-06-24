package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.models.Dac;

import java.util.Date;
import java.util.List;

public class DacService {

    private DacDAO dacDAO;

    @Inject
    public DacService(DacDAO dacDAO) {
        this.dacDAO = dacDAO;
    }

    public List<Dac> findAll() {
        return dacDAO.findAll();
    }

    public Dac findById(Integer dacId) {
        return dacDAO.findById(dacId);
    }

    public Integer createDac(String name, String description) {
        Date createDate = new Date();
        return dacDAO.createDac(name, description, createDate);
    }

    public void updateDac(String name, String description, Integer dacId) {
        Date updateDate = new Date();
        dacDAO.updateDac(name, description, updateDate, dacId);
    }

    public void deleteDac(Integer dacId) {
        dacDAO.deleteDac(dacId);
    }

}
