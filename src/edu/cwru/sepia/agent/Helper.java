package edu.cwru.sepia.agent;

import java.util.Set;

import edu.cwru.sepia.agent.AstarAgent.MapLocation;

public class Helper {
	// Below are 2 helpers methods to use in support of AstarSearch

	/**
	 * Method to calculate heuristic Chebyshev distance
	 * 
	 * @param currentPosition the current postion on map
	 * @param goal the position of the tow
	 * @return the heuristic Chebyshev distance
	 */
	public static float heuristicCalculation(MapLocation currentPosition, MapLocation goal) {
		return (float) Math.max(Math.abs(goal.x - currentPosition.x), Math.abs(goal.y - currentPosition.y));
	}

	/** Method to test if a position is valid to move to
	 * 
	 * @param currentPosition the current position on map 
	 * @param destination target to move to
	 * @param xExtent width of map
	 * @param yExtent length of map
	 * @param resourceLocations locations of resources
	 * @return whether we can move to this position
	 */
	public static boolean isPositionValid(MapLocation currentPosition, MapLocation destination,
			int xExtent, int yExtent, Set<MapLocation> resourceLocations) {
    	// Tests grid bounds to determine if the next location is within the grid and 
		// not the same as the current location
    	boolean valid = (destination.x >= 0) && (destination.y >= 0) 
    			&& (destination.x < xExtent) && (destination.y < yExtent)
    			&& ((currentPosition.x != destination.x) || (currentPosition.y != destination.y));
    	if (resourceLocations.contains(destination)){
    		return false;
    	}
    	return valid;
    }

}
