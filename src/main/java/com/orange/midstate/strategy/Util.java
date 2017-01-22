package com.orange.midstate.strategy;

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.orange.model.state.AppState;
import com.orange.model.state.OverviewApp;

public class Util {
    public static Set<OverviewApp> deepCopy(Set<OverviewApp> apps) {
	return apps.stream().map(OverviewApp::new).collect(Collectors.toSet());
    }

    public static OverviewApp searchByName(Set<OverviewApp> apps, String appName) {
	return searchApp(apps, app -> app.getName().equals(appName));
    }

    public static Set<OverviewApp> searchByState(Set<OverviewApp> apps, AppState appState) {
	return search(apps, app -> app.getState() == appState);
    }

    public static Set<OverviewApp> search(Set<OverviewApp> apps, Predicate<OverviewApp> predicate) {
	return apps.stream().filter(predicate).collect(Collectors.toSet());
    }

    public static OverviewApp searchApp(Set<OverviewApp> apps, Predicate<OverviewApp> predicate) {
	Set<OverviewApp> result = search(apps, predicate);
	switch (result.size()) {
	case 0:
	    return null;
	case 1:
	    return result.iterator().next();
	default:
	    throw new IllegalStateException(String
		    .format("Illegal state: found more than one apps [%s] which satisfy [%s]", result, predicate));
	}
    }

    public static Set<OverviewApp> exludedApps(Set<OverviewApp> apps, OverviewApp inclusion) {
	return apps.stream().filter(app -> app != inclusion).collect(Collectors.toSet());
    }

    public static Set<OverviewApp> exludedApps(Set<OverviewApp> apps, Set<OverviewApp> inclusion) {
	return apps.stream().filter(app -> !inclusion.contains(app)).collect(Collectors.toSet());
    }
}
