package ec;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import ch.idsia.benchmark.tasks.BasicTask;
import ch.idsia.tools.EvaluationInfo;
import ch.idsia.tools.MarioAIOptions;

public final class EMOSteadyState {

	PrintWriter pw;
	ArrayList<SingleFitnessAgent> pop;
	float xoverProb;
	float mutProb;
	int selectTourneySize;
	ArrayList<SingleFitnessAgent> bestList;
	SingleFitnessAgent bestAgent;
	int difficulty;
	int unimprovementTimeout;
	int unimprovementCount = 0;
	int rngSeed;
	
	public EMOSteadyState(PrintWriter pw, int initialChromoLength, float jumpDensity, int popSize, float xoverProb, float mutProb, 
			int selectTourneySize, int difficulty, int unimprovementTimeout, int rngSeed) {
		this.pw = pw;
		bestList = new ArrayList<SingleFitnessAgent>();
		pop = new ArrayList<SingleFitnessAgent>();
		Random random = new Random();
		for (int i = 0; i < popSize; ++i) {
			int[] instructions = new int[initialChromoLength];
			for (int j = 0; j < initialChromoLength; ++j) {
				if (random.nextFloat() < jumpDensity)
					instructions[j] = 1;
				else
					instructions[j] = 0;
			}
			pop.add(new SingleFitnessAgent(instructions));
		}
		
		this.xoverProb = xoverProb;
		this.mutProb = mutProb;
		this.selectTourneySize = selectTourneySize;
		this.difficulty = difficulty;
		this.unimprovementTimeout = unimprovementTimeout;
		this.rngSeed = rngSeed;
		
		// evaluate initial pop
		calculateFitnesses(pop);
		
		bestAgent = pop.get(0);
		for (SingleFitnessAgent agent : pop) {
			if (bestAgent.getX() < 4096.0) {
				if (agent.getX() > bestAgent.getX())
					bestAgent = agent;
			}
			else if (agent.getX() >= 4096.0 && agent.getTimeLeft() > bestAgent.getTimeLeft()) {
				bestAgent = agent;
			}
		}
		
		
		// find Pareto front
		ArrayList<SingleFitnessAgent> rest = new ArrayList<SingleFitnessAgent>(pop);
		SingleFitnessAgent farthest = findFarthestDistanceAgent(rest);
		bestList.add(farthest);
		while (rest.size() > 0) {
			SingleFitnessAgent nextFarthest = findFarthestDistanceAgent(rest);
			if (nextFarthest.getTimeLeft() > farthest.getTimeLeft()) {
				bestList.add(nextFarthest);
				farthest = nextFarthest;
			}
		}
		
	}
	
	public void run(int numOfEvals) {
		int currentEvals = pop.size();
		
		while (currentEvals < numOfEvals) {
			// check if we should stop due to unimprovement
			if (unimprovementCount >= unimprovementTimeout) {
				String announcement = "Stopping due to lack of improvent in " + unimprovementTimeout + " evaluations.\n"
										+ "Current number of evaluations: " + currentEvals;
				pw.println(announcement);
				pw.flush();
				System.out.println(announcement);
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
			/*
			 for (SingleFitnessAgent agent : pop) {
				if (bestAgent.getX() < 4096.0) {
					if (agent.getX() > bestAgent.getX())
						bestAgent = agent;
				}
				else if (agent.getX() >= 4096.0 && agent.getTimeLeft() > bestAgent.getTimeLeft()) {
					bestAgent = agent;
				}
			}
			 */
			for (SingleFitnessAgent child : children) {
				if (bestAgent.getX() < 4096.0) {
					if (child.getX() > bestAgent.getX()) {
						bestAgent = child;
						unimprovementCount = 0;
					}
					else
						++ unimprovementCount;
				}
				else if (child.getX() >= 4096.0 && child.getTimeLeft() > bestAgent.getTimeLeft()) {
					bestAgent = child;
					unimprovementCount = 0;
				}
				else
					++unimprovementCount;
			}
			
			
			// select replacements
			for (SingleFitnessAgent child : children) {
				tourneyReplacement(child);
			}
			
			// refind non-dominated solutions
			bestList = new ArrayList<SingleFitnessAgent>();
			ArrayList<SingleFitnessAgent> rest = new ArrayList<SingleFitnessAgent>(pop);
			SingleFitnessAgent farthest = findFarthestDistanceAgent(rest);
			bestList.add(farthest);
			while (rest.size() > 0) {
				SingleFitnessAgent nextFarthest = findFarthestDistanceAgent(rest);
				if (nextFarthest.getTimeLeft() < farthest.getTimeLeft()) {
					bestList.add(nextFarthest);
					farthest = nextFarthest;
				}
			}
		}
	}
	
	public SingleFitnessAgent findFarthestDistanceAgent(ArrayList<SingleFitnessAgent> agentList) {
		int farthest = 0;
		for (int i = 1; i < agentList.size(); ++i)
			if (agentList.get(i).getX() > agentList.get(farthest).getX())
				farthest = i;
		
		SingleFitnessAgent farthestDistanceAgent = agentList.remove(farthest);
		return farthestDistanceAgent;
	}
	
	public double getAverageFitness() {
		double result = 0.0;
		
		for (SingleFitnessAgent agent : pop) {
			result += agent.getFitness();
		}
		
		return result / pop.size();
	}
	
	public String popAsString() {
		StringBuilder sb = new StringBuilder();
		
		for (SingleFitnessAgent agent : pop)
			sb.append(agent).append("\n");
		
		sb.append("\n");
		
		return sb.toString();
	}
	
	public void printPop() {
		printAgents(pop);
	}
	
	public void runWithVisualization(SingleFitnessAgent agent) {
		final MarioAIOptions marioAIOptions = new MarioAIOptions(new String[0]);
		marioAIOptions.setVisualization(true);
		marioAIOptions.setLevelDifficulty(difficulty);
		marioAIOptions.setLevelRandSeed(rngSeed);
		final BasicTask basicTask = new BasicTask(marioAIOptions);
		basicTask.setOptionsAndReset(marioAIOptions);
		marioAIOptions.setAgent(agent);
		
		basicTask.doEpisodes(1, false, 1);
	}
	
	public void runWithoutVisualization(SingleFitnessAgent agent) {
		final MarioAIOptions marioAIOptions = new MarioAIOptions(new String[0]);
		marioAIOptions.setVisualization(false);
		marioAIOptions.setLevelDifficulty(difficulty);
		marioAIOptions.setLevelRandSeed(rngSeed);
		final BasicTask basicTask = new BasicTask(marioAIOptions);
		basicTask.setOptionsAndReset(marioAIOptions);
		marioAIOptions.setAgent(agent);
		
		basicTask.doEpisodes(1, false, 1);
	}
	
	private void calculateFitnesses(ArrayList<SingleFitnessAgent> agents) {
		final MarioAIOptions marioAIOptions = new MarioAIOptions(new String[0]);
		marioAIOptions.setVisualization(false);
		marioAIOptions.setLevelDifficulty(difficulty);
		marioAIOptions.setLevelRandSeed(rngSeed);
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
			
			float distancePercentage = agent.getX();
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
			agent.setFitness(distancePercentage - jumpCount);
			agent.chopInstructions();
		}
	}
	
