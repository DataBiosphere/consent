package org.broadinstitute.consent.http.service;

import com.google.cloud.storage.BlobId;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.enumeration.DatasetPropertyType;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dataset_registration_v1.AlternativeDataSharingPlanReason;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DataLocation;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1;
import org.broadinstitute.consent.http.models.dataset_registration_v1.NihAnvilUse;
import org.broadinstitute.consent.http.models.dataset_registration_v1.NihICsSupportingStudy;
import org.broadinstitute.consent.http.service.dao.DatasetServiceDAO;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;

/**
 * Specialized service class which specifically handles
 * the process of registering a new dataset into the system,
 * i.e. creates new datasets.
 */
public class DatasetRegistrationService {
    private final DatasetDAO datasetDAO;
    private final DatasetServiceDAO datasetServiceDAO;
    private final GCSService gcsService;

    public DatasetRegistrationService(DatasetDAO datasetDAO, DatasetServiceDAO datasetServiceDAO, GCSService gcsService) {
        this.datasetDAO = datasetDAO;
        this.datasetServiceDAO = datasetServiceDAO;
        this.gcsService = gcsService;
    }


    /**
     * This method takes an instance of a dataset registration schema and creates datasets from it.
     * There will be one dataset per ConsentGroup in the dataset.
     *
     * @param registration The DatasetRegistrationSchemaV1.yaml
     * @param user The User creating these datasets
     * @param files Map of files, where the key is the name of the field
     * @return List of created Datasets from the provided registration schema
     */
    public List<Dataset> createDatasetsFromRegistration(
            DatasetRegistrationSchemaV1 registration,
            User user,
            Map<String, FormDataBodyPart> files) throws IOException, SQLException {

        Map<String, BlobId> uploadedFileCache = new HashMap<>();

        List<DatasetServiceDAO.DatasetInsert> datasetInserts = new ArrayList<>();
        for (int consentGroupIdx = 0; consentGroupIdx < registration.getConsentGroups().size(); consentGroupIdx++) {
            datasetInserts.add(createDatasetInsert(registration, user, files, uploadedFileCache, consentGroupIdx));
        }

        List<Integer> createdDatasetIds = datasetServiceDAO.insertDatasets(datasetInserts);
        return datasetDAO.findDatasetsByIdList(createdDatasetIds);
    }

    private BlobId uploadFile(FormDataBodyPart file) throws IOException {
        String mediaType = file.getContentDisposition().getType();

        return gcsService.storeDocument(
                file.getValueAs(InputStream.class),
                mediaType,
                UUID.randomUUID());
    }

    private DatasetServiceDAO.DatasetInsert createDatasetInsert(DatasetRegistrationSchemaV1 registration,
                                                                User user,
                                                                Map<String, FormDataBodyPart> files,
                                                                Map<String, BlobId> uploadedFileCache,
                                                                Integer consentGroupIdx) throws IOException {
        ConsentGroup consentGroup = registration.getConsentGroups().get(consentGroupIdx);

        List<DatasetProperty> props = convertRegistrationToDatasetProperties(registration, consentGroupIdx);
        DataUse dataUse = generateDataUseFromConsentGroup(consentGroup);
        List<FileStorageObject> fileStorageObjects = uploadFiles(files, uploadedFileCache, consentGroupIdx, user);

        return new DatasetServiceDAO.DatasetInsert(
                consentGroup.getConsentGroupName(),
                registration.getDataAccessCommitteeId(),
                dataUse,
                user.getUserId(),
                props,
                fileStorageObjects
        );
    }

    private DataUse generateDataUseFromConsentGroup(ConsentGroup group) {
        DataUse dataUse = new DataUse();

        dataUse.setCollaboratorRequired(group.getCol());
        dataUse.setDiseaseRestrictions(group.getDiseaseSpecificUse());
        dataUse.setEthicsApprovalRequired(group.getIrb());
        dataUse.setGeneralUse(group.getGeneralResearchUse());
        dataUse.setGeographicalRestrictions(group.getGs());
        dataUse.setGeneticStudiesOnly(group.getGso());
        dataUse.setHmbResearch(group.getHmb());
        dataUse.setPublicationMoratorium(group.getMor() ? group.getMorDate() : null);

        dataUse.setMethodsResearch(!group.getNmds()); // TODO: is this right?
        dataUse.setCommercialUse(!group.getNpu());
        dataUse.setOther(group.getOtherPrimary());
        dataUse.setSecondaryOther(group.getOtherSecondary());
        dataUse.setPopulationOriginsAncestry(group.getPoa());
        dataUse.setPublicationResults(group.getPub());

        return dataUse;
    }

