package uz.nchat.client.net;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Calls the server's REST API: registration, login and "who am I".
 * Methods block, so the UI calls them from a background thread.
 */
public class ApiClient {

    /** What the server returns after login or registration. */
    public record Session(String token, String username, String displayName) {
    }

    /** A code was sent to this (masked) number. */
    public record LinkStarted(String maskedPhone, long expiresInSeconds) {
    }

    /** A Nchat user found by phone. */
    public record Match(String username, String displayName) {
    }

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final String baseUrl;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    private final ObjectMapper json = new ObjectMapper();

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Session register(String username, String displayName, String password)
            throws IOException, InterruptedException, AuthException {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("username", username);
        body.put("displayName", displayName);
        body.put("password", password);
        JsonNode response = send(post("/api/auth/register", body));
        return toSession(response);
    }

    public Session login(String username, String password)
            throws IOException, InterruptedException, AuthException {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("username", username);
        body.put("password", password);
        return toSession(send(post("/api/auth/login", body)));
    }

    /** Checks a saved token. Returns the session with fresh profile data, or throws if the token is no longer valid. */
    public Session me(String token) throws IOException, InterruptedException, AuthException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/me"))
                .timeout(TIMEOUT)
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        JsonNode response = send(request);
        return new Session(token, response.path("username").asText(), response.path("displayName").asText());
    }

    /** Whether the signed-in user has linked a phone number. */
    public boolean isPhoneLinked(String token) throws IOException, InterruptedException, AuthException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/me"))
                .timeout(TIMEOUT)
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        return send(request).path("phoneLinked").asBoolean(false);
    }

    /** Asks the server to send a code to the number. The number is sent once and is not stored. */
    public LinkStarted startPhoneLink(String token, String phone)
            throws IOException, InterruptedException, AuthException {
        JsonNode response = send(post("/api/phone/start", Map.of("phone", phone), token));
        return new LinkStarted(response.path("maskedPhone").asText(), response.path("expiresInSeconds").asLong());
    }

    public void verifyPhone(String token, String code) throws IOException, InterruptedException, AuthException {
        send(post("/api/phone/verify", Map.of("code", code), token));
    }

    public void unlinkPhone(String token) throws IOException, InterruptedException, AuthException {
        send(HttpRequest.newBuilder(URI.create(baseUrl + "/api/phone"))
                .timeout(TIMEOUT)
                .header("Authorization", "Bearer " + token)
                .DELETE()
                .build());
    }

    /** Sends phone keys (never numbers) and returns which of them belong to Nchat users. */
    public List<Match> lookup(String token, List<String> phoneKeys)
            throws IOException, InterruptedException, AuthException {
        JsonNode response = send(post("/api/contacts/lookup", Map.of("phoneKeys", phoneKeys), token));
        List<Match> matches = new ArrayList<>();
        for (JsonNode node : response) {
            matches.add(new Match(node.path("username").asText(), node.path("displayName").asText()));
        }
        return matches;
    }

    private HttpRequest post(String path, Map<String, ?> body) throws IOException {
        return post(path, body, null);
    }

    private HttpRequest post(String path, Map<String, ?> body, String token) throws IOException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder.build();
    }

    private JsonNode send(HttpRequest request) throws IOException, InterruptedException, AuthException {
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();
        JsonNode body = response.body().isBlank() ? json.createObjectNode() : json.readTree(response.body());
        if (status >= 200 && status < 300) {
            return body;
        }
        String message = body.path("error").asText("");
        if (message.isEmpty()) {
            message = (status == 401) ? "Please sign in again" : "Server error (" + status + ")";
        }
        throw new AuthException(status, message);
    }

    private static Session toSession(JsonNode body) {
        return new Session(body.path("token").asText(), body.path("username").asText(), body.path("displayName").asText());
    }
}
