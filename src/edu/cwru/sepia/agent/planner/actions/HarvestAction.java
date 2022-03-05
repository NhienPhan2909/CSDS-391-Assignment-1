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
	
	/**
	 * A helper method to help savePlan() in PlannerAgent
	 * Return the string with the ID of peasant which move, the type and amount of resource harvested
	 */
	@Override
	public String toString() {
		// A constant to keep track the resource amount harvested (peasant always harvest with amount of 100 each time)
		int amountHarvest = 100;
		// A variable to keep track the resource type harvested
		String resourceType = (resource.isGold()) ? "Gold" : "Wood";
		return "HarvestAction(" + peasant.getID() +", " + resourceType + ", " + amountHarvest + ")";
	}
}