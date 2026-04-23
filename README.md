# Smart Campus Sensor & Room Management API
### 5COSC022W - Client Server Architectures Coursework 2025/26

---

## Overview

This project is a RESTful API built using JAX-RS (Jersey) with an embedded Grizzly HTTP server.
The API manages Rooms and Sensors across a university Smart Campus. All data is stored 
in-memory using ConcurrentHashMap. No database is used.

**Base URL:** `http://localhost:8080/api/v1/`

---
## API Endpoints

| Method | URL | Description |
|--------|-----|-------------|
| GET | /api/v1/ | Discovery - API metadata |
| GET | /api/v1/rooms | Get all rooms |
| POST | /api/v1/rooms | Create a new room |
| GET | /api/v1/rooms/{roomId} | Get room by ID |
| DELETE | /api/v1/rooms/{roomId} | Delete a room |
| GET | /api/v1/sensors | Get all sensors |
| GET | /api/v1/sensors?type=CO2 | Filter sensors by type |
| POST | /api/v1/sensors | Register a new sensor |
| GET | /api/v1/sensors/{sensorId} | Get sensor by ID |
| DELETE | /api/v1/sensors/{sensorId} | Delete a sensor |
| GET | /api/v1/sensors/{sensorId}/readings | Get all readings |
| POST | /api/v1/sensors/{sensorId}/readings | Add a new reading |
| GET | /api/v1/sensors/{sensorId}/readings/{id} | Get single reading |

---

## Sample curl Commands

**1. Discovery**
```bash
curl -X GET http://localhost:8080/api/v1/
```

**2. Create a Room**
```bash
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d "{\"id\":\"LIB-301\",\"name\":\"Library Quiet Study\",\"capacity\":80}"
```

**3. Get All Rooms**
```bash
curl -X GET http://localhost:8080/api/v1/rooms
```

**4. Create a Sensor**
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d "{\"id\":\"TEMP-001\",\"type\":\"Temperature\",\"status\":\"ACTIVE\",\"currentValue\":22.5,\"roomId\":\"LIB-301\"}"
```

**5. Add a Reading**
```bash
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d "{\"value\":25.6}"
```

**6. Filter Sensors by Type**
```bash
curl -X GET "http://localhost:8080/api/v1/sensors?type=Temperature"
```

**7. Delete Room with Sensors (expect 409)**
```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```

**8. Create Sensor with wrong roomId (expect 422)**
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d "{\"id\":\"TEMP-002\",\"type\":\"Temperature\",\"status\":\"ACTIVE\",\"currentValue\":20,\"roomId\":\"INVALID\"}"
```

**9. Post Reading to MAINTENANCE sensor (expect 403)**
```bash
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-003/readings \
  -H "Content-Type: application/json" \
  -d "{\"value\":30}"
```

**10. Get Reading History**
```bash
curl -X GET http://localhost:8080/api/v1/sensors/TEMP-001/readings
```

---

## Project Structure

```
scfinal/
├── pom.xml
└── src/main/java/com/smartcampus/
    ├── Main.java
    ├── JsonUtil.java
    ├── model/
    │   ├── Room.java
    │   ├── Sensor.java
    │   └── SensorReading.java
    ├── store/
    │   └── DataStore.java
    ├── resource/
    │   ├── DiscoveryResource.java
    │   ├── RoomResource.java
    │   ├── SensorResource.java
    │   └── SensorReadingResource.java
    ├── exception/
    │   ├── RoomNotEmptyException.java
    │   ├── LinkedResourceNotFoundException.java
    │   └── SensorUnavailableException.java
    ├── mapper/
    │   ├── RoomNotEmptyExceptionMapper.java
    │   ├── LinkedResourceNotFoundExceptionMapper.java
    │   ├── SensorUnavailableExceptionMapper.java
    │   └── GlobalExceptionMapper.java
    └── filter/
        └── LoggingFilter.java
```

---

## Conceptual Report — Question Answers

---
### Part 1.1 — JAX-RS Resource Class Lifecycle
Q: In your report, explain the default lifecycle of a JAX-RS Resource class. Is a
new instance instantiated for every incoming request, or does the runtime treat it as a
singleton? Elaborate on how this architectural decision impacts the way you manage and
synchronize your in-memory data structures (maps/lists) to prevent data loss or race con
ditions

