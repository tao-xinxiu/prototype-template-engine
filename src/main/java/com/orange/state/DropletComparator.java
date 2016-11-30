package com.orange.state;

import com.orange.model.OverviewDroplet;

public class DropletComparator {
	private OverviewDroplet currentDroplet;
	private OverviewDroplet desiredDroplet;

	public DropletComparator(OverviewDroplet currentDroplet, OverviewDroplet desiredDroplet) {
		if (!currentDroplet.getGuid().equals(desiredDroplet.getGuid())) {
			throw new IllegalStateException(String.format(
					"Illegal DropletComparator with different guid in currentDroplet %s and desiredDroplet %s",
					currentDroplet, desiredDroplet));
		}
		this.currentDroplet = currentDroplet;
		this.desiredDroplet = desiredDroplet;
	}

	public OverviewDroplet getCurrentDroplet() {
		return currentDroplet;
	}

	public OverviewDroplet getDesiredDroplet() {
		return desiredDroplet;
	}

}
