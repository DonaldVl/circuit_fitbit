package com.cycos.circuit;

public class FitbitData {
	private String userID = null;
	private String apiBaseUrl = null;
	private String fitbitSiteBaseUrl = null;
	private String exampleBaseUrl = null;
	private String clientConsumerKey = null;
	private String clientSecret = null;
	private String pin = null;
	private String accessToken = null;
	private String accessTokenSecret = null;
	private ConfigHandler config = null;

	public FitbitData(String userID) {
		this.setUserID(userID);
		config = new ConfigHandler();
		config.load();
		setApiBaseUrl(config.get("apiBaseUrl"));
        setFitbitSiteBaseUrl(config.get("fitbitSiteBaseUrl"));
        setExampleBaseUrl(config.get("exampleBaseUrl"));
        setClientConsumerKey(config.get("clientConsumerKey"));
        setClientSecret(config.get("clientSecret"));
        setPin(config.get("PIN"));
        setAccessToken(config.get("accessToken"));
        setAccessTokenSecret(config.get("accessTokenSecret"));
	}

	public String getUserID() {
		return userID;
	}

	public void setUserID(String userID) {
		this.userID = userID;
	}

	public String getApiBaseUrl() {
		return apiBaseUrl;
	}

	public void setApiBaseUrl(String apiBaseUrl) {
		this.apiBaseUrl = apiBaseUrl;
		config.set("apiBaseUrl", apiBaseUrl);
	}

	public String getFitbitSiteBaseUrl() {
		return fitbitSiteBaseUrl;
	}

	public void setFitbitSiteBaseUrl(String fitbitSiteBaseUrl) {
		this.fitbitSiteBaseUrl = fitbitSiteBaseUrl;
		config.set("fitbitSiteBaseUrl", fitbitSiteBaseUrl);
	}

	public String getClientConsumerKey() {
		return clientConsumerKey;
	}

	public void setClientConsumerKey(String clientConsumerKey) {
		this.clientConsumerKey = clientConsumerKey;
		config.set("clientConsumerKey", clientConsumerKey);
	}

	public String getClientSecret() {
		return clientSecret;
	}

	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
		config.set("clientSecret", clientSecret);
	}

	public String getPin() {
		return pin;
	}

	public void setPin(String pin) {
		this.pin = pin;
		config.set("pin", pin);
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

	public String getExampleBaseUrl() {
		return exampleBaseUrl;
	}

	public void setExampleBaseUrl(String exampleBaseUrl) {
		this.exampleBaseUrl = exampleBaseUrl;
		config.set("exampleBaseUrl", exampleBaseUrl);
	}

}
