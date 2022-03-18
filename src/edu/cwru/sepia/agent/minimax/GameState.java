package edu.cwru.sepia.agent.minimax;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.DirectedAction;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.agent.HelperAstar;
import edu.cwru.sepia.agent.AstarAgent;
import edu.cwru.sepia.agent.AstarAgent.MapLocation;
import edu.cwru.sepia.agent.planner.actions.StripsAction;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.util.Direction;

public class GameState {
	// Create an AstarAgent
	AstarAgent aStarAgent;
	
	// Get the name for the move and the attack action
	public static final String ACTION_MOVE_NAME = Action.createPrimitiveMove(0, null).getType().name();
	public static final String ACTION_ATTACK_NAME = Action.createPrimitiveAttack(0, 0).getType().name();

	// The helper data structure game map for easy manipulation
	public GameMap GameMap;
	// A flag to check if it is our turn to play
	private boolean ourTurn;
	// A flag to check whether utility for a state is calculated yet
	private boolean utilityCalculated = false;
	// The utility of a state
	private double utility = 0.0;

	/**
	 * Constructor that takes from a SEPIA state view. This is only called once when
	 * the game begins to run.
	 * 
	 * @param state
	 */
	public GameState(State.StateView state) {
		// Create a game map
		this.GameMap = new GameMap(state.getXExtent(), state.getYExtent());
		// Add all game units to the game map data structure
		state.getAllUnits().stream().forEach((e) -> {
			this.GameMap.addGameUnit(e.getID(), e.getXPosition(), e.getYPosition(), e.getHP(), e.getHP(),
					e.getTemplateView().getBasicAttack(), e.getTemplateView().getRange());
		});
		// Add all resource units to the map data structure
		state.getAllResourceNodes().stream().forEach((e) -> {
			this.GameMap.addResource(e.getID(), e.getXPosition(), e.getYPosition());
		});
		// Change the turn boolean flag to true to signify that we play the first turn
		this.ourTurn = true;
		aStarAgent = new AstarAgent(2);
	}

	/**
	 * This constructor uses the previous game state as input. Always call this
	 * constructor, except only when the game begins to run.
	 * 
	 * @param gameState: the previous state
	 */
	public GameState(GameState gameState) {
		// Create a game map
		this.GameMap = new GameMap(gameState.GameMap.width, gameState.GameMap.height);
		// Add all game units to the game map data structure
		gameState.GameMap.getAllGameUnits().stream().forEach((e) -> {
			this.GameMap.addGameUnit(e.getUnitId(), e.getXPosition(), e.getYPosition(), e.getCurrentHealth(),
					e.getMaxHealth(), e.getAttackDamage(), e.getAttackRange());
		});
		// Add all resource units to the game map data structure
		gameState.GameMap.resourceUnits.values().stream().forEach((e) -> {
			this.GameMap.addResource(e.getUnitId(), e.getXPosition(), e.getYPosition());
		});
		// Change the boolean turn flag to signify the next turn is for the opposite
		// side
		this.ourTurn = !gameState.ourTurn;
		// Calculate the utility and modify the utility boolean flag
		this.utilityCalculated = gameState.utilityCalculated;
		this.utility = gameState.utility;
		aStarAgent = gameState.aStarAgent;
	}

	/**
	 * Calculate the utility of a state.
	 * 
	 * @return
	 */
	public double getUtility() {
		// Check if utility is already calculated
		if (this.utilityCalculated) {
			return this.utility;
		}

		// Calculate utility of the state based on the cumulative health our footmen
		// still have
		//this.utility += getHealthUtility()*20;
		// Calculate utility of the state based on cumulative damaged dealt to enemies
		this.utility += getDamageUtility()*20;
		// Calculate utility of the state based on the probability that our footmen can
		// avoid obstacles (resources)
		this.utility += getPositionUtility()*50;
		// Modify the utility boolean flag
		this.utilityCalculated = true;
		return this.utility;
	}
	
	public boolean canFootmanAttack(MapLocation footmanPosition, MapLocation archerPosition, 
			int xExtent, int yExtent, Set<MapLocation> resourceLocations) {
		for (int x = -1; x < 2; x++) {
	    	for (int y = -1; y < 2; y++) {
	    		// Exclude the current position
	    		if (x == footmanPosition.x && y == footmanPosition.y) {
	    			continue;
	    		}
	    		
	    		// Compute the location of the potential positions
	    		MapLocation potentialPosition = new MapLocation(footmanPosition.x + x, footmanPosition.y + y, null, 0);
	    			
	    		if (HelperAstar.isPositionValid(footmanPosition, potentialPosition, xExtent, yExtent, resourceLocations)
	    				&& (potentialPosition.equals(archerPosition))) {
	    			return true;
	    		}
	    	}
	    }
		return false;
	}
	

