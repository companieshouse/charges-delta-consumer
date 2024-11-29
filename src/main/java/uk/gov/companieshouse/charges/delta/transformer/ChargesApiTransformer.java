package uk.gov.companieshouse.charges.delta.transformer;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.trim;
import static uk.gov.companieshouse.charges.delta.ChargesDeltaConsumerApplication.NAMESPACE;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
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
import uk.gov.companieshouse.charges.delta.exception.RetryableErrorException;
import uk.gov.companieshouse.charges.delta.logging.DataMapHolder;
import uk.gov.companieshouse.charges.delta.mapper.ChargeApiMapper;
import uk.gov.companieshouse.charges.delta.mapper.NoticeTypeMapperUtils;
import uk.gov.companieshouse.charges.delta.processor.EncoderUtil;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Component
public class ChargesApiTransformer {

    public static final String COMPANY = "/company/";
    public static final String FILING_HISTORY = "/filing-history/";
    public static final String CHARGES = "/charges/";
    public static final String DEFAULT_FILING_TYPE = "";

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);
    private final ChargeApiMapper chargeApiMapper;
    private EncoderUtil encoderUtil;

    /**
     * The constructor.
     */
    @Autowired
    public ChargesApiTransformer(ChargeApiMapper descriptiveChargeApiMapper,
            EncoderUtil encoderUtil) {
        this.chargeApiMapper = descriptiveChargeApiMapper;
        this.encoderUtil = encoderUtil;
    }

    /**
     * Transforms a Charge object within ChargesDelta object into an InternalChargeApi using
     * mapstruct.
     *
     * @param charge source object
     * @return source object mapped to InternalChargeApi
     */
    public InternalChargeApi transform(Charge charge, MessageHeaders headers) {
        LOGGER.trace(String.format("Charge message to be transformed "
                + ": %s", charge), DataMapHolder.getLogMap());
        try {
            ChargeApi chargeApi = chargeApiMapper.chargeToChargeApi(charge,
                    charge.getCompanyNumber());
            updateChargeApiWithLinks(charge, chargeApi, charge.getCompanyNumber());
            InternalChargeApi internalChargeApi = new InternalChargeApi();
            internalChargeApi.setExternalData(chargeApi);
            updateInternalChargeApi(
                    getKafkaHeader(headers, KafkaHeaders.RECEIVED_TOPIC),
                    getKafkaHeader(headers, KafkaHeaders.RECEIVED_PARTITION),
                    getKafkaHeader(headers, KafkaHeaders.OFFSET), internalChargeApi, charge);
            LOGGER.trace(String.format("Charge message transformed to InternalChargeApi "
                    + ": %s", internalChargeApi), DataMapHolder.getLogMap());
            return internalChargeApi;
        } catch (Exception ex) {
            throw new RetryableErrorException("Unable to map Charge delta to Charge API object",
                    ex);
        }
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
                                    + encoderUtil.encodeWithoutSha1(
                                    transactionsApi.getLinks().getFiling()));
                }
            }
        }
        mapTransIdAndNoticeType(charge, chargeApi,companyNumber);
        ChargeLink chargeLink = new ChargeLink();
        chargeLink.setSelf(COMPANY + companyNumber + CHARGES
                + encoderUtil.encodeWithSha1(charge.getId()));
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
     * Maps transid, notice_type of Charge to filing within TransactionsLinks and filing type within
     * TransactionApi.
     */
    private void mapTransIdAndNoticeType(Charge charge, ChargeApi chargeApi,
                                         String companyNumber) {
        TransactionsApi transactionsApi = new TransactionsApi();

        if (charge.getTransId() != null) {
            TransactionsLinks transactionsLinks = new TransactionsLinks();
            transactionsLinks.setFiling(encode(companyNumber, trim(charge.getTransId())));
            transactionsApi.setLinks(transactionsLinks);
        }

        transactionsApi.setFilingType(getFilingType(charge));
        if (!isEmpty(trim(charge.getDeliveredOn()))) {
            transactionsApi.setDeliveredOn(LocalDate.parse(charge.getDeliveredOn(),
                    DateTimeFormatter.ofPattern("yyyyMMdd")));
        }

        if (chargeApi.getTransactions() != null) {
            sortListBasedOnDeliveredOnDate(chargeApi);
            chargeApi.getTransactions().add(0, transactionsApi);
        } else {
            chargeApi.addTransactionsItem(transactionsApi);
        }
    }

    private void sortListBasedOnDeliveredOnDate(ChargeApi chargeApi) {
        chargeApi.getTransactions()
                .sort(Comparator.comparing(TransactionsApi::getDeliveredOn,
                        Comparator.nullsLast(Comparator.naturalOrder())));
    }

    private String getFilingType(Charge charge) {
        return NoticeTypeMapperUtils.map.get(trim(charge.getNoticeType())) != null
                ? NoticeTypeMapperUtils.map.get(trim(charge.getNoticeType()))
                .getFilingType(charge.getTransDesc()) : DEFAULT_FILING_TYPE;
    }

    private String encode(String companyNumber, String id) {
        return String.format(COMPANY + "%s" + FILING_HISTORY + "%s",
                companyNumber, encoderUtil.encodeWithoutSha1(id));
    }

    private OffsetDateTime stringToOffsetDateTime(String date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSSSSS")
                .withZone(ZoneId.of("Z"));
        ZonedDateTime datetime = ZonedDateTime.parse(date, formatter);
        return datetime.toOffsetDateTime();
    }
}