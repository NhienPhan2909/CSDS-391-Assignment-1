package edu.cwru.sepia.agent.minimax;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import edu.cwru.sepia.agent.AstarAgent.MapLocation;

// A helper class with helper methods to use for easier manipulation of units in the game
public class GameMap {
	// A 2D array that stores the units at their location on the map
	public Unit[][] GameMap;
	// A map contains all game units (footmen and archers on both sides)
	private Map<Integer, GameUnit> GameUnits = new HashMap<Integer, GameUnit>(4);
	// An array list contains all game units belong to our side
	private ArrayList<GameUnit> allies = new ArrayList<GameUnit>(2);
	// An array list contains all game units belong to the other side
	private ArrayList<GameUnit> enemies = new ArrayList<GameUnit>(2);
	// A map contains all resource units
	Map<Integer, ResourceUnit> resourceUnits = new HashMap<Integer, ResourceUnit>();
	// width and height of the map
	int width;
	int height;

	// Constructor
	public GameMap(int width, int height){
		GameMap = new Unit[width][height];
		this.width = width;
		this.height = height;
	}
	
	public int getXExtent() {
		return width;
	}
	
	public int getYExtent() {
		return height;
	}
	
	public Unit[][] getUnitMatrix() {
		return GameMap;
	}

	/**
	 * Add a resource unit to the board data structure
	 * @param resourceId: ID of resource unit
	 * @param xPosition: X position of resource unit
	 * @param yPosition: Y position of resource unit
	 */
	public void addResource(int resourceId, int xPosition, int yPosition){
		// Create a resource unit
		ResourceUnit resource = new ResourceUnit(resourceId, xPosition, yPosition);
		// Record its location on the game map
		GameMap[xPosition][yPosition] = resource;
		// Add it to the map of resource units
		resourceUnits.put(resource.getUnitId(), resource);
	}

	/**
	 * Add a game unit to the game map data structure
	 * @param gameUnitId: ID of the game unit
	 * @param xPosition: X position of the game unit
	 * @param yPosition: Y position of the game unit
	 * @param currentHealth: current health of the game unit
	 * @param maxHealth: the maximum health a unit can fhave
	 * @param attackDamage: the damage a unit can deal towards enemies
	 * @param attackRange: the range of attack of a unit
	 */
	public void addGameUnit(int gameUnitId, int xPosition, int yPosition,
			int currentHealth, int maxHealth, int attackDamage, int attackRange){
		// Create a game unit
		GameUnit GameUnit = new GameUnit(gameUnitId, xPosition, yPosition,
				currentHealth, maxHealth, attackDamage, attackRange);
		// Record its position on the board
		GameMap[xPosition][yPosition] = GameUnit;
		// Add it to the map of all game units
		GameUnits.put(gameUnitId, GameUnit);
		// Add it to the array of units on our side if possible
		if(GameUnit.isAlly()){
			allies.add(GameUnit);
		} else { // Add it to the array of units on the other side otherwise
			enemies.add(GameUnit);
		}
	}

	/**
	 * Move a game unit to a new location
	 * @param gameUnitId: ID of unit to move
	 * @param xOffset: the X distance to move
	 * @param yOffset: the Y distance to move
	 */
	void moveGameUnit(int gameUnitId, int xOffset, int yOffset){
		// Get the game unit with the correct ID
		GameUnit GameUnit = getGameUnit(gameUnitId);
		// Get current position of the unit
		int currentXPosition = GameUnit.getXPosition();
		int currentYPosition = GameUnit.getYPosition();
		// Calculate new position
		int nextXPosition = currentXPosition + xOffset;
		int nextYPosition = currentYPosition + yOffset;
		// Delete the unit at the old location on game map
		GameMap[currentXPosition][currentYPosition] = null;
		// Set new location for the unit
		GameUnit.setXPosition(nextXPosition);
		GameUnit.setYPosition(nextYPosition);
		// Record new location of the unit on game map
		GameMap[nextXPosition][nextYPosition] = GameUnit;
	}

	/**
	 * Attack a unit on the other side
	 * @param attacker: the attacking unit
	 * @param defender: the unit being attacked
	 */
	public void attackGameUnit(GameUnit attacker, GameUnit defender){
		if(defender != null && attacker != null){
			// Minus the health of the attacked unit (the same amount as the damage of the attacker)
			defender.setCurrentHealth(defender.getCurrentHealth() - attacker.getAttackDamage());
		}
	}

