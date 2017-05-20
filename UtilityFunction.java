package JaipurABM;

public class UtilityFunction {
	public static double a;
	public static double aPrime;
	public static double bPrime;
	public static double b;
	public static double exogenousTerm;
	public static double parameterDelta;

	
	
	public static double calculateUtilityForConserverStayingConserver(double famRatio, int famDelta, double friendRatio, 
															int friendDelta, double acqRatio){
		
		
		double aFam = updateInitialParameter(famDelta, a);
		double aFriends = updateInitialParameter(friendDelta, a);
//		double aFam = a;
//		double aFriends = a;
		
		double utility = (a * acqRatio) + (aFam * famRatio) + (aFriends * friendRatio) + exogenousTerm;
		return utility;
	}
	
	public static double calculateUtilityForConserverBecomingNonConserver(double famRatio, int famDelta, double friendRatio, 
																int friendDelta, double otherRatio){
		//UPDATED TO TEST original a/b trends
		double bFam = updateInitialParameter(famDelta, b);
		double bFriends = updateInitialParameter(friendDelta, b);
//		double bFam = b;
//		double bFriends = b;
		double utility = (b * otherRatio) + (bFam * famRatio) + (bFriends * friendRatio);
		return utility;
	}
	
	public static double calculateUtilityForNonConserverBecomingConserver(double famRatio, int famDelta, double friendRatio, 
																int friendDelta, double otherRatio){
		double aPrimeFam = updateInitialParameter(famDelta, aPrime);
		double aPrimeFriends = updateInitialParameter(friendDelta, aPrime);
		double utility = (aPrime * otherRatio) + (aPrimeFam * famRatio) + (aPrimeFriends * friendRatio) + exogenousTerm;
		//System.out.println("utility of changing: " + utility);
		return utility;
	}
	
	public static double calculateUtilityForNonConserverStayingNonConserver(double famRatio, int famDelta, double friendRatio, 
			int friendDelta, double otherRatio){

		double bPrimeFam = updateInitialParameter(famDelta, bPrime);
		double bPrimeFriends = updateInitialParameter(friendDelta, bPrime);
		double utility = (bPrime * otherRatio) + (bPrimeFam * famRatio) + (bPrimeFriends * friendRatio);
		//System.out.println("utility of staying: " + utility);
		return utility;
	}
	
	//this changes parameters for both switching behavior and not switching, for both conservers and nonconservers
	public static double updateInitialParameter(int delta, double coefficient){
		double newValue = 0.0;
		if(delta < 2 && delta > 0){ //increase a or b by 0.05 if delta = 1
			newValue =  coefficient + parameterDelta;
		}
		else if(delta == 2 || delta > 2){ //increase by 0.1 if delta = 2 or more
			newValue =  coefficient + (2 * parameterDelta);
		}
		else{
			newValue = coefficient;
		}
		return newValue;
	}
}
