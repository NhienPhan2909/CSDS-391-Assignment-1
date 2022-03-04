package edu.cwru.sepia.agent.planner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import edu.cwru.sepia.agent.planner.actions.BuildAction;
import edu.cwru.sepia.agent.planner.actions.DepositAction;
import edu.cwru.sepia.agent.planner.actions.GoAction;
import edu.cwru.sepia.agent.planner.actions.HarvestAction;
import edu.cwru.sepia.agent.planner.actions.StripsAction;
import edu.cwru.sepia.agent.planner.resources.Gold;
import edu.cwru.sepia.agent.planner.resources.Resource;
import edu.cwru.sepia.agent.planner.resources.Wood;
import edu.cwru.sepia.environment.model.state.State;

public class GameState implements Comparable<GameState> {
	// ID for peasant unit template
	public static int PEASANT_TEMPLATE_ID;
	// Position of town hall in a game state
	public static Position TOWN_HALL_POSITION;
	// A string for town hall name to assist constructor later
	public static final String TOWN_HALL_NAME = "townhall";
	// The ID of town hall
	public static int TOWN_HALL_ID;
	
	// The amount gold and wood already extracted
	private int extractedGold = 0;
	private int extractedWood = 0;
	// The target amount of gold and wood 
	private static int requiredGold;
	private static int requiredWood;
	
	// A string for gold mine name to assist constructor later
	private static final String GOLD_MINE_NAME = "GOLD_MINE";
	// The quantity of resource that a peasant can extract each time
	private static final int EXTRACT_AMOUNT = 100; 

	// The cost of a state
	private double cost = 0;
	// The heuristic of a state
	private double heuristic = 0;

	// A map that contain all peasants in state
	private Map<Integer, Peasant> peasants = new HashMap<Integer, Peasant>(3);
	// A set of positions of all resources
	private static Set<Position> resourcePositions = new HashSet<Position>();
	// A map that contain all resources in state
	private Map<Integer, Resource> resources = new HashMap<Integer, Resource>(7);
	// The list of STRIPS Action which is the execution plan
	private List<StripsAction> plan = new ArrayList<StripsAction>(300);
	
	// A boolean flag to determine if this state can build peasant
	private static boolean canBuildPeasants;
	// The ID for the next peasant created
	private int nextID = 0;
	// Amount of gold to build a peasant
	private static final int REQUIRED_GOLD_TO_BUILD = 400;
	// Maximum number of peasants
	private static final int MAX_NUM_PEASANTS = 3;
	// A number use as weight in case build new peasants for heuristic calculation 
	private static final int BUILD_PEASANT_OFFSET = 20000;

	/**
	 * 
	 * @param state The current stateview at the time the plan is being created
	 * @param playernum The player number of agent that is planning
	 * @param requiredGold The goal amount of gold (e.g. 200 for the small scenario)
	 * @param requiredWood The goal amount of wood (e.g. 200 for the small scenario)
	 * @param buildPeasants True if the BuildPeasant action should be consIDered
	 */
	public GameState(State.StateView state, int playernum, int requiredGold, int requiredWood, boolean buildPeasants) {
		// Set the target amount of gold and wood as well as the build peasant flag
		GameState.requiredGold = requiredGold;
		GameState.requiredWood = requiredWood;
		GameState.canBuildPeasants = buildPeasants;
		
		// For each resource node
		state.getAllResourceNodes().stream().forEach(e -> {
			// Get the position of the resource
			Position position = new Position(e.getXPosition(), e.getYPosition());
			// Add the resource position to the set of all resources' positions
			GameState.resourcePositions.add(position);
			// If the resource is gold
			if(e.getType().name().equals(GOLD_MINE_NAME)) {
				// Add a new gold unit to map of all resources using the resource ID as the mapping key
				resources.put(e.getID(), new Gold(e.getID(), e.getAmountRemaining(), position));
			} else {
				// Otherwise add a new wood unit to map of all resources using the resource ID as the mapping key
				resources.put(e.getID(), new Wood(e.getID(), e.getAmountRemaining(), position));
			}
		});
		
		// For each unit node (peasants and town hall)
		state.getAllUnits().stream().forEach(e -> {
			// Get the position of the unit
			Position position = new Position(e.getXPosition(), e.getYPosition());
			// If the unit is a town hall
			if(e.getTemplateView().getName().toLowerCase().equals(TOWN_HALL_NAME)) {
				// Set town hall name and position for this game state
				GameState.TOWN_HALL_POSITION = position;
				GameState.TOWN_HALL_ID = e.getID();
			} else {
				// Otherwise (the unit is a peasant) set the peasant template ID for this game state
				GameState.PEASANT_TEMPLATE_ID = e.getTemplateView().getID();
				// Create a peasant at the town hall
				// Add to the map of all peasants using the peasant ID as the mapping key
				this.peasants.put(e.getID(), new Peasant(e.getID(), TOWN_HALL_POSITION));
			}
		});
		// Calculate the ID for the next peasant that might be created
		this.nextID = 1 + this.peasants.size() + this.resources.size();
	}

