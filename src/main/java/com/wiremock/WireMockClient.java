package com.wiremock;

import com.google.gson.Gson;
import okhttp3.*;

import java.util.Map;

public class WireMockClient {

    private final String adminBase; // e.g. http://localhost:8089/__admin
    private final OkHttpClient http = new OkHttpClient();
    private final Gson gson = new Gson();
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public WireMockClient(String adminBase) {
        this.adminBase = adminBase.replaceAll("/+$$", "");
    }

    public Map listMappings() throws Exception {
        Request req = new Request.Builder()
                .url(adminBase + "/mappings")
                .get()
                .build();
        return execToMap(req);
    }

    public Map getMapping(String id) throws Exception {
        Request req = new Request.Builder()
                .url(adminBase + "/mappings/" + id)
                .get()
                .build();
        return execToMap(req);
    }

    public Map createMapping(Map body) throws Exception {
        Request req = new Request.Builder()
                .url(adminBase + "/mappings")
                .post(RequestBody.create(gson.toJson(body), JSON))
                .build();
        return execToMap(req);
    }

    public Map updateMapping(String id, Map body) throws Exception {
        Request req = new Request.Builder()
                .url(adminBase + "/mappings/" + id)
                .put(RequestBody.create(gson.toJson(body), JSON))
                .build();
        return execToMap(req);
    }

    public void deleteMapping(String id) throws Exception {
        Request req = new Request.Builder()
                .url(adminBase + "/mappings/" + id)
                .delete()
                .build();
        try (Response resp = http.newCall(req).execute()) {
            if (!resp.isSuccessful()) {
                throw new RuntimeException("Delete failed: " + status(resp));
            }
        }
    }

    /**
     * Persists stubs to files (requires WireMock started with a root dir).
     */
    public Map saveToFiles() throws Exception {
        Request req = new Request.Builder()
                .url(adminBase + "/mappings/save")
                .post(RequestBody.create(new byte[0], null))
                .build();
        return execToMap(req);
    }

    /**
     * Nukes in-memory stubs.
     */
    public void resetAll() throws Exception {
        Request req = new Request.Builder()
                .url(adminBase + "/mappings/reset")
                .post(RequestBody.create(new byte[0], null))
                .build();
        try (Response resp = http.newCall(req).execute()) {
            if (!resp.isSuccessful()) {
                throw new RuntimeException("Reset failed: " + status(resp));
            }
        }
    }

    // --- helpers
    private Map execToMap(Request req) throws Exception {
        System.out.println("WireMock " + req.method() + " " + req.url());
        try (Response resp = http.newCall(req).execute()) {
            if (!resp.isSuccessful()) {
                String errBody = resp.body() != null ? resp.body().string() : "";
                throw new RuntimeException("HTTP " + status(resp) + (errBody.isBlank() ? "" : "\n" + errBody));
            }
            String json = resp.body() != null ? resp.body().string() : "{}";
            return gson.fromJson(json, Map.class);
        }
    }

    private String status(Response resp) {
        return resp.code() + " " + (resp.message() == null ? "" : resp.message());
    }
}

