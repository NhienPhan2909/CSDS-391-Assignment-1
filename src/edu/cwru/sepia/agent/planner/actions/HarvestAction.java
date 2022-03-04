package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.Peasant;
import edu.cwru.sepia.agent.planner.Position;
import edu.cwru.sepia.agent.planner.resources.Resource;
import edu.cwru.sepia.util.Direction;

// An action that harvest the resource
public class HarvestAction implements StripsAction {
	Peasant peasant;
	Resource resource;

	public HarvestAction(Peasant peasant, Resource resource) {
		this.peasant = peasant;
		this.resource = resource;
	}

	@Override
	public boolean preconditionsMet(GameState state) {
		return resource.stillRemaining() && !peasant.isCarry() && peasant.getPosition().equals(resource.getPosition());
	}

	@Override
	public void applyAction(GameState state) {
		// Apply the harvest action
		state.applyHarvestAction(this, peasant.getID(), resource.getID());
	}

	@Override
	public boolean isDirectedAction() {
		// Assert that this is a directed action
		return true;
	}

	@Override
	public Position getPositionForDirection() {
		// Get the position of the resource to harvest
		return resource.getPosition();
	}

	@Override
	public Action createSepiaAction(Direction direction) {
		// Create the harvest action
		return Action.createPrimitiveGather(peasant.getID(), direction);
	}

	@Override
	public int getUnitID() {
		// Get the ID of the peasant
		return peasant.getID();
	}
}
