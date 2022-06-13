package uk.gov.companieshouse.charges.delta.mapper;

import org.apache.commons.lang3.math.NumberUtils;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import uk.gov.companieshouse.api.charges.InsolvencyCasesApi;
import uk.gov.companieshouse.api.charges.InsolvencyCasesLinks;
import uk.gov.companieshouse.api.delta.InsolvencyCase;

@Mapper(componentModel = "spring")
public interface InsolvencyCasesApiMapper {

    @Mapping(target = "caseNumber", ignore = true)
    @Mapping(target = "links", ignore = true)

    InsolvencyCasesApi insolvencyCaseToInsolvencyCasesApi(
            InsolvencyCase insolvencyCase, @Context String companyNumber);

    /**
     * sets properties of InsolvencyCasesApi object.
     */
    @AfterMapping
    default void setProperties(@MappingTarget InsolvencyCasesApi insolvencyCasesApi,
                                  InsolvencyCase insolvencyCase, @Context String companyNumber) {
        setCaseNumber(insolvencyCasesApi, insolvencyCase);
        setInsolvencyCaseLink(insolvencyCasesApi, companyNumber);
    }

    /**
     * sets case number.
     */
    private InsolvencyCasesApi setCaseNumber(InsolvencyCasesApi insolvencyCasesApi,
                                             InsolvencyCase insolvencyCase) {
        if (NumberUtils.isParsable(insolvencyCase.getCase())) {
            insolvencyCasesApi.setCaseNumber(insolvencyCase.getCase());
        }
        return insolvencyCasesApi;
    }


    /**
     * sets insolvency case link.
     */
    private InsolvencyCasesApi setInsolvencyCaseLink(InsolvencyCasesApi insolvencyCasesApi,
                                                     String companyNumber) {
        if (insolvencyCasesApi.getCaseNumber() != null) {
            InsolvencyCasesLinks insolvencyCasesLinks = new InsolvencyCasesLinks();
            insolvencyCasesLinks.setCase(
                    String.format("/company/%s/insolvency#case-%s",
                            companyNumber, insolvencyCasesApi.getCaseNumber()));
            insolvencyCasesApi.setLinks(insolvencyCasesLinks);
        }
        return insolvencyCasesApi;
    }
}
