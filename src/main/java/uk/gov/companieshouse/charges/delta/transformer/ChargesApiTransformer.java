package uk.gov.companieshouse.charges.delta.transformer;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.delta.Charge;
import uk.gov.companieshouse.api.delta.ChargesDelta;
import uk.gov.companieshouse.api.delta.InsolvencyDelta;

@Component
public class ChargesApiTransformer {
    public String transform(Charge charge) {
        // TODO: Use mapStruct to transform json object to Open API generated object
        return charge.toString();
    }
}
