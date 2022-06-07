package uk.gov.companieshouse.charges.delta.mapper;

import static java.lang.Boolean.TRUE;
import static org.apache.commons.lang3.BooleanUtils.toBooleanObject;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.trim;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import uk.gov.companieshouse.api.charges.ChargeApi;
import uk.gov.companieshouse.api.charges.ClassificationApi;
import uk.gov.companieshouse.api.charges.ParticularsApi;
import uk.gov.companieshouse.api.charges.SecuredDetailsApi;
import uk.gov.companieshouse.api.delta.Charge;
import uk.gov.companieshouse.api.delta.ShortParticularFlags;


@Mapper(componentModel = "spring",
        uses = {
            PersonsEntitledApiMapper.class,
            InsolvencyCasesApiMapper.class,
            TransactionsApiMapper.class})
public interface ChargeApiMapper {

    String YYYY_MM_DD = "yyyyMMdd";
    String SET_TYPE = "setType";
    String SET_DESCRIPTION = "setDescription";
    //TODO Better way of ignoring fields to be looked at
    // etag doesn't exist on source. This needs to be populated by data api

    @Mapping(target = "etag", ignore = true)
    @Mapping(target = "chargeCode", source = "code")
    @Mapping(target = "classification", ignore = true)
    @Mapping(target = "resolvedOn", ignore = true)
    @Mapping(target = "particulars", ignore = true)
    @Mapping(target = "securedDetails", ignore = true)
    @Mapping(target = "scottishAlterations.hasRestrictingProvisions",
            expression = "java(org.apache.commons.lang3.BooleanUtils"
                    + ".toBooleanObject(charge.getRestrictingProvisions()))")
    @Mapping(target = "scottishAlterations.hasAlterationsToOrder",
            expression = "java(org.apache.commons.lang3.BooleanUtils"
                    + ".toBooleanObject(charge.getAlterationsToOrder()))")
    @Mapping(target = "scottishAlterations.hasAlterationsToProhibitions",
            expression = "java(org.apache.commons.lang3.BooleanUtils"
                    + ".toBooleanObject(charge.getAlterationsToProhibitions()))")
    @Mapping(target = "moreThanFourPersonsEntitled",
            expression = "java(org.apache.commons.lang3.BooleanUtils.toBooleanObject"
                    + "(sourceCharge.getMoreThan4Persons()) == Boolean.TRUE ? Boolean.TRUE : null)")
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
    @Mapping(target = "id", ignore = true)
    ChargeApi chargeToChargeApi(Charge sourceCharge,
                                @Context String companyNumber) throws NoSuchMethodException,
            InvocationTargetException, IllegalAccessException;

    /**
     * Map Source Charge to ClassificationApi.
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
     * Maps source Charge to ParticularsApi model.
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
            mapShortParticularFlagsToParticularsApi(particularsApi, shortParticularFlags, charge);
        }

        stringToParticularsApiEnum(charge.getDescriptionOfPropertyUndertaking(), particularsApi,
                ParticularsApi.TypeEnum.CHARGED_PROPERTY_OR_UNDERTAKING_DESCRIPTION);
        stringToParticularsApiEnum(charge.getDescriptionOfPropertyCharged(), particularsApi,
                ParticularsApi.TypeEnum.CHARGED_PROPERTY_DESCRIPTION);
        stringToParticularsApiEnum(charge.getBriefDescription(), particularsApi,
                ParticularsApi.TypeEnum.BRIEF_DESCRIPTION);
        stringToParticularsApiEnum(charge.getShortParticulars(), particularsApi,
                ParticularsApi.TypeEnum.SHORT_PARTICULARS);

        chargeApi.setParticulars(isAnyFieldValueNotNull(particularsApi) ? particularsApi : null);
    }

    /**
     * Helper method to determine if any first level field value in an object is not null.
     */
    private boolean isAnyFieldValueNotNull(Object targetObject) {
        if (targetObject == null) {
            return false;
        }

        return ObjectUtils.anyNotNull(Arrays.stream(targetObject.getClass().getDeclaredFields())
                .filter(field -> !field.isSynthetic())
                .map(field -> {
                    try {
                        field.setAccessible(true);
                        return field.get(targetObject);
                    } catch (IllegalAccessException ex) {
                        throw new RuntimeException(ex);
                    }
                }).toArray(Object[]::new));
    }

