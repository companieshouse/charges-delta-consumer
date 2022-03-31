package uk.gov.companieshouse.charges.delta.mapper;

import static uk.gov.companieshouse.charges.delta.mapper.MapperUtils.NO_PATTERN;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

public class NoticeTypeTransDesc {

    public static final String DEFAULT = "1";
    private String noticeType;

    private Map<String, String> filingTypeAndTransDescPattern;

    public NoticeTypeTransDesc(String noticeType) {
        this.noticeType = noticeType;
    }

    public String getNoticeType() {
        return noticeType;
    }

    public void setNoticeType(String noticeType) {
        this.noticeType = noticeType;
    }

    public Map<String, String> getFilingTypeAndTransDescPattern() {
        return filingTypeAndTransDescPattern;
    }

    public void setFilingTypeAndTransDescPattern(Map<String, String>
                                                         filingTypeAndTransDescPattern) {
        this.filingTypeAndTransDescPattern = filingTypeAndTransDescPattern;
    }

    /**
     * adds transDesc pattern and filing type as key-value pair into a list.
     */
    public NoticeTypeTransDesc addTransDescPattern(String transDescPattern, String filingType) {
        if (this.filingTypeAndTransDescPattern == null) {
            this.filingTypeAndTransDescPattern = new HashMap<>();
        }
        this.filingTypeAndTransDescPattern.put(transDescPattern, filingType);
        return this;
    }

    /**
     * gets filing type based if transDesc matches transDesc pattern.
     */
    public String getFilingType(String transDesc) {
        if (StringUtils.isEmpty(transDesc)
                || filingTypeAndTransDescPattern.size() == 1) {
            return filingTypeAndTransDescPattern.get(NO_PATTERN);
        }
        String filingType = null;

        filingType = this.filingTypeAndTransDescPattern.entrySet().stream()
                .filter(x -> transDesc != null && transDesc.matches(x.getKey()))
                .map(x -> x.getValue())
                .collect(toSingleton());

        filingType = StringUtils.isEmpty(filingType) ? DEFAULT : filingType;
        return filingType;
    }

    /**
     * gets a single value from the list.
     */
    public static <T> Collector<T, ?, T> toSingleton() {
        return Collectors.collectingAndThen(
                Collectors.toList(),
                list -> {
                    if (list.size() > 0) {
                        return list.get(0);
                    } else {
                        return null;
                    }
                }
        );
    }

    @Override
    public String toString() {
        return "NoticeTypeTransDesc{"
                + "noticeType='" + noticeType + '\''
                + ", filingTypeAndTransDescPattern=" + filingTypeAndTransDescPattern
                + '}';
    }
}
