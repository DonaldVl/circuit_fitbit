package com.cycos.circuit;

import java.util.HashMap;
import java.util.Map;

public class FitbitUsers {
	private Map<String, UserData> users = null;

	public FitbitUsers() {
		users = new HashMap<String, UserData>();
	}
	
	public void add(UserData user) {
		users.put(user.getUserID(), user);
	}

	public void remove(UserData user) {
		users.remove(user.getUserID());
	}
}
