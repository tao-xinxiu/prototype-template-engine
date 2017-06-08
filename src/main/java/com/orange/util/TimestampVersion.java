package com.orange.util;

import java.sql.Timestamp;

public class TimestampVersion {
    public static String get() {
	return new Timestamp(System.currentTimeMillis()).toInstant().toString();
    }
}
