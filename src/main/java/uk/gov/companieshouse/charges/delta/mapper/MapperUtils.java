package uk.gov.companieshouse.charges.delta.mapper;

import java.security.SecureRandom;
import java.util.Calendar;

import org.apache.commons.codec.digest.DigestUtils;

public class MapperUtils {

    /**
     * Utility method to generate etag.
     */
    public static String generateEtag() {
        Long timeInMs = Calendar.getInstance().getTimeInMillis();
        Long timeInSec = timeInMs / 1000;
        SecureRandom random = new SecureRandom();
        byte[] values = new byte[8];
        random.nextBytes(values);
        return DigestUtils.sha1Hex(timeInSec.toString() + timeInMs.toString() + random.toString());
    }
}
