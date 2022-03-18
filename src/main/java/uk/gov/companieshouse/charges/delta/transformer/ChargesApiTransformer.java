package uk.gov.companieshouse.charges.delta.transformer;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.charges.ChargeApi;
import uk.gov.companieshouse.api.charges.ChargeLink;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.api.charges.InternalData;
import uk.gov.companieshouse.api.delta.Charge;
import uk.gov.companieshouse.charges.delta.mapper.ChargeApiMapper;
import uk.gov.companieshouse.charges.delta.processor.Encoder;
import uk.gov.companieshouse.logging.Logger;


@Component
public class ChargesApiTransformer {
    private final ChargeApiMapper chargeApiMapper;
    private Encoder encoder;
    private final Logger logger;

    /**
     * The constructor.
     */
    @Autowired
    public ChargesApiTransformer(ChargeApiMapper chargeApiMapper,
                                 Encoder encoder,
                                 Logger logger) {
        this.chargeApiMapper = chargeApiMapper;
        this.encoder = encoder;
        this.logger = logger;
    }

    /**
     * Transforms a Charge object within ChargesDelta object
     * into an InternalChargeApi using mapstruct.
     * @param charge source object
     * @return source object mapped to InternalChargeApi
     */
    public InternalChargeApi transform(Charge charge, MessageHeaders headers)
            throws NoSuchMethodException,
            InvocationTargetException, IllegalAccessException {
        logger.trace(String.format("DSND-498: Charge message to be transformed "
                + ": %s", charge));
        ChargeApi chargeApi = chargeApiMapper.chargeToChargeApi(charge);
        updateChargeApiWithLinks(charge, chargeApi, charge.getCompanyNumber());
        InternalChargeApi internalChargeApi = new InternalChargeApi();
        internalChargeApi.setExternalData(chargeApi);
        updateInternalChargeApi(
                getKafkaHeader(headers, KafkaHeaders.RECEIVED_TOPIC),
                getKafkaHeader(headers, KafkaHeaders.RECEIVED_PARTITION_ID),
                getKafkaHeader(headers, KafkaHeaders.OFFSET), internalChargeApi);
        logger.trace(String.format("DSND-498: Charge message transformed to InternalChargeApi "
                + ": %s", internalChargeApi));
        return internalChargeApi;
    }

    private void updateChargeApiWithLinks(Charge charge, ChargeApi chargeApi,
                                          String companyNumber) {
        chargeApi.getTransactions().stream()
                .forEach(transactionsApi -> transactionsApi.getLinks()
                        .setFiling("/company/" + companyNumber + "/filing-history/"
                                + encoder.encodeWithoutSha1(
                                transactionsApi.getLinks() != null
                                        ? transactionsApi.getLinks().getFiling() : null)));
        ChargeLink chargeLink = new ChargeLink();
        chargeLink.setSelf("/company/" + companyNumber + "/charges/"
                + encoder.encodeWithSha1(charge.getId()));
        chargeApi.setLinks(chargeLink);
    }

    private void updateInternalChargeApi(String receivedTopic, String partition, String offset,
                                         InternalChargeApi internalChargeApi) {
        final String updatedBy = String.format("%s-%s-%s", receivedTopic, partition, offset);
        InternalData internalData = new InternalData();
        internalData.setUpdatedBy(updatedBy);
        internalChargeApi.setInternalData(internalData);
    }

    private String getKafkaHeader(MessageHeaders headers, String headerKey) {
        return Objects.requireNonNull(headers.get(headerKey)).toString();
    }
}