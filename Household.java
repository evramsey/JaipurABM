import ec.util.MersenneTwisterFast;
import sim.engine.SimState;
import sim.engine.Steppable;

import java.util.ArrayList;

/**
 *  Agent class of households
 */

public class Household implements Steppable {
	public static ArrayList<Household> houseHoldAgents = new ArrayList<>();
	private static double percentConsEachTimeStep;
	private static double percentInitialCons;
	protected static MersenneTwisterFast rng = new MersenneTwisterFast();    //Random number generator
	public boolean isConserver;
	private boolean has3SocialNetworks;
	private int vertexIndex;
	public double monthlyDemand;
	public int maxNumFamilyMembers;
	public int maxNumCloseFriends;
	public int currentNumFamilyMembers;
	public int currentNumCloseFriends;
	public int currentNumAcquaintances;
	public int currentNumConnections;
	int numSkippedStepsUtilUpdate;
	int numSkippedStepsTalkUpdate;
    public double exogenousTerm;
	public double exogenousTermDrought;
	public int householdSize;
    private static double exogenousTermSeed;
	private static double exogenousTermDroughtSeed;
    private double familyDelta;
	private static int utilSkipStepSeed;
	private static int talkSkipStepSeed;
    private double friendDelta;
	private double RatioFamConservers;
	private double RatioFamNonConservers;
	private double RatioFriendConservers;
	private double RatioFriendNonConservers;
	private double RatioAcqConservers;
	private double RatioAcqNonConservers;
	protected ArrayList<Household> acqAlreadySpokenTo = new ArrayList<Household>();
	protected ArrayList<Household> friendsAlreadySpokenTo = new ArrayList<Household>();
	protected ArrayList<Household> famAlreadySpokenTo = new ArrayList<Household>();
	protected double timeStepBorn;

	/**
	 * Constructor of household agent
	 * @param vertexNumber
	 * @param timeStep
	 */
	public Household (int vertexNumber, double timeStep) {
		householdSize = ValueGenerator.getPoissonValue(5.15);//household size, from 2011 census
		isConserver = generateConservationStatus(timeStep);
		vertexIndex = vertexNumber;
		has3SocialNetworks = dftABM.doesModelHave3Networks();
		currentNumFamilyMembers = 0;
		currentNumCloseFriends = 0;
		currentNumAcquaintances = 0;
		currentNumConnections = 0;
		numSkippedStepsUtilUpdate = ValueGenerator.getPoissonValue(utilSkipStepSeed);
		numSkippedStepsTalkUpdate = ValueGenerator.getPoissonValue(talkSkipStepSeed);
		if(exogenousTermSeed == 0.0){
			exogenousTerm = 0.0;
		}
		else {
			exogenousTerm = ValueGenerator.createNormalDistSample(exogenousTermSeed, 0.1);
		}
		if(exogenousTermDroughtSeed == 0.0){
			exogenousTermDrought = 0.0;
		}
		else{
			exogenousTermDrought = ValueGenerator.createNormalDistSample(exogenousTermDroughtSeed, 0.1);
		}
		timeStepBorn = timeStep;
	}

	/**
	 * Actions taken by an agent at each time step
	 * @param state
	 */
    public void step(SimState state) {
		monthlyDemand = getThisHouseholdDemand();
		DataCollector.CumulativeDemand = DataCollector.CumulativeDemand + monthlyDemand;
		DataCollector.modelPopulation = DataCollector.modelPopulation + this.householdSize;
		DataCollector.numAgents++;
		if(isConserver){
			DataCollector.numConservers++;
		}
		int graphStructure = dftABM.getGraphStructureNum();
		if(!this.existsAtTimeStep(state.schedule.getTime())){
			return;
		}
		boolean droughtBool = dftABM.getLastYearDroughtIndex();
			if (!shouldSkipStep(numSkippedStepsTalkUpdate)) {
				talk(graphStructure, state);
			}
			if(shouldSkipStep(numSkippedStepsUtilUpdate)){
				return;
			}
			//irreversible conservation behaviors
			if (!isConserver) {
				if(droughtBool){
					calculateUtilityandUpdateConsumption(exogenousTermDrought);//add drought term
				}
				else {
					calculateUtilityandUpdateConsumption(0);//set drought term = 0
				}
			}
	}

