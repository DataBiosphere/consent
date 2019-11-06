package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DataSetDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.UserRole;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.mockito.Mockito.when;

public class DacServiceTest {

    private DacService service;

    @Mock
    DacDAO dacDAO;

    @Mock
    DACUserDAO dacUserDAO;

    @Mock
    DataSetDAO dataSetDAO;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    private void initService() {
        service = new DacService(dacDAO, dacUserDAO, dataSetDAO);
    }

    @Test
    public void testFindAll() {
        when(dacDAO.findAll()).thenReturn(Collections.emptyList());
        initService();

        Assert.assertTrue(service.findAll().isEmpty());
    }

    @Test
    public void testFindAllDACUsersBySearchString() {
        when(dacDAO.findAll()).thenReturn(Collections.emptyList());
        when(dacDAO.findAllDACUserMemberships()).thenReturn(Collections.emptyList());
        initService();

        Assert.assertTrue(service.findAllDacsWithMembers().isEmpty());
    }

    @Test
    public void testFindAllDACUsersBySearchString_case_1() {
        when(dacDAO.findAll()).thenReturn(getDacs());
        when(dacDAO.findAllDACUserMemberships()).thenReturn(getDacUsers());
        initService();

        List<Dac> dacs = service.findAllDacsWithMembers();
        Assert.assertFalse(dacs.isEmpty());
        Assert.assertEquals(dacs.size(), getDacs().size());
        List<Dac> dacsWithMembers = dacs.
                stream().
                filter(d -> !d.getChairpersons().isEmpty()).
                filter(d -> !d.getMembers().isEmpty()).
                collect(Collectors.toList());
        Assert.assertFalse(dacsWithMembers.isEmpty());
        Assert.assertEquals(1, dacsWithMembers.size());
    }

    @Test
    public void testFindById() {
        int dacId = 1;
        when(dacDAO.findById(dacId)).thenReturn(getDacs().get(0));
        when(dacDAO.findMembersByDacIdAndRoleId(dacId, UserRoles.CHAIRPERSON.getRoleId())).thenReturn(Collections.singletonList(getDacUsers().get(0)));
        when(dacDAO.findMembersByDacIdAndRoleId(dacId, UserRoles.MEMBER.getRoleId())).thenReturn(Collections.singletonList(getDacUsers().get(1)));
        initService();

        Dac dac = service.findById(dacId);
        Assert.assertNotNull(dac);
        Assert.assertFalse(dac.getChairpersons().isEmpty());
        Assert.assertFalse(dac.getMembers().isEmpty());
    }

    /**
     * @return A list of 5 dacs
     */
    private List<Dac> getDacs() {
        return IntStream.range(1, 5).
                mapToObj(i -> {
                    Dac dac = new Dac();
                    dac.setDacId(i);
                    dac.setDescription("Dac " + i);
                    dac.setName("Dac " + i);
                    return dac;
                }).collect(Collectors.toList());
    }

    /**
     *
     * @return A list of two users in a single DAC
     */
    private List<DACUser> getDacUsers() {
        DACUser chair = new DACUser();
        chair.setDacUserId(1);
        chair.setDisplayName("Chair");
        chair.setEmail("chair@duos.org");
        chair.setRoles(new ArrayList<>());
        chair.getRoles().add(new UserRole(1, chair.getDacUserId(), UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName(), 1));

        DACUser member = new DACUser();
        member.setDacUserId(2);
        member.setDisplayName("Member");
        member.setEmail("member@duos.org");
        member.setRoles(new ArrayList<>());
        member.getRoles().add(new UserRole(1, member.getDacUserId(), UserRoles.MEMBER.getRoleId(), UserRoles.MEMBER.getRoleName(), 1));

        List<DACUser> users = new ArrayList<>();
        users.add(chair);
        users.add(member);
        return users;
    }

}
