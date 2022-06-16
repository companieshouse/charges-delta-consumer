package uk.gov.companieshouse.charges.delta.mapper;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.charges.ChargeApi;
import uk.gov.companieshouse.api.delta.Charge;
import uk.gov.companieshouse.logging.Logger;

/**
 * Recases descriptive fields mapped by a {@link ChargeApiMapper charges mapper} as follows if
 * property mapper.descriptive-mappings-enabled is true:
 * <br>
 * <ul>
 * <li>
 *     Field classification.description will be recased
 *     {@link TextFormatter#formatAsSentence(String) as a sentence.}
 * </li>
 * <li>
 *     Field particulars.description will be recased
 *     {@link TextFormatter#formatAsParticulars(String) as a particulars string}.
 * </li>
 * <li>
 *     Field secured_details.description will be recased
 *     {@link TextFormatter#formatAsSentence(String) as a sentence}.
 * </li>
 * <li>
 *     Field(s) persons_entitled.n.name will be recased
 *     {@link TextFormatter#formatAsEntityName(String) as an entity name}.
 * </li>
 * </ul>
 */
@Component
public class DescriptiveChargeApiMapper implements ChargeApiMapper {

    private final ChargeApiMapper chargeApiMapper;

    private final TextFormatter textFormatter;

    private final boolean descriptiveMappingsEnabled;

    private final Logger logger;

    /**
     * Create a new {@link DescriptiveChargeApiMapper instance}.
     *
     * @param chargeApiMapper The {@link ChargeApiMapper instance} that field mapping will be
     *                        delegated to.
     * @param textFormatter A {@link TextFormatter text formatter instance} used to
     *                      transform fields.
     * @param descriptiveMappingsEnabled True if fields should be recased, otherwise false.
     * @param logger A {@link Logger logger instance}.
     */
    public DescriptiveChargeApiMapper(ChargeApiMapper chargeApiMapper,
                                      TextFormatter textFormatter,
                                      @Value("${mapper.descriptive-mappings-enabled}")
                                              boolean descriptiveMappingsEnabled,
                                      Logger logger) {
        this.chargeApiMapper = chargeApiMapper;
        this.textFormatter = textFormatter;
        this.descriptiveMappingsEnabled = descriptiveMappingsEnabled;
        this.logger = logger;
    }

    @Override
    public ChargeApi chargeToChargeApi(Charge sourceCharge, String companyNumber)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ChargeApi result = chargeApiMapper.chargeToChargeApi(sourceCharge, companyNumber);
        if (descriptiveMappingsEnabled) {
            logger.debug("Descriptive mappings enabled; transforming descriptive fields...");
            Optional.ofNullable(result.getClassification())
                    .ifPresent(classification -> classification.setDescription(
                            textFormatter.formatAsSentence(classification.getDescription())));
            Optional.ofNullable(result.getParticulars())
                    .ifPresent(particulars -> particulars.setDescription(
                            textFormatter.formatAsParticulars(particulars.getDescription())));
            Optional.ofNullable(result.getSecuredDetails())
                    .ifPresent(details -> details.setDescription(
                            textFormatter.formatAsSentence(details.getDescription())));
            Optional.ofNullable(result.getPersonsEntitled()).stream().flatMap(Collection::stream)
                    .forEach(person -> person.setName(
                            textFormatter.formatAsEntityName(person.getName())));
        } else {
            logger.debug("Descriptive mappings disabled; not transforming descriptive fields...");
        }
        return result;
    }
}
