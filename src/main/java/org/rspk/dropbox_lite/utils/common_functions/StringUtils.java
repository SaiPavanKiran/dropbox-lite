package org.rspk.dropbox_lite.utils.common_functions;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

public class StringUtils {

    private static final SecureRandom sc = new SecureRandom();
    private static final String CHARACTERS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";


    public static String subStringAfterLast(String value,String str) {
        if(value == null) return "";
        int index = value.lastIndexOf(str);

        if (index == -1 || index == value.length() - 1) {
            return "";
        }

        return value.substring(index + 1).toLowerCase();
    }


    public static String randomStr(int length) {
        StringBuilder stringBuilder = new StringBuilder();
        int charsLength = CHARACTERS.length();

        for(int i = 0;i<length;i++){
            stringBuilder.append(CHARACTERS.charAt(sc.nextInt(charsLength)));
        }
        return stringBuilder.toString();
    }

    public static UUID toUUIDorNull(String string) {
        if(string == null) return null;
        return UUID.fromString(string);
    }

    public static List<UUID> toUUIDList(List<String> strings) {
        return strings.stream().map(UUID::fromString).toList();
    }

}
