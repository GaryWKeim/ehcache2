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

import static io.restassured.RestAssured.expect;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ManagementRESTServiceConfiguration;
import net.sf.ehcache.config.TerracottaClientConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.management.resource.CacheEntityV2;

import org.hamcrest.Matchers;
import org.junit.BeforeClass;
import org.junit.Test;

import io.restassured.http.ContentType;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: Anthony Dahanne
 * The aim of this test is to check via HTTP that the ehcache standalone agent /tc-management-api/agents/cacheManagers/caches endpoint
 * works fine
 */
public class CacheResourceServiceImplTest extends ResourceServiceImplITHelper {
  protected static final String EXPECTED_RESOURCE_LOCATION = "/tc-management-api/v2/agents{agentIds}/cacheManagers{cmIds}/caches{cacheIds}";

  @BeforeClass
  public static void setUpCluster() throws Exception {
    setUpCluster(CacheResourceServiceImplTest.class);
  }

  @Test
  /**
   * - GET the list of caches
   *
   * @throws Exception
   */
  public void getCachesTest() throws Exception {
    // I need a cacheManager not clustered
    CacheManager standaloneCacheManager = createStandaloneCacheManagerARC();
    Cache cacheStandalone = standaloneCacheManager.getCache("testCacheStandaloneARC");

    for (int i=0; i<1000 ; i++) {
      cacheStandalone.put(new Element("key" + i, "value" + i));
    }

    String agentsFilter = "";
    String cmsFilter = "";
    String cachesFilter = "";

    givenStandalone()
    .expect()
      .contentType(ContentType.JSON)
      .body("entities.size()", is(2))
      .body("entities.find { it.name == 'testCache' }.agentId", equalTo("embedded"))
      .rootPath("entities.find { it.name == 'testCacheStandaloneARC' }")
        .body("agentId", equalTo("embedded"))
        .body("cacheManagerName", equalTo("testCacheManagerStandaloneARC"))
        .body("attributes.DiskExpiryThreadIntervalSeconds", equalTo(120))
        .body("attributes.Status", equalTo("STATUS_ALIVE"))
      .statusCode(200)
    .when()
      .get(EXPECTED_RESOURCE_LOCATION, agentsFilter, cmsFilter, cachesFilter);

    cachesFilter = ";names=testCacheStandaloneARC";
    // we filter to return only the attribute CacheNames, and working only on the testCache2 Cache
    givenStandalone()
      .queryParam("show", "Status")
      .queryParam("show", "Enabled")
    .expect()
      .contentType(ContentType.JSON).rootPath("entities")
      .body("get(0).agentId", equalTo("embedded"))
      .body("get(0).name", equalTo("testCacheStandaloneARC"))
      .body("get(0).cacheManagerName", equalTo("testCacheManagerStandaloneARC"))
      .body("get(0).attributes.Status", equalTo("STATUS_ALIVE"))
      .body("get(0).attributes.Enabled", equalTo(true))
      .body("get(0).attributes.MaxEntriesInCache", nullValue())
      .body("size()",is(1))
      .statusCode(200)
    .when()
      .get(EXPECTED_RESOURCE_LOCATION, agentsFilter, cmsFilter, cachesFilter);

    standaloneCacheManager.removeAllCaches();
    standaloneCacheManager.shutdown();
  }

