package edu.cwru.sepia.agent.minimax;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.DirectedAction;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.util.Direction;

public class GameState {
	// Get the name for the move and the attack action
	public static final String ACTION_MOVE_NAME = Action.createPrimitiveMove(0, null).getType().name();
	public static final String ACTION_ATTACK_NAME = Action.createPrimitiveAttack(0, 0).getType().name();

	// The helper data structure game map for easy manipulation
	private GameMap GameMap;
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
		this.utility += getHealthUtility()*0.25;
		// Calculate utility of the state based on cumulative damaged dealt to enemies
		this.utility += getDamageUtility()*0.25;
		// Calculate utility of the state based on the probability that our footmen can
		// avoid obstacles (resources)
		this.utility += getPositionUtility()*0.5;
		// Modify the utility boolean flag
		this.utilityCalculated = true;
		return this.utility;
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
		// if the map has no resources unit at all or no resource units near a footman-archer pair
		if (this.GameMap.resourceUnits.isEmpty() || noresourceUnitsAreInTheArea()) {
			// return the distance to the enemy with negative weight
			return distanceFromEnemy() * -1;
		}
		// calculate the percentage of footmen blocked by resources
		double percentageBlocked = percentageOfBlockedFootmen();
		// if there are footmen blocked by resources
		if (percentageBlocked > 0) {
			// return the percentage of footmen blocked with negative weight
			return -200000 * percentageBlocked;
		}
		// return the distance to the enemy with negative weight otherwise
		return distanceFromEnemy() * -1;
	}

	/**
	 * @return ratio between footmen blocked by resources and total footmen still
	 *         alive
	 */
	private double percentageOfBlockedFootmen() {
		// variable to count number of footmen blocked
		int blockedCount = 0;
		// variable to count number of footmen alive
		int totalCount = 0;
		// check each alive footman
		for (GameUnit ally : this.GameMap.getAliveAllies()) {
			// find the closest enemy to this footman
			GameUnit enemy = this.getClosestEnemy(ally);
			// if there is a closest enemy
			if (enemy != null) {
				// get the position of the footman
				int i = ally.getXPosition();
				int j = ally.getYPosition();
				// while the enemy does not have the same position with the footman
				while (i != enemy.getXPosition() || j != enemy.getYPosition()) {
					// increment blocked count if there is an element at current position
					if (this.GameMap.isPositionValid(i, j) && this.GameMap.isResource(i, j)) {
						blockedCount++;
					}
					// increment/decrement i to consider positions closer to enemy
					if (i < enemy.getXPosition()) {
						i++;
					} else if (i > enemy.getXPosition()) {
						i--;
					}
					// // increment/decrement j to consider positions closer to enemy
					if (j < enemy.getYPosition()) {
						j++;
					} else if (j > enemy.getYPosition()) {
						j--;
					}
				}				
			}
			// increment the count for number of alive footmen
			totalCount++;
		}
		// return 0 if there is no alive footmen
		if (totalCount == 0) {
			return 0;
		}
		// otherwise return the ratio between footmen blocked and total alive footmen
		//return blockedCount / totalCount;
		return blockedCount / totalCount;
	}

	/**
	 * @return true if no resource units near footman-archer pair
	 */
	private boolean noresourceUnitsAreInTheArea() {
		// variable to count the resource
		int resourceCount = 0;
		// variable to count the game units
		int unitCount = 0;
		// check each footman that is still alive
		for (GameUnit ally : this.GameMap.getAliveAllies()) {
			// check each enemy that is still alive
			for (GameUnit enemy : this.GameMap.getAliveEnemies()) {
				// increment resource count if there are resource near footman-archer
				if (numresourceUnitInAreaBetween(ally, enemy) != 0) {
					resourceCount++;
				}
			}
			// increment the unit count after we check are around a footman
			unitCount++;
		}
		// return true if no resource units near footman-archer pair, false otherwise
		return resourceCount < unitCount;
	}

	/**
	 * @param ally: footman unit
	 * @param enemy: archer unit
	 * @return the number of resource units in the largest rectangle possible between
	 * the two game units' coordinates.
	 */
	private double numresourceUnitInAreaBetween(GameUnit ally, GameUnit enemy) {
		// variable to count resource unit
		double resourceCount = 0.0;
		// check each position inside a rectangular area between two game units
		// get the largest horizontal dimension for our rectangular area
		for (int i = Math.min(ally.getXPosition(), enemy.getXPosition()); i < Math.max(ally.getXPosition(),
				enemy.getXPosition()); i++) {
			// get the largest vertical dimension for our rectangular area
			for (int j = Math.min(ally.getYPosition(), enemy.getYPosition()); j < Math.max(ally.getYPosition(),
					enemy.getYPosition()); j++) {
				// increment resource count if there is a resource at a position inside the area
				if (this.GameMap.isResource(i, j)) {
					resourceCount += 1;
				}
			}
		}
		return resourceCount;
	}

	/**
	 * @return the cumulative distance to the closest enemy from each footman
	 */
	private double distanceFromEnemy() {
		// variable to keep track of the closest distance
		double utility = 0.0;
		// check each footman still alive
		for (GameUnit ally : this.GameMap.getAliveAllies()) {
			// a variable to hold the distance between this footman and enemies
			double value = Double.POSITIVE_INFINITY;
			// check each enemy still alive
			for (GameUnit enemy : this.GameMap.getAliveEnemies()) {
				// calculate the Manhattan distance (the distance the footman may have to move)
				// between footman and enemy and update value if the newly calculated value is smaller
				value = Math.min(this.GameMap.manhattanDistance(ally, enemy), value);
			}
			// add the smallest distance to an enemy from this footman to the cumulative distance
			if (value != Double.POSITIVE_INFINITY) {
				utility += value;
			}
		}
		return utility;
	}

	/**
	 * @param ally: the footman unit
	 * @return the closest enemy 
	 */
	private GameUnit getClosestEnemy(GameUnit ally) {
		// create a variable to keep track of the closest enemy
		GameUnit closestEnemy = null;
		// check each enemy still alive
		for (GameUnit enemy : this.GameMap.getAliveEnemies()) {
			// if there is no enemy considered then by default this is closest enemy
			if (closestEnemy == null) {
				closestEnemy = enemy;
			} // if the distance to this enemy is smaller than the current samllest distance to enemy
			else if (this.GameMap.manhattanDistance(ally, enemy) < this.GameMap.manhattanDistance(ally, closestEnemy)) {
				// update closest enemy
				closestEnemy = enemy;
			}
		}
		return closestEnemy;
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