package edu.cwru.sepia.agent.minimax;

import java.util.*;

public class Helper {

	// HELPER METHODS TO SUPPORT MINIMAXALPHABETA

	/**
	 * Check if the search has reached the depth of the ply to look ahead
	 * 
	 * @param node:  the current state of the game
	 * @param depth: the depth of the ply to look ahead
	 * @return whether we have reached the end of the ply
	 */
	public static boolean endPlyCheck(GameStateChild node, int depth) {
		return depth == 0;
	}

	/**
	 * Calculate the max value for the Max step
	 * 
	 * @param node:  the current state of the game
	 * @param depth: the depth of the ply to look ahead
	 * @param alpha: the current alpha value in the alphaBetaSearch
	 * @param beta:  the current beta value in the alphaBetaSearch
	 * @return the max value for the node in the max step
	 */
	public static double maxValueCalculation(GameStateChild node, int depth, double alpha, double beta) {
		// check if we reach the end of the ply
		if (endPlyCheck(node, depth)) {
			return node.state.getUtility();
		}
		// holder for the maxValue
		double maxValue = Double.NEGATIVE_INFINITY;

		// check each child node in the list of children of the current state
		for (GameStateChild child : MinimaxAlphaBeta.orderChildrenWithHeuristics(node.state.getChildren())) {
			// compare the current max value with the min value of the child state
			// (calculate the min value of the child state by calling the
			// minValueCalculation method)
			maxValue = Math.max(maxValue, minValueCalculation(child, depth - 1, alpha, beta));
			// return max value if max value is larger than beta (pruning)
			if (maxValue >= beta) {
				return maxValue;
			}
			// append alpha if max value is larger than alpha
			alpha = Math.max(alpha, maxValue);
		}
		return maxValue;
	}

	/**
	 * Calculate the min value for the Min step
	 * 
	 * @param node:  the current state of the game
	 * @param depth: the depth of the ply to look ahead
	 * @param alpha: the current alpha value in the alphaBetaSearch
	 * @param beta:  the current beta value in the alphaBetaSearch
	 * @return the min value for the node in the min step
	 */
	public static double minValueCalculation(GameStateChild node, int depth, double alpha, double beta) {
		// check if we reach the end of the ply
		if (endPlyCheck(node, depth)) {
			return node.state.getUtility();
		}
		// holder for min value
		double minValue = Double.POSITIVE_INFINITY;

		// check each child node in the list of children of the current state
		for (GameStateChild child : MinimaxAlphaBeta.orderChildrenWithHeuristics(node.state.getChildren())) {
			// compare the current min value with the max value of the child state
			// (calculate the max value of the child state by calling the
			// maxValueCalculation method)
			minValue = Math.min(minValue, maxValueCalculation(child, depth - 1, alpha, beta));
			// return min value if min value is smaller than alpha (pruning)
			if (minValue <= alpha) {
				return minValue;
			}
			// append beta if min value is smaller than beta
			beta = Math.min(beta, minValue);
		}
		return minValue;
	}

	/*
	 * Find a specific child node with a specific utility value in the game state
	 * child list of the parent node
	 * 
	 * @param node: the parent node
	 * 
	 * @param value: utility value of the child node we want to find
	 * 
	 * @return child with the correct utility value
	 */
	public static GameStateChild findSpecificChild(GameStateChild node, double utilityValue) {
		// get the list of children nodes
		List<GameStateChild> children = node.state.getChildren();

		// if there is no children, return the current node
		if (children.isEmpty()) {
			return node;
		}

		// check each child node in the children list
		for (GameStateChild child : children) {
			// return if we find the correct node
			if (child.state.getUtility() == utilityValue) {
				return child;
			}
		}

		// Sort the children list and return the children with the best utility
		// if the children list is not empty but there is no child with the correct
		// utility value
		Collections.sort(children);
		return children.get(0);
	}
	
}
