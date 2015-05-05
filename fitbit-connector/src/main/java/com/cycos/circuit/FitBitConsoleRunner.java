package com.cycos.circuit;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;

import com.fitbit.api.client.FitbitAPIEntityCache;
import com.fitbit.api.client.FitbitApiClientAgent;
import com.fitbit.api.client.FitbitApiCredentialsCache;
import com.fitbit.api.client.FitbitApiCredentialsCacheMapImpl;
import com.fitbit.api.client.FitbitApiEntityCacheMapImpl;
import com.fitbit.api.client.FitbitApiSubscriptionStorage;
import com.fitbit.api.client.FitbitApiSubscriptionStorageInMemoryImpl;
import com.fitbit.api.client.service.FitbitAPIClientService;

public class FitBitConsoleRunner 
{
    public static void main(String[] args) {
    	System.setProperty("http.proxySet", "true");
    	System.setProperty("http.proxyHost", "proxy.cycos.com");
    	System.setProperty("http.proxyPort", "8080");
    	System.setProperty("https.proxyHost", "proxy.cycos.com");
    	System.setProperty("https.proxyPort", "8080");
    	FitBitConnector conn = new FitBitConnector();
    	conn.init();
    	conn.fetchUserActivities();
    }
}
