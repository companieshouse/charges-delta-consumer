package uk.gov.companieshouse.charges.delta.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.FileCopyUtils;
import uk.gov.companieshouse.api.charges.ChargesApi;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.api.delta.AdditionalNotice;
import uk.gov.companieshouse.api.delta.Charge;
import uk.gov.companieshouse.api.delta.ChargesDelta;
import uk.gov.companieshouse.api.delta.Person;
import uk.gov.companieshouse.delta.ChsDelta;

import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TestData {

    private ObjectMapper objectMapper;
    public ChargesDelta createChargesDelta() {

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

        charge.setChargeNumber("577");
        charge.setMigratedFrom(Charge.MigratedFromEnum.STEM);
        charge.setAmountSecured("£48,000.00                               AND ALL OTHER MONIES DUE OR TO BECOME DUE");
        charge.setType("MARINE MORTGAGE                         ");
        charge.setShortParticulars("LEGEND 33 HULL ID: LUH33057L405         ");
        charge.setStatus("1");
        charge.setSatisfiedOn("20070809");
        charge.setCreatedOn("20070605");
        chargesDelta.addChargesItem(charge);

        return chargesDelta;
    }

    public Message<ChsDelta> createChsDeltaMessage(String fileName) throws IOException {
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

    public InternalChargeApi mockInternalChargeApi()
    {
        return new InternalChargeApi();

    }

    public ChargesDelta createChargesDelta(String jsonFileName) throws IOException {

        String chargesDelta = loadTestdataFile(jsonFileName);
        return getObjectMapper().readValue(chargesDelta, ChargesDelta.class);

    }

    public ChargesApi createChargesApi(String jsonFileName) throws IOException {

        String chargesApi = loadTestdataFile(jsonFileName);
        return getObjectMapper().readValue(chargesApi, ChargesApi.class);

    }

    private ObjectMapper getObjectMapper()
    {
        objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setDateFormat(new SimpleDateFormat("yyyyMMdd"));
        return objectMapper;
    }

    public String loadTestdataFile(String jsonFileName) throws IOException {
        InputStreamReader exampleChargesJsonPayload = new InputStreamReader(
                Objects.requireNonNull(ClassLoader.getSystemClassLoader().getResourceAsStream(jsonFileName)));
        return FileCopyUtils.copyToString(exampleChargesJsonPayload);
    }
}
