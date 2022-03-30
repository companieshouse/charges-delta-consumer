package uk.gov.companieshouse.charges.delta.mapper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

public class MapperUtils {

    public static final String NOTICE_TYPES_DATA_FILE = "noticeTypes.txt";
    public static final String NOTICE_TYPE = "notice_type:";
    public static final String TRANS_DESC = "trans_desc:";
    public static final String NO_PATTERN = "no_pattern";
    public static final String FILING_TYPE = "filing_type:";
    public static final String DELIMITER = ",";
    public static Map<String, NoticeTypeTransDesc> map = new HashMap<>();

    static {
        populateData();
    }

    /**
     * Load all data for notice types.
     * create map with FilingTypes And Patterns For Various NoticeTypes values.
     */
    private static void populateData() {
        List<String> list = getDataFile(NOTICE_TYPES_DATA_FILE);

        for (String line: list) {
            StringTokenizer st = new StringTokenizer(line, DELIMITER);
            String key = null;
            String filingType = null;
            String transDesc = null;
            NoticeTypeTransDesc noticeTypeTransDesc = new NoticeTypeTransDesc(key);
            while (st.hasMoreTokens()) {
                String noticeType = st.nextToken().trim();

                if (noticeType.contains(NOTICE_TYPE)) {
                    key = noticeType.split(NOTICE_TYPE)[1].trim();
                }
                transDesc = st.nextToken().trim();
                if (transDesc.contains(TRANS_DESC)) {
                    transDesc = transDesc.split(TRANS_DESC)[1].trim();
                    filingType = st.nextToken().trim();
                } else {
                    filingType = transDesc;
                    transDesc = NO_PATTERN;
                }
                if (filingType.contains(FILING_TYPE)) {
                    filingType = filingType.split(FILING_TYPE)[1].trim();

                }

                noticeTypeTransDesc.setNoticeType(key);
                noticeTypeTransDesc.addTransDescPattern(transDesc, filingType);
                map.put(key, noticeTypeTransDesc);
            }
        }

        System.out.println(map);
        System.out.println(map.size());
    }

    private static List<String> getDataFile(String fileName) {

        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        InputStream inputStream = Objects.requireNonNull(classLoader
                .getResourceAsStream(fileName));
        List<String> list = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.toList());
        System.out.println("list =" + list);
        System.out.println("list size = " + list.size());
        return list;
    }

    /**
     * main method.
     */
    public static void main(String[] args) {

        populateData();
        String filingType = map.get("MR01").getFilingType("REGISTRATION OF A CHARGE/"
                + "CO EXTEND / CHARGE CODE 012033960816");
        System.out.println("filingType = " + filingType);
        //match();
    }

    private static void match() {
        String input = "ACQUISITION OF A CHARGE/CO CHARLES/EXTEND / CHARGE CODE  012033960816";
        String input2 = "ACQUISITION OF A CHARGE/CO CHARLES/EXTEND / CHARGE CODE tttttyyy9999";
        String input3 = "REGISTRATION OF A CHARGE/CO EXTEND / CHARGE CODE 012033960816";
        String pattern =
                "^(?i:REGISTRATION OF A CHARGE/CO EXTEND / CHARGE.*?(\\S{8}\\d{4}|\\d+).*)$";
        boolean match = input3.matches(pattern);
        System.out.println(match);
        System.out.println(pattern.length());
    }

}
