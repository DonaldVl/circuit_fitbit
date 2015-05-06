package com.cycos.circuit;

public class CircuitData {
	private String userID = null;
	private String conversationID = null;
	private String accessToken = null;
	private String accessTokenSecret = null;

	public CircuitData(String userID, String conversationID, String accessToken, String accessTokenSecret) {
		this.setUserID(userID);
		this.setConversationID(conversationID);
		this.setAccessToken(accessToken);
		this.setAccessTokenSecret(accessTokenSecret);
	}

	public String getConversationID() {
		return conversationID;
	}

	public void setConversationID(String conversationID) {
		this.conversationID = conversationID;
	}

	public String getUserID() {
		return userID;
	}

	public void setUserID(String userID) {
		this.userID = userID;
	}

	public String getAccessTokenSecret() {
		return accessTokenSecret;
	}

	public void setAccessTokenSecret(String accessTokenSecret) {
		this.accessTokenSecret = accessTokenSecret;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}
}