  @Test
  /**
   * - PUT an updated CacheEntity
   *
   * @throws Exception
   */
  public void updateCachesTest__FailWhenNotSpecifyingACache() throws Exception {
    // you have to specify a cache when doing mutation
    CacheEntityV2 cacheManagerEntity = new CacheEntityV2();
    Map<String,Object> attributes = new HashMap<String, Object>();
    attributes.put("MaxEntriesLocalHeap",20000);
    attributes.put("Enabled", Boolean.FALSE);
    cacheManagerEntity.getAttributes().putAll(attributes);
    String agentsFilter = "";
    String cmsFilter = "";
    String cachesFilter = "";

    givenStandalone()
      .contentType(ContentType.JSON)
      .body(cacheManagerEntity)
    .expect()
      .statusCode(400)
      .body("details", equalTo(""))
      .body("error", equalTo("No cache specified. Unsafe requests must specify a single cache name."))
    .when()
      .put(EXPECTED_RESOURCE_LOCATION, agentsFilter, cmsFilter, cachesFilter);

    cachesFilter = ";names=testCache";
    givenStandalone()
      .contentType(ContentType.JSON)
      .body(cacheManagerEntity)
    .expect()
      .statusCode(400)
      .body("details", equalTo(""))
      .body("error", equalTo("No cache specified. Unsafe requests must specify a single cache name."))
    .when()
      .put(EXPECTED_RESOURCE_LOCATION, agentsFilter, cmsFilter, cachesFilter);

    cmsFilter = ";names=testCacheManager";
    cachesFilter = ";names=boups";
    givenStandalone()
      .contentType(ContentType.JSON)
      .body(cacheManagerEntity)
    .expect()
      .statusCode(400)
      .body("details", equalTo("Cache not found !"))
      .body("error", equalTo("Failed to create or update cache"))
    .when()
      .put(EXPECTED_RESOURCE_LOCATION, agentsFilter, cmsFilter, cachesFilter);

    cmsFilter = ";names=pif";
    cachesFilter = ";names=testCache";
    givenStandalone()
      .contentType(ContentType.JSON)
      .body(cacheManagerEntity)
    .expect()
      .statusCode(500)
    .when()
      .put(EXPECTED_RESOURCE_LOCATION, agentsFilter, cmsFilter, cachesFilter);

    cmsFilter = "";
    cachesFilter = "";
    // we check nothing has changed
    givenStandalone()
    .expect()
      .contentType(ContentType.JSON).rootPath("entities")
      .body("get(0).agentId", equalTo("embedded"))
      .body("get(0).name", equalTo("testCache"))
      .body("get(0).attributes.MaxEntriesLocalHeap",equalTo(10000) )
      .body("get(0).attributes.Enabled", equalTo(Boolean.TRUE))
      .statusCode(200)
    .when()
      .get(EXPECTED_RESOURCE_LOCATION, agentsFilter, cmsFilter, cachesFilter);
  }

  @Test
  /**
   * - PUT an updated CacheEntity
   *
   * Those are the mutable attributes from the rest agent, followed by Gary's comments
   * ENABLED_ATTR: the user can change directly from the management panel
   * LOGGING_ENABLED: just never had this in the DevConsole and nobody's ever asked for it
   * BULK_LOAD_ENABLED: will probably be adding this with the management panel overhaul
   * MAX_ENTRIES_LOCAL_HEAP: we do support this, but not when you've already specified MAX_BYTES_LOCAL_HEAP
   * MAX_ELEMENTS_ON_DISK: same as above, except you've already specified MAX_BYTES_LOCAL_HEAP
   * MAX_ENTRIES_IN_CACHE: if it's a Terracotta-clustered cache, we support this
   * MAX_BYTES_LOCAL_DISK_STRING
   * MAX_BYTES_LOCAL_HEAP_STRING
   * TIME_TO_IDLE_SEC
   * TIME_TO_LIVE_SEC
   *
   * @throws Exception
   */
  public void updateCachesTest() throws Exception {
    // I need a cacheManager not clustered
    CacheManager standaloneCacheManager = createStandaloneCacheManager();

    // you have to specify a cache when doing mutation
    CacheEntityV2 cacheEntity = new CacheEntityV2();
    Map<String,Object> attributes = new HashMap<String, Object>();
    attributes.put("MaxEntriesInCache", 30000);
    attributes.put("MaxEntriesLocalHeap",20000);
    attributes.put("LoggingEnabled", Boolean.TRUE);
    attributes.put("MaxElementsOnDisk",40000);
    attributes.put("TimeToIdleSeconds", 20);
    attributes.put("TimeToLiveSeconds", 43);
    attributes.put("Enabled", Boolean.FALSE);

    String agentsFilter = "";
    String cmsFilter = ";names=testCacheManagerStandalone";
    String cachesFilter = ";names=testCacheStandalone";
    cacheEntity.getAttributes().putAll(attributes);
    givenStandalone()
      .contentType(ContentType.JSON)
      .body(cacheEntity)
    .expect()
      .statusCode(204)
        .log().ifStatusCodeIsEqualTo(400)
    .when()
      .put(EXPECTED_RESOURCE_LOCATION, agentsFilter, cmsFilter, cachesFilter);

    cmsFilter = "";
    // we check the properties were changed
    givenStandalone()
    .expect()
      .contentType(ContentType.JSON).rootPath("entities")
      .body("get(0).agentId", equalTo("embedded"))
      .body("get(0).name", equalTo("testCacheStandalone"))
      .body("get(0).attributes.MaxEntriesInCache", equalTo(30000))
      .body("get(0).attributes.MaxEntriesLocalHeap", equalTo(20000))
      .body("get(0).attributes.LoggingEnabled", equalTo(Boolean.TRUE))
      .body("get(0).attributes.MaxElementsOnDisk", equalTo(40000))
      .body("get(0).attributes.TimeToIdleSeconds", equalTo(20))
      .body("get(0).attributes.TimeToLiveSeconds", equalTo(43))
      .body("get(0).attributes.Enabled", equalTo(Boolean.FALSE))
      .statusCode(200)
    .when()
      .get(EXPECTED_RESOURCE_LOCATION, agentsFilter, cmsFilter, cachesFilter);

    standaloneCacheManager.removeAllCaches();
    standaloneCacheManager.shutdown();
    // I need another cache that does not have set MaxBytesLocalHeap nor MaxBytesLocalDisk
    CacheManager cacheManagerNew = getCacheManagerNew();

    cacheEntity = new CacheEntityV2();
    attributes = new HashMap<String, Object>();
    attributes.put("MaxBytesLocalDiskAsString", "30M");
    attributes.put("MaxBytesLocalHeapAsString","20M");
    cacheEntity.getAttributes().putAll(attributes);

    cmsFilter = ";names=cacheManagerNew";
    cachesFilter = ";names=CacheNew";

    givenStandalone()
      .contentType(ContentType.JSON)
      .body(cacheEntity)
    .expect()
      .log().ifStatusCodeIsEqualTo(400)
      .statusCode(204)
    .when()
      .put(EXPECTED_RESOURCE_LOCATION, agentsFilter, cmsFilter, cachesFilter);

    cmsFilter = "";
    cachesFilter = ";names=CacheNew";
    // we check the properties were changed
    givenStandalone()
    .expect()
      .contentType(ContentType.JSON).rootPath("entities")
      .body("get(0).agentId", equalTo("embedded"))
      .body("get(0).name", equalTo("CacheNew"))
      .body("get(0).attributes.MaxBytesLocalDiskAsString", equalTo("30M"))
      .body("get(0).attributes.MaxBytesLocalHeapAsString", equalTo("20M"))
      .statusCode(200)
    .when()
      .get(EXPECTED_RESOURCE_LOCATION, agentsFilter, cmsFilter, cachesFilter);

    cacheManagerNew.removeAllCaches();
    cacheManagerNew.shutdown();
  }

