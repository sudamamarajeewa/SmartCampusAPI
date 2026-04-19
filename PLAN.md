# 📅 Smart Campus API — 5-Day Development Plan
**Module:** 5COSC022W Client-Server Architectures  
**Due:** 24 April 2026, 13:00  
**Stack:** JAX-RS (Jersey) + Apache Tomcat + Maven  
**Repo:** Public GitHub (this repo)

---

> **Daily Commit Rule:** At the end of every day, commit your progress with a clear message.
> Example: `git commit -m "Day 1: Project setup, Maven config, discovery endpoint"`

---

## 🗓️ Day 1 — April 19 (Sunday)
### Part 1: Project Setup & Discovery Endpoint *(10 Marks)*

**Goal:** Get a working Maven + Jersey + Tomcat project with your first endpoint responding.

### Tasks:
- [ ] **1.1 Bootstrap Maven Project**
  - Create `pom.xml` with the following dependencies:
    - `jersey-container-servlet` (JAX-RS implementation)
    - `jersey-media-json-jackson` (JSON support via Jackson)
    - `javax.servlet-api` (provided, for Tomcat)
    - `jersey-hk2` (dependency injection)
  - Set packaging to `war` so Tomcat can deploy it
  - Target Java 11+

- [ ] **1.2 Create `web.xml`**
  - Configure the Jersey servlet as the front controller in `src/main/webapp/WEB-INF/web.xml`

- [ ] **1.3 Create `SmartCampusApplication.java`**
  - Extend `javax.ws.rs.core.Application`
  - Annotate with `@ApplicationPath("/api/v1")`
  - Register your resource classes (or use package scanning)

- [ ] **1.4 Create the three POJOs**
  - `Room.java` — id, name, capacity, sensorIds (List<String>)
  - `Sensor.java` — id, type, status, currentValue, roomId
  - `SensorReading.java` — id, timestamp, value

- [ ] **1.5 Create `DataStore.java` (Singleton)**
  - A single class with `static` `ConcurrentHashMap` instances for rooms, sensors, and readings
  - This is your in-memory "database"

- [ ] **1.6 Implement Discovery Endpoint**
  - Create `DiscoveryResource.java`
  - `GET /api/v1` → returns JSON with API version, admin contact, and links to `/api/v1/rooms` and `/api/v1/sensors`

- [ ] **1.7 Deploy & Test**
  - Build with `mvn clean package`
  - Deploy the `.war` to Tomcat (`webapps/` folder)
  - Test with Postman: `GET http://localhost:8080/SmartCampusAPI/api/v1`

- [ ] **1.8 Write report answer for Part 1 in README.md**
  - Q: Default lifecycle of a JAX-RS resource class (per-request vs singleton)?
  - Q: What is HATEOAS and why does it benefit client developers?

**📌 Git Commit:** `"Day 1: Maven setup, POJOs, DataStore, discovery endpoint /api/v1"`

---

## 🗓️ Day 2 — April 20 (Monday)
### Part 2: Room Management *(20 Marks)*

**Goal:** Full CRUD for Rooms with safety logic on deletion.

### Tasks:
- [ ] **2.1 Create `RoomResource.java`** at path `/rooms`
  - `GET /api/v1/rooms` → return all rooms as JSON list
  - `POST /api/v1/rooms` → create a new room, return `201 Created` with the new room
  - `GET /api/v1/rooms/{roomId}` → return single room or 404 if not found

