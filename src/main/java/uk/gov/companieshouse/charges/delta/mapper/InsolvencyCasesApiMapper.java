package uk.gov.companieshouse.charges.delta.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.companieshouse.api.charges.InsolvencyCasesApi;
import uk.gov.companieshouse.api.delta.InsolvencyCase;

@Mapper(componentModel = "spring")
public interface InsolvencyCasesApiMapper {

    @Mapping(target = "caseNumber", source = "case")
    @Mapping(target = "links", ignore = true)
    InsolvencyCasesApi insolvencyCaseToInsolvencyCasesApi(InsolvencyCase insolvencyCase);
}