  @Test
  /**
   * - PUT an updated CacheEntity
   *
   * Those are the mutable attributes from the rest agent, followed by Gary's comments
   * ENABLED_ATTR: the user can change directly from the management panel
   * LOGGING_ENABLED: just never had this in the DevConsole and nobody's ever asked for it
   * BULK_LOAD_ENABLED: will probably be adding this with the management panel overhaul
   * MAX_ENTRIES_LOCAL_HEAP: we do support this, but not when you've already specified MAX_BYTES_LOCAL_HEAP
   * MAX_ELEMENTS_ON_DISK: same as above, except you've already specified MAX_BYTES_LOCAL_HEAP
   * MAX_ENTRIES_IN_CACHE: if it's a Terracotta-clustered cache, we support this
   * MAX_BYTES_LOCAL_DISK_STRING
   * MAX_BYTES_LOCAL_HEAP_STRING
   * TIME_TO_IDLE_SEC
   * TIME_TO_LIVE_SEC
   *
   * @throws Exception
   */
  public void updateCachesTest__clustered() throws Exception {
    CacheManager clusteredCacheManager = createClusteredCacheManager();

    try {
      // you have to specify a cache when doing mutation
      CacheEntityV2 cacheEntity = new CacheEntityV2();
      Map<String, Object> attributes = new HashMap<String, Object>();
      attributes.put("MaxEntriesInCache", 30000);
      attributes.put("MaxEntriesLocalHeap", 20000);
      attributes.put("LoggingEnabled", Boolean.TRUE);
      attributes.put("TimeToIdleSeconds", 20);
      attributes.put("TimeToLiveSeconds", 43);
      attributes.put("NodeBulkLoadEnabled", Boolean.TRUE); //ONLY FOR CLUSTERED !!!
      attributes.put("Enabled", Boolean.FALSE);

      final String agentsFilter = ";ids=" + clusteredCacheManagerAgentId;
      String cmsFilter = ";names=testCacheManagerClustered";
      String cachesFilter = ";names=testCacheClustered";
      cacheEntity.getAttributes().putAll(attributes);
      givenClustered()
        .contentType(ContentType.JSON)
        .body(cacheEntity)
      .expect()
        .statusCode(204)
        .log().ifStatusCodeIsEqualTo(400)
      .when()
        .put(EXPECTED_RESOURCE_LOCATION, agentsFilter, cmsFilter, cachesFilter);

      cmsFilter = "";
      // we check the properties were changed
      givenClustered()
      .expect()
        .contentType(ContentType.JSON).rootPath("entities")
        .body("get(0).agentId", equalTo(clusteredCacheManagerAgentId))
        .body("get(0).name", equalTo("testCacheClustered"))
        .body("get(0).attributes.MaxEntriesInCache", equalTo(30000))
        .body("get(0).attributes.MaxEntriesLocalHeap", equalTo(20000))
        .body("get(0).attributes.LoggingEnabled", equalTo(Boolean.TRUE))
        .body("get(0).attributes.NodeBulkLoadEnabled", equalTo(Boolean.TRUE)) //ONLY FOR CLUSTERED !!!
        .body("get(0).attributes.TimeToIdleSeconds", equalTo(20))
        .body("get(0).attributes.TimeToLiveSeconds", equalTo(43))
        .body("get(0).attributes.Enabled", equalTo(Boolean.FALSE))
        .statusCode(200)
      .when()
        .get(EXPECTED_RESOURCE_LOCATION, agentsFilter, cmsFilter, cachesFilter);
    } finally {
      clusteredCacheManager.shutdown();
    }
  }

