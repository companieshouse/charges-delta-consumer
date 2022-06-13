package uk.gov.companieshouse.charges.delta.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.charges.*;
import uk.gov.companieshouse.api.delta.Charge;
import uk.gov.companieshouse.logging.Logger;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DescriptiveChargeApiMapperTest {

    @Mock
    private ChargeApiMapper delegate;

    @Mock
    private TextFormatter textFormatter;

    @Mock
    private Logger logger;

    @Mock
    private Charge charge;

    @Mock
    private ChargeApi chargeApi;

    @Mock
    private ClassificationApi classificationApi;

    @Mock
    private ParticularsApi particularsApi;

    @Mock
    private SecuredDetailsApi securedDetailsApi;

    @Mock
    private PersonsEntitledApi personsEntitledApi;

    @Test
    void testChargeToChargeApiMapsFieldsWithFlagEnabled() throws InvocationTargetException,
            NoSuchMethodException, IllegalAccessException {
        // given
        DescriptiveChargeApiMapper mapper = new DescriptiveChargeApiMapper(delegate, textFormatter, true, logger);
        when(delegate.chargeToChargeApi(any(), any())).thenReturn(chargeApi);
        when(chargeApi.getClassification()).thenReturn(classificationApi);
        when(classificationApi.getDescription()).thenReturn("classification");
        when(chargeApi.getParticulars()).thenReturn(particularsApi);
        when(particularsApi.getDescription()).thenReturn("particulars");
        when(chargeApi.getSecuredDetails()).thenReturn(securedDetailsApi);
        when(securedDetailsApi.getDescription()).thenReturn("secured_details");
        when(chargeApi.getPersonsEntitled()).thenReturn(Collections.singletonList(personsEntitledApi));
        when(personsEntitledApi.getName()).thenReturn("name");

        // when
        ChargeApi result = mapper.chargeToChargeApi(charge, "12345678");

        // then
        assertSame(chargeApi, result);
        verify(textFormatter).formatAsSentence("classification");
        verify(textFormatter).formatAsParticulars("particulars");
        verify(textFormatter).formatAsSentence("secured_details");
        verify(textFormatter).formatAsEntityName("name");
        verify(logger).debug("Descriptive mappings enabled; transforming descriptive fields...");
    }

    @Test
    void testChargeToChargeApiMapsFieldsWithFlagDisabled() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        // given
        DescriptiveChargeApiMapper mapper = new DescriptiveChargeApiMapper(delegate, textFormatter, false, logger);
        when(delegate.chargeToChargeApi(any(), any())).thenReturn(chargeApi);

        // when
        ChargeApi result = mapper.chargeToChargeApi(charge, "12345678");

        // then
        assertSame(chargeApi, result);
        verifyNoInteractions(textFormatter);
        verify(logger).debug("Descriptive mappings disabled; not transforming descriptive fields...");
    }
}