    private List<FileStorageObject> uploadFiles(Map<String, FormDataBodyPart> files,
                                                Map<String, BlobId> uploadedFileCache,
                                                Integer consentGroupIdx,
                                                User user) throws IOException {
        List<FileStorageObject> consentGroupFSOs = new ArrayList<>();

        if (files.containsKey("alternativeDataSharingPlan")) {
            consentGroupFSOs.add(uploadFile(
                                 files, uploadedFileCache, user,
                                 "alternativeDataSharingPlan",
                                 FileCategory.ALTERNATIVE_DATA_SHARING_PLAN));
        }

        if (files.containsKey("consentGroups["+consentGroupIdx+"].nihInstitutionalCertificationFile")) {
            consentGroupFSOs.add(uploadFile(
                    files, uploadedFileCache, user,
                    "consentGroups["+ consentGroupIdx +"].nihInstitutionalCertificationFile",
                    FileCategory.NIH_INSTITUTIONAL_CERTIFICATION));
        }

        return consentGroupFSOs;

    }

    private FileStorageObject uploadFile(Map<String, FormDataBodyPart> files,
                                         Map<String, BlobId> uploadedFileCache,
                                         User user,
                                         String name,
                                         FileCategory category) throws IOException {

        FormDataBodyPart bodyPart = files.get(name);

        if (!uploadedFileCache.containsKey(name)) {
            BlobId id = uploadFile(bodyPart);
            uploadedFileCache.put(name, id);
        }

        BlobId id = uploadedFileCache.get(name);

        FileStorageObject fso = new FileStorageObject();
        fso.setCategory(category);
        fso.setFileName(bodyPart.getContentDisposition().getFileName());
        fso.setMediaType(bodyPart.getMediaType().toString());
        fso.setBlobId(id);
        fso.setCreateUserId(user.getUserId());

        return fso;
    }


    /**
     * Extracts an individual field as a dataset property.
     *
     * @param name The human-readable name of the field
     * @param schemaProp The schema property name (camelCase)
     * @param type The type of the field, e.g. Boolean, String
     * @param getField Lambda which gets the field's value
     */
    private record DatasetPropertyExtractor(
            String name,
            String schemaProp,
            DatasetPropertyType type,

            /*
             * Takes in: Dataset registration object and consent group index (as integer)
             * Produces: The value of the field, can be null if field not present.
             */
            BiFunction<DatasetRegistrationSchemaV1,Integer,Object> getField
    ) {

        /**
         * Converts a field on the given registration to a DatasetProperty.
         *
         * @param registration The registration object to extract from
         * @param consentGroupIdx The index of the consent group to extract from
         * @return The dataset property, if the field has a value, otherwise Optional.empty()
         */
        Optional<DatasetProperty> extract(DatasetRegistrationSchemaV1 registration, int consentGroupIdx) {
            Object value = this.getField.apply(registration, consentGroupIdx);
            if (Objects.isNull(value)) {
                return Optional.empty();
            }

            DatasetProperty datasetProperty = new DatasetProperty();
            datasetProperty.setPropertyName(this.name);
            datasetProperty.setPropertyType(this.type);
            datasetProperty.setSchemaProperty(this.schemaProp);
            datasetProperty.setPropertyValue(this.type.coerce(value.toString()));

            return Optional.of(datasetProperty);

        }
    };