  @Test
  /**
   * - PUT an updated CacheEntity, with attributes not allowed
   * only 6 attributes are supported (cf previosu test), the others are forbidden because we do not allow them to be updated
   * @throws Exception
   */
  public void updateCachesTest__FailWhenMutatingForbiddenAttributes() throws Exception {
    CacheEntityV2 cacheManagerEntity = new CacheEntityV2();
    cacheManagerEntity.setName("superName");
    Map<String,Object> attributes = new HashMap<String, Object>();
    attributes.put("LocalOffHeapSizeInBytes","20000");
    attributes.put("Pinned", Boolean.TRUE);
    cacheManagerEntity.getAttributes().putAll(attributes);

    String agentsFilter = "";
    String cmsFilter = ";names=testCacheManager";
    String cachesFilter = ";names=testCache";

    givenStandalone()
      .contentType(ContentType.JSON)
      .body(cacheManagerEntity)
    .expect()
      .statusCode(400)
      .body("details", allOf(containsString("You are not allowed to update those attributes : name LocalOffHeapSizeInBytes Pinned . Only"),
                             containsString("TimeToIdleSeconds"), containsString("Enabled"), containsString("MaxBytesLocalDiskAsString"),
                             containsString("MaxBytesLocalHeapAsString"), containsString("MaxElementsOnDisk"), containsString("TimeToLiveSeconds"),
                             containsString("MaxEntriesLocalHeap"), containsString("LoggingEnabled"), containsString("NodeBulkLoadEnabled"),
                             containsString("MaxEntriesInCache"), Matchers.containsString(" can be updated for a Cache.")))
      .body("error", equalTo("Failed to create or update cache"))
    .when()
      .put(EXPECTED_RESOURCE_LOCATION, agentsFilter, cmsFilter, cachesFilter);

    cmsFilter ="";
    // we check nothing has changed
    givenStandalone()
    .expect()
      .contentType(ContentType.JSON).rootPath("entities")
      .body("get(0).agentId", equalTo("embedded"))
      .body("get(0).name", equalTo("testCache"))
      .body("get(0).attributes.LocalOffHeapSizeInBytes", nullValue())
      .body("get(0).attributes.Pinned", equalTo(Boolean.FALSE))
      .body("size()",is(1))
      .statusCode(200)
    .when()
      .get(EXPECTED_RESOURCE_LOCATION, agentsFilter, cmsFilter, cachesFilter);
  }

  @Test
  /**
   * - PUT an updated CacheEntity
   * @throws Exception
   */
  public void updateCachesTest__CacheManagerDoesNotExist() throws Exception {
    String agentsFilter = "";
    String cmsFilter = ";names=cachemanagerDoesNotExist";
    String cachesFilter = ";names=testCache";

    CacheEntityV2 cacheEntity = new CacheEntityV2();
    givenStandalone()
      .contentType(ContentType.JSON)
      .body(cacheEntity)
    .expect()
      .statusCode(500)
    .when()
      .put(EXPECTED_RESOURCE_LOCATION, agentsFilter, cmsFilter, cachesFilter);
  }

