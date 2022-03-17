package uk.gov.companieshouse.charges.delta.transformer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.charges.ChargeApi;
import uk.gov.companieshouse.api.charges.ChargeLink;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.api.delta.Charge;
import uk.gov.companieshouse.charges.delta.mapper.ChargeApiMapper;
import uk.gov.companieshouse.charges.delta.processor.Encoder;

@Component
public class ChargesApiTransformer {
    private final ChargeApiMapper chargeApiMapper;
    private Encoder encoder;

    @Autowired
    public ChargesApiTransformer(ChargeApiMapper chargeApiMapper, Encoder encoder) {
        this.chargeApiMapper = chargeApiMapper;
        this.encoder = encoder;
    }

    /**
     * Transforms an Charge object from an ChargesDelta object
     * into an InternalChargeApi using mapstruct.
     * @param charge source object
     * @return source object mapped to InternalChargeApi
     */
    public InternalChargeApi transform(Charge charge) {

        ChargeApi chargeApi = chargeApiMapper.chargeToChargeApi(charge);
        String companyNumber = charge.getCompanyNumber();
        updateChargeApi(charge, chargeApi, companyNumber);
        InternalChargeApi internalChargeApi = new InternalChargeApi();
        internalChargeApi.setExternalData(chargeApi);
        return internalChargeApi;
    }

    private void updateChargeApi(Charge charge, ChargeApi chargeApi, String companyNumber) {
        chargeApi.getTransactions().stream()
                        .forEach(transactionsApi -> transactionsApi.getLinks()
                                .setFiling("/company/" + companyNumber + "/filing-history/"
                                        + encoder.encodeWithoutSha1(
                                        transactionsApi.getLinks().getFiling())));
        ChargeLink chargeLink = new ChargeLink();
        chargeLink.setSelf("/company/" + companyNumber + "/charges/"
                + encoder.encodeWithSha1(charge.getId()));
        chargeApi.setLinks(chargeLink);
    }

}