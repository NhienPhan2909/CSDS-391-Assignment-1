package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionFeedback;
import edu.cwru.sepia.action.ActionResult;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.agent.planner.actions.*;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Template;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * This is an outline of the PEAgent. Implement the provided methods. You may
 * add your own methods and members.
 */
public class PEAgent extends Agent {

	// The plan being executed
	private Stack<StripsAction> plan = null;

	// maps the real unit Ids to the plan's unit ids
	// when you're planning you won't know the true unit IDs that sepia assigns. So
	// you'll use placeholders (1, 2, 3).
	// this maps those placeholders to the actual unit IDs.
	private Map<Integer, Integer> peasantIdMap;
	private int townhallId;
	private int peasantTemplateId;

	public PEAgent(int playernum, Stack<StripsAction> plan) {
		super(playernum);
		peasantIdMap = new HashMap<Integer, Integer>();
		this.plan = plan;

	}

	@Override
	public Map<Integer, Action> initialStep(State.StateView stateView, History.HistoryView historyView) {
		// gets the townhall ID and the peasant ID
		for (int unitId : stateView.getUnitIds(playernum)) {
			Unit.UnitView unit = stateView.getUnit(unitId);
			String unitType = unit.getTemplateView().getName().toLowerCase();
			if (unitType.equals("townhall")) {
				townhallId = unitId;
			} else if (unitType.equals("peasant")) {
				peasantIdMap.put(unitId, unitId);
			}
		}

		// Gets the peasant template ID. This is used when building a new peasant with
		// the townhall
		for (Template.TemplateView templateView : stateView.getTemplates(playernum)) {
			if (templateView.getName().toLowerCase().equals("peasant")) {
				peasantTemplateId = templateView.getID();
				break;
			}
		}

		return middleStep(stateView, historyView);
	}

	/**
	 * This is where you will read the provided plan and execute it. If your plan is
	 * correct then when the plan is empty the scenario should end with a victory.
	 * If the scenario keeps running after you run out of actions to execute then
	 * either your plan is incorrect or your execution of the plan has a bug.
	 *
	 * For the compound actions you will need to check their progress and wait until
	 * they are complete before issuing another action for that unit. If you issue
	 * an action before the compound action is complete then the peasant will stop
	 * what it was doing and begin executing the new action.
	 *
	 * To check a unit's progress on the action they were executing last turn, you
	 * can use the following: historyView.getCommandFeedback(playernum,
	 * stateView.getTurnNumber() - 1).get(unitID).getFeedback() This returns an enum
	 * ActionFeedback. When the action is done, it will return
	 * ActionFeedback.COMPLETED
	 *
	 * Alternatively, you can see the feedback for each action being executed during
	 * the last turn. Here is a short example. if (stateView.getTurnNumber() != 0) {
	 * Map<Integer, ActionResult> actionResults =
	 * historyView.getCommandFeedback(playernum, stateView.getTurnNumber() - 1); for
	 * (ActionResult result : actionResults.values()) { <stuff> } } Also remember to
	 * check your plan's preconditions before executing!
	 */
	@Override
	public Map<Integer, Action> middleStep(State.StateView stateView, History.HistoryView historyView) {
		// Create the map for actions
		Map<Integer, Action> actionMap = new HashMap<Integer, Action>();

		// Return the empty map for actions if there is no plan
		if (plan.isEmpty()) {
			return actionMap;
		}

		// Get the turn number of the previous turn
		int previousTurnNumber = stateView.getTurnNumber() - 1;
		// If there is previous turn, then add the next action to the action map
		if (previousTurnNumber < 0) {
			addNextAction(actionMap, stateView);
			return actionMap;
		}

		// Create a map of results of actions of the previous turn
		Map<Integer, ActionResult> previousActions = historyView.getCommandFeedback(playernum, previousTurnNumber);
		// A boolean flag to check if the turn is finished
		boolean finished = false;
		// While the previous turn was not yet finished
		while (!finished) {
			// Set the boolean flag for finished to true if the plan is empty
			if (plan.empty()) {
				finished = true;
			} else {
				// Consider the first action in plan
				StripsAction nextAction = plan.peek();
				// Get the result of the last action committed by the same unit
				ActionResult lastActionResult = previousActions.get(nextAction.getUnitID());
				// If last action failed
				if (lastActionFailed(lastActionResult)) {
					actionMap.put(lastActionResult.getAction().getUnitId(), lastActionResult.getAction());
				} // Set the boolean flag for finished to true if the peasant unavailable 
				  // or if the next state is to build more peasant
				if (!peasantAvailable(actionMap, nextAction, lastActionResult) || waitOnBuild(actionMap, nextAction)) {
					finished = true;
				} else {
					// Add first action in plan to the action map
					addNextAction(actionMap, stateView);
				}
			}
		}
		return actionMap;
	}

