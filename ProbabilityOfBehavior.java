package JaipurABM;

public class ProbabilityOfBehavior {
	private static double beta = 0.5;
	
	public static double probabilityConsToCons(double utilConsToCons, double utilConsToNonCons){
		double numerator = Math.exp(utilConsToCons * beta);
		double denominator = Math.exp(utilConsToCons * beta) + (Math.exp(utilConsToNonCons * beta));	
		double probability = numerator/denominator;
		//System.out.println("probability cons to cons: " + probability);
		return probability;
	}
	
	public static double probabilityConsToNonCons(double utilConsToCons, double utilConsToNonCons){
		double numerator = Math.exp(beta * utilConsToNonCons);
		double denominator = Math.exp(beta * utilConsToCons) + Math.exp(beta * utilConsToNonCons);	
		double probability = numerator/denominator;
//		System.out.println("util cons to cons: " + utilConsToCons + " util cons to noncons: " + utilConsToNonCons);
//		System.out.println("probability cons to noncons: " + probability);
		return probability;
	}
	
	public static double probabilityNonConsToCons(double utilNonConsToCons, double utilNonConsToNonCons){
		double numerator = Math.exp(utilNonConsToCons * beta);
		double denominator = (Math.exp(utilNonConsToCons * beta) + Math.exp(utilNonConsToNonCons * beta));	
		double probability = numerator/denominator;
//		System.out.println("util noncons to cons: " + utilNonConsToCons + " util noncons to noncons: " + utilNonConsToNonCons);
//		System.out.println("probability noncons to cons: " + probability);
		return probability;
	}
	
	public static double probabilityNonConsToNonCons(double utilNonConsToCons, double utilNonConsToNonCons){
		double numerator = Math.exp(utilNonConsToNonCons * beta);
		double denominator = (Math.exp(utilNonConsToCons * beta) + Math.exp(utilNonConsToNonCons * beta));	
		double probability = numerator/denominator;
//		System.out.println("util noncons to cons: " + utilNonConsToCons + " util noncons to noncons: " + utilNonConsToNonCons);
//		System.out.println("probability noncons to noncons: " + probability);
		return probability;
	}

	public static void setBeta(double betaVal){
		beta = betaVal;
	}

	public static double getBeta(){
		return beta;
	}
}