	// select parents based on max fitness of tournament
	private SingleFitnessAgent tourneySelectParent() {
		Random random = new Random();
		SingleFitnessAgent[] possibles = new SingleFitnessAgent[selectTourneySize];
		for (int i = 0; i < possibles.length; ++i)
			possibles[i] = pop.get(random.nextInt(pop.size()));
		
		/*
		SingleFitnessAgent bestAgent = possibles[0];
		for (int i = 1; i < possibles.length; ++i)
			if (possibles[i].getFitness() > bestAgent.getFitness())
				bestAgent = possibles[i];
		*/
		
		/* old implementation
		SingleFitnessAgent farthestDistanceAgent = possibles[0];
		for (int i = 1; i < possibles.length; ++i)
			if (possibles[i].getX() > farthestDistanceAgent.getX())
				farthestDistanceAgent = possibles[i];
		
		SingleFitnessAgent leastJumpsAgent = possibles[0];
		for (int i = 1; i < possibles.length; ++i)
			if (possibles[i].getJumpCount() < leastJumpsAgent.getJumpCount())
				leastJumpsAgent = possibles[i];
		
		// return non-dominated agent
		if (farthestDistanceAgent.getId() == leastJumpsAgent.getId())
			return farthestDistanceAgent;
		
		SingleFitnessAgent bestAgent = null;
		if ((farthestDistanceAgent.getX() / 4096.f) <= (1 - (leastJumpsAgent.getJumpCount() / leastJumpsAgent.getInstructionsLength())))
			if (leastJumpsAgent.getJumpCount() == 0)
				bestAgent = farthestDistanceAgent;
			else
				bestAgent = leastJumpsAgent;
		else
			bestAgent = farthestDistanceAgent;
		*/
		
		// get bestAgent based on farthest distance
		SingleFitnessAgent bestAgent = possibles[0];
		for (int i = 1; i < possibles.length; ++i) 
			if (possibles[i].getX() > bestAgent.getX())
				bestAgent = possibles[i];
		
		if (bestAgent.getX() >= 4096.0) 
			for (SingleFitnessAgent otherAgent : possibles)
				if (otherAgent.getX() >= 4096.0 && otherAgent.getJumpCount() < bestAgent.getJumpCount())
					bestAgent = otherAgent;
			
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
		
		/* old implementation
		int leastDistanceAgent = possibles[0];
		for (int i = 1; i < possibles.length; ++i)
			if (pop.get(possibles[i]).getX() < pop.get(leastDistanceAgent).getX())
				leastDistanceAgent = possibles[i];
		
		int mostJumpsAgent = possibles[0];
		for (int i = 1; i < possibles.length; ++i)
			if (pop.get(possibles[i]).getJumpCount() > pop.get(possibles[i]).getJumpCount())
				mostJumpsAgent = possibles[i];
		
		if (pop.get(leastDistanceAgent).getId() == pop.get(mostJumpsAgent).getId()) {
			pop.remove(leastDistanceAgent);
			pop.add(agent);
			return;
		}
		
		int bestAgent = -1;
		if ((pop.get(leastDistanceAgent).getX() / 4096.f) >= (1 - (pop.get(mostJumpsAgent).getJumpCount() / pop.get(mostJumpsAgent).getInstructionsLength())))
			bestAgent = mostJumpsAgent;
		else
			bestAgent = leastDistanceAgent;
			
		pop.remove(bestAgent);
		pop.add(agent);
		*/
		
		int worstAgentIndex = possibles[0];
		for (int i = 1; i < possibles.length; ++i)
			if (pop.get(possibles[i]).getX() < pop.get(worstAgentIndex).getX())
				worstAgentIndex = possibles[i];
		
		for (int i = 0; i < possibles.length; ++i)
			if ((pop.get(possibles[i]).getX() <= pop.get(worstAgentIndex).getX()) &&
					(pop.get(possibles[i]).getTimeLeft() < pop.get(worstAgentIndex).getTimeLeft()))
				worstAgentIndex = possibles[i];
		
		pop.remove(worstAgentIndex);
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
