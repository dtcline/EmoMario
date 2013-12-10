package ec;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

import ch.idsia.benchmark.tasks.BasicTask;
import ch.idsia.tools.MarioAIOptions;

public final class Main {

	// machineName numberOfIterations difficulty rngSeed
	public static void main(String[] args) {
		
		//int[] instructions;
		//instructions = new int[]{ 0, 1, 1, 1, 0, 0, 0, 1, 1, 0, 0, 0, 0, 1, 0, 1, 1, 1, 1, 1, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 1, 0, 1, 0, 1, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 0, 1, 0, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 0, 1, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1, 1, 1, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 1, 0, 1, 1, 0, 1, 0, 1, 1, 1, 0, 1, 1, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 1, 1, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 0, 1, 0, 1, 0, 1, 0, 0, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1, 1, 1, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 1, 0, 1, 1, 0, 0, 0, 1, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 0, 0, 1, 1, 0, 1, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 1, 1, 0, 1, 0, 1, 0, 0, 0, 1, 0, 1, 0, 0, 1, 1, 1, 0, 0, 1, 1, 1, 1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 1, 1, 1, 1, 0, 0, 1, 1, 1, 0, 1, 1, 0, 1, 0, 1, 1, 0, 1, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0, 1, 0, 1, 1, 0, 1, 0, 1, 1, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 1, 0, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 1, 1, 1, 1, 0, 1, 1, 0, 0, 0, 1, 1, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 1, 1, 0, 1, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 1, 0};
		//runRunner(instructions);
		
		//runSteadyState(args);
		
		runEmo(args); 
		
	}
	
	static void runRunner(int[] instructions) {
		int difficulty = 1;
		int rngSeed = 5;
		// darwin0
		SingleFitnessAgent agent = new SingleFitnessAgent(instructions);
		final MarioAIOptions marioAIOptions = new MarioAIOptions(new String[0]);
		marioAIOptions.setVisualization(true);
		marioAIOptions.setLevelDifficulty(difficulty);
		marioAIOptions.setLevelRandSeed(rngSeed);
		final BasicTask basicTask = new BasicTask(marioAIOptions);
		basicTask.setOptionsAndReset(marioAIOptions);
		marioAIOptions.setAgent(agent);
		basicTask.doEpisodes(1, false, 1);
	}
	
	static void runSteadyState(String[] args) {
		String  machineName = "mac";//args[0];
		int numberOfIterations = 1;//Integer.valueOf(args[1]);
		int difficulty = 1;//Integer.valueOf(args[2]);
		int rngSeed = 5;//Integer.valueOf(args[3]);
		for (int i = 0; i < numberOfIterations; ++i) {
			PrintWriter pw = null;
			try {
				pw = new PrintWriter(new File(machineName + "_" + i + "_output.txt"));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				System.exit(1);
			}
			long start = System.currentTimeMillis();
			pw.println(Arrays.toString(args) + "\n");
			// 3 is a good seed for diff==0 (202 distance), 5 is a good seed for diff==1 (126 distance)
			SteadyStateESO ssga = new SteadyStateESO(pw, 4096, 0.5f, 100, 0.85f, 0.001f, 4, difficulty, 100000, 
					rngSeed);
			
			// print initials
			StringBuilder sb = new StringBuilder();
			
			sb.append("Initial best = " + ssga.bestAgent.getFitness()).append("\n");
			sb.append(ssga.bestAgent).append("\n");
			sb.append("Initial average = " + ssga.getAverageFitness()).append("\n\n");
			System.out.println(sb.toString());
			pw.println(sb.toString());
			
			ssga.run(10000);
			
			// print finals
			sb = new StringBuilder();
			
			sb.append("Final best = " + ssga.getBest().getFitness()).append("\n");
			sb.append(ssga.bestAgent).append("\n");
			sb.append("Final average = " + ssga.getAverageFitness()).append("\n");
			
			// print pop
			sb.append("Final pop = ").append("\n");
			String popString = ssga.popAsString();
			sb.append(popString).append("\n\n");
			System.out.println(sb.toString());
			pw.println(sb.toString());
			
		    long stop = System.currentTimeMillis();
		    String timeString ="Total time == " + (stop - start) / 1000.0; 
		    System.out.println(timeString);
		    pw.println(timeString);
		    pw.flush();
		}
	}
		
	static void runEmo(String[] args) {
		String  machineName = args[0];
		int numberOfIterations = Integer.valueOf(args[1]);
		int difficulty = Integer.valueOf(args[2]);
		int rngSeed = Integer.valueOf(args[3]);
		for (int i = 0; i < numberOfIterations; ++i) {
			PrintWriter pw = null;
			try {
				pw = new PrintWriter(new File(machineName + "_" + i + "_output.txt"));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				System.exit(1);
			}
			long start = System.currentTimeMillis();
			pw.println(Arrays.toString(args) + "\n");
			// 3 is a good seed for diff==0 (202 distance), 5 is a good seed for diff==1 (126 distance)
			EMOSteadyState ssga = new EMOSteadyState(pw, 4096, 0.5f, 100, 0.85f, 0.001f, 4, difficulty, 100000, 
					rngSeed);
			
			// print initials
			StringBuilder sb = new StringBuilder();
			
			sb.append("Initial best = " + ssga.bestAgent.getFitness()).append("\n");
			sb.append(ssga.bestAgent).append("\n");
			sb.append("Initial average = " + ssga.getAverageFitness()).append("\n\n");
		
			sb.append("Initial # non-dominated agents == ").append(ssga.bestList.size()).append("\n");
			for (SingleFitnessAgent agent : ssga.bestList)
				sb.append(agent).append("\n");
			sb.append("\n");
			System.out.println(sb.toString());
			pw.println(sb.toString());
			
			ssga.run(1000000);
			
			// print finals
			sb = new StringBuilder();
			
			sb.append("Final best = " + ssga.bestAgent.getFitness()).append("\n");
			sb.append(ssga.bestAgent).append("\n");
			sb.append("Final average = " + ssga.getAverageFitness()).append("\n\n");
			
			sb.append("Final # non-dominated agents == ").append(ssga.bestList.size()).append("\n");
			for (SingleFitnessAgent agent : ssga.bestList)
				sb.append(agent).append("\n");
			sb.append("\n");
			
			// print pop
			sb.append("Final pop = ").append("\n");
			String popString = ssga.popAsString();
			sb.append(popString).append("\n\n");
			System.out.println(sb.toString());
			pw.println(sb.toString());
			
		    long stop = System.currentTimeMillis();
		    String timeString ="Total time == " + (stop - start) / 1000.0 + "\n\n"; 
		    System.out.println(timeString);
		    pw.println(timeString);
		    
		    pw.flush();
		}
	}

}
