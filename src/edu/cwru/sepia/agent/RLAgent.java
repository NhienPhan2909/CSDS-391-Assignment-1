package edu.cwru.sepia.agent;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionFeedback;
import edu.cwru.sepia.action.ActionResult;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.environment.model.history.DamageLog;
import edu.cwru.sepia.environment.model.history.DeathLog;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;

import java.io.*;
import java.util.*;

public class RLAgent extends Agent {
	private static final int NUM_LEARNING_EPISODES = 10;
	private static final int NUM_EVALUATING_EPISODES = 5;

    /**
     * Set in the constructor. Defines how many learning episodes your agent should run for.
     * When starting an episode. If the count is greater than this value print a message
     * and call sys.exit(0)
     */
    public final int numEpisodes;

    /**
     * List of your footmen and your enemies footmen
     */
    private List<Integer> myFootmen;
    private List<Integer> enemyFootmen;

    /**
     * Convenience variable specifying enemy agent number. Use this whenever referring
     * to the enemy agent. We will make sure it is set to the proper number when testing your code.
     */
    public static final int ENEMY_PLAYERNUM = 1;

    /**
     * Set this to whatever size your feature vector is.
     */
    public static final int NUM_FEATURES = 2;

    /** Use this random number generator for your epsilon exploration. When you submit we will
     * change this seed so make sure that your agent works for more than the default seed.
     */
    public final Random random = new Random(12345);
    
    /**
	 * Variables for tracking of evaluation/learning episodes
	 */
	private int currentEpisode = 0;
	// Keep track of the episodes in each learning phase
	private int currentPhaseEpisodeCount = 0;
	// Boolean value to signal if episode is in evaluation
	private boolean inEvaluation = true;
	// Store average rewards of 10 learning episodes in a learning phase
	private List<Double> averageRewards = new ArrayList<Double>(10);
	// Store the total rewards of 10 learning episodes
	private double totalReward = 0.0;
	
	/**
	 * Variables for figuring out rewards
	 */
	private State.StateView previousStateView;
	// Flag if there are enemy deaths in a state
	private boolean awardedDeathPoints;

    /**
     * Your Q-function weights.
     */
    public Double[] weights;
    // A map which maps each footman to the list of rewards they create
    public Map<Integer, List<Double>> footmenRewards = new HashMap<Integer, List<Double>>();

    /**
     * These variables are set for you according to the assignment definition. You can change them,
     * but it is not recommended. If you do change them please let us know and explain your reasoning for
     * changing them.
     */
    public final double gamma = 0.9;
    public final double learningRate = .0001;
    public final double epsilon = .02;

    public RLAgent(int playernum, String[] args) {
        super(playernum);

        if (args.length >= 1) {
            numEpisodes = Integer.parseInt(args[0]);
            System.out.println("Running " + numEpisodes + " episodes.");
        } else {
            numEpisodes = 10;
            System.out.println("Warning! Number of episodes not specified. Defaulting to 10 episodes.");
        }

        boolean loadWeights = false;
        if (args.length >= 2) {
            loadWeights = Boolean.parseBoolean(args[1]);
        } else {
            System.out.println("Warning! Load weights argument not specified. Defaulting to not loading.");
        }

        if (loadWeights) {
            weights = loadWeights();
        } else {
            // initialize weights to random values between -1 and 1
            weights = new Double[NUM_FEATURES];
            for (int i = 0; i < weights.length; i++) {
                weights[i] = random.nextDouble() * 2 - 1;
            }
        }
    }

    /**
     * We've implemented some setup code for your convenience. Change what you need to.
     */
    @Override
    public Map<Integer, Action> initialStep(State.StateView stateView, History.HistoryView historyView) {

        // You will need to add code to check if you are in a testing or learning episode

        // Find all of your units
        myFootmen = new LinkedList<>();
        for (Integer unitId : stateView.getUnitIds(playernum)) {
            Unit.UnitView unit = stateView.getUnit(unitId);

            String unitName = unit.getTemplateView().getName().toLowerCase();
            if (unitName.equals("footman")) {
                myFootmen.add(unitId);
            } else {
                System.err.println("Unknown unit type: " + unitName);
            }
        }

        // Find all of the enemy units
        enemyFootmen = new LinkedList<>();
        for (Integer unitId : stateView.getUnitIds(ENEMY_PLAYERNUM)) {
            Unit.UnitView unit = stateView.getUnit(unitId);

            String unitName = unit.getTemplateView().getName().toLowerCase();
            if (unitName.equals("footman")) {
                enemyFootmen.add(unitId);
            } else {
                System.err.println("Unknown unit type: " + unitName);
            }
        }
        // Initialize the reward maps
        initializeRewardsMap();
        return middleStep(stateView, historyView);
    }
    
