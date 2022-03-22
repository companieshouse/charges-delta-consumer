package uk.gov.companieshouse.charges.delta.processor;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
     * @param transIdSalt input String value
     *
     */
    @Autowired
    public Encoder(@Value("${api.charge-id-salt}") String chargeIdSalt,
                   @Value("${api.trans-id-salt}") String transIdSalt) {

        CHARGE_ID_SALT = chargeIdSalt;
        TRANS_ID_SALT = transIdSalt;
    }

    public String base64Encode(final byte[] bytes) {
        return Base64.getUrlEncoder().encodeToString(bytes);
    }

    /**
     * Apply SHA-1 digest algorithm on the value after concatenating with salt.
     *
     * @param plainValue input String value
     * @return returns      byte array
     */
    public byte[] getSha1Digest(final String plainValue) {
        //concatenate chargeId with salt. Salt is externalized
        byte[] byteValue = (plainValue + CHARGE_ID_SALT).getBytes(StandardCharsets.UTF_8);
        //get sha1 hash value using commons-codec.
        // Please note that this is a 40 char hex representation of a 20 byte value.
        String sha1Value = DigestUtils.sha1Hex(byteValue);
        return sha1Value.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * encodes with base64 encoding and ‘+' replaced by ‘-’ and ‘/’ replaced by '_’.
     *
     * @param plain input String value
     * @return returns base64 encoded String with salt
     */
    public String encodeWithSha1(String plain) {
        return base64Encode(getSha1Digest(plain))
                .replace("+", "-")
                .replace("/", "_");
    }

    /**
     * encodes without sha1 and with base64 encoding.
     *
     * @param plain input String value
     * @return returns base64 encoded String with salt
     */
    public String encodeWithoutSha1(String plain) {

        return base64Encode(plain != null
                ? (plain + TRANS_ID_SALT).getBytes(StandardCharsets.UTF_8) : null);
    }


}
