package JaipurABM;

import org.graphstream.algorithm.generator.Generator;
import org.graphstream.algorithm.generator.WattsStrogatzGenerator;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import sim.engine.SimState;

import java.io.*;
import java.util.*;



/**
 *
 * @author lizramsey
 *
 */


public class JaipurABM extends SimState {

	//graphStructure acts as the seed to determine which social network to implement
	//"original" is for network broken down by friends, acquaintances, and families, selected randomly
	//"kleinbergSmallWorldNetwork" is obviously for Kleinberg Small World
	public static String graphStructure;
	public static Graph graph;
	public static String resultsFileName;
	public static double socialPressureAverage;
	public static double stdDevPressureDelta;
	public static int numRuns = 1;
	public static int numStepsInMain;
	public static double averageHouseholdSize = 5.1;
	public static String populationCSVfile;
	public static int numTotalAgents = 0;
	public static int currentJob = 0;
	public static int population;
	private static int vertexNumber = 0;
	public static ArrayList<Household> network = new ArrayList<Household>();
	public static ArrayList<Household> newAgentsAtThisTimeStepOriginalNetwork = new ArrayList<Household>();
	static ArrayList<Household> neighborArray = new ArrayList<Household>();
	Household neighborHousehold = new Household();

	public JaipurABM(long seed) {
		super(seed);
	}

	public static double run_jobs(double A, double B, double beta, double delta, double stdDevDelta, int n_jobs,
								  String population_file, String outputFileName, boolean deleteAutogen, int networkStructure) throws IOException {
		int thisJob = 0;
		double avgR2 = 0;
		for (int i = 0; i < n_jobs; i++) {
			System.out.println("this job number: " + thisJob);
			avgR2 += run(A, B, beta, socialPressureAverage, stdDevDelta, thisJob, population_file, outputFileName, deleteAutogen, networkStructure);
			thisJob++;
			File autoGen = new File("./output/" + outputFileName + "/autogen_output.csv");
			if(deleteAutogen){
				if(autoGen.exists()) {
					autoGen.delete();
					System.out.println("autogen deleted");
				}
			}
		}

		return avgR2 / n_jobs;

	}

	public static double run(double A, double B, double beta, double delta, double stdDevDelta, int jobNum,
							 String population_file, String outputFileName, boolean deleteAutogen, int netStructure) throws IOException {
		SimState state = new JaipurABM(System.currentTimeMillis());
		resultsFileName = outputFileName;
		DataCollector.outputFileIdentifier = resultsFileName;
		setModelInputs(A, B, beta, delta, stdDevDelta, population_file, outputFileName, deleteAutogen, netStructure);
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
		double r2 = DataCollector.calculateR2(jobNum);
		numRuns++;
		return r2;
	}

	public static void initialize_agents() {
		network = new ArrayList<Household>();
		newAgentsAtThisTimeStepOriginalNetwork = new ArrayList<Household>();
		neighborArray = new ArrayList<Household>();

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
		if (!graphStructure.equalsIgnoreCase("original")) {    //the original network allows each household to store its own network;
			//all others take their network structure from the ABM itself
			if (graphStructure.equalsIgnoreCase("watts strogatz small world network")) {
				//call graph generator
				graph = generateWattsStrogatzSmallWorldSocialNetwork();
				createSocialNetwork(graph);
			} else {
				System.out.println("no network structure identified, exiting");
				System.exit(1);
			}
		}
		schedule.scheduleRepeating(0.1, dc); //puts JaipurABM.DataCollector on schedule
	}


	/*
    Create number of agents at [n] time step and store in agent array.
	 */
	public Household createNewAgent(int[][] populationArray, double timeStep) {
		Household hh = new Household(vertexNumber, timeStep);   //passes that property array to the new agents
		vertexNumber++;
		network.add(hh);         //Add household agent object to our household agent list
		return hh;
	}

	protected void createAgentPopulation(int[][] populationArray) {
		int numTimeSteps = populationArray.length;
		for (int i = 0; i < numTimeSteps; i++) {                                            //for each time step i
			newAgentsAtThisTimeStepOriginalNetwork.clear();                                //remove all old agents from new agent array
			double double_i = (double) (i);
			population = populationArray[i][1];
			int numNewAgents = getNumNewAgents(populationArray, double_i);
			for (int j = 0; j < numNewAgents; j++) {                                        //create number of new agents required
				Household newAgent = createNewAgent(populationArray, double_i);
				Household.houseHoldAgents.add(newAgent);
				schedule.scheduleRepeating(double_i, newAgent);
				numTotalAgents++;
			}
		}
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

	public static String getGraphStructure() {
		return graphStructure;
	}

	public Graph generateWattsStrogatzSmallWorldSocialNetwork() {
		Graph graph = new SingleGraph("JaipurResidents");
		if (numTotalAgents > 1) {
			Generator gen = new WattsStrogatzGenerator(numTotalAgents, 48, 0.25);//n number of agents, num connections k, rewiring probability beta
			gen.addSink(graph);
			gen.begin();
			while (gen.nextEvents()) {
				//sleep();
			}
			gen.end();
			graph.display(false);
			return graph;
		} else {
			System.out.println("numTotalAgents is failing");
			System.exit(0);
			return graph;
		}
	}

	protected void sleep() {
		try {
			Thread.sleep(100);
		} catch (Exception e) {
		}
	}

	private void createSocialNetwork(Graph graph) {
		assignUUIDsToGraph(graph);
		for (Node n : graph) {
			assignNeighborArray(n);

		}
	}

	private void assignUUIDsToGraph(Graph graph) {
		int i = 0;
		for (Node n : graph) {
			UUID correspondingHhUUID = network.get(i).getUUID();
			n.setAttribute("nodeUUID", correspondingHhUUID);
			i++;
		}
	}

	private void assignNeighborArray(Node node) {
//		Household thisNodeCorrespondingHH = findHouseHoldFromUUID(node.getAttribute("nodeUUID"));
		neighborArray = new ArrayList<Household>();
		neighborHousehold = null;
		Collection<Edge> edgeCollection = node.getLeavingEdgeSet();
		for (Edge edge : edgeCollection) {
			Node oppositeNode = edge.getOpposite(node);
			UUID oppositeNodeUUID = oppositeNode.getAttribute("nodeUUID");
			neighborHousehold = findHouseHoldFromUUID(oppositeNodeUUID);
			neighborArray.add(neighborHousehold);
		}
		node.setAttribute("neighborArray", neighborArray);
	}


	private Household findHouseHoldFromUUID(UUID uuidFind) {
		for (Household hh : network) {
			if (hh.uuid == uuidFind) {
				return hh;
			}
		}
		System.out.println("findHouseholdFromUUID isn't working");
		return null;
	}


	public static Graph getGraph() {
		return graph;
	}

	private static void setModelInputs(double A, double B, double beta, double delta, double stdDevDelta,
									   String popFileName, String outFileName, boolean deleteAutogen,
									   int netStructure) throws FileNotFoundException {
		UtilityFunction.setAandBPrime(A);
		UtilityFunction.setBandAPrime(B);
		ProbabilityOfBehavior.setBeta(beta);
		socialPressureAverage = delta;
		stdDevPressureDelta = stdDevDelta;
		Household.setPercentConservers(0.005);
		resultsFileName = outFileName;
		if(netStructure == 1){
			graphStructure = "original";
		}
		else if(netStructure == 2){
			graphStructure = "watts strogatz small world network";
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

}