- [ ] **2.2 Implement DELETE with Business Logic**
  - `DELETE /api/v1/rooms/{roomId}`
  - Check if the room has any sensors assigned to it (check `sensorIds` list)
  - If yes → throw `RoomNotEmptyException` (you'll map this in Day 4)
  - If no → delete and return `200 OK` or `204 No Content`

- [ ] **2.3 Add basic 404 handling for unknown roomId**
  - Throw a `NotFoundException` (or your own custom exception) if room not found

- [ ] **2.4 Test all Room endpoints in Postman**
  - POST a room → GET all rooms → GET by ID → DELETE (with sensors check later)

- [ ] **2.5 Write report answer for Part 2 in README.md**
  - Q: Returning only IDs vs full room objects — tradeoffs?
  - Q: Is DELETE idempotent in your implementation? Justify.

**📌 Git Commit:** `"Day 2: Room CRUD endpoints, DELETE safety check logic"`

---

## 🗓️ Day 3 — April 21 (Tuesday)
### Part 3: Sensor Operations & Linking *(20 Marks)*

**Goal:** Sensor management with room validation and type filtering.

### Tasks:
- [ ] **3.1 Create `SensorResource.java`** at path `/sensors`
  - `GET /api/v1/sensors` → return all sensors
  - `GET /api/v1/sensors?type=CO2` → filter by type using `@QueryParam("type")`
  - `POST /api/v1/sensors` → register a new sensor
    - Validate that the `roomId` in the request body exists in `DataStore`
    - If roomId does NOT exist → throw `LinkedResourceNotFoundException` (mapped in Day 4)
    - If roomId exists → add sensor, also add `sensorId` to the Room's `sensorIds` list
    - Return `201 Created`

- [ ] **3.2 Add `@Consumes(MediaType.APPLICATION_JSON)` and `@Produces(MediaType.APPLICATION_JSON)`**
  - Ensure content-type headers are correctly set on all endpoints

- [ ] **3.3 Test Sensor endpoints in Postman**
  - POST sensor with valid roomId
  - POST sensor with invalid roomId (should get 422)
  - GET all sensors
  - GET sensors filtered by type

- [ ] **3.4 Write report answers for Part 3 in README.md**
  - Q: What happens if client sends `text/plain` instead of `application/json`?
  - Q: `@QueryParam` (filtering) vs path-based `/sensors/type/CO2` — which is better and why?

**📌 Git Commit:** `"Day 3: Sensor CRUD, room validation on POST, query param filtering"`

---

## 🗓️ Day 4 — April 22 (Wednesday)
### Part 4: Sub-Resources + Part 5: Error Handling & Logging *(20 + 30 Marks)*

**Goal:** Sensor readings sub-resource + all exception mappers + logging filter.

### Sub-Resource (Part 4):
- [ ] **4.1 Add sub-resource locator in `SensorResource.java`**
  ```java
  @Path("{sensorId}/readings")
  public SensorReadingResource getReadingsResource(@PathParam("sensorId") String sensorId) {
      return new SensorReadingResource(sensorId);
  }
  ```

- [ ] **4.2 Create `SensorReadingResource.java`**
  - Constructor accepts `sensorId`
  - `GET /api/v1/sensors/{sensorId}/readings` → list all readings for that sensor
  - `POST /api/v1/sensors/{sensorId}/readings` → add a new reading
    - Check if sensor exists → 404 if not
    - Check if sensor status is `"MAINTENANCE"` → throw `SensorUnavailableException`
    - Add reading to `DataStore`
    - **Side effect:** Update `sensor.currentValue` with the new reading's value
    - Return `201 Created`

- [ ] **4.3 Write report answer for Part 4 in README.md**
  - Q: Architectural benefits of the Sub-Resource Locator pattern?

### Error Handling (Part 5):
- [ ] **5.1 Create `RoomNotEmptyException.java`**
  - Custom `RuntimeException`
  - Mapper: returns `409 Conflict` with JSON body

- [ ] **5.2 Create `LinkedResourceNotFoundException.java`**
  - Custom `RuntimeException`
  - Mapper: returns `422 Unprocessable Entity` with JSON body

- [ ] **5.3 Create `SensorUnavailableException.java`**
  - Custom `RuntimeException`
  - Mapper: returns `403 Forbidden` with JSON body

- [ ] **5.4 Create `GenericExceptionMapper.java`**
  - Implements `ExceptionMapper<Throwable>`
  - Catches all unexpected errors
  - Returns `500 Internal Server Error` with a safe generic message (NO stack traces!)

- [ ] **5.5 Create `ApiLoggingFilter.java`**
  - Implements both `ContainerRequestFilter` and `ContainerResponseFilter`
  - Log HTTP method + URI on every request
  - Log response status code on every response
  - Use `java.util.logging.Logger`

- [ ] **5.6 Register all mappers and the filter** in your Application class

- [ ] **5.7 Test error scenarios in Postman**
  - DELETE room with sensors → 409
  - POST sensor with bad roomId → 422
  - POST reading to MAINTENANCE sensor → 403
  - Force a 500 (e.g., send malformed JSON)

- [ ] **5.8 Write report answers for Part 5 in README.md**
  - Q: Why is 422 more semantically accurate than 404 for a bad roomId reference?
  - Q: Security risks of exposing Java stack traces?
  - Q: Why use JAX-RS filters for logging instead of inline Logger calls?

**📌 Git Commit:** `"Day 4: SensorReadings sub-resource, all exception mappers, logging filter"`

---

## 🗓️ Day 5 — April 23 (Thursday)
### Final Polish, README, Postman Demo & Submission Prep

**Goal:** Clean up, document, record video, and submit.

### Tasks:
- [ ] **5.1 Full end-to-end Postman test run**
  - Create a Postman collection covering every endpoint
  - Verify all happy paths and error cases work correctly
  - Save your Postman collection as `SmartCampusAPI.postman_collection.json` and commit it

- [ ] **5.2 Complete `README.md`** on GitHub with:
  - Overview of API design and resource hierarchy
  - Build & run instructions (Maven + Tomcat steps)
  - At least **5 sample `curl` commands** from different parts of the API
  - All report answers (from Days 1–4 above) formatted clearly

- [ ] **5.3 Final code cleanup**
  - Remove any debug `System.out.println` (the filter handles logging)
  - Ensure consistent JSON error response format across all mappers
  - Add sample data seeding (optional: pre-populate a few rooms/sensors on startup)

- [ ] **5.4 Record Video Demonstration (≤ 10 minutes)**
  - Use Postman to demo every task live
  - Show your face/camera and speak clearly about what each call does
  - Cover: Discovery → Rooms CRUD → Sensors (with filter) → Readings → Error cases

- [ ] **5.5 Submit on Blackboard**
  - GitHub repo link (ensure repo is **public**)
  - Upload video file directly to Blackboard submission link
  - Double check README has the report answers (PDF format if separated)

**📌 Final Git Commit:** `"Day 5: README complete, Postman collection added, final cleanup"`

---

## 📁 Recommended Project Structure

```
SmartCampusAPI/
├── pom.xml
├── README.md
├── PLAN.md
├── SmartCampusAPI.postman_collection.json
└── src/
    └── main/
        ├── java/
        │   └── com/smartcampus/
        │       ├── SmartCampusApplication.java   ← @ApplicationPath("/api/v1")
        │       ├── data/
        │       │   └── DataStore.java             ← ConcurrentHashMap storage
        │       ├── model/
        │       │   ├── Room.java
        │       │   ├── Sensor.java
        │       │   └── SensorReading.java
        │       ├── resource/
        │       │   ├── DiscoveryResource.java     ← GET /api/v1
        │       │   ├── RoomResource.java          ← /rooms
        │       │   ├── SensorResource.java        ← /sensors
        │       │   └── SensorReadingResource.java ← /sensors/{id}/readings
        │       ├── exception/
        │       │   ├── RoomNotEmptyException.java
        │       │   ├── LinkedResourceNotFoundException.java
        │       │   ├── SensorUnavailableException.java
        │       │   └── mapper/
        │       │       ├── RoomNotEmptyExceptionMapper.java
        │       │       ├── LinkedResourceNotFoundExceptionMapper.java
        │       │       ├── SensorUnavailableExceptionMapper.java
        │       │       └── GenericExceptionMapper.java
        │       └── filter/
        │           └── ApiLoggingFilter.java
        └── webapp/
            └── WEB-INF/
                └── web.xml
```

---

## ⚡ Key Technical Reminders

| Topic | Detail |
|---|---|
| **Framework** | JAX-RS via Jersey (NOT Spring Boot) |
| **Server** | Apache Tomcat (deploy `.war` to `webapps/`) |
| **Storage** | `ConcurrentHashMap` in `DataStore.java` — no database! |
| **JSON** | Jackson via `jersey-media-json-jackson` dependency |
| **API Base Path** | `http://localhost:8080/api/v1` |
| **WAR File Name** | Set `<finalName>ROOT</finalName>` in `pom.xml` so Tomcat deploys at root context `/` |
| **Sensor Status Values** | `"ACTIVE"`, `"MAINTENANCE"`, `"OFFLINE"` |
| **IDs** | Use `UUID.randomUUID().toString()` for auto-generated IDs |
| **Packaging** | Must be `.war` for Tomcat deployment |

---

## ✅ Marks At A Glance

| Part | Topic | Marks | Day |
|---|---|---|---|
| Part 1 | Setup + Discovery | 10 | Day 1 |
| Part 2 | Room Management | 20 | Day 2 |
| Part 3 | Sensors & Filtering | 20 | Day 3 |
| Part 4 | Sub-Resources (Readings) | 20 | Day 4 |
| Part 5 | Error Handling & Logging | 30 | Day 4 |
| **Total** | | **100** | |

> 🔔 **Remember:** Each section = 50% coding + 30% video demo + 20% report answer.
> Never skip the report questions — they're easy marks!
