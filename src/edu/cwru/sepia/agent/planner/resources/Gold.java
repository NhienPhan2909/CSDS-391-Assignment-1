package edu.cwru.sepia.agent.planner.resources;

import edu.cwru.sepia.agent.planner.Position;

// A class that represents gold resource
public class Gold extends Resource {
	
	// Constructor to create gold in the first game state
	public Gold(int id, int amountRemaining, Position position) {
		this.id = id;
		this.amountRemaining = amountRemaining;
		this.position = position;
	}

	// Constructor to create gold in all other game states
	public Gold(Resource gold) {
		this.id = gold.id;
		this.amountRemaining = gold.amountRemaining;
		this.position = new Position(gold.position);
	}

	// Assert that this resource is gold
	@Override
	public boolean isGold() {
		return true;
	}

	// Assert that this resource is not wood
	@Override
	public boolean isWood() {
		return false;
	}
}