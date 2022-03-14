package uk.gov.companieshouse.charges.delta.mapper;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import uk.gov.companieshouse.api.charges.ChargeApi;
import uk.gov.companieshouse.api.charges.ClassificationApi;
import uk.gov.companieshouse.api.charges.ParticularsApi;
import uk.gov.companieshouse.api.charges.SecuredDetailsApi;
import uk.gov.companieshouse.api.delta.Charge;
import uk.gov.companieshouse.api.delta.ShortParticularFlags;

@Mapper(componentModel = "spring", uses = {
        PersonsEntitledApiMapper.class,
        InsolvencyCasesApiMapper.class,
        ParticularsApiMapper.class,
        TransactionsApiMapper.class}
)
public interface ChargeApiMapper {

    @Mapping(target = "etag", ignore = true) // doesn't exist on source
    @Mapping(target = "chargeCode", source = "code")
    // TODO check with data
    @Mapping(target = "classification", ignore = true)
    @Mapping(target = "resolvedOn", ignore = true)
    @Mapping(target = "particulars", ignore = true)
    @Mapping(target = "securedDetails", ignore = true)
    @Mapping(target = "scottishAlterations.hasRestrictingProvisions",
            source = "restrictingProvisions")
    @Mapping(target = "scottishAlterations.hasAlterationsToOrder",
            source = "alterationsToOrder")
    @Mapping(target = "scottishAlterations.hasAlterationsToProhibitions",
            source = "alterationsToProhibitions")
    @Mapping(target = "moreThanFourPersonsEntitled",
            source = "moreThan4Persons")
    @Mapping(target = "transactions", source = "additionalNotices")
    @Mapping(target = "links", ignore = true)
    @Mapping(target = "insolvencyCases", source = "insolvencyCases")
    ChargeApi chargeToChargeApi(Charge sourceCharge);

    /**
     * Map Source String property to Target enum.
     */

    @AfterMapping
    default void mapToClassificationApi(@MappingTarget ChargeApi chargeApi,
                                        Charge charge) {
        ClassificationApi classificationApi = chargeApi.getClassification() == null
                ? new ClassificationApi() : chargeApi.getClassification();

        stringToClassificationApiEnum(charge.getType(), classificationApi,
                ClassificationApi.TypeEnum.CHARGE_DESCRIPTION);
        stringToClassificationApiEnum(charge.getNatureOfCharge(), classificationApi,
                ClassificationApi.TypeEnum.NATURE_OF_CHARGE);
        chargeApi.setClassification(classificationApi);
    }

    /**
     * Maps Charge to ParticularsApi model.
     */
    @AfterMapping
    default void mapToParticularsApi(@MappingTarget ChargeApi chargeApi,
                                        Charge charge) {
        ParticularsApi particularsApi = chargeApi.getParticulars() == null
                ? new ParticularsApi() : chargeApi.getParticulars();
        ShortParticularFlags shortParticularFlags = charge.getShortParticularFlags().get(0);
        if (shortParticularFlags != null) {
            mapShortParticularFlagsToParticularsApi(particularsApi, shortParticularFlags);
        }

        stringToParticularsApiEnum(charge.getDescriptionOfPropertyUndertaking(), particularsApi,
                ParticularsApi.TypeEnum.CHARGED_PROPERTY_OR_UNDERTAKING_DESCRIPTION);
        stringToParticularsApiEnum(charge.getDescriptionOfPropertyCharged(), particularsApi,
                ParticularsApi.TypeEnum.CHARGED_PROPERTY_DESCRIPTION);
        stringToParticularsApiEnum(charge.getBriefDescription(), particularsApi,
                ParticularsApi.TypeEnum.BRIEF_DESCRIPTION);
        stringToParticularsApiEnum(charge.getShortParticulars(), particularsApi,
                ParticularsApi.TypeEnum.SHORT_PARTICULARS);
        chargeApi.setParticulars(particularsApi);
    }

    /**
     * Maps ShortParticularFlags To ParticularsApi model.
     */
    @AfterMapping
    default void mapToSecuredDetailsApiApi(@MappingTarget ChargeApi chargeApi,
                                        Charge charge) {
        SecuredDetailsApi securedDetailsApi = chargeApi.getSecuredDetails() == null
                ? new SecuredDetailsApi() : chargeApi.getSecuredDetails();

        stringToSecuredDetailsApiEnum(charge.getType(), securedDetailsApi,
                SecuredDetailsApi.TypeEnum.OBLIGATIONS_SECURED);
        stringToSecuredDetailsApiEnum(charge.getNatureOfCharge(), securedDetailsApi,
                SecuredDetailsApi.TypeEnum.AMOUNT_SECURED);
        chargeApi.setSecuredDetails(securedDetailsApi);
    }

    /**
     * Maps ShortParticularFlags To ParticularsApi model.
     */
    private void mapShortParticularFlagsToParticularsApi(
            ParticularsApi particularsApi,
            ShortParticularFlags shortParticularFlags) {
        particularsApi.setContainsFixedCharge(
                BooleanUtils.toBoolean(shortParticularFlags.getFixedCharge()));
        particularsApi.setChargorActingAsBareTrustee(
                BooleanUtils.toBoolean(shortParticularFlags.getBareTrustee()));
        particularsApi.setContainsFloatingCharge(
                BooleanUtils.toBoolean(shortParticularFlags.getContainsFloatingCharge()));
        particularsApi.setContainsNegativePledge(
                BooleanUtils.toBoolean(shortParticularFlags.getNegativePledge()));
        particularsApi.setFloatingChargeCoversAll(
                BooleanUtils.toBoolean(shortParticularFlags.getFloatingChargeAll()));
    }

    /**
     * Maps property in Charge to enum in ParticularApi model.
     */
    private void stringToParticularsApiEnum(String property, ParticularsApi particularsApi,
                              ParticularsApi.TypeEnum theEnum) {
        if (!StringUtils.isEmpty(property)) {

            particularsApi.setType(theEnum);
            particularsApi.setDescription(property);
        }
    }

    /**
     * Maps property in Charge to enum in ClassificationApi model.
     */
    private void stringToClassificationApiEnum(String property, ClassificationApi classificationApi,
                                               ClassificationApi.TypeEnum theEnum) {
        if (!StringUtils.isEmpty(property)) {

            classificationApi.setType(theEnum);
            classificationApi.setDescription(property);
        }
    }

    /**
     * Maps property in Charge to enum in SecuredDetailsApi model.
     */
    private void stringToSecuredDetailsApiEnum(String property, SecuredDetailsApi securedDetailsApi,
                                               SecuredDetailsApi.TypeEnum theEnum) {
        if (!StringUtils.isEmpty(property)) {

            securedDetailsApi.setType(theEnum);
            securedDetailsApi.setDescription(property);
        }
    }


    /**
     * Generic method that maps property in Charge to enum in a model.
     */
    /*private <T> void stringToEnum(String property, T obj) throws NoSuchMethodException {
        if (!StringUtils.isEmpty(property)) {

           obj.se
        }
    }*/
}
