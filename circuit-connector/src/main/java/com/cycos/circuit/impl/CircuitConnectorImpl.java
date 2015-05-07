package com.cycos.circuit.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.ansible.clientapi.testclient.client.SyncCircuitClient;
import net.ansible.clientapi.testclient.connection.EventListener;
import net.ansible.clientapi.testclient.websocket.WebsocketConnection;
import net.ansible.protobuf.conversation.Conversation.ConversationType;
import net.ansible.protobuf.conversation.ConversationEvent.CreateConversationEvent;
import net.ansible.protobuf.conversation.ConversationEvent.EventType;
import net.ansible.protobuf.conversation.ConversationItem;
import net.ansible.protobuf.conversation.ConversationItem.ConversationItemType;
import net.ansible.protobuf.conversation.ConversationItem.SystemItem;
import net.ansible.protobuf.conversation.ConversationItem.TextItem;
import net.ansible.protobuf.conversation.Participant;
import net.ansible.protobuf.websocket.WSMessage;
import net.ansible.protobuf.websocket.WSMessage.ContentType;
import net.ansible.protobuf.websocket.WSMessage.Event;

import com.cycos.circuit.CircuitConnector;
import com.cycos.circuit.CircuitEventListener;
import com.cycos.circuit.ConfigHandler;
import com.cycos.circuit.UserData;

public class CircuitConnectorImpl implements CircuitConnector, EventListener {

    private CircuitEventListener listener;
    private final SyncCircuitClient client;
    private final String userId;

    public CircuitConnectorImpl(ConfigHandler config) throws Exception {
    	String url = config.get("circuitUri");
        WebsocketConnection con = new WebsocketConnection(new URI(url), true);
        client = new SyncCircuitClient(con, config.get("circuitUsername"), config.get("circuitPassword"));
        userId = client.user().getLoggedOnUser().getResponse().getUser().getGetLoggedOn().getUser().getUserId();
        client.addEventListener(this);
        this.listener = new CircuitEventListener() {

            public void onNewFoodEntry(String userId, String food) {
            }

            public void onNewFitbitUserId(String userId, String fitbitUserId) {            
            }

            public void onNewDirectConversation(String conversationID, List<String> userID) {
            }

            public void onNewDirectConversation(String conversationID, String userID) {
            }

            public void onNewAuthenticationToken(String userID, String token) {
            }
        };
    }

    public void setCircuitEventListener(CircuitEventListener listener) {
        this.listener = listener;
    }

    public List<UserData> getAllFitbitUsers() {
        return Collections.<UserData> emptyList();
    }

    public void createWelcomeTextItem(String conversationId) {
        client.conversation().addTextItem(conversationId, "Welcome to fitbit instructor", "blalalalala", TextItem.ContentType.RICH, null, null, null);
    }

    public void createURLTextItem(String conversationId, String url) {
        client.conversation().addTextItem(conversationId, null,
                "Please click the link and follow the instruction. Afterwards come back and post your token with fitbit token 'MY_TOKEN'",
                TextItem.ContentType.RICH, null, null, null);
    }

    public void saveUserCredentials(String conversationId, String accessToken, String accessTokenSecret) {
        client.conversation().addTextItem(conversationId, null, "accessToken='" + accessToken + "' accessTokenSecret='" + accessTokenSecret + "'",
                TextItem.ContentType.RICH, null, null, null);
    }

    public void createTextItem(String conversationId, String text) {
        client.conversation().addTextItem(conversationId, null, text, TextItem.ContentType.RICH, null, null, null);

    }

    public void createDirectConversation(String userId) {
        client.conversation()
                .create(Arrays.asList(new Participant[] { new Participant(userId) }), ConversationType.DIRECT, "FitBit private instructions", null);

    }

    public void eventReceived(WSMessage message) {
        Event e = message.getEvent();
        if (e.getType() == ContentType.CONVERSATION && e.getConversation() != null && e.getConversation().getCreate() != null) {
            CreateConversationEvent create = e.getConversation().getCreate();

            // Remove fitbit user
            create.getConversation().getParticipantsList().remove(new Participant(userId));
            if (e.getConversation().getCreate().getConversation().getType() == ConversationType.DIRECT) {

                listener.onNewDirectConversation(create.getConversation().getConvId(), create.getConversation().getParticipantsList().get(0).getUserId());
            } else {

                ArrayList<String> users = new ArrayList<String>(create.getConversation().getParticipantsList().size());
                for (Participant participant : create.getConversation().getParticipantsList()) {
                    users.add(participant.getUserId());
                }
                listener.onNewDirectConversation(create.getConversation().getConvId(), users);
            }

        } else if (e.getType() == ContentType.CONVERSATION && e.getConversation() != null && e.getConversation().getAddItem() != null
                && e.getConversation().getAddItem().getItem().getType() == ConversationItemType.SYSTEM
                && e.getConversation().getAddItem().getItem().getSystem().getType() == SystemItem.ContentType.PARTICIPANT_ADDED) {

            for (Participant p : e.getConversation().getAddItem().getItem().getSystem().getAffectedParticipantsList()) {
                if (p.getUserId().equals(userId)) {
                    ArrayList<String> users = new ArrayList<String>(e.getConversation().getAddItem().getItem().getSystem().getAffectedParticipantsList().size());
                    for (Participant participant : e.getConversation().getAddItem().getItem().getSystem().getAffectedParticipantsList()) {
                        users.add(participant.getUserId());
                    }
                    listener.onNewDirectConversation(e.getConversation().getConvId(), users);
                }
            }

        } else if (EventType.ADD_ITEM.equals(message.getEvent().getConversation().getType())) {

            if (!ignoreItem(message.getEvent().getConversation().getAddItem().getItem())) {

                if (ConversationItemType.TEXT.equals(message.getEvent().getConversation().getAddItem().getItem().getType())) {
                    String command = message.getEvent().getConversation().getAddItem().getItem().getText().getContent();
                    if (isCommand(command)) {

                        String creatorId = message.getEvent().getConversation().getAddItem().getItem().getCreatorId();

                        if (command.startsWith("fitbit user")) {
                            // User has given his Id
                            listener.onNewFitbitUserId(creatorId, extractFitbitUserId(command));

                        } else if (command.startsWith("fitbit food")) {
                            // User has added new food entry
                            listener.onNewFoodEntry(creatorId, extractFoodEntry(command));
                        } else if (command.startsWith("fitbit token")) {
                            // User has provided the token
                            listener.onNewAuthenticationToken(creatorId, extractToken(command));
                        }

                    }
                }

            }
        }
    }

    private String extractToken(String command) {
        return command.substring("fitbit token ".length());
    }

    private String extractFoodEntry(String command) {
        return command.substring("fitbit food ".length());
    }

    private String extractFitbitUserId(String command) {
        return command.substring("fitbit user ".length());
    }

    private boolean isCommand(String command) {
        return command.startsWith("fitbit");
    }

    private boolean ignoreItem(ConversationItem item) {
        return userId.equals(item.getCreatorId());
    }
}
