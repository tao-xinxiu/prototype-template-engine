package com.orange.paas;

import com.orange.model.state.ArchitectureMicroservice;
import com.orange.model.workflow.Step;

public interface UpdateStepDirectory {
    public abstract Step add(ArchitectureMicroservice microservice);

    public abstract Step remove(ArchitectureMicroservice microservice);

    public abstract Step update(ArchitectureMicroservice currentMicroservice, ArchitectureMicroservice desiredMicroservice);
}
