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
        	if (child.state.canAttack) {
        		attackMoves.add(child);
        	}
        	else
        		nonAttackMoves.add(child);
        }
        
        // sort the list of nodes with attack actions based on utility values
        Collections.sort(attackMoves);
        //System.out.println(attackMoves.isEmpty());
        // sort the list of nodes with no attack actions based on utility values
        Collections.sort(nonAttackMoves);
        // add the non-attack list to the end of the attack list
        // (prioritize attack actions)
        attackMoves.addAll(nonAttackMoves);
        // return the list of ordered children nodes
        //Collections.sort(attackMoves);
        /*List<GameStateChild> newList = Stream.concat(attackMoves.stream(), nonAttackMoves.stream())
                .collect(Collectors.toList());
        Collections.sort(newList);
        return newList;*/
        //if (attackMoves.isEmpty())
        	//return nonAttackMoves;
        //attackMoves.addAll(nonAttackMoves);
        return attackMoves;
    }

}