package com.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Discovery Endpoint — GET /api/v1
 *
 * Returns API metadata: version, admin contact, and hypermedia links to
 * all primary resource collections. This implements a basic HATEOAS pattern,
 * allowing clients to navigate the API without needing hardcoded URLs.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Response discover() {

        // Build the links map (HATEOAS — Hypermedia As The Engine Of Application State)
        Map<String, String> links = new LinkedHashMap<>();
        links.put("rooms",   "/api/v1/rooms");
        links.put("sensors", "/api/v1/sensors");

        // Build the full response body
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("api",         "Smart Campus Sensor & Room Management API");
        response.put("version",     "1.0.0");
        response.put("description", "REST API for managing campus rooms, sensors, and sensor readings");
        response.put("contact",     "admin@smartcampus.university.ac.uk");
        response.put("status",      "RUNNING");
        response.put("links",       links);

        return Response.ok(response).build();
    }
}