	/**
	 * @return the cumulative amount of health-retained ratio our footmen still have
	 */
	private double getHealthUtility() {
		// utility variable
		double utility = 0.0;
		// check each footman that is still alive
		for (GameUnit GameUnit : this.GameMap.getAliveAllies()) {
			// add the ratio between the health still have and the maximum health of each
			// footman to the utility
			utility += GameUnit.getCurrentHealth() / GameUnit.getMaxHealth();
		}
		return utility;
	}

	/**
	 * @return the cumulative damage has been dealt to enemies
	 */
	private double getDamageUtility() {
		// utility variable
		double utility = 0.0;
		// check each archer that is still alive
		for (GameUnit GameUnit : this.GameMap.getAliveEnemies()) {
			// add the amount of damage each archer has endured to the utility
			utility += GameUnit.getMaxHealth() - GameUnit.getCurrentHealth();
		}
		return utility;
	}

	/**
	 * @return how well the footmen can avoid running into resources
	 */
	private double getPositionUtility() {
		// return the distance to the enemy with negative weight otherwise
		double x = distanceFromEnemy();
		if (x == -0)
			return 0;
		else
			return x*-1;
	}

	/**
	 * @return the cumulative distance to the closest enemy from each footman
	 */
	private double distanceFromEnemy() {
		double utility = 0.0;
		
		// A variable to hold the result of AstarSearch
		Stack<MapLocation> AstarResult = new Stack<MapLocation>();
		
		// Create resource location set
		Set<MapLocation> resourceLocations = new HashSet<MapLocation>();
		for (Map.Entry<Integer, ResourceUnit> set : this.GameMap.resourceUnits.entrySet()) {
			int x = set.getValue().getXPosition();
			int y = set.getValue().getYPosition();
			MapLocation newResourcePosition = new MapLocation(x, y, null, 0);
			resourceLocations.add(newResourcePosition);
        }
		
		for(GameUnit ally : this.GameMap.getAliveAllies()){
			double value = Double.POSITIVE_INFINITY;
			if (!this.GameMap.canBeUnderAttackUnits(ally).isEmpty()) {
				return 0;
			}
			double newValue = 0;
			for(GameUnit enemy : this.GameMap.getAliveEnemies()){
				MapLocation start = new MapLocation(ally.getXPosition(), ally.getYPosition(), null, 0);
				MapLocation goal = new MapLocation(enemy.getXPosition(), enemy.getYPosition(), null, 0);
				
				
				AstarResult = aStarAgent.AstarSearch(goal, start, this.GameMap.getXExtent(), this.GameMap.getYExtent(),
						null, resourceLocations);
				while (!AstarResult.isEmpty()) {
					newValue = AstarResult.pop().cost;
				}
				value = Math.min(newValue, value);
			}
			if(value != Double.POSITIVE_INFINITY){
				utility += value;
			}
		}
		return utility;
	}

	/**
	 * Takes into account the current turn (good or bad) and generates children for
	 * the current ply.
	 * 
	 * @return all of the possible children of this GameState
	 */
	public List<GameStateChild> getChildren() {
		// get all the game units belong the player of this turn
		Collection<GameUnit> activeUnits;
		// get footman if it is our turn
		if (ourTurn) {
			activeUnits = this.GameMap.getAliveAllies();
		} // get archer if it is not our turn
		else {
			activeUnits = this.GameMap.getAliveEnemies();
		}
		// Create a list of lists of actions to keep track of the actions of each game unit
		// Each inner list is a list of actions for one game unit
		// Each element of the out list is the collections of actions for one game unit
		List<List<Action>> actionsForEachGameUnit = activeUnits.stream().map(e -> getActionsForGameUnit(e))
				.collect(Collectors.toList());
		// Create a list of maps - each map shows combinations of a actions for a unit
		List<Map<Integer, Action>> actionMaps = enumerateActionCombinations(actionsForEachGameUnit);
		// return a list of game state children for the the list of maps above (one map - one game state chile)
		return enumerateChildrenFromActionMaps(actionMaps);
	}

