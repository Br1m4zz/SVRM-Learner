/*
 *  Copyright (c) 2016 Joeri de Ruiter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package nl.cypherpunk.SVCSLearner;

import de.learnlib.acex.analyzers.AcexAnalyzers;
import de.learnlib.algorithms.dhc.mealy.MealyDHC;
import de.learnlib.algorithms.kv.mealy.KearnsVaziraniMealy;
import de.learnlib.algorithms.lstar.mealy.ExtensibleLStarMealyBuilder;
import de.learnlib.algorithms.malerpnueli.MalerPnueliMealy;
import de.learnlib.algorithms.rivestschapire.RivestSchapireMealy;
import de.learnlib.api.SUL;
import de.learnlib.api.algorithm.LearningAlgorithm;
import de.learnlib.api.logging.LearnLogger;
import de.learnlib.api.oracle.EquivalenceOracle;
import de.learnlib.api.query.DefaultQuery;
import de.learnlib.filter.cache.mealy.MealyCacheOracle;
import de.learnlib.filter.statistic.Counter;
import de.learnlib.filter.statistic.oracle.MealyCounterOracle;
import de.learnlib.oracle.equivalence.*;
import de.learnlib.oracle.membership.SULOracle;
import de.learnlib.util.statistics.SimpleProfiler;
import net.automatalib.automata.transducers.MealyMachine;
import net.automatalib.serialization.dot.GraphDOT;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import nl.cypherpunk.SVCSLearner.LogOracle.MealyLogOracle;
import nl.cypherpunk.SVCSLearner.SVCS.SVCSSUL;
import nl.cypherpunk.SVCSLearner.SVCS.SVCSConfig;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Random;

/**
 * @author Joeri de Ruiter (joeri@cs.ru.nl)
 */
public class Learner {
	LearningConfig config;
	Alphabet<String> alphabet;
	boolean combine_query = false;
	public SUL<String, String> sul;
	SULOracle<String, String> memOracle;
	MealyLogOracle<String, String> logMemOracle;
	MealyCounterOracle<String, String> statsMemOracle;
	MealyCacheOracle<String, String> cachedMemOracle;
	MealyCounterOracle<String, String> statsCachedMemOracle;
	LearningAlgorithm<MealyMachine<?, String, ?, String>, String, Word<String>> learningAlgorithm;

	SULOracle<String, String> eqOracle;
	MealyLogOracle<String, String> logEqOracle;
	MealyCounterOracle<String, String> statsEqOracle;
	MealyCacheOracle<String, String> cachedEqOracle;
	MealyCounterOracle<String, String> statsCachedEqOracle;
	EquivalenceOracle<MealyMachine<?, String, ?, String>, String, Word<String>> equivalenceAlgorithm;
	
	public Learner(LearningConfig config) throws Exception {
		this.config = config;
		
		// Create output directory if it doesn't exist
		Path path = Paths.get(config.output_dir);
		if(Files.notExists(path)) {
			Files.createDirectories(path);
		}
		
		LearnLogger log = LearnLogger.getLogger(Learner.class.getSimpleName());
		log.logPhase("Initialzing SV-CS Model with configuration");
		sul = new SVCSSUL(new SVCSConfig(config));
		alphabet =((SVCSSUL)sul).getalAlphabet();

		loadLearningAlgorithm(config.learning_algorithm, alphabet, sul);
		loadEquivalenceAlgorithm(config.eqtest, alphabet, sul);
	}

