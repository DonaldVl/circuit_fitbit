package com.cycos.circuit.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import net.ansible.clientapi.testclient.client.CircuitClient;
import net.ansible.clientapi.testclient.client.SyncCircuitClient;
import net.ansible.clientapi.testclient.connection.EventListener;
import net.ansible.clientapi.testclient.facade.CircuitFactory;
import net.ansible.clientapi.testclient.logging.IClientLogger;
import net.ansible.clientapi.testclient.logging.TestLibLogger;
import net.ansible.clientapi.testclient.websocket.ConnectionConfig;
import net.ansible.clientapi.testclient.websocket.WebsocketConnection;
import net.ansible.protobuf.conversation.Conversation;
import net.ansible.protobuf.conversation.Conversation.ConversationType;
import net.ansible.protobuf.conversation.ConversationArea.Direction;
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

    private static final String IDENTIFIER_TOKEN = "accessToken";
    private CircuitEventListener listener;
    private final CircuitClient client;
    private final String userId;
    private List<Command> commands = new ArrayList<Command>();
    
    public CircuitConnectorImpl(ConfigHandler config) throws Exception {
        initCommands();
        TestLibLogger.setLogger(new IClientLogger() {

            public void warn(String message) {
                System.out.println(message);
            }

            public void trace(String message) {
                System.out.println(message);
            }

            public void info(String message) {
                System.out.println(message);
            }

            public void error(String message) {
                System.out.println(message);
            }

            public void debug(String message) {
                System.out.println(message);
            }
        });

        String userEmail = config.get("circuitUsername");
        String password = config.get("circuitPassword");
        String server = config.get("circuitHost");
        String port = config.get("circuitPort");

        if (config.get("circuitUri") != null && !config.get("circuitUri").isEmpty()) {
            WebsocketConnection con = new WebsocketConnection(new URI(config.get("circuitUri")), true);
            client = new SyncCircuitClient(con, userEmail, password);
        } else if (Boolean.valueOf(config.get("clientApiDirect")).booleanValue()) {
            client = CircuitFactory.getSyncClientApiClient(server, userEmail, password);
        } else {
            ConnectionConfig testLibConfig = new ConnectionConfig(server, port);
            client = CircuitFactory.getAccessServerClient(testLibConfig, userEmail, password);
        }

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
        List<UserData> result = new LinkedList<UserData>();
        WSMessage conversations = client.conversation().getConversations(userId, 0L, Direction.AFTER, Integer.MAX_VALUE, 2);
        List<Conversation> conversationsList = conversations.getResponse().getConversation().getGetConversations().getConversationsList();

        for (Conversation conversation : conversationsList) {
            WSMessage allConversationItems = client.conversation().getAllConversationItems(conversation.getConvId(), 0L, Direction.AFTER, Integer.MAX_VALUE);
            List<ConversationItem> itemsList = allConversationItems.getResponse().getConversation().getGetItemsByConversation().getItemsList();

            for (ConversationItem item : itemsList) {
                if (item.getType() == ConversationItemType.TEXT && item.getText().getContent().startsWith(IDENTIFIER_TOKEN)) {
                    String content = item.getText().getContent();
                    String[] split = content.split("'");
                    String accessToken = split[1];
                    String accessTokenSecret = split[3];
                    String fitBitUserId = split[5];

                    // Remove fitbit user and get the user itself
                    conversation.getParticipantsList().remove(new Participant(userId));

                    System.out.println(String.format("Found fitbit user configuration in conversation '%s' with credentials fitbitUserId='%s' accessToken='%s' accessTokenSecret='%s'", item.getConvId(), fitBitUserId, accessToken, accessTokenSecret));
                    
                    UserData data = new UserData(conversation.getParticipantsList().get(0).getUserId(), fitBitUserId, item.getConvId(), accessToken, accessTokenSecret);
                    result.add(data);
                }
            }
        }

        return result;
    }

    public void createWelcomeTextItem(String conversationId) {
        System.out.println("Create Welcome message in conversation " + conversationId);
        client.conversation().addTextItem(conversationId, "Welcome to fitbit instructor", "To connect your fitbit account to circuit I need some information. Step One: Please give me your fitbit user id with fitbit user 'YOUR_USERID'", TextItem.ContentType.RICH, null, null, null);
    }

    public void createURLTextItem(String conversationId, String url) {
        System.out.println("Create URL "+ url + " text message in conversation " + conversationId);
        client.conversation().addTextItem(conversationId, null,
                "Please click the link " + url + " and follow the instruction. Afterwards come back and post your token with fitbit token 'MY_TOKEN'",
                TextItem.ContentType.RICH, null, null, null);
    }

    public void saveUserCredentials(String conversationId, String fitbitUserId, String accessToken, String accessTokenSecret) {
        System.out.println(String.format("Create text item in conversation '%s' with fitbit user credentials fitbitUserId='%s' accessToken='%s' accessTokenSecret='%s'", conversationId, fitbitUserId, accessToken, accessTokenSecret));
        client.conversation().addTextItem(conversationId, null, "accessToken='" + accessToken + "' accessTokenSecret='" + accessTokenSecret + "' fitBitUserId='" + fitbitUserId + "'",
                TextItem.ContentType.RICH, null, null, null);
    }

    public void createTextItem(String conversationId, String text) {
        System.out.println(String.format("Create text item with text '%s' in conversation '%s'", conversationId, text));   
        client.conversation().addTextItem(conversationId, null, text, TextItem.ContentType.RICH, null, null, null);

    }

    public void createDirectConversation(String userId) {
        System.out.println(String.format("Create direct conversation with user  '%s'", userId));
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
                        for(Command myCommand:commands) {
                            if(myCommand.match(command)) {
                                System.out.println("Process command: " + command);
                                myCommand.processs(creatorId, command);
                            }
                            
                        }
                    }
                }

            }
        }
    }
    
    private boolean isCommand(String command) {
        return command.startsWith("fitbit");
    }

    private boolean ignoreItem(ConversationItem item) {
        return userId.equals(item.getCreatorId());
    }
    
    private void initCommands() {
        commands.add(new Command("token") {
            @Override
            public void processs(String circuitUserId, String command) {
                String result = extractAfter(command);
                if(result.startsWith("#160;")) {
                    result = result.substring("#160;".length());
                }
                listener.onNewAuthenticationToken(circuitUserId, result);
            }
            
        });
        
        commands.add(new Command("food") {
            @Override
            public void processs(String circuitUserId, String command) {
                listener.onNewFoodEntry(circuitUserId, extractAfter(command));
            }
            
        });
        
        commands.add(new Command("user") {
            @Override
            public void processs(String circuitUserId, String command) {
                listener.onNewFitbitUserId(circuitUserId, extractAfter(command));
            }
            
        });        
    }
        
    abstract class Command {
        public static final String prefix = "fitbit";
        private String command;
        
        public Command(String command) {
            this.command = command;
        }
        
        public boolean match(String command) {
            return command.startsWith(prefix + " " + this.command);
        }
        
        public String extractAfter(String command) {
            String start = prefix + " " + this.command + " ";
            return command.substring(start.length());
        }
        
        public abstract void processs(String circuitUserId, String command);
    }
}
