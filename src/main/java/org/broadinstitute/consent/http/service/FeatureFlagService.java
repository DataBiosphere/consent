package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import java.util.List;
import org.broadinstitute.consent.http.db.FeatureFlagDAO;
import org.broadinstitute.consent.http.models.FeatureFlag;
import org.broadinstitute.consent.http.util.ConsentLogger;

public class FeatureFlagService implements ConsentLogger {

  private final FeatureFlagDAO featureFlagDAO;

  @Inject
  public FeatureFlagService(FeatureFlagDAO featureFlagDAO) {
    this.featureFlagDAO = featureFlagDAO;
  }

  /**
   * Get all feature flags
   *
   * @return List of all feature flags
   */
  public List<FeatureFlag> getAllFeatureFlags() {
    return featureFlagDAO.findAll();
  }

  /**
   * Get a feature flag by id
   *
   * @param id The feature flag id
   * @return The feature flag
   * @throws NotFoundException if the feature flag does not exist
   */
  public FeatureFlag getFeatureFlagById(String id) {
    FeatureFlag flag = featureFlagDAO.findById(id);
    if (flag == null) {
      throw new NotFoundException("Feature flag with id '" + id + "' not found");
    }
    return flag;
  }

  /**
   * Get the value of a feature flag by id, or return null if not found This is a helper method for
   * other services
   *
   * @param id The feature flag id
   * @return The feature flag value, or null if not found
   */
  public String getFeatureFlagValue(String id) {
    FeatureFlag flag = featureFlagDAO.findById(id);
    return flag != null ? flag.getValue() : null;
  }

  /**
   * Get the value of a feature flag by id, or return a default value if not found This is a helper
   * method for other services
   *
   * @param id The feature flag id
   * @param defaultValue The default value to return if not found
   * @return The feature flag value, or the default value if not found
   */
  public String getFeatureFlagValue(String id, String defaultValue) {
    FeatureFlag flag = featureFlagDAO.findById(id);
    return flag != null ? flag.getValue() : defaultValue;
  }

  /**
   * Check if a feature flag is enabled (value is "true") This is a helper method for other services
   *
   * @param id The feature flag id
   * @return true if the flag exists and its value is "true", false otherwise
   */
  public boolean isFeatureEnabled(String id) {
    String value = getFeatureFlagValue(id);
    return "true".equalsIgnoreCase(value);
  }

  /**
   * Create or update a feature flag
   *
   * @param id The feature flag id
   * @param value The feature flag value
   * @return The created or updated feature flag
   */
  public FeatureFlag createOrUpdateFeatureFlag(String id, String value) {
    if (id == null || id.trim().isEmpty()) {
      throw new IllegalArgumentException("Feature flag id cannot be empty");
    }
    if (value == null) {
      throw new IllegalArgumentException("Feature flag value cannot be null");
    }

    if (featureFlagDAO.exists(id)) {
      featureFlagDAO.update(id, value);
    } else {
      featureFlagDAO.insert(id, value);
    }
    return featureFlagDAO.findById(id);
  }

  /**
   * Delete a feature flag by id
   *
   * @param id The feature flag id
   * @throws NotFoundException if the feature flag does not exist
   */
  public void deleteFeatureFlag(String id) {
    if (!featureFlagDAO.exists(id)) {
      throw new NotFoundException("Feature flag with id '" + id + "' not found");
    }
    featureFlagDAO.deleteById(id);
  }

  /**
   * Check if a feature flag exists
   *
   * @param id The feature flag id
   * @return true if the feature flag exists, false otherwise
   */
  public boolean exists(String id) {
    return featureFlagDAO.exists(id);
  }
}
