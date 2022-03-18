package edu.cwru.sepia.agent;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.util.Direction;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public class AstarAgent extends Agent {

    public static class MapLocation implements Comparable<MapLocation>
    {
        public int x, y;
        // Variable to determine the previous position
        public MapLocation cameFrom;
        // Variable to hold the heuristic value of a node
        public float heuristic;
        // Variable to hold the cost of a node
        public float cost;

        public MapLocation(int x, int y, MapLocation cameFrom, float cost)
        {
            this.x = x;
            this.y = y;
        }
        
        /**
         * Method to check if another position is the same as the current position
         */
        @Override
        public boolean equals(Object o) {
        	if (o instanceof MapLocation) {
        		MapLocation temp = (MapLocation)o;
        		return temp.x == this.x && temp.y == this.y;
        	}
        	else {
        		return false;
        	}
        }
        
        @Override
        public int hashCode (){
        	return 31*x+y;
        }
        
        /**
         * The method to compare the f(n) = g(n) + h(n) values of two positions
         */
        @Override
    	public int compareTo(MapLocation loc) {
    		
    		// Determine the estimated cost for each node
    		double cost = heuristic + this.cost;
    		double costToCompare = loc.heuristic + loc.cost;
    		
    		if (cost > costToCompare) {
    			return -1;
    		}
    		else if (cost == costToCompare) {
    			return 0;
    		}
    		else {
    			return 1;
    		}
    	}
    }

    Stack<MapLocation> path;
    int footmanID, townhallID, enemyFootmanID;
    MapLocation nextLoc;

    private long totalPlanTime = 0; // nsecs
    private long totalExecutionTime = 0; //nsecs

    public AstarAgent(int playernum)
    {
        super(playernum);

        //System.out.println("Constructed AstarAgent");
    }  

    @Override
    public Map<Integer, Action> initialStep(State.StateView newstate, History.HistoryView statehistory) {
        // get the footman location
        List<Integer> unitIDs = newstate.getUnitIds(playernum);

        if(unitIDs.size() == 0)
        {
            System.err.println("No units found!");
            return null;
        }

        footmanID = unitIDs.get(0);

        // double check that this is a footman
        if(!newstate.getUnit(footmanID).getTemplateView().getName().equals("Footman"))
        {
            System.err.println("Footman unit not found");
            return null;
        }

        // find the enemy playernum
        Integer[] playerNums = newstate.getPlayerNumbers();
        int enemyPlayerNum = -1;
        for(Integer playerNum : playerNums)
        {
            if(playerNum != playernum) {
                enemyPlayerNum = playerNum;
                break;
            }
        }

        if(enemyPlayerNum == -1)
        {
            System.err.println("Failed to get enemy playernumber");
            return null;
        }

        // find the townhall ID
        List<Integer> enemyUnitIDs = newstate.getUnitIds(enemyPlayerNum);

        if(enemyUnitIDs.size() == 0)
        {
            System.err.println("Failed to find enemy units");
            return null;
        }

        townhallID = -1;
        enemyFootmanID = -1;
        for(Integer unitID : enemyUnitIDs)
        {
            Unit.UnitView tempUnit = newstate.getUnit(unitID);
            String unitType = tempUnit.getTemplateView().getName().toLowerCase();
            if(unitType.equals("townhall"))
            {
                townhallID = unitID;
            }
            else if(unitType.equals("footman"))
            {
                enemyFootmanID = unitID;
            }
            else
            {
                System.err.println("Unknown unit type");
            }
        }

        if(townhallID == -1) {
            System.err.println("Error: Couldn't find townhall");
            return null;
        }

        long startTime = System.nanoTime();
        path = findPath(newstate);
        totalPlanTime += System.nanoTime() - startTime;

        return middleStep(newstate, statehistory);
    }

    @Override
    public Map<Integer, Action> middleStep(State.StateView newstate, History.HistoryView statehistory) {
        long startTime = System.nanoTime();
        long planTime = 0;

        Map<Integer, Action> actions = new HashMap<Integer, Action>();

        if(shouldReplanPath(newstate, statehistory, path)) {
            long planStartTime = System.nanoTime();
            path = findPath(newstate);
            planTime = System.nanoTime() - planStartTime;
            totalPlanTime += planTime;
        }

        Unit.UnitView footmanUnit = newstate.getUnit(footmanID);

        int footmanX = footmanUnit.getXPosition();
        int footmanY = footmanUnit.getYPosition();

        if(!path.empty() && (nextLoc == null || (footmanX == nextLoc.x && footmanY == nextLoc.y))) {

            // stat moving to the next step in the path
            nextLoc = path.pop();

            System.out.println("Moving to (" + nextLoc.x + ", " + nextLoc.y + ")");
        }

        if(nextLoc != null && (footmanX != nextLoc.x || footmanY != nextLoc.y))
        {
            int xDiff = nextLoc.x - footmanX;
            int yDiff = nextLoc.y - footmanY;

            // figure out the direction the footman needs to move in
            Direction nextDirection = getNextDirection(xDiff, yDiff);

            actions.put(footmanID, Action.createPrimitiveMove(footmanID, nextDirection));
        } else {
            Unit.UnitView townhallUnit = newstate.getUnit(townhallID);

            // if townhall was destroyed on the last turn
            if(townhallUnit == null) {
                terminalStep(newstate, statehistory);
                return actions;
            }

            if(Math.abs(footmanX - townhallUnit.getXPosition()) > 1 ||
                    Math.abs(footmanY - townhallUnit.getYPosition()) > 1)
            {
                System.err.println("Invalid plan. Cannot attack townhall");
                totalExecutionTime += System.nanoTime() - startTime - planTime;
                return actions;
            }
            else {
                System.out.println("Attacking TownHall");
                // if no more movements in the planned path then attack
                actions.put(footmanID, Action.createPrimitiveAttack(footmanID, townhallID));
            }
        }

        totalExecutionTime += System.nanoTime() - startTime - planTime;
        return actions;
    }

    @Override
    public void terminalStep(State.StateView newstate, History.HistoryView statehistory) {
        System.out.println("Total turns: " + newstate.getTurnNumber());
        System.out.println("Total planning time: " + totalPlanTime/1e9);
        System.out.println("Total execution time: " + totalExecutionTime/1e9);
        System.out.println("Total time: " + (totalExecutionTime + totalPlanTime)/1e9);
        System.exit(0);
    }

    @Override
    public void savePlayerData(OutputStream os) {

    }

    @Override
    public void loadPlayerData(InputStream is) {

    }

    /**
     * You will implement this method.
     *
     * This method should return true when the path needs to be replanned
     * and false otherwise. This will be necessary on the dynamic map where the
     * footman will move to block your unit.
     *
     * @param state
     * @param history
     * @param currentPath
     * @return
     */
    private boolean shouldReplanPath(State.StateView state, History.HistoryView history, Stack<MapLocation> currentPath)
    {
    	Unit.UnitView enemyFootmanUnit = state.getUnit(enemyFootmanID);
    	MapLocation enemyFootmanPosition = null;
    	// If there is no enemy on map
    	if (enemyFootmanID == -1){
    		return false;
    	}
    	// Get the postion of enemy footman
    	else{
    		enemyFootmanPosition = new MapLocation(enemyFootmanUnit.getXPosition(), enemyFootmanUnit.getYPosition(), null, 0);
    	}
    	
    	// Replan if the position of the enemy footman is on the path of our footman
    	for (MapLocation position : currentPath){
    		if (enemyFootmanPosition.equals(position)){
    			return true;
    		}
    	}
        return false;
    }

    /**
     * This method is implemented for you. You should look at it to see examples of
     * how to find units and resources in Sepia.
     *
     * @param state
     * @return
     */
    private Stack<MapLocation> findPath(State.StateView state)
    {
        Unit.UnitView townhallUnit = state.getUnit(townhallID);
        Unit.UnitView footmanUnit = state.getUnit(footmanID);

        MapLocation startLoc = new MapLocation(footmanUnit.getXPosition(), footmanUnit.getYPosition(), null, 0);

        MapLocation goalLoc = new MapLocation(townhallUnit.getXPosition(), townhallUnit.getYPosition(), null, 0);

        MapLocation footmanLoc = null;
        if(enemyFootmanID != -1) {
            Unit.UnitView enemyFootmanUnit = state.getUnit(enemyFootmanID);
            footmanLoc = new MapLocation(enemyFootmanUnit.getXPosition(), enemyFootmanUnit.getYPosition(), null, 0);
        }

        // get resource locations
        List<Integer> resourceIDs = state.getAllResourceIds();
        Set<MapLocation> resourceLocations = new HashSet<MapLocation>();
        for(Integer resourceID : resourceIDs)
        {
            ResourceNode.ResourceView resource = state.getResourceNode(resourceID);

            resourceLocations.add(new MapLocation(resource.getXPosition(), resource.getYPosition(), null, 0));
        }

        return AstarSearch(startLoc, goalLoc, state.getXExtent(), state.getYExtent(), footmanLoc, resourceLocations);
    }
    
    /**
     * This is the method you will implement for the assignment. Your implementation
     * will use the A* algorithm to compute the optimum path from the start position to
     * a position adjacent to the goal position.
     *
     * You will return a Stack of positions with the top of the stack being the first space to move to
     * and the bottom of the stack being the last space to move to. If there is no path to the townhall
     * then return null from the method and the agent will print a message and do nothing.
     * The code to execute the plan is provided for you in the middleStep method.
     *
     * As an example consider the following simple map
     *
     * F - - - -
     * x x x - x
     * H - - - -
     *
     * F is the footman
     * H is the townhall
     * x's are occupied spaces
     *
     * xExtent would be 5 for this map with valid X coordinates in the range of [0, 4]
     * x=0 is the left most column and x=4 is the right most column
     *
     * yExtent would be 3 for this map with valid Y coordinates in the range of [0, 2]
     * y=0 is the top most row and y=2 is the bottom most row
     *
     * resourceLocations would be {(0,1), (1,1), (2,1), (4,1)}
     *
     * The path would be
     *
     * (1,0)
     * (2,0)
     * (3,1)
     * (2,2)
     * (1,2)
     *
     * Notice how the initial footman position and the townhall position are not included in the path stack
     *
     * @param start Starting position of the footman
     * @param goal MapLocation of the townhall
     * @param xExtent Width of the map
     * @param yExtent Height of the map
     * @param resourceLocations Set of positions occupied by resources
     * @return Stack of positions with top of stack being first move in plan
     */
    public Stack<MapLocation> AstarSearch(MapLocation start, MapLocation goal, int xExtent, int yExtent, 
    		MapLocation enemyFootmanLoc, Set<MapLocation> resourceLocations)
    {
    	// Open list
    	ArrayList<MapLocation> openList = new ArrayList<MapLocation>();
    	// Closed list
    	ArrayList<MapLocation> closedList = new ArrayList<MapLocation>();
    	
    	// Begin exploration
    	// closedList.add(enemyFootmanLoc);
    	// Add the starting location to the open list and empty the closed list
    	openList.add(start);
    	// Sort the open list to determine the order of removal from the open list
    	// Sort the open list in ascending order to create a stack that traces the
    	// path from the goal back to the start
    	Collections.sort(openList, Collections.reverseOrder());
    	
    	// While the open list is not empty 
    	while (!openList.isEmpty()) {
    		
    		// Test if the current position is already the goal position
    		MapLocation currentPosition = openList.get(0);
    		if (currentPosition.equals(goal)) {
    			break;
    		}
    		
    		// Move this node from open list to closed list
    		openList.remove(currentPosition);
    		closedList.add(currentPosition);
    		
    		// Look at every next potential positions to get to from the current position
    		ArrayList<MapLocation> potentialPositions = calculateSurroundingPositions(currentPosition, xExtent, yExtent, resourceLocations);
    		for (MapLocation potentialPosition : potentialPositions) {
    			
    			// Calculate the path cost of reaching the potential position
    			// Assuming the movement cost is just 1 and remain the same for all potential positions
    			float checkCost = currentPosition.cost + 1;
    			
    			// If the cost is less than the cost known for this position, remove from list
    			if (checkCost < potentialPosition.cost) {
    				if (openList.contains(potentialPosition)) {
    					openList.remove(potentialPosition);
    				}
    				if (closedList.contains(potentialPosition)) {
    					closedList.remove(potentialPosition);
    				}
    			}
    			
    			// If the location is not in the open or closed list then
    			// (This part of code is also used to record better cost of already visited positions)
    			if (!openList.contains(potentialPosition) && !closedList.contains(potentialPosition)) {
    				// Record the cost
    				potentialPosition.cost = checkCost;
    				// Record the heuristic value
    				potentialPosition.heuristic = HelperAstar.heuristicCalculation(currentPosition, goal);
    				// Add potential position to open list
    				openList.add(potentialPosition);
    				// Set parent to current position
    				potentialPosition.cameFrom = currentPosition;
    				// Sort the open list again to maintain the removal order
    				Collections.sort(openList, Collections.reverseOrder());
    			}
    		}
    	}
    	    	
    	// Check if the goal has a parent node
    	if (openList.get(0).cameFrom == null) {
    		return null;
    	}
    	
    	// Get the final node in the open list to test
        MapLocation test = openList.get(0);
        // Test if the end of our goal path is not the goal position, if not close program
        if (HelperAstar.heuristicCalculation(test, goal) != 0) {
        	System.out.println("No available path.");
    		System.exit(0);
    		return null;
        }
        
    	// Create a stack to keep track of the movement of the footman in reality
    	Stack<MapLocation> pathOfFootman = new Stack<MapLocation>();
    	
    	// While the current node does not equal start we retrace the open list from the goal back to the start
    	MapLocation retrace = openList.get(0).cameFrom;
    	while (!retrace.equals(start)) {
    		pathOfFootman.push(retrace);
    		retrace = retrace.cameFrom;
    	}
    	
        return pathOfFootman;
    }

    /**
     * A method to calculate surrounding positions of a current positions
     * 
     * @param currentPosition the current map location
     * @param xExtent the width of the map
     * @param yExtent the height of the map
     * @return an array list 8 surrounding positions
     */
    public ArrayList<MapLocation> calculateSurroundingPositions(MapLocation currentPosition, int xExtent,
    		int yExtent, Set<MapLocation> resourceLocations) {
    	// Array list of surrounding positions
    	ArrayList<MapLocation> surroundingPositions = new ArrayList<MapLocation>();
    	
    	// Iterates through all potential positions
    	for (int x = -1; x < 2; x++) {
    		for (int y = -1; y < 2; y++) {
    			
    			// Exclude the current position
    			if (x == currentPosition.x && y == currentPosition.y) {
    				continue;
    			}
    			
    			// Compute the location of the potential positions
    			MapLocation potentialPosition = new MapLocation(currentPosition.x + x, currentPosition.y + y, null, 0);
    			
    			// Add the potential move to the array list if it is a valid positions
    			if (HelperAstar.isPositionValid(currentPosition, potentialPosition, xExtent, yExtent, resourceLocations)) {
    				surroundingPositions.add(potentialPosition);
    			}
    		}
    	}
    	
    	return surroundingPositions;
    }
    
    /**
     * Primitive actions take a direction (e.g. NORTH, NORTHEAST, etc)
     * This converts the difference between the current position and the
     * desired position to a direction.
     *
     * @param xDiff Integer equal to 1, 0 or -1
     * @param yDiff Integer equal to 1, 0 or -1
     * @return A Direction instance (e.g. SOUTHWEST) or null in the case of error
     */
    private Direction getNextDirection(int xDiff, int yDiff) {

        // figure out the direction the footman needs to move in
        if(xDiff == 1 && yDiff == 1)
        {
            return Direction.SOUTHEAST;
        }
        else if(xDiff == 1 && yDiff == 0)
        {
            return Direction.EAST;
        }
        else if(xDiff == 1 && yDiff == -1)
        {
            return Direction.NORTHEAST;
        }
        else if(xDiff == 0 && yDiff == 1)
        {
            return Direction.SOUTH;
        }
        else if(xDiff == 0 && yDiff == -1)
        {
            return Direction.NORTH;
        }
        else if(xDiff == -1 && yDiff == 1)
        {
            return Direction.SOUTHWEST;
        }
        else if(xDiff == -1 && yDiff == 0)
        {
            return Direction.WEST;
        }
        else if(xDiff == -1 && yDiff == -1)
        {
            return Direction.NORTHWEST;
        }

        System.err.println("Invalid path. Could not determine direction");
        return null;
    }
}