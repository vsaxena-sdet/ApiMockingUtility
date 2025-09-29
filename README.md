# ApiMockingUtility

A lightweight API mocking utility that embeds a WireMock server and a small Javalin-based UI/REST layer to manage stubs (mappings).

- WireMock serves your mocked APIs (default port 8089)
- Javalin serves a tiny UI and a REST wrapper to manage WireMock mappings (default port 9090)
- Create, update, delete, list stubs, persist them to files, or reset in-memory state


<img width="1710" height="481" alt="Screenshot 2025-09-30 at 12 07 53 AM" src="https://github.com/user-attachments/assets/4b392206-1ad0-47a3-9401-693a654a50b0" />

<img width="1710" height="700" alt="Screenshot 2025-09-30 at 12 07 58 AM" src="https://github.com/user-attachments/assets/8168d03d-cc51-421e-bfe8-f202c9506ea6" />


## Quick start

Prerequisites:
- Java 17+
- Maven 3.8+

Build and run:

1) Package the app
- mvn -q -DskipTests package

2) Run the shaded jar
- java -jar target/apimockingutility-0.1.0-SNAPSHOT.jar

What you get:
- UI: http://localhost:9090/
- Health: http://localhost:9090/health
- WireMock admin: http://localhost:8089/__admin
- Your mocked endpoints: http://localhost:8089/... (based on mappings)


## Docker

Build the image:
- docker build -t apimockingutility:local .

Run the container:
- docker run --rm -p 9090:9090 -p 8089:8089 \
  -e PORT=9090 -e WIREMOCK_PORT=8089 \
  apimockingutility:local

Persist mappings to the host (recommended):
- mkdir -p ./mappings
- docker run --rm -p 9090:9090 -p 8089:8089 \
  -e PORT=9090 -e WIREMOCK_PORT=8089 \
  -e WIREMOCK_ROOT_MODE=dir -e WIREMOCK_ROOT_DIR=/app \
  -v "$PWD/mappings":/app/mappings \
  apimockingutility:local

After creating or editing stubs via the UI/REST, call the save endpoint (see below) so WireMock writes files under /app/mappings (mounted to ./mappings on your host).


## Configuration (env vars)

- PORT: Javalin app port for UI/REST (default: 9090)
- WIREMOCK_PORT: WireMock server port (default: 8089)
- WIREMOCK_ROOT_MODE: Where WireMock reads/writes stubs. Options:
  - dir: use filesystem directory (default). Enables persisting to ./mappings (or WIREMOCK_ROOT_DIR/mappings)
  - classpath: use resources inside the jar (read-only at runtime)
- WIREMOCK_ROOT_DIR: Filesystem root used when WIREMOCK_ROOT_MODE=dir (default: ".", the working directory). Mappings live under <root>/mappings
- WIREMOCK_ADMIN_BASE: Override WireMock admin base URL for advanced deployments (default derived from WIREMOCK_PORT)


## REST API (management layer)
Base URL: http://localhost:9090

- GET /health → "OK"

Mappings
- GET /api/mappings → List mappings (pass-through to WireMock __admin)
- GET /api/mappings/{id} → Get one mapping
- POST /api/mappings → Create mapping. Body: WireMock mapping JSON
- PUT /api/mappings/{id} → Update mapping
- DELETE /api/mappings/{id} → Delete mapping
- POST /api/mappings/save → Persist current in-memory stubs to files (requires WIREMOCK_ROOT_MODE=dir)
- POST /api/mappings/reset → Reset in-memory stubs

UI
- GET / → redirects to /index.html (simple client served from classpath under /public)


## Creating a simple stub

Example: Mock GET /hello on WireMock with a JSON response.

- curl -X POST http://localhost:9090/api/mappings \
  -H "Content-Type: application/json" \
  -d '{
    "request": { "method": "GET", "url": "/hello" },
    "response": {
      "status": 200,
      "headers": { "Content-Type": "application/json" },
      "jsonBody": { "message": "world" }
    }
  }'

Test it on WireMock:
- curl http://localhost:8089/hello

Persist to files (optional, requires WIREMOCK_ROOT_MODE=dir):
- curl -X POST http://localhost:9090/api/mappings/save

You should now see a mapping JSON file under ./mappings (or under <WIREMOCK_ROOT_DIR>/mappings).


## Project layout

- src/main/java/com/wiremock/App.java → boots WireMock and Javalin
- src/main/java/com/wiremock/web/ApiRoutes.java → REST endpoints and UI routing
- src/main/java/com/wiremock/WireMockClient.java → thin client for WireMock __admin
- src/main/resources/public → simple static UI
- src/main/resources/mappings → example mapping packaged in the jar
- mappings/ → filesystem mappings (when persisting to disk)


## Notes

- By default, this app starts WireMock in "dir" mode pointing to the working directory so saving mappings writes to ./mappings.
- If you run with classpath mode, mappings packaged under src/main/resources/mappings are visible/read at runtime, but saving is not possible.
- The shaded JAR includes everything needed to run standalone.


## License

MIT (or as defined by the repository owner).
