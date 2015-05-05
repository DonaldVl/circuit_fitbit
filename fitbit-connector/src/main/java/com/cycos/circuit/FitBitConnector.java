package com.cycos.circuit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Properties;

import com.fitbit.api.FitbitAPIException;
import com.fitbit.api.client.FitbitAPIEntityCache;
import com.fitbit.api.client.FitbitApiClientAgent;
import com.fitbit.api.client.FitbitApiCredentialsCache;
import com.fitbit.api.client.FitbitApiCredentialsCacheMapImpl;
import com.fitbit.api.client.FitbitApiEntityCacheMapImpl;
import com.fitbit.api.client.FitbitApiSubscriptionStorage;
import com.fitbit.api.client.FitbitApiSubscriptionStorageInMemoryImpl;
import com.fitbit.api.client.LocalUserDetail;
import com.fitbit.api.client.service.FitbitAPIClientService;
import com.fitbit.api.common.model.user.UserInfo;
import com.fitbit.api.model.APIResourceCredentials;

public class FitBitConnector {
	private FitbitAPIEntityCache entityCache = new FitbitApiEntityCacheMapImpl();
	private FitbitApiCredentialsCache credentialsCache = new FitbitApiCredentialsCacheMapImpl();
	private FitbitApiSubscriptionStorage subscriptionStore = new FitbitApiSubscriptionStorageInMemoryImpl();
	private FitbitAPIClientService<FitbitApiClientAgent> apiClientService = null;
	private Properties properties;
	private String apiBaseUrl = null;
	private String fitbitSiteBaseUrl = null;
	private String exampleBaseUrl = null;
	private String clientConsumerKey = null;
	private String clientSecret = null;
	private String pin = null;
	private String accessToken = null;
	private String accessTokenSecret = null;
	
	public FitBitConnector() {
		try {
			properties = new Properties();
			properties.load(getClass().getClassLoader().getResourceAsStream("config.properties"));
		} catch (IOException e) {
	        System.out.println("Error reading properties file");
	    }
	}
	
	public void init() {
        apiBaseUrl = properties.getProperty("apiBaseUrl");
        fitbitSiteBaseUrl = properties.getProperty("fitbitSiteBaseUrl");
        exampleBaseUrl = properties.getProperty("exampleBaseUrl");
        clientConsumerKey = properties.getProperty("clientConsumerKey");
        clientSecret = properties.getProperty("clientSecret");
        pin = properties.getProperty("PIN");
        accessToken = properties.getProperty("accessToken");
        accessTokenSecret = properties.getProperty("accessTokenSecret");
	    
		apiClientService = new FitbitAPIClientService<FitbitApiClientAgent>(
		           new FitbitApiClientAgent(apiBaseUrl, fitbitSiteBaseUrl, credentialsCache),
		            clientConsumerKey,
		            clientSecret,
		            credentialsCache,
		            entityCache,
		            subscriptionStore
		);
		
		String userID = "36C6JF";
		LocalUserDetail ud = new LocalUserDetail(userID);
		String url;
		try {
			url = apiClientService.getResourceOwnerAuthorizationURL(ud, "");
			APIResourceCredentials creds = apiClientService.getResourceCredentialsByUser(ud);
			if (accessToken == null && accessTokenSecret == null) {
				System.out.println("Pease open this URL and enable the application: " + url);
				System.out.println("Enter PIN:");
				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				pin = br.readLine();
				System.out.println("PIN: " + pin);
				creds.setTempTokenVerifier(pin);
				apiClientService.saveResourceCredentials(ud, creds);
				apiClientService.getTokenCredentials(ud);		
				properties.setProperty("accessToken", creds.getAccessToken());
				properties.setProperty("accessTokenSecret", creds.getAccessTokenSecret());
				String file = getClass().getClassLoader().getResource("config.properties").getFile();
				System.out.println("File: " + file);
				OutputStream os = new FileOutputStream(getClass().getClassLoader().getResource("config.properties").getFile());
				properties.store(os, "");
			} else {
				creds.setAccessToken(accessToken);
				creds.setAccessTokenSecret(accessTokenSecret);
			}
			
			UserInfo userInfo = apiClientService.getClient().getUserInfo(ud);
			System.out.println("Welcome " + userInfo.getDisplayName());
			//Activities activities = apiService.getActivities(user, new LocalDate());
		} catch (FitbitAPIException e) {
			System.out.println("FitBit error: " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("IO error: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
