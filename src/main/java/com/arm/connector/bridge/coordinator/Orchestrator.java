/**
 * @file    orchestrator.java
 * @brief   orchestrator for the connector bridge
 * @author  Doug Anson
 * @version 1.0
 * @see
 *
 * Copyright 2015. ARM Ltd. All rights reserved.
 *
 * Use of this software is restricted by the terms of the license under which this software has been
 * distributed (the "License"). Any use outside the express terms of the License, including wholly
 * unlicensed use, is prohibited. You may not use this software unless you have been expressly granted
 * the right to use it under the License.
 * 
 */

package com.arm.connector.bridge.coordinator;

import com.arm.connector.bridge.console.ConsoleManager;
import com.arm.connector.bridge.coordinator.processors.ibm.ibmPeerProcessorManager;
import com.arm.connector.bridge.coordinator.processors.interfaces.MDSInterface;
import com.arm.connector.bridge.coordinator.processors.arm.MDSProcessor;
import com.arm.connector.bridge.coordinator.processors.interfaces.PeerInterface;
import com.arm.connector.bridge.coordinator.processors.sample.Sample3rdPartyProcessor;
import com.arm.connector.bridge.core.ErrorLogger;
import com.arm.connector.bridge.preferences.PreferenceManager;
import com.arm.connector.bridge.transport.HttpTransport;
import com.codesnippets4all.json.generators.JSONGenerator;
import com.codesnippets4all.json.generators.JsonGeneratorFactory;
import com.codesnippets4all.json.parsers.JSONParser;
import java.util.ArrayList;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This the primary orchestrator for the connector bridge
 * @author Doug Anson
 */
public class Orchestrator implements MDSInterface, PeerInterface {
    private HttpServlet              m_servlet = null;
    
    private ErrorLogger              m_error_logger = null;
    private PreferenceManager        m_preference_manager = null;
    
    // mDS processor
    private MDSInterface             m_mds_rest_processor = null;
    
    // Peer processor list
    private ArrayList<PeerInterface> m_peer_processor_list = null;
    
    private ConsoleManager           m_console_manager = null;

    private HttpTransport            m_http = null;
    
    private JsonGeneratorFactory     m_json_factory = null;
    private JSONGenerator            m_json_generator = null;
    private JSONParser               m_json_parser = null;
    private boolean                  m_listeners_initialized = false;
    
    private final String             m_mds_non_domain = null;
    private String                   m_mds_domain = null;
    
    public Orchestrator(ErrorLogger error_logger,PreferenceManager preference_manager, String domain) {
        // save the error handler
        this.m_error_logger = error_logger;
        this.m_preference_manager = preference_manager;
                      
        // MDS domain is required 
        if (domain != null && domain.equalsIgnoreCase(this.preferences().valueOf("mds_def_domain")) == false)
            this.m_mds_domain = domain;
          
        // create the JSON Generator
        this.m_json_factory = JsonGeneratorFactory.getInstance();
        this.m_json_generator = this.m_json_factory.newJsonGenerator();

        // create the JSON Parser
        this.m_json_parser = new JSONParser();
        
        // build out the HTTP transport
        this.m_http = new HttpTransport(this.m_error_logger,this.m_preference_manager);
                        
        // REQUIRED: We always create the mDS REST processor
        this.m_mds_rest_processor = new MDSProcessor(this,this.m_http);
        
        // initialize our peer processor list
        this.initPeerProcessorList();
                
        // create the console manager
        this.m_console_manager = new ConsoleManager(this);
    }
    
    // initialize our peer processor
    private void initPeerProcessorList() {
        // initialize the list
        this.m_peer_processor_list = new ArrayList<>();
        
        // add peer processors
        if (this.ibmPeerEnabled()) {
            // IBM/MQTT: create the MQTT processor manager
            this.errorLogger().info("Orchestrator: adding IBM IoTF/StarterKit/MQTT Processor");
            this.m_peer_processor_list.add(ibmPeerProcessorManager.createPeerProcessor(this,this.m_http));
        }
        if (this.samplePeerEnabled()) {
            // Microsoft: create IoTEventHub processor
            this.errorLogger().info("Orchestrator: adding 3rd Party Sample REST Processor");
            this.m_peer_processor_list.add(Sample3rdPartyProcessor.createPeerProcessor(this,this.m_http));
        }
    }
    
