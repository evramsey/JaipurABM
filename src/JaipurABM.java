

import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;

import com.opencsv.CSVReader;
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

	public static void main(String[] args) throws IOException {

//		double test = run(0.9, 0.9, 0.3, 0.6, 60, 6);

		//GenerateInputFilesFromRanges();
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		String formattedDate = sdf.format(date);

		DataCollector.time_simulation_start = formattedDate;

		String out_filename = "./output/" + formattedDate + "/r2_output.csv";
		CSVReader reader = new CSVReader(new FileReader(out_filename));
		List<String[]> lines = reader.readAll();
		reader.close();

		String[] last_row = lines.get(lines.size() - 1);

		double A = Double.parseDouble(last_row[4]);
		double B = Double.parseDouble(last_row[6]);
		double beta = Double.parseDouble(last_row[9]);
		double delta = Double.parseDouble(last_row[10]);
		int utility_steps = Integer.parseInt(last_row[11]);
		int talk_steps = 6;

		String in_file_name = "autogen.txt";
		String output_file = String.format(in_file_name);
		GenerateInputFile(output_file, A, B, delta, beta, utility_steps, talk_steps);
		DataCollector.in_filename = in_file_name;
		runSimulation(in_file_name);
		DataCollector.calculateR2();

		System.out.println("all runs finished, exiting");
		System.exit(0);
	}

	public static double run(double A, double B, double beta, double delta, int utility_steps, int talk_steps) throws IOException{
		//GenerateInputFilesFromRanges();
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		String formattedDate = sdf.format(date);

		DataCollector.time_simulation_start = "results"; //formattedDate;

//		String out_filename = "./output/" + formattedDate + "/r2_output.csv";
//		CSVReader reader = new CSVReader(new FileReader(out_filename));
//		List<String[]> lines = reader.readAll();
//		reader.close();
//
//		String[] last_row = lines.get(lines.size() - 1);

		String in_file_name = "autogen.txt";

		String output_file = String.format(in_file_name);

		GenerateInputFile(output_file, A, B, delta, beta, utility_steps, talk_steps);
		DataCollector.in_filename = in_file_name;
		runSimulation(in_file_name);
		double r2 = DataCollector.calculateR2();

		return r2;
	}

//	public static void main(String[] args) throws IOException {
//
//		//GenerateInputFilesFromRanges();
//		Date date = new Date();
//		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
//		String formattedDate = sdf.format(date);
//
//		formattedDate = "20160916075227";
//
//		DataCollector.time_simulation_start = formattedDate;
//
//		boolean set_vals = true;
//		int gen_ctr = 1;
//		for (double A = 0.1; A <= 1.001; A+=0.05) {
//			for (double B = 0.1; B <= 1.001; B+=0.05) {
//				for (double delta = 0.0; delta <= 0.301; delta+=0.1) {
//					for (double beta = 0; beta <= 1.001; beta+=0.1) {
//						for (int utility_steps = 6; utility_steps <= 60; utility_steps+=6) {
//							for (int talk_steps = 6; talk_steps <= 6; talk_steps+=6) {
//
//								if (set_vals){
//									String out_filename = "./output/" + formattedDate + "/r2_output.csv";
//									CSVReader reader = new CSVReader(new FileReader(out_filename));
//									List<String[]> lines = reader.readAll();
//									reader.close();
//
//									String[] last_row = lines.get(lines.size() - 1);
//
//									set_vals = false;
//									A = Double.parseDouble(last_row[4]);
//									B = Double.parseDouble(last_row[6]);
//									beta = Double.parseDouble(last_row[9]);
//									delta = Double.parseDouble(last_row[10]);
//									utility_steps = Integer.parseInt(last_row[11]);
//									talk_steps = 6;
//									continue;
//								}
//
//								if (A <= B){
//									continue;
//								}
//
//								String in_file_name = "autogen.txt";
//								String output_file = String.format(in_file_name, gen_ctr);
//								GenerateInputFile(output_file, A, B, delta, beta, utility_steps, talk_steps);
//								DataCollector.in_filename = in_file_name;
//								runSimulation(in_file_name);
//								DataCollector.calculateR2();
//								gen_ctr++;
//							}
//						}
//					}
//				}
//			}
//		}

//		File[] files = new File("./input").listFiles();
//		//If this pathname does not denote a directory, then listFiles() returns null.
//
//		for (File file : files) {
//			if (file.isFile() && file.getName().endsWith(".txt")) {
//				DataCollector.in_filename = file.getName();
//				runSimulation(file.getAbsolutePath());
//				//DataCollector.writeGexf();
//				int i = 1;
//				//DataCollector.writeJsonData();
//			}
//		}
//
//		try {
//			DataCollector.aggregateResults();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}

//		txtFileInput = txtFileInput + DataCollector.txtFileInput;
//		generateTxtFile(txtFileInput);
//		System.out.println("all runs finished, exiting");
//		System.exit(0);
//	}

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
	private static void GenerateInputFilesFromRanges(){
		int talk_steps = 1;
		int gen_ctr = 1;
		for (double A = 1.0; A <= 10.0; A+=0.5) {
			for (double B = 1.0; B <= 10.0; B+=0.5) {
				for (double delta = 0.0; delta <= 0.3; delta+=0.1) {
					for (double beta = 0; beta <= 1.0; beta+=0.1) {
						for (int utility_steps = 6; utility_steps <= 60; utility_steps+=6) {
							for (talk_steps = 6; talk_steps <= 60; talk_steps+=6) {
								String output_file = String.format("./input/autogen_%s.txt", gen_ctr);
								GenerateInputFile(output_file, A, B, delta, beta, utility_steps, talk_steps);
								gen_ctr++;
							}
						}
					}
				}
			}
		}
	}
	private static void GenerateInputFile(String output_file_name, double A, double B, double delta, double beta, int utility_steps, int talk_steps){
		String s = new StringBuilder()
				.append("Model Initialization Parameters\n")
				.append("\n")
				.append("Number of jobs:\n")
				.append("1\n")
				.append("Number of steps in main (2*(1 + number of dates to allow data collector to run—should be 458 for real run, 12 for test):\n")
				.append("458\n")
				.append("Average household size:\n")
				.append("5.1\n")
				.append("\n")
				.append("Social network setup (string):\n")
				.append("original\n")
				.append("\n")
				.append("OutputFileName (string):\n")
				.append("/Users/lizramsey/Documents/workspace/JaipurABM/GeneratedTXTs/Scenario19_0.05IC_a0.7_b0.3_et0_beta1_delta0_utilskip48_talkskip6.txt\n")
				.append("\n")
				.append("Agent Initialization Parameters\n")
				.append("\n")
				.append("Percent initial conservers:\n")
				.append("0.05\n")
				.append("\n")
				.append("Utility Function Parameters\n")
				.append("\n")
				.append("a/b prime value:\n")
				.append(A + "\n")
				.append("a prime/b value:\n")
				.append(B + "\n")
				.append("exogenous term:\n")
				.append("0\n")
				.append("beta (1 means util function completely determines action, 0 means completely random):\n")
				.append(beta + "\n")
				.append("delta\n")
				.append(delta + "\n")
				.append("skip steps in utility calculation:\n")
				.append(utility_steps + "\n")
				.append("skip steps in talk function:\n")
				.append(talk_steps + "\n")
				.append("\n")
				.append("\n")
				.append("For original network—\n")
				.append("k:\n")
				.append("p: (or something like that, look this up)\n")
				.toString();
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(output_file_name));
			writer.write(s);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