	/**
	 * This constructor uses the previous game state as input. Always call this
	 * constructor, except only when the game begins to run.
	 * 
	 * @param gameState: the previous state
	 */
	public GameState(GameState state) {
		// Set the extracted amount of gold and wood
		this.extractedGold = state.extractedGold;
		this.extractedWood = state.extractedWood;
		// Set the ID for the next peasant that might be created
		this.nextID = state.nextID;
		// Set the cost of this state
		this.cost = state.cost;
		// Copy the map of peasants from the parameter state to this state
		state.peasants.values().stream().forEach(e -> this.peasants.put(e.getID(), new Peasant(e)));
		// Copy the map of resources from the parameter state to this state
		state.resources.values().stream().forEach(e -> {
			if(e.isGold()) {
				this.resources.put(e.getID(), new Gold(e));
			} else {
				this.resources.put(e.getID(), new Wood(e));
			}
		});	
		// Copy the execution plan from the parameter state to this state
		state.plan.stream().forEach(e -> plan.add(e));
	}
	
	/**
	 * Get the peasant with a specific ID
	 * @param peasantID the ID to look for
	 * @return the peasant with the correct ID
	 */
	private Peasant getSpecificPeasant(int peasantID) {
		return this.peasants.get(peasantID);
	}

	/**
	 * Get the resource with a specific ID
	 * @param resourceID the ID to look for
	 * @return the resource with the correct ID
	 */
	private Resource getSpecificResource(int resourceID) {
		return this.resources.get(resourceID);
	}

	/**
	 * Check if a peasant can harvest
	 * @param peasant the peasant to check
	 * @return true if can harvest, false otherwise
	 */
	private boolean peasantCanHarvest(Peasant peasant) {
		// Check if there is a resource at peasant's position and if the resource still remains
		return isResourcePosition(peasant.getPosition()) && getResourceForPosition(peasant.getPosition()).stillRemaining();
	}
	
	/**
	 * Get resource at a specific position.
	 * @param position the position to get resource
	 * @return resource at that position
	 */
	private Resource getResourceForPosition(Position position) {
		// Get all resources from the resource map with the correct position and get the first instance (safe check)
		return this.resources.values().stream().filter(e -> e.getPosition().equals(position)).findFirst().get();
	}

	/**
	 * Check if there are resources at a specific position
	 * @param position position to check
	 * @return true if there is resource at this position, false otherwise
	 */
	private boolean isResourcePosition(Position position) {
		// Check if the set of all resource positions contains the position we want to check
		return GameState.resourcePositions.contains(position);
	}
	
	/**
	 * Increment extracted gold by a certain amount
	 * @param amount the amount to add
	 */
	private void increaseExtractedGold(int amount) {
		this.extractedGold += amount;
	}
	
	/**
	 * Increment extracted wood by a certain amount
	 * @param amount the amount to add
	 */
	private void increaseExtractedWood(int amount) {
		this.extractedWood += amount;
	}

	/**
	 * Get the execution plan and turn it into a Stack data structure instead of list
	 * to support AStarsearch in PlannerAgent
	 * @return the execution plan as a stack of STRIPS actions
	 */
	public Stack<StripsAction> getPlan() {
		// A variable for plan as Stack data structure
		Stack<StripsAction> plan = new Stack<StripsAction>();
		// Copy each action in plan to the new Stack data structure
		for(int i = this.plan.size() - 1; i > -1; i--) {
			plan.push(this.plan.get(i));
		}
		return plan;
	}

