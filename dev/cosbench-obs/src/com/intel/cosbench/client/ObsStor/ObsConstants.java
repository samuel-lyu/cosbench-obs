package com.intel.cosbench.client.ObsStor;

public interface ObsConstants {
	// --------------------------------------------------------------------------
    // CONNECTION
    // --------------------------------------------------------------------------

    String CONN_TIMEOUT_KEY = "timeout";
    int CONN_TIMEOUT_DEFAULT = 30000;
    // --------------------------------------------------------------------------
    // ENDPOINT
    // --------------------------------------------------------------------------
    String ENDPOINT_KEY = "endpoint";
    String ENDPOINT_DEFAULT = "http://obs.cn-north-1.myhwclouds.com";

    // --------------------------------------------------------------------------
    // AUTHENTICATION
    // --------------------------------------------------------------------------

    String AUTH_USERNAME_KEY = "accesskey";
    String AUTH_USERNAME_DEFAULT = "";
    
    String AUTH_PASSWORD_KEY = "secretkey";
    String AUTH_PASSWORD_DEFAULT = "";
    
    // --------------------------------------------------------------------------
    // CLIENT CONFIGURATION
    // --------------------------------------------------------------------------
    String PROXY_HOST_KEY = "proxyhost";
    String PROXY_PORT_KEY = "proxyport";    
    
    // --------------------------------------------------------------------------
    // PATH STYLE ACCESS
    // --------------------------------------------------------------------------
    String PATH_STYLE_ACCESS_KEY = "path_style_access";
    boolean PATH_STYLE_ACCESS_DEFAULT = false;

    // --------------------------------------------------------------------------
    // CONTEXT NEEDS FROM AUTH MODULE
    // --------------------------------------------------------------------------
    String ObsCLIENT_KEY = "Obsclient";

}
