package gov.bnl.channelfinder.report;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * split the pv name segment into primary, secondary, and tertiary.
 */
public class PVNameSplitter {
    static final String PRIMARY = "Primary";
    static final String PRIMARY_INST = "Primary_Inst";
    static final String SECONDARY = "Secondary";
    static final String SECONDARY_INST = "Secondary_Inst";
    static final String TERTIARY = "Tertiary";
    static final String TERTIARY_INST = "Tertiary_Inst";

    // Matches
    static String priSecTer = "^(\\w*)(:[\\w]*)?-([\\w]*)(:[\\w]*)?-([\\w]*)(:[\\w]*)?";
    private static final Pattern priSecTerPattern = Pattern.compile(priSecTer);

    static String priSec = "^(\\w*)(:[\\w]*)?-([\\w]*)(:[\\w]*)?";
    private static final Pattern priSecPattern = Pattern.compile(priSec);

    static String pri = "^(\\w*)(:[\\w]*)?";
    private static final Pattern priPattern = Pattern.compile(pri);

    /**
     * TODO: this could seriously use a lot of cleaning up.
     * TODO: a single regex pattern
     *
     * @param str
     */
    public static Map<String, Optional<String>> process(String str) {
        Map<String, Optional<String>> result = new HashMap<>();

        result.put(PRIMARY, Optional.empty());
        result.put(PRIMARY_INST, Optional.empty());
        result.put(SECONDARY, Optional.empty());
        result.put(SECONDARY_INST, Optional.empty());
        result.put(TERTIARY, Optional.empty());
        result.put(TERTIARY_INST, Optional.empty());

        Matcher matcher = priSecTerPattern.matcher(str);
        if (matcher.matches()) {
            result.put(PRIMARY, Optional.ofNullable(matcher.group(1)));
            result.put(PRIMARY_INST, Optional.ofNullable(matcher.group(2) == null ? null : matcher.group(2).substring(1)));
            result.put(SECONDARY, Optional.ofNullable(matcher.group(3)));
            result.put(SECONDARY_INST, Optional.ofNullable(matcher.group(4) == null ? null : matcher.group(4).substring(1)));
            result.put(TERTIARY, Optional.ofNullable(matcher.group(5)));
            result.put(TERTIARY_INST, Optional.ofNullable(matcher.group(6) == null ? null : matcher.group(6).substring(1)));
        } else {
            matcher = priSecPattern.matcher(str);
            if (matcher.matches()) {
                result.put(PRIMARY, Optional.ofNullable(matcher.group(1)));
                result.put(PRIMARY_INST, Optional.ofNullable(matcher.group(2) == null ? null : matcher.group(2).substring(1)));
                result.put(SECONDARY, Optional.ofNullable(matcher.group(3)));
                result.put(SECONDARY_INST, Optional.ofNullable(matcher.group(4) == null ? null : matcher.group(4).substring(1)));
            } else {
                matcher = priPattern.matcher(str);
                if (matcher.matches()) {
                    result.put(PRIMARY, Optional.ofNullable(matcher.group(1)));
                    result.put(PRIMARY_INST, Optional.ofNullable(matcher.group(2) == null ? null : matcher.group(2).substring(1)));
                }
            }
        }
        return result;
    }

}
