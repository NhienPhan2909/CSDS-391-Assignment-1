package edu.cwru.sepia.agent.planner;

public class Peasant {
	// ID of the peasant
	private int id;
	// Current position of the peasant
	private Position position;
	// The amount of gold this peasant carrying
	private int goldAmount = 0;
	// The amount of wood this peasant is carrying
	private int woodAmount = 0;

	// Constructor to create peasant in the first game state
	public Peasant(int id, Position position) {
		this.id = id;
		this.position = position;
	}

	// Constructor to create peasant in all other game states
	public Peasant(Peasant peasant) {
		this.id = peasant.id;
		this.position = new Position(peasant.position);
		this.goldAmount = peasant.goldAmount;
		this.woodAmount = peasant.woodAmount;
	}

	/**
	 * Get the ID of this peasant
	 * 
	 * @return the peasant ID
	 */
	public int getID() {
		return id;
	}

	/**
	 * Set the ID of the peasant to a new ID
	 * 
	 * @param id new ID of the peasant
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Get the current position of the peasant
	 * 
	 * @return the current position of the peasant
	 */
	public Position getPosition() {
		return position;
	}

	/**
	 * Set the position of the peasant to a new position
	 * 
	 * @param position new position of the peasant
	 */
	public void setPosition(Position position) {
		this.position = position;
	}

	/**
	 * Get the amount of gold the peasant is carrying
	 * 
	 * @return the amount of gold the peasant is carrying
	 */
	public int getGoldAmount() {
		return goldAmount;
	}

	/**
	 * Set the amount of gold the peasant is carrying to a new amount
	 * 
	 * @param goldAmount the new amount of gold
	 */
	public void setGoldAmount(int goldAmount) {
		this.goldAmount = goldAmount;
	}

	/**
	 * Get the amount of wood the peasant is carrying
	 * 
	 * @return the amount of wood the peasant is carrying
	 */
	public int getWoodAmount() {
		return woodAmount;
	}

	/**
	 * Set the amount of wood the peasant is carrying to a new amount
	 * 
	 * @param goldAmount the new amount of wood
	 */
	public void setWoodAmount(int woodAmount) {
		this.woodAmount = woodAmount;
	}

	/**
	 * Check if the peasant is carrying gold
	 * 
	 * @return true if the peasant is carrying gold, false otherwise
	 */
	public boolean hasGold() {
		return goldAmount > 0;
	}

	/**
	 * Check if the peasant is carrying wood
	 * 
	 * @return true if the peasant is carrying wood, false otherwise
	 */
	public boolean hasWood() {
		return woodAmount > 0;
	}
	
	/**
	 * Check if the peasant is carrying any resources
	 * @return true if the peasant is carrying any resources, false otherwise
	 */
	public boolean isCarry() {
		return hasGold() || hasWood();
	}
	
	/**
	 * Check if the object we want to compare to is this peasant
	 */
	@Override
	public boolean equals(Object obj) {
		// Check if the object we want to compare to is this peasant
		if (this == obj)
			return true;
		// Check if the there is nothing to compare
		if (obj == null)
			return false;
		// Check if the object we want to compare to is not a peasant
		if (getClass() != obj.getClass())
			return false;
		// Cast the object as peasant if it is a peasant
		Peasant other = (Peasant) obj;
		// If the peasant has a different ID
		if (id != other.id)
			return false;
		// If the other peasant carries a different amount of gold
		if (goldAmount != other.goldAmount)
			return false;
		// If the other peasant carries a different amount of wood
		if (woodAmount != other.woodAmount)
			return false;
		// If this peasant does not have position (safe check) 
		if (position == null && other.position != null) 
			return false;
		// If two peasants have different positions
		else if (!position.equals(other.position))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + goldAmount;
		result = prime * result + woodAmount;
		result = prime * result + ((position == null) ? 0 : position.hashCode());
		return result;
	}

}