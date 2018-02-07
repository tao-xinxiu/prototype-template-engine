package com.orange.nextstate.strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.orange.model.StrategyConfig;
import com.orange.model.state.Architecture;
import com.orange.model.state.ArchitectureMicroservice;

public abstract class Strategy {
    protected List<Transition> transitions = new ArrayList<Transition>();
    public abstract boolean valid(Architecture currentState, Architecture finalState);
//    public abstract List<Transition> transitions();
    
    protected StrategyConfig config;
    protected StrategyLibrary library;

    public Strategy(StrategyConfig config) {
	this.config = config;
	this.library = new StrategyLibrary(this);
    }

    public List<Transition> getTransitions() {
        return transitions;
    }

    /**
     * return whether currentState is an instantiated architecture of
     * desiredState.
     * 
     * @param currentState
     * @param desiredState
     * @return
     */
    public boolean isInstantiation(Architecture currentState, Architecture desiredState) {
	if (desiredState == null) {
	    return false;
	}
	if (!currentState.getSites().equals(desiredState.getSites())) {
	    return false;
	}
	for (String site : currentState.listSitesName()) {
	    Set<ArchitectureMicroservice> desiredMicroservices = desiredState.getArchitectureMicroservices(site);
	    Set<ArchitectureMicroservice> microservices = currentState.getArchitectureMicroservices(site);
	    if (microservices.size() != desiredMicroservices.size()) {
		return false;
	    }
	    for (ArchitectureMicroservice desiredMicroservice : desiredMicroservices) {
		if (microservices.stream().noneMatch(ms -> isInstantiation(ms, desiredMicroservice))) {
		    return false;
		}
	    }
	}
	return true;
    }

    /**
     * return whether currentMicroservice is an instantiated microservice of
     * desiredMicroservice.
     * 
     * @param currentState
     * @param desiredState
     * @return
     */
    protected boolean isInstantiation(ArchitectureMicroservice currentMicroservice,
	    ArchitectureMicroservice desiredMicroservice) {
	if (desiredMicroservice == null) {
	    return false;
	}
	if (desiredMicroservice.getGuid() != null
		&& !desiredMicroservice.getGuid().equals(currentMicroservice.getGuid())) {
	    return false;
	}
	if (desiredMicroservice.getVersion() != null
		&& !desiredMicroservice.getVersion().equals(currentMicroservice.getVersion())) {
	    return false;
	}
	if (!currentMicroservice.getName().equals(desiredMicroservice.getName())
		|| !currentMicroservice.getPath().equals(desiredMicroservice.getPath())
		|| !currentMicroservice.getState().equals(desiredMicroservice.getState())
		|| currentMicroservice.getNbProcesses() != desiredMicroservice.getNbProcesses()
		|| !currentMicroservice.getEnv().equals(desiredMicroservice.getEnv())
		|| !currentMicroservice.getRoutes().equals(desiredMicroservice.getRoutes())
		|| !currentMicroservice.getMemory().equals(desiredMicroservice.getMemory())
		|| !currentMicroservice.getDisk().equals(desiredMicroservice.getDisk())) {
	    return false;
	}
	return true;
    }

}