    /**
     * Initialize the reward maps for each footman and the list of rewards they create
     */
    private void initializeRewardsMap() {
		for(Integer footmenID : myFootmen){
			List<Double> rewards = new ArrayList<Double>();
			footmenRewards.put(footmenID, rewards);
		}
	}

    /**
     * You will need to calculate the reward at each step and update your totals. You will also need to
     * check if an event has occurred. If it has then you will need to update your weights and select a new action.
     *
     * Some useful API calls here are:
	 *
     * If you are using the footmen vectors you will also need to remove killed enemies and your units which being killed. To do so use the historyView
     * to get a DeathLog. Each DeathLog tells you which player's unit died and the unit ID of the dead unit. To get
     * the deaths from the last turn do something similar to the following snippet. Please be aware that on the first
     * turn you should not call this as you will get nothing back.
     *
     ** 
     *for(DeathLog deathLog : historyView.getDeathLogs(stateView.getTurnNumber() -1)) {
     *     System.out.println("Player: " + deathLog.getController() + " unit: " + deathLog.getDeadUnitID());
     * }
     **
     * You should also check for completed actions using the history view. Obviously you never want a footman just
     * sitting around doing nothing (the enemy certainly isn't going to stop attacking). So at the minimum you will
     * have an event whenever one your footmen's targets is killed or an action fails. Actions may fail if the target
     * is surrounded or the unit cannot find a path to the unit. To get the action results from the previous turn
     * you can do something similar to the following. Please be aware that on the first turn you should not call this
     **
     * Map<Integer, ActionResult> actionResults = historyView.getCommandFeedback(playernum, stateView.getTurnNumber() - 1);
     * for(ActionResult result : actionResults.values()) {
     *     System.out.println(result.toString());
     * }
     **
     *
     * Remember that you can use result.getFeedback() on an ActionResult, and compare the result to an ActionFeedback enum.
     * Useful ActionFeedback values include COMPLETED, FAILED, and INCOMPLETE.
     * 
     * You can also get the ID of the unit executing an action from an ActionResult. For example,
     * result.getAction().getUnitID()
     * 
     * For this assignment it will be most useful to create compound attack actions. These will move your unit
     * within range of the enemy and then attack them once. You can create one using the static method in Action:
     * Action.createCompoundAttack(attackerID, targetID)
     * 
     * You will then need to add the actions you create to a Map that will be returned. This creates a mapping
     * between the ID of the unit performing the action and the Action object.
     * 
     * @return New actions to execute or nothing if an event has not occurred.
     */
    @Override
    public Map<Integer, Action> middleStep(State.StateView stateView, History.HistoryView historyView) {
    	// Reset the death points flat
    	awardedDeathPoints = false;
    	// Count the number of previous turns
		int previousTurnCount = stateView.getTurnNumber() - 1;
		// Create a map for new actions to execute
		Map<Integer, Action> marchingOrders = new HashMap<>();
		// If this is the first turn then create actions to execute
		if(previousTurnCount < 0){
			return createActions(stateView, historyView);
		}
		// If this is not the first turn then finish current turn then create actions to execute
		finishTurn(stateView, historyView, previousTurnCount);
		marchingOrders = createActions(stateView, historyView);
		// Update previous state view and return actions to execute
		previousStateView = stateView;
		return marchingOrders;
    }
    
    /**
	 * Create a map that maps each unit to an action to execute
	 * @param stateView
	 * @param historyView
	 * @return map of actions
	 */
	private Map<Integer, Action> createActions(State.StateView stateView, History.HistoryView historyView) {
		// Create the map of actions
		Map<Integer, Action> marchingOrders = new HashMap<Integer, Action>();
		// Check each ally footman
		for(Integer allyID : myFootmen){
			// Select the enemy this footman should attack, create action and add to the map of actions
			int enemyID = selectAction(stateView, historyView, allyID);
			Action action = Action.createCompoundAttack(allyID, enemyID);
			marchingOrders.put(allyID, action);
		}
		return marchingOrders;
	}

