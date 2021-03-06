package com.cycos.circuit;

import java.util.List;

public interface CircuitConnector {
	public List<UserData> getAllFitbitUsers();
	public void createWelcomeTextItem(String conversationID);
	public void createGroupWelcomeTextItem(String conversationID);
	public void createURLTextItem(String conversationId, String url);
	public void saveUserCredentials(String conversationId, String fitbitUserId, String accessToken, String accessTokenSecret);
	public void createTextItem(String conversationId, String text);
	public void createDirectConversation(String userId);
	public void setCircuitEventListener(CircuitEventListener listener);
	public String getName(String circuitUserId);
	public String getCircuitUserId();
}
