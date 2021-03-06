package uk.gov.companieshouse.charges.delta.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

/**
 * Configuration class for logging.
 */
@Configuration
public class LoggingConfig {

    private static Logger staticLogger;

    @Value("${logger.namespace}")
    private String loggerNamespace;

    /**
     * Creates a logger with specified namespace.
     *
     * @return the {@link LoggerFactory} for the specified namespace
     */
    @Bean
    public Logger logger() {
        Logger loggerBean = LoggerFactory.getLogger(loggerNamespace);
        staticLogger = loggerBean;
        return loggerBean;
    }

    public static Logger getLogger() {
        return staticLogger;
    }
}
