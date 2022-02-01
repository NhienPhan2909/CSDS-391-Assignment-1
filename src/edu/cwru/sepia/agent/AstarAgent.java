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

    class MapLocation
    {
        public int x, y;
        public MapLocation cameFrom;
		public float cost;

        public MapLocation(int x, int y, MapLocation cameFrom, float cost)
        {
            this.x = x;
            this.y = y;
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

        System.out.println("Constructed AstarAgent");
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
     * You can check the position of the enemy footman with the following code:
     * state.getUnit(enemyFootmanID).getXPosition() or .getYPosition().
     * 
     * There are more examples of getting the positions of objects in SEPIA in the findPath method.
     *
     * @param state
     * @param history
     * @param currentPath
     * @return
     */
    private boolean shouldReplanPath(State.StateView state, History.HistoryView history, Stack<MapLocation> currentPath)
    {
    	Unit.UnitView enemy_footman_unit = state.getUnit(enemyFootmanID);
    	Unit.UnitView player_footman_unit = state.getUnit(footmanID);
    	
    	// check if circumnavigate is possible and if there is no enemy
        if (currentPath.size() < 4 || enemy_footman_unit == null) {
            return false;
        }
        
        // if there is enemy
        if(enemy_footman_unit != null) 
        {
            // find enemy's position on map
            int enemy_x = enemy_footman_unit.getXPosition();
            int enemy_y = enemy_footman_unit.getYPosition();

            // find our footman's position on map
            int player_x = player_footman_unit.getXPosition();
            int player_y = player_footman_unit.getYPosition();

            // if enemy is blocking our path (enemy in a 7x7 square around our foot man) 
            // then the agent have to replan.
			for (int i = player_x - 3; i <= player_y + 3; i++)
				for (int j = player_y - 3; j <= player_x + 3; j++)
    				if (enemy_x == i && enemy_y == j)
    					return true;
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
     * Therefore your you need to find some possible adjacent steps which are in range 
     * and are not trees or the enemy footman.
     * Hint: Set<MapLocation> resourceLocations contains the locations of trees
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
    private Stack<MapLocation> AstarSearch(MapLocation start, MapLocation goal, int xExtent, int yExtent, MapLocation enemyFootmanLoc, Set<MapLocation> resourceLocations)
    {
    	// a boolean flag to check if the goal is reached
    	boolean goal_reached = false;
    	
    	// a variable to keep track of the current position of the footman
    	MapLocation current_position = start;
    	
    	// a 2-D boolean array to keep track of whether there is a resouce at a position
    	boolean[][] resource_existence = new boolean[xExtent][yExtent];
    	for (MapLocation resourceLocation : resourceLocations) 
    	{
    		resource_existence[resourceLocation.x][resourceLocation.y] = true;
    	}
    	
    	// open list and closed list for AstarSearch
        Stack<MapLocation> open_list = new Stack<MapLocation>();
        Stack<MapLocation> closed_list = new Stack<MapLocation>();
        
        // stack to keep track of optimal path towards the goal
        Stack<MapLocation> goal_path = new Stack<MapLocation>();
        
        // begin exploration of result path
        open_list.add(start);
        do {
        	closed_list.add(open_list.pop());
        	
        	// an array to store surrounding positions of the current position
        	// these are the potential moves for the next move of our footman
        	MapLocation[] potential_moves = calculate_surrounding_positions(current_position);
        	
        	// variable to hold value of heuristic function
        	float heuristic_holder = Float.MAX_VALUE;
        	
            // variable to hold value for best moves (resulted based on calculation of heuristic value)
        	// start with the first location in our array of potential moves
            MapLocation heuristic_move = potential_moves[0];
            
            // go over each move in the potential moves array to find the best move
            for (MapLocation potential_move : potential_moves) 
            {
            	// check if a move is legitimate
            	if (is_position_valid(potential_move, xExtent, yExtent) && 
            			is_position_empty(potential_move, enemyFootmanLoc, resource_existence)) {
            		//check if a move is yet to be explored
            		if (!closed_list.contains(potential_move)) {
            			// add the potential move to open list
            			open_list.add(potential_move);
            			// calculate the heuristic distance (cost) of this potential move
                        float potential_move_heuristic = heuristic_calculation(potential_move, goal);
                        potential_move.cost = potential_move_heuristic;
                        
                        // update the heuristic value and heuristic move if possible
                        if (potential_move_heuristic < heuristic_holder) {
                            heuristic_holder = potential_move_heuristic;
                            heuristic_move = potential_move;
                        }
                        
                        // add potential move to close list.
                        closed_list.add(potential_move);
            		}
            	}
            }
            
            // clear open list for next evaluation
            open_list.clear();
            // add the new best position to open list
            open_list.add(heuristic_move);
            // move to the new best position
            current_position = heuristic_move;

            // check if the goal has been reached
            if (is_same_position(heuristic_move, goal)) {
                goal_reached = true;
            } else {
            	// if not yet reached the goal, add this move to the goal path.
                goal_path.add(heuristic_move); 
            }
        } // repeat until the goal is reached or until all alternative is already considered
        while (!goal_reached && !open_list.isEmpty());

        // create a stack to keep track of the actual moves to make in reality
        Stack<MapLocation> result = new Stack<MapLocation>();
		while (!goal_path.isEmpty()) {
			result.push(goal_path.pop());
		}    
            	
        return result;
    }
    
    // Below are 5 helpers methods to use in support of AstarSearch
	
	/** Method to calculate heuristic Chebyshev distance
	 * 
	 * @param current_position
	 * @param goal
	 * @return the heuristic Chebyshev distance
	 */
	private float heuristic_calculation(MapLocation current_position, MapLocation goal)
	{
		if (Math.abs(goal.x - current_position.x) > Math.abs(goal.y - current_position.y)) {
            return (float) Math.abs(goal.x - current_position.x);
        }
        return (float) Math.abs(goal.y - current_position.y);
	}
	
	/** Method to check if the location moved to is empty (do not have enemy/resource there)
	 * 
	 * @param destination
	 * @param enemy_position
	 * @param resource_existence --> true if there is a resource at that location, false otherwise 
	 * @return true if the destination is empty, false otherwise
	 */
	private boolean is_position_empty(MapLocation destination, MapLocation enemy_position, boolean[][] resource_existence) 
	{
		return (destination != enemy_position) && (!resource_existence[destination.x][destination.y]);
	}
	
	/** Method to check if the location moved to is in the map
	 * 
	 * @param destination
	 * @param xExtent
	 * @param yExtent
	 * @return true if destination is in the map, false otherwise
	 */
	private boolean is_position_valid(MapLocation destination, int xExtent, int yExtent) 
	{
		return (destination.x >= 0 && destination.x <= xExtent) && (destination.y >= 0 && destination.y <= yExtent);
	}

    /** Method to check if the current position is the same as the location intent to move to
     * 
     * @param current_position
     * @param destination
     * @return true  if it is the same position, false otherwise
     */
	private boolean is_same_position(MapLocation current_position, MapLocation destination) 
	{
		return (current_position.x == destination.x) && (current_position.y == destination.y);
	}
	
	/** Method to calculate the surrounding positions from any current position
	 * 
	 * @param current_position
	 * @return an array of 8 positions around the current position
	 */
	private MapLocation[] calculate_surrounding_positions(MapLocation current_position) 
	{
		// array of surrounding positions
		MapLocation[] surrounding_positions = new MapLocation[8];
		
		// pointer for the surrounding positions array
		int pointer = 0;
		/*for (int i = current_position.x - 1; i <= current_position.x + 1; i++) {
			for (int j = current_position.y - 1; j <= current_position.y + 1; j++) {
				MapLocation new_location = new MapLocation(i, j, current_position, 0);
				if (!is_same_position(new_location, current_position)) {
					surrounding_positions[pointer] = new_location;
					pointer++;
				}
			}
		}*/                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       
		for (int i = current_position.x - 1; i <= current_position.x + 1; i++) {
			surrounding_positions[pointer] = new MapLocation(i, current_position.y + 1, current_position, 0);
			pointer++;
			surrounding_positions[pointer] = new MapLocation(i, current_position.y - 1, current_position, 0);
			pointer++;
		}
		surrounding_positions[pointer] = new MapLocation(current_position.x - 1, current_position.y, current_position, 0);
		pointer++;
		surrounding_positions[pointer] = new MapLocation(current_position.x + 1, current_position.y, current_position, 0);
		pointer++;
		return surrounding_positions;
	}

    /**
     * Primitive actions take a direction (e.g. Direction.NORTH, Direction.NORTHEAST, etc)
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
