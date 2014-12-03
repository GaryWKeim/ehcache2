package org.terracotta.modules.ehcache;

import net.sf.ehcache.config.CacheConfiguration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.internal.cache.BufferingToolkitCache;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Eugene Shelestovich
 */
public class WanAwareToolkitCacheTest {

  private static final String                  VALUE = "v1";
  private static final String                  KEY   = "k1";
  private BufferingToolkitCache<String, String> delegate;
  private ConcurrentMap<String, Serializable>  configMap;
  private WanAwareToolkitCache<String, String> wanAwareCache;
  private WanAwareToolkitCache<String, String> masterWanAwareCache;
  private WanAwareToolkitCache<String, String> replicaWanAwareCache;
  private WanAwareToolkitCache<String, String> unidirectionalReplicaWanAwareCache;
  private ToolkitLock activeLock;
  private boolean                              waitHappened;

  @Before
  public void setUp() {
    this.waitHappened = false;
    delegate = mock(BufferingToolkitCache.class);
    activeLock = mock(ToolkitLock.class);
    when(activeLock.isHeldByCurrentThread()).thenReturn(false, true);
    configMap = new ConcurrentHashMap<String, Serializable>();

    masterWanAwareCache = getTestableWanAwareToolkitCache(true, true);
    replicaWanAwareCache = getTestableWanAwareToolkitCache(false, true);
    unidirectionalReplicaWanAwareCache = getTestableWanAwareToolkitCache(false, false);

    // by default we perform tests on master cache
    wanAwareCache = masterWanAwareCache;
  }

  private WanAwareToolkitCache<String, String> getTestableWanAwareToolkitCache(boolean isMasterCache, final boolean bidirectional) {
    return new WanAwareToolkitCache<String, String>(delegate, configMap, null, null, activeLock,
        new CacheConfiguration(), isMasterCache, bidirectional) {
      @Override
      void waitUntilActive() {
        waitHappened = true;
      }

      @Override
      void notifyClients() {
        // Do Nothing
      }
    };
  }


  @Test
  public void testCacheMustBeInactiveByDefault() {
    // After setup, a new wan-aware cache is inactive by default
    Assert.assertFalse(wanAwareCache.isReady());
  }

  @Test
  public void testClientShouldNotWaitWhenCacheActive() {
    whenCacheIsActive().andClientPerformsPutOperation().assertWaitHappened(false).andAssertPutCallDelegatedToCache();
  }

  @Test
  public void testClientShouldWaitWhenCacheIsNotActive() {
    whenCacheIsNotActive().andClientPerformsPutOperation().assertWaitHappened(true).andAssertPutCallDelegatedToCache();
  }

  @Test
  public void testOrchestratorShouldNotWaitWhenCacheInactive() {
    whenCacheIsNotActive().andOrchestratorPerformsOperation().assertWaitHappened(false);
  }

  @Test
  public void testClientShouldNotWaitWhenMasterCacheOrchestratorIsDown() {
    whenCacheIsActive().whenOrchestratorIsDead().andClientPerformsPutOperation().assertWaitHappened(false)
        .andAssertPutCallDelegatedToCache();
  }

  @Test
  public void testClientShouldWaitWhenReplicaCacheOrchestratorIsDown() {
    forReplicaCache().whenCacheIsActive().whenOrchestratorIsDead().andClientPerformsPutOperation()
        .assertWaitHappened(true).andAssertPutCallDelegatedToCache();
  }

  @Test
  public void testClientShouldNotWaitWhenUnidirectionalReplicaCacheOrchestratorIsDown() {
    forUnidirectionalReplicaCache().whenCacheIsActive().whenOrchestratorIsDead().andClientPerformsPutOperation()
        .assertWaitHappened(false).andAssertPutCallDelegatedToCache();
  }

  private WanAwareToolkitCacheTest forReplicaCache() {
    wanAwareCache = replicaWanAwareCache;
    return this;
  }

  private WanAwareToolkitCacheTest forUnidirectionalReplicaCache() {
    wanAwareCache = unidirectionalReplicaWanAwareCache;
    return this;
  }

  private WanAwareToolkitCacheTest andOrchestratorPerformsOperation() {
    wanAwareCache.putVersioned(KEY, VALUE, 0);
    wanAwareCache.removeVersioned(KEY, 0);
    wanAwareCache.clearVersioned();
    return this;
  }

  private void andAssertPutCallDelegatedToCache() {
    verify(delegate).put(KEY, VALUE);
  }

  private WanAwareToolkitCacheTest assertWaitHappened(boolean expected) {
    Assert.assertEquals(expected, waitHappened);
    return this;
  }

  private WanAwareToolkitCacheTest andClientPerformsPutOperation() {
    wanAwareCache.put(KEY, VALUE);
    return this;
  }

  private WanAwareToolkitCacheTest whenCacheIsActive() {
    wanAwareCache.activate();
    wanAwareCache.goLive();
    return this;
  }

  private WanAwareToolkitCacheTest whenCacheIsNotActive() {
    wanAwareCache.deactivate();
    return this;
  }

  private WanAwareToolkitCacheTest whenOrchestratorIsDead() {
    wanAwareCache.markOrchestratorDead();
    return this;
  }

}
