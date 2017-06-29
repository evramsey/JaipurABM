package JaipurABM;

import ec.util.MersenneTwisterFast;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import sim.engine.SimState;
import sim.engine.Steppable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author lizramsey
 *         approximates behaviors of households with indoor plumbing
 */

public class Household implements Steppable {

	public static ArrayList<Household> houseHoldAgents = new ArrayList<>();
	public static double percentConservers;
	protected static MersenneTwisterFast rng = new MersenneTwisterFast();    //Random number generator for cycling agents during friendship assignment.

	//personal attributes for each JaipurABM.Household Agent
	private int independentLikelihoodDFInstall;
	private int friendLikelihoodDFInstall;
	protected int familyLikelihoodDFInstall;

	public int householdSize;
	public boolean isConserver;
	protected String vertexName;
	public double monthlyDemand;
	public int maxNumFamilyMembers;
	public int maxNumCloseFriends;
	public int maxNumConnections;
	public int maxNumAcquaintances;
	int numSkippedStepsUtilUpdate;
	int numSkippedStepsTalkUpdate;
    private int remainingConnections;
    private double familyDelta;
    private double friendDelta;
    private boolean converted_WC_to_NC = false;
    private boolean converted_NC_to_WC = false;
	private double RatioFamConservers;
	private double RatioFamNonConservers;
	private double RatioFriendConservers;
	private double RatioFriendNonConservers;
	private double RatioAcqConservers;
	private double RatioAcqNonConservers;

    public UUID uuid;//Unique ID for this particular agent
	public ArrayList<Household> acquaintances = new ArrayList<Household>();
	protected ArrayList<Household> closeFriends = new ArrayList<Household>();
	protected ArrayList<Household> respectedFamilyMembers = new ArrayList<Household>();//An array of IDs for agents that are in this agent's network

	protected ArrayList<Household> acqAlreadySpokenTo = new ArrayList<Household>();
	protected ArrayList<Household> friendsAlreadySpokenTo = new ArrayList<Household>();
	protected ArrayList<Household> famAlreadySpokenTo = new ArrayList<Household>();

	protected ArrayList<UUID> relatedUuids = new ArrayList<>();

	protected double timeStepBorn;

	/**
	 * Default constructor
	 */
	public Household(){}

	/**
	 * 
	 * @param vertexNumber
	 * @param timeStep
	 */
	public Household (int vertexNumber, double timeStep) {
		independentLikelihoodDFInstall = ValueGenerator.getValueLikert(2.88, 1.46);//ind likelihood DFI, from survey
		friendLikelihoodDFInstall = ValueGenerator.getValueLikert(3.12, 1.43);//friend likelihood DFI, from survey
		familyLikelihoodDFInstall = ValueGenerator.getValueLikert(3.8, 1.48);//family likelihood DFI, from survey
		familyDelta = ValueGenerator.generateSocialPressureDelta(JaipurABM.socialPressureAverage, JaipurABM.stdDevPressureDelta);
		friendDelta = ValueGenerator.generateSocialPressureDelta(JaipurABM.socialPressureAverage, JaipurABM.stdDevPressureDelta);
		householdSize = ValueGenerator.getPoissonValueWithMaxAndMin(5.1, 1, 20);//household size, from 2011 census
		isConserver = generateConservationStatus();
		vertexName	= "vert" + vertexNumber;
		maxNumConnections = ValueGenerator.getPoissonValueWithMaxAndMin(48.0, 11, 185);
		maxNumFamilyMembers = ValueGenerator.getPoissonValueWithMaxAndMin(5.48, 0,10);
		maxNumCloseFriends = ValueGenerator.getPoissonValueWithMaxAndMin(2.64, 0, 10);
		numSkippedStepsUtilUpdate = ValueGenerator.getPoissonValue(120);
		numSkippedStepsTalkUpdate = ValueGenerator.getPoissonValue(12);
		maxNumAcquaintances = maxNumConnections - maxNumFamilyMembers - maxNumCloseFriends;
		if(maxNumAcquaintances < 0) {
            maxNumAcquaintances = 0;
        }

        setRemainingConnections();

		uuid = UUID.randomUUID(); //set uuid
		timeStepBorn = timeStep;
	}