    /**
     *
     * Here you will calculate the cumulative average rewards for your testing episodes. If you have just
     * finished a set of test episodes you will call out testEpisode.
     *
     * It is also a good idea to save your weights with the saveWeights function.
     */
    @Override
    public void terminalStep(State.StateView stateView, History.HistoryView historyView) {
    	// Finish the last turn and update the episode counts
    	finishTurn(stateView, historyView, stateView.getTurnNumber() - 1); 
		currentPhaseEpisodeCount++;
		// If the turn is still in evaluation and the number of episodes count for one evaluation is reached
		if(inEvaluation){
			if(currentPhaseEpisodeCount == NUM_EVALUATING_EPISODES){
				// Calculate average reward for the last 10 learning episodes and reset their total reward to 0
				averageRewards.add(totalReward/NUM_EVALUATING_EPISODES);
				totalReward = 0.0;
				// Print test data, reset episode counts and evaluation flag for the next phase
				printTestData(averageRewards);
				currentPhaseEpisodeCount = 0;
				inEvaluation = false;
			}
		} else if(currentPhaseEpisodeCount == NUM_LEARNING_EPISODES){
			inEvaluation = true;
			currentPhaseEpisodeCount = 0;
			totalReward = 0.0;
		}
		// Save weights and check if the numbered of total required learning episode is reached
		saveWeights(weights);
		currentEpisode++;
		if(currentEpisode > numEpisodes){
			System.out.println("Finish All Episodes.");
			System.exit(0);
		}
    }
    
    /**
	 * 
	 * Remove dead agents the lists and update the rewards.
	 * Calculate reward for remaining agents and updated the weights.
	 * Update weights and store reward for each footman 
	 * 
	 * @param stateView
	 * @param historyView
	 * @param previousTurnCount
	 */
	private void finishTurn(State.StateView stateView, History.HistoryView historyView, int previousTurnCount) {
		// Create a map that maps each footman to their corresponding action result
		Map<Integer, ActionResult> commandsIssued = historyView.getCommandFeedback(playernum, previousTurnCount);
		// Create a list of death logs and check each death log
		List<DeathLog> deathLogs = historyView.getDeathLogs(previousTurnCount);
		for(DeathLog deathLog : deathLogs){
			// Remove the enemy footman from enemies list if the an enemy dies
			if(deathLog.getController() == ENEMY_PLAYERNUM){
				enemyFootmen.remove(((Integer) deathLog.getDeadUnitID()));
			} else {
				// Else remove the ally footmen from ally lists
				myFootmen.remove(((Integer) deathLog.getDeadUnitID()));
				// Calculate reward of the last turn, update total reward and the current rewards
				double reward = calculateReward(stateView, historyView, deathLog.getDeadUnitID());
				totalReward = totalReward + reward;
				List<Double> rewards = footmenRewards.get(deathLog.getDeadUnitID());
				rewards.add(reward);
			}
		}
		
		// Check each ally footman
		for(Integer allyID : myFootmen){
			// Calculate reward of the last turn, update total reward, 
			double reward = calculateReward(stateView, historyView, allyID);
			totalReward = totalReward + reward;
			// Get ID of the enemy attacked by ally footman
			int enemyID = ((TargetedAction) commandsIssued.get(allyID).getAction()).getTargetId();
			// Update the current rewards and calculate discounted reward for this footman during this epidsode
			List<Double> rewards = footmenRewards.get(allyID);
			rewards.add(reward);
			double cumulativeDiscountedReward = getCumulativeDiscountedRewardForOneFootman(allyID);
			// Update the weight if this episode is not in evaluation
			if(!inEvaluation){
				weights = primitiveToObjectDouble(updateWeights(	
								objectToPrimitiveDouble(weights), 
								calculateFeatureVector(previousStateView, historyView, allyID, enemyID),
								cumulativeDiscountedReward, stateView, historyView, allyID)
				);
			}
		}
	}
	
	/**
	 * Since the Q-function weights are a list of Double values (object type) and the arithmetic calculation of weight
	 * uses double values (primitive type), the following two methods simply convert the type of Q-function weights
	 * back and forth between primitive and object types to use data for calculation.
	 */
	
	public Double[] primitiveToObjectDouble(double[] array){
		Double[] result = new Double[array.length];
		for(int i = 0; i < array.length; i++){
			result[i] = array[i];
		}
		return result;
	}
	
	public double[] objectToPrimitiveDouble(Double[] array){
		double[] result = new double[array.length];
		for(int i = 0; i < array.length; i++){
			result[i] = array[i];
		}
		return result;
	}
	
	/**
	 * Using the rewards stored at every step of a footman to calculate the discounted reward.
	 * 
	 * @param allyID ally footman ID
	 * @return discountedReward discounted reward of this footman during this entire episode
	 */
	private double getCumulativeDiscountedRewardForOneFootman(Integer allyID) {
		// Create a list of rewards for each step, the discounted reward and the discount rate
		List<Double> rewards = footmenRewards.get(allyID);
		double discountedReward = 0;
		// Update the discounted reward for each step
		for(int i = rewards.size() - 1; i >= 0; i--){
			discountedReward = discountedReward + gamma*rewards.get(i);
		}
		return discountedReward;
	}

