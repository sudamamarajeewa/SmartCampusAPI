package com.smartcampus;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * JAX-RS Application entry point.
 * All resources under com.smartcampus are auto-discovered via web.xml package scanning.
 * The @ApplicationPath annotation sets the base URI prefix for all REST endpoints.
 */
@ApplicationPath("/api/v1")
public class SmartCampusApplication extends Application {
    // No explicit registration needed — web.xml package scanning handles discovery
}
