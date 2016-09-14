

import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;

import org.apache.commons.collections15.Factory;
import org.graphstream.algorithm.generator.Generator;
import org.graphstream.algorithm.generator.GridGenerator;
import org.graphstream.algorithm.generator.WattsStrogatzGenerator;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;

import sim.engine.*;
import ec.util.MersenneTwisterFast;


/**
 *
 * @author lizramsey
 *
 */


public class JaipurABM extends SimState{

	//graphStructure acts as the seed to determine which social network to implement
	//"original" is for network broken down by friends, acquaintances, and families, selected randomly
	//"kleinbergSmallWorldNetwork" is obviously for Kleinberg Small World
	public static String graphStructure;
	public static int jobs;
	//public static int numStepsInMain = 240;	//Update this for each run as you add more timesteps in excel doc 
	//also,should be double the number of lines of data in excel file, since datacollector needs its own step
	public static int numStepsInMain;
	public static double averageHouseholdSize = 5.1;
	public static String populationCSVfile = "/Users/lizramsey/Documents/workspace/JaipurABM/src/AgentPopulation.csv";
	//public static String populationCSVfile = "/Users/lizramsey/Documents/workspace/JaipurABM/src/PopTest.csv";
	public static String outputFileName;
	public static String dataSourceFile ="/Users/lizramsey/Documents/workspace/JaipurABM/src/Initialization_Parameters.txt";
	public static int numStepsSkippedToUpdateFunctions;
	public static int numStepsSkippedToUpdateUtilityFunctions;
	public static int numStepsSkippedToUpdateTalkFunction;
	
	public static int numTotalAgents = 0;
	public static String txtFileInput;

	public static int currentJob = 0;
	public static double percentageConserverCurrent = 0.0;
	public static int population;
	public static int numCurrentAgents = 0;
	private static int vertexNumber = 0;
	//don't forget to account for this in documentation; numAgents will say 200, for example, but there will really be 201

	public static ArrayList<Household> network = new ArrayList<Household>();
	public static ArrayList<Household> newAgentsAtThisTimeStepOriginalNetwork = new ArrayList<Household>();
	static ArrayList<Household> neighborArray = new ArrayList<Household>();
	Household neighborHousehold = new Household();

	public JaipurABM(long seed){
		super(seed);
	}

	public static void main(String[] args) {

		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		String formattedDate = sdf.format(date);

		DataCollector.time_simulation_start = formattedDate;

		File[] files = new File("./input").listFiles();
		//If this pathname does not denote a directory, then listFiles() returns null.

		for (File file : files) {
			if (file.isFile()) {
				DataCollector.in_filename = file.getName();
				runSimulation(file.getAbsolutePath());
			}
		}

		try {
			DataCollector.aggregateResults();
		} catch (IOException e) {
			e.printStackTrace();
		}

//		txtFileInput = txtFileInput + DataCollector.txtFileInput;
//		generateTxtFile(txtFileInput);
		System.out.println("all runs finished, exiting");
		System.exit(0);
	}

	public static void runSimulation()
	{
		runSimulation(dataSourceFile);
	}

	public static void runSimulation(String input_file){
		SimState state = new JaipurABM(System.currentTimeMillis());
		scanInputCSV.readInData(input_file);
		System.out.println("num skipped steps " + numStepsSkippedToUpdateUtilityFunctions);

		currentJob = 0;

		for(int job = 0; job < jobs; job++){
			initialize_agents();

			System.out.println("JOB NUMBER " + job);
			state.schedule.clear();
			state.setJob(job);
			currentJob++;
			state.start();
			txtFileInput = "Utility Function Values\ta value\t" + UtilityFunction.a + "\ta prime value\t" + UtilityFunction.aPrime +
					"\tb value\t" + UtilityFunction.b + "\tb prime value\t" + UtilityFunction.bPrime + "\texogenous term\t" +
					UtilityFunction.exogenousTerm + "\tbeta\t" + ProbabilityOfBehavior.beta + "\tdelta\t" +
					UtilityFunction.parameterDelta + "\tNum skipped steps to update utility\t" + numStepsSkippedToUpdateUtilityFunctions +
					"\tnum skipped steps to update talk function\t" + numStepsSkippedToUpdateTalkFunction+"\n\n";
			txtFileInput =  txtFileInput + "Job Number\tTime Step\tModel Population\t# of Agents\t# of Conservers\tRatio of Conservers to Total Agents\tTotal Demand\n";		//create .txt file for outputting results
			do
				if (!state.schedule.step(state)) {
					break;
				}
			while(state.schedule.getSteps() < numStepsInMain);
			state.finish();
		}
	}

