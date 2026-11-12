package main;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Client {
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String basePath;

    public record ServerResponse(String message, List<String> successors){}

    public Client(String port) {
        this.basePath = "http://localhost:" + port;
    }
    public ServerResponse sendRequest(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(basePath + (path.startsWith("/") ? path : "/" + path)))
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        if (response.statusCode() != 200) {
            System.err.println("Ошибка: " + response.statusCode());
            return null;
        }

        ServerResponse serverResponse = mapper.readValue(response.body(), ServerResponse.class);

        if (serverResponse == null
                || serverResponse.message() == null
                || serverResponse.successors() == null
                || serverResponse.successors().stream().anyMatch(s -> s == null)) {
            System.err.println("Не удалось получить необходимые данные!");
            return null;
        }

        return serverResponse;
    }
}
