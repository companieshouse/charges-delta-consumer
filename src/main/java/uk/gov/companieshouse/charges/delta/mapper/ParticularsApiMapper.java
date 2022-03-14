package uk.gov.companieshouse.charges.delta.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.companieshouse.api.charges.ParticularsApi;
import uk.gov.companieshouse.api.charges.PersonsEntitledApi;
import uk.gov.companieshouse.api.delta.Person;
import uk.gov.companieshouse.api.delta.ShortParticularFlags;

//@Mapper(componentModel = "spring")
public interface ParticularsApiMapper {

    /*@Mapping(target = "containsFixedCharge", source = "fixedCharge")
    @Mapping(target = "floatingChargeCoversAll", source = "floatingChargeAll")
    @Mapping(target = "containsNegativePledge", source = "negativePledge")
    @Mapping(target = "chargorActingAsBareTrustee", source = "bareTrustee")
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "description", ignore = true)*/
    ParticularsApi personToPersonsEntitledApi(ShortParticularFlags shortParticularFlags);
}