	/**
	 * For a given GameUnit generates all their possible moves: Move: NORTH, EAST,
	 * SOUTH, WEST or Attack any enemy close enough
	 * 
	 * @param GameUnit
	 * @return List of actions given GameUnit could take
	 */
	private List<Action> getActionsForGameUnit(GameUnit GameUnit) {
		// create a list to store actions
		List<Action> actions = new ArrayList<Action>();
		// go over each direction
		for (Direction direction : Direction.values()) {
			switch (direction) {
			case NORTH:
			case EAST:
			case SOUTH:
			case WEST:
				// calculate the new X position regarding to the direction
				int nextX = GameUnit.getXPosition() + direction.xComponent();
				// calculate the new Y position regarding to the direction
				int nextY = GameUnit.getYPosition() + direction.yComponent();
				// add the potential action to the action list if the move is valid
				if (this.GameMap.isPositionValid(nextX, nextY) && this.GameMap.isEmpty(nextX, nextY)) {
					actions.add(Action.createPrimitiveMove(GameUnit.getUnitId(), direction));
				}
				break;
			default:
				break;
			}
		}
		// check for an attack move an add attack move to actions if possible
		for (Integer id : this.GameMap.canBeUnderAttackUnits(GameUnit)) {
			actions.add(Action.createPrimitiveAttack(GameUnit.getUnitId(), id));
		}
		return actions;
	}

	/**
	 * Give a list of actions for every GameUnit returns Maps from unitId to Action
	 * for each possible combination of actions for a pair of footmen or archers
	 */
	private List<Map<Integer, Action>> enumerateActionCombinations(List<List<Action>> allActions) {
		// Create a list of maps - each map contains combination of actions for each unit
		List<Map<Integer, Action>> actionMaps = new ArrayList<Map<Integer, Action>>();
		// return if the input action list is empty
		if (allActions.isEmpty()) {
			return actionMaps;
		}
		// Get the list of actions for the first unit
		List<Action> actionsForFirstGameUnit = allActions.get(0);
		// Check every actions of the first unit
		for (Action actionForGameUnit : actionsForFirstGameUnit) {
			// If there is only one action action in the input list
			if (allActions.size() == 1) {
				// create a new map for actions of the unit
				Map<Integer, Action> actionMap = new HashMap<Integer, Action>();
				// add the action to the action map
				actionMap.put(actionForGameUnit.getUnitId(), actionForGameUnit);
				// add the action map to the list of maps
				actionMaps.add(actionMap);
			} else {
				// check all actions for the other units
				for (Action actionForOtherGameUnit : allActions.get(1)) {
					// create an action map to contain actions of each unit
					Map<Integer, Action> actionMap = new HashMap<Integer, Action>();
					// add the action of the first unit to the action map
					actionMap.put(actionForGameUnit.getUnitId(), actionForGameUnit);
					// add the action of the other units to the map
					actionMap.put(actionForOtherGameUnit.getUnitId(), actionForOtherGameUnit);
					// add the action map to the list of maps
					actionMaps.add(actionMap);
				}
			}
		}
		return actionMaps;
	}

	/**
	 * Given all Maps from unitId to Action that are possible for the current ply
	 * generate the GameStateChild for each Map
	 */
	private List<GameStateChild> enumerateChildrenFromActionMaps(List<Map<Integer, Action>> actionMaps) {
		// Create a list of game state child to keep track of the children states
		List<GameStateChild> children = new ArrayList<GameStateChild>(25);
		// check the map of each unit in the list of action maps 
		for (Map<Integer, Action> actionMap : actionMaps) {
			// Create a game state child
			GameState child = new GameState(this);
			// Apply each action in the action map of this unit to the new game state
			for (Action action : actionMap.values()) {
				child.applyAction(action);
			}
			// add this new game state to the list of children states
			children.add(new GameStateChild(actionMap, child));
		}
		return children;
	}

	/**
	 * Applies a given action to this GameState
	 * @param action: move or attack
	 */
	private void applyAction(Action action) {
		// if it is a move
		if (action.getType().name().equals(ACTION_MOVE_NAME)) {
			// Create a directed action for this action
			DirectedAction directedAction = (DirectedAction) action;
			// Execute the directed action - move the unit to the new destination
			this.GameMap.moveGameUnit(directedAction.getUnitId(), directedAction.getDirection().xComponent(),
					directedAction.getDirection().yComponent());
		} // if it is an attack
		else {
			// Create a targeted action
			TargetedAction targetedAction = (TargetedAction) action;
			// Get the attacking unit using the targeted action
			GameUnit attacker = this.GameMap.getGameUnit(targetedAction.getUnitId());
			// Get the attacked unit using the targeted action
			GameUnit defender = this.GameMap.getGameUnit(targetedAction.getTargetId());
			// Execute the attack
			this.GameMap.attackGameUnit(attacker, defender);
		}
	}

}