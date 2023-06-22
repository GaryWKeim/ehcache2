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
import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.UnsupportedEncodingException;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author: Anthony Dahanne
 * The aim of this test is to check via HTTP that the ehcache standalone agent /tc-management-api/agents/cacheManagers/caches endpoint
 * works fine
 */
public class CacheStatisticSamplesResourceServiceImplTest extends ResourceServiceImplITHelper {
  protected static final String EXPECTED_RESOURCE_LOCATION = "/tc-management-api/v2/agents{agentIds}/cacheManagers{cmIds}/caches{cacheIds}/statistics/samples{sampleIds}";

  @BeforeClass
  public static void setUpCluster() throws Exception {
    setUpCluster(CacheStatisticSamplesResourceServiceImplTest.class);
  }

  @Before
  public void setUp() throws UnsupportedEncodingException {
    // Leave testCache2 empty because it uses Byte-based sizing and we can't/don't want to open up java.lang
    cacheManagerMaxBytes = getCacheManagerMaxBytes();
  }

  @Test
  /**
   * - GET the list of cache statistics
   *
   * @throws Exception
   */
  public void getCacheStatisticSamples() throws Exception {
    /*
[
  {
    "version": null,
    "agentId": "embedded",
    "name": "testCache2",
    "cacheManagerName": "testCacheManagerProgrammatic",
    "statName": "LocalHeapSize",
    "statValueByTimeMillis": {
      "1371850582455": 1000,
      "1371850583455": 1000,
      "1371850584455": 1000,
      "1371850585455": 1000
    }
  }
]
     */

    String agentsFilter = "";
    String cmsFilter = "";
    String cachesFilter = "";
    String samplesFilter = "";

    Cache exampleCache = cacheManagerMaxElements.getCache("testCache");

    for (int i = 0; i < 1000; i++) {
      exampleCache.put(new Element("key" + i, "value" + i));
      givenStandalone()
        .expect()
        .statusCode(200)
        .when()
        .get(EXPECTED_RESOURCE_LOCATION, agentsFilter, cmsFilter, cachesFilter, samplesFilter);
    }

    cmsFilter= ";names=testCacheManagerProgrammatic";
    cachesFilter = ";names=testCache2";
    samplesFilter = ";names=LocalHeapSize";
    // we precise the cacheManager, cache and 2 stats we want to retrieve
    givenStandalone()
    .expect().contentType(ContentType.JSON).rootPath("entities")
      .body("get(0).agentId", equalTo("embedded"))
      .body("get(0).name", equalTo("testCache2"))
      .body("get(0).cacheManagerName", equalTo("testCacheManagerProgrammatic"))
      .body("get(0).statName", equalTo("LocalHeapSize"))
      // we got samples
      .body("get(0).statValueByTimeMillis.size()", greaterThan(0))
      // LocalHeapSize == 0
      .body("get(0).statValueByTimeMillis.values()[0]", equalTo(0))
      .body("size()", is(1))
      .statusCode(200)
    .when()
      .get(EXPECTED_RESOURCE_LOCATION, agentsFilter, cmsFilter, cachesFilter, samplesFilter);

    cmsFilter= "";
    // we precise the cache and 2 stats we want to retrieve
    givenStandalone()
    .expect().contentType(ContentType.JSON).rootPath("entities")
      .body("get(0).agentId", equalTo("embedded"))
      .body("get(0).name", equalTo("testCache2"))
      .body("get(0).cacheManagerName", equalTo("testCacheManagerProgrammatic"))
      .body("get(0).statName", equalTo("LocalHeapSize"))
              // we got no samples
      .body("get(0).statValueByTimeMillis.size()", greaterThan(0))
              // LocalHeapSize == null
      .body("get(0).statValueByTimeMillis.values()[0]", equalTo(0))
      .body("size()", is(1))
      .statusCode(200)
    .when()
      .get(EXPECTED_RESOURCE_LOCATION, agentsFilter, cmsFilter, cachesFilter, samplesFilter);

    cachesFilter = "";
    // we precise 2 stats we want to retrieve
    givenStandalone()
    .expect()
      .contentType(ContentType.JSON).rootPath("entities")
      .body("size()", is(2))
      .rootPath("entities.find { it.name =='testCache2' }")
        .body("agentId", equalTo("embedded"))
        .body("cacheManagerName", equalTo("testCacheManagerProgrammatic"))
        .body("statName", equalTo("LocalHeapSize"))
        .body("statValueByTimeMillis.size()", greaterThan(0))
        .body("statValueByTimeMillis.values()[0]", equalTo(0))
      .rootPath("entities.find { it.name =='testCache' }")
        .body("agentId", equalTo("embedded"))
        .body("cacheManagerName", equalTo("testCacheManager"))
        .body("statName", equalTo("LocalHeapSize"))
        .body("statValueByTimeMillis.size()", greaterThan(0))
        .body("statValueByTimeMillis.values()[0]", greaterThan(0))
      .statusCode(200)
    .when()
      .get(EXPECTED_RESOURCE_LOCATION, agentsFilter, cmsFilter, cachesFilter, samplesFilter);

    samplesFilter = "";
    // we precise nothing : we get it all !
    givenStandalone()
    .expect()
      .contentType(ContentType.JSON).rootPath("entities")
      .body("size()", greaterThan(40))
      .statusCode(200)
    .when()
      .get(EXPECTED_RESOURCE_LOCATION, agentsFilter, cmsFilter, cachesFilter, samplesFilter);
  }

