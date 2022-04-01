package uk.gov.companieshouse.charges.delta.transformer;

import java.lang.reflect.InvocationTargetException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.charges.ChargeApi;
import uk.gov.companieshouse.api.charges.ChargeLink;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.api.charges.InternalData;
import uk.gov.companieshouse.api.charges.TransactionsApi;
import uk.gov.companieshouse.api.charges.TransactionsLinks;
import uk.gov.companieshouse.api.delta.Charge;
import uk.gov.companieshouse.charges.delta.mapper.ChargeApiMapper;
import uk.gov.companieshouse.charges.delta.mapper.MapperUtils;
import uk.gov.companieshouse.charges.delta.processor.Encoder;
import uk.gov.companieshouse.logging.Logger;

@Component
public class ChargesApiTransformer {
    public static final String COMPANY = "/company/";
    public static final String FILING_HISTORY = "/filing-history/";
    public static final String CHARGES = "/charges/";
    public static final String DEFAULT_FILING_TYPE = "1";
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
                getKafkaHeader(headers, KafkaHeaders.OFFSET), internalChargeApi, charge);
        logger.trace(String.format("DSND-498: Charge message transformed to InternalChargeApi "
                + ": %s", internalChargeApi));
        return internalChargeApi;
    }

    private void updateChargeApiWithLinks(Charge charge, ChargeApi chargeApi,
                                          String companyNumber) {
        if (chargeApi.getTransactions() != null) {
            for (TransactionsApi transactionsApi : chargeApi.getTransactions()) {
                if (transactionsApi.getLinks() != null
                        &&
                        transactionsApi.getLinks().getFiling() != null) {
                    transactionsApi.getLinks()
                            .setFiling(COMPANY + companyNumber + FILING_HISTORY
                                    + encoder.encodeWithoutSha1(
                                            transactionsApi.getLinks().getFiling()));
                }
            }
        }
        mapTransIdAndNoticeType(charge, chargeApi, companyNumber);
        ChargeLink chargeLink = new ChargeLink();
        chargeLink.setSelf(COMPANY + companyNumber + CHARGES
                + encoder.encodeWithSha1(charge.getId()));
        chargeApi.setLinks(chargeLink);
    }

    private void updateInternalChargeApi(String receivedTopic, String partition, String offset,
                                         InternalChargeApi internalChargeApi, Charge charge) {
        final String updatedBy = String.format("%s-%s-%s", receivedTopic, partition, offset);
        InternalData internalData = new InternalData();
        internalData.setUpdatedBy(updatedBy);
        internalData.setDeltaAt(stringToOffsetDateTime(charge.getDeltaAt()));
        internalChargeApi.setInternalData(internalData);
    }

    private String getKafkaHeader(MessageHeaders headers, String headerKey) {
        return Objects.requireNonNull(headers.get(headerKey)).toString();
    }

    /**
     * Maps transid, notice_type of Charge to filing within TransactionsLinks and
     * filing type within TransactionApi.
     */
    private void mapTransIdAndNoticeType(Charge charge, ChargeApi chargeApi,
                                          String companyNumber) {
        TransactionsApi transactionsApi = new TransactionsApi();
        TransactionsLinks transactionsLinks = new TransactionsLinks();

        transactionsLinks.setFiling(charge.getTransId() != null
                ? encode(companyNumber, charge.getTransId()) : null);
        transactionsApi.setLinks(transactionsLinks);
        transactionsApi.setFilingType(getFilingType(charge));
        chargeApi.addTransactionsItem(transactionsApi);
    }

    private String getFilingType(Charge charge) {
        String filingType = MapperUtils.map.get(charge.getNoticeType()) != null
                ? MapperUtils.map.get(charge.getNoticeType())
                .getFilingType(charge.getTransDesc()) : DEFAULT_FILING_TYPE;
        return filingType;
    }

    private String encode(String companyNumber, String id) {
        return String.format(COMPANY + "%s" + FILING_HISTORY + "%s",
                companyNumber, encoder.encodeWithoutSha1(id));
    }

    private OffsetDateTime stringToOffsetDateTime(String date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSSSSS")
                .withZone(ZoneId.of("Z"));
        ZonedDateTime datetime = ZonedDateTime.parse(date, formatter);
        return datetime.toOffsetDateTime();
    }
}