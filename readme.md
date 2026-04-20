# Smart Campus Sensor & Room Management API

## 1. Overview of API Design
The Smart Campus API is built using JAX-RS (Jersey) to provide a RESTful interface for managing campus facilities. The architecture is built around three core resource models: **Rooms**, **Sensors**, and **Sensor Readings**. 
* **Resource Hierarchy:** The API follows RESTful principles with a clear hierarchy. Operations on rooms occur at /api/v1/rooms, whilst operations on sensors occur at /api/v1/sensors. 
* **Sub-Resource Locator Pattern:** To manage historical data, readings are nested inside sensors using a sub-resource locator pattern at /api/v1/sensors/{sensorId}/readings.
* **State & Data Management:** Data is managed in-memory using a thread-safe ConcurrentHashMap Singleton (DataStore), preventing race conditions in a concurrent Tomcat environment.
* **Error Handling:** The API uses comprehensive custom exception mappers to ensure no internal Java stack traces are leaked, mapping logical errors to proper HTTP semantic codes (e.g. 404 Not Found, 409 Conflict, 422 Unprocessable Entity, 403 Forbidden). 

---

## 2. Build and Launch Instructions

### Prerequisites
* Java JDK 11 or higher installed and on system PATH
* Apache Maven installed and on system PATH
* Apache Tomcat 9.x (or later) extracted to a local directory

### Step-by-Step Instructions
1. **Clone the repository:**
   ```bash
   git clone https://github.com/sudamamarajeewa/SmartCampusAPI.git
   cd SmartCampusAPI
   ```
2. **Compile and package the project using Maven:**
   Run the following command in the project root to create the .war deployment file.
   ```bash
   mvn clean package
   ```
   *(Note: This creates a file named ROOT.war in the target/ directory.)*
3. **Deploy to Apache Tomcat:**
   Copy the ROOT.war file from the target/ directory and paste it into Tomcat's webapps/ directory.
   ```bash
   cp target/ROOT.war /path/to/tomcat/webapps/ROOT.war
   ```
4. **Launch the server:**
   Navigate to your Tomcat bin/ directory and execute the startup script:
   * **Windows:** double-click startup.bat (or run startup.bat from terminal)
   * **Linux/Mac:** ./startup.sh
5. **Verify the deployment:**
   Once Tomcat is running, the API will be accessible locally. You can verify it is active by visiting the discovery endpoint:
   http://localhost:8080/api/v1

---

## 3. Sample curl Commands

Here are sample curl commands demonstrating successful interactions with the different parts of the API:

**1. Create a new Room (POST)**
```bash
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d "{\"id\": \"LIB-301\", \"name\": \"Library Quiet Study\", \"capacity\": 50}"
```

**2. Register a new Sensor linked to the Room (POST)**
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d "{\"id\": \"CO2-001\", \"type\": \"CO2\", \"status\": \"ACTIVE\", \"roomId\": \"LIB-301\"}"
```

**3. Retrieve Sensors filtered by type (GET)**
```bash
curl -X GET "http://localhost:8080/api/v1/sensors?type=CO2" \
  -H "Accept: application/json"
```

**4. Add a Sensor Reading to the Sub-Resource (POST)**
```bash
curl -X POST http://localhost:8080/api/v1/sensors/CO2-001/readings \
  -H "Content-Type: application/json" \
  -d "{\"value\": 845.3}"
```

**5. Attempt to delete a Room that has active sensors assigned to it (DELETE - Returns 409 Conflict)**
```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301 \
  -H "Accept: application/json"
