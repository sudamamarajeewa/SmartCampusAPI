package com.smartcampus.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * API Observability Filter — logs every incoming request and outgoing response.
 *
 * Implements both ContainerRequestFilter and ContainerResponseFilter so a
 * single
 * class handles both sides of the HTTP lifecycle.
 * Using JAX-RS filters for cross-cutting concerns like logging is better than
 * inserting Logger.info() into every resource method because:
 * - It avoids code duplication across dozens of endpoints
 * - Resource methods stay focused on business logic only
 * - Logging can be added/removed/changed in one place without touching
 * resources
 * - Follows the Separation of Concerns principle
 *
 * @Provider — tells Jersey to auto-discover and register this filter via
 *           package scanning.
 */
@Provider
public class ApiLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = Logger.getLogger(ApiLoggingFilter.class.getName());

    /**
     * Runs BEFORE the request reaches a resource method.
     * Logs the HTTP method and full request URI.
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        LOGGER.info(String.format(
                "[REQUEST]  --> %s %s",
                requestContext.getMethod(),
                requestContext.getUriInfo().getRequestUri()));
    }

    /**
     * Runs AFTER the resource method has built a response.
     * Logs the final HTTP status code returned to the client.
     */
    @Override
    public void filter(ContainerRequestContext requestContext,
            ContainerResponseContext responseContext) throws IOException {
        LOGGER.info(String.format(
                "[RESPONSE] <-- %d %s  (for %s %s)",
                responseContext.getStatus(),
                statusText(responseContext.getStatus()),
                requestContext.getMethod(),
                requestContext.getUriInfo().getPath()));
    }

    /** Maps status code to a human-readable label for cleaner log output. */
    private String statusText(int status) {
        switch (status) {
            case 200:
                return "OK";
            case 201:
                return "Created";
            case 204:
                return "No Content";
            case 400:
                return "Bad Request";
            case 403:
                return "Forbidden";
            case 404:
                return "Not Found";
            case 409:
                return "Conflict";
            case 415:
                return "Unsupported Media Type";
            case 422:
                return "Unprocessable Entity";
            case 500:
                return "Internal Server Error";
            case 501:
                return "Not Implemented";
            default:
                return "";
        }
    }
}
