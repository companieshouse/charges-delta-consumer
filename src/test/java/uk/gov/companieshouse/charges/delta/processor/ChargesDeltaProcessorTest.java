package uk.gov.companieshouse.charges.delta.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import uk.gov.companieshouse.api.delta.Charge;
import uk.gov.companieshouse.api.delta.ChargesDelta;
import uk.gov.companieshouse.charges.delta.model.TestData;
import uk.gov.companieshouse.charges.delta.producer.ChargesDeltaProducer;
import uk.gov.companieshouse.charges.delta.transformer.ChargesApiTransformer;
import uk.gov.companieshouse.delta.ChsDelta;
import uk.gov.companieshouse.logging.Logger;

import java.io.IOException;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ChargesDeltaProcessorTest {

    private ChargesDeltaProcessor deltaProcessor;

    @Mock
    private ChargesDeltaProducer chargesDeltaProducer;

    @Mock
    private ChargesApiTransformer transformer;

    @Mock
    private Logger logger;

    private TestData testData;

    @BeforeEach
    void setUp() {
        deltaProcessor = new ChargesDeltaProcessor(chargesDeltaProducer, transformer, logger);
        testData = new TestData();
    }

    @Test
    @DisplayName("Transforms a kafka message containing a ChsDelta payload into an ChargesDelta")
    void When_ValidChsDeltaMessage_Expect_ValidChargesDeltaMapping() throws IOException {
        Message<ChsDelta> mockChsDeltaMessage = testData.createChsDeltaMessage("charges-delta-example.json");
        ChargesDelta expectedChargesDelta = testData.createChargesDelta();
        Charge charge = expectedChargesDelta.getCharges().get(0);
        when(transformer.transform(charge)).thenCallRealMethod();
        deltaProcessor.processDelta(mockChsDeltaMessage);
        verify(transformer).transform(charge);
    }

    @Test
    @DisplayName("Transforms a kafka message containing a ChsDelta payload into an ChargesDelta with no items")
    void When_InValidChsDeltaMessage_Expect_Exception() throws IOException {
        Message<ChsDelta> mockChsDeltaMessage = testData.createChsDeltaMessage("charges-delta-example-no-charge.json");
        deltaProcessor.processDelta(mockChsDeltaMessage);
        verifyNoInteractions(transformer);
    }

}
