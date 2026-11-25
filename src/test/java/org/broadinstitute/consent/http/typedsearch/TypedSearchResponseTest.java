package org.broadinstitute.consent.http.typedsearch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Checks the body of each typed search fixture with status 200 against the contract. */
class TypedSearchResponseTest {

  private static final String REQUEST =
      "{'view':'datasets','scope':{'library':null},'facets':[{'key':'dac'}],"
          + "'page':{'offset':0,'limit':2}}";

  static Stream<String> successes() throws Exception {
    return TypedSearchFixtures.fixtures()
        .filter(
            name -> {
              try {
                return TypedSearchFixtures.read(name).path("response").path("status").asInt()
                    == 200;
              } catch (Exception e) {
                throw new IllegalStateException(e);
              }
            });
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("successes")
  void bodyFollowsTheContract(String name) throws Exception {
    JsonNode fixture = TypedSearchFixtures.read(name);
    List<String> errors = new ArrayList<>();
    TypedSearchResponseCheck.checkBody(
        fixture.get("request"),
        TypedSearchFixtures.roles(fixture),
        fixture.path("response").get("body"),
        errors);
    assertEquals(List.of(), errors, name);
  }

  /** Each body breaks one rule for {@link #REQUEST}, so the check must reject it. */
  @ParameterizedTest(name = "{0}")
  @ValueSource(
      strings = {
        "{'view':'studies','total':0,'page':{'offset':0,'limit':2},'results':[],'facets':{'dac':[]}}",
        "{'view':'datasets','total':0,'page':{'offset':0,'limit':25},'results':[],'facets':{'dac':[]}}",
        "{'view':'datasets','total':'3','page':{'offset':0,'limit':2},'results':[],'facets':{'dac':[]}}",
        "{'view':'datasets','total':1,'page':{'offset':0,'limit':2},'results':[{},{}],'facets':{'dac':[]}}",
        "{'view':'datasets','total':1,'page':{'offset':0,'limit':2},'results':[1],'facets':{'dac':[]}}",
        "{'view':'datasets','total':1,'page':{'offset':0,'limit':2},'results':[{'deletable':true}],'facets':{'dac':[]}}",
        "{'view':'datasets','total':0,'page':{'offset':0,'limit':2},'results':[]}",
        "{'view':'datasets','total':1,'page':{'offset':0,'limit':2},'facets':{'dac':[]},"
            + "'results':[{'datasetId':1,'datasetName':null,'dataUse':{'primary':[],'secondary':[]}}]}",
        "{'view':'datasets','total':1,'page':{'offset':0,'limit':2},'facets':{'dac':[]},"
            + "'results':[{'datasetId':1}]}",
        "{'view':'datasets','total':0,'page':{'offset':0,'limit':2},'results':[],"
            + "'facets':{'dac':[{'value':'A','count':1},{'value':'B','count':2}]}}",
        "{'view':'datasets','total':0,'page':{'offset':0,'limit':2},'results':[],"
            + "'facets':{'dac':[{'value':'A','count':1.5}]}}"
      })
  void checkRejectsABodyThatBreaksTheContract(String body) throws Exception {
    List<String> errors = new ArrayList<>();
    TypedSearchResponseCheck.checkBody(
        TypedSearchFixtures.MAPPER.readTree(REQUEST.replace('\'', '"')),
        Set.of("RESEARCHER"),
        TypedSearchFixtures.MAPPER.readTree(body.replace('\'', '"')),
        errors);
    assertFalse(errors.isEmpty(), body);
  }
}
