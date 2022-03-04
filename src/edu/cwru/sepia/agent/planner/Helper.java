package edu.cwru.sepia.agent.planner;

import java.util.Map;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionFeedback;
import edu.cwru.sepia.action.ActionResult;
import edu.cwru.sepia.agent.planner.actions.StripsAction;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;

// Helper class that contains helper methods for PEAgent
public class Helper {

	// HELPER METHODS FOR MIDDLESTEP METHOD OF PEAGENT

	/**
	 * Check if the last action fail
	 * 
	 * @param lastActionResult the result of the last action
	 * @return true if last action fail, false otherwise
	 */
	public static boolean isLastActionFailure(ActionResult lastActionResult) {
		// Check if there is a result for the last action and whether the last action failed
		return lastActionResult != null && lastActionResult.getFeedback() == ActionFeedback.FAILED;
	}

	/**
	 * Check if a peasant is available for action
	 * 
	 * @param actionMap        the map of SEPIA actions
	 * @param nextAction       the next action to consider
	 * @param lastActionResult the result of the last action
	 * @return true if a peasant is available, false otherwise
	 */
	public static boolean canPeasantDoAction(Map<Integer, Action> actionMap, StripsAction nextAction,
			ActionResult lastActionResult) {
		// Check if the peasant ID is in the actions map, if there is a last action result,
		// and if the last action result is incomplete.
		return !actionMap.containsKey(nextAction.getUnitID()) && !(lastActionResult != null
				&& lastActionResult.getFeedback().ordinal() == ActionFeedback.INCOMPLETE.ordinal());
	}

	/**
	 * Check if the next action is to build more peasants
	 * 
	 * @param actionMap  the map of SEPIA actions
	 * @param nextAction the next action to consider
	 * @return true if the next action is to build more peasants, false otherwise
	 */
	public static boolean isBuildNext(Map<Integer, Action> actionMap, StripsAction nextAction) {
		// Check if the unit apply the action is townhall and whether the map is empty
		return nextAction.getUnitID() == GameState.TOWN_HALL_ID && !actionMap.isEmpty();
	}

	/**
	 * Add first STRIPS action in plan as a SEPIA action to the SEPIA action map
	 * 
	 * @param actionMap the map of SEPIA actions
	 * @param state     the current state that has this action map
	 */
	public static void addNextAction(Map<Integer, Action> actionMap, State.StateView state, PEAgent agent) {
		// Get the first STRIPS action from the plan
		StripsAction action = agent.getPlan().pop();
		// A variable for the SEPIA action
		Action sepiaAction = null;
		// If the STRIPS action is a directed action
		if (!action.isDirectedAction()) {
			// Create a blank SEPIA action
			sepiaAction = action.createSepiaAction(null);
		} else {
			// Get the peasant that may apply this action
			UnitView peasant = state.getUnit(action.getUnitID());
			// If there is no peasant then push add the STRIPS action to the plan
			if (peasant == null) {
				agent.getPlan().push(action);
				return;
			}
			// Get the position of the peasant
			Position peasantPosition = new Position(peasant.getXPosition(), peasant.getYPosition());
			// Get the position where the peasant aim to apply the action
			Position destinationPosition = action.getPositionForDirection();
			// Create the SEPIA action
			sepiaAction = action.createSepiaAction(peasantPosition.getDirection(destinationPosition));
		}
		// Add the SEPIA action to the action map using the unit ID as key for mapping
		actionMap.put(sepiaAction.getUnitId(), sepiaAction);
	}

}