	public static void initialize_agents(){
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
		double totalDemand = 0.0;
		schedule.clear();
		int[][] populationArray = scanInputCSV.popScan(populationCSVfile);
		super.start();
		createAgentPopulation(populationArray);
		if(!graphStructure.equalsIgnoreCase("original")){	//the original network allows each household to store its own network;
			//all others take their network structure from the ABM itself
			if(graphStructure.equalsIgnoreCase("kleinberg small world network")){
				//call kleinberg generator
				Graph KBSWgraph = generateKleinbergSmallWorldSocialNetwork();
				createSocialNetwork(KBSWgraph);
			}
			else{
				System.out.println("no network structure identified, exiting");
				System.exit(1);
			}

		}
		schedule.scheduleRepeating(0.1, dc); //puts DataCollector on schedule
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

	protected void createAgentPopulation(int [][] populationArray){
		int numTimeSteps = populationArray.length;
		for (int i = 0; i < numTimeSteps; i++){											//for each time step i
			newAgentsAtThisTimeStepOriginalNetwork.clear();								//remove all old agents from new agent array
			double double_i = (double) (i);
			population = populationArray[i][1];
			int numNewAgents = getNumNewAgents(populationArray, double_i);
			for (int j = 0; j < numNewAgents; j++){										//create number of new agents required
				//System.out.println(" num newAgents: " + getNumNewAgents(populationArray, double_i));
				Household newAgent = createNewAgent(populationArray, double_i);

				// TODO is this ever used?
				Household.houseHoldAgents.add(newAgent);

				schedule.scheduleRepeating(double_i, newAgent);
				numTotalAgents++;
			}
		}
	}


	public static double getCumulativeDemand(){
		double totalDemand = 0.0;
		for(Household hh: network){
			totalDemand = totalDemand + hh.monthlyDemand;
		}
		return totalDemand;
	}

	protected int getNumNewAgents(int[][] population, double timeStep){
		int intTimeStep = (int) timeStep;
		int popCurrentTimeStep;
		int popPreviousTimeStep;
		int currentNumHouseholds;
		int previousNumHouseholds;
		int numNewHouseholds;
		int numTotalTimeSteps = population.length;
		if(intTimeStep < 0 || intTimeStep > numTotalTimeSteps - 1){ //red flag that schedule isn't running correctly
			//may cause errors when running schedule if you don't keep tabs on iterations
			System.out.println("incorrect time step");
			return -1;
		}
		popCurrentTimeStep = population[intTimeStep][1];
		double curNumHouseholds = popCurrentTimeStep / averageHouseholdSize;
		int currentNumHouseholdsInt = (int) (curNumHouseholds);
		if (intTimeStep > 1 || intTimeStep == 1){
			popPreviousTimeStep = population[intTimeStep - 1][1];
			double prevNumHouseholds = popPreviousTimeStep / averageHouseholdSize;
			double numNewHouseholdDouble = curNumHouseholds - prevNumHouseholds;
			numNewHouseholds = (int) (numNewHouseholdDouble);
			return numNewHouseholds;
		}else{
			numNewHouseholds = currentNumHouseholdsInt;
			return numNewHouseholds;
		}
	}

	protected static void generateTxtFile(String input){
		for (int i = 0; i < jobs; i ++){	
			PrintStream outPrint = null;
			try{
				outPrint = new PrintStream(new File(outputFileName));
			}
			catch (FileNotFoundException e){
				System.out.println(outputFileName + " (No such file or directory)");
				System.exit(1);
			}
			outPrint.println(input);
		}
	}

	public static int getCurrentJob() {
		return currentJob;
	}

	public static String getGraphStructure() {
		return graphStructure;
	}

	public Graph generateKleinbergSmallWorldSocialNetwork(){
		Graph graph = new SingleGraph("JaipurResidents");
		if(numTotalAgents > 1){
			Generator gen = new WattsStrogatzGenerator(numTotalAgents, 2, 0.5);//n number of agents, num connections k, rewiring probability beta
			gen.addSink(graph);
			gen.begin();
			while(gen.nextEvents()){
				sleep();
			}
			gen.end();
			graph.display(false);
			return graph;
		}
		else{
			System.out.println("numTotalAgents is failing");
			System.exit(0);
			return graph;
		}
	}

	protected void sleep() {
		try { Thread.sleep(300); } catch (Exception e) {}
	}

	private void createSocialNetwork(Graph graph){
		//go through all households in the model
		for (Household hh : network){
			//get the current household's name
			String agentName = hh.getVertexName();
			//for this given node in the graph, find the corresponding agent
			for (Node n: graph){
				String nodePseudonym = "vert" + (n.getIndex());
				//if the node corresponds with the agent, then we get all the agent's connections and store them in the acq array
				if(agentName.equals(nodePseudonym)){
					hh.acquaintances = findNeighborArray(n);
				}
			}
		}
	}

	private ArrayList<Household> findNeighborArray(Node node){
		neighborArray = new ArrayList<Household>();
		neighborHousehold = new Household();
		Collection<Edge> edgeCollection = node.getLeavingEdgeSet();
		for (Edge edge: edgeCollection){
			Node oppositeNode = edge.getOpposite(node);
			//find corresponding household in overall model
			for (Household hh: network){
				String comparedName = hh.getVertexName();
				String vertexName = "vert" + oppositeNode;
				if(!comparedName.equalsIgnoreCase(vertexName)){
					continue;
				}
				else if(comparedName.equalsIgnoreCase(vertexName)){
					UUID uuid = hh.getUUID();
					for (Household potentialAcq : neighborArray){
						if(uuid == potentialAcq.uuid){
							continue;
						}			
					}
					for (Household potentialAcq : hh.acquaintances){
						if(uuid == potentialAcq.uuid){
							continue;
						}
					}
					neighborHousehold = findHouseHoldFromUUID(uuid);
				}
				else{
					throw new IllegalArgumentException("error in VertexHouseholdEquivalent");
				}
				//Household neighborHousehold = findVertexHouseholdEquivalent(edge.getOpposite(node));
				neighborArray.add(neighborHousehold);
			}
			if(neighborArray.isEmpty()){
				System.out.println("Error in neighbor array");
				return null;
			}
		}
		return neighborArray;
	}

	private Household findHouseHoldFromUUID(UUID uuidFind){
		for (Household hh: network){
			if(hh.uuid == uuidFind){
				return hh;
			}
		}
		System.out.println("findHouseholdFromUUID isn't working");
		return null;
	}

}
