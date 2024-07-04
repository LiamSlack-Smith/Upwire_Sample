package processing.terminal;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import common.Utils;
import dao.auction.IAuctionDao;
import dao.chatGPTSession.SessionMessage;
import dao.terminal.ITerminalDao;
import dao.terminal.Terminal;
import org.bson.types.ObjectId;
import requests.CreateTerminalRequest;
import requests.UpdateTerminalRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

public class TerminalProcessor implements ITerminalProcessor {

    private final ITerminalDao terminalDao;

    private final IAuctionDao auctionDao;

    @Inject
    public TerminalProcessor(ITerminalDao terminalDao, IAuctionDao auctionDao) {
        this.terminalDao = terminalDao;
        this.auctionDao = auctionDao;
    }

    @Override
    public List<Terminal> getAll() {
        return terminalDao.getAll();
    }

    @Override
    public List<Terminal> getTerminals() {
        return terminalDao.getAll();
    }

    @Override
    public void save(Terminal terminal){
        terminalDao.insertOrUpdate(terminal);
    }

    @Override
    public Terminal getTerminal(String terminalId) { return terminalDao.getById(terminalId); }

    @Override
    public Terminal getTerminalByNumber(String terminalNumber) { return terminalDao.getByTerminalNumber(terminalNumber); }

    @Override
    public List<Terminal> getAll(String symbol) {
        return terminalDao.getAll(symbol);
    }

    @Override
    public List<Terminal> getAllActive(Date expiryDate) {
        return terminalDao.getAllActive(expiryDate);
    }

    @Override
    public JsonObject GPTSearch(String message) throws IOException {
        // Set up the connection
        URL url = new URL("https://api.openai.com/v1/chat/completions");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "");

