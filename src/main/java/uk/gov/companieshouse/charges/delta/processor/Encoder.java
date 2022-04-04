package uk.gov.companieshouse.charges.delta.processor;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Encoder {

    private static final String SHA_1 = "SHA-1";

    public static String CHARGE_ID_SALT;

    public static String TRANS_ID_SALT;

    /**
     * Initialize by passing in salts for chargeId and transId.
     *
     * @param chargeIdSalt input String value
     * @param transIdSalt  input String value
     */
    @Autowired
    public Encoder(@Value("${api.charge-id-salt}") String chargeIdSalt,
            @Value("${api.trans-id-salt}") String transIdSalt) {

        CHARGE_ID_SALT = chargeIdSalt;
        TRANS_ID_SALT = transIdSalt;
    }

    public String base64Encode(final byte[] plainValue) {
        return Base64.encodeBase64String(plainValue);
    }

    /**
     * Apply SHA-1 digest algorithm on the value after concatenating with salt.
     *
     * @param plainValue input String value
     * @return returns sha1 encoded hex
     */
    public String getSha1Digest(final String plainValue) {
        return DigestUtils.sha1Hex(plainValue + CHARGE_ID_SALT);
    }

    /**
     * encodes with base64 encoding and ‘+' replaced by ‘-’ and ‘/’ replaced by '_’.
     *
     * @param plain input String value
     * @return returns base64 encoded String with salt
     */
    public String encodeWithSha1(String plain) {
        var sha1Value = getSha1Digest(plain);
        var base10 = new BigInteger(sha1Value, 16); // base 10 int
        return new String(Base64.encodeInteger(base10))
                .replace("+", "-")
                .replace("/", "_"); // base64 string
    }

    /**
     * encodes without sha1 and with base64 encoding.
     *
     * @param plain input String value
     * @return returns base64 encoded String with salt
     */
    public String encodeWithoutSha1(String plain) {
        if (plain == null || plain.isEmpty()) {
            return plain;
        }
        return base64Encode((plain + TRANS_ID_SALT).getBytes(StandardCharsets.UTF_8));
    }


}
