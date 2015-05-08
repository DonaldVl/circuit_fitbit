package com.cycos.circuit;

import java.util.List;

public interface CircuitEventListener {
	public void onNewDirectConversation(String conversationID, String userID);
	public void onNewFitbitUserId(String userId, String fitbitUserId, String conversationID);
	public void onNewAuthenticationToken(String userID, String token, String conversationID);
	public void onNewFoodEntry(String userId, String food);
	public void onNewGroupConversation(String conversationID, List<String> userID);
    public void onNewActivityEntry(String circuitUserId, String extractAfter);
    public void onShowStatsRequest(String circuitUserId);
    public void onShowAlarmRequest(String circuitUserId);
    public void onShowProfileRequest(String circuitUserId);
    public void onStartCombatMode(String circuitUserId, String extractAfter, String conversationId);
}
