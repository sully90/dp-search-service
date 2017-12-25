package com.github.onsdigital.search;

import com.github.onsdigital.fanoutcascade.exceptions.ExceptionHandler;
import com.github.onsdigital.fanoutcascade.exceptions.PurgingExceptionHandler;
import com.github.onsdigital.fanoutcascade.handlertasks.FanoutCascadeMonitoringTask;
import com.github.onsdigital.fanoutcascade.pool.FanoutCascade;
import com.github.onsdigital.fanoutcascade.pool.FanoutCascadeRegistry;
import com.github.onsdigital.search.fanoutcascade.handlers.TrainingSetHandler;
import com.github.onsdigital.search.fanoutcascade.handlers.PerformaceCheckerHandler;
import com.github.onsdigital.search.fanoutcascade.handlertasks.ModelTrainingTask;
import com.github.onsdigital.search.fanoutcascade.handlertasks.PerformanceCheckerTask;
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

        // Setup a exception handler
        ExceptionHandler exceptionHandler = new PurgingExceptionHandler();
        FanoutCascadeRegistry.getInstance().setExceptionHandler(exceptionHandler);

        // Setup FanoutCascade
        FanoutCascadeRegistry.getInstance().registerMonitoringThread();
        FanoutCascadeRegistry.getInstance().register(PerformanceCheckerTask.class, PerformaceCheckerHandler.class, 1);
        FanoutCascadeRegistry.getInstance().register(ModelTrainingTask.class, TrainingSetHandler.class, 1);

        // Submit the tasks
        FanoutCascade.getInstance().getLayerForTask(FanoutCascadeMonitoringTask.class).submit(new FanoutCascadeMonitoringTask());
        FanoutCascade.getInstance().getLayerForTask(PerformanceCheckerTask.class).submit(new PerformanceCheckerTask());
    }
}
