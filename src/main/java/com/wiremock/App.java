package com.wiremock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.wiremock.config.ServerConfig;
import com.wiremock.web.ApiRoutes;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

public class App {

    public static void main(String[] args) {
        ServerConfig cfg = new ServerConfig();

        // Configure WireMock to use classpath files by default so shaded JAR works out-of-the-box
        WireMockConfiguration wmOptions = options().port(cfg.getWireMockPort());
        if ("dir".equalsIgnoreCase(cfg.getWireMockRootMode())) {
            wmOptions = wmOptions.usingFilesUnderDirectory(cfg.getWireMockRootDir());
        } else {
            wmOptions = wmOptions.usingFilesUnderClasspath("");
        }
        WireMockServer wmServer = new WireMockServer(wmOptions);
        wmServer.start();

        WireMockClient wm = new WireMockClient(cfg.adminBaseUrl());

        Javalin app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";
            config.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/";
                staticFiles.directory = "/public";
                staticFiles.location = Location.CLASSPATH;
            });
            // basic CORS for local hacking
            config.plugins.enableCors(cors -> cors.add(it -> {
                it.anyHost();
                it.exposeHeader("Content-Type");
            }));
        }).start(cfg.getAppPort());

        new ApiRoutes(wm).register(app);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { app.stop(); } catch (Exception ignored) {}
            try { wmServer.stop(); } catch (Exception ignored) {}
        }));

        System.out.println("UI up at http://localhost:" + cfg.getAppPort() + "  (Admin: " + cfg.adminBaseUrl() + ")");
    }
}