	public void loadLearningAlgorithm(String algorithm, Alphabet<String> alphabet, SUL<String, String> sul) throws Exception {

		logMemOracle = new MealyLogOracle<String, String>(sul, LearnLogger.getLogger("learning_queries"));
		statsMemOracle = new MealyCounterOracle<String, String>(logMemOracle, "membership queries to SUL");

		cachedMemOracle = MealyCacheOracle.createDAGCacheOracle(alphabet, statsMemOracle);
		statsCachedMemOracle = new MealyCounterOracle<String, String>(statsMemOracle, "membership queries to cache");
		switch(algorithm.toLowerCase()) {
			case "lstar":
				learningAlgorithm = new ExtensibleLStarMealyBuilder<String, String>().withAlphabet(alphabet).withOracle(statsCachedMemOracle).create();
				break;
				 	
			case "dhc":
				learningAlgorithm = new MealyDHC<String, String>(alphabet, statsCachedMemOracle);
				break;
				
			case "kv":
				learningAlgorithm = new KearnsVaziraniMealy<String, String>(alphabet, statsCachedMemOracle, true, AcexAnalyzers.BINARY_SEARCH_BWD);
				break;
				
				
			case "mp":
				learningAlgorithm = new MalerPnueliMealy<String, String>(alphabet, statsCachedMemOracle);
				break;
				
			case "rs":
				learningAlgorithm = new RivestSchapireMealy<String, String>(alphabet, statsCachedMemOracle);
				break;

			default:
				throw new Exception("Unknown learning algorithm " + config.learning_algorithm);
		}		
	}
	

	public void loadEquivalenceAlgorithm(String algorithm, Alphabet<String> alphabet, SUL<String, String> sul) throws Exception {

		logEqOracle = new MealyLogOracle<String, String>(sul, LearnLogger.getLogger("equivalence_queries"));
		statsEqOracle = new MealyCounterOracle<String, String>(logEqOracle, "equivalence queries to SUL");
		cachedEqOracle = MealyCacheOracle.createDAGCacheOracle(alphabet, statsEqOracle);
		statsCachedEqOracle = new MealyCounterOracle<String, String>(cachedMemOracle, "equivalence queries to cache");

		switch(algorithm.toLowerCase()) {
			case "wmethod":
				equivalenceAlgorithm = new MealyWMethodEQOracle<String, String>(statsCachedMemOracle,0,config.max_depth);
				break;

			case "modifiedwmethod":
				equivalenceAlgorithm = new ModifiedWMethodEQOracle.MealyModifiedWMethodEQOracle<String, String>(config.max_depth, statsCachedMemOracle);
				break;
				
			case "wpmethod":
				equivalenceAlgorithm = new MealyWpMethodEQOracle<String, String>(statsCachedMemOracle,0,config.max_depth);
				break;
				
			case "randomwords":
				equivalenceAlgorithm = new MealyRandomWordsEQOracle<String, String>(statsCachedMemOracle, config.min_length, config.max_length, config.nr_queries, new Random(config.seed));
				break;
				
			default:
				throw new Exception("Unknown equivalence algorithm " + config.eqtest);
		}	
	}
	
	public void learn() throws IOException, InterruptedException {
		LearnLogger log = LearnLogger.getLogger(Learner.class.getSimpleName());

		log.logPhase( "Using learning algorithm " + learningAlgorithm.getClass().getSimpleName());
		log.logPhase( "Using equivalence algorithm " + equivalenceAlgorithm.getClass().getSimpleName());

		log.logPhase( "Starting learning");
		write_learninglog("Starting learning");
		SimpleProfiler.start("Total time");
		
		boolean learning = true;
		Counter round = new Counter("Rounds", "");

		round.increment();
		System.out.println("Starting round " + round.getCount());
		write_learninglog("Starting round"+ round.getCount());
		SimpleProfiler.start("Learning");
		learningAlgorithm.startLearning();
		SimpleProfiler.stop("Learning");

		MealyMachine<?, String, ?, String> hypothesis = learningAlgorithm.getHypothesisModel();
		while(learning) {
			// Write outputs
			writeDotModel(hypothesis, alphabet, config.output_dir + "/hypothesis_" + round.getCount() + ".dot");

			System.out.println("Searching for counter-example");
			write_learninglog("Searching for counter-example");
			SimpleProfiler.start("Searching for counter-example");
			DefaultQuery<String, Word<String>> counterExample = equivalenceAlgorithm.findCounterExample(hypothesis, alphabet);	
			SimpleProfiler.stop("Searching for counter-example");

			if(counterExample == null) {
				learning = false;

				writeDotModel(hypothesis, alphabet, config.output_dir + "/learnedModel.dot");
				//writeAutModel(hypothesis, alphabet, config.output_dir + "/learnedModel.aut");
			}
			else {			
				// Counter example found, update hypothesis and continue learning
				System.out.println("Counter-example found: " + counterExample.toString());
								//TODO Add more logging
				write_learninglog("Counter-example found: " + counterExample.toString());				
				round.increment();

				System.out.println("Starting round " + round.getCount());
				write_learninglog("Starting round"+ round.getCount());
				SimpleProfiler.start("Learning");
				learningAlgorithm.refineHypothesis(counterExample);
				SimpleProfiler.stop("Learning");

				hypothesis = learningAlgorithm.getHypothesisModel();
			}
		}

		SimpleProfiler.stop("Total time");
		
		// Output statistics
		System.out.println("-------------------------------------------------------");
		System.out.println( SimpleProfiler.getResults());
		System.out.println(round.getSummary());
		System.out.println(statsMemOracle.getStatisticalData().getSummary());
		System.out.println(statsCachedMemOracle.getStatisticalData().getSummary());
		System.out.println(statsEqOracle.getStatisticalData().getSummary());
		System.out.println(statsCachedEqOracle.getStatisticalData().getSummary());
		System.out.println( "States in final hypothesis: " + hypothesis.size());
		write_learninglog("-------------------------------------------------------");
		write_learninglog(SimpleProfiler.getResults());
		write_learninglog(round.getSummary());
		write_learninglog(statsMemOracle.getStatisticalData().getSummary());
		write_learninglog(statsCachedMemOracle.getStatisticalData().getSummary());
		write_learninglog(statsEqOracle.getStatisticalData().getSummary());
		write_learninglog(statsCachedEqOracle.getStatisticalData().getSummary());
		write_learninglog("States in final hypothesis: " + hypothesis.size());

	}
	
