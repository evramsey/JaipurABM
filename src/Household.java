import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.collections15.Factory;
import org.apache.commons.math3.distribution.PoissonDistribution;

import sim.engine.SimState;
import sim.engine.Steppable;
import ec.util.MersenneTwisterFast;

import java.util.Set;
import java.util.HashSet;

/**
 * @author lizramsey
 *         approximates behaviors of households with indoor plumbing
 */

public class Household implements Steppable {
	//values updated by scanned docs

	public static ArrayList<Household> houseHoldAgents = new ArrayList<>();
	public static double percentConservers;
	private static String textFileInput;
	protected static final int PROBABILITY_OF_FRIENDSHIP = 100; //Probability an agent will befriend another agent.
	protected static MersenneTwisterFast rng = new MersenneTwisterFast();    //Random number generator for cycling agents during friendship assignment.

	//personal attributes for each Household Agent
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

    private int remainingConnections;

    public UUID uuid;//Unique ID for this particular agent
	protected ArrayList<Household> acquaintances = new ArrayList<Household>();
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
	public Household(int vertexNumber, double timeStep) {
		independentLikelihoodDFInstall = ValueGenerator.getValueLikert(2.88, 1.46);//ind likelihood DFI, from survey
		friendLikelihoodDFInstall = ValueGenerator.getValueLikert(3.12, 1.43);//friend likelihood DFI, from survey
		familyLikelihoodDFInstall = ValueGenerator.getValueLikert(3.8, 1.48);//family likelihood DFI, from survey
		householdSize = ValueGenerator.getValueWithRange(5.1, 2.61, 1, 20);//household size, from 2011 census
		isConserver = Household.generateConservationStatus();
		vertexName	= "vert" + vertexNumber;
		//TODO: remove this after testing
//		maxNumConnections = 8;
//		maxNumFamilyMembers = 4;
//		maxNumCloseFriends = 2;

		maxNumConnections = ValueGenerator.getValueWithRange(48.0, 27.0, 11, 185);//max num connections/social network size
		maxNumFamilyMembers = ValueGenerator.getValueWithRange(5.48, 1.8, 0, 10);//family network, from latrine study
		maxNumCloseFriends = ValueGenerator.getValueWithRange(2.64, 2.16, 0, 10);//friend network, from latrine study
		maxNumAcquaintances = maxNumConnections - maxNumFamilyMembers - maxNumCloseFriends;
		if(maxNumAcquaintances < 0) {
            maxNumAcquaintances = 0;
        }

        setRemainingConnections();

		uuid = UUID.randomUUID(); //set uuid
		timeStepBorn = timeStep;
	}

	public int getRemainingConnections(){
		int nExistingConnections = respectedFamilyMembers.size() + closeFriends.size() + acquaintances.size();
        remainingConnections = maxNumConnections - nExistingConnections;

		if (nExistingConnections > maxNumConnections){
			 System.out.println("Household has more connections (" + nExistingConnections
					+ ") than maximum allowed connections (" + maxNumConnections + ")");
		}
//		else{
        return remainingConnections;
//		}
	}

    public void setRemainingConnections(){
        int nExistingConnections = respectedFamilyMembers.size() + closeFriends.size() + acquaintances.size();
        remainingConnections = maxNumConnections - nExistingConnections;

        if (nExistingConnections > maxNumConnections){
            System.out.println("Household has more connections (" + nExistingConnections
                    + ") than maximum allowed connections (" + maxNumConnections + ")");
        }
    }


	public void step(SimState state) {
		prepareStep(state);
		//testingForJobError();
		//sets up the simstate
		//JaipurResidentialWUOriginal jaipurWaterUse = (JaipurResidentialWUOriginal) state; 

		monthlyDemand = getThisHouseholdDemand();
		//TODO: is this the right time to put in the data collector?
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
		
		//slow down spread in model
		if(shouldSkipStep(JaipurABM.numStepsSkippedToUpdateUtilityFunctions)){
			return;
		}
		
		//to prevent reversing of conservation decisions (DF toilet installation is permanent, at least in this model for now)
		if(!isConserver){
			calculateUtilityandUpdateConsumption();
		}
	}

	public String getVertexName() {
		return vertexName;
	}

	public void setHouseholdSize(int newHouseholdSize) {
		householdSize = newHouseholdSize;
	}