By default, JAX-RS creates a **new instance** of each resource class for every incoming 
HTTP request. This is called the per-request lifecycle. Once the request is done and 
the response is sent, that instance is thrown away.

This means if I store data inside the resource class as a normal field like 
`HashMap<String, Room> rooms = new HashMap<>()`, that data gets reset on every 
single request and nothing is saved. To fix this, I created a separate 
`DataStore.java` class that holds all the data as `static` fields. Static fields 
belong to the class itself, not to any instance, so they survive across all requests.

Because multiple requests can come in at the same time from different threads, I used 
`ConcurrentHashMap` instead of a plain `HashMap`. A plain HashMap can get corrupted 
if two threads write to it at the same time. ConcurrentHashMap handles that 
automatically and is thread-safe. Another option Jersey provides is the `@Singleton` 
annotation on the resource class, which tells Jersey to only create one instance and 
reuse it for all requests.

---

### Part 1.2 — HATEOAS

HATEOAS stands for Hypermedia As The Engine Of Application State. It is considered 
Level 3 on the Richardson Maturity Model, which is the highest level of REST design.

In a HATEOAS API, every response includes links that tell the client what they can 
do next. For example, when you call `GET /api/v1/`, the response does not just return 
data — it also returns links to `/api/v1/rooms` and `/api/v1/sensors`. The client 
can follow those links without needing to know the URLs in advance.

This is better than static documentation because documentation goes out of date 
quickly. If I change a URL path on the server, a client using HATEOAS links will 
still work because it just follows whatever link the server gives. Clients that 
hard-code URLs into their code will break. HATEOAS also means less training needed 
for developers using the API because the API explains itself through its responses.

---

### Part 2.1 — Returning Full Objects vs IDs Only

If the API returns only IDs like `["LIB-301", "LAB-102"]`, the response is small 
and fast. But the client then has to send one extra GET request for every single 
room to get the actual data. If there are 100 rooms, that is 100 extra requests. 
This is called the N+1 problem and it makes the system slow.

If the API returns full room objects in the list, the client gets everything in one 
request and does not need to make more calls. This is better for performance even 
though the response payload is bigger. In real systems this is handled by using 
pagination like `?page=1&size=20` so the response does not get too large.

For this coursework I chose to return full objects because it is better for a 
facilities management system where users need to see room details right away.

---

### Part 2.2 — Is DELETE Idempotent?

Yes, DELETE is idempotent. Idempotent means that sending the same request multiple 
times produces the same result on the server.

First call: `DELETE /api/v1/rooms/LIB-301` → room is found, deleted, returns 200 OK.

Second call: `DELETE /api/v1/rooms/LIB-301` → room is not found, returns 404 Not Found.

The response codes are different, but the important thing is the server state is 
the same after both calls — the room does not exist. The server is not changed a 
second time. This makes DELETE safe to retry. For example if a network timeout 
happens and the client is not sure if the first request worked, they can safely 
send it again without worrying about deleting something twice.

POST is not idempotent because sending POST twice creates two resources.

---

### Part 3.1 — What Happens if Client Sends Wrong Content-Type?

The `@Consumes(MediaType.APPLICATION_JSON)` annotation tells JAX-RS to only 
accept requests where the Content-Type header is `application/json`.

If a client sends `text/plain` or `application/xml`, JAX-RS checks the Content-Type 
before even running the resource method. It sees there is no method that can handle 
that media type and automatically sends back **HTTP 415 Unsupported Media Type**. 
The request body is completely ignored and no business logic runs at all.

This is useful for security because it stops the server from trying to parse 
unexpected formats which could cause errors. It also tells the API client exactly 
what format they need to use.

---

### Part 3.2 — @QueryParam vs Path Segment for Filtering

Path segment approach like `/api/v1/sensors/type/CO2` is wrong because it suggests 
that `type/CO2` is its own separate resource in the system, which is not true. 
It is just a filter.

Query parameter approach like `/api/v1/sensors?type=CO2` is the correct REST 
convention. Query parameters are for filtering, sorting, and searching a collection, 
not for identifying a resource.

Benefits of query parameters:
- They are optional — `GET /api/v1/sensors` still works without any filter
- You can combine multiple filters easily: `?type=CO2&status=ACTIVE`
- The resource URL stays clean and stable
- HTTP caches and proxies understand query parameters for filtering