    private static final List<DatasetPropertyExtractor> datasetPropertyExtractors = List.of(
            new DatasetPropertyExtractor(
                    "PI Name", "piName", DatasetPropertyType.String,
                    (registration, idx) -> registration.getPiName()),
            new DatasetPropertyExtractor(
                    "Study Name", "studyName", DatasetPropertyType.String,
                    (registration, idx) -> registration.getStudyName()),
            new DatasetPropertyExtractor(
                    "Study Type", "studyType", DatasetPropertyType.String,
                    (registration, idx) -> registration.getStudyType().value()),
            new DatasetPropertyExtractor(
                    "Data Types", "dataTypes", DatasetPropertyType.Json,
                    (registration, idx) -> GsonUtil.getInstance().toJson(registration.getDataTypes())),
            new DatasetPropertyExtractor(
                    "Study Description", "studyDescription", DatasetPropertyType.String,
                    (registration, idx) -> registration.getStudyDescription()),
            new DatasetPropertyExtractor(
                    "Phenotype Indication", "phenotypeIndication", DatasetPropertyType.String,
                    (registration, idx) -> registration.getPhenotypeIndication()),
            new DatasetPropertyExtractor(
                    "Species", "species", DatasetPropertyType.String,
                    (registration, idx) -> registration.getSpecies()),
            new DatasetPropertyExtractor(
                    "Data Submitter User ID", "dataSubmitterUserId", DatasetPropertyType.Number,
                    (registration, idx) -> registration.getDataSubmitterUserId()),
            new DatasetPropertyExtractor(
                    "Data Custodian Email", "dataCustodianEmail", DatasetPropertyType.String,
                    (registration, idx) -> registration.getDataCustodianEmail()),
            new DatasetPropertyExtractor(
                    "Public Visibility", "publicVisibility", DatasetPropertyType.Boolean,
                    (registration, idx) -> registration.getPublicVisibility()),
            new DatasetPropertyExtractor(
                    "NIH Anvil Use", "nihAnvilUse", DatasetPropertyType.Json,
                    (registration, idx) -> GsonUtil.getInstance().toJson(registration.getNihAnvilUse().stream().map(NihAnvilUse::value).toList())),
            new DatasetPropertyExtractor(
                    "Submitting To Anvil", "submittingToAnvil", DatasetPropertyType.Boolean,
                    (registration, idx) -> registration.getSubmittingToAnvil()),
            new DatasetPropertyExtractor(
                    "dbGaP phs ID", "dbGaPPhsID", DatasetPropertyType.String,
                    (registration, idx) -> registration.getDbGaPPhsID()),
            new DatasetPropertyExtractor(
                    "dbGaP Study Registration Name", "dbGaPStudyRegistrationName", DatasetPropertyType.String,
                    (registration, idx) -> registration.getDbGaPStudyRegistrationName()),
            new DatasetPropertyExtractor(
                    "Embargo Release Date", "embargoReleaseDate", DatasetPropertyType.Date,
                    (registration, idx) -> registration.getEmbargoReleaseDate()),
            new DatasetPropertyExtractor(
                    "Sequencing Center", "sequencingCenter", DatasetPropertyType.String,
                    (registration, idx) -> registration.getSequencingCenter()),
            new DatasetPropertyExtractor(
                    "PI Email", "piEmail", DatasetPropertyType.String,
                    (registration, idx) -> registration.getPiEmail()),
            new DatasetPropertyExtractor(
                    "PI Institution", "piInstitution", DatasetPropertyType.Number,
                    (registration, idx) -> registration.getPiInstitution()),
            new DatasetPropertyExtractor(
                    "NIH Grant Contract Number", "nihGrantContractNumber", DatasetPropertyType.String,
                    (registration, idx) -> registration.getNihGrantContractNumber()),
            new DatasetPropertyExtractor(
                    "NIH ICs Supporting Study", "nihICsSupportingStudy", DatasetPropertyType.String,
                    (registration, idx) -> GsonUtil.getInstance().toJson(registration.getNihICsSupportingStudy().stream().map(NihICsSupportingStudy::value).toList())),
            new DatasetPropertyExtractor(
                    "NIH Program Officer Name", "nihProgramOfficerName", DatasetPropertyType.String,
                    (registration, idx) -> registration.getNihProgramOfficerName()),
            new DatasetPropertyExtractor(
                    "NIH Institution Center Submission", "nihInstitutionCenterSubmission", DatasetPropertyType.String,
                    (registration, idx) -> registration.getNihInstitutionCenterSubmission().value()),
            new DatasetPropertyExtractor(
                    "NIH Genomic Program Administrator Name", "nihGenomicProgramAdministratorName", DatasetPropertyType.String,
                    (registration, idx) -> registration.getNihGenomicProgramAdministratorName()),
            new DatasetPropertyExtractor(
                    "Multi Center Study", "multiCenterStudy", DatasetPropertyType.Boolean,
                    (registration, idx) -> registration.getMultiCenterStudy()),
            new DatasetPropertyExtractor(
                    "Collaborating Sites", "collaboratingSites", DatasetPropertyType.Json,
                    (registration, idx) -> GsonUtil.getInstance().toJson(registration.getCollaboratingSites())),
            new DatasetPropertyExtractor(
                    "Controlled Access Required For Genomic Summary Results GSR", "controlledAccessRequiredForGenomicSummaryResultsGSR", DatasetPropertyType.Boolean,
                    (registration, idx) -> registration.getControlledAccessRequiredForGenomicSummaryResultsGSR()),
            new DatasetPropertyExtractor(
                    "Controlled Access Not Required For Genomic Summary Results GSR Explanation", "controlledAccessRequiredForGenomicSummaryResultsGSRNotRequiredExplanation", DatasetPropertyType.String,
                    (registration, idx) -> registration.getControlledAccessRequiredForGenomicSummaryResultsGSRNotRequiredExplanation()),
            new DatasetPropertyExtractor(
                    "Alternative Data Sharing Plan", "alternativeDataSharingPlan", DatasetPropertyType.Boolean,
                    (registration, idx) -> registration.getAlternativeDataSharingPlan()),
            new DatasetPropertyExtractor(
                    "Alternative Data Sharing Plan Reasons", "alternativeDataSharingPlanReasons", DatasetPropertyType.Json,
                    (registration, idx) -> GsonUtil.getInstance().toJson(registration.getAlternativeDataSharingPlanReasons().stream().map(AlternativeDataSharingPlanReason::value).toList())),
            new DatasetPropertyExtractor(
                    "Alternative Data Sharing Plan Explanation", "alternativeDataSharingPlanExplanation", DatasetPropertyType.String,
                    (registration, idx) -> registration.getAlternativeDataSharingPlanExplanation()),
            new DatasetPropertyExtractor(
                    "Alternative Data Sharing Plan File Name", "alternativeDataSharingPlanFileName", DatasetPropertyType.String,
                    (registration, idx) -> registration.getAlternativeDataSharingPlanFileName()),
            new DatasetPropertyExtractor(
                    "Alternative Data Sharing Plan Data Submitted", "alternativeDataSharingPlanDataSubmitted", DatasetPropertyType.String,
                    (registration, idx) -> registration.getAlternativeDataSharingPlanDataSubmitted().value()),
            new DatasetPropertyExtractor(
                    "Alternative Data Sharing Plan Data Released", "alternativeDataSharingPlanDataReleased", DatasetPropertyType.Boolean,
                    (registration, idx) -> registration.getAlternativeDataSharingPlanDataReleased()),
            new DatasetPropertyExtractor(
                    "Alternative Data Sharing Plan Target Delivery Date", "alternativeDataSharingPlanTargetDeliveryDate", DatasetPropertyType.Date,
                    (registration, idx) -> registration.getAlternativeDataSharingPlanTargetDeliveryDate()),
            new DatasetPropertyExtractor(
                    "Alternative Data Sharing Plan Target Public Release Date", "alternativeDataSharingPlanTargetPublicReleaseDate", DatasetPropertyType.Date,
                    (registration, idx) -> registration.getAlternativeDataSharingPlanTargetPublicReleaseDate()),
            new DatasetPropertyExtractor(
                    "Alternative Data Sharing Plan Controlled Open Access", "alternativeDataSharingPlanControlledOpenAccess", DatasetPropertyType.String,
                    (registration, idx) -> registration.getAlternativeDataSharingPlanControlledOpenAccess().value()),
            new DatasetPropertyExtractor(
                    "Data Location", "consentGroup.dataLocation", DatasetPropertyType.Json,
                    (registration, idx) -> GsonUtil.getInstance().toJson(registration.getConsentGroups().get(idx).getDataLocation().stream().map(DataLocation::value).toList())),
            new DatasetPropertyExtractor(
                    "File Types", "consentGroup.fileTypes", DatasetPropertyType.Json,
                    (registration, idx) -> GsonUtil.getInstance().toJson(registration.getConsentGroups().get(idx).getFileTypes())),
            new DatasetPropertyExtractor(
                    "URL", "consentGroup.url", DatasetPropertyType.String,
                    (registration, idx) -> registration.getConsentGroups().get(idx).getUrl().toString())
    );


    private List<DatasetProperty> convertRegistrationToDatasetProperties(DatasetRegistrationSchemaV1 registration, int consentGroupIdx) {
        List<DatasetProperty> datasetProperties = new ArrayList<>();
        for (DatasetPropertyExtractor datasetPropertyExtractor : datasetPropertyExtractors) {

            Optional<DatasetProperty> extractedProperty = datasetPropertyExtractor.extract(registration, consentGroupIdx);

            if (extractedProperty.isPresent()) {
                datasetProperties.add(extractedProperty.get());
            }
        }

        return datasetProperties;
    }

}
