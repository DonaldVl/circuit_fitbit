package com.cycos.circuit;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import net.ansible.clientapi.testclient.client.SyncCircuitClient;
import net.ansible.clientapi.testclient.connection.EventListener;
import net.ansible.clientapi.testclient.websocket.WebsocketConnection;
import net.ansible.protobuf.conversation.Conversation.ConversationType;
import net.ansible.protobuf.conversation.ConversationEvent.CreateConversationEvent;
import net.ansible.protobuf.conversation.ConversationItem.ConversationItemType;
import net.ansible.protobuf.conversation.ConversationItem.SystemItem;
import net.ansible.protobuf.conversation.Participant;
import net.ansible.protobuf.websocket.WSMessage;
import net.ansible.protobuf.websocket.WSMessage.ContentType;
import net.ansible.protobuf.websocket.WSMessage.Event;

public class CircuitConnection {

    public static void main(String[] args) throws URISyntaxException, Exception {
        WebsocketConnection con = new WebsocketConnection(new URI("wss://localhost:8082/mock/ws"), true);
        final SyncCircuitClient client = new SyncCircuitClient(con, "fitbit@unify.com", "abc123");
        final String userId = client.user().getLoggedOnUser().getResponse().getUser().getGetLoggedOn().getUser().getUserId();

        client.conversation().create(Arrays.asList(new Participant[] { new Participant("2"), new Participant("3") }), ConversationType.GROUP, "FitBit Test",
                null);

        client.addEventListener(new EventListener() {
            public void eventReceived(WSMessage message) {
                Event e = message.getEvent();
                if (e.getType() == ContentType.CONVERSATION && e.getConversation() != null && e.getConversation().getCreate() != null) {
                    CreateConversationEvent create = e.getConversation().getCreate();
                    client.conversation().addTextItem(create.getConversation().getConvId(), "Hello, I am your personal fitness coach.");
                }
            }
        });

        client.addEventListener(new EventListener() {
            public void eventReceived(WSMessage message) {
                Event e = message.getEvent();
                if (e.getType() == ContentType.CONVERSATION && e.getConversation() != null && e.getConversation().getAddItem() != null
                        && e.getConversation().getAddItem().getItem().getType() == ConversationItemType.SYSTEM
                        && e.getConversation().getAddItem().getItem().getSystem().getType() == SystemItem.ContentType.PARTICIPANT_ADDED) {

                    for (Participant p : e.getConversation().getAddItem().getItem().getSystem().getAffectedParticipantsList()) {
                        if (p.getUserId().equals(userId)) {
                            client.conversation().addTextItem(e.getConversation().getConvId(), "Hello, I am your personal fitness coach.");
                        }
                    }

                }
            }
        });

    }
}
