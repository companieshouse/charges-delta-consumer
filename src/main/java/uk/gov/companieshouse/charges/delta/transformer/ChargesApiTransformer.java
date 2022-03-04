package uk.gov.companieshouse.charges.delta.transformer;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.api.delta.Charge;

@Component
public class ChargesApiTransformer {
    public InternalChargeApi transform(Charge charge) {
        // TODO: Use mapStruct to transform json object to Open API generated object
        return new InternalChargeApi();
    }
}
