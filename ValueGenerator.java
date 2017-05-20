package JaipurABM;

import org.apache.commons.math3.distribution.*;

public class ValueGenerator {
	
	public static int getValueLikert(double avg, double stdDev){
		NormalDistribution normDist = new NormalDistribution(avg,stdDev);
		double generatedValue = -1;
		while (generatedValue < 1 || generatedValue > 5){
			generatedValue = normDist.sample();
		}
		int intValue = (int) (Math.round(generatedValue));			
		return intValue;
	}
	
	public static int getValueWithRange(double avg, double stdDev, int min, int max){
		NormalDistribution normDist = new NormalDistribution(avg,stdDev);
		double generatedValue = -1;
		while (generatedValue < min || generatedValue > max){
			generatedValue = normDist.sample();
		}
		int intValue = (int) (Math.round(generatedValue));			
		return intValue;
	}

	public static int getPoissonValue(double avg){
		PoissonDistribution poissondist = new PoissonDistribution(avg);
		int value = poissondist.sample();
		return value;
	}

	public static int getPoissonValueWithMaxAndMin(double avg, int min, int max){
		int value = getPoissonValue(avg);
		while (value > max || value < min){
			value = getPoissonValue(avg);
		}
		return value;
	}
}
