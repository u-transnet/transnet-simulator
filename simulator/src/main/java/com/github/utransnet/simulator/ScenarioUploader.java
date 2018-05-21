package com.github.utransnet.simulator;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by Artem on 13.03.2018.
 */
@Slf4j
public class ScenarioUploader {

    static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static OkHttpClient client = new OkHttpClient();

    @SneakyThrows
    public static void main(String[] args) {
        sendPost("http://localhost:8989/import_config", "test-scenario.json");
        sendPost("http://localhost:8989/import_route", "test-route-map.json");
    }

    private static void sendPost(String url, String file) throws IOException {
        String json = new String(Files.readAllBytes(Paths.get(file)), StandardCharsets.UTF_8);
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        Response response = client.newCall(request).execute();
        log.info(response.body().string());
    }
}