	/**
	 * Calculate whether agent changes adoption/conservation status
	 * @param thisTimeStep
	 * @return boolean of status change
	 */
	public static boolean generateConservationStatus(double thisTimeStep) {
		double num = rng.nextDouble(true, false);    //includes 0.0 and 1.0 in randomly drawn number's interval
		if (thisTimeStep == 0){
			return num < percentInitialCons || num == percentInitialCons;
		}
		else {
			return num < percentConsEachTimeStep || num == percentConsEachTimeStep;
		}
	}

	/**
	 * Calculate household demand, based on static input parameters
	 * @return calculated demand, in gallons per month
	 */
	public double getThisHouseholdDemand() {
		double demand;
		if (!isConserver) {
			demand = householdSize * 200 * (365/12.0);
		} else {
			demand = householdSize * (200 - (5.1*1.52)) * (365/12.0);//1.52 is difference per flush, 5.1 is average number of flushes per person per day
		}
		return demand;
	}

	/**
	 *
	 * @param communicatedList
	 * @param calculatingConserver
	 * @return
	 */
	public double calculateRatiosForUtilityFxn(ArrayList<Household> communicatedList, boolean calculatingConserver){
		int numConserversSpokenTo = 0;
		int numNonConserversSpokenTo;
		int networkSize;
		if(has3SocialNetworks){
			networkSize = currentNumAcquaintances + currentNumFamilyMembers + currentNumCloseFriends;
		}
		else {
			networkSize = currentNumAcquaintances;
		}
		double ratio = 0.0;
		for (Household hh: communicatedList){
			if(hh.isConserver){
				numConserversSpokenTo++;
			}
		}
		numNonConserversSpokenTo = networkSize - numConserversSpokenTo;
		if(calculatingConserver){
			if(numConserversSpokenTo == 0 || networkSize == 0){
				ratio = 0.0;
			}
			else{
				double numConsDouble = (double)(numConserversSpokenTo);
				ratio = numConsDouble/networkSize;
			}
		}
		else{
			if(numNonConserversSpokenTo == 0 || networkSize == 0){
				ratio = 0.0;
			}
			else{
				double numNonConsDouble = (double)(numNonConserversSpokenTo);
				ratio = numNonConsDouble/networkSize;
			}
		}
		return ratio;
	}

	/**
	 * Call utility calculation functions for changing and maintaining conservation status, then update consumption
	 * @param exogTermDrought
	 */
	private void calculateUtilityandUpdateConsumption(double exogTermDrought) {
		if(!has3SocialNetworks){
			RatioAcqConservers = calculateRatiosForUtilityFxn(acqAlreadySpokenTo, true);
			RatioAcqNonConservers = 1 - RatioAcqConservers;
		}
		else {
			RatioFamConservers = calculateRatiosForUtilityFxn(famAlreadySpokenTo, true);
			RatioFamNonConservers = 1 - RatioFamConservers;
			RatioFriendConservers = calculateRatiosForUtilityFxn(friendsAlreadySpokenTo, true);
			RatioFriendNonConservers = 1 - RatioFriendConservers;
			RatioAcqConservers = calculateRatiosForUtilityFxn(acqAlreadySpokenTo, true);
			RatioAcqNonConservers = 1 - RatioAcqConservers;
		}
		if(has3SocialNetworks){
			calculateUtilFor3Networks(exogTermDrought);
		}
		else{
			calculateUtilFor1Network(exogTermDrought);
		}
	}

