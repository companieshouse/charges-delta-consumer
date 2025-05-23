package uk.gov.companieshouse.charges.delta.mapper;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.charges.ChargeApi;
import uk.gov.companieshouse.api.charges.ClassificationApi;
import uk.gov.companieshouse.api.charges.ParticularsApi;
import uk.gov.companieshouse.api.charges.PersonsEntitledApi;
import uk.gov.companieshouse.api.charges.SecuredDetailsApi;
import uk.gov.companieshouse.api.delta.Charge;
import uk.gov.companieshouse.logging.Logger;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;

@ExtendWith(MockitoExtension.class)
class DescriptiveChargeApiMapperTest {

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
    void testChargeToChargeApiMapsFields() throws InvocationTargetException,
            NoSuchMethodException, IllegalAccessException {
        // given
        DescriptiveChargeApiMapper mapper = new DescriptiveChargeApiMapper(delegate, textFormatter);
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
    }
}