    // use IBM peer processor?
    private Boolean ibmPeerEnabled() {
        return (this.preferences().booleanValueOf("enable_iotf_addon") || this.preferences().booleanValueOf("enable_starterkit_addon"));
    }
    
    // use sample 3rd Party peer processor?
    private Boolean samplePeerEnabled() {
        return this.preferences().booleanValueOf("enable_3rd_party_rest_processor");
    }
    
    // are the listeners active?
    public boolean peerListenerActive() { 
        return this.m_listeners_initialized; 
    }
    
    // initialize peer listener
    public void initPeerListener() {
        if (!this.m_listeners_initialized) {
            // MQTT Listener
            for(int i=0;i<this.m_peer_processor_list.size();++i) {
                this.m_peer_processor_list.get(i).initListener();
            }
            this.m_listeners_initialized = true;
        }
    }
    
    // stop the peer listener
    public void stopPeerListener() {
        if (this.m_listeners_initialized) {
            // MQTT Listener
            for(int i=0;i<this.m_peer_processor_list.size();++i) {
                this.m_peer_processor_list.get(i).stopListener();
            }
            this.m_listeners_initialized = false;
        }
    }
    
    // initialize the mDS webhook
    public void initMDSWebhook() {
        if (this.m_mds_rest_processor != null) {
            this.m_mds_rest_processor.setNotificationCallbackURL();
        }
    }
    
    // reset mDS webhook
    public void resetMDSWebhook() {
        // REST (mDS)
        if (this.m_mds_rest_processor != null) {
            this.m_mds_rest_processor.resetNotificationCallbackURL();
        }
    }
    
    // process the mDS notification
    public void processNotification(HttpServletRequest request, HttpServletResponse response) {
        // process the received REST message
        //this.errorLogger().info("events (REST-" + request.getMethod() + "): " + request.getRequestURI());
        this.mds_rest_processor().processMDSMessage(request, response);
    }
    
    // process the Console request
    public void processConsole(HttpServletRequest request, HttpServletResponse response) {
        // process the received REST message
        //this.errorLogger().info("console (REST-" + request.getMethod() + "): " + request.getServletPath());
        this.console_manager().processConsole(request, response);
    }
    
    // set the HttpServlet
    private void setServlet(HttpServlet servlet) {
        this.m_servlet = servlet;
    }

    // get the HttpServlet
    public HttpServlet getServlet() {
        return this.m_servlet;
    }
    
    // get the ErrorLogger
    public ErrorLogger errorLogger() {
        return this.m_error_logger;
    }
    
    // get he Preferences manager
    public final PreferenceManager preferences() { 
        return this.m_preference_manager; 
    }
    
    // get the Peer processor
    public ArrayList<PeerInterface> peer_processor_list() {
        return this.m_peer_processor_list;
    }
    
    // get the mDS processor
    public MDSInterface mds_rest_processor() {
        return this.m_mds_rest_processor;
    }
    
    // get the console manager
    public ConsoleManager console_manager() {
        return this.m_console_manager;
    }
    
    // get our mDS domain
    public String getDomain() {
        return this.m_mds_domain;
    }
    
    // get the JSON parser instance
    public JSONParser getJSONParser() {
        return this.m_json_parser;
    }
    
    // get the JSON generation instance
    public JSONGenerator getJSONGenerator() {
        return this.m_json_generator;
    }
   
    // get our ith peer processor
    private PeerInterface peerProcessor(int index) {
        if (index >= 0 && this.m_peer_processor_list != null && index < this.m_peer_processor_list.size()) {
            return this.m_peer_processor_list.get(index); 
        }
        return null;
    }
    
    // MDSInterface Orchestration
    @Override
    public void processMDSMessage(HttpServletRequest request, HttpServletResponse response) {
        this.mds_rest_processor().processMDSMessage(request, response);
    }

    @Override
    public void processDeregistrations(String[] deregistrations) {
        this.mds_rest_processor().processDeregistrations(deregistrations);
    }

    @Override
    public String subscribeToEndpointResource(String uri, Map options, Boolean init_webhook) {
        return this.mds_rest_processor().subscribeToEndpointResource(uri, options, init_webhook);
    }

