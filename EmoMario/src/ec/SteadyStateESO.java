package ec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import ch.idsia.benchmark.tasks.BasicTask;
import ch.idsia.tools.EvaluationInfo;
import ch.idsia.tools.MarioAIOptions;

public final class SteadyStateESO {

	ArrayList<SingleFitnessAgent> pop;
	float xoverProb;
	float mutProb;
	int selectTourneySize;
	SingleFitnessAgent bestAgent;
	int difficulty;
	int unimprovementTimeout;
	int unimprovementCount = 0;
	
	public SteadyStateESO(int initialChromoLength, int popSize, float xoverProb, float mutProb, 
			int selectTourneySize, int difficulty, int unimprovementTimeout) {
		pop = new ArrayList<SingleFitnessAgent>();
		Random random = new Random();
		for (int i = 0; i < popSize; ++i) {
			int[] instructions = new int[initialChromoLength];
			for (int j = 0; j < initialChromoLength; ++j) {
				if (random.nextFloat() < 0.5f)
					instructions[j] = 1;
				else
					instructions[j] = 0;
			}
			pop.add(new SingleFitnessAgent(instructions));
		}
		
		// evaluate initial pop
		calculateFitnesses(pop);
		bestAgent = pop.get(0);
		for (int i = 1; i < pop.size(); ++i)
			if (pop.get(i).getFitness() > bestAgent.getFitness())
				bestAgent = pop.get(i);
		
		this.xoverProb = xoverProb;
		this.mutProb = mutProb;
		this.selectTourneySize = selectTourneySize;
		this.difficulty = difficulty;
		this.unimprovementTimeout = unimprovementTimeout;
	}
	
	public void run(int numOfEvals) {
		int currentEvals = pop.size();
		
		while (currentEvals < numOfEvals) {
			// check if we should stop due to unimprovement
			if (unimprovementCount >= unimprovementTimeout) {
				System.out.println("Stopping due to lack of improvent in " + currentEvals + " evaluations.");
				break;
			}
			
			/* XXX actually, we can always minimize jumps of a level completer, so keep going...
			// stop immediately if an agent is successful (SHOULD be one of the best agents)
			if (levelCompleterFound()) {
				System.out.println("Level completer found in " + currentEvals + " evaluations!");
				break;
			}
			*/
			
			// print currentEvals at even intervals to give feedback
			if (currentEvals % 10000 == 0)
				System.out.println("Finished " + currentEvals + " evaluations.");
			
			// select parents
			SingleFitnessAgent parent1 = tourneySelectParent();
			SingleFitnessAgent parent2 = tourneySelectParent();
			
			// crossover
			ArrayList<SingleFitnessAgent> children = crossover(parent1, parent2);
			
			// mutate
			for (SingleFitnessAgent child : children)
				mutate(child);
			
			// calculate children's fitnesses
			calculateFitnesses(children);
			currentEvals += children.size();
			
			// print children
			//printAgents(children);
			
			
			// update bestAgent
			for (SingleFitnessAgent child : children)
				if (child.getFitness() > bestAgent.getFitness()) {
					bestAgent = child;
					// found a new best so reset unimprovementCount
					unimprovementCount = 0;
				}
				else // this agent was not an improvement of the best...
					++unimprovementCount;
			
			// select replacements
			for (SingleFitnessAgent child : children) {
				tourneyReplacement(child);
			}
		}
	}
	
	public SingleFitnessAgent getBest() {
		return bestAgent;
	}
	
	public double getAverageFitness() {
		double result = 0.0;
		
		for (SingleFitnessAgent agent : pop) {
			result += agent.getFitness();
		}
		
		return result / pop.size();
	}
	
	public void printPop() {
		printAgents(pop);
	}
	
	public void runVisualization(SingleFitnessAgent agent) {
		final MarioAIOptions marioAIOptions = new MarioAIOptions(new String[0]);
		marioAIOptions.setVisualization(true);
		marioAIOptions.setLevelDifficulty(difficulty);
		final BasicTask basicTask = new BasicTask(marioAIOptions);
		basicTask.setOptionsAndReset(marioAIOptions);
		marioAIOptions.setAgent(agent);
		basicTask.doEpisodes(1, false, 1);
	}
	