	public void step(SimState state) {
		prepareStep(state);
		monthlyDemand = getThisHouseholdDemand();
		DataCollector.CumulativeDemand = DataCollector.CumulativeDemand + monthlyDemand;
		DataCollector.modelPopulation = DataCollector.modelPopulation + this.householdSize;
		DataCollector.numAgents++;
		if(isConserver){
			DataCollector.numConservers++;
		}
		String graphStructure = JaipurABM.getGraphStructure();
		//TODO: bracketed out to find updating error
		if(graphStructure.equalsIgnoreCase("original")){
			talkToThreeNetworks(state);
		}
		else{
			talk(acquaintances, acqAlreadySpokenTo, state);
		}
		if(shouldSkipStep(numSkippedStepsUtilUpdate)){
			return;
		}
		//to prevent reversing of conservation decisions (DF toilet installation is permanent, at least in this model for now)
		//TODO:testing reversible conservation behaviors by bracketing out !isConserver
		//if(!isConserver){
			calculateUtilityandUpdateConsumption();
		//}
	}

	public String getVertexName() {
		return vertexName;
	}

	public void setHouseholdSize(int newHouseholdSize) {
		householdSize = newHouseholdSize;
	}

	public static boolean generateConservationStatus() {
		double num = rng.nextDouble(true, false);    //includes 0.0 and 1.0 in randomly drawn number's interval
		return num < percentConservers || num == percentConservers;
	}
	public void setRemainingConnections(){
		int nExistingConnections = respectedFamilyMembers.size() + closeFriends.size() + acquaintances.size();
		remainingConnections = maxNumConnections - nExistingConnections;

		if (nExistingConnections > maxNumConnections){
			System.out.println("JaipurABM.Household has more connections (" + nExistingConnections
					+ ") than maximum allowed connections (" + maxNumConnections + ")");
		}
	}

	//TODO: once the model is running, edit this with realistic consumption values and months
	public double getThisHouseholdDemand() {
		double demand;
		if (!isConserver) {
			demand = householdSize * 125 * 30;
		} else {
			demand = householdSize * 100 * 30;
		}
		return demand;
	}

