package ec;

import java.util.Arrays;

import ch.idsia.agents.Agent;
import ch.idsia.agents.controllers.BasicMarioAIAgent;
import ch.idsia.benchmark.mario.engine.sprites.Mario;
import ch.idsia.benchmark.mario.environments.Environment;

public class InstructionAgent extends BasicMarioAIAgent implements Agent {

	private static int numIds = 0;
	private int id = -1;
	private int[] instructions;
	private int currentIndex = 0;
	private int jumpCount = 0;
	private int instructionCount = 0;
	private boolean completedLevel = false;
	private float distancePercentage = -1.f;
	private int mode = -1;
	private int timeLeft = -1;
	private float x;
	
	public InstructionAgent(int[] instructions) {
		super("InstructionAgent");
		this.instructions = instructions;
		action[Mario.KEY_RIGHT] = true;
		action[Mario.KEY_SPEED] = true;
		id = numIds++;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("id == ").append(id).append("\n")
		  .append("completedLevel == ").append(completedLevel).append("\n")
		  .append("distancePercentage == ").append(distancePercentage).append("\n")
		  .append("mode == ").append(mode).append("\n")
		  .append("jumpCount == ").append(jumpCount).append("\n")
		  .append("timeLeft == ").append(timeLeft).append("\n")
		  .append("instructionsLength == ").append(instructions.length).append("\n")
		  .append("instructions == ").append(Arrays.toString(instructions)).append("\n");
		
		return sb.toString();
	}
	
	@Override
	public boolean[] getAction() {
		//action[Mario.KEY_SPEED] = action[Mario.KEY_JUMP] = isMarioAbleToJump || !isMarioOnGround;
		if (currentIndex < instructions.length - 1) {
			if (instructions[currentIndex++] == 1) {
				action[Mario.KEY_JUMP] = isMarioAbleToJump || !isMarioOnGround;
				++jumpCount;
			}
			else
				action[Mario.KEY_JUMP] = false;
		}
		else 
			action[Mario.KEY_JUMP] = false;
		
		++instructionCount;
		
		return action;
	}
	
	@Override
	public void integrateObservation(Environment environment) {
		super.integrateObservation(environment);
		x = environment.getMarioFloatPos()[0];
	}
	
	@Override
	public void reset() {
		currentIndex = 0;
		jumpCount = 0;
		instructionCount = 0;
		action = new boolean[Environment.numberOfKeys];
		action[Mario.KEY_RIGHT] = true;
		action[Mario.KEY_SPEED] = true;
	}
	
	public float getX() {
		return x;
	}
	
	public int getJumpCount() {
		return jumpCount;
	}
	
	public int getInstructionsLength() {
		return instructions.length;
	}
	
	public int[] getInstructions() {
		return instructions;
	}
	
	public void setInstructions(int[] newInstructions) {
		instructions = newInstructions;
	}
	
	public void setStats(boolean completedLevel, float distance, int mode, int timeLeft) {
		this.completedLevel = completedLevel;
		this.distancePercentage = distance;
		this.mode = mode;
		this.timeLeft = timeLeft;
	}
	
	public boolean completedLevel() {
		return completedLevel;
	}
	
	public void chopInstructions() {
		int[] newInstructions = new int[instructionCount];
		if (instructionCount < instructions.length)
			System.arraycopy(instructions, 0, newInstructions, 0, instructionCount);
		else {
			System.arraycopy(instructions, 0, newInstructions, 0, instructions.length);
		}
		instructions = newInstructions;
	}

}
