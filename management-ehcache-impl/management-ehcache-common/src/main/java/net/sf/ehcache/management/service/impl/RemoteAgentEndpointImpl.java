/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package net.sf.ehcache.management.service.impl;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.l1bridge.AbstractRemoteAgentEndpointImpl;
import org.terracotta.management.l1bridge.RemoteCallDescriptor;

public class RemoteAgentEndpointImpl extends AbstractRemoteAgentEndpointImpl implements RemoteAgentEndpointImplMBean {
  private static final Logger LOG = LoggerFactory.getLogger(RemoteAgentEndpointImpl.class);

  public static final String AGENCY = "Ehcache";
  public static final String MBEAN_NAME_PREFIX = "net.sf.ehcache:type=" + IDENTIFIER;
  
  private final Map<String, ObjectName> uuidObjectNameMap = new HashMap<String, ObjectName>();
  
  private final ThreadLocal<Boolean> tsaBridged = new ThreadLocal<Boolean>() {
    @Override
    protected Boolean initialValue() {
      return false;
    }
  };

  public RemoteAgentEndpointImpl() {
  }

  protected boolean isTsaSecured() {
    return false;
  }

  public boolean isTsaBridged() {
    return tsaBridged.get();
  }

  public void registerMBean(String clientUUID) {
    if (clientUUID == null) {
      throw new NullPointerException("clientUUID cannot be null");
    }
    ObjectName objectName;
    try {
      objectName = new ObjectName(MBEAN_NAME_PREFIX + ",node=" + clientUUID);
      MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
      platformMBeanServer.registerMBean(this, objectName);
      uuidObjectNameMap.put(clientUUID, objectName);
    } catch (InstanceAlreadyExistsException iaee) {
      // the MBean has already been registered, ignore it
      objectName = null;
    } catch (Exception e) {
      LOG.warn("Error registering RemoteAgentEndpointImpl MBean with UUID: " + clientUUID, e);
      objectName = null;
    }
  }

  public void unregisterMBean(String clientUUID) {
    ObjectName objectName = uuidObjectNameMap.get(clientUUID);
    
    try {
      MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
      platformMBeanServer.unregisterMBean(objectName);
      uuidObjectNameMap.remove(clientUUID);
    } catch (Exception e) {
      LOG.warn("Error unregistering RemoteAgentEndpointImpl MBean : " + objectName, e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public byte[] invoke(RemoteCallDescriptor remoteCallDescriptor) throws Exception {
    try {
      tsaBridged.set(true);
      return super.invoke(remoteCallDescriptor);
    } finally {
      tsaBridged.set(false);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getVersion() {
    return this.getClass().getPackage().getImplementationVersion();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getAgency() {
    return AGENCY;
  }

}
