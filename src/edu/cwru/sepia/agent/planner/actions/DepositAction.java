package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.Peasant;
import edu.cwru.sepia.agent.planner.Position;
import edu.cwru.sepia.util.Direction;

// An action that deposit the resource to the town hall 
public class DepositAction implements StripsAction {
	// The peasant who do this action
	Peasant peasant;
	// The current position of the peasant
	Position peasantPosition;
	// The position of the town hall
	Position townHallPosition;
	// A boolean flag to check if the peasant is carrying any resources
	boolean isCarry;
	
	//Constructor
	public DepositAction(Peasant peasant) {
		this.peasant = peasant;
		this.peasantPosition = peasant.getPosition();
		this.isCarry = peasant.isCarry();
		this.townHallPosition = GameState.TOWN_HALL_POSITION;
	}

	@Override
	public boolean preconditionsMet(GameState state) {
		// Check if the peasant is carrying any resources and if the peasant reaches the townhall
		return isCarry && peasant.getPosition().equals(townHallPosition);
	}

	@Override
	public void applyAction(GameState state) {
		// Deposit resources
		state.applyDepositAction(this, peasant.getID());
	}

	@Override
	public boolean isDirectedAction() {
		// Assert that this is a directed action
		return true;
	}
	
	@Override
	public Position getPositionForDirection() {
		// Get the town hall position
		return townHallPosition;
	}
	
	@Override
	public Action createSepiaAction(Direction direction) {
		// Create a deposit action
		return Action.createPrimitiveDeposit(peasant.getID(), direction);
	}
	
	@Override
	public int getUnitID() {
		// Get the ID of the peasant
		return peasant.getID();	
	}

	
}
