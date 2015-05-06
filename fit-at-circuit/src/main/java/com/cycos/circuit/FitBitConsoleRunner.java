package com.cycos.circuit;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;

import com.cycos.circuit.impl.CircuitConnectorImpl;
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
    	CircuitConnector circuit = new CircuitConnectorImpl();
    	FitBitConnector conn = new FitBitConnector(circuit);
    	conn.init();
    	FitbitUserData userData = new FitbitUserData("36C6JF");
    	conn.addUser(userData);
    	int i = 0; 
    	while(i < 10) {
    		//conn.fetchUserActivities();
        	conn.fetchUserSteps();
        	i++;
    		try {
				Thread.sleep(1000 * 60);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
    	}
    }
}
