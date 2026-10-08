package com.surendra.priorauth.security;

import java.util.regex.Pattern;

/** Masks PHI for display and logs. Keeps just enough to let a person confirm the record. */
public final class PhiMasker {

    /** Member IDs look like three letters followed by 6+ digits, e.g. ABC123456789. */
    private static final Pattern MEMBER_ID = Pattern.compile("\\b([A-Z]{3})\\d{2,}(\\d{4})\\b");
    /** ISO dates such as a date of birth: 1985-04-12. */
    private static final Pattern ISO_DATE = Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}\\b");

    private PhiMasker() {
    }

    /** ABC123456789 -> ABC*****6789 */
    public static String maskMemberId(String memberId) {
        if (memberId == null || memberId.length() <= 4) {
            return "****";
        }
        String last4 = memberId.substring(memberId.length() - 4);
        return "*".repeat(memberId.length() - 4) + last4;
    }

    /** 1985-04-12 -> ****-**-** (year kept out too: DOB is a direct identifier) */
    public static String maskDate(String date) {
        return date == null ? null : "****-**-**";
    }

    /** Jane Doe -> J*** D** */
    public static String maskName(String name) {
        if (name == null || name.isBlank()) {
            return name;
        }
        StringBuilder out = new StringBuilder();
        for (String part : name.trim().split("\\s+")) {
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(part.charAt(0)).append("*".repeat(Math.max(part.length() - 1, 1)));
        }
        return out.toString();
    }

    /** Scrubs member IDs and dates from free text, used for every log line. */
    public static String scrub(String text) {
        if (text == null) {
            return null;
        }
        String masked = MEMBER_ID.matcher(text).replaceAll("$1*****$2");
        return ISO_DATE.matcher(masked).replaceAll("****-**-**");
    }
}
