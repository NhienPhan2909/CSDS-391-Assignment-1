package edu.cwru.sepia.agent;

import edu.cwru.sepia.agent.AstarAgent.MapLocation;

public class Helper {
	// Below are 4 helpers methods to use in support of AstarSearch

	/**
	 * Method to calculate heuristic Chebyshev distance
	 * 
	 * @param current_position
	 * @param goal
	 * @return the heuristic Chebyshev distance
	 */
	public static float heuristicCalculation(MapLocation current_position, MapLocation goal) {
		return (float) Math.max(Math.abs(goal.x - current_position.x), Math.abs(goal.y - current_position.y));
	}

	/**
	 * Method to check if the location moved to is empty (do not have enemy/resource
	 * there)
	 * 
	 * @param destination
	 * @param enemy_position
	 * @param resource_existence --> true if there is a resource at that location,
	 *                           false otherwise
	 * @return true if the destination is empty, false otherwise
	 */
	public static boolean isPositionEmpty(MapLocation destination, MapLocation enemy_position,
			boolean[][] resource_existence) {
		return (destination != enemy_position) && (!resource_existence[destination.x][destination.y]);
	}

	/**
	 * Method to check if the location moved to is in the map
	 * 
	 * @param destination
	 * @param xExtent
	 * @param yExtent
	 * @return true if destination is in the map, false otherwise
	 */
	public static boolean isPositionValid(MapLocation destination, int xExtent, int yExtent) {
		return (destination.x >= 0 && destination.x <= xExtent) && (destination.y >= 0 && destination.y <= yExtent);
	}

	/**
	 * Method to check if the current position is the same as the location intent to
	 * move to
	 * 
	 * @param current_position
	 * @param destination
	 * @return true if it is the same position, false otherwise
	 */
	public static boolean isSamePosition(MapLocation current_position, MapLocation destination) {
		return (current_position.x == destination.x) && (current_position.y == destination.y);
	}

}
