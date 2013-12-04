package ec;

public final class SingleFitnessAgent extends InstructionAgent {

	double fitness = 0.0;
	
	public SingleFitnessAgent(int[] instructions) {
		super(instructions);
	}
	
	public double getFitness() {
		return fitness;
	}
	
	public void setFitness(double fitness) {
		this.fitness = fitness;
	}

}