	// 4 HELPER METHODS FOR MIDDLESTEP

	/**
	 * Check if the last action fail
	 * 
	 * @param lastActionResult the result of the last action
	 * @return true if last action fail, false otherwise
	 */
	private boolean lastActionFailed(ActionResult lastActionResult) {
		// Check if there is a result for the last action and whether the last action
		// failed
		return lastActionResult != null && lastActionResult.getFeedback() == ActionFeedback.FAILED;
	}

	/**
	 * Check if a peasant is available
	 * @param actionMap        the map of SEPIA actions
	 * @param nextAction       the next action to consider
	 * @param lastActionResult the result of the last action
	 * @return true if a peasant is available, false otherwise
	 */
	private boolean peasantAvailable(Map<Integer, Action> actionMap, StripsAction nextAction,
			ActionResult lastActionResult) {
		// Check if the peasant ID is in the actions map, if there is a last action
		// result, and if the last action
		// result is incomplete.
		return !actionMap.containsKey(nextAction.getUnitID()) && !(lastActionResult != null
				&& lastActionResult.getFeedback().ordinal() == ActionFeedback.INCOMPLETE.ordinal());
	}

	/**
	 * Check if the next action is to build more peasants
	 * @param actionMap  the map of SEPIA actions
	 * @param nextAction the next action to consider
	 * @return true if the next action is to build more peasants, false otherwise
	 */
	private boolean waitOnBuild(Map<Integer, Action> actionMap, StripsAction nextAction) {
		// Check if the unit apply the action is townhall and whether the map is empty
		return nextAction.getUnitID() == GameState.TOWN_HALL_ID && !actionMap.isEmpty();
	}

	/**
	 * Add first STRIPS action in plan as a SEPIA action to the SEPIA action map
	 * @param actionMap the map of SEPIA actions
	 * @param state     the current state that has this action map
	 */
	private void addNextAction(Map<Integer, Action> actionMap, State.StateView state) {
		// Get the first STRIPS action from the plan
		StripsAction action = plan.pop();
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
				plan.push(action);
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

	/**
	 * Returns a SEPIA version of the specified Strips Action.
	 *
	 * You can create a SEPIA deposit action with the following method
	 * Action.createPrimitiveDeposit(int peasantId, Direction townhallDirection)
	 *
	 * You can create a SEPIA harvest action with the following method
	 * Action.createPrimitiveGather(int peasantId, Direction resourceDirection)
	 *
	 * You can create a SEPIA build action with the following method
	 * Action.createPrimitiveProduction(int townhallId, int peasantTemplateId)
	 *
	 * You can create a SEPIA move action with the following method
	 * Action.createCompoundMove(int peasantId, int x, int y)
	 * 
	 * Hint: peasantId could be found in peasantIdMap
	 *
	 * these actions are stored in a mapping between the peasant unit ID executing
	 * the action and the action you created.
	 *
	 * @param action StripsAction
	 * @return SEPIA representation of same action
	 */
	/*private Action createSepiaAction(StripsAction action) {
		return null;
	}*/

	@Override
	public void terminalStep(State.StateView stateView, History.HistoryView historyView) {

	}

	@Override
	public void savePlayerData(OutputStream outputStream) {

	}

	@Override
	public void loadPlayerData(InputStream inputStream) {

	}
}
