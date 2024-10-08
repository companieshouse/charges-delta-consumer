package uk.gov.companieshouse.charges.delta.config;

import org.mapstruct.factory.Mappers;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import uk.gov.companieshouse.charges.delta.mapper.ChargeApiMapper;
import uk.gov.companieshouse.charges.delta.mapper.DescriptiveChargeApiMapper;
import uk.gov.companieshouse.charges.delta.mapper.InsolvencyCasesApiMapper;
import uk.gov.companieshouse.charges.delta.mapper.PersonsEntitledApiMapper;
import uk.gov.companieshouse.charges.delta.mapper.TextFormatter;
import uk.gov.companieshouse.charges.delta.mapper.TransactionsApiMapper;
import uk.gov.companieshouse.charges.delta.processor.EncoderUtil;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@TestConfiguration
public class TestConfig {

    @Bean
    public TextFormatter textFormatter() {
        return new TextFormatter();
    }

    @Bean
    public ChargeApiMapper descriptiveChargeApiMapper(ChargeApiMapper chargeApiMapper, TextFormatter textFormatter) {
        return new DescriptiveChargeApiMapper(chargeApiMapper, textFormatter);
    }

    @Bean
    public ChargeApiMapper chargeApiMapper() {
        return Mappers.getMapper(ChargeApiMapper.class);
    }

    @Bean
    public PersonsEntitledApiMapper personsEntitledApiMapper() {
        return Mappers.getMapper(PersonsEntitledApiMapper.class);
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
    public EncoderUtil encoderUtil() {
        return new EncoderUtil("chargeId_salt", "transId_salt");
    }

    @Bean
    public Logger logger() {
        return LoggerFactory.getLogger("TestConfig");
    }

}
