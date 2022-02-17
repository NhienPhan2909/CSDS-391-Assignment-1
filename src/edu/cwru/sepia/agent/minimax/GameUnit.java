package edu.cwru.sepia.agent.minimax;
// A class that represent footmen and archers
public class GameUnit extends Unit {
	private int maxHealth;
	private int currentHealth;
	private int attackDamage;
	private int attackRange;

	public GameUnit(int unitId, int xPosition, int yPosition, 
			int currentHealth, int maxHealth, int attackDamage, int attackRange) {
		super(unitId, xPosition, yPosition);
		this.currentHealth = currentHealth;
		this.maxHealth = maxHealth;
		this.attackDamage = attackDamage;
		this.attackRange = attackRange;
	}

	public boolean isAlly() {
		return this.getUnitId() == 0 || this.getUnitId() == 1;
	}

	public boolean isAlive() {
		return currentHealth > 0;
	}

	int getCurrentHealth() {
		return currentHealth;
	}

	void setCurrentHealth(int health) {
		this.currentHealth = health;
	}

	int getMaxHealth() {
		return maxHealth;
	}

	int getAttackDamage() {
		return attackDamage;
	}

	int getAttackRange() {
		return attackRange;
	}
}