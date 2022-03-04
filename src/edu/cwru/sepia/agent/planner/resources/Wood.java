package edu.cwru.sepia.agent.planner.resources;

import edu.cwru.sepia.agent.planner.Position;

// A class that represents wood resource
public class Wood extends Resource {
	
	// Constructor to create wood in the first game state
	public Wood(int id, int amountRemaining, Position position) {
		this.id = id;
		this.amountRemaining = amountRemaining;
		this.position = position;
	}

	// Constructor to create wood in all other game states
	public Wood(Resource gold) {
		this.id = gold.id;
		this.amountRemaining = gold.amountRemaining;
		this.position = new Position(gold.position);
	}

	// Assert that this resource is not gold
	@Override
	public boolean isGold() {
		return false;
	}

	// Assert that this resource is wood
	@Override
	public boolean isWood() {
		return true;
	}
}