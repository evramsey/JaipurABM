package JaipurABM;

import org.graphstream.algorithm.generator.Generator;
import org.graphstream.algorithm.generator.RandomGenerator;
import org.graphstream.algorithm.generator.WattsStrogatzGenerator;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import sim.engine.SimState;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Scanner;


/**
 *A model of dual flush toilet adoption through a growing population.
 * @author E.V. Ramsey and E.Z. Berglund
 */
public class JaipurABM extends SimState {

	//graphStructure acts as the seed to determine which social network to implement
	//"original" is for network broken down by friends, acquaintances, and families, selected randomly
	//"kleinbergSmallWorldNetwork" is for Kleinberg Small World
	public static int graphStructureNum; //1 = random, 2 = random w/ social networks, 3 = WS small world
	public static Graph socialGraph;
	public static String resultsFileName;
	private static boolean has3Networks;
	public static double socialPressureAverage;
	public static double stdDevPressureDelta;
	public static int numRuns = 1;
	public static int numStepsInMain;
	public static String populationCSVfile;
	public static int numTotalAgents = 0;
	public static int currentJob = 0;
	public static int population;
	private static int vertexNumber = 0;
	public static ArrayList<Household> network;
	private static int avgNumConnections = 48;
	private static boolean lastYearWasDroughtYear = false;

	public JaipurABM(long seed) {
		super(seed);
	}

    /**
     *
     * @param avgConnections average number of connections between agents
     * @param A parameter for similar behavior
     * @param B parameter for dissimilar behavior
     * @param beta 
     * @param delta
     * @param stdDevDelta
     * @param exogTerm
     * @param exogTermDrought
     * @param numSkippedTalkSteps
     * @param numSKippedUtilUpdateSteps
     * @param n_jobs
     * @param population_file
     * @param outputFileName
     * @param deleteAutogen
     * @param networkStructure
     * @return val
     * @throws IOException
     */
	public static double runJobs(int avgConnections, double A, double B, double beta, double delta, double stdDevDelta, double exogTerm,
                                 double exogTermDrought, int numSkippedTalkSteps, int numSKippedUtilUpdateSteps, int n_jobs,
                                 String population_file, String outputFileName, boolean deleteAutogen,
                                 int networkStructure) throws IOException {
		avgNumConnections = avgConnections;
		double totalStandardError = 0.0;
		for (int i = 0; i < n_jobs; i++) {
			System.out.println("this job number: " + currentJob);
			totalStandardError += run(A, B, beta, delta, stdDevDelta, exogTerm, exogTermDrought, numSkippedTalkSteps, numSKippedUtilUpdateSteps,
					currentJob, population_file, outputFileName, deleteAutogen, networkStructure);
			currentJob++;
			File autoGen = new File("./output/" + outputFileName + "/autogen_output.csv");
			if(deleteAutogen){
				if(autoGen.exists()) {
					autoGen.delete();
					System.out.println("autogen deleted");
				}
			}
		}
		double val = totalStandardError / n_jobs;
		return val;
	}

	//delta and stdDevDelta only apply to three network structure, which we are no longer using, but we'll leave it in here rather than recoding.
	public static double run(double A, double B, double beta, double delta, double stdDevDelta, double exogTerm, double exogTermDrought, int numSkippedTalkSteps, int numSkippedUtilUpdateSteps,
							 int jobNum, String population_file, String outputFileName, boolean deleteAutogen, int netStructure) throws IOException {
		SimState state = new JaipurABM(System.currentTimeMillis());
		resultsFileName = outputFileName;
		DataCollector.outputFileIdentifier = resultsFileName;
		setModelInputs(A, B, beta, delta, stdDevDelta, exogTerm, exogTermDrought, numSkippedTalkSteps, numSkippedUtilUpdateSteps,population_file,
				outputFileName, deleteAutogen, netStructure);
		String in_file_name = "autogen.txt";
		DataCollector.in_filename = in_file_name;
		state.nameThread();
		initialize_agents();
		state.schedule.clear();
		state.setJob(0);
		state.start();
		do
			if (!state.schedule.step(state)) {
				break;
		} while (state.schedule.getSteps() < numStepsInMain);
		state.finish();
		state = null;
		double sn = DataCollector.calculateStandardError(jobNum);
		double nse = DataCollector.calculateNSE(jobNum);
		double mnse = DataCollector.calculate_mod_NSE(jobNum);
		numRuns++;
		return sn;
		//return -1;
	}

	public static void initialize_agents() {
		network = new ArrayList<Household>();
		//newAgentsAtThisTimeStepOriginalNetwork = new ArrayList<Household>();
		Household.houseHoldAgents = new ArrayList<>();
		numTotalAgents = 0;
		vertexNumber = 0;
		System.out.println("Network agents initialized");
	}

	public void start() {
		DataCollector dc = new DataCollector();
		schedule.clear();
		int[][] populationArray = scanInputCSV.popScan(populationCSVfile);
		super.start();
		createAgentPopulation(populationArray);
		createGraphAndNetwork();

		schedule.scheduleRepeating(0.1, dc); //puts JaipurABM.DataCollector on schedule
	}

