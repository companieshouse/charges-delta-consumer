package uk.gov.companieshouse.charges.delta.transformer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.charges.ChargeApi;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.api.delta.Charge;
import uk.gov.companieshouse.charges.delta.mapper.ChargeApiMapper;

@Component
public class ChargesApiTransformer {
    private final ChargeApiMapper chargeApiMapper;

    @Autowired
    public ChargesApiTransformer(ChargeApiMapper chargeApiMapper) {
        this.chargeApiMapper = chargeApiMapper;
    }

    /**
     * Transforms an Charge object from an ChargesDelta object
     * into an InternalChargeApi using mapstruct.
     *
     * @param charge source object
     * @return source object mapped to InternalChargeApi
     */
    public InternalChargeApi transform(Charge charge) {
        InternalChargeApi internalChargeApi = new InternalChargeApi();
        ChargeApi chargeApi = chargeApiMapper.chargeToChargeApi(charge);
        internalChargeApi.setExternalData(chargeApi);
        return internalChargeApi;
    }
}