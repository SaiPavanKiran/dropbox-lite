//we can directly use the blueprints for such type of simple functions -- just for practice
package org.rspk.dropbox_lite.lambdas;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Map;

public class PeriodicWebsiteChecking implements RequestHandler<Map<String, Object>, Object> {

    private static final String SITE = System.getenv("site");
    private static final String EXPECTED = System.getenv("expected");

    private boolean validate(String res) {
        if (EXPECTED == null || res == null) {
            return false;
        }
        return res.contains(EXPECTED);
    }

    @Override
    public Object handleRequest(Map<String, Object> event, Context context) {
        // Extract time safely from the CloudWatch/EventBridge event payload
        String eventTime = (event != null && event.containsKey("time")) ? event.get("time").toString() : "N/A";

        System.out.println("Checking " + SITE + " at " + eventTime + "..."); // prints to cloud watch

        try {
            StringBuilder responseContent = getStringBuilder();

            // Validate response
            if (!validate(responseContent.toString())) {
                throw new RuntimeException("Validation failed");
            }

            System.out.println("Check passed!");
            return eventTime;

        } catch (Exception e) {
            System.out.println("Check failed!");
            throw new RuntimeException(e);
        } finally {
            System.out.println("Check complete at " + LocalDateTime.now());
        }
    }

    private static StringBuilder getStringBuilder() throws IOException {
        URL url = new URL(SITE);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("User-Agent", "AWS Lambda");
        connection.setRequestMethod("GET");

        StringBuilder responseContent = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                responseContent.append(line);
            }
        }
        return responseContent;
    }
}