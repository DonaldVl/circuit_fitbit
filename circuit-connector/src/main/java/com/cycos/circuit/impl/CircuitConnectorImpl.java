package com.cycos.circuit.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import net.ansible.protobuf.websocket.WSMessage.Response.ReturnCode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cycos.circuit.CircuitConnector;
import com.cycos.circuit.CircuitEventListener;
import com.cycos.circuit.ConfigHandler;
import com.cycos.circuit.UserData;

public class CircuitConnectorImpl implements CircuitConnector, EventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(CircuitConnectorImpl.class);

    private static final String IDENTIFIER_TOKEN = "<b>Great your fitbit account is now connected to your circuit acccount</b><br><br>";
    private final static String STANDARD_SUBJECT = "Hi from your fitbit instructor";
    private CircuitEventListener listener;
    private final CircuitClient client;
    private final String userId;
    private List<Command> commands = new ArrayList<Command>();

    public CircuitConnectorImpl(ConfigHandler config) throws Exception {
        initCommands();
        TestLibLogger.setLogger(new IClientLogger() {

            public void warn(String message) {
                LOGGER.info(message);
            }

            public void trace(String message) {
                LOGGER.info(message);
            }

            public void info(String message) {
                LOGGER.info(message);
            }

            public void error(String message) {
                LOGGER.info(message);
            }

            public void debug(String message) {
                LOGGER.info(message);
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
            ConnectionConfig testLibConfig = null;
            if (config.get("proxyHost") != null) {
                testLibConfig = new ConnectionConfig(server, port, config.get("proxyHost"), config.get("proxyPort"));
            } else {
                testLibConfig = new ConnectionConfig(server, port);
            }
            client = CircuitFactory.getAccessServerClient(testLibConfig, userEmail, password);
        }

        userId = client.user().getLoggedOnUser().getResponse().getUser().getGetLoggedOn().getUser().getUserId();
        client.addEventListener(this);
        this.listener = new CircuitEventListener() {

            public void onNewFoodEntry(String userId, String food) {
            }

            public void onNewFitbitUserId(String userId, String fitbitUserId, String conversationID) {
            }

            public void onNewGroupConversation(String conversationID, List<String> userID) {
            }

            public void onNewDirectConversation(String conversationID, String userID) {
            }

            public void onNewAuthenticationToken(String userID, String token, String conversationID) {
            }

            public void onNewActivityEntry(String circuitUserId, String extractAfter) {
            }

            public void onShowStatsRequest(String circuitUserId) {
            }

            public void onShowAlarmRequest(String circuitUserId) {
            }

            public void onShowProfileRequest(String circuitUserId) {
            }

            public void onStartCombatMode(String circuitUserId, int minutes, String conversationId) {
            }

            public void onStopCombatMode(String circuitUserId, String extractAfter, String conversationId) {
            }
        };
    }

    public void setCircuitEventListener(CircuitEventListener listener) {
        this.listener = listener;
    }

    public List<UserData> getAllFitbitUsers() {
        List<UserData> result = new LinkedList<UserData>();
        WSMessage conversations = client.conversation().getConversations(userId, 0L, Direction.AFTER, Integer.MAX_VALUE, 2);

        List<Conversation> conversationsList = null;
        if (conversations.getResponse().getCode() == ReturnCode.NO_RESULT) {
            conversationsList = Collections.<Conversation> emptyList();
        } else {
            conversationsList = conversations.getResponse().getConversation().getGetConversations().getConversationsList();
        }

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
                    // conversation.getParticipantsList().remove(new Participant(userId));
                    for (Participant participant : conversation.getParticipantsList()) {
                        if (participant.getUserId().equals(userId)) {
                            conversation.getParticipantsList().remove(participant);
                            break;
                        }
                    }

                    LOGGER.info(String.format(
                            "Found fitbit user configuration in conversation '%s' with credentials fitbitUserId='%s' accessToken='%s' accessTokenSecret='%s'",
                            item.getConvId(), fitBitUserId, accessToken, accessTokenSecret));

                    UserData data = new UserData(conversation.getParticipantsList().get(0).getUserId(), fitBitUserId, item.getConvId(), accessToken,
                            accessTokenSecret);
                    result.add(data);
                }
            }
        }

        return result;
    }

    public void createWelcomeTextItem(String conversationId) {
        LOGGER.info("Create Welcome message in conversation " + conversationId);

        StringBuilder builder = new StringBuilder();
        builder.append("<i>");
        builder.append("To connect your fitbit account to circuit I need some information.");
        builder.append("</i>");

        builder.append("<br>");

        builder.append("<b>");
        builder.append("Step One: Please send me your fitbit user with the follwing message");
        builder.append("</b>");

        builder.append("<br><br>");

        builder.append("<div style=\"font-size: 12px;\" align=\"center\">");
        builder.append("fitbit user YOUR_USERID");
        builder.append("</div>");

        client.conversation().addTextItem(conversationId, STANDARD_SUBJECT, builder.toString(), TextItem.ContentType.RICH, null, null, null);
    }

    public void createGroupWelcomeTextItem(String conversationId) {
        LOGGER.info("Create Welcome message in conversation " + conversationId);

        StringBuilder builder = new StringBuilder();
        builder.append("<i>");
        builder.append("You want to have a challenge with the already registered conversation participants?");
        builder.append("</i>");

        builder.append("<br>");

        builder.append("Type ");
        builder.append("<b>");
        builder.append("fitbit challenge mode TIME");
        builder.append("</b>");
        builder.append(" to start your competition.");
        builder.append("<br><br>");

        builder.append("For example:");
        builder.append("<br>");
        builder.append("fitbit challenge mode 10m will compare the steps within the next 10 minutes");

        client.conversation().addTextItem(conversationId, STANDARD_SUBJECT, builder.toString(), TextItem.ContentType.RICH, null, null, null);
    }

    public void createURLTextItem(String conversationId, String url) {
        LOGGER.info("Create URL " + url + " text message in conversation " + conversationId);

        StringBuilder builder = new StringBuilder();
        builder.append("<b>");
        builder.append("Step Two: Please click the link");
        builder.append("</b>");

        builder.append("<br><br>");

        builder.append(String.format("<a href=\"%s\">%s</a>", url, url));
        builder.append("<br><br>");

        builder.append("<b>");
        builder.append("and follow the instruction. Afterwards come back and post your token with the follwing message");
        builder.append("</b>");

        builder.append("<br><br>");
        builder.append("<div style=\"font-size: 12px;\" align=\"center\">");
        builder.append("fitbit token YOUR_TOKEN");
        builder.append("</div>");

        client.conversation().addTextItem(conversationId, STANDARD_SUBJECT, builder.toString(), TextItem.ContentType.RICH, null, null, null);
    }

    public void saveUserCredentials(String conversationId, String fitbitUserId, String accessToken, String accessTokenSecret) {
        LOGGER.info(String.format(
                "Create text item in conversation '%s' with fitbit user credentials fitbitUserId='%s' accessToken='%s' accessTokenSecret='%s'", conversationId,
                fitbitUserId, accessToken, accessTokenSecret));

        StringBuilder builder = new StringBuilder();
        builder.append("<b>");
        builder.append("Great your fitbit account is now connected to your circuit acccount");
        builder.append("</b>");

        builder.append("<br>");
        builder.append("<br>");

        builder.append("accessToken=");
        builder.append("'");
        builder.append(accessToken);
        builder.append("'");

        builder.append("<br>");

        builder.append("accessTokenSecret=");
        builder.append("'");
        builder.append(accessTokenSecret);
        builder.append("'");

        builder.append("<br>");

        builder.append("fitBitUserId=");
        builder.append("'");
        builder.append(fitbitUserId);
        builder.append("'");

        client.conversation().addTextItem(conversationId, STANDARD_SUBJECT, builder.toString(), TextItem.ContentType.RICH, null, null, null);
    }

    public void createTextItem(String conversationId, String text) {
        LOGGER.info(String.format("Create text item with text '%s' in conversation '%s'", conversationId, text));
        client.conversation().addTextItem(conversationId, STANDARD_SUBJECT, text, TextItem.ContentType.RICH, null, null, null);

    }

    public void createDirectConversation(String userId) {
        LOGGER.info(String.format("Create direct conversation with user  '%s'", userId));
        client.conversation()
                .create(Arrays.asList(new Participant[] { new Participant(userId) }), ConversationType.DIRECT, "FitBit private instructions", null);

    }

    public void eventReceived(WSMessage message) {
        Event e = message.getEvent();
        if (e.getType() == ContentType.CONVERSATION && e.getConversation() != null && e.getConversation().getCreate() != null) {

            CreateConversationEvent create = e.getConversation().getCreate();
            // createWelcomeTextItem(create.getConversation().getConvId());
            // Remove fitbit user
            for (Participant participant : create.getConversation().getParticipantsList()) {
                if (participant.getUserId().equals(userId)) {
                    create.getConversation().getParticipantsList().remove(participant);
                    break;
                }
            }

            if (e.getConversation().getCreate().getConversation().getType() == ConversationType.DIRECT) {

                listener.onNewDirectConversation(create.getConversation().getConvId(), create.getConversation().getParticipantsList().get(0).getUserId());
            } else {

                ArrayList<String> users = new ArrayList<String>(create.getConversation().getParticipantsList().size());
                for (Participant participant : create.getConversation().getParticipantsList()) {
                    users.add(participant.getUserId());
                }
                listener.onNewGroupConversation(create.getConversation().getConvId(), users);
            }

        } else if (e.getType() == ContentType.CONVERSATION && e.getConversation() != null && e.getConversation().getAddItem() != null
                && e.getConversation().getAddItem().getItem().getType() == ConversationItemType.SYSTEM
                && e.getConversation().getAddItem().getItem().getSystem().getType() == SystemItem.ContentType.PARTICIPANT_ADDED) {

            for (Participant p : e.getConversation().getAddItem().getItem().getSystem().getAffectedParticipantsList()) {
                if (p.getUserId().equals(userId)) {
                    WSMessage conversationMsg = client.conversation().getConversationById(e.getConversation().getConvId());
                    List<Participant> participants = conversationMsg.getResponse().getConversation().getGetConversationById().getConversation()
                            .getParticipantsList();

                    ArrayList<String> users = new ArrayList<String>(participants.size());
                    for (Participant participant : participants) {
                        users.add(participant.getUserId());
                    }
                    listener.onNewGroupConversation(e.getConversation().getConvId(), users);
                    break;
                }
            }

        } else if (EventType.ADD_ITEM.equals(message.getEvent().getConversation().getType())) {

            if (!ignoreItem(message.getEvent().getConversation().getAddItem().getItem())) {

                if (ConversationItemType.TEXT.equals(message.getEvent().getConversation().getAddItem().getItem().getType())) {
                    String command = message.getEvent().getConversation().getAddItem().getItem().getText().getContent();
                    if (isCommand(command)) {

                        String creatorId = message.getEvent().getConversation().getAddItem().getItem().getCreatorId();
                        for (Command myCommand : commands) {
                            if (myCommand.match(command)) {
                                LOGGER.info("Process command: " + command);
                                ProcesssContext context = new ProcesssContext(creatorId, command);
                                context.conversationId = message.getEvent().getConversation().getConvId();
                                myCommand.processs(context);
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

    private String normalize(String result) {
        if (result.startsWith("#160;")) {
            result = result.substring("#160;".length());
        } else if (result.startsWith("&#160;")) {
            result = result.substring("&#160;".length());
        }
        return result;
    }

    private void initCommands() {
        commands.add(new Command("token") {
            @Override
            public void processs(ProcesssContext context) {
                String result = extractAfter(context.command).trim();
                result = normalize(result);
                listener.onNewAuthenticationToken(context.circuitUserId, result, context.conversationId);
            }

        });

        commands.add(new Command("food") {
            @Override
            public void processs(ProcesssContext context) {
                listener.onNewFoodEntry(context.circuitUserId, extractAfter(context.command));
            }

        });

        commands.add(new Command("activity") {
            @Override
            public void processs(ProcesssContext context) {
                listener.onNewActivityEntry(context.circuitUserId, extractAfter(context.command));
            }

        });

        commands.add(new Command("show stats") {
            @Override
            public void processs(ProcesssContext context) {
                listener.onShowStatsRequest(context.circuitUserId);
            }

        });

        commands.add(new Command("show alarm") {
            @Override
            public void processs(ProcesssContext context) {
                listener.onShowAlarmRequest(context.circuitUserId);
            }

        });

        commands.add(new Command("show profile") {
            @Override
            public void processs(ProcesssContext context) {
                listener.onShowProfileRequest(context.circuitUserId);
            }

        });

        commands.add(new Command("user") {
            @Override
            public void processs(ProcesssContext context) {
                String user = extractAfter(context.command);
                user = normalize(user.trim());

                listener.onNewFitbitUserId(context.circuitUserId, user, context.conversationId);
            }

        });

        commands.add(new Command("challenge mode") {
            @Override
            public void processs(ProcesssContext context) {
                String command = extractAfter(context.command);
                int minutes = 0;

                if (!command.isEmpty()) {
                    String extract = command.trim();
                    String[] split = extract.split("m");
                    if (split != null && split.length >= 1) {
                        minutes = Integer.valueOf(split[0]).intValue();
                        LOGGER.info("Extracted {} minutes for new combat in conversation", minutes);
                    }

                }
                listener.onStartCombatMode(context.circuitUserId, minutes, context.conversationId);
            }

        });

        commands.add(new Command("challenge stop") {
            @Override
            public void processs(ProcesssContext context) {
                listener.onStopCombatMode(context.circuitUserId, extractAfter(context.command), context.conversationId);
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
            String start = prefix + " " + this.command;

            String result = start.length() == command.length() ? "" : command.substring(start.length());
            return result;
        }

        public abstract void processs(ProcesssContext context);
    }

    public String getName(String circuitUserId) {
        return client.user().getUser(circuitUserId).getResponse().getUser().getGetById().getUser().getDisplayName();
    }

    public String getCircuitUserId() {
        return this.userId;
    }
}
