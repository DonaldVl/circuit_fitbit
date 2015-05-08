package com.cycos.circuit;

import java.util.List;

public interface CircuitEventListener {
	public void onNewDirectConversation(String conversationID, String userID);
	public void onNewFitbitUserId(String userId, String fitbitUserId);
	public void onNewAuthenticationToken(String userID, String token);
	public void onNewFoodEntry(String userId, String food);
	public void onNewGroupConversation(String conversationID, List<String> userID);
    public void onNewActivityEntry(String circuitUserId, String extractAfter);
}
