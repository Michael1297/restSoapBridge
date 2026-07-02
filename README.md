# REST SOAP Bridge

Universal REST → SOAP bridge.  
The application accepts JSON over HTTP, invokes SOAP operations via Apache CXF, and returns JSON.

Swagger UI lists all available endpoints generated from WSDL/XSD.

## Requirements

- Java 17+
- Maven 3.9+
- Reachable SOAP backend (e.g. CXF at `http://host:8080/services/`)

## Build

```bash
mvn clean package -DskipTests
```

JAR:

```text
target/restSoapBridge-1.0-SNAPSHOT.jar
```

## Run

```bash
java -jar target/restSoapBridge-1.0-SNAPSHOT.jar
```

After startup:

| URL | Description |
|-----|-------------|
| `http://localhost:9194/swagger-ui.html` | Swagger UI |
| `http://localhost:9194/v3/api-docs` | OpenAPI JSON |
| `http://localhost:9194/api/...` | Bridge REST endpoints |

Default port is `9194` (see `server.port`).

---

## Application settings

All settings are provided via `application.yml` or standard Spring Boot mechanisms (env vars, CLI args, external config files).

### Main properties

```yaml
server:
  port: 9194

bridge:
  auto:
    services-url: http://127.0.0.1:8080/services/
    path-prefix: /api
    http-method: POST

logging:
  level:
    io.github.connellite: DEBUG
```

### `bridge.auto.*`

| Property | Description | Example |
|----------|-------------|---------|
| `services-url` | URL of the CXF services listing page | `http://127.0.0.1:8080/services/` |
| `path-prefix` | REST path prefix | `/api` |
| `http-method` | HTTP method for all generated endpoints | `POST` |

One endpoint is created per SOAP operation:

```text
{path-prefix}/{ServiceName}/{operationName}
```

Example: `POST /api/Auth/login`.

---

## Environment variables

Spring Boot maps properties to UPPER_SNAKE_CASE automatically:

| Property | Environment variable |
|----------|---------------------|
| `server.port` | `SERVER_PORT` |
| `bridge.auto.services-url` | `BRIDGE_AUTO_SERVICES_URL` |
| `bridge.auto.path-prefix` | `BRIDGE_AUTO_PATH_PREFIX` |
| `bridge.auto.http-method` | `BRIDGE_AUTO_HTTP_METHOD` |

Example:

```bash
export SERVER_PORT=9194
export BRIDGE_AUTO_SERVICES_URL=http://soap-backend:8080/services/
export BRIDGE_AUTO_PATH_PREFIX=/api
export BRIDGE_AUTO_HTTP_METHOD=POST
java -jar restSoapBridge-1.0-SNAPSHOT.jar
```

---

## Configuration after build (outside the JAR)

### 1. Command-line arguments

```bash
java -jar restSoapBridge-1.0-SNAPSHOT.jar \
  --server.port=9194 \
  --bridge.auto.services-url=http://192.168.1.10:8080/services/ \
  --bridge.auto.path-prefix=/api \
  --bridge.auto.http-method=POST
```

### 2. External `application.yml`

Layout:

```text
/opt/bridge/
├── restSoapBridge-1.0-SNAPSHOT.jar
└── application.yml
```

`application.yml`:

```yaml
server:
  port: 9194

bridge:
  auto:
    services-url: http://192.168.1.10:8080/services/
    path-prefix: /api
    http-method: POST
```

Run from the same directory:

```bash
cd /opt/bridge
java -jar restSoapBridge-1.0-SNAPSHOT.jar
```

### 3. Explicit config directory

```bash
java -jar restSoapBridge-1.0-SNAPSHOT.jar \
  --spring.config.additional-location=file:./config/
```

---

## How auto-generation works

On startup the bridge:

1. Loads the HTML page at `bridge.auto.services-url`
2. Discovers WSDL links (CXF services page)
3. Parses WSDL and XSD
4. Builds request/response mappings in memory

Example generated mapping:

```yaml
path: /api/Auth/login
method: POST
request:
  "$.user": "$.login.user"
response:
  "#root.return": "$.return"
```

---

## Example request

```bash
curl -X POST http://localhost:9194/api/Auth/login \
  -H "Content-Type: application/json" \
  -d '{"user":"admin","pass":"secret"}'
```

The response depends on WSDL/XSD. For a typical `login`:

```json
{"return":"<session-id>"}
```

---

## Common errors

| HTTP | Cause |
|------|-------|
| 404 | Endpoint not generated (operation missing in WSDL or different `path-prefix`) |
| 401 | SOAP `AccessDeniedException` |
| 502 | SOAP error, WSDL/XSD parsing failure, or backend unreachable |

---

## Recommended production layout

```text
/etc/rest-soap-bridge/
└── application.yml

/var/lib/rest-soap-bridge/
└── restSoapBridge-1.0-SNAPSHOT.jar
```

Run:

```bash
java -jar /var/lib/rest-soap-bridge/restSoapBridge-1.0-SNAPSHOT.jar \
  --spring.config.additional-location=file:/etc/rest-soap-bridge/application.yml
```
