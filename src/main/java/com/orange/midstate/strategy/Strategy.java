package com.orange.midstate.strategy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.orange.model.AppProperty;
import com.orange.model.SiteDeploymentConfig;
import com.orange.model.state.OverviewApp;
import com.orange.util.SetUtil;

public abstract class Strategy {
    protected static List<AppProperty> updateOrder = Collections.unmodifiableList(Arrays.asList(AppProperty.Path,
	    AppProperty.Env, AppProperty.State, AppProperty.Routes, AppProperty.NbProcesses));
    protected OverviewApp desiredApp;
    protected SiteDeploymentConfig config;
    protected Set<OverviewApp> currentRelatedApps;

    /**
     * 
     * @param currentApps
     * @param desiredApp
     * @param config
     * @param relatedAppsPredicate
     *            The predicate of which apps in the current state is related to
     *            the desired app.
     */
    public Strategy(Set<OverviewApp> currentApps, OverviewApp desiredApp, SiteDeploymentConfig config) {
	this.desiredApp = desiredApp;
	this.config = config;
	this.currentRelatedApps = SetUtil.search(currentApps, app -> app.getName().equals(desiredApp.getName()));
    }

    public Strategy(Set<OverviewApp> currentApps, OverviewApp desiredApp, SiteDeploymentConfig config,
	    List<AppProperty> updateOrder) {
	this.desiredApp = desiredApp;
	this.config = config;
	this.currentRelatedApps = SetUtil.search(currentApps, app -> app.getName().equals(desiredApp.getName()));
	if (updateOrder.size() != 5 || !updateOrder.containsAll(Arrays.asList(AppProperty.values()))) {
	    throw new IllegalStateException("Invalid update order.");
	}
	Strategy.updateOrder = Collections.unmodifiableList(updateOrder);
    }

    /**
     * get reference of the current app intended to updated to the desired app.
     * 
     * @return
     */
    protected abstract OverviewApp instantiatedDesiredApp(Set<OverviewApp> currentRelatedApps);

    public abstract Set<OverviewApp> onNoInstantiatedDesiredApp();

    public Set<OverviewApp> onPathUpdated() {
	return directlyUpdateProperty(AppProperty.Path);
    }

    public Set<OverviewApp> onEnvUpdated() {
	return directlyUpdateProperty(AppProperty.Env);
    }

    public Set<OverviewApp> onStateUpdated() {
	return directlyUpdateProperty(AppProperty.State);
    }

    public Set<OverviewApp> onRoutesUpdated() {
	return directlyUpdateProperty(AppProperty.Routes);
    }

    public Set<OverviewApp> onNbProcessesUpdated() {
	return directlyUpdateProperty(AppProperty.NbProcesses);
    }

    public Set<OverviewApp> nothingUpdated() {
	Set<OverviewApp> desiredRelatedApps = SetUtil.deepCopy(currentRelatedApps);
	desiredRelatedApps.remove(SetUtil.exludedApps(desiredRelatedApps, instantiatedDesiredApp(desiredRelatedApps)));
	return desiredRelatedApps;
    }

    protected Set<OverviewApp> directlyUpdateProperty(AppProperty property) {
	Set<OverviewApp> desiredRelatedApps = SetUtil.deepCopy(currentRelatedApps);
	try {
	    Method getPropertyMethod = OverviewApp.class.getMethod("get" + property);
	    Method setPropertyMethod = OverviewApp.class.getMethod("set" + property, getPropertyMethod.getReturnType());
	    setPropertyMethod.invoke(instantiatedDesiredApp(desiredRelatedApps), getPropertyMethod.invoke(desiredApp));
	} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
		| InvocationTargetException e) {
	    throw new IllegalStateException(e);
	}
	return desiredRelatedApps;
    }

    public static List<AppProperty> getUpdateOrder() {
	return updateOrder;
    }

    public OverviewApp getInstantiatedDesiredApp() {
	return instantiatedDesiredApp(currentRelatedApps);
    }
}
