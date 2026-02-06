package io.github.seijikohara.examples;

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
