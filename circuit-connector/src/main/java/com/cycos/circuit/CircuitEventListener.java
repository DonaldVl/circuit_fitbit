package com.cycos.circuit;

import net.ansible.protobuf.conversation.Conversation;

public interface CircuitEventListener {
	public void onNewDirectConversation(String conversationID, String userID);
	public void onNewAuthenticationToken(String userID, String token);
}
