package com.cycos.circuit;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.joda.time.LocalDate;

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
import com.fitbit.api.common.model.activities.Activities;
import com.fitbit.api.common.model.activities.ActivityLog;
import com.fitbit.api.common.model.user.UserInfo;
import com.fitbit.api.model.APIResourceCredentials;
import com.google.api.client.util.Collections2;
import com.google.api.client.util.Sets;

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
	private String userID = null;
	private LocalUserDetail ud = null;
	private List<ActivityLog> activitiesStore = null;
	
	public FitBitConnector() {
		try {
			userID = "36C6JF";
			ud = new LocalUserDetail(userID);
			properties = new Properties();
			properties.load(getClass().getClassLoader().getResourceAsStream("config.properties"));
			activitiesStore = new ArrayList<ActivityLog>();
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
	
	public void fetchUserActivities() {
		try {
			LocalDate date = LocalDate.now();
			System.out.println("Printing activities for: " + date.toString());
			Activities activities = apiClientService.getActivities(ud, date);
			List<ActivityLog> activitiesList = activities.getActivities();
			for(ActivityLog log : activitiesList) {
				System.out.println("Activity: " + log.getActivityId());
				System.out.println("---------");
				System.out.println("Start: " + log.getStartTime());
				System.out.println("Description: " + log.getDescription());
				System.out.println("Distance: " + log.getDistance());
			}
			storeActivities(activitiesList);
		} catch (FitbitAPIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void storeActivities(List<ActivityLog> activitiesList) {
		System.out.println("New activites list: " + activitiesList);
		System.out.println("Stored activities list " + activitiesStore);
		for(ActivityLog log : activitiesList) {
			System.out.println("Searching for activity " + log.getActivityId());
			boolean isFound = false;
			for(ActivityLog log2 : activitiesStore) {
				if(log2.getActivityId() == log.getActivityId()) {
					System.out.println("Found activity in store " + log2.getActivityId());
					isFound = true;
				}
			}
			if(!isFound) {
				System.out.println("New activity found to add to store: " + log.getActivityId());
				activitiesStore.add(log);
			}
		}
	}
}
