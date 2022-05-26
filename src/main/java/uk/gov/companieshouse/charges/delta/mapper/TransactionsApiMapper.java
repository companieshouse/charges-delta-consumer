package uk.gov.companieshouse.charges.delta.mapper;

import static org.apache.commons.lang3.StringUtils.trim;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import uk.gov.companieshouse.api.charges.TransactionsApi;
import uk.gov.companieshouse.api.delta.AdditionalNotice;

@Mapper(componentModel = "spring")
public interface TransactionsApiMapper {

    String DEFAULT_FILING_TYPE = "";

    @Mapping(target = "links.filing", source = "transId")
    @Mapping(target = "filingType", ignore = true)
    @Mapping(target = "transactionId", ignore = true)
    @Mapping(target = "insolvencyCaseNumber", source = "case")
    @Mapping(target = "deliveredOn", ignore = true)
    TransactionsApi additionalNoticeToTransactionsApi(AdditionalNotice additionalNotice);

    /**
     * Format dates to yyyyMMdd format.
     */
    @AfterMapping
    default void setDates(@MappingTarget TransactionsApi transactionsApi,
            AdditionalNotice additionalNotice) {
        if (additionalNotice.getDeliveredOn() != null) {
            transactionsApi.setDeliveredOn(LocalDate.parse(additionalNotice.getDeliveredOn(),
                    DateTimeFormatter.ofPattern("yyyyMMdd")));
        }
    }

    /**
     * sets notice type based on matching rules and patterns.
     */
    @AfterMapping
    default void setNoticeType(@MappingTarget TransactionsApi transactionsApi,
            AdditionalNotice additionalNotice) {
        if (additionalNotice.getNoticeType() != null) {
            transactionsApi.setFilingType(getFilingType(additionalNotice));
        }
    }

    /**
     * Get filing type for a given notice type and its matching trans desc pattern if applicable.
     */
    private String getFilingType(AdditionalNotice additionalNotice) {
        String filingType =
                NoticeTypeMapperUtils.map.get(trim(additionalNotice.getNoticeType())) != null
                        ? NoticeTypeMapperUtils.map.get(trim(additionalNotice.getNoticeType()))
                        .getFilingType(additionalNotice.getTransDesc()) : DEFAULT_FILING_TYPE;
        return filingType;
    }
}
