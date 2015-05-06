package com.cycos.circuit;

public class CircuitData {
	private String userID = null;
	private String fitbitDirectConversationID = null;

	public CircuitData(String userID) {
		this.userID = userID;
	}

	public String getFitbitDirectConversationID() {
		return fitbitDirectConversationID;
	}

	public void setFitbitDirectConversationID(String fitbitDirectConversationID) {
		this.fitbitDirectConversationID = fitbitDirectConversationID;
	}

}
