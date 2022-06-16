package uk.gov.companieshouse.charges.delta.steps;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.admin.model.GetServeEventsResult;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.header.Header;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import uk.gov.companieshouse.charges.delta.config.DelegatingLatch;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.common.Metadata.metadata;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ChargesConsumerDeleteSteps {

    private static final String HEALTHCHECK_URI = "/charges-delta-consumer/healthcheck";
    private static final String HEALTHCHECK_RESPONSE_BODY = "{\"status\":\"UP\"}";

    public static final String CONTENT_TYPE = "Content-Type";
    public static final String APPLICATION_JSON = "application/json";
    public static final String TAGS = "tags";
    public static final String STUBBED_FOR_DELETE_TEST = "stubbed_for_delete_test";
    public static final String RETRY_TOPIC_ATTEMPTS = "retry_topic-attempts";
    public static final String DELETE_URL = "/company/%s/charges/%s";

    @Autowired
    public KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired
    protected TestRestTemplate restTemplate;
    @Value("${charges.delta.topic}")
    private String topic;
    @Autowired
    private TestSupport testSupport;
    @Autowired
    public KafkaConsumer<String, Object> kafkaConsumer;
    @Autowired
    private DelegatingLatch delegatingLatch;

    private WireMockServer wireMockServer;
    private String companyNumber;
    private String chargeId;

    @Given("Charges delta consumer service for delete is running")
    public void charges_delta_consumer_service_is_running() {

        ResponseEntity<String> response = restTemplate.getForEntity(HEALTHCHECK_URI, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.valueOf(200));
        assertThat(response.getBody()).isEqualTo(HEALTHCHECK_RESPONSE_BODY);
        this.delegatingLatch.setLatch(new CountDownLatch(1));
    }

    @Given("Stubbed Charges Data API delete endpoint will return {string} http response code for {string} and {string}")
    public void stubChargesDataApiDeleteEndpointForResponse(String statusValue, String companyNumber, String chargeId){
        this.companyNumber = companyNumber;
        this.chargeId = chargeId;
        int requiredStatusValue = Integer.parseInt(statusValue);
        wireMockServer = testSupport.setupWiremock();

        stubFor(
            delete(urlEqualTo(String.format(DELETE_URL, companyNumber, chargeId)))
                .willReturn(aResponse()
                    .withStatus(requiredStatusValue)
                    .withHeader(CONTENT_TYPE, APPLICATION_JSON))
                .withMetadata(metadata()
                    .list(TAGS, STUBBED_FOR_DELETE_TEST))
        );
    }

    @When("delete message with payload {string} is published to charges topic")
    public void messagePublishedToChargesTopic(String dataFile) throws InterruptedException, ExecutionException, TimeoutException {
        String chargesDeltaDataJson = testSupport.loadInputFile(dataFile);
        kafkaTemplate.send(topic, testSupport.createChsDeltaMessage(chargesDeltaDataJson, true)).get(30, TimeUnit.SECONDS);
        delegatingLatch.getLatch().await();
    }

    @Then("delete message should be moved to topic {string}")
    public void the_message_should_be_moved_to_topic(String destinatonTopic) {
        ConsumerRecord<String, Object> singleRecord = KafkaTestUtils.getSingleRecord(kafkaConsumer,
                destinatonTopic, 5000L);
        assertNotNull(singleRecord);
    }

    @Then("Charges Data API delete endpoint is never invoked")
    public void chargesDataAPIEndpointIsNeverInvoked() {
        GetServeEventsResult events = wireMockServer.getServeEvents();
        assertTrue(events.getServeEvents().isEmpty());
        verify(0, deleteRequestedFor(urlMatching(String.format(
                DELETE_URL, this.companyNumber, this.chargeId))));
    }

    @Then("Charges Data API delete endpoint is only invoked once getting back {string}")
    public void chargesDataAPIEndpointIsInvokedOnlyOnce(String responseCode) {
        GetServeEventsResult events = wireMockServer.getServeEvents();
        assertEquals(1, events.getServeEvents().size());
        assertThat(events.getServeEvents().get(0).getResponse().getStatus())
                .isEqualTo(Integer.parseInt(responseCode));
        String deleteUrl = String.format(DELETE_URL, this.companyNumber, this.chargeId);
        assertThat(events.getServeEvents().get(0).getRequest().getUrl())
                .isEqualTo(deleteUrl);

        verify(1, deleteRequestedFor(urlMatching(deleteUrl)));
    }

    @Then("Charges Data API delete endpoint is retried {string} getting back {string}")
    public void chargesDataAPIDeleteEndpointIsRetried(String retries, String responseCode) {
        int retryCount = Integer.parseInt(retries);
        GetServeEventsResult events = wireMockServer.getServeEvents();
        // initial attempt plus retries
        assertEquals(retryCount , events.getServeEvents().size());
        assertThat(events.getServeEvents().get(0).getResponse().getStatus())
                .isEqualTo(Integer.parseInt(responseCode));
        String deleteUrl = String.format(DELETE_URL, this.companyNumber, this.chargeId);
        assertThat(events.getServeEvents().get(0).getRequest().getUrl())
                .isEqualTo(deleteUrl);

        verify(retryCount, deleteRequestedFor(urlMatching(deleteUrl)));
    }

    @Then("delete message should be retried {string} on retry topic and moved to {string}")
    public void theMessageShouldBeRetried(String requiredRetries, String retryTopic) {
        ConsumerRecord<String, Object> singleRecord = KafkaTestUtils.getSingleRecord(kafkaConsumer,
                retryTopic, 5000L);

        assertThat(singleRecord.value()).isNotNull();
        List<Header> retryList = StreamSupport.stream(singleRecord.headers().spliterator(), false)
            .filter(header -> header.key().equalsIgnoreCase(RETRY_TOPIC_ATTEMPTS))
            .collect(Collectors.toList());

        assertThat(retryList.size()).isEqualTo(Integer.parseInt(requiredRetries));
    }

}
