package uk.gov.companieshouse.charges.delta.mapper;

import org.apache.commons.lang3.math.NumberUtils;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import uk.gov.companieshouse.api.charges.InsolvencyCasesApi;
import uk.gov.companieshouse.api.delta.InsolvencyCase;

@Mapper(componentModel = "spring")
public interface InsolvencyCasesApiMapper {

    @Mapping(target = "caseNumber", ignore = true)
    @Mapping(target = "transactionId", ignore = true)
    @Mapping(target = "links", ignore = true)
    InsolvencyCasesApi insolvencyCaseToInsolvencyCasesApi(InsolvencyCase insolvencyCase);

    /**
     * sets case number and transaction id.
     */
    @AfterMapping
    default void setProperties(@MappingTarget InsolvencyCasesApi insolvencyCasesApi,
                                  InsolvencyCase insolvencyCase) {
        if (NumberUtils.isParsable(insolvencyCase.getCase())) {
            insolvencyCasesApi.setCaseNumber(Integer.parseInt(insolvencyCase.getCase()));
        } else {
            insolvencyCasesApi.setCaseNumber(0);
        }
        if (NumberUtils.isParsable(insolvencyCase.getTransactionId())) {
            insolvencyCasesApi.setTransactionId(Long.parseLong(insolvencyCase.getTransactionId()));
        } else {
            insolvencyCasesApi.setTransactionId(0L);
        }
    }
}