	/**
	 * Check if already reach the goal
	 * @return true if the goal conditions are met in this instance of game state.
	 */
	public boolean isGoal() {
		// Compare the extracted amount of gold and wood to the required amount
		return extractedGold >= requiredGold && extractedWood >= requiredWood;
	}

	/**
	 * Adds for the amount of resources still needing to be collected.
	 * Adds for not having peasants
	 * Adds for not being near resources if not holding anything
	 * Adds for not being near town all if holding something
	 * Subtracts for if you can make peasants or if you are next to a resource and not holding anything
	 * 
	 * @return The value estimated remaining cost to reach a goal state from this state.
	 */
	public double heuristic() {
		// If the heuristic for this state is already calculated then return the heuristic
		if(this.heuristic != 0) {
			return heuristic;
		}
		
		// If the one resource is extracted more than other 
		if(extractedWood > extractedGold || extractedWood < extractedGold) {
			// Increase the heuristic by the amount resource that a peasant can extract each time
			this.heuristic += 100;
		}
		
		// If the required amount of gold is not yet over-reached
		if(extractedGold <= requiredGold) {
			// Add the difference between required and extracted amount to heuristic
			this.heuristic += (requiredGold - extractedGold);
		} else {
			// Add the difference between extracted and required amount to heuristic
			this.heuristic += (extractedGold - requiredGold);
		}
		// If the required amount of wood is not yet over-reached
		if(extractedWood <= requiredWood) {
			// Add the difference between required and extracted amount to heuristic
			this.heuristic += (requiredWood - extractedWood);
		} else {
			// Add the difference between extracted and required amount to heuristic
			this.heuristic += (extractedWood - requiredWood);
		}
		
		// Check each peasant
		for(Peasant peasant : this.peasants.values()) {
			// Minus the amount of resource being carried by a peasant from the heuristic
			if(peasant.isCarry()) {
				this.heuristic -= peasant.getGoldAmount() + peasant.getWoodAmount();
			} else if(peasantCanHarvest(peasant)) { // If the peasant can harvest resource
				// Minus from the heuristic by half the amount of resource that a peasant can extract each time
				this.heuristic -= 50;
			} else if(!isResourcePosition(peasant.getPosition())) { // If the peasant is not at the resource position
				// Increase the heuristic by the amount resource that a peasant can extract each time
				this.heuristic += 100;
			}
		}
		
		if(canBuildPeasants) {
			this.heuristic += (MAX_NUM_PEASANTS - this.peasants.size()) * BUILD_PEASANT_OFFSET;
			if(canBuild()){
				this.heuristic -= BUILD_PEASANT_OFFSET;
			}
		}
		return this.heuristic;
	}

	/**
	 * Cost is updated every time a move is applied.
	 *
	 * @return The current cost to reach this goal
	 */
	public double getCost() {
		return this.cost;
	}

	public boolean canBuild() {
		return extractedGold >= REQUIRED_GOLD_TO_BUILD && this.peasants.size() < MAX_NUM_PEASANTS;
	}

