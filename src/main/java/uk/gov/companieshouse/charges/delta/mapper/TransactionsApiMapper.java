package uk.gov.companieshouse.charges.delta.mapper;

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

    @Mapping(target = "links.filing", source = "transId")
    @Mapping(target = "filingType", source = "noticeType")
    @Mapping(target = "transactionId", source = "transId")
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
}
