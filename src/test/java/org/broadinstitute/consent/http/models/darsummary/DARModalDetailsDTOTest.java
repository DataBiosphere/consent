package org.broadinstitute.consent.http.models.darsummary;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.OntologyEntry;
import org.broadinstitute.consent.http.models.User;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DARModalDetailsDTOTest {

    @Mock
    UserDAO userDAO;

    @Mock
    InstitutionDAO institutionDAO;

    private final String DAR_CODE = "DAR-1";
    private final String TITLE = "Mocked Title";
    private final String OTHERTEXT = "Other text";
    private final DataAccessRequest dar = new DataAccessRequest();
    private final DataAccessRequestData data = new DataAccessRequestData();
    private final User user = new User();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Integer userId = userDAO.insertUser("email", "name", new Date());
        User user = userDAO.findUserById(userId);
        dar.setUserId(userId);
        data.setInstitution("");
        data.setDarCode(DAR_CODE);
        data.setProjectTitle(TITLE);
        data.setDiseases(true);
        data.setMethods(true);
        data.setControls(true);
        data.setPopulation(true);
        data.setOther(true);
        data.setOtherText(OTHERTEXT);
        data.setOntologies(ontologies());
        data.setForProfit(false);
        data.setOneGender(true);
        data.setGender("F");
        data.setPediatric(true);
        data.setIllegalBehavior(true);
        data.setAddiction(true);
        data.setSexualDiseases(true);
        data.setStigmatizedDiseases(true);
        data.setVulnerablePopulation(true);
        data.setPopulationMigration(true);
        data.setPsychiatricTraits(true);
        data.setNotHealth(true);
        dar.setData(data);
    }

    private List<OntologyEntry> ontologies(){
        OntologyEntry ont1 = new OntologyEntry();
        ont1.setLabel("OD-1: Ontology One");
        OntologyEntry ont2 = new OntologyEntry();
        ont2.setLabel("OD-2: Ontology Two");
        OntologyEntry ont3 = new OntologyEntry();
        ont3.setLabel("OD-3: Ontology Three");
        return Arrays.asList(ont1, ont2, ont3);
    }
}
