package uk.gov.companieshouse.charges.delta.config;

import org.mapstruct.factory.Mappers;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.companieshouse.charges.delta.mapper.ChargeApiMapper;
import uk.gov.companieshouse.charges.delta.mapper.ClassificationApiMapper;
import uk.gov.companieshouse.charges.delta.mapper.InsolvencyCasesApiMapper;
import uk.gov.companieshouse.charges.delta.mapper.PersonsEntitledApiMapper;
import uk.gov.companieshouse.charges.delta.mapper.TransactionsApiMapper;
import uk.gov.companieshouse.charges.delta.processor.Encoder;

@TestConfiguration
public class TestConfig {

        @Bean
        public ChargeApiMapper chargeApiMapper() {
            return Mappers.getMapper(ChargeApiMapper.class);
        }

        @Bean
        public PersonsEntitledApiMapper personsEntitledApiMapper() {
            return Mappers.getMapper(PersonsEntitledApiMapper.class);
        }

        @Bean
        public ClassificationApiMapper classificationApiMapper() {
            return Mappers.getMapper(ClassificationApiMapper.class);
        }

        @Bean
        public InsolvencyCasesApiMapper insolvencyCasesApiMapper() {
            return Mappers.getMapper(InsolvencyCasesApiMapper.class);
        }

        @Bean
        public TransactionsApiMapper transactionsApiMapper() {
            return Mappers.getMapper(TransactionsApiMapper.class);
        }

        @Bean
        public Encoder encoder() {
        return new Encoder("salt");
    }


}
