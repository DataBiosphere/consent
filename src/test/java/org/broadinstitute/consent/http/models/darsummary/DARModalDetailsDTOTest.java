package org.broadinstitute.consent.http.models.darsummary;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Date;

import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DatasetDetailEntry;
import org.broadinstitute.consent.http.models.OntologyEntry;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.util.DarUtil;
import org.junit.Before;
import org.junit.Test;
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
        data.setProfileName("name");
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

    @Test
    public void generateModalDetailsDTO(){
        DARModalDetailsDTO modalDetailsDTO = new DARModalDetailsDTO()
            .setDarCode(dar.getData().getDarCode())
            .setPrincipalInvestigator(DarUtil.findPI(user))
            .setInstitutionName((user.getInstitutionId() == null) ?
            "" 
            : institutionDAO.findInstitutionById(user.getInstitutionId()).getName())
            .setProjectTitle(dar.getData().getProjectTitle())
            .setDepartment(dar.getData().getDepartment())
            .setCity(dar.getData().getCity())
            .setCountry(dar.getData().getCountry())
            .setNihUsername(dar.getData().getNihUsername())
            .setIsThereDiseases(false)
            .setIsTherePurposeStatements(false)
            .setResearchType(dar)
            .setDiseases(dar)
            .setPurposeStatements(dar)
            .setDatasets(Collections.emptyList());
        modalDetailsDTO.getDarCode();
        assertTrue(modalDetailsDTO.getDarCode().equals(DAR_CODE));
        assertTrue(modalDetailsDTO.getInstitutionName().equals(""));
        assertTrue(modalDetailsDTO.getPrincipalInvestigator().equals("- -"));
        assertTrue(modalDetailsDTO.getProjectTitle().equals(TITLE));
        assertTrue(modalDetailsDTO.isTherePurposeStatements());
        assertTrue(modalDetailsDTO.isRequiresManualReview());
        assertTrue(modalDetailsDTO.isSensitivePopulation());
        assertTrue(modalDetailsDTO.isThereDiseases());

        assertTrue(modalDetailsDTO.getDiseases().size() == 3);
        assertTrue(modalDetailsDTO.getDiseases().get(0).equals("OD-1: Ontology One"));
        assertTrue(modalDetailsDTO.getDiseases().get(1).equals("OD-2: Ontology Two"));
        assertTrue(modalDetailsDTO.getDiseases().get(2).equals("OD-3: Ontology Three"));

        assertTrue(modalDetailsDTO.getPurposeStatements().size() == 10);

        assertTrue(modalDetailsDTO.getResearchType().size() == 5);
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