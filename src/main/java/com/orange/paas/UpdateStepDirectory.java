package com.orange.paas;

import com.orange.model.state.OverviewApp;
import com.orange.model.workflow.Step;
import com.orange.update.AppComparator;

public interface UpdateStepDirectory {
    public abstract Step addApp(OverviewApp app);

    public abstract Step removeApp(OverviewApp app);

    public abstract Step updateApp(AppComparator appComparator);
}