        // Prepare and send the request body
        String requestBody = constructRequestBody(message);
        conn.setDoOutput(true);
        try (OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream())) {
            out.write(requestBody);
        }

        // Receive and process the response
        StringBuilder response = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        }

        String manipulatedResponse = getResponse(response.toString());

        JsonObject newJson = gson.fromJson(formatResponseContent(manipulatedResponse), JsonObject.class);

        System.out.println("Response body: " + newJson);

        return newJson;
    }

    private String constructRequestBody(String message) {
        String template = "{ \"model\": \"gpt-3.5-turbo-16k\", \"temperature\": 0.5, \"messages\": [%s]}";
        String messages = String.format("\"role\": \"user\", \"content\": \"%s\"", message);
        return String.format(template, messages);
    }

    private JsonObject getResponse(String jsonStr) {
        Gson gson = new Gson();
        JsonObject responseJson = gson.fromJson(jsonStr, JsonObject.class);
        String responseContent = responseJson.getAsJsonArray("choices").get(0).getAsJsonObject().get("message").getAsString();
        return gson.fromJson(responseContent, JsonObject.class);
    }

    private String formatResponseContent(String responseContent) {
        String[] fields = {"country", "location", "name", "contractDate", "productGrades", "transportModes", "region", "state", "message"};
        for (String field : fields) {
            if (responseContent.contains(field)) {
                responseContent = replaceSpacesWithUnderscores(responseContent, field);
            }
        }
        return responseContent;
    }

    private String replaceSpacesWithUnderscores(String responseContent, String fieldName) {
        int start = responseContent.indexOf(fieldName + ":") + (fieldName + ": ").length();
        int end = responseContent.indexOf("}", start);
        if (end == -1) end = responseContent.length();

        String fieldContent = responseContent.substring(start, end).replace(" ", "_");
        return responseContent.substring(0, start) + fieldContent + responseContent.substring(end);
    }

    @Override
    public String elasticSearch(JsonObject response) throws Exception {
        // Setup connection
        String url = "ELASTIC_SEARCH_URL";
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod("GET");

        // Set basic auth header
        String username = "";
        String password = "";
        String auth = username + ":" + password;
        String authHeader = "Basic " + new String(Base64.getEncoder().encode(auth.getBytes()));
        con.setRequestProperty("Authorization", authHeader);
        con.setRequestProperty("Content-Type", "application/json");

        // Build request body dynamically
        String requestBody = "{ \"size\" : " + (response.has("size") ? response.get("size").getAsString() : "10") + ", \"query\": { \"bool\" : { \"must\" : [";

        // Keys and Fields for product grades, transport modes, exclusive products, and exclusive transport modes
        String[] keys = {"productGrades", "transportModes", "exclusiveProductGrades", "exclusiveTransportModes"};
        String[] fields = {"products", "transportModes", "exclusiveProducts", "exclusiveTransportModes"};

        for (int i = 0; i < keys.length; i++) {
            if (response.has(keys[i])) {
                String terms = response.get(keys[i]).getAsJsonArray().stream()
                        .map(item -> "\"" + item.getAsString().replace("_", " ") + "\"")
                        .collect(Collectors.joining(", "));
                if (!terms.isEmpty()) {
                    requestBody += "{ \"terms\": {\"" + fields[i] + "\" : [" + terms + "]}}, ";
                }
            }
        }

        // Match queries for fields like name, country, etc.
        String[] matchFields = {"name", "country", "region", "state", "location"};
        for (String field : matchFields) {
            if (response.has(field)) {
                String matchQuery = String.format("{ \"match\": { \"%s\" : { \"query\" : \"%s\", \"analyzer\": \"standard\" } } }, ", field, response.get(field).getAsString().replace("_", " "));
                requestBody += matchQuery;
            }
        }

        // Remove the last comma and space if necessary
        if (requestBody.endsWith(", ")) {
            requestBody = requestBody.substring(0, requestBody.length() - 2);
        }

        requestBody += "] } } }"; // Closing braces for the JSON body

        // Send the request
        con.setDoOutput(true);
        try (OutputStreamWriter out = new OutputStreamWriter(con.getOutputStream(), "UTF-8")) {
            out.write(requestBody);
        }

        // Get the response
        StringBuilder responseBuffer = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                responseBuffer.append(inputLine);
            }
        }

        System.out.println("Response: " + responseBuffer.toString());
        return responseBuffer.toString();
    }

    @Override
    public String terminalStatSearch(JsonObject response) throws Exception {
        String url = "ELASTIC_SEARCH_URL";
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod("GET");

        // Set authorization header
        String username = "";
        String password = "";
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
        String authHeader = "Basic " + new String(encodedAuth);
        con.setRequestProperty("Authorization", authHeader);
        con.setRequestProperty("Content-Type", "application/json");

        // Construct the request body dynamically
        String requestBody = "{ \"size\" : " + (response.has("size") ? response.get("size").getAsString() : "10") +
                ", \"query\": { \"bool\" : { \"must\" : [";

        // Handle match queries for state, region, location, name, country
        String[] matchFields = {"state", "region", "location", "name", "country"};
        for (String field : matchFields) {
            if (response.has(field)) {
                String matchQuery = String.format("{ \"match\": { \"%s\" : { \"query\" : \"%s\", \"analyzer\": \"standard\" } } }, ",
                        field, response.get(field).getAsString().replace("_", " "));
                requestBody += matchQuery;
            }
        }

        // Handle terms queries for productGrades and transportModes
        String[] fields = {"productGrades", "transportModes"};
        String[] keys = {"products", "transportModes"};
        for (int i = 0; i < fields.length; i++) {
            if (response.has(fields[i])) {
                JsonArray jsonArray = response.getAsJsonArray(fields[i]);
                String termsQuery = jsonArray.size() > 0 ? "{ \"terms\": {\"" + keys[i] + "\" : [" : "";
                for (JsonElement element : jsonArray) {
                    termsQuery += "\"" + element.getAsString().replace("_", " ") + "\", ";
                }
                if (!termsQuery.isEmpty()) {
                    termsQuery = termsQuery.substring(0, termsQuery.length() - 2) + "]}}, ";
                    requestBody += termsQuery;
                }
            }
        }

        // Remove the last comma and space if necessary
        if (requestBody.endsWith(", ")) {
            requestBody = requestBody.substring(0, requestBody.length() - 2);
        }

        requestBody += "] } } }, \"aggs\": { \"trm_count\": { \"value_count\": { \"field\": \"symbol.raw\" } }, " +
                "\"loc_count\": { \"terms\": { \"field\": \"location.raw\" } }, " +
                "\"prod_count\": { \"terms\": { \"field\": \"products\" } }, " +
                "\"mode_count\": { \"terms\": { \"field\": \"transportModes\" } } } }";

        // Print and send the request
        System.out.println(requestBody);
        con.setDoOutput(true);
        try (OutputStreamWriter out = new OutputStreamWriter(con.getOutputStream(), "UTF-8")) {
            out.write(requestBody);
        }

        // Read and return the response
        StringBuilder searchResponse = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                searchResponse.append(inputLine);
            }
        }

        System.out.println("Response: " + searchResponse.toString());
        return searchResponse.toString();
    }

    private JsonObject getLocation(String responseString){
        JsonObject locationObject = new JsonObject();

        Gson gson = new Gson();
        JsonObject responseJson = gson.fromJson(responseString, JsonObject.class);

        locationObject = responseJson.getAsJsonObject("hits").getAsJsonArray("hits").get(0).getAsJsonObject().getAsJsonObject("_source").getAsJsonObject("pin").getAsJsonObject("location");

        System.out.println(locationObject);

        return locationObject;
    }

    public String getTerminalsInRange(JsonObject location, String range) throws Exception {
        String url = "ELASTIC_SEARCH_URL";
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        con.setRequestMethod("GET");

        // Set the authorization header
        String username = "";
        String password = "";
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
        String authHeader = "Basic " + new String(encodedAuth);
        con.setRequestProperty("Authorization", authHeader);

        con.setRequestProperty("Content-Type", "application/json");

        String query = "{" +
                "  \"size\": 10," +
                "  \"query\": {" +
                "    \"bool\": {" +
                "      \"must\": {" +
                "        \"match_all\": {}" +
                "      }," +
                "      \"filter\": {" +
                "        \"geo_distance\": {" +
                "          \"distance\": \"" + range + "\"," +
                "          \"pin.location\": {" +
                "            \"lat\": " + location.get("lat").getAsString() + "," +
                "            \"lon\": " + location.get("lon").getAsString() +
                "          }" +
                "        }" +
                "      }" +
                "    }" +
                "  }" +
                "}";

        con.setDoOutput(true);
        con.getOutputStream().write(query.getBytes("UTF-8"));

        // Get the response
        int responseCode = con.getResponseCode();
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer searchResponse = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            searchResponse.append(inputLine);
        }
        in.close();

        String responseString = searchResponse.toString();

        return responseString;
    }

    @Override
    public ObjectId create(CreateTerminalRequest createData) {

        Terminal terminal = new Terminal();

        Utils.UpdateModel(terminal, createData);
        terminalDao.insertOrUpdate(terminal);
        return terminal.getId();
    }

    @Override
    public void update(UpdateTerminalRequest updateData) {
        Terminal terminal = terminalDao.getById(updateData.id);

        if (terminal != null) {
            Utils.UpdateModel(terminal, updateData);
            terminalDao.insertOrUpdate(terminal);
        }
    }

    @Override
    public void remove(String id) {
        Terminal terminal = terminalDao.getById(id);
        if (terminal != null) {
            terminalDao.remove(terminal);
        }
    }
}
