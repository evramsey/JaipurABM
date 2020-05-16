public class ProbabilityOfBehavior {
	private static double beta;
	
	public static double probabilityConsToCons(double utilConsToCons, double utilConsToNonCons){
		double numerator = Math.exp(utilConsToCons * beta);
		double denominator = Math.exp(utilConsToCons * beta) + (Math.exp(utilConsToNonCons * beta));	
		double probability = numerator/denominator;
		return probability;
	}

	public static double probabilityNonConsToNonCons(double utilNonConsToCons, double utilNonConsToNonCons){
		double numerator = Math.exp(utilNonConsToNonCons * beta);
		double denominator = (Math.exp(utilNonConsToCons * beta) + Math.exp(utilNonConsToNonCons * beta));	
		double probability = numerator/denominator;
		return probability;
	}

	public static void setBeta(double betaVal){
		beta = betaVal;
	}

	public static double getBeta(){
		return beta;
	}
}
