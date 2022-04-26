package uk.gov.companieshouse.charges.delta.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import org.apache.commons.io.FileUtils;
import org.springframework.util.ResourceUtils;
import uk.gov.companieshouse.api.delta.ChargesDelta;
import uk.gov.companieshouse.delta.ChsDelta;

public class TestData {

    private ObjectMapper objectMapper;

    public ChsDelta createChsDeltaMessage(String chargesDeltaData) throws IOException {

        return ChsDelta.newBuilder()
                .setData(chargesDeltaData)
                .setContextId("context_id")
                .setAttempt(1)
                .build();
    }

    public String loadFile(String dir, String fileName) {
        try {
            return FileUtils.readFileToString(ResourceUtils.getFile("classpath:payloads/"+ dir
                    + "/" + fileName), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Unable to locate file %s", fileName));
        }
    }

    public String loadInputFile(String fileName) {
        return loadFile("input", fileName);
    }

    public String loadOutputFile(String fileName) {
        return loadFile("output", fileName);
    }

    public ChargesDelta createChargesDelta(String chargesDeltaData) throws IOException {

        return getObjectMapper().readValue(chargesDeltaData, ChargesDelta.class);

    }

    private ObjectMapper getObjectMapper()
    {
        objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setDateFormat(new SimpleDateFormat("yyyyMMdd"));
        return objectMapper;
    }

}