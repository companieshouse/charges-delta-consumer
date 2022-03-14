package uk.gov.companieshouse.charges.delta.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.companieshouse.api.charges.ScottishAlterationsApi;
import uk.gov.companieshouse.api.delta.Charge;

//@Mapper(componentModel = "spring")
public interface ScottishAlterationsApiMapper {

    /*@Mapping(target = "hasRestrictingProvisions", source = "restrictingProvisions")
    @Mapping(target = "hasAlterationsToOrder", source = "alterationsToOrder")
    @Mapping(target = "hasAlterationsToProhibitions", source = "alterationsToProhibitions")*/
    ScottishAlterationsApi chargeToScottishAlterationsApi(Charge charge);
}
