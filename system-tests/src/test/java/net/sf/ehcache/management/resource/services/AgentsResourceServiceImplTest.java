/*
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is
 *      Terracotta, Inc., a Software AG company
 */
package net.sf.ehcache.management.resource.services;

import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.junit.BeforeClass;
import org.junit.Test;

import static io.restassured.RestAssured.expect;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.containsString;

/**
 * @author: Anthony Dahanne
 * The aim of this test is to check via HTTP that the ehcache standalone agent /tc-management-api/agents/ endpoint
 * works fine
 */
public class AgentsResourceServiceImplTest extends ResourceServiceImplITHelper {
  protected static final String EXPECTED_RESOURCE_LOCATION = "/tc-management-api/v2/agents";

  @BeforeClass
  public static void setUpCluster() throws Exception {
    setUpCluster(AgentsResourceServiceImplTest.class);
  }

  @Test
  /**
   * - GET the list of agents
   * - GET the subresource /info
   *
   * @throws Exception
   */
  public void getAgentsTest__OneCacheManager() throws Exception {
    givenStandalone()
    .expect()
      .contentType(ContentType.JSON)
      .rootPath("entities.get(0)")
      .body("agentId", equalTo("embedded"))
      .body("agencyOf", equalTo("Ehcache"))
      .body("rootRepresentables.cacheManagerNames", equalTo("testCacheManager"))
      .statusCode(200)
    .when()
      .get(EXPECTED_RESOURCE_LOCATION);

    givenStandalone()
    .expect().contentType(ContentType.JSON)
      .rootPath("entities.get(0)")
      .body("agentId", equalTo("embedded"))
      .body("agencyOf", equalTo("Ehcache"))
      .body("rootRepresentables.cacheManagerNames", equalTo("testCacheManager"))
      .statusCode(200)
    .when()
      .get(EXPECTED_RESOURCE_LOCATION +";ids=embedded");

    givenStandalone()
    .expect()
      .contentType(ContentType.JSON)
      .statusCode(400)
      .when().get(EXPECTED_RESOURCE_LOCATION +";ids=w00t");

    givenStandalone()
    .expect()
      .contentType(ContentType.JSON)
      .rootPath("entities.get(0)")
      .body("agentId", equalTo("embedded"))
      .body("agencyOf", equalTo("Ehcache"))
      .body("available", equalTo(true))
      .body("secured", equalTo(false))
      .body("sslEnabled", equalTo(false))
      .body("needClientAuth", equalTo(false))
      .body("licensed", equalTo(false))
      .body("sampleHistorySize", equalTo(30))
      .body("sampleIntervalSeconds", equalTo(1))
      .body("enabled", equalTo(true))
      .statusCode(200)
    .when()
      .get(EXPECTED_RESOURCE_LOCATION + INFO);

    givenStandalone()
    .expect()
      .contentType(ContentType.JSON)
      .rootPath("entities.get(0)")
      .body("agentId", equalTo("embedded"))
      .body("agencyOf", equalTo("Ehcache"))
      .body("available", equalTo(true))
      .body("secured", equalTo(false))
      .body("sslEnabled", equalTo(false))
      .body("needClientAuth", equalTo(false))
      .body("licensed", equalTo(false))
      .body("sampleHistorySize", equalTo(30))
      .body("sampleIntervalSeconds", equalTo(1))
      .body("enabled", equalTo(true))
      .statusCode(200)
    .when()
      .get(EXPECTED_RESOURCE_LOCATION  +";ids=embedded"+ INFO);
  }

  @Test
  /**
   * - GET the list of agents
   *
   * @throws Exception
   */
  public void getAgentsTest__TwoCacheManagers() throws Exception {
    // we configure the second cache manager programmatically
    cacheManagerMaxBytes = getCacheManagerMaxBytes();
    // let's check the agent was edited correctly server side
    givenStandalone()
    .expect()
      .contentType(ContentType.JSON)
      .rootPath("entities.get(0)")
      .body("agentId", equalTo("embedded"))
      .body("agencyOf", equalTo("Ehcache"))
      .body("rootRepresentables.cacheManagerNames", allOf(containsString("testCacheManagerProgrammatic"), containsString("testCacheManager")))
      .statusCode(200)
    .when()
      .get(EXPECTED_RESOURCE_LOCATION);
    cacheManagerMaxBytes.clearAll();
    cacheManagerMaxBytes.shutdown();
  }

  @Test
  public void getAgentsTest__clustered() throws Exception {
    givenClustered()
    .expect()
      .contentType(ContentType.JSON)
      .rootPath("entities")
      .body("get(0).agentId", Matchers.equalTo("embedded"))
      .body("get(0).agencyOf", Matchers.equalTo("TSA"))
      .body("get(0).rootRepresentables.urls", Matchers.equalTo("http://localhost:" + MANAGEMENT_PORT))
      .body("get(1).agentId", anyOf(containsString("localhost_"), containsString("127.0.0.1_"), containsString("localhost.localdomain_"), containsString("localhost.home_")))
      .body("get(1).agencyOf", Matchers.equalTo("Ehcache"))
      .statusCode(200)
    .when()
      .get(EXPECTED_RESOURCE_LOCATION);
  }
}
