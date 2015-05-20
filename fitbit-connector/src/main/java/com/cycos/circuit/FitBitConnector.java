package com.cycos.circuit;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fitbit.api.FitbitAPIException;
import com.fitbit.api.client.FitbitAPIEntityCache;
import com.fitbit.api.client.FitbitApiClientAgent;
import com.fitbit.api.client.FitbitApiCredentialsCache;
import com.fitbit.api.client.FitbitApiCredentialsCacheMapImpl;
import com.fitbit.api.client.FitbitApiEntityCacheMapImpl;
import com.fitbit.api.client.FitbitApiSubscriptionStorage;
import com.fitbit.api.client.FitbitApiSubscriptionStorageInMemoryImpl;
import com.fitbit.api.client.LocalUserDetail;
import com.fitbit.api.client.service.FitbitAPIClientService;
import com.fitbit.api.common.model.activities.Activities;
import com.fitbit.api.common.model.devices.Device;
import com.fitbit.api.common.model.foods.NutritionalValuesEntry;
import com.fitbit.api.common.model.sleep.SleepLog;
import com.fitbit.api.common.model.user.UserInfo;
import com.fitbit.api.model.APICollectionType;
import com.fitbit.api.model.APIResourceCredentials;
import com.fitbit.api.model.FitbitUser;

