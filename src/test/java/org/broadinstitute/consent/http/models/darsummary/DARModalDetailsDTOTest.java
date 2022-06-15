package org.broadinstitute.consent.http.models.darsummary;

import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DatasetDetailEntry;
import org.broadinstitute.consent.http.models.OntologyEntry;
import org.broadinstitute.consent.http.models.User;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

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
        data.setDatasetDetail(getDatasetDetail());
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

    private ArrayList<DatasetDetailEntry> getDatasetDetail(){
        DatasetDetailEntry entry1 = new DatasetDetailEntry();
        entry1.setName("First:");
        entry1.setDatasetId("First Sample Detail");
        DatasetDetailEntry entry2 = new DatasetDetailEntry();
        entry2.setName("Second:");
        entry2.setDatasetId("Second Sample Detail");
        DatasetDetailEntry entry3 = new DatasetDetailEntry();
        entry3.setName("First:");
        entry3.setDatasetId("First Sample Detail");
        ArrayList<DatasetDetailEntry> list = new ArrayList<>();
        list.add(entry1);
        list.add(entry2);
        list.add(entry3);
        return list;
    }
}