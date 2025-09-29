package com.wiremock.web;

import com.wiremock.WireMockClient;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.Map;

public class ApiRoutes {

    private final WireMockClient wm;

    public ApiRoutes(WireMockClient wm) {
        this.wm = wm;
    }

    public void register(Javalin app) {
        // Health
        app.get("/health", ApiRoutes::health);

        // --- API: list mappings
        app.get("/api/mappings", ctx -> ctx.json(wm.listMappings()));

        // --- API: get one mapping by id
        app.get("/api/mappings/{id}", ctx -> {
            String id = ctx.pathParam("id");
            ctx.json(wm.getMapping(id));
        });

        // --- API: create mapping
        app.post("/api/mappings", ctx -> {
            Map body = ctx.bodyAsClass(Map.class); // raw pass-through
            ctx.json(wm.createMapping(body));
        });

        // --- API: update mapping
        app.put("/api/mappings/{id}", ctx -> {
            String id = ctx.pathParam("id");
            Map body = ctx.bodyAsClass(Map.class);
            ctx.json(wm.updateMapping(id, body));
        });

        // --- API: delete mapping
        app.delete("/api/mappings/{id}", ctx -> {
            String id = ctx.pathParam("id");
            wm.deleteMapping(id);
            ctx.status(204);
        });

        // --- API: persist current in-memory mappings to files
        app.post("/api/mappings/save", ctx -> ctx.json(wm.saveToFiles()));

        // --- API: reset mappings (careful: nukes in-memory stubs)
        app.post("/api/mappings/reset", ctx -> {
            wm.resetAll();
            ctx.status(204);
        });

        // Home → serve UI
        app.get("/", ApiRoutes::home);
    }

    private static void home(Context ctx) {
        ctx.redirect("/index.html");
    }

    private static void health(Context ctx) {
        ctx.result("OK");
    }
}
