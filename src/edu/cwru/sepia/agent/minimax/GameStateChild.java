package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.environment.model.state.State;

import java.util.Map;

/**
 * Do not change this class.
 */

/*
 * Implements Comparable to compare the utility of each child state.
 * Use later in MinimaxAlphaBeta in orderChildrenWithHeuristics method to sort the states based on utility.
 */
public class GameStateChild implements Comparable<GameStateChild> {
    //* This is set of unit actions that produced the game state
    public Map<Integer, Action> action;
    //* This is the game state resulting from the specified set of actions
    public GameState state;

    public GameStateChild(State.StateView state)
    {
        action = null;
        this.state = new GameState(state);
    }

    public GameStateChild(Map<Integer, Action> action, GameState state)
    {
        this.action = action;
        this.state = state;
    }

	@Override
	public int compareTo(GameStateChild gsc) 
	{
		if (this.state.getUtility() > gsc.state.getUtility()){
    		return -1;
    	} else if (this.state.getUtility() < gsc.state.getUtility()){
    		return 1;
    	} else {
    		return 0;
    	}
	}
}