```

---

## 4. Report: Coursework Questions

**Part 1: Service Architecture & Setup**
**1. Default lifecycle of a JAX-RS resource class and its impact on in-memory data:**
By default, JAX-RS operates on a "per-request" lifecycle, meaning a new instance of the resource class is instantiated for every incoming HTTP request. If data structures (like maps or lists) were managed as standard instance variables inside the resource class, that data would be lost every time the request completed. To prevent data loss and safely manage state, I implemented a Singleton DataStore class using static declarations. I specifically chose ConcurrentHashMap for the collections to prevent race conditions and ensure thread-safe synchronization across concurrent Tomcat requests.

**2. The benefit of Hypermedia (HATEOAS) for client developers:**
HATEOAS (Hypermedia As The Engine Of Application State) allows a client to dynamically navigate an API by following links provided inside the JSON responses, rather than manually hardcoding URL structures based on static documentation. This benefits developers by heavily decoupling the client side from the server logic—if the server changes its internal routing structure in the future, the client naturally adapts without breaking, as it relies on the provided runtime links.

**Part 2: Room Management**
**1. Implications of returning only IDs versus full room objects:**
Returning only IDs radically minimizes the payload size and conserves network bandwidth, which is excellent for raw speed but forces the client to make multiple additional round-trip requests to fetch the actual metadata for each room (the N+1 problem). Returning full room objects simplifies client-side processing since only a single request is required to render the application interface, but at the cost of consuming more bandwidth if the client application never intends to display the deeper metadata.

**2. Is the DELETE operation idempotent in this implementation?**
Strictly speaking, the DELETE implementation behaves slightly differently on subsequent calls. The first DELETE request for a valid room removes it and understandably returns a 200 OK. If the client mistakenly sends the exact same DELETE request again, the system will return a 404 Not Found because the entity no longer exists. While a pure idempotent DELETE would return 200 OK endlessly, returning 404 is a deliberate, pragmatic design choice that guarantees the server state does not change after the first call, whilst clearly informing the client the resource is gone.

**Part 3: Sensor Operations & Linking**
**1. Technical consequences of sending data in an incorrect format (text/plain vs application/json):**
Because the resource explicitly defines @Consumes(MediaType.APPLICATION_JSON), the JAX-RS framework strictly filters incoming requests by reading the client's Content-Type header prior to invoking the method. If a client attempts to send text/plain or application/xml, JAX-RS automatically intercepts the mismatch at the framework level and returns an HTTP 415 Unsupported Media Type error. The underlying Java method is never even executed, safely preventing malformed parsing logic. 

**2. Contrast between query parameters (?type=) and URL paths (/type/CO2) for filtering:**
The query parameter approach is vastly superior for filtering collections because URL path segments should be strictly reserved for identifying specific nouns or physical resources. A query string allows for optional modifiers to be cleanly stacked (e.g. ?type=CO2&status=ACTIVE) without bloating the routing table. If path variables were used for filtering, the service would require rigid, dedicated route definitions for every possible combination of filters, drastically increasing API complexity.

**Part 4: Deep Nesting with Sub-Resources**
**1. Architectural benefits of the Sub-Resource Locator pattern:**
The Sub-Resource Locator pattern heavily promotes the Single Responsibility Principle. By delegating the nested /readings path to an entirely separate SensorReadingResource controller, we avoid creating a bloated "God class" where one file manages hundreds of endpoints. It massively simplifies the code by ensuring the SensorResource is only responsible for high-level sensor metadata, while the sub-resource securely handles internal data logic natively associated with historical metrics.

**Part 5: Advanced Error Handling, Exception Mapping & Logging**
**1. Why HTTP 422 is more semantically accurate than 404 for dependency validation:**
A 404 Not Found implies that the target URL endpoint does not exist. However, when placing a POST request with an invalid roomId inside the body, the target URL (/api/v1/sensors) *does* exist, and the JSON format is syntactically flawless. 422 Unprocessable Entity is precisely designed for this scenario: the server perfectly parses the request, but rejects processing it due to underlying semantic logical errors inside the payload (a broken primary/foreign key reference).

**2. Security risks associated with exposing internal Java stack traces:**
Exposing a stack trace allows external API consumers to directly view the internal file structures, class names, framework versions (e.g. Jersey, Tomcat), and database logic. From a cybersecurity perspective, this is lethal; attackers can actively weaponize this footprinting data to search for known CVEs vulnerabilities specifically targeting the revealed technology stack, dramatically simplifying the process of exploiting the server. 

**3. The advantage of JAX-RS filters for cross-cutting logging concerns:**
Cross-cutting concerns like observability apply identically globally across every single controller. By leveraging ContainerRequestFilter and ContainerResponseFilter, we ensure 100% logging coverage identically across all endpoints without writing duplicated Logger.info() statements inside individual methods. It heavily adheres to DRY (Don't Repeat Yourself) principles and guarantees an engineer won't accidentally forget to add logging when building new API routes in the future.
