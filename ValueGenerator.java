import ec.util.MersenneTwisterFast;
import sim.util.distribution.*;

/**
 * Generate random samples pulled from a given distribution
 */
public class ValueGenerator {
	static MersenneTwisterFast rng = new MersenneTwisterFast();

    /**
     * Generate random sample from a normal distribution
     * @param avg mean of the distribution
     * @param stdDev standard deviation of the distribution
     * @return sample value
     */
	public static double createNormalDistSample(double avg, double stdDev) {
		double normDistSample = -1.0;
		while(normDistSample < 0) {
			double value = rng.nextGaussian();
			normDistSample = value * stdDev + avg;
		}
		return normDistSample;
	}

    /**
     * Generate random sample from a poisson distribution
     * @param avg mean of the distribution
     * @return sample value
     */
	public static int getPoissonValue(double avg) {
		Poisson poissonDist = new Poisson(avg, rng);
		int value = poissonDist.nextInt();
		while (value < 0) {
			value = poissonDist.nextInt();
		}
		return value;
	}
}