	public void createGraphAndNetwork() {
		if (graphStructureNum == 3) {
			socialGraph = generateWattsStrogatzSmallWorldSocialNetwork();
		} else {
			socialGraph = generateRandomGraph();
		}
		for (Node node : socialGraph) {
			Household hh = network.get(node.getIndex());
			node.setAttribute("household", hh);
			Collection<Edge> edgeCollection = node.getLeavingEdgeSet();
			if (has3Networks) {
				for (Edge edge : edgeCollection) {
					addToNetworks(hh, node, edge);
					}
				for (Edge edge1 : edgeCollection) {
					if (!edge1.hasAttribute("relationship")) {
						Node oppNode = edge1.getOpposite(node);
						System.out.println("why don't " + node.getIndex() + " and " + oppNode.getIndex() + " have a relationship?");
						edge1.setAttribute("relationship", "acquaintance");
					}
				}
			} else{
				for (Edge edge : edgeCollection) {
					hh.increaseNumAcquaintances();
				}
			}
		}
	}

	private void addToNetworks(Household hh, Node node, Edge edge){
		int maxNumFamConnections = hh.getMaxNumFamilyMembers();
		int maxNumFriendConnections = hh.getMaxNumCloseFriends();
		Node oppNode = edge.getOpposite(node);
		Household oppHH = network.get(oppNode.getIndex());
		if(edge.hasAttribute("relationship")){
			return;
		}
		if (hh.currentNumFamilyMembers < maxNumFamConnections) {
			if (oppHH.currentNumFamilyMembers < oppHH.getMaxNumFamilyMembers()) {
				edge.setAttribute("relationship", "family");
				hh.increaseNumFamilyMembers();
				oppHH.increaseNumFamilyMembers();
				return;
			}
		}
		if (hh.currentNumCloseFriends < maxNumFriendConnections) {
			if (oppHH.currentNumCloseFriends < oppHH.getMaxNumCloseFriends()) {
				edge.setAttribute("relationship", "friend");
				hh.increaseNumFriends();
				oppHH.increaseNumFriends();
				return;
			}
		}
		edge.setAttribute("relationship", "acquaintance");
		hh.increaseNumAcquaintances();
		oppHH.increaseNumAcquaintances();
		if(hh.currentNumAcquaintances > node.getDegree()){
			System.out.println("adding too many acquaintances");
		}
	}


	private void checkAndUpdateNetworkSizes(Household hh, Collection<Edge> edgeCollection) {
		for (Edge edge : edgeCollection) {
			if (edge.hasAttribute("relationship")) {
				String relationship = edge.getAttribute("relationship");
				if (relationship.equals("family")) {
					hh.increaseNumFamilyMembers();
				} else if (relationship.equals("friend")) {
					hh.increaseNumFriends();
				} else if (relationship.equals("acquaintance")) {
					hh.increaseNumAcquaintances();
				} else {
					System.out.println("error in 3 networks relationship assignment");
					System.out.println(relationship);
				}

			}
		}
	}


	/*
    Create number of agents at [n] time step and store in agent array.
	 */
	public Household createNewAgent(double timeStep) {
		Household hh = new Household(vertexNumber, timeStep);   //passes that property array to the new agents
		vertexNumber++;
		network.add(hh);         //Add household agent object to our household agent list
		return hh;
	}

	protected void createAgentPopulation(int[][] populationArray) {
		int numTimeSteps = populationArray.length;
		for (int i = 0; i < numTimeSteps; i++) {                                            //for each time step i
			double double_i = (double) (i);
			population = populationArray[i][1];
			int numNewAgents = getNumNewAgents(populationArray, double_i);
			for (int j = 0; j < numNewAgents; j++) {                                        //create number of new agents required
				Household newAgent = createNewAgent(double_i);
				Household.houseHoldAgents.add(newAgent);
				schedule.scheduleRepeating(double_i, newAgent);
				numTotalAgents++;
			}
		}
	}

	protected Graph generateRandomGraph() {
		Graph graph = new SingleGraph("Random");
		Generator gen = new RandomGenerator(avgNumConnections);
		gen.addSink(graph);
		gen.begin();
		while (graph.getNodeCount() < network.size() && gen.nextEvents()) ;
		gen.end();
		return graph;
	}


	private static int getNumNewAgents(int[][] population, double timeStep) {
		int intTimeStep = (int) timeStep;
		int popPreviousTimeStep;
		int numNewHouseholds;
		int numTotalTimeSteps = population.length;
		if (intTimeStep < 0 || intTimeStep > numTotalTimeSteps - 1) {
			System.out.println("incorrect time step JaipurABM.getNumNewagents");
			System.exit(1);
		}
		int popCurrentTimeStep = population[intTimeStep][1];
		if (intTimeStep > 1 || intTimeStep == 1) {
			popPreviousTimeStep = population[intTimeStep - 1][1];
			numNewHouseholds = popCurrentTimeStep - popPreviousTimeStep;
			return numNewHouseholds;
		} else {
			numNewHouseholds = popCurrentTimeStep;
			return numNewHouseholds;
		}
	}

