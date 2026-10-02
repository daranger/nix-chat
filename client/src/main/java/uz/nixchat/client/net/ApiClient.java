package uz.nixchat.client.net;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Calls the server's REST API: registration, login and "who am I".
 * Methods block, so the UI calls them from a background thread.
 */
public class ApiClient {

    /** What the server returns after login or registration. */
    public record Session(String token, String username, String displayName) {
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

    private HttpRequest post(String path, Map<String, String> body) throws IOException {
        return HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
                .build();
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