    @Override
    public String subscribeToEndpointResource(String ep_name, String uri, Boolean init_webhook) {
        return this.mds_rest_processor().subscribeToEndpointResource(ep_name, uri, init_webhook);
    }

    @Override
    public String unsubscribeFromEndpointResource(String uri, Map options) {
        return this.mds_rest_processor().unsubscribeFromEndpointResource(uri, options);
    }

    @Override
    public String performDeviceDiscovery(Map options) {
        return this.mds_rest_processor().performDeviceDiscovery(options);
    }

    @Override
    public String performDeviceResourceDiscovery(String uri) {
        return this.mds_rest_processor().performDeviceResourceDiscovery(uri);
    }

    @Override
    public String processEndpointResourceOperation(String verb, String uri, Map options) {
        return this.mds_rest_processor().processEndpointResourceOperation(verb, uri, options);
    }

    @Override
    public String processEndpointResourceOperation(String verb, String ep_name, String uri) {
        return this.mds_rest_processor().processEndpointResourceOperation(verb, ep_name, uri);
    }

    @Override
    public String processEndpointResourceOperation(String verb, String ep_name, String uri, String value) {
        return this.mds_rest_processor().processEndpointResourceOperation(verb, ep_name, uri, value);
    }

    @Override
    public void setNotificationCallbackURL() {
        this.mds_rest_processor().setNotificationCallbackURL();
    }

    @Override
    public void resetNotificationCallbackURL() {
        this.mds_rest_processor().resetNotificationCallbackURL();
    }
    
    @Override
    public void pullDeviceMetadata(Map endpoint) {
        this.mds_rest_processor().pullDeviceMetadata(endpoint);
    }

    // PeerInterface Orchestration
    @Override
    public String createAuthenticationHash() {
        String hash = "";
        for(int i=0;this.m_peer_processor_list != null && i<this.m_peer_processor_list.size();++i) {
            hash += this.peerProcessor(i).createAuthenticationHash();
        }
        return hash;
    }

    @Override
    public void processNewRegistration(Map message) {
        for(int i=0;this.m_peer_processor_list != null && i<this.m_peer_processor_list.size();++i) {
            this.peerProcessor(i).processNewRegistration(message);
        }
    }

    @Override
    public void processReRegistration(Map message) {
        for(int i=0;this.m_peer_processor_list != null && i<this.m_peer_processor_list.size();++i) {
            this.peerProcessor(i).processReRegistration(message);
        }
    }

    @Override
    public String[] processDeregistrations(Map message) {
        ArrayList<String> deregistrations = new ArrayList<>();
        for(int i=0;this.m_peer_processor_list != null && i<this.m_peer_processor_list.size();++i) {
            String[] ith_deregistrations = this.peerProcessor(i).processDeregistrations(message);
            for(int j=0;ith_deregistrations != null && j<ith_deregistrations.length;++j) {
                boolean add = deregistrations.add(ith_deregistrations[j]);
            }
        }
        String[] dereg_str_array = new String[deregistrations.size()];
        return deregistrations.toArray(dereg_str_array); 
    }

    @Override
    public void processRegistrationsExpired(Map message) {
        for(int i=0;this.m_peer_processor_list != null && i<this.m_peer_processor_list.size();++i) {
            this.peerProcessor(i).processRegistrationsExpired(message);
        }
    }

    @Override
    public void processAsyncResponses(Map message) {
        for(int i=0;this.m_peer_processor_list != null && i<this.m_peer_processor_list.size();++i) {
            this.peerProcessor(i).processAsyncResponses(message);
        }
    }

    @Override
    public void processNotification(Map message) {
        for(int i=0;this.m_peer_processor_list != null && i<this.m_peer_processor_list.size();++i) {
            this.peerProcessor(i).processNotification(message);
        }
    }

    @Override
    public void initListener() {
        for(int i=0;this.m_peer_processor_list != null && i<this.m_peer_processor_list.size();++i) {
            this.peerProcessor(i).initListener();
        }
    }

    @Override
    public void stopListener() {
        for(int i=0;this.m_peer_processor_list != null && i<this.m_peer_processor_list.size();++i) {
            this.peerProcessor(i).stopListener();
        }
    }
}
