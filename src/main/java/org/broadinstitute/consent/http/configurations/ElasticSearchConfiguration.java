package org.broadinstitute.consent.http.configurations;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ElasticSearchConfiguration {

  @NotNull private String indexName;

  @NotNull private List<String> servers;

  @NotNull private String datasetIndexName;

  private int port = 9200;

  private String protocol = "http";

  private String cloudId;

  private String authUser;

  private String authPassword;

  public List<String> getServers() {
    return servers;
  }

  public void setServers(List<String> servers) {
    this.servers = servers;
  }

  public String getIndexName() {
    return indexName;
  }

  public void setIndexName(String indexName) {
    this.indexName = indexName;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getDatasetIndexName() {
    return datasetIndexName;
  }

  public void setDatasetIndexName(String datasetIndexName) {
    this.datasetIndexName = datasetIndexName;
  }

  public String getProtocol() {
    return protocol;
  }

  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }

  public String getCloudId() {
    return cloudId;
  }

  public void setCloudId(String cloudId) {
    this.cloudId = cloudId;
  }

  public String getAuthUser() {
    return authUser;
  }

  public void setAuthUser(String authUser) {
    this.authUser = authUser;
  }

  public String getAuthPassword() {
    return authPassword;
  }

  public void setAuthPassword(String authPassword) {
    this.authPassword = authPassword;
  }
}
