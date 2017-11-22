package com.github.onsdigital.search;

import com.github.onsdigital.search.server.SearchEngineService;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.TracingConfig;
import org.glassfish.jersey.server.mvc.jsp.JspMvcFeature;


public class App extends ResourceConfig {

    public App() {
        // Resources.
        packages(SearchEngineService.class.getPackage().getName());

        // MVC.
        register(JspMvcFeature.class);
        property(JspMvcFeature.TEMPLATES_BASE_PATH, "/WEB-INF/jsp");

        // Logging.
//        register(LoggingFeature.class);

        // Tracing support.
        property(ServerProperties.TRACING, TracingConfig.ON_DEMAND.name());
    }
}
