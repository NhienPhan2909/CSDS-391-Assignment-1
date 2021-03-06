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
	// ID for the Peasant unit type
	public static int PEASANT_TEMPLATE_ID;
	// Position of town hall in a game state
	public static Position TOWN_HALL_POSITION;
	// The ID of town hall
	public static int TOWN_HALL_ID;
	
	// The amount gold and wood already extracted
	private int extractedGold = 0;
	private int extractedWood = 0;
	// The quantity of resource that a peasant can extract each time
	private static final int EXTRACT_AMOUNT = 100;
	// The quantity of resource that a peasant can extract each time
	private static final int REQUIRED_GOLD_FOR_PEASANT_BUILD = 400;
	// Maximum number of peasants can be built
	private static final int MAX_PEASANTS = 3;
		
	// The target amount of gold and wood 
	private static int requiredGold;
	private static int requiredWood;
	
	// The cost of a state
	private double cost = 0;
	// The heuristic of a state
	private double heuristic = 0;
	// The ID of the next peasant to be built
	private int nextPeasantID = 0;

	// A map that contain all peasants in state
	private Map<Integer, Peasant> peasants = new HashMap<Integer, Peasant>(3);
	// A set of positions of all resources
	private static Set<Position> resourcePositions = new HashSet<Position>();
	// A map that contain all resources in state
	private Map<Integer, Resource> resources = new HashMap<Integer, Resource>(7);
	// The list of STRIPS Action which is the execution plan
	private List<StripsAction> plan = new ArrayList<StripsAction>(300);
	
	// A boolean flag to determine if this state should consider build peasant
	private static boolean canBuildPeasants;
	
	/**
     * Construct a GameState from a stateview object. This is used to construct the initial search node. All other
     * nodes should be constructed from the another constructor you create or by factory functions that you create.
     * This constructor is only called once when the game begin to run.
     *
     * @param state The current stateview at the time the plan is being created
     * @param playernum The player number of agent that is planning
     * @param requiredGold The goal amount of gold (e.g. 200 for the small scenario)
     * @param requiredWood The goal amount of wood (e.g. 200 for the small scenario)
     * @param buildPeasants True if the BuildPeasant action should be considered
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
			if(e.getType().name().equals("GOLD_MINE")) {
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
			if(e.getTemplateView().getName().toLowerCase().equals("townhall")) {
				// Set town hall name and position for this game state
				GameState.TOWN_HALL_POSITION = position;
				GameState.TOWN_HALL_ID = e.getID();
			} else { // Otherwise (the unit is a peasant) 
				// Create a peasant at the town hall
				GameState.PEASANT_TEMPLATE_ID = e.getTemplateView().getID();
				// Add to the map of all peasants using the peasant ID as the mapping key
				this.peasants.put(e.getID(), new Peasant(e.getID(), TOWN_HALL_POSITION));
			}
		});
		// Calculate the unique ID for the peasant unit
		this.nextPeasantID = 1 + this.peasants.size() + this.resources.size();
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
		// Set the ID for next peasant
		this.nextPeasantID = state.nextPeasantID;
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
     * Unlike in the first A* assignment there are many possible goal states. As long as the wood and gold requirements
     * are met the peasants can be at any location and the capacities of the resource locations can be anything. Use
     * this function to check if the goal conditions are met and return true if they are.
     *
     * @return true if the goal conditions are met in this instance of game state.
     */
	public boolean isGoal() {
		// Compare the extracted amount of gold and wood to the required amount
		return extractedGold >= requiredGold && extractedWood >= requiredWood;
	}

	/**
     * Write your heuristic function here. Remember this must be admissible for the properties of A* to hold. If you
     * can come up with an easy way of computing a consistent heuristic that is even better, but not strictly necessary.
     *
     * Add a description here in your submission explaining your heuristic.
     *
     * @return The value estimated remaining cost to reach a goal state from this state.
     */
	public double heuristic() {
		// If the heuristic for this state is already calculated then return the heuristic
		if(this.heuristic != 0) {
			return heuristic;
		}
		
		// If the one resource is extracted more than other (minimize over-reach to improve runtime)
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
		
		// If this state should consider build peasant	
		if(canBuildPeasants) {
			// Add to the heuristic an arbitrary large amount of offset multiplied by the number of peasants can 
			// be built more. This is to account for an  immediate negative effect of build action.
			this.heuristic += (MAX_PEASANTS - this.peasants.size()) * 20000;
			// If it is possible to build more peasants
			if(canBuild()){
				// Minus the arbitrary large offset from heuristic to account for the trade off against the 
				// longer-term positive effect that the parallelism will allow.
				this.heuristic -= 20000;
			}
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
		return this.heuristic;
	}

	/**
    *
    * Write the function that computes the current cost to get to this node. This is combined with your heuristic to
    * determine which actions/states are better to explore.
    *
    * @return The current cost to reach this goal
    */
	public double getCost() {
		return this.cost;
	}
	
	/**
	 * Check if it is possible to build a new peasant
	 * @return true if possible, false otherwise
	 */
	public boolean canBuild() {
		return extractedGold >= REQUIRED_GOLD_FOR_PEASANT_BUILD && this.peasants.size() < MAX_PEASANTS;
	}

	/**
     * The branching factor of this search graph are much higher than the planning. Generate all of the possible
     * successor states and their associated actions in this method.
     *
     * @return A list of the possible successor states and their associated actions
     */
	public List<GameState> generateChildren() {
		// Create a list of children game states
		List<GameState> childrenStates = new ArrayList<GameState>();
		
		// If it is possible to build peasants in children states
		if(canBuildPeasants && this.canBuild()) {
			// Create a child state and Build STRIPS action
			GameState buildPeasantState = new GameState(this);
			BuildAction action = new BuildAction(TOWN_HALL_ID, PEASANT_TEMPLATE_ID);
			// Apply the STRIP action to child state and add the child state to the children states list if possible
			if(action.preconditionsMet(buildPeasantState)) {
				action.apply(buildPeasantState);
				childrenStates.add(buildPeasantState);
			}
			return childrenStates;
		}
		
		// Create the first child state from the current state
		GameState childState = new GameState(this);
		
		// Go over each peasant
		for(Peasant peasant : this.peasants.values()) {
			// If the peasant is carrying resources
			if(peasant.isCarry()) {
				// Create a deposit and a go STRIPS actions
				DepositAction depositAction = new DepositAction(peasant);
				GoAction goAction = new GoAction(peasant, TOWN_HALL_POSITION);
				// Apply the deposit action if possible (peasant reach the town hall)
				if(depositAction.preconditionsMet(childState)) {
					depositAction.apply(childState);
				} else { // Apply the go action to go to the town hall
					goAction.apply(childState);
				}
			} else if(peasantCanHarvest(peasant)) { // If peasant can harvest resource
				// Create and apply a harvest action
				HarvestAction harvestAction = new HarvestAction(peasant, getResourceForPosition(peasant.getPosition()));
				harvestAction.apply(childState);					
			} else { // If the peasant is not carrying resources
				// Check each resource unit
				for(Resource resource : this.resources.values()) {
					// Create a child of the current child state
					GameState grandChildState = new GameState(childState);
					// Create a go action towards the resource unit
					GoAction goAction = new GoAction(peasant, resource.getPosition());
					// Apply the go action to the grand child state if possible
					if(goAction.preconditionsMet(grandChildState)) {
						goAction.apply(grandChildState);
					}
					// Add this grand child state to the list of children game states
					childrenStates.add(grandChildState);
				}
			}
		}
		// Add the child state to the list of children game states
		childrenStates.add(childState);
		
		// Go over each peasant again to check the specific game state child of each peasant
		// instead of apply actions to a general game state like above
		for(Peasant peasant : this.peasants.values()) {
			// Create a child state that is specific for each peasant
			GameState specificChildState = new GameState(this);
			// Create and apply the deposit action if possible (peasant reach the town hall)
			DepositAction depositAction = new DepositAction(peasant);
			if(depositAction.preconditionsMet(specificChildState)) {
				depositAction.apply(specificChildState);
			}

			// Check each resource unit
			for(Resource resource : this.resources.values()) {
				// Create a grand child state for the specific child state of the corresponding peasant
				GameState specificGrandChildState = new GameState(specificChildState);
				// Create a STRIPS harvest action if possible, 
				// if not create a STRIPS go action to move the peasant to the resource
				StripsAction action = null;
				if(peasant.getPosition().equals(resource.getPosition())) { 
					action = new HarvestAction(peasant, resource);
				} else {
					action = new GoAction(peasant, resource.getPosition());
				}
				// Apply whichever STRIPS action that met preconditions
				if(action.preconditionsMet(specificGrandChildState)) {
					action.apply(specificGrandChildState);
				}
				// Add this grand child state to the list of children game states
				childrenStates.add(specificGrandChildState);
			}
			// Create the STRIP go action to move the peasant towards the town hall and apply if possible
			GoAction goAction = new GoAction(peasant, TOWN_HALL_POSITION);
			if(goAction.preconditionsMet(specificChildState)) {
				goAction.apply(specificChildState);
			}
			// Add this child state of a specific peasant to the list of children game states
			childrenStates.add(specificChildState);
		}
		
		return childrenStates;
	}
	
	/**
	 * Apply the STRIPS build action to a game state
	 * @param buildAction the STRIPS go action to apply
	 */
	public void applyBuildAction(StripsAction buildAction) {
		// Minus the amount of gold required to build peasants from the amount of extracted gold
		this.extractedGold = this.extractedGold - REQUIRED_GOLD_FOR_PEASANT_BUILD;
		// Create a new peasant and increment the ID for next peasant to be created
		Peasant peasant = new Peasant(nextPeasantID, new Position(TOWN_HALL_POSITION));
		nextPeasantID++;
		// Add the newly created peasant to the map that contains all peasants
		this.peasants.put(peasant.getID(), peasant);
	}

	/**
	 * Apply the STRIPS go action to a game state
	 * @param goAction the STRIPS go action to apply
	 * @param peasantID the ID of the peasant which do this action
	 * @param destination the destination to move to
	 */
	public void applyGoAction(StripsAction goAction, int peasantID, Position destination) {
		getSpecificPeasant(peasantID).setPosition(destination);
	}

	/**
	 * Apply the STRIPS harvest action to a game state
	 * @param harvestAction the STRIPS harvest action to apply
	 * @param peasantID the ID of the peasant which do this action
	 * @param resourceID the ID of the resource to be harvested
	 */
	public void applyHarvestAction(StripsAction harvestAction, int peasantID, int resourceID) {
		// Get the resource and peasant with the correct IDs
		Resource resource = getSpecificResource(resourceID);
		Peasant peasant = getSpecificPeasant(peasantID);
		// If resource is gold
		if(resource.isGold()) {
			// Adjust the gold amount that the peasant carries
			peasant.setGoldAmount(Math.min(100, resource.getAmountRemaining()));
			// Adjust the amount of gold remaining
			resource.setAmountRemaining(Math.max(0, resource.getAmountRemaining() - 100));
		} else { // If the resource is wood
			// Adjust the wood amount that the peasant carries
			peasant.setWoodAmount(Math.min(100, resource.getAmountRemaining()));
			// Adjust the amount of gold remaining
			resource.setAmountRemaining(Math.max(0, resource.getAmountRemaining() - 100));
		}
	}

	/**
	 * Deposit the amount of resource a peasant carries to the town hall
	 * @param depositAction the STRIP deposit action to apply
	 * @param peasantID the ID of the peasant which do this action
	 */
	public void applyDepositAction(StripsAction depositAction, int peasantID) {
		// Get the peasant with the correct ID
		Peasant peasant = getSpecificPeasant(peasantID);
		// If the peasant is carrying gold
		if(peasant.hasGold()) {
			// Increment the amount of extracted gold
			increaseExtractedGold(peasant.getGoldAmount());
			// Set the amount of gold the peasant carries back to 0
			peasant.setGoldAmount(0);
		} else { // If the peasant is carrying wood
			// Increment the amount of extracted wood
			increaseExtractedWood(peasant.getWoodAmount());
			// Set the amount of wood the peasant carries back to 0
			peasant.setWoodAmount(0);
		}
	}
	
	/**
	 * Add a STRIP action to the plan and add the cost of the action to the overall cost of the plan
	 * @param action the action to be added
	 */
	public void updatePlanAndCost(StripsAction action) {
		// Add action to the plan
		plan.add(action);
		// Increment the cost of the plan
		this.cost += action.getCost();
	}

	/**
     * This is necessary to use your state in the Java priority queue. See the official priority queue and Comparable
     * interface documentation to learn how this function should work.
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

	/**
     * This will be necessary to use the GameState as a key in a Set or Map.
     *
     * @param o The game state to compare
     * @return True if this state equals the other state, false otherwise.
     */
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
	
	/**
     * This is necessary to use the GameState as a key in a HashSet or HashMap. Remember that if two objects are
     * equal they should hash to the same value.
     *
     * @return An integer hashcode that is equal for equal states.
     */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + extractedGold;
		result = prime * result + extractedWood;
		result = prime * result + ((peasants == null) ? 0 : peasants.hashCode());
		return result;
	}
}