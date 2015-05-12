package com.cycos.circuit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cycos.circuit.impl.CircuitConnectorImpl;

public class FitBitConsoleRunner 
{
    private static final Logger LOGGER = LoggerFactory.getLogger(FitBitConsoleRunner.class);
    
    public static void main(String[] args) {
    	System.setProperty("http.proxySet", "true");
    	System.setProperty("http.proxyHost", "proxy.cycos.com");
    	System.setProperty("http.proxyPort", "8080");
    	System.setProperty("http.nonProxyHosts", "lap-tea-2|localhost|172.17.196.191");
    	System.setProperty("https.proxyHost", "proxy.cycos.com");
    	System.setProperty("https.proxyPort", "8080");
    	System.setProperty("https.nonProxyHosts", "lap-tea-2|localhost|172.17.196.191");
    	
    	ConfigHandler config = new ConfigHandler();
    	config.load();
    	CircuitConnector circuit = null;
        try {
            circuit = new CircuitConnectorImpl(config);
        } catch (Exception e) {
            LOGGER.error("Connection to Access server was not possible", e);
            return;
        }
    	FitBitConnector conn = new FitBitConnector(circuit);
    	conn.init();
    	//UserData userData = new UserData("36C6JF", null, null, null);
    	//userData.readAuthFromFile();
    	//conn.addUser(userData);
    	while(true) {
        	conn.fetchUserSteps();
    		try {
				Thread.sleep(1000 * 60);
			} catch (InterruptedException e) {
			    LOGGER.error("Error while waiting", e);
			}
    	}
    }
}
