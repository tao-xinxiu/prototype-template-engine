package com.orange.util;

import java.sql.Timestamp;
import java.util.Set;
import java.util.UUID;

public class VersionGenerator {
    public static String fromTimestamp(Set<String> noCollision) {
	String timeStamp = fromTimestamp();
	while (noCollision.contains(timeStamp)) {
	    try {
		Thread.sleep(1);
	    } catch (InterruptedException e) {
		e.printStackTrace();
	    }
	    timeStamp = fromTimestamp();
	}
	return timeStamp;
    }
    
    private static String fromTimestamp() {
	return "v" + new Timestamp(System.currentTimeMillis()).toInstant();
    }

    public static String random(Set<String> noCollision) {
	String random = random();
	while (noCollision.contains(random)) {
	    random = random();
	}
	return random;
    }

    private static String random() {
	return UUID.randomUUID().toString().substring(0, 8);
    }
}
