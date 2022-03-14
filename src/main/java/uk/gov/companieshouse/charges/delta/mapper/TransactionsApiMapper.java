package uk.gov.companieshouse.charges.delta.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.companieshouse.api.charges.PersonsEntitledApi;
import uk.gov.companieshouse.api.charges.TransactionsApi;
import uk.gov.companieshouse.api.delta.AdditionalNotice;
import uk.gov.companieshouse.api.delta.Person;

@Mapper(componentModel = "spring")
public interface TransactionsApiMapper {

    @Mapping(target = "links.filing", source = "transId")
    @Mapping(target = "filingType", source = "noticeType")
    @Mapping(target = "transactionId", source = "transId")
    @Mapping(target = "insolvencyCaseNumber", source = "case")
   TransactionsApi additionalNoticeToTransactionsApi(AdditionalNotice additionalNotice);
}
