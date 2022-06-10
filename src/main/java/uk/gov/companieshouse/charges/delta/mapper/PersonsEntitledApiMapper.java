package uk.gov.companieshouse.charges.delta.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.companieshouse.api.charges.PersonsEntitledApi;
import uk.gov.companieshouse.api.delta.Person;

@Mapper(componentModel = "spring")
public interface PersonsEntitledApiMapper {

    @Mapping(target = "name", expression = "java(uk.gov.companieshouse.charges.delta.mapper."
            + "TextFormatter.formatAsEntityName(person.getPerson()))")
    PersonsEntitledApi personToPersonsEntitledApi(Person person);
}