	public List<GameState> generateChildren() {
		List<GameState> children = new ArrayList<GameState>();
		if(canBuildPeasants && this.canBuild()) {
			GameState buildChild = new GameState(this);
			BuildAction action = new BuildAction(TOWN_HALL_ID, PEASANT_TEMPLATE_ID);
			if(action.preconditionsMet(buildChild)) {
				action.apply(buildChild);
				children.add(buildChild);
			}
			return children;
		}

		GameState child = new GameState(this);
		for(Peasant peasant : this.peasants.values()) {			
			if(peasant.isCarry()) {
				if(peasant.getPosition().equals(TOWN_HALL_POSITION)) {
					DepositAction action = new DepositAction(peasant);
					if(action.preconditionsMet(child)) {
						action.apply(child);
					}
				} else {
					GoAction action = new GoAction(peasant, TOWN_HALL_POSITION);
					if(action.preconditionsMet(child)) {
						action.apply(child);
					}
				}
			} else if(peasantCanHarvest(peasant)) {
				for(Resource resource : this.resources.values()) {
					HarvestAction action = new HarvestAction(peasant, resource);
					if(action.preconditionsMet(child)) {
						action.apply(child);
					}
				}
			} else {
				for(Resource resource : this.resources.values()) {
					GameState innerChild = new GameState(child);
					GoAction action = new GoAction(peasant, resource.getPosition());
					if(action.preconditionsMet(innerChild)) {
						action.apply(innerChild);
					}
					for(Peasant other : this.peasants.values()) {
						if(!other.equals(peasant) && !other.isCarry() && !peasantCanHarvest(peasant)) {
							if(resource.getAmountRemaining() >= EXTRACT_AMOUNT * 2) {
								GoAction otherAction = new GoAction(other, resource.getPosition());
								if(otherAction.preconditionsMet(innerChild)) {
									otherAction.apply(innerChild);
								}
							}
						}
					}
					children.add(innerChild);
				}
			}
		}
		children.add(child);
		
		for(Peasant peasant : this.peasants.values()) {
			GameState innerChild = new GameState(this);
			
			DepositAction depositAction = new DepositAction(peasant);
			if(depositAction.preconditionsMet(innerChild)) {
				depositAction.apply(innerChild);
			}
			
			for(Resource resource : this.resources.values()) {
				GameState innerInnerChild = new GameState(innerChild);
				StripsAction action = null;
				if(peasant.getPosition().equals(resource.getPosition())) {
					action = new HarvestAction(peasant, resource);
				} else {
					action = new GoAction(peasant, resource.getPosition());
				}
				if(action.preconditionsMet(innerInnerChild)) {
					action.apply(innerInnerChild);
				}
				children.add(innerInnerChild);
			}
			
			GoAction moveAction = new GoAction(peasant, TOWN_HALL_POSITION);
			if(moveAction.preconditionsMet(innerChild)) {
				moveAction.apply(innerChild);
			}
			
			children.add(innerChild);
		}
		
		return children;
	}

	public void applyBuildAction(StripsAction action) {
		this.extractedGold = this.extractedGold - REQUIRED_GOLD_TO_BUILD;
		Peasant peasant = new Peasant(nextID, new Position(TOWN_HALL_POSITION));
		nextID++;
		this.peasants.put(peasant.getID(), peasant);
	}

	public void applyMoveAction(StripsAction action, int peasantID, Position destination) {
		getSpecificPeasant(peasantID).setPosition(destination);
	}

	public void applyHarvestAction(StripsAction action, int peasantID, int resourceID) {
		Resource resource = getSpecificResource(resourceID);
		Peasant peasant = getSpecificPeasant(peasantID);
		if(resource.isGold()) {
			peasant.setGoldAmount(Math.min(100, resource.getAmountRemaining()));
			resource.setAmountRemaining(Math.max(0, resource.getAmountRemaining() - 100));
		} else {
			peasant.setWoodAmount(Math.min(100, resource.getAmountRemaining()));
			resource.setAmountRemaining(Math.max(0, resource.getAmountRemaining() - 100));
		}
	}

	public void applyDepositAction(StripsAction action, int peasantID) {
		Peasant peasant = getSpecificPeasant(peasantID);
		if(peasant.hasGold()) {
			increaseExtractedGold(peasant.getGoldAmount());
			peasant.setGoldAmount(0);
		} else {
			increaseExtractedWood(peasant.getWoodAmount());
			peasant.setWoodAmount(0);
		}
	}
	
	public void updatePlanAndCost(StripsAction action) {
		plan.add(action);
		this.cost += action.getCost();
	}

	/**
	 *
	 * @param o The other game state to compare
	 * @return 1 if this state costs more than the other, 0 if equal, -1 otherwise
	 */
	@Override
	public int compareTo(GameState o) {
		if(this.heuristic() > o.heuristic()){
			return 1;
		} else if(this.heuristic() < o.heuristic()){
			return -1;
		}
		return 0;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + extractedGold;
		result = prime * result + extractedWood;
		result = prime * result + ((peasants == null) ? 0 : peasants.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GameState other = (GameState) obj;
		if (extractedGold != other.extractedGold)
			return false;
		if (extractedWood != other.extractedWood)
			return false;
		if (peasants == null) {
			if (other.peasants != null)
				return false;
		} else if (!peasants.equals(other.peasants))
			return false;
		return true;
	}
}