package com.cycos.circuit;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FitbitUsers {
    private static final Logger LOGGER = LoggerFactory.getLogger(FitbitUsers.class);
    private Map<String, UserData> users = null;

    public FitbitUsers() {
        users = new HashMap<String, UserData>();
    }

    public void addAll(List<UserData> newUsers) {
        for (UserData user : newUsers) {
            if ((user.getAccessToken() == null || user.getAccessToken().equals("null"))
                    || (user.getAccessTokenSecret() == null || user.getAccessTokenSecret().equals("null"))
                    || (user.getFitbitUserId() == null || user.getFitbitUserId().equals("null"))
                    || (user.getConversationID() == null || user.getConversationID().equals("null"))) {
                LOGGER.info("Uncomplete user: " + user.getFitbitUserId());
            } else {
                add(user);
                LOGGER.info("Added fitbit user: " + user.getFitbitUserId());
            }

        }
    }

    public Collection<UserData> getUsers() {
        return users.values();
    }

    public void add(UserData user) {
        users.put(user.getUserID(), user);
    }

    public void remove(UserData user) {
        users.remove(user.getUserID());
    }

    public UserData get(String userID) {
        return users.get(userID);
    }
}
