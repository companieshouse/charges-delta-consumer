package uk.gov.companieshouse.charges.delta.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.FileCopyUtils;
import uk.gov.companieshouse.api.delta.AdditionalNotice;
import uk.gov.companieshouse.api.delta.Charge;
import uk.gov.companieshouse.api.delta.ChargesDelta;
import uk.gov.companieshouse.api.delta.Person;
import uk.gov.companieshouse.charges.delta.producer.ChargesDeltaProducer;
import uk.gov.companieshouse.charges.delta.transformer.ChargesApiTransformer;
import uk.gov.companieshouse.delta.ChsDelta;
import uk.gov.companieshouse.logging.Logger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
public class ChargesDeltaProcessorTest {

    private ChargesDeltaProcessor deltaProcessor;

    @Mock
    private ChargesDeltaProducer chargesDeltaProducer;

    @Mock
    private ChargesApiTransformer transformer;

    @Mock
    private Logger logger;

    @BeforeEach
    void setUp() {
        deltaProcessor = new ChargesDeltaProcessor(chargesDeltaProducer, transformer, logger);
    }

    @Test
    @DisplayName("Transforms a kafka message containing a ChsDelta payload into an ChargesDelta")
    void When_ValidChsDeltaMessage_Expect_ValidChargesDeltaMapping() throws IOException {
        Message<ChsDelta> mockChsDeltaMessage = createChsDeltaMessage("charges-delta-example.json");
        ChargesDelta expectedChargesDelta = createChargesDelta();
        Charge charge = expectedChargesDelta.getCharges().get(0);
        when(transformer.transform(charge)).thenCallRealMethod();
        deltaProcessor.processDelta(mockChsDeltaMessage);
        verify(transformer).transform(charge);
    }

    @Test
    @DisplayName("Transforms a kafka message containing a ChsDelta payload into an ChargesDelta with no items")
    void When_InValidChsDeltaMessage_Expect_Exception() throws IOException {
        Message<ChsDelta> mockChsDeltaMessage = createChsDeltaMessage("charges-delta-example-no-charge.json");
        deltaProcessor.processDelta(mockChsDeltaMessage);
        verifyNoInteractions(transformer);
    }

    private Message<ChsDelta> createChsDeltaMessage(String fileName) throws IOException {
        InputStreamReader exampleChargesJsonPayload = new InputStreamReader(
                Objects.requireNonNull(ClassLoader.getSystemClassLoader().getResourceAsStream(fileName)));
        String chargesData = FileCopyUtils.copyToString(exampleChargesJsonPayload);

        ChsDelta mockChsDelta = ChsDelta.newBuilder()
                .setData(chargesData)
                .setContextId("context_id")
                .setAttempt(1)
                .build();

        return MessageBuilder
                .withPayload(mockChsDelta)
                .setHeader(KafkaHeaders.RECEIVED_TOPIC, "test")
                .setHeader("CHARGES_DELTA_RETRY_COUNT", 1)
                .build();
    }

    private ChargesDelta createChargesDelta() {

        ChargesDelta chargesDelta = new ChargesDelta();
        Charge charge = new Charge();
        charge.setCompanyNumber("01099198");
        charge.setDeltaAt("20211029142043360560");
        charge.setId("3387778");

        List<Person> personList = new ArrayList<>();
        Person person = new Person();
        person.setPerson("LOMBARD NORTH CENTRAL PLC");
        personList.add(person);
        charge.setPersonsEntitled(personList);

        charge.noticeType("395");
        charge.setTransDesc("PARTICULARS OF MORTGAGE/CHARGE");
        charge.submissionType("9");
        charge.deliveredOn("20070609");


        List<AdditionalNotice> additionalNoticeList = new ArrayList<>();
        AdditionalNotice additionalNotice = new AdditionalNotice();
        additionalNotice.setNoticeType("403a");
        additionalNotice.setTransId("3387778");
        additionalNotice.setTransDesc("DECLARATION OF SATISFACTION OF MORTGAGE/CHARGE");
        additionalNotice.setSubmissionType("9");
        additionalNotice.setDeliveredOn("20070809");
        additionalNoticeList.add(additionalNotice);
        charge.setAdditionalNotices(additionalNoticeList);

        charge.setChargeNumber(Integer.valueOf("577"));
        charge.setMigratedFrom("STEM");
        charge.setAmountSecured("£48,000.00                               AND ALL OTHER MONIES DUE OR TO BECOME DUE");
        charge.setType("MARINE MORTGAGE                         ");
        charge.setShortParticulars("LEGEND 33 HULL ID: LUH33057L405         ");
        charge.setStatus(Integer.valueOf("1"));
        charge.setSatisfiedOn("20070809");
        charge.setCreatedOn("20070605");
        chargesDelta.addChargesItem(charge);

        return chargesDelta;
    }
}