	/**
	 * Calculate utility for behaviors if agent has friends, family, and acquaintance networks and update status
	 * @param exogTermDrought
	 */
	private void calculateUtilFor3Networks(double exogTermDrought){
		if (isConserver) {
			double randNum = rng.nextDouble(true, false);
			double utilStay = UtilityFunction.calculateUtilityForConserverStayingConserver3Networks(RatioFamConservers,
					familyDelta, RatioFriendConservers, friendDelta, RatioAcqConservers, exogenousTerm, exogTermDrought);
			double utilChange = UtilityFunction.calculateUtilityForConserverBecomingNonConserver3Networks(RatioFamNonConservers,
					familyDelta, RatioFriendNonConservers, friendDelta, RatioAcqNonConservers);
			double probabilityConsToCons = ProbabilityOfBehavior.probabilityConsToCons(utilStay, utilChange);
			if (randNum >= probabilityConsToCons) {
				isConserver = false;
			}
		} else {
			double randNum = rng.nextDouble(true, false);
			double utilStay = UtilityFunction.calculateUtilityForNonConserverStayingNonConserver3Networks(RatioFamNonConservers,
					familyDelta, RatioFriendNonConservers, friendDelta, RatioAcqNonConservers);
			double utilChange = UtilityFunction.calculateUtilityForNonConserverBecomingConserver3Networks(RatioFamConservers,
					familyDelta, RatioFriendConservers, friendDelta, RatioAcqConservers, exogenousTerm, exogTermDrought);
			double probabilityNonConsToNonCons = ProbabilityOfBehavior.probabilityNonConsToNonCons(utilChange, utilStay);
			if (randNum >= probabilityNonConsToNonCons) {
				isConserver = true;
			}
		}
	}

	/**
	 * Calculate utility for behaviors if agent has only one type of neighboring agents (acquaintances only) and update
	 * status
	 * @param exogTermDrought
	 */
	private void calculateUtilFor1Network(double exogTermDrought){
		if (isConserver) {
			double randNum = rng.nextDouble(true, false);
			double utilStay = UtilityFunction.calculateUtilityForConserverStayingConserver1Network(RatioAcqConservers,
					exogenousTerm, exogTermDrought);
			double utilChange = UtilityFunction.calculateUtilityForConserverBecomingNonConserver1Network(RatioAcqNonConservers);
			double probabilityConsToCons = ProbabilityOfBehavior.probabilityConsToCons(utilStay, utilChange);
			if (randNum >= probabilityConsToCons) {
				isConserver = false;
			}
		} else {
			double randNum = rng.nextDouble(true, false);
			double utilStay = UtilityFunction.calculateUtilityForNonConserverStayingNonConserver1Network(RatioAcqNonConservers);
			double utilChange = UtilityFunction.calculateUtilityForNonConserverBecomingConserver1Network(RatioAcqConservers,
					exogenousTerm, exogTermDrought);
			double probabilityNonConsToNonCons = ProbabilityOfBehavior.probabilityNonConsToNonCons(utilChange, utilStay);
			if (randNum >= probabilityNonConsToNonCons) {
				isConserver = true;
			}
		}
	}

	/**
	 * Select an agent to communicate with, then add it to communicated network
	 * @param networkStructure
	 * @param state
	 */
	private void talk(int networkStructure, SimState state) {
		int numAgentToTalkTo = dftABM.getCommunicatedAgent(this.vertexIndex, state.schedule.getTime());
		if (numAgentToTalkTo == -1) {
			return;
		}
		if (networkStructure == 2) {
			int relNum = dftABM.getTypeOfRelationship(this.vertexIndex, numAgentToTalkTo);
			if(relNum < 0){
				return;
			}
			findSocialNetworkAndAddAgent(relNum, numAgentToTalkTo, state);
		}
		else if (networkStructure == 1 || networkStructure == 3){
			addtoAcquaintanceNetwork(numAgentToTalkTo);
		}
		else{
			System.out.println("network structure incorrect in talk function: " + networkStructure);
			System.exit(1);
		}
	}

	/**
	 * Check time step born of agent to make sure it is active
	 * @param thisTimeStep
	 * @return whether agent is active
	 */
	public boolean existsAtTimeStep(double thisTimeStep){
		if(thisTimeStep - timeStepBorn ==0 || thisTimeStep - timeStepBorn > 0){
			return true;
		}
		else{
			return false;
		}
	}

