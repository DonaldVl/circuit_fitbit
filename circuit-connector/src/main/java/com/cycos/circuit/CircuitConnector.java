package com.cycos.circuit;

public interface CircuitConnector {
	public void login(String username, String password, CircuitEventListener listener);
	
	public String getDirectConversation(String user);
	
	public void createTextItem(String conversationID, String text);
}
