package uk.gov.companieshouse.charges.delta.mapper;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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
        TransactionsApiMapper.class}
)
public interface ChargeApiMapper {

    // doesn't exist on source. This needs to be populated by data api
    @Mapping(target = "etag", ignore = true)
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
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "deliveredOn", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "satisfiedOn", ignore = true)
    @Mapping(target = "acquiredOn", ignore = true)
    @Mapping(target = "assetsCeasedReleased", ignore = true)
    @Mapping(target = "coveringInstrumentDate", ignore = true)
    ChargeApi chargeToChargeApi(Charge sourceCharge) throws NoSuchMethodException,
            InvocationTargetException, IllegalAccessException;

    /**
     * Map Source String property to Target enum.
     */

    @AfterMapping
    default void mapToClassificationApi(@MappingTarget ChargeApi chargeApi,
                                        Charge charge) throws NoSuchMethodException,
            InvocationTargetException, IllegalAccessException {
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
                                     Charge charge) throws InvocationTargetException,
            NoSuchMethodException, IllegalAccessException {
        ParticularsApi particularsApi = chargeApi.getParticulars() == null
                ? new ParticularsApi() : chargeApi.getParticulars();
        ShortParticularFlags shortParticularFlags = charge.getShortParticularFlags() == null
                ? null : charge.getShortParticularFlags().get(0);
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
                                           Charge charge) throws InvocationTargetException,
            NoSuchMethodException, IllegalAccessException {
        SecuredDetailsApi securedDetailsApi = chargeApi.getSecuredDetails() == null
                ? new SecuredDetailsApi() : chargeApi.getSecuredDetails();

        stringToSecuredDetailsApiEnum(charge.getType(), securedDetailsApi,
                SecuredDetailsApi.TypeEnum.OBLIGATIONS_SECURED);
        stringToSecuredDetailsApiEnum(charge.getNatureOfCharge(), securedDetailsApi,
                SecuredDetailsApi.TypeEnum.AMOUNT_SECURED);
        chargeApi.setSecuredDetails(securedDetailsApi);
    }

    /**
     * Maps status from Charges Delta To Status is ChargeApi model.
     * 0  : outstanding
     * 1  : fully-satisfied
     * 2  : part-satisfied
     * 7  : satisfied
     */
    @AfterMapping
    default void mapStatuses(@MappingTarget ChargeApi chargeApi,
                             Charge charge) {
        int status = Integer.parseInt(charge.getStatus());
        switch (status) {
            case 0:
                chargeApi.setStatus(ChargeApi.StatusEnum.OUTSTANDING);
                break;
            case 1:
                chargeApi.setStatus(ChargeApi.StatusEnum.FULLY_SATISFIED);
                break;
            case 2:
                chargeApi.setStatus(ChargeApi.StatusEnum.PART_SATISFIED);
                break;
            case 7:
                chargeApi.setStatus(ChargeApi.StatusEnum.SATISFIED);
                break;
            default:
                //do nothing
                break;
        }
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
                                            ParticularsApi.TypeEnum theEnum)
            throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        stringToEnum(property, particularsApi, ParticularsApi.class, theEnum);
    }

    /**
     * Maps property in Charge to enum in ClassificationApi model.
     */
    private void stringToClassificationApiEnum(String property,
                                               ClassificationApi classificationApi,
                                               ClassificationApi.TypeEnum theEnum)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        stringToEnum(property, classificationApi, ClassificationApi.class, theEnum);
    }

    /**
     * Maps property in Charge to enum in SecuredDetailsApi model.
     */
    private void stringToSecuredDetailsApiEnum(String property,
                                               SecuredDetailsApi securedDetailsApi,
                                               SecuredDetailsApi.TypeEnum theEnum)
            throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        stringToEnum(property, securedDetailsApi, SecuredDetailsApi.class, theEnum);
    }

    /**
     * Format dates to yyyyMMdd format.
     */
    @AfterMapping
    default void setDates(@MappingTarget ChargeApi chargeApi, Charge charge) {
        chargeApi.setDeliveredOn(parseDate(charge.getDeliveredOn(), "yyyyMMdd"));

        chargeApi.setCreatedOn(parseDate(charge.getCreatedOn(), "yyyyMMdd"));

        chargeApi.setSatisfiedOn(parseDate(charge.getSatisfiedOn(), "yyyyMMdd"));

        chargeApi.setCreatedOn(parseDate(charge.getCreatedOn(), "yyyyMMdd"));

        chargeApi.setAcquiredOn(parseDate(charge.getAcquiredOn(), "yyyyMMdd"));

        chargeApi.setCoveringInstrumentDate(parseDate(charge.getCoveringInstrumentDate(),
                "yyyyMMdd"));

    }

    /**
     * Format string dates of format yyyyMMdd to LocalDate.
     */
    private LocalDate parseDate(String sourceDate, String format) {
        if (sourceDate != null) {
            return LocalDate.parse(sourceDate,
                    DateTimeFormatter.ofPattern(format));
        }
        return null;
    }

    /**
     * Maps assets_ceased_released from Charges Delta To assets_ceased_released enum in
     * ChargeApi model.
     * 3  : property-ceased-to-belong
     * 4  : part-property-release-and-ceased-to-belong
     * 5  : part-property-released
     * 6  : part-property-ceased-to-belong
     * 8  : whole-property-released
     * 9  : multiple-filings
     * 10 : whole-property-released-and-ceased-to-belong
     */
    @AfterMapping
    default void mapAssetsCeasedReleasedEnum(@MappingTarget ChargeApi chargeApi,
                                             Charge charge) {
        int assetsCeasedReleased = Integer.parseInt(charge.getAssetsCeasedReleased());
        switch (assetsCeasedReleased) {
            case 3:
                chargeApi.setAssetsCeasedReleased(
                        ChargeApi.AssetsCeasedReleasedEnum.PROPERTY_CEASED_TO_BELONG);
                break;
            case 4:
                chargeApi.setAssetsCeasedReleased(
                        ChargeApi.AssetsCeasedReleasedEnum
                                .PART_PROPERTY_RELEASE_AND_CEASED_TO_BELONG);
                break;
            case 5:
                chargeApi.setAssetsCeasedReleased(
                        ChargeApi.AssetsCeasedReleasedEnum.PART_PROPERTY_RELEASED);
                break;
            case 6:
                chargeApi.setAssetsCeasedReleased(
                        ChargeApi.AssetsCeasedReleasedEnum.PART_PROPERTY_CEASED_TO_BELONG);
                break;
            case 8:
                chargeApi.setAssetsCeasedReleased(
                        ChargeApi.AssetsCeasedReleasedEnum.WHOLE_PROPERTY_RELEASED);
                break;
            case 9:
                chargeApi.setAssetsCeasedReleased(
                        ChargeApi.AssetsCeasedReleasedEnum.MULTIPLE_FILINGS);
                break;
            case 10:
                chargeApi.setAssetsCeasedReleased(
                        ChargeApi.AssetsCeasedReleasedEnum
                                .WHOLE_PROPERTY_RELEASED_AND_CEASED_TO_BELONG);
                break;
            default:
                //do nothing
                break;
        }
    }


    /**
     /**
     * Generic method that Maps property from an object to description in the target object and
     * sets the enum in the target object model.
     */
    private <T> void stringToEnum(String property, Object obj, Class<T> clazz,
                                  Enum<?> theEnum) throws NoSuchMethodException,
            InvocationTargetException, IllegalAccessException {
        if (!StringUtils.isEmpty(property)) {
            obj.getClass().getMethod("setType", theEnum.getClass()).invoke(obj, theEnum);
            obj.getClass().getMethod("setDescription", String.class).invoke(obj, property);
        }
    }
}