	public static int getCurrentJob() {
		return currentJob;
	}


	public Graph generateWattsStrogatzSmallWorldSocialNetwork() {
		Graph graph = new SingleGraph("JaipurResidents");
		if (numTotalAgents > 1) {
			Generator gen = new WattsStrogatzGenerator(numTotalAgents, avgNumConnections, 0.25);//n number of agents, num connections k, rewiring probability beta
			gen.addSink(graph);
			gen.begin();
			while (gen.nextEvents()) {
				//sleep();
			}
			gen.end();
			//graph.display(true);
			return graph;
		} else {
			System.out.println("numTotalAgents is failing");
			System.exit(0);
			return graph;
		}
	}
//
//	protected void sleep() {
//		try {
//			Thread.sleep(100);
//		} catch (Exception e) {
//		}
//	}

	private static void setModelInputs(double A, double B, double beta, double delta, double stdDevDelta, double exogTerm, double exogTermDrought, int numSkippedTalkSteps,
									   int numSkippedUtilitySteps, String popFileName, String outFileName, boolean deleteAutogen,
									   int netStructure) throws FileNotFoundException {
		UtilityFunction.setAandBPrime(A);
		UtilityFunction.setBandAPrime(B);
		Household.setExogenousTermSeed(exogTerm);
		Household.setExogenousTermDroughtSeed(exogTermDrought);
		ProbabilityOfBehavior.setBeta(beta);
		socialPressureAverage = delta;
		stdDevPressureDelta = stdDevDelta;
		Household.setUtilSkipStepSeed(numSkippedUtilitySteps);
		Household.setTalkSkipStepSeed(numSkippedTalkSteps);
		Household.setPercentConsEachTimeStep(0);
		//Household.setPercentInitialCons(0.263); //model percentage at end of 2015/beginning of 2016
		Household.setPercentInitialCons(0); //model percentage at start of 1996
		resultsFileName = outFileName;
		graphStructureNum = netStructure;
		if(graphStructureNum == 1 || graphStructureNum == 3){
			has3Networks = false;
		}
		else if(graphStructureNum == 2) {
			has3Networks = true;
		}
		else{
			System.out.println("no network structure specified in JaipurABM.setModelInputs, exiting");
			System.exit(1);
		}
		populationCSVfile = popFileName;
		DataCollector.setDeleteAutogenTxt(deleteAutogen);
		int lineNum = 0;
		Scanner popScan = null;
		File popFile = new File(populationCSVfile);
		try {
			popScan = new Scanner(popFile);
		} catch (FileNotFoundException e) {
			System.out.println("error in reading file in JaipurABM.setModelInputs");
		}
		while (popScan.hasNextLine()) {
			popScan.nextLine();
			lineNum++;
		}
		numStepsInMain = lineNum * 2;
	}

	public static int getCommunicatedAgent(int vertexIndex, double timeStep){
		Node thisVertNode = socialGraph.getNode(vertexIndex);
		Collection<Edge> edgeCollection = thisVertNode.getLeavingEdgeSet();
		ArrayList<Integer> oppNodeList = new ArrayList<>();
		for (Edge edge : edgeCollection) {
			Node oppositeNode = edge.getOpposite(thisVertNode);
			oppNodeList.add(oppositeNode.getIndex());
		}
		Collections.shuffle(oppNodeList);
		int i = 0;
		for(int n: oppNodeList){
			Household hh = network.get(n);
			if(hh.existsAtTimeStep(timeStep)){
				return oppNodeList.get(i);
			}
			i++;
		}
		return -1;
	}

	public static int getTypeOfRelationship(int thisVertexIndex, int oppVertexIndex){
		Node thisVertNode = socialGraph.getNode(thisVertexIndex);
		Node oppVertNode = socialGraph.getNode(oppVertexIndex);
		Edge relationshipEdge = thisVertNode.getEdgeBetween(oppVertexIndex);
		String relationship = "";
		if(relationshipEdge.hasAttribute("relationship")) {
			relationship = relationshipEdge.getAttribute("relationship");
			if (relationship.equalsIgnoreCase("family")) {
				return 0;
			} else if (relationship.equalsIgnoreCase("friend")) {
				return 1;
			} else if (relationship.equalsIgnoreCase("acquaintance")) {
				return 2;
			} else {
				return -1;
			}
		}
		else{
			System.out.println(thisVertNode.getIndex() + " and " + oppVertexIndex + "have no relationships");
			return -1;
		}
	}

	public static boolean doesModelHave3Networks(){return has3Networks;}

	public static int getGraphStructureNum(){return graphStructureNum;}

	public static void setLastYearDroughtIndex(boolean bool){ lastYearWasDroughtYear = bool;}

	public static boolean getLastYearDroughtIndex(){return lastYearWasDroughtYear;}

}

