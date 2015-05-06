package com.cycos.circuit.impl;

import java.net.URI;
import java.net.URISyntaxException;

import net.ansible.clientapi.testclient.client.SyncCircuitClient;
import net.ansible.clientapi.testclient.websocket.WebsocketConnection;

import com.cycos.circuit.CircuitConnector;
import com.cycos.circuit.CircuitEventListener;

public class CircuitConnectorImpl implements CircuitConnector {

	public void login(String username, String password,
			CircuitEventListener listener) {
        WebsocketConnection con;
		try {
			con = new WebsocketConnection(new URI("wss://localhost:8082/mock/ws"), true);
	        final SyncCircuitClient client = new SyncCircuitClient(con, username, password);
	        final String userId = client.user().getLoggedOnUser().getResponse().getUser().getGetLoggedOn().getUser().getUserId();
		
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public String getDirectConversation(String user) {
		// TODO Auto-generated method stub
		return null;
	}

	public void createTextItem(String conversationID, String text) {
		// TODO Auto-generated method stub

	}

}
