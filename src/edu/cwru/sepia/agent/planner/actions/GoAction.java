package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.Peasant;
import edu.cwru.sepia.agent.planner.Position;
import edu.cwru.sepia.util.Direction;

// An action that move a peasant to another position
public class GoAction implements StripsAction{
	// The peasant who do this action
	Peasant peasant;
	// The target position to do this action
	Position position;
	
	// Constructor
	public GoAction(Peasant peasant, Position destination) {
		this.peasant = peasant;
		this.position = position;
	}

	@Override
	public boolean preconditionsMet(GameState state) {
		// Check if the peasant is not already at the target position
		return !peasant.getPosition().equals(position);
	}

	@Override
	public void applyAction(GameState state) {
		// Move the peasant to the target position
		state.applyMoveAction(this, peasant.getID(), position);
	}

	@Override
	public Action createSepiaAction(Direction direction) {
		// Create a move action towards the target position
		return Action.createCompoundMove(peasant.getID(), position.x, position.y);
	}

	@Override
	public int getUnitID() {
		// Get the ID of the peasant
		return peasant.getID();	
	}
	
	@Override
	public double getCost() {
		// The cost of a move is the Chebyshev distance between the current and target positions
		return peasant.getPosition().chebyshevDistance(position) - 1;
	}


}