    /**
     * Maps source Charge To SecuredDetailsApi model.
     */
    @AfterMapping
    default void mapToSecuredDetailsApiApi(@MappingTarget ChargeApi chargeApi,
                                           Charge charge) throws InvocationTargetException,
            NoSuchMethodException, IllegalAccessException {
        SecuredDetailsApi securedDetailsApi = chargeApi.getSecuredDetails() == null
                ? new SecuredDetailsApi() : chargeApi.getSecuredDetails();
        if (!StringUtils.isEmpty(charge.getObligationsSecured())
                || !StringUtils.isEmpty(charge.getAmountSecured())) {
            stringToSecuredDetailsApiEnum(charge.getObligationsSecured(), securedDetailsApi,
                    SecuredDetailsApi.TypeEnum.OBLIGATIONS_SECURED);
            stringToSecuredDetailsApiEnum(charge.getAmountSecured(), securedDetailsApi,
                    SecuredDetailsApi.TypeEnum.AMOUNT_SECURED);
            chargeApi.setSecuredDetails(securedDetailsApi);
        }

    }

    /**
     * Maps status from Charges Delta To Status in ChargeApi model.
     * 0  : outstanding
     * 1  : fully-satisfied
     * 2  : part-satisfied
     * 7  : satisfied
     */
    @AfterMapping
    default void mapStatuses(@MappingTarget ChargeApi chargeApi,
                             Charge charge) {

        chargeApi.setStatus(populateStatusEnumMap().get(
                charge.getStatus() != null ? Integer.parseInt(charge.getStatus()) : null));
    }

    /**
     * Maps ShortParticularFlags To ParticularsApi model.
     */
    private void mapShortParticularFlagsToParticularsApi(
            ParticularsApi particularsApi,
            ShortParticularFlags shortParticularFlags, Charge charge) {
        particularsApi.setContainsFixedCharge(
                toBooleanObject(shortParticularFlags.getFixedCharge()) == TRUE ? TRUE : null);
        particularsApi.setChargorActingAsBareTrustee(
                toBooleanObject(shortParticularFlags.getBareTrustee()) == TRUE ? TRUE : null);
        particularsApi.setContainsFloatingCharge(
                orAsBoleanObjects(shortParticularFlags.getContainsFloatingCharge(),
                charge.getFloatingCharge()) == TRUE ? TRUE : null);
        particularsApi.setContainsNegativePledge(
                toBooleanObject(shortParticularFlags.getNegativePledge()) == TRUE ? TRUE : null);
        particularsApi.setFloatingChargeCoversAll(
                toBooleanObject(shortParticularFlags.getFloatingChargeAll()) == TRUE ? TRUE : null);
    }

    /**
     * Converting strings to Boolean could result in null value oring then gives NPE.
     *
     * @param str1 first 'boolean string'
     * @param str2 second 'boolean string'
     * @return the 2 values ord together ignoring any nulls
     */
    private Boolean orAsBoleanObjects(String str1, String str2) {
        Boolean bool1 = toBooleanObject(str1);
        Boolean bool2 = toBooleanObject(str2);
        if (bool1 == null) {
            return bool2;
        }
        if (bool2 == null) {
            return bool1;
        }
        return bool1 || bool2;
    }