    /**
     * Calculate the updated weights for this agent. 
     * @param oldWeights Weights prior to update
     * @param oldFeatures Features from (s,a)
     * @param totalReward Cumulative discounted reward for this footman.
     * @param stateView Current state of the game.
     * @param historyView History of the game up until this point
     * @param footmanId The footman we are updating the weights for
     * @return The updated weight vector.
     */
    public double[] updateWeights(double[] oldWeights, double[] oldFeatures, double totalReward, State.StateView stateView, History.HistoryView historyView, int footmanId) {
    	// Create the weight list corresponding to each feature
    	double[] newWeights = new double[NUM_FEATURES];
    	// Get the enemy ID for the attack move of this footman that brings the best Q-value
		int toAttack = getArgMaxForQ(stateView, historyView, footmanId);
		// Calculate the best Q-value for this attack mmove
		double maxQValue = calcQValue(stateView, historyView, footmanId, toAttack);
		// Calculate the hitherto Q-value up until now
		double previousQValue = calcQValueGivenFeatures(oldFeatures);
		// Create the feature vector and update the new weights for each feature
		double[] features = calculateFeatureVector(stateView, historyView, footmanId, toAttack);
		for(int i = 0; i < NUM_FEATURES; i++){
			newWeights[i] = oldWeights[i] + learningRate * (totalReward + (gamma * maxQValue) - previousQValue) * features[i];
		}
		return newWeights;
    }
    
    
    

    /**
     * Given a footman and the current state and history of the game select the enemy that this unit should
     * attack. This is where you would do the epsilon-greedy action selection.
     *
     * @param stateView Current state of the game
     * @param historyView The entire history of this episode
     * @param attackerId The footman that will be attacking
     * @return The enemy footman ID this unit should attack
     */
    public int selectAction(State.StateView stateView, History.HistoryView historyView, int attackerId) {
    	// Create the probability value
    	Double decider = random.nextDouble();
    	// With probability (1-epsilon), attack the enemy which yields the best Q-value
		if(decider < 1 - epsilon || inEvaluation){
			return getArgMaxForQ(stateView, historyView, attackerId);
		} else { // attack another enemy with probability epsilon
			return enemyFootmen.get(random.nextInt(enemyFootmen.size()));
		}
    }

    /**
     * Given the current state and the footman in question calculate the reward received on the last turn.
     * This is where you will check for things like Did this footman take or give damage? Did this footman die
     * or kill its enemy. Did this footman start an action on the last turn? See the assignment description
     * for the full list of rewards.
     *
     * Remember that you will need to discount this reward based on the timestep it is received on. See
     * the assignment description for more details.
     *
     * As part of the reward you will need to calculate if any of the units have taken damage. You can use
     * the history view to get a list of damages dealt in the previous turn. Use something like the following.
     *
     * for(DamageLog damageLogs : historyView.getDamageLogs(lastTurnNumber)) {
     *     System.out.println("Defending player: " + damageLog.getDefenderController() + " defending unit: " + \
     *     damageLog.getDefenderID() + " attacking player: " + damageLog.getAttackerController() + \
     *     "attacking unit: " + damageLog.getAttackerID() + "damage: " + damageLog.getDamage());
     * }
     *
     * You will do something similar for the deaths. See the middle step documentation for a snippet
     * showing how to use the deathLogs.
     *
     * To see if a command was issued you can check the commands issued log.
     *
     * Map<Integer, Action> commandsIssued = historyView.getCommandsIssued(playernum, lastTurnNumber);
     * for (Map.Entry<Integer, Action> commandEntry : commandsIssued.entrySet()) {
     *     System.out.println("Unit " + commandEntry.getKey() + " was command to " + commandEntry.getValue().toString);
     * }
     *
     * @param stateView The current state of the game.
     * @param historyView History of the episode up until this turn.
     * @param footmanId The footman ID you are looking for the reward from.
     * @return The current reward
     */
    public double calculateReward(State.StateView stateView, History.HistoryView historyView, int footmanId) {
    	// Create variables for reward the count of previous turns
    	double reward = -0.1;
		int previousTurnCount = stateView.getTurnNumber() - 1;

		// Check the damage log of the last turn
		for(DamageLog damageLog : historyView.getDamageLogs(previousTurnCount)) {
			// Increase reward if this footman attacked enemy
			if(damageLog.getAttackerController() == playernum && damageLog.getAttackerID() == footmanId){
				reward = reward + damageLog.getDamage();
			} else if(damageLog.getAttackerController() == ENEMY_PLAYERNUM && damageLog.getDefenderID() == footmanId){
				// Decrease reward if this footman is attacked by enemy
				reward = reward - damageLog.getDamage();
			}
		}
		// Check the damage log of the last turn
		for(DeathLog deathLog : historyView.getDeathLogs(previousTurnCount)){
			if(deathLog.getController() == ENEMY_PLAYERNUM && footmanWasAttackingDeadEnemy(footmanId, deathLog, historyView, previousTurnCount)){
				if(!awardedDeathPoints){
					// Add 100 to reward and change the enemy death flag if the enemy was killed
					reward = reward + 100;
					awardedDeathPoints = true;
				}
			} else if(deathLog.getDeadUnitID() == footmanId) {
				// Decrease 100 from reward if the ally footman was killed
				reward = reward - 100;
			}
		}
		return reward;
    }
    
