package org.broadinstitute.consent.http.service;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.DataSetDAO;
import org.broadinstitute.consent.http.enumeration.AssociationType;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.DataSetProperty;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.Dictionary;
import org.broadinstitute.consent.http.models.dto.DataSetDTO;
import org.broadinstitute.consent.http.models.dto.DataSetPropertyDTO;

public class DatasetService {

    public static final String DATASET_NAME_KEY = "Dataset Name";
    public static final String CONSENT_NAME_PREFIX = "DUOS-DS-CG-";
    private final ConsentDAO consentDAO;
    private final DataSetDAO dataSetDAO;

    @Inject
    public DatasetService(ConsentDAO consentDAO, DataSetDAO dataSetDAO) {
        this.consentDAO = consentDAO;
        this.dataSetDAO = dataSetDAO;
    }

    /**
     * Create a minimal consent from the data provided in a Dataset.
     *
     * @param dataset The DataSetDTO
     * @return The created Consent
     */
    public Consent createConsentForDataset(DataSetDTO dataset) {
        String consentId = UUID.randomUUID().toString();
        Optional<DataSetPropertyDTO> nameProp = dataset.getProperties()
            .stream()
            .filter(p -> p.getPropertyName().equalsIgnoreCase(DATASET_NAME_KEY))
            .findFirst();
        // Typically, this is a construct from ORSP consisting of dataset name and some form of investigator code.
        // In our world, we'll use that dataset name if provided, or the alias.
        String groupName = nameProp.isPresent() ? nameProp.get().getPropertyValue() : dataset.getAlias();
        String name = CONSENT_NAME_PREFIX + dataset.getDataSetId();
        Date createDate = new Date();
        boolean manualReview = isConsentDataUseManualReview(dataset.getDataUse());
        /*
         * Consents created for a dataset do not need the following properties:
         * use restriction
         * data user letter
         * data user letter name
         * translated use restriction
         */
        consentDAO.insertConsent(consentId, manualReview, null, dataset.getDataUse().toString(),
            null, name, null, createDate, createDate, null,
            true, groupName, dataset.getDacId());
        String associationType = AssociationType.SAMPLESET.getValue();
        consentDAO.insertConsentAssociation(consentId, associationType, dataset.getDacId());
        return consentDAO.findConsentById(consentId);
    }

    private boolean isConsentDataUseManualReview(DataUse dataUse) {
        return dataUse.getAddiction() ||
            dataUse.getEthicsApprovalRequired() ||
            dataUse.getIllegalBehavior() ||
            dataUse.getManualReview() ||
            Objects.nonNull(dataUse.getOther()) ||
            dataUse.getOtherRestrictions() ||
            dataUse.getPopulationOriginsAncestry() ||
            (Objects.nonNull(dataUse.getPopulationRestrictions()) && !dataUse.getPopulationRestrictions().isEmpty()) ||
            dataUse.getPsychologicalTraits() ||
            dataUse.getSexualDiseases() ||
            dataUse.getStigmatizeDiseases() ||
            dataUse.getVulnerablePopulations();
    }

    public DataSet createDataset(DataSetDTO dataset, String name, Integer userId) {
        Timestamp now = new Timestamp(new Date().getTime());
        int lastAlias = dataSetDAO.findLastAlias();
        int alias = lastAlias + 1;

        Integer id = dataSetDAO
            .insertDatasetV2(name, now, userId, dataset.getObjectId(), dataset.getActive(), alias);

        List<DataSetProperty> propertyList = processDatasetProperties(id, dataset.getProperties());
        dataSetDAO.insertDataSetsProperties(propertyList);

        return getDatasetWithPropertiesById(id);
    }

    public DataSet getDatasetByName(String name) {
        return dataSetDAO.getDatasetByName(name);
    }

    public DataSet findDatasetById(Integer id) {
        return dataSetDAO.findDataSetById(id);
    }

    public Set<DataSetProperty> getDatasetProperties(Integer datasetId) {
        return dataSetDAO.findDatasetPropertiesByDatasetId(datasetId);
    }

    public DataSet getDatasetWithPropertiesById(Integer datasetId) {
        DataSet dataset = dataSetDAO.findDataSetById(datasetId);
        Set<DataSetProperty> properties = getDatasetProperties(datasetId);
        dataset.setProperties(properties);
        return dataset;
    }

    public DataSetDTO getDatasetDTO(Integer datasetId) {
        Set<DataSetDTO> dataset = dataSetDAO.findDatasetDTOWithPropertiesByDatasetId(datasetId);
        DataSetDTO result = new DataSetDTO();
        for (DataSetDTO d : dataset) {
            result = d;
        }
        return result;
    }

    public List<DataSetProperty> processDatasetProperties(Integer datasetId, List<DataSetPropertyDTO> properties) {
        Date now = new Date();
        List<Dictionary> dictionaries = dataSetDAO.getMappedFieldsOrderByReceiveOrder();
        List<String> keys = dictionaries.stream().map(Dictionary::getKey).collect(Collectors.toList());

        return properties.stream()
            .filter(p -> keys.contains(p.getPropertyName()) && !p.getPropertyName().equals("Dataset Name"))
            .map(p ->
                new DataSetProperty(datasetId, dictionaries.get(keys.indexOf(p.getPropertyName())).getKeyId(), p.getPropertyValue(), now)
            )
            .collect(Collectors.toList());
    }

    public List<DataSetPropertyDTO> findInvalidProperties(List<DataSetPropertyDTO> properties) {
        List<Dictionary> dictionaries = dataSetDAO.getMappedFieldsOrderByReceiveOrder();
        List<String> keys = dictionaries.stream().map(Dictionary::getKey).collect(Collectors.toList());

        return properties.stream()
            .filter(p -> !keys.contains(p.getPropertyName()))
            .collect(Collectors.toList());
    }

}
