package examples;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Utility class for making HTTP requests in tests.
 */
public final class HttpClientTestUtils {

    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    private HttpClientTestUtils() {
    }

    /**
     * Sends a GET request to the specified URL.
     *
     * @param url the URL to send the request to
     * @return the HTTP response
     * @throws Exception if an error occurs
     */
    public static HttpResponse<String> get(final String url) throws Exception {
        final var request = HttpRequest.newBuilder(URI.create(url))
                .GET()
                .build();
        return CLIENT.send(request, BodyHandlers.ofString());
    }

    /**
     * Sends a raw HTTP/1.1 GET with an explicit Host header.
     *
     * <p>The JDK HttpClient forbids overriding the restricted Host header, so this uses a plain
     * socket to let tests exercise a Host that diverges from the actual connection (for example, to
     * verify that the LOCAL port strategy ignores the Host header port).
     *
     * @param host       the TCP host to connect to
     * @param port       the TCP port to connect to
     * @param path       the request path (for example, "/api/hello")
     * @param hostHeader the Host header value to send (for example, "example.invalid:1")
     * @throws IOException if the request fails
     */
    public static void getWithHostHeader(
            final String host,
            final int port,
            final String path,
            final String hostHeader) throws IOException {
        sendRawRequest(host, port, "GET " + path + " HTTP/1.1\r\n"
                + "Host: " + hostHeader + "\r\n"
                + "Connection: close\r\n\r\n");
    }

    /**
     * Sends raw bytes over a plain socket and returns the full response text.
     *
     * <p>The JDK HttpClient always produces a well-formed request, so this uses a plain socket to
     * let tests exercise server parse-failure paths (for example, a malformed request line that the
     * server rejects before normal processing but still hands to the access log).
     *
     * @param host       the TCP host to connect to
     * @param port       the TCP port to connect to
     * @param rawRequest the raw request bytes to send, as an ASCII string
     * @return the raw response text decoded as ISO-8859-1
     * @throws IOException if the request fails
     */
    public static String sendRawRequest(
            final String host,
            final int port,
            final String rawRequest) throws IOException {
        try (var socket = new Socket(host, port)) {
            // Fail instead of hanging the build if the server ever stops closing the connection.
            socket.setSoTimeout(5000);
            socket.getOutputStream().write(rawRequest.getBytes(StandardCharsets.US_ASCII));
            socket.getOutputStream().flush();
            // Drain the response so the server completes the exchange and logs the access event.
            final var response = socket.getInputStream().readAllBytes();
            return new String(response, StandardCharsets.ISO_8859_1);
        }
    }

    /**
     * Sends a GET request with query parameters.
     *
     * @param url         the URL to send the request to
     * @param queryString the query string (without leading ?)
     * @return the HTTP response
     * @throws Exception if an error occurs
     */
    public static HttpResponse<String> getWithQuery(final String url, final String queryString) throws Exception {
        final var fullUrl = url + "?" + queryString;
        return get(fullUrl);
    }

    /**
     * Sends a POST request with a JSON body.
     *
     * @param url  the URL to send the request to
     * @param body the JSON body
     * @return the HTTP response
     * @throws Exception if an error occurs
     */
    public static HttpResponse<String> post(final String url, final String body) throws Exception {
        final var request = HttpRequest.newBuilder(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build();
        return CLIENT.send(request, BodyHandlers.ofString());
    }

    /**
     * Sends a PUT request with a JSON body.
     *
     * @param url  the URL to send the request to
     * @param body the JSON body
     * @return the HTTP response
     * @throws Exception if an error occurs
     */
    public static HttpResponse<String> put(final String url, final String body) throws Exception {
        final var request = HttpRequest.newBuilder(URI.create(url))
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build();
        return CLIENT.send(request, BodyHandlers.ofString());
    }

    /**
     * Sends a DELETE request to the specified URL.
     *
     * @param url the URL to send the request to
     * @return the HTTP response
     * @throws Exception if an error occurs
     */
    public static HttpResponse<String> delete(final String url) throws Exception {
        final var request = HttpRequest.newBuilder(URI.create(url))
                .DELETE()
                .build();
        return CLIENT.send(request, BodyHandlers.ofString());
    }

    /**
     * Sends a GET request with HTTP Basic authentication.
     *
     * @param url      the URL to send the request to
     * @param username the username
     * @param password the password
     * @return the HTTP response
     * @throws Exception if an error occurs
     */
    public static HttpResponse<String> getWithBasicAuth(
            final String url,
            final String username,
            final String password) throws Exception {
        final var credentials = Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        final var request = HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Basic " + credentials)
                .GET()
                .build();
        return CLIENT.send(request, BodyHandlers.ofString());
    }

    /**
     * Sends a POST request with HTTP Basic authentication.
     *
     * @param url      the URL to send the request to
     * @param body     the JSON body
     * @param username the username
     * @param password the password
     * @return the HTTP response
     * @throws Exception if an error occurs
     */
    public static HttpResponse<String> postWithBasicAuth(
            final String url,
            final String body,
            final String username,
            final String password) throws Exception {
        final var credentials = Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        final var request = HttpRequest.newBuilder(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .header("Authorization", "Basic " + credentials)
                .build();
        return CLIENT.send(request, BodyHandlers.ofString());
    }
}
