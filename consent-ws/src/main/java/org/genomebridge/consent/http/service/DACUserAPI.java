package org.genomebridge.consent.http.service;

import org.genomebridge.consent.http.models.DACUser;

import javax.ws.rs.NotFoundException;

public interface DACUserAPI {

    DACUser createDACUser(DACUser dacUser) throws IllegalArgumentException;

    DACUser describeDACUserByEmail(String email) throws NotFoundException;

    DACUser updateDACUserByEmail(DACUser rec) throws IllegalArgumentException, NotFoundException;

    void deleteDACUser(String email) throws IllegalArgumentException, NotFoundException;

}