	/**
	 * Check if a position is empty
	 * @param xPosition: X position needs to check
	 * @param yPosition: Y position needs to check
	 * @return true if the position is empty, false otherwise
	 */
	public boolean isEmpty(int xPosition, int yPosition){
		return GameMap[xPosition][yPosition] == null;
	}

	/**
	 * Check if there is a resource at a position
	 * @param xPosition: X position needs to check
	 * @param yPosition: Y position needs to check
	 * @return true if there is a resource, false otherwise
	 */
	public boolean isResource(int xPosition, int yPosition){
		// check if there is a unit at this location and whether that unit is a resource unit
		return GameMap[xPosition][yPosition] != null && 
				resourceUnits.containsKey(GameMap[xPosition][yPosition].getUnitId());
	}

	/**
	 * Check if a position is inside the game map
	 * @param xPosition: X position needs to check
	 * @param yPosition: Y position needs to check
	 * @return: true if the position is in the game map, false otherwise
	 */
	public boolean isPositionValid(int xPosition, int yPosition){
		return xPosition >= 0 && xPosition < width && yPosition >= 0 && yPosition < height; 
	}

	/**
	 * Get a game unit
	 * @param gameUnitId: ID of the unit need to get
	 * @return: the correct game unit
	 */
	public GameUnit getGameUnit(int gameUnitId) {
		// Get the game unit with the correct ID
		GameUnit GameUnit = GameUnits.get(gameUnitId);
		// Return null if the unit is already killed
		if(!GameUnit.isAlive()){
			return null;
		}
		// Return the correct game unit
		return GameUnit;
	}

	/**
	 * Get all game units
	 * @return: all game units
	 */
	public Collection<GameUnit> getAllGameUnits() {
		return GameUnits.values();
	}
	
	/**
	 * Get all game unit on our side that is not killed yet
	 * @return: all alive unit on our side
	 */
	public Collection<GameUnit> getAliveAllies(){
		return allies.stream().filter(e -> e.isAlive()).collect(Collectors.toList());
	}

	/**
	 * Get all game unit on the other side that is not killed yet
	 * @return: all alive unit on the other side
	 */
	public Collection<GameUnit> getAliveEnemies(){
		return enemies.stream().filter(e -> e.isAlive()).collect(Collectors.toList());
	}

	/**
	 * Calculate the Manhattan distance between two units
	 * @param gameUnit1: first unit
	 * @param gameUnit2: second unit
	 * @return: Manhattan distance between first and second units
	 */
	public double manhattanDistance(Unit gameUnit1, Unit gameUnit2) {
		return (Math.abs(gameUnit1.getXPosition() - gameUnit2.getXPosition()) 
				+ Math.abs(gameUnit1.getYPosition() - gameUnit2.getYPosition())) - 1;
	}

	/**
	 * Calculate the Euclidean distance (possible for attack) between two units
	 * @param gameUnit1: first unit
	 * @param gameUnit2: second unit
	 * @return: Euclidean distance between first and second units
	 */
	public int attackDistance(GameUnit gameUnit1, GameUnit gameUnit2){
		return (int) Math.floor(Math.hypot(Math.abs(gameUnit1.getXPosition() - gameUnit2.getXPosition()),
				Math.abs(gameUnit1.getYPosition() - gameUnit2.getYPosition())));
	}

	/**
	 * Get a list of ID of units that can be attacked by one unit
	 * @param GameUnit: the unit we are considering
	 * @return: a list of ID of units that can be attacked by that unit
	 */
	public List<Integer> canBeUnderAttackUnits(GameUnit GameUnit) {
		// a list of unit that can be attacked
		List<Integer> attackable = new ArrayList<Integer>();
		// check all other units
		for(GameUnit otherGameUnit : getAllGameUnits()){
			// if two units are on opposite side and the other unit is within the attack range
			if(otherGameUnit.getUnitId() != GameUnit.getUnitId() && (otherGameUnit.isAlly() != GameUnit.isAlly()) && 
					attackDistance(GameUnit, otherGameUnit) <= GameUnit.getAttackRange()){
				// add the other unit to the list of attackable units for the unit that we are considering
				attackable.add(otherGameUnit.getUnitId());
			}
		}
		return attackable;
	}

}
