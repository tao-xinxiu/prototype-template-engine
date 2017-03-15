package com.orange.update;

import java.util.Set;

import com.orange.model.state.OverviewApp;
import com.orange.model.state.Route;
import com.orange.model.workflow.Step;

public interface UpdateStepDirectory {
    public abstract Step addApp(OverviewApp app);

    public abstract Step removeApp(OverviewApp app);

    public abstract Step updateAppName(OverviewApp desiredApp);

    public abstract Step updateAppEnv(OverviewApp desiredApp);

    public abstract Step addAppRoutes(String appId, Set<Route> addedRoutes);

    public abstract Step removeAppRoutes(String appId, Set<Route> removedRoutes);

    public abstract Step updateAppState(OverviewApp currentApp, OverviewApp desiredApp);

    public abstract Step scaleApp(String appId, int instances);
}