If I used path segments, adding more filters would make the URL very messy and hard 
to maintain.

---

### Part 4.1 — Sub-Resource Locator Pattern Benefits

The Sub-Resource Locator pattern means a resource method returns an instance of 
another class that handles the rest of the URL. In my code, `SensorResource` has 
a method `@Path("{sensorId}/readings")` that returns `new SensorReadingResource(sensorId)`. 
All reading operations are then handled by that separate class.

Benefits compared to putting everything in one big class:

**1. Single Responsibility** — `SensorResource` only deals with sensors. 
`SensorReadingResource` only deals with readings. Each class has one job.

**2. Easier to manage** — If I put 50 endpoints in one class it becomes very hard 
to read and debug. Separate classes keep things clean.

**3. Context passing** — I pass `sensorId` in the constructor so the reading 
resource always knows which sensor it is working with. No need to extract the 
path parameter again in every method.

**4. Matches real world** — Sensors own their readings, just like rooms own their 
sensors. The code structure mirrors the real campus structure.

---

### Part 5.2 — Why 422 Instead of 404 for Invalid roomId?

HTTP 404 means the URL endpoint itself was not found. For example if someone calls 
`GET /api/v1/rooms/ABC123` and that room does not exist, 404 is correct because 
the resource at that URL is missing.

But when a client sends `POST /api/v1/sensors` with a valid JSON body that contains 
a `roomId` pointing to a room that does not exist, the situation is different. The 
endpoint `/api/v1/sensors` does exist and works fine. The problem is inside the 
request body — the `roomId` value references something that does not exist in 
the system.

HTTP 422 Unprocessable Entity means the JSON was received and parsed correctly, 
but the content makes no sense or cannot be processed. This is exactly what 
happened — the JSON is valid but the foreign key inside it is broken.

Using 404 here would confuse the client into thinking the API endpoint is missing, 
when really their data is wrong. 422 gives the right message.

---

### Part 5.4 — Security Risk of Exposing Stack Traces

Sending a Java stack trace back to the client in an API response is a serious 
security problem for several reasons:

**Package and class names** — The trace shows things like 
`com.smartcampus.store.DataStore` which tells an attacker exactly how the 
application is structured internally.

**Library versions** — The trace shows which libraries are used and their versions. 
An attacker can look these up in security databases like CVE to find known 
vulnerabilities in that exact version.

**File paths** — Stack traces sometimes include the full file path on the server 
like `C:\Users\HP\Downloads\scfinal\...` which reveals the server's directory 
structure.

**Logic flow** — Method names and line numbers reveal how the code works, helping 
an attacker figure out how to trigger specific bugs.

My `GlobalExceptionMapper` catches all unexpected errors, logs the full details 
on the server side using `java.util.logging.Logger`, and only sends back a 
safe generic message to the client saying "An unexpected error occurred."

---

### Part 5.5 — Why Use Filters Instead of Manual Logging?

If I put `Logger.info()` statements manually inside every resource method, these 
problems come up:

**Repetition** — I have to write the same logging code in every method. If I have 
20 endpoints, I write it 20 times.

**Easy to forget** — When I add a new endpoint later, I might forget to add the 
logging. Then that endpoint has no logs and I cannot see what is happening.

**Harder to change** — If I want to change the log format, I have to go into every 
single method and update it.

Using a JAX-RS filter solves all of this. The `LoggingFilter` class implements 
`ContainerRequestFilter` and `ContainerResponseFilter`. It is registered once and 
automatically runs for every single request and response in the whole application. 
If I add 10 more endpoints tomorrow, they are all automatically logged with no 
extra work.

This follows the principle of Separation of Concerns — resource methods should 
only contain business logic, not logging infrastructure.

---

## HTTP Status Codes Used

| Code | Meaning | When |
|------|---------|------|
| 200 | OK | Successful GET or DELETE |
| 201 | Created | Successful POST |
| 400 | Bad Request | Missing required fields |
| 403 | Forbidden | Reading from MAINTENANCE sensor |
| 404 | Not Found | Room or Sensor ID does not exist |
| 409 | Conflict | Deleting room that still has sensors |
| 415 | Unsupported Media Type | Wrong Content-Type sent |
| 422 | Unprocessable Entity | Sensor POST with invalid roomId |
| 500 | Internal Server Error | Unexpected server error |