  @Test
  /**
   * - GET the list of cache statistics
   *
   * @throws Exception
   */
  public void getCacheStatisticSamples__clustered() throws Exception {
    String agentsFilter = ";ids=" + cacheManagerMaxElementsAgentId;
    String cmsFilter= ";names=testCacheManager";
    String cachesFilter = ";names=testCache";
    String samplesFilter = ";names=LocalHeapSize";

    Cache exampleCache = cacheManagerMaxElements.getCache("testCache");
    for (int i = 0; i < 1000; i++) {
      exampleCache.put(new Element("key" + i, "value" + i));
      givenClustered()
        .expect()
        .statusCode(200)
        .when()
        .get(EXPECTED_RESOURCE_LOCATION, agentsFilter, cmsFilter, cachesFilter, samplesFilter);
    }

    // we precise the cacheManager, cache and 2 stats we want to retrieve
    givenClustered()
    .expect()
      .contentType(ContentType.JSON).rootPath("entities")
      .body("get(0).agentId", equalTo(cacheManagerMaxElementsAgentId))
      .body("get(0).name", equalTo("testCache"))
      .body("get(0).cacheManagerName", equalTo("testCacheManager"))
      .body("get(0).statName", equalTo("LocalHeapSize"))
              // we got samples
      .body("get(0).statValueByTimeMillis.size()", greaterThan(0))
              // LocalHeapSize > 0
      .body("get(0).statValueByTimeMillis.values()[0]", greaterThan(0))
      .body("size()", is(1))
      .statusCode(200)
    .when()
      .get(EXPECTED_RESOURCE_LOCATION, agentsFilter, cmsFilter, cachesFilter, samplesFilter);

    cmsFilter= "";
    // we precise the cache and 2 stats we want to retrieve
    givenClustered()
    .expect()
      .contentType(ContentType.JSON).rootPath("entities")
      .body("get(0).agentId", equalTo(cacheManagerMaxElementsAgentId))
      .body("get(0).name", equalTo("testCache"))
      .body("get(0).cacheManagerName", equalTo("testCacheManager"))
      .body("get(0).statName", equalTo("LocalHeapSize"))
              // we got samples
      .body("get(0).statValueByTimeMillis.size()", greaterThan(0))
              // LocalHeapSize > 0
      .body("get(0).statValueByTimeMillis.values()[0]", greaterThan(0))
      .body("size()", is(1))
      .statusCode(200)
    .when()
      .get(EXPECTED_RESOURCE_LOCATION, agentsFilter, cmsFilter, cachesFilter, samplesFilter);

    agentsFilter = "";
    cachesFilter = "";
    // we precise 2 stats we want to retrieve
    givenClustered()
    .expect()
      .contentType(ContentType.JSON).rootPath("entities")
      .body("size()", is(2))
      .rootPath("entities.find { it.name =='testCache2' }")
        .body("agentId", equalTo(cacheManagerMaxBytesAgentId))
        .body("cacheManagerName", equalTo("testCacheManagerProgrammatic"))
        .body("statName", equalTo("LocalHeapSize"))
        .body("statValueByTimeMillis.size()", equalTo(0))
        .body("statValueByTimeMillis.values()[0]", nullValue())
      .rootPath("entities.find { it.name =='testCache' }")
        .body("agentId", equalTo(cacheManagerMaxElementsAgentId))
        .body("cacheManagerName", equalTo("testCacheManager"))
        .body("statName", equalTo("LocalHeapSize"))
        .body("statValueByTimeMillis.size()", greaterThan(0))
        .body("statValueByTimeMillis.values()[0]", greaterThan(0))
      .statusCode(200)
    .when()
      .get(EXPECTED_RESOURCE_LOCATION, agentsFilter, cmsFilter, cachesFilter, samplesFilter);

    samplesFilter = "";
    // we precise nothing : we get it all !
    givenClustered()
    .expect()
      .contentType(ContentType.JSON).rootPath("entities")
      .body("size()", greaterThan(40))
      .statusCode(200)
    .when()
      .get(EXPECTED_RESOURCE_LOCATION, agentsFilter, cmsFilter, cachesFilter, samplesFilter);
  }

  @After
  public void tearDown() {
    if (cacheManagerMaxBytes != null) {
      cacheManagerMaxBytes.shutdown();
    }
  }
}