public class FitBitConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(FitBitConnector.class);

    private FitbitAPIEntityCache entityCache = new FitbitApiEntityCacheMapImpl();
    private FitbitApiCredentialsCache credentialsCache = new FitbitApiCredentialsCacheMapImpl();
    private FitbitApiSubscriptionStorage subscriptionStore = new FitbitApiSubscriptionStorageInMemoryImpl();
    private FitbitAPIClientService<FitbitApiClientAgent> apiClientService = null;
    private String apiBaseUrl = null;
    private String fitbitSiteBaseUrl = null;
    private String clientConsumerKey = null;
    private String clientSecret = null;
    private FitbitData data = null;
    private CircuitConnector circuit = null;
    private FitbitUsers users = null;
    private FitbitUsers tempUsers = null;
    private HashSet<String> combatConversations = new HashSet<String>();
    private HashMap<String, UserResult> combat = new HashMap<String, UserResult>();
    private HashMap<String, CombatConversation> multiCombatConversation = new HashMap<String, CombatConversation>();

    public FitBitConnector(final CircuitConnector circuit) {
        users = new FitbitUsers();
        tempUsers = new FitbitUsers();
        data = new FitbitData();
        this.circuit = circuit;
        users.addAll(circuit.getAllFitbitUsers());
        this.circuit.setCircuitEventListener(new CircuitEventListener() {

            public void onNewFoodEntry(String userId, String food) {
                UserData user = users.get(userId);
                LocalUserDetail ud = authenticateUser(user);
                addFood(ud, food);
            }

            public void onNewFitbitUserId(String userId, String fitbitUserId, String conversationID) {
                UserData user = tempUsers.get(userId);
                if (user == null) {
                    user = new UserData(userId, null, conversationID, null, null);
                    tempUsers.add(user);
                }
                user.setFitbitUserId(fitbitUserId);
                LocalUserDetail ud = new LocalUserDetail(user.getFitbitUserId());
                try {
                    String url = apiClientService.getResourceOwnerAuthorizationURL(ud, "");
                    circuit.createURLTextItem(user.getConversationID(), url);
                } catch (FitbitAPIException e) {
                    LOGGER.error("New error", e);
                    ;
                }
            }

            public void onNewGroupConversation(String conversationID, List<String> userIds) {
                LOGGER.info("Fitbit user was added to a group conversation. Create direct conversation with all non fitbit members.");
                for (String userId : userIds) {
                    // Ignore fitBit user
                    if (userId.equals(circuit.getCircuitUserId())) {
                        continue;
                    }

                    if (users.get(userId) == null) {
                        LOGGER.info(userId + " has no account. Create new direct conversation");
                        circuit.createDirectConversation(userId);
                    } else {
                        UserData userData = users.get(userId);
                        userData.addGroupConversation(conversationID);
                    }
                }
                circuit.createGroupWelcomeTextItem(conversationID);
            }

            public void onNewDirectConversation(String conversationID, String userID) {
                UserData user = new UserData(userID, null, conversationID, null, null);
                tempUsers.add(user);
                circuit.createWelcomeTextItem(conversationID);
            }

            public void onNewAuthenticationToken(String userID, String token, String conversationID) {
                UserData user = tempUsers.get(userID);
                LocalUserDetail ud = preAuthenticateUser(user);
                createAccessToken(ud, token, user);
                circuit.saveUserCredentials(user.getConversationID(), user.getFitbitUserId(), user.getAccessToken(), user.getAccessTokenSecret());
                users.add(user);

                UserData userData = tempUsers.get(userID);
                if (userData != null) {
                    tempUsers.remove(user);
                }

                showProfile(ud, user);
                showDevice(ud, user);
                sendDailyStatistics(ud, user);
            }

            public void onNewActivityEntry(String circuitUserId, String extractAfter) {
                LOGGER.info("Nothing to demo");

            }

            public void onShowStatsRequest(String circuitUserId) {
                UserData userData = users.get(circuitUserId);
                LocalUserDetail ud = authenticateUser(userData);
                sendDailyStatistics(ud, userData);
            }

            public void onShowAlarmRequest(String circuitUserId) {
                // TODO Auto-generated method stub

            }

            public void onShowProfileRequest(String circuitUserId) {
                UserData userData = users.get(circuitUserId);
                LocalUserDetail ud = authenticateUser(userData);
                showProfile(ud, userData);
            }

            public void onStartCombatMode(final String circuitUserId, final int minutes, final String conversationId) {
                circuit.createTextItem(conversationId, "Let' get ready to rumble");
                final AtomicInteger counter = new AtomicInteger(3);
                final Timer timer = new Timer();
                final Timer cleanUpTimer = new Timer();
                final Timer summaryTimer = new Timer();
                timer.scheduleAtFixedRate(new TimerTask() {

                    @Override
                    public void run() {

                        circuit.createTextItem(conversationId, "" + counter.getAndDecrement());
                        if (counter.get() <= 0) {
                            timer.cancel();

                            if (!combatConversations.contains(conversationId)) {
                                combatConversations.add(conversationId);
                            } else {
                                LOGGER.warn("Combat mode is already enabled for conversation {}", conversationId);
                            }
                            summaryTimer.scheduleAtFixedRate(new TimerTask() {

                                @Override
                                public void run() {
                                    createIntermediateSummary(conversationId);
                                }

                            }, 0, 2 * 60 * 1000);
                            cleanUpTimer.schedule(new TimerTask() {

                                @Override
                                public void run() {
                                    createEndSummary(conversationId);
                                    cleanUpTimer.cancel();
                                    summaryTimer.cancel();

                                }
                            }, minutes * 60 * 1000);

                        }
                    }

                }, 0, 1000);

            }

            public void onStopCombatMode(String circuitUserId, String extractAfter, String conversationId) {
                combatConversations.clear();
                combat.clear();
            }
        });
    }

    public void init() {
        apiBaseUrl = data.getApiBaseUrl();
        fitbitSiteBaseUrl = data.getFitbitSiteBaseUrl();
        clientConsumerKey = data.getClientConsumerKey();
        clientSecret = data.getClientSecret();

        apiClientService = new FitbitAPIClientService<FitbitApiClientAgent>(new FitbitApiClientAgent(apiBaseUrl, fitbitSiteBaseUrl, credentialsCache),
                clientConsumerKey, clientSecret, credentialsCache, entityCache, subscriptionStore);
    }

    public void addUser(UserData userData) {
        users.add(userData);
        LocalUserDetail ud = new LocalUserDetail(userData.getFitbitUserId());
        String url;
        try {
            url = apiClientService.getResourceOwnerAuthorizationURL(ud, "");
            APIResourceCredentials creds = apiClientService.getResourceCredentialsByUser(ud);
            if (userData.getAccessToken() == null && userData.getAccessTokenSecret() == null) {
                LOGGER.info("Pease open this URL and enable the application: " + url);
                LOGGER.info("Enter PIN:");
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                String pin = br.readLine();
                LOGGER.info("PIN: " + pin);
                createAccessToken(ud, pin, userData);
            } else {
                creds.setAccessToken(userData.getAccessToken());
                creds.setAccessTokenSecret(userData.getAccessTokenSecret());
            }

            UserInfo userInfo = apiClientService.getClient().getUserInfo(ud);
            LOGGER.info("Welcome " + userInfo.getDisplayName());
        } catch (FitbitAPIException e) {
            LOGGER.info("FitBit error: " + e.getMessage());
            LOGGER.error("New error", e);
            ;
        } catch (IOException e) {
            LOGGER.info("IO error: " + e.getMessage());
            LOGGER.error("New error", e);
            ;
        }
    }

    public LocalUserDetail authenticateUser(UserData userData) {
        LocalUserDetail ud = new LocalUserDetail(userData.getFitbitUserId());
        try {
            String url = apiClientService.getResourceOwnerAuthorizationURL(ud, "");
        } catch (FitbitAPIException e) {
            LOGGER.error("New error", e);
        }
        APIResourceCredentials creds = apiClientService.getResourceCredentialsByUser(ud);
        if (userData.getAccessToken() != null) {
            creds.setAccessToken(userData.getAccessToken());
            creds.setAccessTokenSecret(userData.getAccessTokenSecret());
        }
        return ud;
    }

    public LocalUserDetail preAuthenticateUser(UserData userData) {
        LocalUserDetail ud = new LocalUserDetail(userData.getFitbitUserId());
        APIResourceCredentials creds = apiClientService.getResourceCredentialsByUser(ud);
        if (userData.getAccessToken() != null) {
            creds.setAccessToken(userData.getAccessToken());
            creds.setAccessTokenSecret(userData.getAccessTokenSecret());
        }
        return ud;
    }

    public void createAccessToken(LocalUserDetail ud, String pin, UserData userData) {
        try {
            APIResourceCredentials creds = apiClientService.getResourceCredentialsByUser(ud);
            creds.setTempTokenVerifier(pin);
            apiClientService.saveResourceCredentials(ud, creds);
            apiClientService.getTokenCredentials(ud);
            userData.setAccessToken(creds.getAccessToken());
            userData.setAccessTokenSecret(creds.getAccessTokenSecret());
        } catch (FitbitAPIException e) {
            LOGGER.error("New error", e);
            ;
        }
    }

    public void createSubscription(UserData userData) {
        LocalUserDetail ud = new LocalUserDetail(userData.getFitbitUserId());
        try {
            apiClientService.subscribe("2", ud, APICollectionType.activities, ud.getUserId());
        } catch (FitbitAPIException e) {
            LOGGER.error("New error", e);
            ;
        }
    }

    public void fetchUserSteps() {
        for (UserData user : users.getUsers()) {
            LocalUserDetail ud = authenticateUser(user);
            try {
                LocalDate date = LocalDate.now();
                LOGGER.info("Printing steps for: " + date.toString());
                Activities activities = apiClientService.getClient().getActivities(ud, FitbitUser.CURRENT_AUTHORIZED_USER, date);
                int stepsNew = activities.getSummary().getSteps();
                LOGGER.info("New number of steps: " + stepsNew + " at time " + LocalDate.now().toString() + " " + LocalTime.now().toString());
                analyzeUserSteps(user, stepsNew);
            } catch (FitbitAPIException e) {
                LOGGER.error("New error", e);
            }
        }
    }

    public void analyzeUserSteps(UserData user, int newSteps) {
        if (user.getSteps() == 0) {
            user.setSteps(newSteps);
            updateCombatUser(user, new Integer(0));
            LOGGER.info("No data");
            return;
        }
        if (newSteps > user.getSteps()) {
            int diffSteps = newSteps - user.getSteps();
            StringBuffer buff = new StringBuffer("In the last minute you walked ");
            buff.append(diffSteps);
            buff.append(" steps! Great, continue like this! :-)");
            publish(user, buff.toString(), new Integer(diffSteps), false);
            user.setSteps(newSteps);
        } else {
            if (newSteps < user.getSteps()) {
                publish(user, "Something is wrong with your tracker!", null, true);
            } else {
                publish(user, "Booooo! You did not walk any step in the last minute...don't you fear getting fat?", null, true);
            }
        }
    }

    private void publish(UserData user, String text, Integer diffSteps, boolean onlyPrivate) {
        circuit.createTextItem(user.getConversationID(), text);
        if (onlyPrivate) {
            return;
        }

        for (String convId : user.getGroupConversations()) {
            if (combatConversations.contains(convId)) {
                String newText = "Hey " + circuit.getName(user.getUserID()) + ": " + text;
                circuit.createTextItem(convId, newText);

                updateCombatUser(user, diffSteps);
            }
        }
    }

    private void updateCombatUser(UserData user, Integer diffSteps) {
        if (diffSteps == null) {
            return;
        }

        if (!combat.containsKey(user.getUserID())) {
            UserResult result = new UserResult();
            result.userId = user.getUserID();
            result.value = 0;
            combat.put(user.getUserID(), result);
        }
        UserResult result = combat.get(user.getUserID());
        result.value = new Integer(result.value.intValue() + diffSteps.intValue());

    }

    public void addFood(LocalUserDetail ud, String food) {
        LOGGER.info("Adding food");
        ArrayList<String> aList = new ArrayList<String>(Arrays.asList(food.split(",")));
        String fd = aList.get(0);
        ;
        String amount = aList.get(1);
        int cal = Integer.parseInt(aList.get(2));
        int unitId = 1;
        NutritionalValuesEntry nutritionalValuesEntry = new NutritionalValuesEntry();
        nutritionalValuesEntry.setCalories(cal);
        int mealTypeId;
        LocalDate date = LocalDate.now();
        Calendar calendar = Calendar.getInstance();
        int hours = calendar.get(Calendar.HOUR_OF_DAY);
        LOGGER.info("It's " + "o'clock. It must be ");
        if (hours >= 8 && hours <= 10) {
            mealTypeId = 1;
            LOGGER.info("breakfast.");
        } else if (hours > 12 && hours <= 17) {
            mealTypeId = 2;
            LOGGER.info("morning snack.");
        } else if (hours > 12 && hours <= 14) {
            mealTypeId = 3;
            LOGGER.info("lunch.");
        } else if (hours > 14 && hours <= 18) {
            mealTypeId = 4;
            LOGGER.info("afternoon snack.");
        } else if (hours > 17 && hours <= 21) {
            mealTypeId = 5;
            LOGGER.info("dinner.");
        } else {
            mealTypeId = 7;
            LOGGER.info("anytime.");
        }

        try {
            apiClientService.getClient().logFood(ud, fd, null, nutritionalValuesEntry, mealTypeId, unitId, amount, date);
        } catch (FitbitAPIException e) {
            LOGGER.error("New error", e);
            ;
        }
        LOGGER.info("Food: " + fd + " added with amount: " + amount + " and mealTypeId: " + mealTypeId);
    }

    public void showDevice(LocalUserDetail ud, UserData user) {
        try {
            StringBuffer summary = new StringBuffer();
            List<Device> devices = apiClientService.getClient().getDevices(ud);
            summary.append("Your device status:<br>");
            for (Device device : devices) {
                summary.append("<b>Device: </b>" + device.getId() + "<br>");
                summary.append("Type: " + device.getType() + "<br>");
                summary.append("Version: " + device.getDeviceVersion() + "<br");
                summary.append("Battery: " + device.getBattery() + "<br>");
                summary.append("Last sync: " + device.getLastSyncTime() + "<br>");
            }
        } catch (FitbitAPIException e) {
            LOGGER.error("New error", e);
        }
    }

    public void showProfile(LocalUserDetail ud, UserData user) {
        try {
            UserInfo userInfo = apiClientService.getClient().getUserInfo(ud);
            StringBuffer profile = new StringBuffer("Welcome to fitbit, " + userInfo.getDisplayName() + "<br>" + "<br>");
            profile.append("Your fitbit profile data:<br>");
            profile.append("<b>Name: </b>" + userInfo.getFullName() + "<br>");
            profile.append("<b>Gender: </b>" + userInfo.getGender() + "<br>");
            profile.append("<b>Height: </b>" + userInfo.getHeight() + "<br>");
            profile.append("<b>Date of birth: </b>" + userInfo.getDateOfBirth() + "<br>");
            circuit.createTextItem(user.getConversationID(), profile.toString());
        } catch (FitbitAPIException e) {
            LOGGER.error("New error", e);
            ;
        }
    }

    public void sendDailyStatistics(LocalUserDetail ud, UserData user) {
        LocalDate date = LocalDate.now();
        StringBuffer summary = new StringBuffer();
        summary.append("I examined your performance till now. Here it is:<br>");
        try {
            List<SleepLog> sleepLogs = apiClientService.getClient().getSleep(ud, FitbitUser.CURRENT_AUTHORIZED_USER, date).getSleepLogs();
            if (sleepLogs.size() > 0) {
                summary.append("<b>Sleep:<b><br>");
            }
            for (SleepLog sleepLog : sleepLogs) {
                int sleepDuration = (int) (sleepLog.getDuration() / 1000 / 60);
                summary.append("Duration: ").append(sleepDuration / 60).append(" hours");
                if (sleepDuration % 60 != 0) {
                    summary.append(" ").append(sleepDuration % 60).append(" minutes");
                }
                summary.append("<br>");
                int awakeningCount = sleepLog.getAwakeningsCount();
                summary.append("Awake Count: ").append(awakeningCount).append("<br>");
            }
            Activities activities = apiClientService.getClient().getActivities(ud, FitbitUser.CURRENT_AUTHORIZED_USER, date);
            int steps = activities.getSummary().getSteps();
            if (steps > 0) {
                summary.append("<b>Number of steps:</b> ").append(steps).append("<br>");
            }
            int activeMinutes = activities.getSummary().getFairlyActiveMinutes() + activities.getSummary().getLightlyActiveMinutes()
                    + activities.getSummary().getVeryActiveMinutes();
            int sedentaryMinutes = activities.getSummary().getSedentaryMinutes();
            summary.append("<b>Active Minutes:<b> ").append(activeMinutes).append("<br>");
            summary.append("<b>Inactive Minutes:</b> ").append(sedentaryMinutes).append("<br>");
        } catch (FitbitAPIException e) {
            LOGGER.error("New error", e);
            ;
        }
        circuit.createTextItem(user.getConversationID(), summary.toString());
    }

    private void generatePie(Map<String, Integer> map) {
        String title = "Our first challenge but not a personal one :-(";
        boolean legend = true;
        boolean tooltips = true;
        Locale urls = Locale.US;

        PieDataset dataset = createPieDataSet(map.entrySet());
        JFreeChart createPieChart = ChartFactory.createPieChart(title, dataset, legend, tooltips, urls);

        try {
            ChartUtilities.saveChartAsJPEG(new File("./src/main/resources/ChallengeResult.jpg"), createPieChart, 500, 300);
        } catch (IOException e) {
            e.printStackTrace();
            fail("Exception while saving chart as jpeg");
        }
    }

    private void createIntermediateSummary(String conversationId) {
        List<UserResult> results = new ArrayList<UserResult>(combat.values());

        if (!results.isEmpty()) {
            Collections.sort(results);
            Collections.reverse(results);

            StringBuilder builder = new StringBuilder();
            for (int cnt = 0; cnt < results.size(); cnt++) {
                UserResult myResult = results.get(cnt);
                builder.append(cnt + 1);
                builder.append(". ");
                builder.append(circuit.getName(myResult.userId));
                builder.append(" with ");
                builder.append(myResult.value);
                builder.append(" steps");
                builder.append("<br>");
            }
            circuit.createTextItem(conversationId, builder.toString());
        }
    }

    private void createEndSummary(String conversationId) {
        LOGGER.info("Clean up of {}", conversationId);

        // Remove conversation from list because competiotion time was reached
        if (combatConversations.contains(conversationId)) {
            combatConversations.remove(conversationId);
        }

        List<UserResult> results = new ArrayList<UserResult>(combat.values());
        Collections.sort(results);
        Collections.reverse(results);

        StringBuilder builder = new StringBuilder();
        builder.append("Challenge ended. The winner is ");
        builder.append("<b>");
        builder.append(circuit.getName(results.get(0).userId));
        builder.append("</b>");

        circuit.createTextItem(conversationId, builder.toString());

        HashMap<String, Integer> map = new HashMap<String, Integer>();
        for (UserResult result : results) {
            map.put(circuit.getName(result.userId), result.value);
        }
        LOGGER.info("Generate pie for converstion '{}' with '{}' users", conversationId, map.size());
        generatePie(map);
        combat.clear();
    }

    private static PieDataset createPieDataSet(Set<Entry<String, Integer>> data) {
        DefaultPieDataset result = new DefaultPieDataset();
        for (Entry<String, Integer> entry : data) {
            result.setValue(entry.getKey(), entry.getValue());
        }
        return result;
    }

    class UserResult implements Comparable<UserResult> {
        String userId;
        Integer value;

        public int compareTo(UserResult o) {
            return value.compareTo(o.value);
        }

    }

    class CombatConversation {
        final AtomicInteger counter = new AtomicInteger(3);
        final Timer timer = new Timer();
        final Timer cleanUpTimer = new Timer();
        final Timer summaryTimer = new Timer();
    }

}