	/**
	 * Add communicated agent to appropriate social network
	 * @param relNum
	 * @param numAgentToTalkTo
	 * @param state
	 */
	private void findSocialNetworkAndAddAgent(int relNum, int numAgentToTalkTo, SimState state){
		if (relNum == 0) {
			addtoFamNetwork(numAgentToTalkTo);
		}
		else if (relNum == 1) {
			addtoFriendNetwork(numAgentToTalkTo);
		}
		else if (relNum == 2) {
			addtoAcquaintanceNetwork(numAgentToTalkTo);
		} else {
			System.out.println("wrong relationships in talk function" + " time step " + state.schedule.getTime());
		}
	}

	/**
	 * Add communicated agent to Family network, and add self to agent's Family network
	 * @param numAgentToTalkTo
	 */
	private void addtoFamNetwork(int numAgentToTalkTo){
		for(Household hh : famAlreadySpokenTo) {
			if(hh.getVertexIndex() == numAgentToTalkTo) {
				return;
			}
		}
		Household potentialFam = dftABM.network.get(numAgentToTalkTo);
		famAlreadySpokenTo.add(dftABM.network.get(numAgentToTalkTo));
		potentialFam.famAlreadySpokenTo.add(this);
	}

	/**
	 * Add communicated agent to Friend network, and add self to agent's Friend network
	 * @param numAgentToTalkTo
	 */
	private void addtoFriendNetwork(int numAgentToTalkTo){
		for(Household hh: friendsAlreadySpokenTo){
			if(hh.getVertexIndex() == numAgentToTalkTo){
				return;
			}
		}
		Household potentialFriend = dftABM.network.get(numAgentToTalkTo);
		friendsAlreadySpokenTo.add(dftABM.network.get(numAgentToTalkTo));
		potentialFriend.friendsAlreadySpokenTo.add(this);
	}

	/**
	 * Add communicated agent to Acquaintance network, and add self to agent's Acquaintance network
	 * @param numAgentToTalkTo
	 */
	private void addtoAcquaintanceNetwork(int numAgentToTalkTo){
		for(Household hh: acqAlreadySpokenTo){
			if(hh.getVertexIndex() == numAgentToTalkTo){
				return;
			}
		}
		Household potentialAcqToTalkTo = dftABM.network.get(numAgentToTalkTo);
		this.acqAlreadySpokenTo.add(dftABM.network.get(numAgentToTalkTo));
		potentialAcqToTalkTo.acqAlreadySpokenTo.add(this);
	}

	/**
	 * Calculate whether the agent skips the current time step for an action
	 * @param numTries
	 * @return boolean of whether to engage in action
	 */
	private boolean shouldSkipStep(int numTries){
			if (numTries == 0){
				return false;
			}
			double chance = 1.0/numTries;
			double randNum = rng.nextDouble(true, false);
		return randNum >= chance;
	}

	public int getVertexIndex(){ return vertexIndex;}

	public static double getExogenousTermSeed() {
		return exogenousTermSeed;
	}

	public int getMaxNumFamilyMembers(){return this.maxNumFamilyMembers;}

	public int getMaxNumCloseFriends(){return this.maxNumCloseFriends;}

	public static void setPercentConsEachTimeStep(double percCons){
		percentConsEachTimeStep = percCons;
	}

	public static void setPercentInitialCons(double percCons) { percentInitialCons = percCons; }

	public void increaseNumFriends(){this.currentNumCloseFriends++;}

	public void increaseNumFamilyMembers(){this.currentNumFamilyMembers++;}

	public void increaseNumAcquaintances(){this.currentNumAcquaintances++;}

	public static void setUtilSkipStepSeed(int seed){
		utilSkipStepSeed = seed;
	}

	public static void setTalkSkipStepSeed(int seed){
		talkSkipStepSeed = seed;
	}

    public static void setExogenousTermSeed(double seed) {
        exogenousTermSeed = seed;
    }

    public static void setExogenousTermDroughtSeed(double seed){
		exogenousTermDroughtSeed = seed;
	}
}


