package edu.cwru.sepia.agent.planner.resources;

import edu.cwru.sepia.agent.planner.Position;

// The superclass for gold and wood
public abstract class Resource {
	// The ID of this resource
	protected int id;
	// The amount of resource remaining
	protected int amountRemaining;
	// The position of this resource
	protected Position position;
	
	// Determine if this resource is gold
	public abstract boolean isGold();
	
	// Determine if this resource is wood
	public abstract boolean isWood();
	
	/**
	 * Get the ID of this resource
	 * @return the resource ID
	 */
	public int getID() {
		return id;
	}
	
	/**
	 * Set the ID of this resource to a new ID
	 * @param id new resource ID
	 */
	public void setID(int id) {
		this.id = id;
	}
	
	/**
	 * Get the amount remaining of this resource
	 * @return the amount remaining of this resource
	 */
	public int getAmountRemaining() {
		return amountRemaining;
	}
	
	/**
	 * Set the amount remaining of this resource to a new amount
	 * @param amountRemaining new amount remaining
	 */
	public void setAmountRemaining(int amountRemaining) {
		this.amountRemaining = amountRemaining;
	}
	
	/**
	 * Get the resource position
	 * @return the resource position
	 */
	public Position getPosition() {
		return position;
	}
	
	/**
	 * Set the resource position to a new position
	 * @param position new resource position
	 */
	public void setPosition(Position position) {
		this.position = position;
	}

	/**
	 * Determine if the resource has been depleted
	 * @return true if there are still resource remaining, false otherwise
	 */
	public boolean stillRemaining() {
		return amountRemaining > 0;
	}
	
}