package org.broadinstitute.consent.http.service;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.users.handler.UserRolesHandler;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class InstitutionService {

  private final InstitutionDAO institutionDAO;
  private final UserRolesHandler userRolesHandler;

  @Inject
  public InstitutionService(InstitutionDAO institutionDAO, UserRolesHandler userRolesHandler) {
    this.institutionDAO = institutionDAO;
    this.userRolesHandler = userRolesHandler;
  }

  public Integer createInstitution(String name, String itDirectorName, String itDirectorEmail, Integer createUser) {
    return this.institutionDAO.insertInstitution(name, itDirectorName, itDirectorEmail, createUser, new Date());
  }

  //NOTE: if name column is designated as unique, we can use the db to throw errors for repeat names
  public String createInstitution(String payload, User user) {
      Institution institution = buildInstitution(payload);
      //NOTE: should an error be thrown if id is NOT null? Data may be valid, but id present indicates non-normal request
      if(institution.getId() == null) {
        throw new IllegalArgumentException("Cannot pass a value for ID when creating a new Institution");
      } else if(institution.getName() == null || institution.getName().isBlank()) {
        throw new IllegalArgumentException("Institution name cannot be null or empty");
      } else {
        Date createTimestamp = new Date();
        institution.setCreateDate(createTimestamp);
        institution.setCreateUser(user.getDacUserId());
        institutionDAO.insertInstitution(
          institution.getName(),
          institution.getItDirectorName(),
          institution.getItDirectorEmail(),
          user.getDacUserId(),
          createTimestamp
        );
        return new Gson().toJson(institution, Institution.class);
      }
  }

  public void updateInstitutionById(Integer id,
                                    String institutionName,
                                    String itDirectorName,
                                    String itDirectorEmail,
                                    Integer updateUser) {
    this.institutionDAO.updateInstitutionById(
      id, institutionName, itDirectorName, itDirectorEmail, updateUser, new Date());
  }

  public void deleteInstitutionById(Integer id) {
    this.institutionDAO.deleteInstitutionById(id);
  }

  public String findInstitutionById(User user, Integer id) {
    Boolean isAdmin = this.checkIfAdmin(user);
    Institution institution = this.institutionDAO.findInstitutionById(id);
    Gson gson = this.getGsonBuilder(isAdmin);
    return gson.toJson(institution);
  }

  public String findAllInstitutions(User user) {
    Boolean isAdmin = this.checkIfAdmin(user);
    List<Institution> institutions = this.institutionDAO.findAllInstitutions();
    //nulling date attributes for non-admin roles
    Gson gson = this.getGsonBuilder(isAdmin);
    return gson.toJson(institutions);
  }

  private Institution buildInstitution(String institutionJson) {
    Institution payload = new Gson().fromJson(institutionJson, Institution.class);
    return payload;
  }


  private Boolean checkIfAdmin(User user) {
    return userRolesHandler.containsRole(user.getRoles(), UserRoles.ADMIN.getRoleName());
  }

  //Gson builder and exclusion strategy helpers
  //Opting to not null values, null has the implication of an absence of value
  //Whereas the absence of a field can mean an absence of value OR an omission of data
  private Gson getGsonBuilder(Boolean isAdmin) {
    ExclusionStrategy strategy = getSerializationExclusionStrategy(isAdmin);
    return new GsonBuilder()
      .addSerializationExclusionStrategy(strategy)
      .create();
  }

  private ExclusionStrategy getSerializationExclusionStrategy(Boolean isAdmin) {
    return new ExclusionStrategy() {
      @Override
      public boolean shouldSkipField(FieldAttributes field) {
        String fieldName = field.getName();
        if(!isAdmin && (fieldName == "createDate" || fieldName == "updateDate")) {
          return true;
        } else {
          return false;
        }
      }
      @Override
      public boolean shouldSkipClass(Class<?> c) {
        return false;
      }
    };
  }
}
