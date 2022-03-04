package uk.gov.companieshouse.charges.delta.transformer;

import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.api.delta.Charge;

import static org.assertj.core.api.Assertions.assertThat;

public class ChargesApiTransformerTest {

    private final ChargesApiTransformer transformer = new ChargesApiTransformer();

    @Test
    public void transformSuccessfully() {
        //TODO Transform ChargesDelta to InternalChargeApi model
        final Charge input = new Charge();
        assertThat(transformer.transform(input)).isEqualTo(new InternalChargeApi());
    }

}
