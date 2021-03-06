package com.cycos.circuit;

import java.util.ArrayList;
import java.util.List;

public class UserData {
    private String userID = null;
    private String conversationID = null;
    private String fitbitUserId = null;
    private String accessToken = null;
    private String accessTokenSecret = null;
    private List<String> groupConversations = new ArrayList<String>();
    private int steps = 0;

    public UserData(String userID, String fitbitUserId, String conversationID, String accessToken, String accessTokenSecret) {
        this.setUserID(userID);
        this.setFitbitUserId(fitbitUserId);
        this.setConversationID(conversationID);
        this.setAccessToken(accessToken);
        this.setAccessTokenSecret(accessTokenSecret);
    }

    public void readAuthFromFile() {
        ConfigHandler config = new ConfigHandler();
        config.load();
        setAccessToken(config.get("accessToken"));
        setAccessTokenSecret(config.get("accessTokenSecret"));
    }

    public void writeAuthToFile() {
        ConfigHandler config = new ConfigHandler();
        config.set("accessToken", accessToken);
        config.set("accessTokenSecret", accessTokenSecret);
    }

    public void setSteps(int steps) {
        this.steps = steps;
    }

    public int getSteps() {
        return steps;
    }

    public String getConversationID() {
        return conversationID;
    }

    public void setConversationID(String conversationID) {
        this.conversationID = conversationID;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getAccessTokenSecret() {
        return accessTokenSecret;
    }

    public void setAccessTokenSecret(String accessTokenSecret) {
        this.accessTokenSecret = accessTokenSecret;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getFitbitUserId() {
        return fitbitUserId;
    }

    public void setFitbitUserId(String fitbitUserId) {
        this.fitbitUserId = fitbitUserId;
    }

    public void addGroupConversation(String convId) {
        groupConversations.add(convId);
    }

    public void removeGroupConversation(String convId) {
        groupConversations.remove(convId);
    }

    public List<String> getGroupConversations() {
        return groupConversations;
    }
}