	public static boolean generateConservationStatus() {
		MersenneTwisterFast rand = new MersenneTwisterFast();
		long num = rand.nextLong(100);    //includes 0.0 and 1.0 in randomly drawn number's interval
		if (num < percentConservers) {
			return true;                            //if random number is smaller than the percent of conservers, return true
		}
		return false;
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
		//int networkSize = thisList.size();
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

	public void calculateUtilityandUpdateConsumption(){
		double RatioFamConservers = calculateRatiosForUtilityFxn(respectedFamilyMembers, famAlreadySpokenTo, true);
		double RatioFamNonConservers = calculateRatiosForUtilityFxn(respectedFamilyMembers, famAlreadySpokenTo, false);
		double RatioFriendConservers = calculateRatiosForUtilityFxn(closeFriends, friendsAlreadySpokenTo, true);
		double RatioFriendNonConservers = calculateRatiosForUtilityFxn(closeFriends, friendsAlreadySpokenTo, false);
		double RatioAcqConservers = calculateRatiosForUtilityFxn(acquaintances, acqAlreadySpokenTo, true);
		double RatioAcqNonConservers = calculateRatiosForUtilityFxn(acquaintances, acqAlreadySpokenTo, false);
		
		int famDelta = calculateDelta(this.independentLikelihoodDFInstall, this.familyLikelihoodDFInstall);
		int friendDelta = calculateDelta(this.independentLikelihoodDFInstall, this.friendLikelihoodDFInstall);
		if(isConserver){
			double randNum = rng.nextDouble(true, true);
			double utilStay = UtilityFunction.calculateUtilityForConserverStayingConserver(RatioFamConservers,
					famDelta, RatioFriendConservers, friendDelta, RatioAcqConservers);
			double utilChange = UtilityFunction.calculateUtilityForConserverBecomingNonConserver(RatioFamNonConservers,
					famDelta, RatioFriendNonConservers, friendDelta, RatioAcqNonConservers);	
		//	System.out.println("utility of staying conserver: " + utilStay + " utility of becoming nonconserver: " + utilChange);
			double probabilityConsToCons = ProbabilityOfBehavior.probabilityConsToCons(utilStay, utilChange);
			if (randNum > probabilityConsToCons){
				isConserver = false;
				System.out.println(vertexName + " is changing from conserver to nonconserver");
			}
//			if (utilChange > utilStay){
//				isConserver = false;
//			}
		} else {
			double randNum = rng.nextDouble(true, true);
			double utilStay = UtilityFunction.calculateUtilityForNonConserverStayingNonConserver(RatioFamNonConservers,
					famDelta, RatioFriendNonConservers, friendDelta, RatioAcqNonConservers);
			double utilChange = UtilityFunction.calculateUtilityForNonConserverBecomingConserver(RatioFamConservers,
					famDelta, RatioFriendConservers, friendDelta, RatioAcqConservers);
		//	System.out.println("utility of staying nonconserver: " + utilStay + " utility of becoming conserver: " + utilChange);
			double probabilityNonConsToNonCons = ProbabilityOfBehavior.probabilityNonConsToNonCons(utilChange, utilStay);
			if (randNum > probabilityNonConsToNonCons){
				isConserver = true;
				//System.out.println(vertexName + " is changing from nonconserver to conserver");
			}
//			if (utilChange > utilStay){
//				isConserver = true;
//			}
		}
	}

	public int calculateDelta(int originalNum, int newNum){
		int delta = newNum - originalNum;
		if (delta < 0){
			delta = 0;
		}
		return delta;
	}

	public void setNetworkSize(){
		//check to see if our total network size is larger than sum of our friends and family numbers
		//if it is, then we set it to the sum of our networks, and that agent just has no acquaintances
		int sumFriendsNFam = maxNumCloseFriends + maxNumFamilyMembers;
		if (sumFriendsNFam > maxNumConnections){
			maxNumConnections = sumFriendsNFam;
		}   	
		maxNumAcquaintances = maxNumConnections - sumFriendsNFam;
		if (maxNumAcquaintances < 0){
			System.out.println("ERROR in setNetworkSize method");
		}
	}

    protected void prepareStep(SimState state){
        String graphStructure = JaipurABM.getGraphStructure();
        if (graphStructure.equalsIgnoreCase("original")){
            double timeStep = state.schedule.getTime();
            assignFamilyToAgentAtTimeStep(timeStep);
            assignCloseFriendsAtTimeStep(timeStep);
            assignAcquaintancesToAgentAtTimeStep(timeStep);
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

//            String msg = String.format("%1$s related %2$s as acquaintences", vertexName, hh.vertexName);
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

    public void assignCloseFriendsAtTimeStep(double timeStep) {
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

	protected boolean doesRelationshipAlreadyExist(UUID targetUuid){
		return relatedUuids.contains(targetUuid);
    }

	public void talk(ArrayList<Household> wholeNetwork, ArrayList<Household> networkTalkedToAlready, SimState state){
		//select random member of arrayList, then, if he doesn't already exist in the talkedTo list, add him. if he does, return and let the next agent go
		if(shouldSkipStep(JaipurABM.numStepsSkippedToUpdateTalkFunction)){
			return;
		}
		ArrayList<Household> shuffledNetwork = wholeNetwork;
		Collections.shuffle(shuffledNetwork);
		Household randHH = null;
		if(shuffledNetwork.isEmpty()){
			//System.out.println("shuffledNetwork has nobody!");
			return;
		}
		//added to get new network structures to change over time
		for (Household hh : shuffledNetwork){
			double currentTimeStep = state.schedule.getTime();
			if(hh.timeStepBorn > currentTimeStep){
				System.out.println(hh.getVertexName() + "was born at time step " + hh.timeStepBorn + " and current time step is " + state.schedule.getTime());
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
						//System.out.println(hh.vertexName + " was found in network, exiting");
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

	public void talkToThreeNetworks(SimState state){
		int num = rng.nextInt(3) + 1;
		//if num is 1 and fam is empty or if num is 2 and friends are empty or num is 3 and acq is empty, pick a new num
		while (num == 1 && respectedFamilyMembers.isEmpty() || num == 2 && closeFriends.isEmpty() || num == 3 && acquaintances.isEmpty()){
			num = rng.nextInt(3) + 1;
		}

		//select one network to communicate with for this timestep
		if (num == 1){
			//communicate with family
			//    		System.out.println("fam spoken to before talk for " + this.vertexName + ": ");
			//    		for (HHwPlumbing2 hh: famAlreadySpokenTo){
			//    			System.out.print(hh.vertexName + " ");
			//    		}
			//    		System.out.println();
			talk(respectedFamilyMembers, famAlreadySpokenTo, state);
			//    		System.out.println("fam spoken to after talk for " + this.vertexName + ": ");
			//    		for (HHwPlumbing2 hh: famAlreadySpokenTo){
			//    			System.out.print(hh.vertexName + " ");
			//    		}
			//    		System.out.println();
		}
		else if (num == 2){
			//communicate with friends
			//    		System.out.println("friends spoken to before talk for " + this.vertexName + ": ");
			//    		for (HHwPlumbing2 hh: friendsAlreadySpokenTo){
			//    			System.out.print(hh.vertexName + " ");
			//    		}
			talk(closeFriends, friendsAlreadySpokenTo, state);
			//    		System.out.println("friends spoken to after talk for " + this.vertexName + ": ");
			//    		for (HHwPlumbing2 hh: friendsAlreadySpokenTo){
			//    			System.out.print(hh.vertexName + " ");
			//    		}
		}
		else if (num == 3){
			//    		//communicate with acq
			//    		System.out.println("acq spoken to before talk for " + this.vertexName + ": ");
			//    		for (HHwPlumbing2 hh: acqAlreadySpokenTo){
			//    			System.out.print(hh.vertexName + " ");
			//    		}
			talk(acquaintances, acqAlreadySpokenTo, state);
			//    		System.out.println("acq spoken to after talk for " + this.vertexName + ": ");
			//    		for (HHwPlumbing2 hh: acqAlreadySpokenTo){
			//    			System.out.print(hh.vertexName + " ");
			//    		}
		}
		else{
			System.out.println("error in rng for talk method");
			return;
		}
	}

	public boolean shouldSkipStep(int numTries){
	//		for(int i = 0; i < numTries; i++){
	//			double randNum = rng.nextDouble(true, false);
	//			if (randNum > 0.5){
	//				return true;
	//			}
	//		}
			double chance = 1.0/numTries;
			double randNum = rng.nextDouble(true, true);
			if(randNum > chance){
				return true;
			}
			return false;
		}

	public UUID getUUID(){	
		return this.uuid;
	}

	public ArrayList<Household> getAcquaintances(){
		return acquaintances;
	}

	public void clearAllNetworks(){
		acquaintances.clear();
		closeFriends.clear();
		respectedFamilyMembers.clear();
		acqAlreadySpokenTo.clear();
		friendsAlreadySpokenTo.clear();
		famAlreadySpokenTo.clear();
	}
	
	public void testingForJobError(){
		if(this.vertexName.equals("vert1")){
			System.out.println("vert name: " + this.vertexName + ", uuid: " + this.uuid);
			System.out.println("Job Number " + JaipurABM.currentJob + " -- acq: ");
			for (Household hh: acquaintances){
				System.out.println(" acq vert name: " + hh.vertexName + " acq uuid name: " + hh.uuid);
			}
			System.out.println();
		}
	}
	
}

