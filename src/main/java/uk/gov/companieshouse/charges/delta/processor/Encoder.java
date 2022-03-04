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

    private static String API_SALT;

    @Autowired
    public Encoder(@Value("${api.salt}") String salt) {
        API_SALT = salt;
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

        byte[] byteValue = (plainValue + API_SALT).getBytes(StandardCharsets.UTF_8);
        String sha1Value = DigestUtils.sha1Hex(byteValue);
        return sha1Value.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * encodes with base64 encoding and ‘+' replaced by ‘-’ and ‘/’ replaced by '_’.
     *
     * @param plain input String value
     * @return returns base64 encoded String
     */
    public String encode(String plain) {
        return base64Encode(getSha1Digest(plain))
                .replace("+", "-")
                .replace("/", "_");
    }


}
