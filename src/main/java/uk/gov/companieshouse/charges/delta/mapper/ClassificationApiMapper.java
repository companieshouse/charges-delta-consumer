package uk.gov.companieshouse.charges.delta.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.companieshouse.api.charges.ChargeApi;
import uk.gov.companieshouse.api.charges.ClassificationApi;
import uk.gov.companieshouse.api.delta.Charge;

@Mapper(componentModel = "spring")
public interface ClassificationApiMapper {

    @Mapping(target = "description", source = "natureOfCharge")
    ClassificationApi chargeToClassificationApi(Charge sourceCharge);
}
