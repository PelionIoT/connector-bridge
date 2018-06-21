/**
 * @file    ApiResponse.java
 * @brief mbed Device Server API Response
 * @author Doug Anson
 * @version 1.0
 * @see
 *
 * Copyright 2015. ARM Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.arm.connector.bridge.coordinator.processors.core;

import com.arm.connector.bridge.coordinator.Orchestrator;
import java.util.HashMap;
import java.util.Map;

/**
 * API Response
 * @author Doug Anson
 */
public class ApiResponse extends Processor {
    private int m_request_id;
    private String m_request_uri;
    private String m_request_data;
    private String m_request_options;
    private String m_request_verb;
    private int m_response_http_code;
    private String m_response_data;
    
    // default constructor
    public ApiResponse(Orchestrator orchestrator,String suffix,String uri,String data,String options,String verb,int request_id) {
        super(orchestrator,suffix);
        this.m_request_id = request_id;
        this.m_request_uri = uri;
        this.m_request_data = data;
        this.m_request_options = options;
        this.m_request_verb = verb;
        this.m_response_http_code = 900;
        this.m_response_data = "";
    }
    
    // set the response data
    public void setReplyData(String response_data) {
        this.m_response_data = response_data;
    }
    
    // set the HTTP response code
    public void setHttpCode(int http_code) {
        this.m_response_http_code = http_code;
    }
    
    // get the HTTP response code
    public int getHttpCode() {
        return this.m_response_http_code;
    }
    
    // get the response data
    public String getReplyData() {
        return this.m_response_data;
    }
    
    // get the request URI
    public String getRequestURI() {
        return this.m_request_uri;
    }
    
    // get the request data
    public String getRequestData() {
        return this.m_request_data;
    }
    
    // get the request options
    public String getRequestOptions() {
        return this.m_request_options;
    }
    
    // get the request http verb
    public String getRequestVerb() {
        return this.m_request_verb;
    }
    
    // create the response JSON
    public String createResponseJSON() {
        HashMap<String,Object> json = new HashMap<>();
        if (this.m_request_uri != null && this.m_request_uri.length() > 0) {
            json.put("api_uri",this.m_request_uri);
        }
        else {
            json.put("api_uri","none");
        }
        if (this.m_request_options != null && this.m_request_options.length() > 0) {
            json.put("api_options",this.m_request_options);
        }
        else {
            json.put("api_options","none");
        }
        if (this.m_request_data != null && this.m_request_data.length() > 0) {
            json.put("api_request_data",this.m_request_data);
        }
        else {
            json.put("api_request_data","none");
        }
        if (this.m_request_verb != null && this.m_request_verb.length() > 0) {
            json.put("api_verb",this.m_request_verb);
        }
        else {
            json.put("api_verb","none");
        }
        if (this.m_response_data != null && this.m_response_data.length() > 0) {
            json.put("api_response",this.parseResponseData(this.m_response_data));
        }
        else {
            json.put("api_response","none");
        }
        json.put("api_http_code",this.m_response_http_code);
        json.put("api_request_id",this.m_request_id);
        return this.jsonGenerator().generateJson(json);
    }
    
    // create the parsed JSON response as a Map
    private Map parseResponseData(String data) {
        // try direct parse of input data first
        Map parsed = this.tryJSONParse(data);
        if (parsed == null || parsed.isEmpty() == true) {
            // create a JSON with the input data and try that...
            HashMap<String,String> d = new HashMap<>();
            if (data == null || data.length() <= 0) {
                data = "none";
            }
            d.put("payload", data);
            parsed = d;
        }
        return parsed;
    }
}
