package com.wiremock.config;

public class ServerConfig {
    private final int appPort; // Javalin port
    private final int wireMockPort;
    private final String wireMockRootMode; // classpath|dir
    private final String wireMockRootDir; // used when mode=dir

    public ServerConfig() {
        this.appPort = intFromEnv("PORT", 9090);
        this.wireMockPort = intFromEnv("WIREMOCK_PORT", 8089);
        // Default to filesystem directory so /__admin/mappings/save works (writes under ./mappings)
        this.wireMockRootMode = System.getenv().getOrDefault("WIREMOCK_ROOT_MODE", "dir");
        this.wireMockRootDir = System.getenv().getOrDefault("WIREMOCK_ROOT_DIR", ".");
    }

    public int getAppPort() { return appPort; }
    public int getWireMockPort() { return wireMockPort; }
    public String getWireMockRootMode() { return wireMockRootMode; }
    public String getWireMockRootDir() { return wireMockRootDir; }

    public String adminBaseUrl() {
        // Allow override, else derive from local WireMock port
        String override = System.getenv("WIREMOCK_ADMIN_BASE");
        if (override != null && !override.isBlank()) return override;
        return "http://localhost:" + wireMockPort + "/__admin";
    }

    private static int intFromEnv(String key, int def) {
        try {
            String v = System.getenv(key);
            return v == null ? def : Integer.parseInt(v);
        } catch (Exception e) {
            return def;
        }
    }
}
