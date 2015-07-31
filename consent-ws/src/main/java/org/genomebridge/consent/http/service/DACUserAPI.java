package org.genomebridge.consent.http.service;

import com.sun.jersey.api.NotFoundException;
import org.genomebridge.consent.http.models.DACUser;

public interface DACUserAPI {

    DACUser createDACUser(DACUser dacUser) throws IllegalArgumentException;

    DACUser describeDACUserByEmail(String email) throws NotFoundException;

    DACUser updateDACUserByEmail(DACUser rec) throws IllegalArgumentException, NotFoundException;

    void deleteDACUser(String email) throws IllegalArgumentException, NotFoundException;

}