    /**
	 * Check if a particular ally footman killed the enemy
	 * @param footmanId ID of the ally footman to check
	 * @param deathLog death log of the state to check
	 * @param historyView history of the episode up until this turn
	 * @param previousTurnCount the count of turns in this episode
	 * @return true this ally footman killed the enemy in the previous turn
	 */
	private boolean footmanWasAttackingDeadEnemy(int footmanId, DeathLog deathLog, History.HistoryView historyView, int previousTurnCount) {
		// Create a map that map each ally footman to the result of their actions in the previous turn
		Map<Integer, ActionResult> actionResults = historyView.getCommandFeedback(playernum, previousTurnCount);
		// If the footman finished an action in the last turn
		if(actionResults.containsKey(footmanId) && actionResults.get(footmanId).getFeedback().equals(ActionFeedback.COMPLETED)){
			// Check if this action is a kill on enemy
			TargetedAction targetedAction = (TargetedAction) actionResults.get(footmanId).getAction() ;
			return targetedAction.getTargetId() == deathLog.getDeadUnitID();
		}
		return false;
	}

    /**
     * Calculate the Q-Value for a given state action pair. The state in this scenario is the current
     * state view and the history of this episode. The action is the attacker and the enemy pair for the
     * SEPIA attack action.
     *
     * This returns the Q-value according to your feature approximation. This is where you will calculate
     * your features and multiply them by your current weights to get the approximate Q-value.
     *
     * @param stateView Current SEPIA state
     * @param historyView Episode history up to this point in the game
     * @param attackerId ally attacking footman
     * @param defenderId An enemy footman that your footman would be attacking
     * @return The approximate Q-value
     */
    public double calcQValue(State.StateView stateView, History.HistoryView historyView, int attackerId, int defenderId) {
    	// Calculate the feature vectors for the given state and attack move
    	double[] featureValues = calculateFeatureVector(stateView, historyView, attackerId, defenderId);
    	// Calculate the Q-value for this feature vector
		return calcQValueGivenFeatures(featureValues);
    }
    
    /**
     * Get the ID of the enemy to attack that maximize the Q-value of a given state
     * @param stateView Current SEPIA state
     * @param historyView Episode history up to this point in the game
     * @param attackerId ally attacking footman
     * @return ID of enemy to attack
     */

	private int getArgMaxForQ(State.StateView stateView, History.HistoryView historyView, int attackerId) {
		// Create the variables for enemy ID and max Q-value
		int toAttackId = -1;
		double max = Double.NEGATIVE_INFINITY;
		// Calculate the Q-value for the attack on each enemy and replace max Q-value if possible
		for(Integer enemyId : enemyFootmen){
			double possible = calcQValue(stateView, historyView, attackerId, enemyId);
			if(possible > max){
				max = possible;
				toAttackId = enemyId;  
			}
		}    	
		return toAttackId;
	}
	
	/**
	 * Calculate the Q-value given a feature vector
	 * @param featureValues feature vector
	 * @return the Q-value
	 */
	private double calcQValueGivenFeatures(double[] featureValues) { 
		double qValue = 0;
		// Sum over the Q-value of each feature to get the Q-value of the feature vector
		for(int i = 0; i < NUM_FEATURES; i++){
			qValue = qValue + weights[i] * featureValues[i];
		}
		return qValue;
	}
	
	// BELOW ARE METHODS FOR CALCULATIONS WHICH ARE RELATED TO FEATURES

    /**
     * Given a state and action calculate your features here. Please include a comment explaining what features
     * you chose and why you chose them.
     *
     * for example: HP 
     * UnitView attacker = stateView.getUnit(attackerId);
     * attacker.getHP()
     * 
     * All of your feature functions should evaluate to a double. Collect all of these into an array. You will
     * take a dot product of this array with the weights array to get a Q-value for a given state action.
     *
     * It is a good idea to make the first value in your array a constant. This just helps remove any offset
     * from 0 in the Q-function. The other features are up to you. Many are suggested in the assignment
     * description.
     *
     * @param stateView Current state of the SEPIA game
     * @param historyView History of the game up until this turn
     * @param attackerId Your footman. The one doing the attacking.
     * @param defenderId An enemy footman. The one you are considering attacking.
     * @return The array of feature function outputs.
     */
	