  @Test
  /**
   * - PUT a CacheEntity, not matching any known caches : creating is not allowed
   *
   * @throws Exception
   */
  public void updateCachesTest__CantCreateCache() throws Exception {
    String agentsFilter = "";
    String cmsFilter = ";names=testCacheManager";
    String cachesFilter = ";names=cacheThatDoesNotExist";
    CacheEntityV2 cacheEntity = new CacheEntityV2();
    givenStandalone()
      .contentType(ContentType.JSON)
      .body(cacheEntity)
    .expect()
      .statusCode(400)
      .body("details", equalTo("Cache not found !"))
      .body("error", equalTo("Failed to create or update cache"))
    .when()
      .put(EXPECTED_RESOURCE_LOCATION, agentsFilter, cmsFilter, cachesFilter);
  }

  private CacheManager getCacheManagerNew() {
    Configuration configuration = new Configuration();
    configuration.setName("cacheManagerNew");

    CacheConfiguration myCache = new CacheConfiguration()
            .eternal(false).name("CacheNew");
    myCache.setMaxBytesLocalHeap("5M");
    myCache.setMaxBytesLocalDisk("3M");
    configuration.addCache(myCache);
    ManagementRESTServiceConfiguration managementRESTServiceConfiguration = new ManagementRESTServiceConfiguration();
    managementRESTServiceConfiguration.setBind("0.0.0.0:"+STANDALONE_REST_AGENT_PORT);
    managementRESTServiceConfiguration.setEnabled(true);
    configuration.addManagementRESTService(managementRESTServiceConfiguration);
    CacheManager cacheManager = new CacheManager(configuration);
    Cache exampleCache = cacheManager.getCache("CacheNew");
    assert (exampleCache != null);
    return cacheManager;
  }

  private CacheManager createStandaloneCacheManagerARC() {
    Configuration configuration = new Configuration();
    configuration.setName("testCacheManagerStandaloneARC");
    configuration.setMaxBytesLocalDisk("10M");
    configuration.setMaxBytesLocalHeap("5M");
    CacheConfiguration myCache = new CacheConfiguration().eternal(false).name("testCacheStandaloneARC");
    configuration.addCache(myCache);
    ManagementRESTServiceConfiguration managementRESTServiceConfiguration = new ManagementRESTServiceConfiguration();
    managementRESTServiceConfiguration.setBind("0.0.0.0:"+STANDALONE_REST_AGENT_PORT);
    managementRESTServiceConfiguration.setEnabled(true);
    configuration.addManagementRESTService(managementRESTServiceConfiguration);

    CacheManager mgr = new CacheManager(configuration);
    Cache exampleCache = mgr.getCache("testCacheStandaloneARC");
    assert (exampleCache != null);
    return mgr;
  }

  private CacheManager createStandaloneCacheManager() {
    CacheConfiguration myCache = new CacheConfiguration().eternal(false).name("testCacheStandalone").maxEntriesLocalHeap(10000);
    Configuration configuration = new Configuration().name("testCacheManagerStandalone").cache(myCache);

    ManagementRESTServiceConfiguration managementRESTServiceConfiguration = new ManagementRESTServiceConfiguration();
    managementRESTServiceConfiguration.setBind("0.0.0.0:"+STANDALONE_REST_AGENT_PORT);
    managementRESTServiceConfiguration.setEnabled(true);
    configuration.addManagementRESTService(managementRESTServiceConfiguration);

    CacheManager mgr = new CacheManager(configuration);
    Cache exampleCache = mgr.getCache("testCacheStandalone");
    assert (exampleCache != null);
    return mgr;
  }

  private String clusteredCacheManagerAgentId;

  private CacheManager createClusteredCacheManager() {
    Configuration configuration = new Configuration();
    configuration.setName("testCacheManagerClustered");
    TerracottaClientConfiguration terracottaConfiguration = new TerracottaClientConfiguration().url(CLUSTER_URL);
    configuration.addTerracottaConfig(terracottaConfiguration);
    CacheConfiguration myCache = new CacheConfiguration().eternal(false).name("testCacheClustered").terracotta(new TerracottaConfiguration()).maxEntriesLocalHeap(10000).timeToIdleSeconds(1);
    configuration.addCache(myCache);
    ManagementRESTServiceConfiguration managementRESTServiceConfiguration = new ManagementRESTServiceConfiguration();
    managementRESTServiceConfiguration.setBind("0.0.0.0:"+STANDALONE_REST_AGENT_PORT);
    managementRESTServiceConfiguration.setEnabled(true);
    configuration.addManagementRESTService(managementRESTServiceConfiguration);

    CacheManager mgr = new CacheManager(configuration);
    Cache exampleCache = mgr.getCache("testCacheClustered");
    assert (exampleCache != null);
    clusteredCacheManagerAgentId = waitUntilEhcacheAgentUp(mgr.getClusterUUID());
    return mgr;
  }
}