	private void calculateFitnesses(ArrayList<SingleFitnessAgent> agents) {
		final MarioAIOptions marioAIOptions = new MarioAIOptions(new String[0]);
		marioAIOptions.setVisualization(false);
		marioAIOptions.setLevelDifficulty(difficulty);
		final BasicTask basicTask = new BasicTask(marioAIOptions);
		basicTask.setOptionsAndReset(marioAIOptions);
		
		for (SingleFitnessAgent agent : agents) {
			marioAIOptions.setAgent(agent);
			basicTask.doEpisodes(1, false, 1);
			// TODO figure out best fitness calculation to do here!!!
			EvaluationInfo ei = basicTask.getEnvironment().getEvaluationInfo();
			while (ei == null) {
				System.out.println("EvaluationInfo null! Waiting...");
				ei = basicTask.getEnvironment().getEvaluationInfo();
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			float distancePercentage = basicTask.getEnvironment().getLevelScene().xCam;
			boolean completedLevel = false;
			if (distancePercentage == 4096.f)
				completedLevel = true;
			int completedMod = 0;
			if (completedLevel)
				completedMod = 1;
			int mode = ei.marioMode; // higher number means less hits; 0 means dead
			int timeLeft = ei.timeLeft;
			agent.setStats(completedLevel, distancePercentage, mode, timeLeft);
			int jumpCount = agent.getJumpCount();
			agent.setFitness(distancePercentage);
			agent.chopInstructions();
		}
	}
	
	// select parents based on max fitness of tournament
	private SingleFitnessAgent tourneySelectParent() {
		Random random = new Random();
		SingleFitnessAgent[] possibles = new SingleFitnessAgent[selectTourneySize];
		for (int i = 0; i < possibles.length; ++i)
			possibles[i] = pop.get(random.nextInt(pop.size()));
		
		SingleFitnessAgent bestAgent = possibles[0];
		for (int i = 1; i < possibles.length; ++i)
			if (possibles[i].getFitness() > bestAgent.getFitness())
				bestAgent = possibles[i];
		
		return bestAgent;
	}
	
	private ArrayList<SingleFitnessAgent> crossover(SingleFitnessAgent parent1, SingleFitnessAgent parent2) {
		ArrayList<SingleFitnessAgent> result = new ArrayList<SingleFitnessAgent>();
		
		int parent1Length = parent1.getInstructionsLength();
		int parent2Length = parent2.getInstructionsLength();
		
		int shortestLength = 0;
		if (parent1Length < parent2Length)
			shortestLength = parent1Length;
		else
			shortestLength = parent2Length;
		
		// perform the physical crossover
		Random random = new Random();
		int rand = shortestLength - 1;
		if (rand <= 0) {
			System.out.println("parent1 instructions:" + Arrays.toString(parent1.getInstructions()));
			System.out.println("parent2 instructions:" + Arrays.toString(parent2.getInstructions()));
			System.out.println(shortestLength);
		}
		int crossPoint = random.nextInt(shortestLength-1) + 1;
		int[] newInstructions1 = new int[parent1Length];
		System.arraycopy(parent2.getInstructions(), 0, newInstructions1, 0, crossPoint);
		System.arraycopy(parent1.getInstructions(), crossPoint, newInstructions1, crossPoint, 
				newInstructions1.length - crossPoint);
		SingleFitnessAgent child1 = new SingleFitnessAgent(newInstructions1);
		result.add(child1);
		int[] newInstructions2 = new int[parent2Length];
		System.arraycopy(parent1.getInstructions(), 0, newInstructions2, 0, crossPoint);
		System.arraycopy(parent2.getInstructions(), crossPoint, newInstructions2, crossPoint, 
				newInstructions2.length - crossPoint);
		SingleFitnessAgent child2 = new SingleFitnessAgent(newInstructions2);
		result.add(child2);
		
		return result;
	}
	
	private void mutate(SingleFitnessAgent agent) {
		int[] instructions = agent.getInstructions();
		Random random = new Random();
		for (int i = 0; i < instructions.length; ++i)
			if (random.nextFloat() < mutProb)
				if (instructions[i] == 1)
					instructions[i] = 0;
				else
					instructions[i] = 1;
	}
	
	// select agent to replace
	private void tourneyReplacement(SingleFitnessAgent agent) {
		Random random = new Random();
		int[] possibles = new int[selectTourneySize];
		for (int i = 0; i < possibles.length; ++i)
			possibles[i] = random.nextInt(pop.size());
		
		int bestAgent = possibles[0];
		for (int i = 1; i < possibles.length; ++i)
			if (pop.get(possibles[i]).getFitness() < pop.get(bestAgent).getFitness())
				bestAgent = possibles[i];
		
		pop.remove(bestAgent);
		pop.add(agent);
	}

	/* not using because of potential of minimizing jumps of level completer
	// used for termination criterion
	private boolean levelCompleterFound() {
		
		for (SingleFitnessAgent agent : pop)
			if (agent.completedLevel())
				return true;
		
		return false;
	}
	*/
	
	private void printAgents(ArrayList<SingleFitnessAgent> agentList) {
		 for (SingleFitnessAgent agent : agentList)
			 System.out.println(agent);
		 System.out.println();
	 }
}
