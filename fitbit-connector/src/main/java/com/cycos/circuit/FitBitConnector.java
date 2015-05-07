package com.cycos.circuit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

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
import com.fitbit.api.common.model.user.UserInfo;
import com.fitbit.api.model.APICollectionType;
import com.fitbit.api.model.APIResourceCredentials;
import com.fitbit.api.model.FitbitUser;

public class FitBitConnector {
	private FitbitAPIEntityCache entityCache = new FitbitApiEntityCacheMapImpl();
	private FitbitApiCredentialsCache credentialsCache = new FitbitApiCredentialsCacheMapImpl();
	private FitbitApiSubscriptionStorage subscriptionStore = new FitbitApiSubscriptionStorageInMemoryImpl();
	private FitbitAPIClientService<FitbitApiClientAgent> apiClientService = null;
	private String apiBaseUrl = null;
	private String fitbitSiteBaseUrl = null;
	private String clientConsumerKey = null;
	private String clientSecret = null;
	private int steps = 0;
	private FitbitData data = null;
	private CircuitConnector circuit = null;
	private FitbitUsers users = null;
	
	public FitBitConnector(final CircuitConnector circuit) {
		data = new FitbitData();
		this.circuit = circuit;
		this.circuit.setCircuitEventListener(new CircuitEventListener() {
            
            public void onNewFoodEntry(String userId, String food) {
                // TODO Auto-generated method stub
                
            }
            
            public void onNewFitbitUserId(String userId, String fitbitUserId) {
                UserData user= users.get(userId);
                user.setFitbitUserId(fitbitUserId);
                LocalUserDetail ud = new LocalUserDetail(user.getFitbitUserId());
                try {
					String url = apiClientService.getResourceOwnerAuthorizationURL(ud, "");
					circuit.createURLTextItem(user.getConversationID(), url);
				} catch (FitbitAPIException e) {
					e.printStackTrace();
				}
            }
            
            public void onNewDirectConversation(String conversationID, List<String> userID) {
                // TODO Auto-generated method stub
                
            }
            
            public void onNewDirectConversation(String conversationID, String userID) {
                UserData user = new UserData(userID, null ,conversationID, null, null);
                users.add(user);
                circuit.createWelcomeTextItem(conversationID);
            }
            
            public void onNewAuthenticationToken(String userID, String token) {
                UserData user = users.get(userID);
                LocalUserDetail ud = new LocalUserDetail(user.getFitbitUserId());
                createAccessToken(ud, token, user);
            }
        });
	}
	
	public void init() {
		users = new FitbitUsers();
        apiBaseUrl = data.getApiBaseUrl();
        fitbitSiteBaseUrl = data.getFitbitSiteBaseUrl();
        clientConsumerKey = data.getClientConsumerKey();
        clientSecret = data.getClientSecret();
        
		apiClientService = new FitbitAPIClientService<FitbitApiClientAgent>(
		           new FitbitApiClientAgent(apiBaseUrl, fitbitSiteBaseUrl, credentialsCache),
		            clientConsumerKey,
		            clientSecret,
		            credentialsCache,
		            entityCache,
		            subscriptionStore
		);
	}
	
	public void addUser(UserData userData) {
		users.add(userData);
		LocalUserDetail ud = new LocalUserDetail(userData.getUserID());
		String url;
		try {
			url = apiClientService.getResourceOwnerAuthorizationURL(ud, "");
			APIResourceCredentials creds = apiClientService.getResourceCredentialsByUser(ud);
			if (userData.getAccessToken() == null && userData.getAccessTokenSecret() == null) {
				System.out.println("Pease open this URL and enable the application: " + url);
				System.out.println("Enter PIN:");
				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				String pin = br.readLine();
				System.out.println("PIN: " + pin);
				createAccessToken(ud, pin, userData);
			} else {
				creds.setAccessToken(userData.getAccessToken());
				creds.setAccessTokenSecret(userData.getAccessTokenSecret());
			}
			
			UserInfo userInfo = apiClientService.getClient().getUserInfo(ud);
			System.out.println("Welcome " + userInfo.getDisplayName());
		} catch (FitbitAPIException e) {
			System.out.println("FitBit error: " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("IO error: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	public LocalUserDetail authenticateUser(UserData userData) {
		LocalUserDetail ud = new LocalUserDetail(userData.getUserID());
		APIResourceCredentials creds = apiClientService.getResourceCredentialsByUser(ud);
		creds.setAccessToken(userData.getAccessToken());
		creds.setAccessTokenSecret(userData.getAccessTokenSecret());
		return ud;
	}
	
	public void createAccessToken(LocalUserDetail ud, String pin, UserData userData) {
		try {
			APIResourceCredentials creds = apiClientService.getResourceCredentialsByUser(ud);
			creds.setTempTokenVerifier(pin);
			apiClientService.saveResourceCredentials(ud, creds);
			apiClientService.getTokenCredentials(ud);
			userData.setAccessToken(creds.getAccessToken());
			userData.setAccessTokenSecret(creds.getAccessTokenSecret());
		} catch (FitbitAPIException e) {
			e.printStackTrace();
		}		
	}
	
	public void createSubscription(UserData userData) {
		LocalUserDetail ud = new LocalUserDetail(userData.getUserID());
		try {
			apiClientService.subscribe("2", ud, 
					APICollectionType.activities, ud.getUserId());
		} catch (FitbitAPIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void fetchUserSteps() {
		for (UserData user : users.getUsers()) {
			LocalUserDetail ud = authenticateUser(user);
			try {
				LocalDate date = LocalDate.now();
				System.out.println("Printing steps for: " + date.toString());
				Activities activities = apiClientService.getClient().getActivities(ud, 
						FitbitUser.CURRENT_AUTHORIZED_USER, date);
				int stepsNew = activities.getSummary().getSteps();
				System.out.println("New number of steps: " + stepsNew + " at time " + LocalDate.now().toString() + " " + LocalTime.now().toString());
				analyzeUserSteps(user, stepsNew);
			} catch (FitbitAPIException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void analyzeUserSteps(UserData user, int newSteps) {
		if (newSteps > steps) {
			int diffSteps = newSteps - steps;
			StringBuffer buff = new StringBuffer("Hi, here is your Fitbit fitness manager!\nIn the last minute you walked ");
			buff.append(diffSteps);
			buff.append(" steps! Great, continue like this! :-)");
			circuit.createTextItem(user.getConversationID(), buff.toString());
			steps = newSteps;
		} else {
			if (newSteps < steps) {
				circuit.createTextItem(user.getConversationID(), 
						"Hi, here is your Fitbit fitness manager!\nSomething is wrong with your tracker!");
			} else {
				circuit.createTextItem(user.getConversationID(), 
						"Hi, here is your Fitbit fitness manager!\nBooooo! You did not walk any step in the last minute...don't you fear getting fat?");
			}
		}
	}
}
