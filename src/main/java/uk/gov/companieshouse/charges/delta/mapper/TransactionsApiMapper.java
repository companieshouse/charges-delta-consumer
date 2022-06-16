package uk.gov.companieshouse.charges.delta.mapper;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.trim;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.apache.commons.lang3.StringUtils;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import uk.gov.companieshouse.api.charges.TransactionsApi;
import uk.gov.companieshouse.api.charges.TransactionsLinks;
import uk.gov.companieshouse.api.delta.AdditionalNotice;

@Mapper(componentModel = "spring")
public interface TransactionsApiMapper {

    String DEFAULT_FILING_TYPE = "";

    @Mapping(target = "links.filing", source = "transId")
    @Mapping(target = "filingType", ignore = true)
    @Mapping(target = "transactionId", ignore = true)
    @Mapping(target = "insolvencyCaseNumber", source = "case")
    @Mapping(target = "deliveredOn", ignore = true)
    TransactionsApi additionalNoticeToTransactionsApi(AdditionalNotice additionalNotice,
                                                      @Context String companyNumber);

    /**
     * Format dates to yyyyMMdd format.
     */
    @AfterMapping
    default void setDates(@MappingTarget TransactionsApi transactionsApi,
            AdditionalNotice additionalNotice) {
        if (!isEmpty(trim(additionalNotice.getDeliveredOn()))) {
            transactionsApi.setDeliveredOn(LocalDate.parse(additionalNotice.getDeliveredOn(),
                    DateTimeFormatter.ofPattern("yyyyMMdd")));
        }
    }

    /**
     * sets notice type based on matching rules and patterns.
     */
    @AfterMapping
    default void setNoticeTypeAndLinksInsolvencyCase(@MappingTarget TransactionsApi transactionsApi,
            AdditionalNotice additionalNotice, @Context String companyNumber) {
        if (additionalNotice.getNoticeType() != null) {
            transactionsApi.setFilingType(getFilingType(additionalNotice));
        }

        setLinksInsolvencyCase(transactionsApi, additionalNotice, companyNumber);

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

    /**
     * sets insolvency case link.
     */
    private void setLinksInsolvencyCase(TransactionsApi transactionsApi,
                                        AdditionalNotice additionalNotice,
                                        String companyNumber) {

        if (transactionsApi.getLinks() == null) {
            transactionsApi.setLinks(new TransactionsLinks());
        }

        if (!StringUtils.isEmpty(additionalNotice.getCase())) {
            transactionsApi.getLinks()
                    .setInsolvencyCase(String.format("/company/%s/insolvency#case-%s",
                        companyNumber, additionalNotice.getCase()));
        }

    }


}
