package edu.cwru.sepia.agent.minimax;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.State;

public class MinimaxAlphaBeta extends Agent {
	
	private final int numPlys;
	
    public MinimaxAlphaBeta(int playernum, String[] args)
    {
        super(playernum);

        if(args.length < 1)
        {
            System.err.println("You must specify the number of plys");
            System.exit(1);
        }

        numPlys = Integer.parseInt(args[0]);
    }

    @Override
    public Map<Integer, Action> initialStep(State.StateView newstate, History.HistoryView statehistory) {
        return middleStep(newstate, statehistory);
    }

    @Override
    public Map<Integer, Action> middleStep(State.StateView newstate, History.HistoryView statehistory) {
        GameStateChild bestChild = alphaBetaSearch(new GameStateChild(newstate),
                numPlys,
                Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY);

        return bestChild.action;
    }

    @Override
    public void terminalStep(State.StateView newstate, History.HistoryView statehistory) {

    }

    @Override
    public void savePlayerData(OutputStream os) {

    }

    @Override
    public void loadPlayerData(InputStream is) {

    }

    /**
     * Search with Alpha Beta pruning
     *
     * @param node The action and state to search from
     * @param depth The remaining number of plys under this node
     * @param alpha The current best value for the maximizing node from this node to the root
     * @param beta The current best value for the minimizing node from this node to the root
     * @return The best child of this node with updated values
     */
    public GameStateChild alphaBetaSearch(GameStateChild node, int depth, double alpha, double beta){
    	// Calculate the max value of for the current state
        double maxValue = Helper.maxValueCalculation(node, depth, alpha, beta);
        // Find and return the correct child node with that max value
        return Helper.findSpecificChild(node, maxValue);
    }

	/**
	 * Heuristic: a footman is better off if attacking so if both footmen are attacking that
	 * state is first in the list ones in which only one footman is attacking are next
	 * and the states where footmen are simply moving are last
	 * 
	 * Initially an idea was to expand the nodes that move the footman towards an archer first
	 * and other moves later however I ran into a problem with that when obstacles were involved
	 * and had better luck with just using attacks as the heuristic.
	 * 
	 * @param children list of possible next GameStateChild
	 * @return list of GameStateChild in order by which should be expanded first by alpha beta search
	 */
    public static List<GameStateChild> orderChildrenWithHeuristics(List<GameStateChild> children){
    	// List of children nodes where a game unit can attack
        List<GameStateChild> attackMoves = new LinkedList<GameStateChild>();
        // List of children nodes where a game unit cannot attack
        List<GameStateChild> nonAttackMoves = new LinkedList<GameStateChild>();
        
        // check each child node in the children list
        for(GameStateChild child : children){
        	// variable to count the number of attack action
        	int numAttacks = 0;
        	// check each action of a child
        	for(Action action : child.action.values()){
        		if(action.getType().name().equals(GameState.ACTION_ATTACK_NAME)){
        			// increment attack count if the action is an attack action
        			numAttacks++;
        		}
        	}
        	
        	// if all the actions of the child node are attack actions
        	// (add to the lists in different order so the sort steps below can have better runtime)
        	if(numAttacks == child.action.size())
        	{
        		// add the child node to the front of the list of nodes where attack is possible 
        		// (prioritize based on number of attack actions)
        		attackMoves.add(0, child);
        	} // if the attack actions list is empty and there exists attack moves
        	else if (numAttacks > 0 && attackMoves.isEmpty())
        	{	
        		// add the child node to the front of the list of nodes where attack is possible 
        		attackMoves.add(0, child);
        	} // if there exists attack moves  
        	else if (numAttacks > 0 && !attackMoves.isEmpty()) 
        	{
        		// add the child node behind nodes whose all actions are attack actions 
        		attackMoves.add(1, child);	
        	} // if there is no attack moves 
        	else 
        	{
        		// add the child node to the list of where attack is impossible
        		nonAttackMoves.add(child);
        	}
        }
        
        // sort the list of nodes with attack actions based on utility values
        Collections.sort(attackMoves);
        // sort the list of nodes with no attack actions based on utility values
        Collections.sort(nonAttackMoves);
        // add the non-attack list to the end of the attack list
        // (prioritize attack actions)
        //attackMoves.addAll(nonAttackMoves);
        // return the list of ordered children nodes
        //Collections.sort(attackMoves);
        /*List<GameStateChild> newList = Stream.concat(attackMoves.stream(), nonAttackMoves.stream())
                .collect(Collectors.toList());
        return newList;*/
        if (attackMoves.isEmpty())
        	return nonAttackMoves;
        return attackMoves;
    }

}