	public static void writeAutModel(MealyMachine<?, String, ?, String> model, Alphabet<String> alphabet, String filename) throws FileNotFoundException {
		// Make use of LearnLib's internal representation of states as integers
		@SuppressWarnings("unchecked")
		MealyMachine<Integer, String, ?, String> tmpModel = (MealyMachine<Integer, String, ?, String>) model;
		
		// Write output to aut-file
		File autFile = new File(filename);
		PrintStream psAutFile = new PrintStream(autFile);
		
		int nrStates = model.getStates().size();
		// Compute number of transitions, assuming the graph is complete
		int nrTransitions = nrStates * alphabet.size();
		
		psAutFile.println("des(" + model.getInitialState().toString() + "," + nrTransitions + "," + nrStates + ")");
		
		Collection<Integer> states = tmpModel.getStates();

		for(Integer state: states) {
			for(String input: alphabet) {
				String output = tmpModel.getOutput(state, input);
				Integer successor = tmpModel.getSuccessor(state, input);
				psAutFile.println("(" + state + ",'" + input + " / " + output + "', " + successor + ")");
			}
		}
		
		psAutFile.close();
	}
	
	public static void writeDotModel(MealyMachine<?, String, ?, String> model, Alphabet<String> alphabet, String filename) throws IOException, InterruptedException {
		// Write output to dot-file
		File dotFile = new File(filename);
		PrintStream psDotFile = new PrintStream(dotFile);
		GraphDOT.write(model, alphabet, psDotFile);
		psDotFile.close();
		
		//TODO Check if dot is available
		
		// Convert .dot to .pdf
		Runtime.getRuntime().exec("dot -Tpdf -O " + filename);
	}

	public void write_learninglog(String info){
		long cur_time = System.currentTimeMillis();
		String timeinfo = "["+cur_time+"]";
		File file = new File(config.output_dir + "/learning_log.txt");
		if (!file.exists()) {
			try {
				file.createNewFile();
			}catch (IOException e){
				e.printStackTrace();
			}
		}

		try{
			BufferedWriter  bfw = new BufferedWriter(new FileWriter(file,true));
			bfw.write(timeinfo+info);
			bfw.newLine();
			bfw.close();
		}catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//learner
	public static void main(String[] args) throws Exception {
		if(args.length < 1) {
			System.err.println("Invalid number of parameters");
			System.exit(-1);
		}
		if(args[0]==null){
			System.err.println("Invalid number of parameters");
			System.exit(-1);
		}
		
		LearningConfig config = new LearningConfig(args[0]);
	
		Learner learner = new Learner(config);
		learner.learn();
		
		System.exit(0);
	}
}