	/**
	 * I calculate in total 8 features.
	 * 
	 * Through empirical testing, I find that 3 features which give the best performance are the constant feature of 1,
	 * the closest enemy check, and the check to see if the ally footman attacked this enemy in the last turn.
	 * 
	 * Other features I calculated but not include in the feature vector:
	 * check if this enemy previously attacked this ally footman, number of enemies that can attack this ally footman,
	 * the health of this ally footman and this enemy footman, and number of other ally footmen attacked this enemy in
	 * the last turn.
	 * 
	 * I choose the closest enemy and the check see if the ally footman attacked this enemy in the last turn to prioritize
	 * a potential kill move since the closest enemy which was previously attacked may take the the least moves to kill.
	 * 
	 * I do not delete any features even though only 3 are used because I think some features would be useful 
	 * for improving the agent's performance under the different scenarios with other seeds.
	 */
    public double[] calculateFeatureVector(State.StateView stateView, History.HistoryView historyView, int attackerId, 
    		int defenderId) {
    	// Array for values of each feature
    	double[] featureValues = new double[NUM_FEATURES];
    	// Calculate the values of all features
		for(int i = 0; i < NUM_FEATURES; i++){
			featureValues[i] = calculateFeatureValue(i, stateView, historyView, attackerId, defenderId);
		}
		return featureValues;
    }
    
    /**
     *  Calculate the value of each feature by calling the corresponding method for each one
     * @param featureID the ID of the feature to calculate
     * @param stateView Current state of the SEPIA game
     * @param historyView History of the game up until this turn
     * @param allyID ID of ally footman
     * @param enemyID ID of enemy footman
     * @return the value of a particular feature
     */
    private double calculateFeatureValue(int featureID, State.StateView stateView, History.HistoryView historyView,
    		int allyID, int enemyID){
		double result = 0;
		switch(featureID){
		// First value in array value - a constant
		case 0:
			result = 1;
			break;
		// If this was the closest enemy
		case 1:
			result = featureNearestEnemy(stateView, historyView, allyID, enemyID);
			break;
		// If this was the enemy previously attacked by this footman
		case 2:
			result = featurePreviouslyAttacking(stateView, historyView, allyID, enemyID);
			break;
		// If this was the enemy previously attacked the ally footman
		case 3:
			result = featureEnemyAttacking(stateView, historyView, allyID, enemyID);
			break;
		// The number of enemies that can attack this ally footman
		case 4:
			result = featureEnemiesThatCanAttackAnAlly(stateView, historyView, allyID);
			break;
		// The health of the ally footman
		case 5:
			result = featureAllyHealth(stateView, allyID);
			break;
		// The health of the enemy footman
		case 6:
			result = featureEnemyHealth(stateView, enemyID);
			break;
		// Number of ally footmen that attacked this particular enemy in last turn
		case 7:
			result = featureAllyAttackerCount(stateView, historyView, enemyID);
			break;
		}
		return result;
	}
	
	/**
	 *  Check if the an enemy footman is the closest enemy to an ally footman
	 * @param stateView Current state of the SEPIA game
     * @param historyView History of the game up until this turn
	 * @param allyID ID of ally footman
	 * @param enemyID ID of enemy footman
	 * @return true if this enemy is the closest one, false otherwise
	 */
    private double featureNearestEnemy(StateView stateView, HistoryView historyView, int allyID, int enemyID) {
		int closestEnemy = getClosestEnemy(stateView, historyView, allyID);
		if(closestEnemy == enemyID){
			return 1;
		}
		return 0;
	}
	
