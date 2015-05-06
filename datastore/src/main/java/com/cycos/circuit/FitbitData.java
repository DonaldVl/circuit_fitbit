package com.cycos.circuit;

public class FitbitData {	
	private String apiBaseUrl = null;
	private String fitbitSiteBaseUrl = null;
	private String exampleBaseUrl = null;
	private String clientConsumerKey = null;
	private String clientSecret = null;
	
	private ConfigHandler config = null;

	public FitbitData() {
		config = new ConfigHandler();
		config.load();
		setApiBaseUrl(config.get("apiBaseUrl"));
        setFitbitSiteBaseUrl(config.get("fitbitSiteBaseUrl"));
        setExampleBaseUrl(config.get("exampleBaseUrl"));
        setClientConsumerKey(config.get("clientConsumerKey"));
        setClientSecret(config.get("clientSecret"));
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

	public String getExampleBaseUrl() {
		return exampleBaseUrl;
	}

	public void setExampleBaseUrl(String exampleBaseUrl) {
		this.exampleBaseUrl = exampleBaseUrl;
		config.set("exampleBaseUrl", exampleBaseUrl);
	}

}
