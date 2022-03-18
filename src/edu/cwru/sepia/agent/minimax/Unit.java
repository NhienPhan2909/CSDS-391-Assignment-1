package edu.cwru.sepia.agent.minimax;

// Superclass of both game unit and resource unit
public abstract class Unit {
	int unitId;
	private int xPosition;
	private int yPosition;

	public Unit(int unitId, int xPosition, int yPosition) {
		this.unitId = unitId;
		this.xPosition = xPosition;
		this.yPosition = yPosition;
	}
	
	public boolean isAlly() {
		return this.getUnitId() == 0 || this.getUnitId() == 1;
	}

	public int getUnitId() {
		return this.unitId;
	}

	public int getXPosition() {
		return this.xPosition;
	}

	public void setXPosition(int xPosition) {
		this.xPosition = xPosition;
	}

	public int getYPosition() {
		return this.yPosition;
	}

	public void setYPosition(int yPosition) {
		this.yPosition = yPosition;
	}
}