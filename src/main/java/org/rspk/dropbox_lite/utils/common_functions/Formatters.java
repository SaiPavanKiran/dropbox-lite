package org.rspk.dropbox_lite.utils.common_functions;

import java.time.format.DateTimeFormatter;

public class Formatters {

    public static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter DATE_TIME_FORMATTER_WITH_MILLIS =
            DateTimeFormatter.ofPattern("yyyy_MM_dd__HH_mm_ss_SSS");

}

