package org.broadinstitute.consent.http.service;

import com.google.cloud.storage.BlobId;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.db.DacDAO;
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

import javax.ws.rs.NotFoundException;
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
    private final DacDAO dacDAO;
    private final DatasetServiceDAO datasetServiceDAO;
    private final GCSService gcsService;

    public DatasetRegistrationService(DatasetDAO datasetDAO, DacDAO dacDAO, DatasetServiceDAO datasetServiceDAO, GCSService gcsService) {
        this.datasetDAO = datasetDAO;
        this.dacDAO = dacDAO;
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
            Map<String, FormDataBodyPart> files) throws IOException, SQLException, IllegalArgumentException {

        if (Objects.isNull(dacDAO.findById(registration.getDataAccessCommitteeId()))) {
            throw new NotFoundException("Could not find DAC");
        }

        registration.setDataSubmitterUserId(user.getUserId());

        Map<String, BlobId> uploadedFileCache = new HashMap<>();

        List<DatasetServiceDAO.DatasetInsert> datasetInserts = new ArrayList<>();

        try {
            for (int consentGroupIdx = 0; consentGroupIdx < registration.getConsentGroups().size(); consentGroupIdx++) {
                datasetInserts.add(createDatasetInsert(registration, user, files, uploadedFileCache, consentGroupIdx));
            }
        } catch (IOException e) {
            // uploading files to GCS failed. rollback files...
            uploadedFileCache.values().forEach((id) -> {
                gcsService.deleteDocument(id.getName());
            });
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

    /*
    Upload all relevant files to GCS and create relevant
     */
    private DatasetServiceDAO.DatasetInsert createDatasetInsert(DatasetRegistrationSchemaV1 registration,
                                                                User user,
                                                                Map<String, FormDataBodyPart> files,
                                                                Map<String, BlobId> uploadedFileCache,
                                                                Integer consentGroupIdx) throws IOException {
        ConsentGroup consentGroup = registration.getConsentGroups().get(consentGroupIdx);

        List<DatasetProperty> props = convertRegistrationToDatasetProperties(registration, consentGroup);
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
        dataUse.setPublicationMoratorium(Objects.nonNull(group.getMor()) && group.getMor() ? group.getMorDate() : null);

        dataUse.setMethodsResearch(Objects.nonNull(group.getMor()) && group.getNmds() ? false : null);
        dataUse.setCommercialUse(Objects.nonNull(group.getNpu()) ? !group.getNpu() : null);
        dataUse.setOther(group.getOtherPrimary());
        dataUse.setSecondaryOther(group.getOtherSecondary());
        dataUse.setPopulationOriginsAncestry(group.getPoa());
        dataUse.setPublicationResults(group.getPub());

        return dataUse;
    }

    private static final String ALTERNATIVE_DATA_SHARING_PLAN_NAME = "alternativeDataSharingPlan";
    private static final String NIH_INSTITUTIONAL_CERTIFICATION_NAME = "consentGroups[%s].nihInstitutionalCertificationFile";

    private List<FileStorageObject> uploadFiles(Map<String, FormDataBodyPart> files,
                                                Map<String, BlobId> uploadedFileCache,
                                                Integer consentGroupIdx,
                                                User user) throws IOException {
        List<FileStorageObject> consentGroupFSOs = new ArrayList<>();

        if (files.containsKey(ALTERNATIVE_DATA_SHARING_PLAN_NAME)) {
            consentGroupFSOs.add(uploadFile(
                                 files, uploadedFileCache, user,
                                 ALTERNATIVE_DATA_SHARING_PLAN_NAME,
                                 FileCategory.ALTERNATIVE_DATA_SHARING_PLAN));
        }

        if (files.containsKey(String.format(NIH_INSTITUTIONAL_CERTIFICATION_NAME, consentGroupIdx))) {
            consentGroupFSOs.add(uploadFile(
                    files, uploadedFileCache, user,
                    String.format(NIH_INSTITUTIONAL_CERTIFICATION_NAME, consentGroupIdx),
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
    public record DatasetPropertyExtractor(
            String name,
            String schemaProp,
            DatasetPropertyType type,
            /*
             * Takes in: Dataset registration object and consent group
             * Produces: The value of the field, can be null if field not present.
             */
            BiFunction<DatasetRegistrationSchemaV1,ConsentGroup,Object> getField
    ) {

        /**
         * Converts a field on the given registration to a DatasetProperty.
         *
         * @param registration The registration object to extract from
         * @param consentGroup The index of the consent group to extract from
         * @return The dataset property, if the field has a value, otherwise Optional.empty()
         */
        Optional<DatasetProperty> extract(DatasetRegistrationSchemaV1 registration, ConsentGroup consentGroup) {
            Object value = this.getField.apply(registration, consentGroup);
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

    private static final List<DatasetPropertyExtractor> DATASET_REGISTRATION_V1_PROPERTY_EXTRACTORS = List.of(
            new DatasetPropertyExtractor(
                    "PI Name", "piName", DatasetPropertyType.String,
                    (registration, consentGroup) -> registration.getPiName()),
            new DatasetPropertyExtractor(
                    "Study Name", "studyName", DatasetPropertyType.String,
                    (registration, consentGroup) -> registration.getStudyName()),
            new DatasetPropertyExtractor(
                    "Study Type", "studyType", DatasetPropertyType.String,
                    (registration, consentGroup) -> registration.getStudyType().value()),
            new DatasetPropertyExtractor(
                    "Data Types", "dataTypes", DatasetPropertyType.Json,
                    (registration, consentGroup) -> {
                        if (Objects.nonNull(registration.getDataTypes())) {
                            return GsonUtil.getInstance().toJson(registration.getDataTypes());
                        }
                        return null;
                    }),
            new DatasetPropertyExtractor(
                    "Study Description", "studyDescription", DatasetPropertyType.String,
                    (registration, consentGroup) -> registration.getStudyDescription()),
            new DatasetPropertyExtractor(
                    "Phenotype Indication", "phenotypeIndication", DatasetPropertyType.String,
                    (registration, consentGroup) -> registration.getPhenotypeIndication()),
            new DatasetPropertyExtractor(
                    "Species", "species", DatasetPropertyType.String,
                    (registration, consentGroup) -> registration.getSpecies()),
            new DatasetPropertyExtractor(
                    "Data Submitter User ID", "dataSubmitterUserId", DatasetPropertyType.Number,
                    (registration, consentGroup) -> registration.getDataSubmitterUserId()),
            new DatasetPropertyExtractor(
                    "Data Custodian Email", "dataCustodianEmail", DatasetPropertyType.Json,
                    (registration, consentGroup) -> {
                        if (Objects.nonNull(registration.getDataCustodianEmail())) {
                            return GsonUtil.getInstance().toJson(registration.getDataCustodianEmail());
                        }
                        return null;

                    }),
            new DatasetPropertyExtractor(
                    "Public Visibility", "publicVisibility", DatasetPropertyType.Boolean,
                    (registration, consentGroup) -> registration.getPublicVisibility()),
            new DatasetPropertyExtractor(
                    "NIH Anvil Use", "nihAnvilUse", DatasetPropertyType.Json,
                    (registration, consentGroup) -> {
                        if (Objects.nonNull(registration.getNihAnvilUse())) {
                            return GsonUtil.getInstance().toJson(registration.getNihAnvilUse().stream().map(NihAnvilUse::value).toList());
                        }
                        return null;
                    }),
            new DatasetPropertyExtractor(
                    "Submitting To Anvil", "submittingToAnvil", DatasetPropertyType.Boolean,
                    (registration, consentGroup) -> registration.getSubmittingToAnvil()),
            new DatasetPropertyExtractor(
                    "dbGaP phs ID", "dbGaPPhsID", DatasetPropertyType.String,
                    (registration, consentGroup) -> registration.getDbGaPPhsID()),
            new DatasetPropertyExtractor(
                    "dbGaP Study Registration Name", "dbGaPStudyRegistrationName", DatasetPropertyType.String,
                    (registration, consentGroup) -> registration.getDbGaPStudyRegistrationName()),
            new DatasetPropertyExtractor(
                    "Embargo Release Date", "embargoReleaseDate", DatasetPropertyType.Date,
                    (registration, consentGroup) -> registration.getEmbargoReleaseDate()),
            new DatasetPropertyExtractor(
                    "Sequencing Center", "sequencingCenter", DatasetPropertyType.String,
                    (registration, consentGroup) -> registration.getSequencingCenter()),
            new DatasetPropertyExtractor(
                    "PI Email", "piEmail", DatasetPropertyType.String,
                    (registration, consentGroup) -> registration.getPiEmail()),
            new DatasetPropertyExtractor(
                    "PI Institution", "piInstitution", DatasetPropertyType.Number,
                    (registration, consentGroup) -> registration.getPiInstitution()),
            new DatasetPropertyExtractor(
                    "NIH Grant Contract Number", "nihGrantContractNumber", DatasetPropertyType.String,
                    (registration, consentGroup) -> registration.getNihGrantContractNumber()),
            new DatasetPropertyExtractor(
                    "NIH ICs Supporting Study", "nihICsSupportingStudy", DatasetPropertyType.Json,
                    (registration, consentGroup) -> {
                        if (Objects.nonNull(registration.getNihICsSupportingStudy())) {
                            return GsonUtil.getInstance().toJson(registration.getNihICsSupportingStudy().stream().map(NihICsSupportingStudy::value).toList());
                        }
                        return null;
                    }),
            new DatasetPropertyExtractor(
                    "NIH Program Officer Name", "nihProgramOfficerName", DatasetPropertyType.String,
                    (registration, consentGroup) -> registration.getNihProgramOfficerName()),
            new DatasetPropertyExtractor(
                    "NIH Institution Center Submission", "nihInstitutionCenterSubmission", DatasetPropertyType.String,
                    (registration, consentGroup) -> {
                        if (Objects.nonNull(registration.getNihInstitutionCenterSubmission())) {
                            return registration.getNihInstitutionCenterSubmission().value();
                        }
                        return null;
                    }),
            new DatasetPropertyExtractor(
                    "NIH Genomic Program Administrator Name", "nihGenomicProgramAdministratorName", DatasetPropertyType.String,
                    (registration, consentGroup) -> registration.getNihGenomicProgramAdministratorName()),
            new DatasetPropertyExtractor(
                    "Multi Center Study", "multiCenterStudy", DatasetPropertyType.Boolean,
                    (registration, consentGroup) -> registration.getMultiCenterStudy()),
            new DatasetPropertyExtractor(
                    "Collaborating Sites", "collaboratingSites", DatasetPropertyType.Json,
                    (registration, consentGroup) -> {
                        if (Objects.nonNull(registration.getCollaboratingSites())) {
                            return GsonUtil.getInstance().toJson(registration.getCollaboratingSites());
                        }
                        return null;
                    }),
            new DatasetPropertyExtractor(
                    "Controlled Access Required For Genomic Summary Results GSR", "controlledAccessRequiredForGenomicSummaryResultsGSR", DatasetPropertyType.Boolean,
                    (registration, consentGroup) -> registration.getControlledAccessRequiredForGenomicSummaryResultsGSR()),
            new DatasetPropertyExtractor(
                    "Controlled Access Not Required For Genomic Summary Results GSR Explanation", "controlledAccessRequiredForGenomicSummaryResultsGSRNotRequiredExplanation", DatasetPropertyType.String,
                    (registration, consentGroup) -> registration.getControlledAccessRequiredForGenomicSummaryResultsGSRNotRequiredExplanation()),
            new DatasetPropertyExtractor(
                    "Alternative Data Sharing Plan", "alternativeDataSharingPlan", DatasetPropertyType.Boolean,
                    (registration, consentGroup) -> registration.getAlternativeDataSharingPlan()),
            new DatasetPropertyExtractor(
                    "Alternative Data Sharing Plan Reasons", "alternativeDataSharingPlanReasons", DatasetPropertyType.Json,
                    (registration, consentGroup) -> {
                        if (Objects.nonNull(registration.getAlternativeDataSharingPlanReasons())) {
                            return GsonUtil.getInstance().toJson(registration.getAlternativeDataSharingPlanReasons().stream().map(AlternativeDataSharingPlanReason::value).toList());
                        }
                        return null;
                    }),
            new DatasetPropertyExtractor(
                    "Alternative Data Sharing Plan Explanation", "alternativeDataSharingPlanExplanation", DatasetPropertyType.String,
                    (registration, consentGroup) -> registration.getAlternativeDataSharingPlanExplanation()),
            new DatasetPropertyExtractor(
                    "Alternative Data Sharing Plan File Name", "alternativeDataSharingPlanFileName", DatasetPropertyType.String,
                    (registration, consentGroup) -> registration.getAlternativeDataSharingPlanFileName()),
            new DatasetPropertyExtractor(
                    "Alternative Data Sharing Plan Data Submitted", "alternativeDataSharingPlanDataSubmitted", DatasetPropertyType.String,
                    (registration, consentGroup) -> {
                        if (Objects.nonNull(registration.getAlternativeDataSharingPlanDataSubmitted())) {
                            return registration.getAlternativeDataSharingPlanDataSubmitted().value();
                        }
                        return null;
                    }),
            new DatasetPropertyExtractor(
                    "Alternative Data Sharing Plan Data Released", "alternativeDataSharingPlanDataReleased", DatasetPropertyType.Boolean,
                    (registration, consentGroup) -> registration.getAlternativeDataSharingPlanDataReleased()),
            new DatasetPropertyExtractor(
                    "Alternative Data Sharing Plan Target Delivery Date", "alternativeDataSharingPlanTargetDeliveryDate", DatasetPropertyType.Date,
                    (registration, consentGroup) -> registration.getAlternativeDataSharingPlanTargetDeliveryDate()),
            new DatasetPropertyExtractor(
                    "Alternative Data Sharing Plan Target Public Release Date", "alternativeDataSharingPlanTargetPublicReleaseDate", DatasetPropertyType.Date,
                    (registration, consentGroup) -> registration.getAlternativeDataSharingPlanTargetPublicReleaseDate()),
            new DatasetPropertyExtractor(
                    "Alternative Data Sharing Plan Controlled Open Access", "alternativeDataSharingPlanControlledOpenAccess", DatasetPropertyType.String,
                    (registration, consentGroup) -> {
                        if (Objects.nonNull(registration.getAlternativeDataSharingPlanControlledOpenAccess())) {
                            return registration.getAlternativeDataSharingPlanControlledOpenAccess().value();
                        }
                        return null;
                    }),
            new DatasetPropertyExtractor(
                    "Data Location", "consentGroup.dataLocation", DatasetPropertyType.Json,
                    (registration, consentGroup) -> {
                        if (Objects.nonNull(consentGroup.getDataLocation())) {
                            return GsonUtil.getInstance().toJson(consentGroup.getDataLocation().stream().map(DataLocation::value).toList());
                        }
                        return null;
                    }),
            new DatasetPropertyExtractor(
                    "File Types", "consentGroup.fileTypes", DatasetPropertyType.Json,
                    (registration, consentGroup) -> {
                        if (Objects.nonNull(consentGroup.getFileTypes())) {
                            return GsonUtil.getInstance().toJson(consentGroup.getFileTypes());
                        }
                        return null;
                    }),
            new DatasetPropertyExtractor(
                    "URL", "consentGroup.url", DatasetPropertyType.String,
                    (registration, consentGroup) -> {
                        if (Objects.nonNull(consentGroup.getUrl())) {
                            return consentGroup.getUrl().toString();
                        }
                        return null;
                    })
    );


    private List<DatasetProperty> convertRegistrationToDatasetProperties(DatasetRegistrationSchemaV1 registration, ConsentGroup consentGroup) {

        return DATASET_REGISTRATION_V1_PROPERTY_EXTRACTORS
                .stream()
                .map((e) -> e.extract(registration, consentGroup))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

}
