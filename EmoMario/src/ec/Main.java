package ec;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

import ch.idsia.benchmark.tasks.BasicTask;
import ch.idsia.tools.MarioAIOptions;

public final class Main {

	public static void main(String[] args) {
		/*
		try {
			System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream("output.txt"))));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		long start = System.currentTimeMillis();
		SteadyStateESO ssga = new SteadyStateESO(4096, 100, 0.85f, 0.001f, 4, 1, 100);
		System.out.println("Initial best = " + ssga.bestAgent.getFitness());
		System.out.println(ssga.bestAgent);
		System.out.println("Initial average = " + ssga.getAverageFitness());
		ssga.run(1000000);
		System.out.println("Final best = " + ssga.getBest().getFitness());
		System.out.println(ssga.bestAgent);
		System.out.println("Final average = " + ssga.getAverageFitness());
		ssga.runVisualization(ssga.bestAgent);
		//System.out.println("Final pop = ");
		//ssga.printPop();
	    long stop = System.currentTimeMillis();
	    System.out.println("Total time == " + (stop - start) / 1000.0);
	}

}
