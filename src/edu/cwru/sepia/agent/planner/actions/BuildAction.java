package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.util.Direction;

// An action to build peasant
public class BuildAction implements StripsAction {
	// The ID of the town hall
	int townHallID;
	// The ID of the peasant to be built
	int peasantTemplateID;

	// Constructor
	public BuildAction(int townHallID, int peasantTemplateID) {
		this.townHallID = townHallID;
		this.peasantTemplateID = peasantTemplateID;
	}

	@Override
	public boolean preconditionsMet(GameState state) {
		// Check if a certain game state allows for building more peasants
		return state.canBuild();
	}

	@Override
	public void applyAction(GameState state) {
		// Build new peasant
		state.applyBuildAction(this);
	}

	@Override
	public Action createSepiaAction(Direction direction) {
		// Create a build action
		return Action.createPrimitiveProduction(townHallID, peasantTemplateID);
	}

	@Override
	public int getUnitID() {
		// Get the ID of the town hall which builds the peasant
		return townHallID;
	}

}