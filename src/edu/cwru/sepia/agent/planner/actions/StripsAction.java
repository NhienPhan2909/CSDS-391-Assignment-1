package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.Position;
import edu.cwru.sepia.util.Direction;

/**
 * A useful start of an interface representing strips actions. You may add new methods to this interface if needed, but
 * you should implement the ones provided. You may also find it useful to specify a method that returns the effects
 * of a StripsAction.
 */
public interface StripsAction {

    /**
     * Returns true if the provided GameState meets all of the necessary conditions for this action to successfully
     * execute.
     *
     * As an example consider a Move action that moves peasant 1 in the NORTH direction. The partial game state might
     * specify that peasant 1 is at location (3, 3). In this case the game state shows that nothing is at location (3, 2)
     * and (3, 2) is within bounds. So the method returns true.
     *
     * If the peasant were at (3, 0) this method would return false because the peasant cannot move to (3, -1).
     *
     * @param state GameState to check if action is applicable
     * @return true if apply can be called, false otherwise
     */
    public boolean preconditionsMet(GameState state);

    /**
     * Applies the action instance to the given GameState producing a new GameState in the process.
     *
     * As an example consider a Move action that moves peasant 1 in the NORTH direction. The partial game state
     * might specify that peasant 1 is at location (3, 3). The returned GameState should specify
     * peasant 1 at location (3, 2).
     *
     * In the process of updating the peasant state you should also update the GameState's cost and parent pointers.
     *
     * @param state State to apply action to
     * @return State resulting from successful action appliction.
     */
    public default GameState apply(GameState state) {
    	applyAction(state);
    	updateState(state);
    	return state;
    }
    
    /**
     * Update the plane and cost for a game state
     * @param state State to apply action to
     */
    public default void updateState(GameState state) {
    	state.updatePlanAndCost(this);
    }

	/**
	 * Apply a SEPIA action to a game state
	 * @param state State to apply action to
	 */
    public default void applyAction(GameState state) {};
    
    /**
     * A boolean flag to show if this action is a directed action
     * @return true if it is a directed action, false otherwise
     */
    public default boolean isDirectedAction() {
		return false;
	}
    
    /**
     * Get the position to move to in a certain direction
     * @return
     */
    public default Position getPositionForDirection() {
    	return null;
    }
	
	/**
	 * Create a SEPIA action in a direction
	 * @param direction to apply an action
	 * @return an action in this direction
	 */
	public Action createSepiaAction(Direction direction);

	/**
	 * Get the ID of the unit to do the action
	 * @return the ID of the unit to perform the action
	 */
	public int getUnitID();
	
	/**
	 * Get the cost of an action
	 * @return the cost of an action
	 */
	public default double getCost() {
		return 1;
	}
}