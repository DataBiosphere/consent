package org.genomebridge.consent.http.service;

import com.sun.jersey.api.NotFoundException;
import org.genomebridge.consent.http.models.DACUser;
import org.genomebridge.consent.http.models.Vote;

import java.util.List;

public interface DACUserAPI {


    Integer createDACUser(DACUser dacUser) throws IllegalArgumentException;

    DACUser describeDACUserByEmail(String email) throws NotFoundException;


}