	/**
	 * Find the closest enemy to an ally footman
	 * @param stateView Current state of the SEPIA game
     * @param historyView History of the game up until this turn
	 * @param allyID ID of ally footman
	 * @return ID of the closest enemy
	 */
    private int getClosestEnemy(StateView stateView, HistoryView historyView, int allyID) {
    	// Create variables for the ID of closest enemy and closest distance
		int closestEnemyID = -1;
		double closestDistance = Double.POSITIVE_INFINITY;
		UnitView ally= stateView.getUnit(allyID);
		// If there is no ally attacker
		if(ally == null){
			// If there is no enemy
			if(enemyFootmen.isEmpty()){
				return -1;
			} else {
				return enemyFootmen.get(0);
			}
		}
		// Get the X and Y positions of the ally footman
		int allyXPosition = ally.getXPosition();
		int allyYPosition = ally.getYPosition();
		// Check each enemy
		for(Integer enemyID : enemyFootmen){
			UnitView enemy = stateView.getUnit(enemyID);
			if(enemy != null){
				// Get the X and Y positions of the enemy footman
				int enemyXPosition = enemy.getXPosition();
				int enemyYPosition = enemy.getYPosition();
				// Calculate the distance between two footmen and update closest distance, closest enemy ID if possible
				double distance = getDistance(allyXPosition, enemyXPosition, allyYPosition, enemyYPosition);
				if(distance < closestDistance){
					closestDistance = distance;
					closestEnemyID = enemyID;
				}
			}
		}
		return closestEnemyID;
	}
	
	
    /**
     * Calculate the Chebyshev distance between two footmen
     * @param attackerXPosition X position of attacking footman
     * @param defenderXPosition X position of attacked footman
     * @param attackerYPosition Y position of attacking footman
     * @param defenderYPosition Y position of attacked footman
     * @return Chebyshev distance
     */
    private double getDistance(int attackerXPosition, int defenderXPosition, int attackerYPosition, int defenderYPosition){
		return Math.max(Math.abs(attackerXPosition - defenderXPosition), Math.abs(attackerYPosition - defenderYPosition));
	}
	
	/**
	 * Check if an ally footman attacked a particular enemy in the previous turn
	 * @param stateView Current state of the SEPIA game
     * @param historyView History of the game up until this turn
	 * @param allyID ID of ally footman to check
	 * @param enemyID ID of enemy to check
	 * @return 1 if this ally footman attacked this enemy, 0 otherwise
	 */
    private double featurePreviouslyAttacking(StateView stateView, HistoryView historyView, int allyID, int enemyID) {
		// Create a map that maps each ally footman to their action in the last turn
    	Map<Integer, Action> commandsIssued = historyView.getCommandsIssued(playernum, stateView.getTurnNumber() - 1);
		// Get the action of the ally footman we want to check and check if it attacked a particular enemy footman
    	TargetedAction targetedAction = (TargetedAction) commandsIssued.get(allyID);
		if(targetedAction == null){
			return 0;
		}
		return targetedAction.getTargetId() == enemyID ? 1 : 0;
	}
	
    /**
	 * Check if an enemy footman attacked a particular all in the previous turn
	 * @param stateView Current state of the SEPIA game
     * @param historyView History of the game up until this turn
	 * @param allyID ID of ally footman to check
	 * @param enemyID ID of enemy to check
	 * @return 1 if this enemy footman attacked ally enemy, 0 otherwise
	 */
    private double featureEnemyAttacking(StateView stateView, HistoryView historyView, int allyID, int enemyID) {
		// Get the count for previous turn and return 0 if it is the first turn
    	int previousTurnNumber = stateView.getTurnNumber() - 1;
		if(previousTurnNumber < 0){
			return 0;
		}
		// Create a map that maps each ally footman to their action in the last turn
		Map<Integer, Action> commandsIssued = historyView.getCommandsIssued(ENEMY_PLAYERNUM, previousTurnNumber);
		// Check each action in the map to see if this particular enemy attacked this particular ally
		for(Action action : commandsIssued.values()){
			TargetedAction targetedAction = (TargetedAction) action;
			if(targetedAction.getTargetId() == allyID && targetedAction.getUnitId() == enemyID){
				return 1;
			}
		}
		return 0;
	}
	
	/**
	 * Count the number of enemies can attack on particular ally footman
	 * @param stateView Current state of the SEPIA game
     * @param historyView History of the game up until this turn
	 * @param allyID ID of ally footman to investigate
	 * @return the number of enemies can attack this ally footman
	 */
    private double featureEnemiesThatCanAttackAnAlly(StateView stateView, HistoryView historyView, int allyID) {
		// Create the variables to count enemies and a variable for the ally footman
    	int enemiesCount = 0;
		UnitView ally = stateView.getUnit(allyID);
		if(ally == null){
			return enemiesCount;
		}
		// Get the X and Y position of ally
		int allyXPosition = ally.getXPosition();
		int allyYPosition = ally.getYPosition();
		// Check each enemy
		for(Integer enemyID : enemyFootmen){
			UnitView enemy = stateView.getUnit(enemyID);
			if(enemy != null){
				// Get the position of the enemy and calculate the distance between that enemy and the ally
				int enemyXPosition = enemy.getXPosition();
				int enemyYPosition = enemy.getYPosition();
				if(getDistance(allyXPosition, enemyXPosition, allyYPosition, enemyYPosition) < 2){
					// Increase the enemies count if possible
					enemiesCount++;
				}
			}
		}
		return enemiesCount;
	}
	
