package com.cycos.circuit;

public class FitbitUserData {
	private String userID = null;
	private String accessToken = null;
	private String accessTokenSecret = null;
	private ConfigHandler config = null;
	
	public FitbitUserData(String userID) {
		this.setUserID(userID);
		config = new ConfigHandler();
		config.load();
		setAccessToken(config.get("accessToken"));
        setAccessTokenSecret(config.get("accessTokenSecret"));
	}
	
	public String getUserID() {
		return userID;
	}

	public void setUserID(String userID) {
		this.userID = userID;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
		config.set("accessToken", accessToken);
	}

	public String getAccessTokenSecret() {
		return accessTokenSecret;
	}

	public void setAccessTokenSecret(String accessTokenSecret) {
		this.accessTokenSecret = accessTokenSecret;
		config.set("accessTokenSecret", accessTokenSecret);
	}
}
