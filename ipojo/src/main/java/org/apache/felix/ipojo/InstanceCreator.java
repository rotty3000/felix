/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.ipojo;

import java.util.Dictionary;

import org.apache.felix.ipojo.util.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

/**
 * An instance creator aims to create instances and to track their factories. 
 * It's allow to create instance from outside factories.
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class InstanceCreator implements ServiceListener {
	
	private BundleContext m_context;
	
	private Logger m_logger;
	
	/**
	 * This structure aims to manage a configuration.
	 * It stores all necessary information to create an instance and to track the factory.
	 */
	private class ManagedConfiguration {
		Dictionary configuration;
		String factoryName;
		ComponentInstance instance;
		
		public ManagedConfiguration(Dictionary conf) { configuration = conf; }
	}
	
	
	/**
	 * Configurations to create and maintains.
	 */
	private ManagedConfiguration[] m_configurations;
	
	public InstanceCreator(BundleContext context, Dictionary[] configurations) {
		m_context = context;
		m_logger = new Logger(context, "InstanceCreator"+context.getBundle().getBundleId(), Logger.WARNING);
		m_configurations = new ManagedConfiguration[configurations.length];
		for(int i = 0; i < configurations.length; i++) {
			ManagedConfiguration conf = new ManagedConfiguration(configurations[i]);
			m_configurations[i] = conf;
			
			// Get the component type name :
			String componentType = (String) conf.configuration.get("component");
			Factory fact = null;
			
			try {
				String fil = "(|(" + org.osgi.framework.Constants.SERVICE_PID + "=" + componentType + ")(component.class=" + componentType + "))";
				ServiceReference[] refs = context.getServiceReferences(org.apache.felix.ipojo.Factory.class.getName(), fil);
				if(refs != null) { 
					fact = (Factory) m_context.getService(refs[0]);
					createInstance(fact, conf);					
				}
				else {
					m_logger.log(Logger.WARNING, "No factory available for the type : " + componentType);
				}
			} catch (InvalidSyntaxException e) { m_logger.log(Logger.ERROR, "Invalid syntax filter for the type : " + componentType, e); }
		}
		
		// Register a service listenner on Factory Service
		try {
			m_context.addServiceListener(this, "(objectClass="+Factory.class.getName() + ")");
		} catch (InvalidSyntaxException e) { m_logger.log(Logger.ERROR, "Invalid syntax filter when registering a listener on Factory Service", e); }
	}
	
	private void createInstance(Factory fact, ManagedConfiguration config) {
		Dictionary conf = config.configuration;
		try {
				config.instance = fact.createComponentInstance(conf);
				config.factoryName = fact.getName();
		} catch (UnacceptableConfiguration e) {
			m_logger.log(Logger.ERROR, "A factory is available for the configuration but the configuration is not acceptable", e);
		}
	}

	public void serviceChanged(ServiceEvent ev) {
		ServiceReference ref = ev.getServiceReference();
		String factoryName = (String) ref.getProperty(org.osgi.framework.Constants.SERVICE_PID);
		String componentClass = (String) ref.getProperty("component.class");
		
		if(ev.getType() == ServiceEvent.REGISTERED) { //A new factory appears
			for(int i = 0; i < m_configurations.length; i++) {
				if(m_configurations[i].instance == null && (m_configurations[i].configuration.get("component").equals(factoryName) ||  m_configurations[i].configuration.get("component").equals(componentClass))) {
					Factory fact = (Factory) m_context.getService(ref);
					createInstance(fact, m_configurations[i]);
				}
			}
			return;
		}
		
		if(ev.getType() == ServiceEvent.UNREGISTERING) {
			for(int i = 0; i < m_configurations.length; i++) {
				if(m_configurations[i].instance != null && m_configurations[i].factoryName.equals(factoryName)) {
					m_configurations[i].instance = null;
					m_configurations[i].factoryName = null;
					m_context.ungetService(ref);
				}
			}
			return;
		}
		
		//TODO manage modification ? normally a factory should not change its property.
	}
	
	/**
	 * Stop all created instances
	 */
	public void stop() {
		for(int i = 0; i < m_configurations.length; i++) {
			if(m_configurations[i].instance != null) { m_configurations[i].instance.stop(); }
			m_configurations[i].instance = null;
		}
		m_configurations = null;
	}

}
