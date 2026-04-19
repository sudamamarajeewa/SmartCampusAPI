# Smart Campus Sensor & Room Management API

> **Module:** 5COSC022W Client-Server Architectures — University of Westminster  
> **Technology:** JAX-RS (Jersey 2.41) + Apache Tomcat + Maven  
> **Base URL:** `http://localhost:8080/api/v1`

---

## Table of Contents
1. [API Overview](#api-overview)
2. [Project Structure](#project-structure)
3. [How to Build & Run](#how-to-build--run)
4. [Sample curl Commands](#sample-curl-commands)
5. [Report: Answers to Coursework Questions](#report-answers-to-coursework-questions)

---

## API Overview

The Smart Campus API provides a RESTful interface for managing campus **Rooms**, **Sensors**, and **Sensor Readings**. It is built with JAX-RS (Jersey) and deployed on Apache Tomcat as a WAR file.

### Resource Hierarchy
```
/api/v1
├── /rooms
│   ├── GET    /              → List all rooms
│   ├── POST   /              → Create a room
│   ├── GET    /{roomId}      → Get a room
│   └── DELETE /{roomId}      → Delete a room (blocked if sensors are assigned)
│
└── /sensors
    ├── GET    /              → List all sensors (supports ?type= filter)
    ├── POST   /              → Register a sensor (validates roomId)
    ├── GET    /{sensorId}    → Get a sensor
    └── /{sensorId}/readings
        ├── GET  /            → Get reading history
        └── POST /            → Add a reading (updates sensor's currentValue)
```

### Data Models

| Model | Key Fields |
|-------|-----------|
| `Room` | `id`, `name`, `capacity`, `sensorIds[]` |
| `Sensor` | `id`, `type`, `status` (`ACTIVE`/`MAINTENANCE`/`OFFLINE`), `currentValue`, `roomId` |
| `SensorReading` | `id` (UUID), `timestamp` (epoch ms), `value` |

### Error Handling

| Scenario | Exception | HTTP Code |
|----------|-----------|-----------|
| DELETE room with sensors | `RoomNotEmptyException` | `409 Conflict` |
| POST sensor with invalid `roomId` | `LinkedResourceNotFoundException` | `422 Unprocessable Entity` |
| POST reading to MAINTENANCE sensor | `SensorUnavailableException` | `403 Forbidden` |
| Any unexpected runtime error | `GenericExceptionMapper` | `500 Internal Server Error` |
| All requests/responses | `ApiLoggingFilter` | (logged only) |

---

## Project Structure

```
SmartCampusAPI/
├── pom.xml                                    ← Maven build (Jersey + Jackson + Tomcat WAR)
├── README.md
├── PLAN.md
├── SmartCampusAPI.postman_collection.json     ← Postman test collection
└── src/main/
    ├── java/com/smartcampus/
    │   ├── SmartCampusApplication.java        ← @ApplicationPath("/api/v1")
    │   ├── data/
    │   │   └── DataStore.java                 ← ConcurrentHashMap in-memory store
    │   ├── model/
    │   │   ├── Room.java
    │   │   ├── Sensor.java
    │   │   ├── SensorReading.java
    │   │   └── ApiError.java                  ← Standard error response body
    │   ├── resource/
    │   │   ├── DiscoveryResource.java          ← GET /api/v1
    │   │   ├── RoomResource.java               ← /api/v1/rooms
    │   │   ├── SensorResource.java             ← /api/v1/sensors
    │   │   └── SensorReadingResource.java      ← /api/v1/sensors/{id}/readings
    │   ├── exception/
    │   │   ├── RoomNotEmptyException.java
    │   │   ├── LinkedResourceNotFoundException.java
    │   │   ├── SensorUnavailableException.java
    │   │   └── mapper/
    │   │       ├── RoomNotEmptyExceptionMapper.java
    │   │       ├── LinkedResourceNotFoundExceptionMapper.java
    │   │       ├── SensorUnavailableExceptionMapper.java
    │   │       └── GenericExceptionMapper.java
    │   └── filter/
    │       └── ApiLoggingFilter.java
    └── webapp/
        └── WEB-INF/
            └── web.xml
```

---

## How to Build & Run

### Prerequisites

| Tool | Version | Download |
|------|---------|----------|
| Java JDK | 11+ | [adoptium.net](https://adoptium.net) |
| Apache Maven | 3.8+ | [maven.apache.org](https://maven.apache.org) |
| Apache Tomcat | 9.x | [tomcat.apache.org](https://tomcat.apache.org) |

> **Windows Note:** Maven is located at `C:\apache-maven-3.9.15\bin\mvn.cmd` on this machine.
> To add it to your PATH permanently: *System Properties → Environment Variables → PATH → add `C:\apache-maven-3.9.15\bin`*

---

### Step 1 — Clone the Repository

```bash
git clone https://github.com/sudamamarajeewa/SmartCampusAPI.git
cd SmartCampusAPI
```

### Step 2 — Build the WAR File

**On Windows (full path):**
```powershell
C:\apache-maven-3.9.15\bin\mvn.cmd clean package
```

**On Windows (if Maven is on PATH) or macOS/Linux:**
```bash
mvn clean package
```

This produces `target/ROOT.war`.

### Step 3 — Deploy to Tomcat

1. **Stop Tomcat** if it is already running:
   ```
   <TOMCAT_HOME>\bin\shutdown.bat
   ```

2. **Delete any existing ROOT** folder inside Tomcat's `webapps/`:
   ```powershell
   Remove-Item "<TOMCAT_HOME>\webapps\ROOT" -Recurse -Force -ErrorAction SilentlyContinue
   ```

3. **Copy the WAR** to Tomcat's webapps folder:
   ```powershell
   Copy-Item "target\ROOT.war" "<TOMCAT_HOME>\webapps\ROOT.war"
   ```
   > The filename `ROOT.war` makes Tomcat serve the app at the root context (`/`),
   > so the API is available at `http://localhost:8080/api/v1` — not `/SmartCampusAPI/api/v1`.

4. **Start Tomcat:**
   ```
   <TOMCAT_HOME>\bin\startup.bat
   ```

5. **Verify** the server is running by visiting:
   ```
   http://localhost:8080/api/v1
   ```
   You should see the Discovery JSON response.

---

### Quick One-Liner (build + copy + start)

Replace `C:\tomcat` with your actual Tomcat path:

```powershell
C:\apache-maven-3.9.15\bin\mvn.cmd clean package -q; `
Copy-Item "target\ROOT.war" "C:\tomcat\webapps\ROOT.war" -Force; `
Start-Process "C:\tomcat\bin\startup.bat"
```

---

## Sample curl Commands

> Make sure the server is running at `http://localhost:8080` before executing these.

### 1. GET — Discovery Endpoint
```bash
curl -X GET http://localhost:8080/api/v1 -H "Accept: application/json"
```

### 2. POST — Create a Room
```bash
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d "{\"id\": \"LIB-301\", \"name\": \"Library Quiet Study\", \"capacity\": 50}"
```

### 3. POST — Register a Sensor (links to room LIB-301)
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d "{\"id\": \"CO2-001\", \"type\": \"CO2\", \"roomId\": \"LIB-301\"}"
```

### 4. GET — List Sensors Filtered by Type
```bash
curl -X GET "http://localhost:8080/api/v1/sensors?type=CO2" \
  -H "Accept: application/json"
```

### 5. POST — Add a Sensor Reading
```bash
curl -X POST http://localhost:8080/api/v1/sensors/CO2-001/readings \
  -H "Content-Type: application/json" \
  -d "{\"value\": 845.3}"
```

### 6. GET — Retrieve Reading History
```bash
curl -X GET http://localhost:8080/api/v1/sensors/CO2-001/readings \
  -H "Accept: application/json"
```

### 7. DELETE — Attempt to Delete Room with Sensors (409 expected)
```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301 \
  -H "Accept: application/json"
```

### 8. POST — Sensor with Non-existent roomId (422 expected)
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d "{\"type\": \"Temperature\", \"roomId\": \"GHOST-999\"}"
```

---

## Report: Answers to Coursework Questions

---

### Part 1 — Service Architecture & Setup

#### Q1: What is the default lifecycle of a JAX-RS resource class? How does it impact in-memory data management?

By default, JAX-RS creates a **new instance of a resource class for every incoming HTTP request** (per-request lifecycle). This is the specification default defined in the JAX-RS spec (Section 3.1). Each request gets its own instance, meaning any instance-level fields are not shared between requests.

This architectural decision has a critical implication for in-memory data storage: if data were stored as **instance fields** inside the resource class, it would be lost after every request. To prevent this, this project uses a dedicated singleton `DataStore` class with **`static` `ConcurrentHashMap` fields**. Because `static` fields belong to the class (not any instance), they persist across all requests and across all resource class instances for the entire lifetime of the JVM.

`ConcurrentHashMap` is chosen over a regular `HashMap` because Tomcat processes concurrent requests on multiple threads. A regular `HashMap` is not thread-safe and can throw `ConcurrentModificationException` or silently corrupt data under concurrent access. `ConcurrentHashMap` provides fine-grained locking (segment-level) that allows safe concurrent reads and writes without the performance overhead of fully synchronised methods.

---

#### Q2: Why is HATEOAS considered a hallmark of advanced RESTful design? How does it benefit client developers?

**HATEOAS** (Hypermedia As The Engine Of Application State) is the principle that API responses should include **links to related resources and possible next actions**, rather than requiring clients to construct URLs manually from documentation.

In this API, the discovery endpoint at `GET /api/v1` returns:
```json
{
  "links": {
    "rooms":   "/api/v1/rooms",
    "sensors": "/api/v1/sensors"
  }
}
```

**Benefits to client developers:**
- **Discoverability:** A client can start at `/api/v1` and navigate the entire API without prior knowledge of the URL structure.
- **Decoupling:** If the server changes `/api/v1/rooms` to `/api/v2/facilities/rooms`, clients using HATEOAS links automatically discover the new path — no client code changes needed.
- **Reduced documentation dependency:** Static documentation becomes stale; HATEOAS makes the API self-documenting at runtime.
- **Guided workflows:** Responses can advertise only the *allowed* next actions based on current state (e.g., a MAINTENANCE sensor response could omit the "add-reading" link).

---

### Part 2 — Room Management

#### Q3: Returning only IDs vs full room objects — what are the implications?

| Approach | Pros | Cons |
|----------|------|------|
| **Return only IDs** | Minimal payload size, fast for large datasets | Client must make N additional requests to fetch details (N+1 problem) |
| **Return full objects** | Single request delivers all data, simpler client code | Larger payload, wasted bandwidth if client only needs IDs |

This API returns **full room objects** in `GET /rooms` because:
- The expected number of rooms in a campus is manageable (hundreds, not millions).
- Returning full objects eliminates extra round-trips, reducing client complexity.
- For very large datasets, pagination or sparse fieldsets (e.g., `?fields=id,name`) would be the appropriate mitigation.

---

#### Q4: Is the DELETE operation idempotent in this implementation?

**Strictly speaking, no — this DELETE is not idempotent** in the pure HTTP sense.

- **First DELETE** on a valid room → `200 OK` (deleted successfully).
- **Second DELETE** on the same room → `404 Not Found` (already gone).

A truly idempotent DELETE would return `200 OK` (or `204 No Content`) on every call, even for already-deleted resources. However, returning `404` on repeat calls is a **deliberate and justifiable design choice**: it gives the client clear, accurate feedback that the resource does not exist, rather than silently pretending the deletion succeeded again. This is a common pragmatic trade-off adopted by many production APIs (e.g., GitHub's API returns `404` on repeated DELETEs). The important guarantee — that **the server state does not change** after the first deletion — still holds.

---

### Part 3 — Sensor Operations & Linking

#### Q5: What happens if a client sends `text/plain` instead of `application/json` to a POST endpoint?

When `@Consumes(MediaType.APPLICATION_JSON)` is declared on a method, JAX-RS checks the incoming `Content-Type` header **before** invoking the method. If the header does not match `application/json`, JAX-RS automatically returns:

```
HTTP 415 Unsupported Media Type
```

The resource method is **never called**. This is handled entirely by the Jersey runtime, not application code. This behaviour provides a clean contract: the server declares what it can consume, and mismatched requests are rejected at the framework level, preventing malformed data from reaching business logic.

---

#### Q6: Why is `@QueryParam` (filtering) superior to path-based filtering like `/sensors/type/CO2`?

| Approach | Example | Assessment |
|----------|---------|------------|
| `@QueryParam` | `GET /sensors?type=CO2` | ✅ Preferred |
| Path segment | `GET /sensors/type/CO2` | ❌ Antipattern |

**Reasons query parameters are better for filtering:**

1. **Semantic clarity:** URL path segments should identify **resources** (nouns: rooms, sensors). Query parameters should carry **modifiers** (filters, sorts, pagination). `/sensors/type/CO2` wrongly implies `type` is a resource.
2. **Optional by design:** Query parameters are naturally optional. With path-based filtering, you need separate route definitions for filtered vs unfiltered, leading to duplication.
3. **Composability:** Multiple filters combine naturally: `?type=CO2&status=ACTIVE`. Path-based approaches require complex nested paths.
4. **RESTful convention:** RFC 3986 defines query components as the appropriate place for non-hierarchical data. REST best practices and major APIs (GitHub, Google, Twitter) all use query params for filtering.

---

### Part 4 — Sub-Resources

#### Q7: What are the architectural benefits of the Sub-Resource Locator pattern?

The Sub-Resource Locator pattern delegates processing of a sub-path to a separate dedicated class. In this API, `SensorResource` does not define the readings endpoints itself — instead, it returns an instance of `SensorReadingResource`:

```java
@Path("/{sensorId}/readings")
public SensorReadingResource getReadingsResource(@PathParam("sensorId") String sensorId) {
    return new SensorReadingResource(sensorId);
}
```

**Benefits:**

1. **Single Responsibility Principle:** `SensorResource` manages sensors. `SensorReadingResource` manages readings. Each class is focused and cohesive.
2. **Manageability at scale:** In a real campus API with dozens of sub-resources, cramming every path into one class creates a "God class" with hundreds of methods. Delegation keeps each class small and comprehensible.
3. **Independent testability:** `SensorReadingResource` can be unit-tested in isolation by constructing it directly with a `sensorId`, without needing a full HTTP context.
4. **Reusability:** If readings needed to be exposed from another parent resource in future, the same `SensorReadingResource` class can be reused.
5. **Cleaner routing logic:** Jersey resolves the path dynamically at runtime, allowing the locator to apply pre-processing logic (e.g., permission checks) before handing off.

---

### Part 5 — Error Handling & Logging

#### Q8: Why is HTTP 422 more semantically accurate than 404 when a `roomId` reference is invalid?

- **404 Not Found** means the **URL path** of the request does not map to any known resource. For example, `GET /api/v1/rooms/FAKE` → the room `FAKE` doesn't exist at that URL.
- **422 Unprocessable Entity** means the **request URL is valid and understood**, but the **request body contains a semantic error** — in this case, a reference to a `roomId` that does not exist.

When a client posts `POST /api/v1/sensors` with `"roomId": "GHOST-999"`:
- The URL `/api/v1/sensors` **does exist** → so 404 would be misleading.
- The JSON is syntactically valid → so 400 Bad Request is imprecise.
- The issue is a **semantic constraint inside the payload** — the referenced room cannot be resolved.

422 communicates precisely: "I understood your request and its format, but I cannot process it because the data is logically inconsistent."

---

#### Q9: What security risks are associated with exposing Java stack traces to API consumers?

Exposing raw stack traces in API responses is a significant security vulnerability:

1. **Technology fingerprinting:** Stack traces reveal the server-side technology stack (e.g., Jersey, Tomcat, Java version). Attackers use this to look up known CVEs for that specific version.
2. **Package/class name disclosure:** Full qualified class names (e.g., `com.smartcampus.data.DataStore`) reveal the internal architecture, making targeted attacks easier.
3. **File path revelation:** Stack traces often include absolute file paths on the server (e.g., `C:\Users\MSI-G\Desktop\...`), exposing the server's directory structure.
4. **Business logic exposure:** Method names and line numbers in traces can reveal the flow of sensitive operations (e.g., authentication, data access).
5. **Crafted attack vectors:** An attacker studying stack traces can intentionally craft requests to trigger specific code paths, escalating from information gathering to active exploitation.

This API uses `GenericExceptionMapper` to intercept all `Throwable`s, log them **server-side only**, and return a generic `500` message to the client — eliminating all of the above risks.

---

#### Q10: Why is it better to use JAX-RS filters for logging rather than inline `Logger.info()` calls?

| Approach | Issue |
|----------|-------|
| Inline logging in every method | Code duplication, violates DRY principle |
| JAX-RS filter | Single class handles all requests/responses |

**Reasons filters are superior:**

1. **Separation of Concerns:** Resource methods should contain only business logic. Logging is a cross-cutting concern — it applies universally and should not be mixed with domain code.
2. **DRY (Don't Repeat Yourself):** With 10+ endpoints, inline logging means 10+ duplicated log statements. A filter handles all of them with zero duplication.
3. **Consistency:** A filter guarantees uniform log format for every request. Inline logging risks inconsistent formats across different developers' code.
4. **Maintainability:** Changing the log format, adding a request ID, or switching logging libraries requires changing **one file** (the filter), not every resource class.
5. **No risk of omission:** A developer adding a new endpoint cannot forget to add logging — the filter catches it automatically.
6. **AOP (Aspect-Oriented Programming):** Filters implement the same concept as AOP interceptors, a well-established enterprise pattern for cross-cutting concerns like logging, authentication, and metrics.

---

*Report prepared for 5COSC022W Client-Server Architectures, University of Westminster, April 2026.*