    /**
     * Maps property in Charge to enum in ParticularApi model.
     */
    private void stringToParticularsApiEnum(String property, ParticularsApi particularsApi,
                                            ParticularsApi.TypeEnum theEnum)
            throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        stringToEnum(property, particularsApi, theEnum);
    }

    /**
     * Maps property in Charge to enum in ClassificationApi model.
     */
    private void stringToClassificationApiEnum(String property,
                                               ClassificationApi classificationApi,
                                               ClassificationApi.TypeEnum theEnum)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        stringToEnum(property, classificationApi, theEnum);
    }

    /**
     * Maps property in Charge to enum in SecuredDetailsApi model.
     */
    private void stringToSecuredDetailsApiEnum(String property,
                                               SecuredDetailsApi securedDetailsApi,
                                               SecuredDetailsApi.TypeEnum theEnum)
            throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        if (!StringUtils.isEmpty(property)) {
            stringToEnum(property, securedDetailsApi, theEnum);
        }
    }

    /**
     * Format dates to yyyyMMdd format.
     */

    @AfterMapping
    default void setDates(@MappingTarget ChargeApi chargeApi, Charge charge) {
        chargeApi.setDeliveredOn(parseDate(charge.getDeliveredOn(), YYYY_MM_DD));

        chargeApi.setCreatedOn(parseDate(charge.getCreatedOn(), YYYY_MM_DD));

        chargeApi.setSatisfiedOn(parseDate(charge.getSatisfiedOn(), YYYY_MM_DD));

        chargeApi.setAcquiredOn(parseDate(charge.getAcquiredOn(), YYYY_MM_DD));

        chargeApi.setCoveringInstrumentDate(parseDate(charge.getCoveringInstrumentDate(),
                YYYY_MM_DD));

    }

    /**
     * Format string dates of format yyyyMMdd to LocalDate.
     */
    private LocalDate parseDate(String sourceDate, String format) {
        if (!isEmpty(trim(sourceDate))) {
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
        chargeApi.setAssetsCeasedReleased(populateAssetsCeasedReleasedEnumMap()
                .get(charge.getAssetsCeasedReleased() != null
                        ? Integer.parseInt(charge.getAssetsCeasedReleased()) : null));
    }


    /**
     * create map with enum values.
     */
    private Map<Integer, ChargeApi.AssetsCeasedReleasedEnum> populateAssetsCeasedReleasedEnumMap() {
        Map<Integer, ChargeApi.AssetsCeasedReleasedEnum> map = new HashMap<>();
        map.put(3, ChargeApi.AssetsCeasedReleasedEnum.PROPERTY_CEASED_TO_BELONG);
        map.put(4, ChargeApi.AssetsCeasedReleasedEnum.PART_PROPERTY_RELEASE_AND_CEASED_TO_BELONG);
        map.put(5, ChargeApi.AssetsCeasedReleasedEnum.PART_PROPERTY_RELEASED);
        map.put(6, ChargeApi.AssetsCeasedReleasedEnum.PART_PROPERTY_CEASED_TO_BELONG);
        map.put(8, ChargeApi.AssetsCeasedReleasedEnum.WHOLE_PROPERTY_RELEASED);
        map.put(9, ChargeApi.AssetsCeasedReleasedEnum.MULTIPLE_FILINGS);
        map.put(10, ChargeApi.AssetsCeasedReleasedEnum
                .WHOLE_PROPERTY_RELEASED_AND_CEASED_TO_BELONG);
        return map;
    }

    /**
     * create map with enum values.
     */
    private Map<Integer, ChargeApi.StatusEnum> populateStatusEnumMap() {
        Map<Integer, ChargeApi.StatusEnum> map = new HashMap<>();
        map.put(0, ChargeApi.StatusEnum.OUTSTANDING);
        map.put(1, ChargeApi.StatusEnum.FULLY_SATISFIED);
        map.put(2, ChargeApi.StatusEnum.PART_SATISFIED);
        map.put(7, ChargeApi.StatusEnum.SATISFIED);

        return map;
    }

    /**
     /**
     * Generic method that Maps property from an object to description in the target object and
     * sets the enum in the target object model.
     */
    private <T> void stringToEnum(String property, Object obj,
                                  Enum<?> theEnum) throws NoSuchMethodException,
            InvocationTargetException, IllegalAccessException {
        if (!StringUtils.isEmpty(property)) {
            obj.getClass().getMethod(SET_TYPE, theEnum.getClass()).invoke(obj, theEnum);
            obj.getClass().getMethod(SET_DESCRIPTION, String.class).invoke(obj, property);
        }
    }
}
