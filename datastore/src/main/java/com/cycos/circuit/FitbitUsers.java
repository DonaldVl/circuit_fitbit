package com.cycos.circuit;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FitbitUsers {
	private Map<String, UserData> users = null;

	public FitbitUsers() {
		users = new HashMap<String, UserData>();
	}
	
	public void addAll(List<UserData> newUsers) {
		for(UserData user : newUsers) {
			if((user.getAccessToken() != null) 
					&& (user.getAccessTokenSecret() != null)
					&& (user.getFitbitUserId() != null)
					&& (user.getConversationID()!= null)) {
				add(user);
			}
			
		}
	}
	
	public Collection<UserData> getUsers() {
		return users.values();
	}
	
	public void add(UserData user) {
		users.put(user.getUserID(), user);
	}

	public void remove(UserData user) {
		users.remove(user.getUserID());
	}
	
	public UserData get(String userID) {
		return users.get(userID);
	}
}
