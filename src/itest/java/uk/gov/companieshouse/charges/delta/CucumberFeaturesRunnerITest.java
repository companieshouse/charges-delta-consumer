package uk.gov.companieshouse.charges.delta;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import io.cucumber.spring.CucumberContextConfiguration;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(features = "src/itest/resources/features",
        plugin = {"pretty", "json:target/cucumber-report.json"},
        glue = {"uk.gov.companieshouse.charges.delta"})
@CucumberContextConfiguration
public class CucumberFeaturesRunnerITest extends AbstractIntegrationTest {

    public static void start() {
        mongoDBContainer.start();
    }

    public static void stop() {
        mongoDBContainer.stop();
    }
}