package uk.gov.companieshouse.charges.delta.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.companieshouse.api.charges.PersonsEntitledApi;
import uk.gov.companieshouse.api.delta.Person;

@Mapper(componentModel = "spring")
public interface PersonsEntitledApiMapper {

    @Mapping(target = "name", source = "person")
    PersonsEntitledApi personToPersonsEntitledApi(Person person);
}
