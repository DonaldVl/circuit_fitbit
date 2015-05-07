package com.cycos.circuit;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class FitbitUsers {
	private Map<String, UserData> users = null;

	public FitbitUsers() {
		users = new HashMap<String, UserData>();
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
