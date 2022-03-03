package uk.gov.companieshouse.charges.delta.transformer;

import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.api.delta.Charge;
import uk.gov.companieshouse.api.delta.ChargesDelta;
import uk.gov.companieshouse.api.delta.InsolvencyDelta;

import static org.assertj.core.api.Assertions.assertThat;

public class ChargesApiTransformerTest {

    private final ChargesApiTransformer transformer = new ChargesApiTransformer();

    @Test
    public void transformSuccessfully() {
        //TODO To change the InsolvencyDelta class to ChargesDelta when this will be available
        final Charge input = new Charge();
        assertThat(transformer.transform(input)).isEqualTo(input.toString());
    }

}