	/**
	 * Get the health of a particular ally footman
	 * @param stateView Current state of the SEPIA game
	 * @param allyID ID of ally footman
	 * @return the health of ally footman
	 */
    private double featureAllyHealth(StateView stateView, int allyID) {
		Unit.UnitView unit = stateView.getUnit(allyID);
		if(unit == null){
			return 0;
		}
		return stateView.getUnit(allyID).getHP();
	}
	
    /**
	 * Get the health of a particular enemy footman
	 * @param stateView Current state of the SEPIA game
	 * @param enemyID ID of enemy footman
	 * @return the health of enemy footman
	 */
    private double featureEnemyHealth(StateView stateView, int enemyID) {
		UnitView enemy = stateView.getUnit(enemyID);		
		return enemy == null ? 0 : enemy.getHP();
	}
	
	/**
	 * Count the number of allies that attacked a particular enemy footman in the last turn
	 * @param stateView Current state of the SEPIA game
     * @param historyView History of the game up until this turn
	 * @param enemyID ID of enemy footman to investigate
	 * @return the number of allies attacked this enemy footman
	 */
    private double featureAllyAttackerCount(StateView stateView, HistoryView historyView, int enemyID) {
    	// Get the count for previous turn and return 0 if it is the first turn
		int previousTurnNumber = stateView.getTurnNumber() - 1;
		if(previousTurnNumber < 0){
			return 0;
		}
		// Create a map that maps each ally footman to their action in the last turn
		Map<Integer, Action> commandsIssued = historyView.getCommandsIssued(playernum, previousTurnNumber);
		// Create a variable to count allies
		int alliesCount = 0;
		// Check each action in the last turn
		for(Action action : commandsIssued.values()){
			// Check if this action targeted this particular enemy an increase allies count in that case
			TargetedAction targetedAction = (TargetedAction) action;
			if(targetedAction.getTargetId() == enemyID){
				alliesCount++;
			}
		}
		return alliesCount;
	}
    
    /**
     * DO NOT CHANGE THIS!
     *
     * Prints the learning rate data described in the assignment. Do not modify this method.
     *
     * @param averageRewards List of cumulative average rewards from test episodes.
     */
    public void printTestData (List<Double> averageRewards) {
        System.out.println("");
        System.out.println("Games Played      Average Cumulative Reward");
        System.out.println("-------------     -------------------------");
        for (int i = 0; i < averageRewards.size(); i++) {
            String gamesPlayed = Integer.toString(10*i);
            String averageReward = String.format("%.2f", averageRewards.get(i));

            int numSpaces = "-------------     ".length() - gamesPlayed.length();
            StringBuffer spaceBuffer = new StringBuffer(numSpaces);
            for (int j = 0; j < numSpaces; j++) {
                spaceBuffer.append(" ");
            }
            System.out.println(gamesPlayed + spaceBuffer.toString() + averageReward);
        }
        System.out.println("");
    }

    /**
     * DO NOT CHANGE THIS!
     *
     * This function will take your set of weights and save them to a file. Overwriting whatever file is
     * currently there. You will use this when training your agents. You will include th output of this function
     * from your trained agent with your submission.
     *
     * Look in the agent_weights folder for the output.
     *
     * @param weights Array of weights
     */
    public void saveWeights(Double[] weights) {
        File path = new File("agent_weights/weights.txt");
        // create the directories if they do not already exist
        path.getAbsoluteFile().getParentFile().mkdirs();

        try {
            // open a new file writer. Set append to false
            BufferedWriter writer = new BufferedWriter(new FileWriter(path, false));

            for (double weight : weights) {
                writer.write(String.format("%f\n", weight));
            }
            writer.flush();
            writer.close();
        } catch(IOException ex) {
            System.err.println("Failed to write weights to file. Reason: " + ex.getMessage());
        }
    }

    /**
     * DO NOT CHANGE THIS!
     *
     * This function will load the weights stored at agent_weights/weights.txt. The contents of this file
     * can be created using the saveWeights function. You will use this function if the load weights argument
     * of the agent is set to 1.
     *
     * @return The array of weights
     */
    public Double[] loadWeights() {
        File path = new File("agent_weights/weights.txt");
        if (!path.exists()) {
            System.err.println("Failed to load weights. File does not exist");
            return null;
        }

        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            String line;
            List<Double> weights = new LinkedList<>();
            while((line = reader.readLine()) != null) {
                weights.add(Double.parseDouble(line));
            }
            reader.close();

            return weights.toArray(new Double[weights.size()]);
        } catch(IOException ex) {
            System.err.println("Failed to load weights from file. Reason: " + ex.getMessage());
        }
        return null;
    }

    @Override
    public void savePlayerData(OutputStream outputStream) {

    }

    @Override
    public void loadPlayerData(InputStream inputStream) {

    }
}
