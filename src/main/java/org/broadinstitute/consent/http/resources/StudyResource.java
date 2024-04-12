package org.broadinstitute.consent.http.resources;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.validator.routines.EmailValidator;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.StudyConversion;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1UpdateValidator;
import org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder;
import org.broadinstitute.consent.http.service.DatasetRegistrationService;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.ElasticSearchService;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.FormDataParam;

@Path("api/dataset/study")
public class StudyResource extends Resource {

  private final DatasetService datasetService;
  private final DatasetRegistrationService datasetRegistrationService;
  private final UserService userService;
  private final ElasticSearchService elasticSearchService;


  @Inject
  public StudyResource(DatasetService datasetService, UserService userService,
      DatasetRegistrationService datasetRegistrationService,
      ElasticSearchService elasticSearchService) {
    this.datasetService = datasetService;
    this.userService = userService;
    this.datasetRegistrationService = datasetRegistrationService;
    this.elasticSearchService = elasticSearchService;
  }

  /**
   * This API creates a study for a provided dataset, or updates existing study/dataset information
   * with what is provided in the request body. It is intended to be a short-lived API that will be
   * removed once all production datasets have been migrated.
   */
  @PUT
  @Path("/convert/{datasetIdentifier}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed({ADMIN})
  public Response convertToStudy(@Auth AuthUser authUser,
    @PathParam("datasetIdentifier") String datasetIdentifier, String json) {
    try {
      User user = userService.findUserByEmail(authUser.getEmail());
      Dataset dataset = datasetService.findDatasetByIdentifier(datasetIdentifier);
      StudyConversion studyConversion = new Gson().fromJson(json, StudyConversion.class);
      Study study = datasetService.convertDatasetToStudy(user, dataset, studyConversion);
      return Response.ok(study).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  /**
   * This API adds/updates custodians for a study. The payload needs to be a JSON array of valid
   * email addresses
   */
  @PUT
  @Path("/{studyId}/custodians")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed({ADMIN})
  public Response updateCustodians(@Auth AuthUser authUser,
    @PathParam("studyId") Integer studyId, String json) {
    try {
      User user = userService.findUserByEmail(authUser.getEmail());
      Gson gson = new Gson();
      Type listOfStringObject = new TypeToken<ArrayList<String>>() {}.getType();
      List<String> custodians = gson.fromJson(json, listOfStringObject);
      // Validate that the custodians are all valid email addresses:
      EmailValidator emailValidator = EmailValidator.getInstance();
      List<Boolean> valid = custodians.stream().map(emailValidator::isValid).toList();
      if (valid.contains(false)) {
        throw new BadRequestException(String.format("Invalid email address: %s", json));
      }
      Study study = datasetService.updateStudyCustodians(user, studyId, json);
      return Response.ok(study).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Path("/{studyId}")
  @Produces(MediaType.APPLICATION_JSON)
  @PermitAll
  public Response getStudyById(@PathParam("studyId") Integer studyId) {
    try {
      Study study = datasetService.getStudyWithDatasetsById(studyId);
      return Response.ok(study).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @DELETE
  @Path("/{studyId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed({ADMIN, CHAIRPERSON, DATASUBMITTER})
  public Response deleteStudyById(@Auth AuthUser authUser, @PathParam("studyId") Integer studyId) {
    try {
      User user = userService.findUserByEmail(authUser.getEmail());
      Study study = datasetService.getStudyWithDatasetsById(studyId);

      if (Objects.isNull(study)) {
        throw new NotFoundException("Study not found");
      }

      // If the user is not an admin, ensure that they are the study/dataset creator
      if (!user.hasUserRole(UserRoles.ADMIN) && (!Objects.equals(study.getCreateUserId(),
          user.getUserId()))) {
        throw new NotFoundException("Study not found");
      }

      boolean deletable = study.getDatasets()
          .stream()
          .allMatch(Dataset::getDeletable);
      if (!deletable) {
        throw new BadRequestException("Study has datasets that are in use and cannot be deleted.");
      }
      Set<Integer> studyDatasetIds = study.getDatasetIds();
      datasetService.deleteStudy(study, user);
      // Remove from ES index
      studyDatasetIds.forEach(id -> {
        try {
          elasticSearchService.deleteIndex(id);
        } catch (IOException e) {
          logException(e);
        }
      });
      return Response.ok().build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Path("/registration/{studyId}")
  @Produces(MediaType.APPLICATION_JSON)
  @PermitAll
  public Response getRegistrationFromStudy(@Auth AuthUser authUser,
      @PathParam("studyId") Integer studyId) {
    try {
      Study study = datasetService.getStudyWithDatasetsById(studyId);
      List<Dataset> datasets =
          Objects.nonNull(study.getDatasets()) ? study.getDatasets().stream().toList() : List.of();
      DatasetRegistrationSchemaV1 registration = new DatasetRegistrationSchemaV1Builder().build(
          study, datasets);
      String entity = GsonUtil.buildGsonNullSerializer().toJson(registration);
      return Response.ok().entity(entity).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @PUT
  @Consumes({MediaType.MULTIPART_FORM_DATA})
  @Produces({MediaType.APPLICATION_JSON})
  @Path("/{studyId}")
  @RolesAllowed({ADMIN, CHAIRPERSON, DATASUBMITTER})
  /*
   * This endpoint accepts a json instance of a dataset-registration-schema_v1.json schema.
   * With that object, we can fully update the study/datasets from the provided values.
   */
  public Response updateStudyByRegistration(
      @Auth AuthUser authUser,
      FormDataMultiPart multipart,
      @PathParam("studyId") Integer studyId,
      @FormDataParam("dataset") String json) {
    try {
      User user = userService.findUserByEmail(authUser.getEmail());
      Study existingStudy = datasetRegistrationService.findStudyById(studyId);

      // Manually validate the schema from an editing context. Validation with the schema tools
      // enforces it in a creation context but doesn't work for editing purposes.
      DatasetRegistrationSchemaV1UpdateValidator updateValidator = new DatasetRegistrationSchemaV1UpdateValidator();
      Gson gson = GsonUtil.gsonBuilderWithAdapters().create();
      DatasetRegistrationSchemaV1 registration = gson.fromJson(json,
          DatasetRegistrationSchemaV1.class);

      if (updateValidator.validate(existingStudy, registration)) {
        // Update study from registration
        Map<String, FormDataBodyPart> files = extractFilesFromMultiPart(multipart);
        Study updatedStudy = datasetRegistrationService.updateStudyFromRegistration(
            studyId,
            registration,
            user,
            files);
        try (Response indexResponse = elasticSearchService.indexStudy(studyId))  {
          if (indexResponse.getStatus() >= Status.BAD_REQUEST.getStatusCode()) {
            logWarn("Non-OK response when reindexing study with id: " + studyId);
          }
        } catch (Exception e) {
          logException("Exception re-indexing datasets from study id: " + studyId, e);
        }
        return Response.ok(updatedStudy).build();
      } else {
        return Response.status(Status.BAD_REQUEST).build();
      }
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  /**
   * Finds and validates all the files uploaded to the multipart.
   *
   * @param multipart Form data
   * @return Map of file body parts, where the key is the name of the field and the value is the
   * body part including the file(s).
   */
  private Map<String, FormDataBodyPart> extractFilesFromMultiPart(FormDataMultiPart multipart) {
    if (Objects.isNull(multipart)) {
      return Map.of();
    }

    Map<String, FormDataBodyPart> files = new HashMap<>();
    for (List<FormDataBodyPart> parts : multipart.getFields().values()) {
      for (FormDataBodyPart part : parts) {
        if (Objects.nonNull(part.getContentDisposition().getFileName())) {
          validateFileDetails(part.getContentDisposition());
          files.put(part.getName(), part);
        }
      }
    }

    return files;
  }

}
