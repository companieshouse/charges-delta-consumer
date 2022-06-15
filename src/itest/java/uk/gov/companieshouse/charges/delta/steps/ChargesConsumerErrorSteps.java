package uk.gov.companieshouse.charges.delta.steps;

import java.util.List;
import java.util.concurrent.TimeUnit;
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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import uk.gov.companieshouse.delta.ChsDelta;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.common.Metadata.metadata;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ChargesConsumerErrorSteps {

    @Autowired
    public KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired
    protected TestRestTemplate restTemplate;
    @Value("${charges.delta.topic}")
    private String topic;

    private WireMockServer wireMockServer;
    private String companyNumber;
    private String chargeId;

    @Autowired
    private TestSupport testSupport;
    @Autowired
    public KafkaConsumer<String, Object> kafkaConsumer;

    public static final String RETRY_TOPIC_ATTEMPTS = "retry_topic-attempts";

    @Given("Stubbed Charges Data API endpoint will return {string} http response code")
    public void stubChargesDataApiEndpointForResponse(String statusValue){
        int requiredStatusValue = Integer.parseInt(statusValue);
        wireMockServer = testSupport.setupWiremock();

        stubFor(
            put(urlEqualTo("/company/" + companyNumber + "/charge/" + chargeId + "/internal"))
                .willReturn(aResponse()
                    .withStatus(requiredStatusValue)
                    .withHeader("Content-Type", "application/json"))
                .withMetadata(metadata()
                    .list("tags", "stubbed_for_error_test"))
        );
    }

    @Given("Stubbed Charges Data API endpoint will return {string} http response code for {string} and {string}")
    public void stubChargesDataApiEndpointForResponse(String statusValue, String companyNumber, String chargeId){
        this.companyNumber = companyNumber;
        this.chargeId = chargeId;
        int requiredStatusValue = Integer.parseInt(statusValue);
        wireMockServer = testSupport.setupWiremock();

        stubFor(
            put(urlEqualTo("/company/" + companyNumber + "/charge/" + chargeId + "/internal"))
                .willReturn(aResponse()
                    .withStatus(requiredStatusValue)
                    .withHeader("Content-Type", "application/json"))
                .withMetadata(metadata()
                    .list("tags", "stubbed_for_error_test"))
        );
    }

    @When("A non-avro format message is sent to the Kafka topic")
    public void aNonAvroFormatMessageIsSentToTheKafkaTopicChargesDeltaTopic()
        throws InterruptedException {
        kafkaTemplate.send(topic, "Not an AVRO message");
        kafkaTemplate.flush();
        TimeUnit.SECONDS.sleep(1);
    }

    @When("A valid avro message in with an invalid json payload is sent to the Kafka topic")
    public void aValidAvroMessageInWithAnInvalidJsonPayloadIsSentToTheKafkaTopic()
        throws InterruptedException {
        sendKafkaMessage(testSupport.createChsDeltaMessageNulPayload());
    }

    @When("a message with payload {string} is published to charges topic")
    public void messagePublishedToChargesTopic(String dataFile) throws InterruptedException {
        String chargesDeltaDataJson = testSupport.loadInputFile(dataFile);

        sendKafkaMessage(testSupport.createChsDeltaMessage(chargesDeltaDataJson));
    }

    @When("a message with payload without charges is published to charges topic")
    public void messagePayloadWithourChargesPublishedToChargesTopic() throws InterruptedException {
        String chargesDeltaDataJson = "{\"charges\": null}";
        ChsDelta deltaMessage = testSupport.createChsDeltaMessage(chargesDeltaDataJson);
        sendKafkaMessage(deltaMessage);
    }

    @Then("the message should be moved to topic {string}")
    public void the_message_should_be_moved_to_topic(String destinatonTopic) {
        ConsumerRecord<String, Object> singleRecord = KafkaTestUtils.getSingleRecord(kafkaConsumer,
                destinatonTopic, 5000L);
        assertNotNull(singleRecord);
    }

    @Then("Charges Data API endpoint is never invoked")
    public void chargesDataAPIEndpointIsNeverInvoked() {
        GetServeEventsResult events = wireMockServer.getServeEvents();
        assertTrue(events.getServeEvents().isEmpty());
    }

    @Then("Charges Data API endpoint is only invoked once")
    public void chargesDataAPIEndpointIsInvokedOnlyOnce() {
        GetServeEventsResult events = wireMockServer.getServeEvents();
        assertEquals(1, events.getServeEvents().size());
    }

    @Then("Charges Data API endpoint is retried {string}")
    public void chargesDataAPIEndpointIsretried(String retries) {
        int retryCount = Integer.parseInt(retries);
        GetServeEventsResult events = wireMockServer.getServeEvents();
        // initial attempt plus retries
        assertEquals(retryCount + 1, events.getServeEvents().size());
    }

    @Then("the message should be retried {string} on retry topic {string}")
    public void theMessageShouldBeRetried(String requiredRetries, String retryTopic) {
        ConsumerRecord<String, Object> singleRecord = KafkaTestUtils.getSingleRecord(kafkaConsumer,
                retryTopic, 5000L);

        assertThat(singleRecord.value()).isNotNull();
        List<Header> retryList = StreamSupport.stream(singleRecord.headers().spliterator(), false)
            .filter(header -> header.key().equalsIgnoreCase(RETRY_TOPIC_ATTEMPTS))
            .collect(Collectors.toList());

        assertThat(retryList.size()).isEqualTo(Integer.parseInt(requiredRetries) + 1);
    }

    private void sendKafkaMessage(ChsDelta deltaMessage) throws InterruptedException {
        kafkaTemplate.send(topic, deltaMessage);
        kafkaTemplate.flush();
        TimeUnit.SECONDS.sleep(1);
    }
}
