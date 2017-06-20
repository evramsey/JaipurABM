package JaipurABM;

import ec.util.MersenneTwisterFast;
import sim.util.distribution.*;

public class ValueGenerator {
	static MersenneTwisterFast rng = new MersenneTwisterFast();

	public static int getValueLikert(double avg, double stdDev) {
		double normDistSample = createNormalDistSample(avg, stdDev);
		while (normDistSample < 0 || normDistSample == 0) {
			normDistSample = createNormalDistSample(avg, stdDev);
		}
		int intValue = (int) (Math.round(normDistSample));
		return intValue;
	}

	public static double createNormalDistSample(double avg, double stdDev) {
		double value = rng.nextGaussian();
		double normDistSample = value * stdDev + avg;
		return normDistSample;
	}


	public static int getPoissonValue(double avg) {
		Poisson poissonDist = new Poisson(avg, rng);
		int value = poissonDist.nextInt();
		while (value == 0 || value < 0) {
			value = poissonDist.nextInt();
		}
		return value;
	}

	public static int getPoissonValueWithMaxAndMin(double avg, int min, int max) {
		int value = getPoissonValue(avg);
		while (value > max || value < min) {
			value = getPoissonValue(avg);
		}
		return value;
	}

	public static double generateSocialPressureDelta(double avg, double stdDev) {
		double normDistSample = createNormalDistSample(avg, stdDev);
		while (normDistSample < 0 || normDistSample == 0) {
			normDistSample = createNormalDistSample(avg, stdDev);
		}
		return normDistSample;
	}
}


