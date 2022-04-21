package uk.gov.companieshouse.charges.delta.steps;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import uk.gov.companieshouse.api.delta.ChargesDelta;
import uk.gov.companieshouse.charges.delta.processor.EncoderUtil;
import uk.gov.companieshouse.charges.delta.service.ApiClientService;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getAllServeEvents;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.removeEventsByStubMetadata;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.common.Metadata.metadata;
import static org.assertj.core.api.Assertions.assertThat;


public class ChargesConsumerSteps {

    @Value("${wiremock.server.port}")
    private String port;

    @Value("${charges.delta.topic}")
    private String topic;

    private static WireMockServer wireMockServer;

    private String companyNumber;

    private String chargeId;

    @Autowired
    private ApiClientService apiClientService;

    @Autowired
    private EncoderUtil encoderUtil;

    @Autowired
    public KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    protected TestRestTemplate restTemplate;

    private TestData testData = new TestData();

    @Given("Charges delta consumer service is running")
    public void charges_delta_consumer_service_is_running() {

        ResponseEntity<String> response = restTemplate.getForEntity("/healthcheck", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.valueOf(200));
        assertThat(response.getBody()).isEqualTo("I am healthy");
    }

    @When("a message with payload {string} is published to topic")
    public void a_message_is_published_to_topic(String dataFile)
            throws InterruptedException, IOException {
        setupWiremock();

        String chargesDeltaDataJson = testData.loadInputFile(dataFile);
        ChargesDelta chargesDeltaData = testData.createChargesDelta(chargesDeltaDataJson);
        companyNumber = chargesDeltaData.getCharges().get(0).getCompanyNumber();
        chargeId = chargesDeltaData.getCharges().get(0).getId();
        chargeId = encoderUtil.encodeWithSha1(chargeId);
        stubChargeDataApi("a_message_is_published_to_topic");
        kafkaTemplate.send(topic, testData.createChsDeltaMessage(chargesDeltaDataJson));
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await(5, TimeUnit.SECONDS);
    }

    @Then("the Consumer should process and send a request with payload {string} "
            + "to the Charges Data API getting back {int}")
    public void should_process_and_send_a_request_to_the_charges_data_api(
            String apiRequestPayloadFile, int responseCode) throws JSONException {

        List<ServeEvent> allServeEvents = getAllServeEvents();
        Optional<ServeEvent> serveEvent = allServeEvents.stream().findFirst();
        String request = serveEvent.get().getRequest().getBodyAsString();
        //verify put request along with delta_at
        verify(1, putRequestedFor(
                urlEqualTo("/company/" + companyNumber + "/charge/" + chargeId
                + "/internal"))
                .withRequestBody(matchingJsonPath("$.internal_data.delta_at")));

        assertThat(serveEvent.get().getResponse().getStatus()).isEqualTo(responseCode);
        //assert all fields in the payload, except for delta_at as wiremock is
        // treating it differently
        //delta_at is being verified above using jsonpath
        JSONAssert.assertEquals(testData.loadOutputFile(apiRequestPayloadFile), request,
                new CustomComparator(JSONCompareMode.LENIENT,
                        new Customization("external_data.etag", (o1, o2) -> true),
                        new Customization("internal_data.delta_at", (o1, o2) -> true)));

        removeEventsByStubMetadata(matchingJsonPath("$.tags[0]",
                equalTo("a_message_is_published_to_topic")));

        wireMockServer.stop();
    }

    private void setupWiremock() {
        wireMockServer = new WireMockServer(Integer.parseInt(port));
        wireMockServer.start();
        configureFor("localhost", Integer.parseInt(port));
    }

    private void stubChargeDataApi(String testMethodIdentifier) {
        stubFor(
                put(urlEqualTo("/company/" + companyNumber + "/charge/" + chargeId + "/internal"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json"))
                        .withMetadata(metadata()
                                .list("tags", testMethodIdentifier))
        );
    }

}
