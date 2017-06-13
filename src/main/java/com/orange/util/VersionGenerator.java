package com.orange.util;

import java.sql.Timestamp;

public class VersionGenerator {
    public static String fromTimestamp() {
	return "v" + new Timestamp(System.currentTimeMillis()).toInstant();
    }

    /**
     * This function is to obtain the "va.b.c" from path.
     * 
     * @param pkgPath
     *            It should conform the format ""xxxxx_va.b.c.yyy"" and "va.b.c"
     *            not contains "_". ex. "app_v1.2.0.zip"
     * @return
     */
    public static String fromPackage(String pkgPath) {
	return pkgPath.substring(pkgPath.lastIndexOf("_") + 1, pkgPath.lastIndexOf("."));
    }

    /**
     * Verify whether pkgPath conforms ""xxxxx_va.b.c.yyy""
     * 
     * @param pkgPath
     * @return
     */
    public static boolean validPackage(String pkgPath) {
	int startIndex = pkgPath.lastIndexOf("_");
	int endIndex = pkgPath.lastIndexOf(".");
	return startIndex != -1 && startIndex < endIndex;
    }
}