	public double calculateRatiosForUtilityFxn(ArrayList<Household> thisList, ArrayList<Household> communicatedList, boolean calculatingConserver){ 
		int numConserversSpokenTo = 0;
		int numNonConserversSpokenTo = 0;
		//TODO: changing the network size to include everyone in social network, not just the one list
		int networkSize = this.acquaintances.size() + this.closeFriends.size() + this.respectedFamilyMembers.size();
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

	private void calculateUtilityandUpdateConsumption() {
		RatioFamConservers = calculateRatiosForUtilityFxn(respectedFamilyMembers, famAlreadySpokenTo, true);
		RatioFamNonConservers = calculateRatiosForUtilityFxn(respectedFamilyMembers, famAlreadySpokenTo, false);
		RatioFriendConservers = calculateRatiosForUtilityFxn(closeFriends, friendsAlreadySpokenTo, true);
		RatioFriendNonConservers = calculateRatiosForUtilityFxn(closeFriends, friendsAlreadySpokenTo, false);
		RatioAcqConservers = calculateRatiosForUtilityFxn(acquaintances, acqAlreadySpokenTo, true);
		RatioAcqNonConservers = calculateRatiosForUtilityFxn(acquaintances, acqAlreadySpokenTo, false);

		//int famDelta = calculateDelta(this.independentLikelihoodDFInstall, this.familyLikelihoodDFInstall);
		//int friendDelta = calculateDelta(this.independentLikelihoodDFInstall, this.friendLikelihoodDFInstall);
		if (isConserver) {
			double randNum = rng.nextDouble(true, false);
//			double utilStay = UtilityFunction.calculateUtilityForConserverStayingConserver(RatioFamConservers,
//					famDelta, RatioFriendConservers, friendDelta, RatioAcqConservers);
//			double utilChange = UtilityFunction.calculateUtilityForConserverBecomingNonConserver(RatioFamNonConservers,
//					famDelta, RatioFriendNonConservers, friendDelta, RatioAcqNonConservers);
			double utilStay = UtilityFunction.calculateUtilityForConserverStayingConserver(RatioFamConservers,
					familyDelta, RatioFriendConservers, friendDelta, RatioAcqConservers);
			double utilChange = UtilityFunction.calculateUtilityForConserverBecomingNonConserver(RatioFamNonConservers,
					familyDelta, RatioFriendNonConservers, friendDelta, RatioAcqNonConservers);
			double probabilityConsToCons = ProbabilityOfBehavior.probabilityConsToCons(utilStay, utilChange);
			if (randNum >= probabilityConsToCons) {
				isConserver = false;
				converted_WC_to_NC = true;
				//System.out.println(vertexName + " is changing from conserver to nonconserver");
			}
		} else {
			double randNum = rng.nextDouble(true, false);
			double utilStay = UtilityFunction.calculateUtilityForNonConserverStayingNonConserver(RatioFamNonConservers,
					familyDelta, RatioFriendNonConservers, friendDelta, RatioAcqNonConservers);
			double utilChange = UtilityFunction.calculateUtilityForNonConserverBecomingConserver(RatioFamConservers,
					familyDelta, RatioFriendConservers, friendDelta, RatioAcqConservers);
			double probabilityNonConsToNonCons = ProbabilityOfBehavior.probabilityNonConsToNonCons(utilChange, utilStay);
			if (randNum >= probabilityNonConsToNonCons) {
				isConserver = true;
				converted_NC_to_WC = true;
			}
		}
	}

	public int calculateDelta(int originalNum, int newNum){
		int delta = newNum - originalNum;
		if (delta < 0){
			delta = 0;
		}
		return delta;
	}


    protected void prepareStep(SimState state){
		converted_NC_to_WC = false;
		converted_WC_to_NC = false;
        String graphStructure = JaipurABM.getGraphStructure();
        if (graphStructure.equalsIgnoreCase("original")){
            double timeStep = state.schedule.getTime();
            assignFamilyToAgentAtTimeStep(timeStep);
            assignCloseFriendsAtTimeStep(timeStep);
            assignAcquaintancesToAgentAtTimeStep(timeStep);
        }
        else{
			setNeighborArrayFromGraph(JaipurABM.getGraph());
		}
    }

    public void assignAcquaintancesToAgentAtTimeStep(double timeStep) {
        if (acquaintances.size() == maxNumAcquaintances || remainingConnections <= 0) {
            return;
        }

        List<Household> availableHouseholds = houseHoldAgents.stream()
                .filter(h -> h.uuid != uuid)
                .filter(h -> h.doesRelationshipAlreadyExist(uuid) == false)
                .filter(h -> h.timeStepBorn <= timeStep)
                .filter(h -> h.remainingConnections > 0)
                .filter(h -> h.acquaintances.size() < h.maxNumAcquaintances)
                .collect(Collectors.toList());

		Collections.shuffle(availableHouseholds);

        for (Household hh : availableHouseholds) {
            // Check if any remaining connections available
            if (acquaintances.size() == maxNumAcquaintances || remainingConnections <= 0) {
                return;
            }

            acquaintances.add(hh);
            hh.acquaintances.add(this);

			relatedUuids.add(hh.uuid);
			hh.relatedUuids.add(uuid);

			this.remainingConnections--;
			hh.remainingConnections--;

//            String msg = String.format("%1$s related %2$s as acquaintances", vertexName, hh.vertexName);
//            System.out.println(msg);
        }
    }

	public void assignFamilyToAgentAtTimeStep(double timeStep) {
        if (respectedFamilyMembers.size() == maxNumFamilyMembers || remainingConnections <= 0) {
            return;
        }

        List<Household> availableHouseholds = houseHoldAgents.stream()
                .filter(h -> h.uuid != uuid)
                .filter(h -> h.doesRelationshipAlreadyExist(uuid) == false)
                .filter(h -> h.timeStepBorn <= timeStep)
                .filter(h -> h.remainingConnections > 0)
                .filter(h -> h.acquaintances.size() < h.maxNumAcquaintances)
                .collect(Collectors.toList());

		Collections.shuffle(availableHouseholds);

		for (Household hh : availableHouseholds) {
            // Check if any remaining connections available
            if (respectedFamilyMembers.size() == maxNumFamilyMembers || remainingConnections <= 0) {
                return;
            }

            respectedFamilyMembers.add(hh);
            hh.acquaintances.add(this);

			relatedUuids.add(hh.uuid);
			hh.relatedUuids.add(uuid);

			this.remainingConnections--;
			hh.remainingConnections--;
		}
	}

    private void assignCloseFriendsAtTimeStep(double timeStep) {
        if (closeFriends.size() == maxNumCloseFriends || remainingConnections <= 0) {
            return;
        }

        List<Household> availableHouseholds = houseHoldAgents.stream()
                .filter(h -> h.uuid != uuid)
                .filter(h -> h.doesRelationshipAlreadyExist(uuid) == false)
                .filter(h -> h.timeStepBorn <= timeStep)
                .filter(h -> h.remainingConnections > 0)
                .filter(h -> h.closeFriends.size() < h.maxNumCloseFriends)
                .collect(Collectors.toList());

		Collections.shuffle(availableHouseholds);

        for (Household hh : availableHouseholds) {
            // Check if any remaining connections available
            if (closeFriends.size() == maxNumCloseFriends || remainingConnections <= 0) {
                return;
            }

            closeFriends.add(hh);
            hh.closeFriends.add(this);

			relatedUuids.add(hh.uuid);
			hh.relatedUuids.add(uuid);

			this.remainingConnections--;
			hh.remainingConnections--;

//            String msg = String.format("%1$s related %2$s as friends", vertexName, hh.vertexName);
//            System.out.println(msg);
        }
    }

	private boolean doesRelationshipAlreadyExist(UUID targetUuid){
		return relatedUuids.contains(targetUuid);
    }

	private void talk(ArrayList<Household> wholeNetwork, ArrayList<Household> networkTalkedToAlready, SimState state){
		//select random member of arrayList, then, if he doesn't already exist in the talkedTo list, add him. if he does, return and let the next agent go
		if(shouldSkipStep(numSkippedStepsTalkUpdate)){
			return;
		}
		ArrayList<Household> shuffledNetwork = wholeNetwork;
		Collections.shuffle(shuffledNetwork);
		Household randHH = null;
		if(shuffledNetwork.isEmpty()){
			return;
		}
		//added to get new network structures to change over time
		for (Household hh : shuffledNetwork){
			double currentTimeStep = state.schedule.getTime();
			if(hh.timeStepBorn > currentTimeStep){
				//System.out.println(hh.getVertexName() + "was born at time step " + hh.timeStepBorn + " and current time step is " + state.schedule.getTime());
				continue;
			}
			else{
				//check to see if hh has spoken to anyone to avoid nullpointer exception
				if(networkTalkedToAlready.isEmpty()){
					randHH = hh;
					networkTalkedToAlready.add(randHH);
					return;
				}		
				//if the agent already exists in the TalkedToAlready array, the talking agent loses his turn
				for (Household hhTalkedTo : networkTalkedToAlready){
					if(hhTalkedTo.uuid == hh.uuid){
						return;
					}
				}
				randHH = hh;
				networkTalkedToAlready.add(randHH);
				return;
			}
		}
		if (randHH == null){
			System.out.println("talk function: " + this.vertexName + " has no current acquaintances");
			return;
		}
	}

	private void talkToThreeNetworks(SimState state){
		int num = rng.nextInt(3) + 1;
		//if num is 1 and fam is empty or if num is 2 and friends are empty or num is 3 and acq is empty, pick a new num
		while (num == 1 && respectedFamilyMembers.isEmpty() || num == 2 && closeFriends.isEmpty() || num == 3 && acquaintances.isEmpty()){
			num = rng.nextInt(3) + 1;
		}

		//select one network to communicate with for this timestep
		if (num == 1){
			//communicate with family
			talk(respectedFamilyMembers, famAlreadySpokenTo, state);
		}
		else if (num == 2){
			talk(closeFriends, friendsAlreadySpokenTo, state);
		}
		else if (num == 3){
			talk(acquaintances, acqAlreadySpokenTo, state);
		}
		else{
			System.out.println("error in rng for talk method");
			return;
		}
	}

	private boolean shouldSkipStep(int numTries){
			double chance = 1.0/numTries;
			double randNum = rng.nextDouble(true, false);
		return randNum >= chance;
	}

	public double getAgentAge(SimState state){
		double age = state.schedule.getTime() - timeStepBorn;
		return age;
	}

	public UUID getUUID(){	
		return this.uuid;
	}

	public ArrayList<Household> getAcquaintances(){
		return acquaintances;
	}

	public double getTimeStepBorn(){ return timeStepBorn; }

	public boolean has_converted_WC_to_NC_thistimestep(){
		return converted_WC_to_NC;
	}

	public boolean has_converted_NC_to_WC_thistimestep(){return converted_NC_to_WC;}

	public int getFamilySize(){return this.respectedFamilyMembers.size();}

	public int getFriendsSize(){return this.closeFriends.size();}

	public int getAcqSize(){return this.acquaintances.size();}

	public double getRatioFamCons(){return this.RatioFamConservers;}

	public double getRatioFriendsCons(){return this.RatioFriendConservers;}

	public double getRatioAcqCons(){return this.RatioAcqConservers;}

	public double getFamDelta(){ return this.familyDelta;}

	public double getFriendDelta(){return this.friendDelta;}

	public boolean isAgentConserver(){ return this.isConserver;}

	private void setNeighborArrayFromGraph(Graph graph){
		for(Node n: graph){
			UUID nodeUUID = n.getAttribute("nodeUUID");
			if(this.uuid == nodeUUID){
				this.acquaintances = n.getAttribute("neighborArray");
				return;
			}
		}
	}

	public static void setPercentConservers(double percCons){
		percentConservers = percCons;
	}

}

