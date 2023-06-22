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
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * The aim of this test is to check via HTTP that the ehcache standalone agent /tc-management-api/agents/cacheManagers/caches/elements endpoint
 * works fine
 *
 * @author Anthony Dahanne
 */
public class ElementsResourceServiceImplTest extends ResourceServiceImplITHelper {
  protected static final String EXPECTED_RESOURCE_LOCATION = "/tc-management-api/v2/agents{agentIds}/cacheManagers{cmIds}/caches{cacheIds}/elements";

  @BeforeClass
  public static void setUpCluster() throws Exception {
    setUpCluster(ElementsResourceServiceImplTest.class);
  }

  @Test
  /**
   * - DELETE all elements from a Cache
   *
   * @throws Exception
   */
  public void deleteElementsTest__notSpecifyingCacheOrCacheManager() throws Exception {
    String agentsFilter = "";
    String cmsFilter = "";
    String cachesFilter = "";
    givenStandalone()
      .contentType(ContentType.JSON)
    .expect()
      .statusCode(400)
      .body("details", equalTo(""))
      .body("error", equalTo("No cache specified. Unsafe requests must specify a single cache name."))
    .when()
      .delete(EXPECTED_RESOURCE_LOCATION, agentsFilter, cmsFilter, cachesFilter);

    cachesFilter = ";names=testCache";
    givenStandalone()
      .contentType(ContentType.JSON)
    .expect()
      .statusCode(400)
      .body("details", equalTo(""))
      .body("error", equalTo("No cache specified. Unsafe requests must specify a single cache name."))
    .when()
      .delete(EXPECTED_RESOURCE_LOCATION, agentsFilter, cmsFilter, cachesFilter);
  }

  @Test
  /**
   * - DELETE all elements from a Cache
   *
   * @throws Exception
   */
  public void deleteElementsTest() throws Exception {
    Cache exampleCache = cacheManagerMaxElements.getCache("testCache");
    for (int i=0; i<1000 ; i++) {
      exampleCache.put(new Element("key" + i, "value" + i));
    }

    assertThat(exampleCache.getSize(), is(1000));

    String agentsFilter = "";
    String cachesFilter = ";names=testCache";
    String cmsFilter = ";names=testCacheManager";
    givenStandalone()
      .contentType(ContentType.JSON)
    .expect()
      .statusCode(204)
    .when()
      .delete(EXPECTED_RESOURCE_LOCATION, agentsFilter, cmsFilter, cachesFilter);

    assertThat(exampleCache.getSize(), is(0));
  }

  @Test
  /**
   * - DELETE all elements from a Cache
   *
   * @throws Exception
   */
  public void deleteElementsTest__clustered() throws Exception {
    Cache exampleCache = cacheManagerMaxElements.getCache("testCache");
    for (int i=0; i<1000 ; i++) {
      exampleCache.put(new Element("key" + i, "value" + i));
    }
    final String agentsFilter = ";ids=" + cacheManagerMaxElementsAgentId;

    assertThat(exampleCache.getSize(), is(1000));

    String cachesFilter = ";names=testCache";
    String cmsFilter = ";names=testCacheManager";
    givenClustered()
      .contentType(ContentType.JSON)
    .expect()
      .statusCode(204)
    .when()
      .delete(EXPECTED_RESOURCE_LOCATION, agentsFilter, cmsFilter, cachesFilter);

    assertThat(exampleCache.getSize(), is(0));
  }
}
