package org.broadinstitute.consent.http.matching;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.broadinstitute.consent.http.service.OntologyService;
import org.broadinstitute.consent.http.service.ontology.OntologyTerm;
import org.broadinstitute.consent.http.service.ontology.ParentTerm;
import org.broadinstitute.consent.http.util.gson.GsonUtil;

public final class DataUseUtil {

  public void setOntologyService(OntologyService ontologyService) {
    this.ontologyService = ontologyService;
  }

  private OntologyService ontologyService;

  // Get a map of disease term to list of parent term ids (which also includes disease term id)
  public Map<String, List<String>> generatePurposeDiseaseIdMap(List<String> diseaseRestrictions)
      throws IOException {
    Map<String, List<String>> map = new HashMap<>();
    if (diseaseRestrictions == null) {
      return map;
    }
    for (String r : diseaseRestrictions) {
      map.put(r, getParentTermIds(r));
    }
    return map;
  }

  // Get a list of term ids that represent a disease term + all parent ids
  public List<String> getParentTermIds(String purposeDiseaseId) throws IOException {
    Gson gson = GsonUtil.getInstance();
    StreamingOutput output = ontologyService.findByTermIds(new String[] {purposeDiseaseId});
    List<String> purposeTermIdList =
        getJsonArrayFromStreamingOutput(output).asList().stream()
            .filter(Objects::nonNull)
            .map(jsonElement -> gson.fromJson(jsonElement, OntologyTerm.class))
            .filter(t -> Objects.nonNull(t.getParents()) && !t.getParents().isEmpty())
            .flatMap(t -> t.getParents().stream())
            .map(ParentTerm::getId)
            .collect(Collectors.toList());
    purposeTermIdList.add(purposeDiseaseId);
    return purposeTermIdList;
  }

  private JsonArray getJsonArrayFromStreamingOutput(StreamingOutput output) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    output.write(baos);
    String jsonString = baos.toString(StandardCharsets.UTF_8);
    return JsonParser.parseString(jsonString).getAsJsonArray();
  }
}
