import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import sim.engine.SimState;
import sim.engine.Steppable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 *Collect demand, population, and number of agents after each time step (at 0.01, 1.01, etc.)
 *puts those into a txt file
 *for all input files, include an extra line of values at the bottom; i.e.
 *if there are 100 timesteps required, add one extra line to make 101 timesteps, so the dataCollector
 *can collect data at 100.1
 *
 */
public class DataCollector implements Steppable {
    public static String in_filename;
    public static String outputFileIdentifier;
    public static int numcalcs = 1;
    public static double CumulativeDemand;
    public static int numAgents;
    public static int population;
    public static int modelPopulation;
    public static int numConservers;
    private static boolean deleteAutogenTxt;
    private static double droughtChance = 0.293;

    /**
     * Constructor
     */
    public DataCollector() {
    }

    /**
     * Step function of scheduler; collect model outputs and place in .csv file
     * @param state SimState passed by scheduler
     */
    public void step(SimState state) {
        int i = in_filename.contains(".") ? in_filename.lastIndexOf('.') : in_filename.length();
        String output_file_name = "./output/" + outputFileIdentifier + "/" + in_filename.substring(0, i) + "_output.csv";
        double ratio = getConserverRatioThisTimeStep();

        CSVWriter writer = null;
        try {
            final File file = new File(output_file_name);
            final File parent_directory = file.getParentFile();
            if (null != parent_directory) {
                parent_directory.mkdirs();
            }
            writer = new CSVWriter(new FileWriter(output_file_name, true), ',');
            ArrayList<String> entries = new ArrayList<>();
            ArrayList<String> headers = new ArrayList<>();
            headers.add("Sim Set");
            entries.add(outputFileIdentifier);
            headers.add("In_Filename");
            entries.add(in_filename);
            headers.add("Job");
            entries.add(String.valueOf(dftABM.getCurrentJob()));
            headers.add("Timestep");
            entries.add(String.valueOf(state.schedule.getTime()));
            headers.add("Population");
            entries.add(String.valueOf(modelPopulation));
            headers.add("Agents");
            entries.add(String.valueOf(numAgents));
            headers.add("Conservers");
            entries.add(String.valueOf(numConservers));
            headers.add("Conserver Ratio");
            entries.add(String.valueOf(ratio));
            headers.add("Cumulative Demand");
            entries.add(String.valueOf(CumulativeDemand));
            headers.add("A");
            entries.add(String.valueOf(UtilityFunction.getAandBPrime()));
            headers.add("B");
            entries.add(String.valueOf(UtilityFunction.getBandAPrime()));
            headers.add("Exogenous Term");
            entries.add(String.valueOf(Household.getExogenousTermSeed()));
            headers.add("Beta");
            entries.add(String.valueOf(ProbabilityOfBehavior.getBeta()));
            String[] headerArray = headers.toArray(new String[0]);
            String[] entryArray = entries.toArray(new String[0]);
            if (!file.exists() || file.length() == 0) {
                writer.writeNext(headerArray);
            }
            writer.writeNext(entryArray);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            writer.flush();
            writer.close();
        } catch (IOException e) {
            writer = null;
            e.printStackTrace();
        }
        numAgents = 0;
        CumulativeDemand = 0.0;
        modelPopulation = 0;
        numConservers = 0;
        double time = state.schedule.getTime();
        if (((time - 0.1) % 12) == 0) {
            setDroughtIndex(state);
        }
    }

    /**
     * Calculate the ratio of conservers at given time step
     * @return ratio
     */
    public double getConserverRatioThisTimeStep() {
        double numConserversDub = (double) (numConservers);
        double numAgentsDub = (double) (numAgents);
        double ratio = numConserversDub / numAgentsDub;
        return ratio;
    }

    /**
     * Get the percentage of values at a given time step from survey results; hardcoded in
     * @return percentage
     */
    public static double[] get_survey_values_percentage() {
        double[] survey_values = new double[19];
        survey_values[0] = 0.005050505; //by end of year 1997
        survey_values[1] = 0.005050505;
        survey_values[2] = 0.005050505;
        survey_values[3] = 0.015151515; //by end of 2000
        survey_values[4] = 0.015151515;
        survey_values[5] = 0.015151515;
        survey_values[6] = 0.02020202;
        survey_values[7] = 0.02020202;
        survey_values[8] = 0.04040404;//by end of 2005
        survey_values[9] = 0.04040404;
        survey_values[10] = 0.045454545;
        survey_values[11] = 0.065656566;
        survey_values[12] = 0.085858586;
        survey_values[13] = 0.095959596;//by end of 2006 2010
        survey_values[14] = 0.111111111;
        survey_values[15] = 0.126262626;
        survey_values[16] = 0.176767677;
        survey_values[17] = 0.232323232;
        survey_values[18] = 0.262626263;// by end of 2015
        return survey_values;
    }

    /**
     * Get the expected raw number of agents at a given time step from survey results; hardcoded in
     * @return expected number of agents
     */
    public static double[] get_survey_values_raw_num_agents() {
        double[] survey_values = new double[19];
        survey_values[0] = 20.61; //by end of year 1997
        survey_values[1] = 21.40;
        survey_values[2] = 22.20;
        survey_values[3] = 68.95; //by end of 2000
        survey_values[4] = 71.11;
        survey_values[5] = 73.26;
        survey_values[6] = 100.55;
        survey_values[7] = 103.41;
        survey_values[8] = 212.57;//by end of 2005
        survey_values[9] = 218.30;
        survey_values[10] = 252.00;
        survey_values[11] = 373.52;
        survey_values[12] = 500.38;
        survey_values[13] = 575.47;//by end of 2010
        survey_values[14] = 717.89;
        survey_values[15] = 874.49;
        survey_values[16] = 1306.31;
        survey_values[17] = 1824.67;
        survey_values[18] = 2174.55;// by end of 2015
        return survey_values;
    }

    /**
     * Calculate the standard error, S, of the model simulation for the given run
     * @param jobNum the number of the current simulation
     * @return S
     * @throws IOException
     */
	public static double calculateStandardError(int jobNum) throws IOException {
		double[] survey_values = get_survey_values_raw_num_agents();
		double[] model_values = new double[19];
		int fi = in_filename.contains(".") ? in_filename.lastIndexOf('.') : in_filename.length();
		String out_filename = "./output/" + outputFileIdentifier + "/" + in_filename.substring(0, fi) + "_output.csv";
		CSVReader reader = new CSVReader(new FileReader(out_filename));
		List<String[]> lines = reader.readAll();
		reader.close();
		int numCalcSteps = 19;
		int m_ctr = 0;
		if (deleteAutogenTxt) {
			jobNum = 0;
		}
        int selectionStartNum = (12 * jobNum * numCalcSteps) + jobNum + 13;
        int selectionEndNum = 12 * numCalcSteps * (jobNum + 1) + jobNum + 1;
        for (int i = selectionStartNum; i <= selectionEndNum; i = i + 12) {
            String[] row = lines.get(i);
            model_values[m_ctr] = Double.parseDouble(row[6]);
            m_ctr++;
        }
		double sum = 0.0;
		for (int k = 0; k < numCalcSteps; k++) {
			double dif = survey_values[k] - model_values[k];
			double sqDif = dif * dif;
			sum = sum + sqDif;
		}
		double standardError = Math.sqrt(sum / (numCalcSteps - 2));
		String output_file_name = "./output/" + outputFileIdentifier + "/standardError_output.csv";
		CSVWriter writer = null;
		try {
			final File file = new File(output_file_name);
			final File parent_directory = file.getParentFile();

			if (null != parent_directory) {
				parent_directory.mkdirs();
			}
			writer = new CSVWriter(new FileWriter(output_file_name, true), ',');
			ArrayList<String> entries = new ArrayList<>();
			ArrayList<String> headers = new ArrayList<>();
			headers.add("Sim_Set");
			entries.add(outputFileIdentifier);
			headers.add("In_Filename");
			entries.add(in_filename);
			headers.add("Job");
			entries.add(String.valueOf(dftABM.getCurrentJob()));
			headers.add("Standard Error");
			entries.add(String.valueOf(standardError));
			headers.add("A");
			entries.add(String.valueOf(UtilityFunction.getAandBPrime()));
			headers.add("B");
			entries.add(String.valueOf(UtilityFunction.getBandAPrime()));
			headers.add("Exogenous Term Average");
			entries.add(String.valueOf(Household.getExogenousTermSeed()));
			headers.add("Beta");
			entries.add(String.valueOf(ProbabilityOfBehavior.getBeta()));
			String[] headerArray = headers.toArray(new String[0]);
			String[] entryArray = entries.toArray(new String[0]);
			if (!file.exists() || file.length() == 0) {
				writer.writeNext(headerArray);
			}
			writer.writeNext(entryArray);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			writer.flush();
			writer.close();
		}
		numcalcs++;
		return standardError;
	}

	public static void setDeleteAutogenTxt(boolean delTxt) {
		deleteAutogenTxt = delTxt;
	}

	public static void setDroughtIndex(SimState state) {
		boolean[] wasDroughtYear = new boolean[20];
        wasDroughtYear[0] = false; //1996 wasn't drought year (year prior to simulation start)
		wasDroughtYear[1] = false; //1997 wasn't drought year
		wasDroughtYear[2] = false;
		wasDroughtYear[3] = true;
		wasDroughtYear[4] = true; //2000
		wasDroughtYear[5] = true;
		wasDroughtYear[6] = true;
		wasDroughtYear[7] = false;
		wasDroughtYear[8] = false;
		wasDroughtYear[9] = true; //2005
		wasDroughtYear[10] = true;
		wasDroughtYear[11] = false;
		wasDroughtYear[12] = false;
		wasDroughtYear[13] = true;
		wasDroughtYear[14] = false; //2010
		wasDroughtYear[15] = false;
		wasDroughtYear[16] = false;
		wasDroughtYear[17] = false;
		wasDroughtYear[18] = false;
		wasDroughtYear[19] = false; //2015

		Double time = state.schedule.getTime();

		int timeInt = (int) (time - 0.1);
		dftABM.setLastYearDroughtIndex(wasDroughtYear[timeInt / 12]);
	}

    /**
     * Decide whether current year is a drought, given a probability of drought; use with randomized simulations
     * @param randNum
     * @return
     */
	public static boolean updateDroughtIndex(double randNum) {
		if (randNum < droughtChance) {
			return true;
		}
		return false;
	}

    /**
     * Calculate the Nash-Sutcliffe Efficiency of the model simulation for a given simulation
     * @param jobNum the simulation number
     * @return nse
     * @throws IOException
     */
	public static double calculateNSE(int jobNum) throws IOException {
		double[] survey_values = get_survey_values_raw_num_agents();
		double[] model_values = new double[19];
		int fi = in_filename.contains(".") ? in_filename.lastIndexOf('.') : in_filename.length();
		String out_filename = "./output/" + outputFileIdentifier + "/" + in_filename.substring(0, fi) + "_output.csv";
		CSVReader reader = new CSVReader(new FileReader(out_filename));
		List<String[]> lines = reader.readAll();
		reader.close();
		int numCalcSteps = 19;
		int m_ctr = 0;
		if (deleteAutogenTxt) {
			jobNum = 0;
		}
        int selectionStartNum = (12 * jobNum * numCalcSteps) + jobNum + 13;
        int selectionEndNum = 12 * numCalcSteps * (jobNum + 1) + jobNum + 1;
        for (int i = selectionStartNum; i <= selectionEndNum; i = i + 12) {
            String[] row = lines.get(i);
            model_values[m_ctr] = Double.parseDouble(row[6]);
            m_ctr++;
        }
		double numerator = 0.0;
        double denominator = 0.0;
        double sum_obs = 0.0;
        for (int i = 0; i < numCalcSteps; i++){
            sum_obs = sum_obs + survey_values[i];
        }
        double mean_obs = sum_obs / numCalcSteps;
		for (int k = 0; k < numCalcSteps; k++) {
			double numerator_dif = survey_values[k] - model_values[k];
			double sq_num_dif = numerator_dif * numerator_dif;
			numerator = numerator + sq_num_dif;
            double denominator_dif = survey_values[k] - mean_obs;
            double sq_den_dif = denominator_dif * denominator_dif;
            denominator = denominator + sq_den_dif;
		}
		double nse = 1.0 - (numerator/denominator);
		String output_file_name = "./output/" + outputFileIdentifier + "/nash_sutcliffe_output.csv";
		CSVWriter writer = null;
		try {
			final File file = new File(output_file_name);
			final File parent_directory = file.getParentFile();

			if (null != parent_directory) {
				parent_directory.mkdirs();
			}
			writer = new CSVWriter(new FileWriter(output_file_name, true), ',');
			ArrayList<String> entries = new ArrayList<>();
			ArrayList<String> headers = new ArrayList<>();
			headers.add("Sim_Set");
			entries.add(outputFileIdentifier);
			headers.add("In_Filename");
			entries.add(in_filename);
			headers.add("Job");
			entries.add(String.valueOf(dftABM.getCurrentJob()));
			headers.add("Nash Sutcliff Efficiency");
			entries.add(String.valueOf(nse));
			headers.add("A");
			entries.add(String.valueOf(UtilityFunction.getAandBPrime()));
			headers.add("B");
			entries.add(String.valueOf(UtilityFunction.getBandAPrime()));
			headers.add("Exogenous Term Average");
			entries.add(String.valueOf(Household.getExogenousTermSeed()));
			headers.add("Beta");
			entries.add(String.valueOf(ProbabilityOfBehavior.getBeta()));
			String[] headerArray = headers.toArray(new String[0]);
			String[] entryArray = entries.toArray(new String[0]);
			if (!file.exists() || file.length() == 0) {
				writer.writeNext(headerArray);
			}
			writer.writeNext(entryArray);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			writer.flush();
			writer.close();
		}
		numcalcs++;
	return nse;
	}

    /**
     * Calculate the Nash-Sutcliffe Efficiency of the model simulation for a given simulation
     * @param jobNum the simulation number
     * @return mnse
     * @throws IOException
     */
    public static double calculate_mod_NSE(int jobNum) throws IOException {
        double[] survey_values = get_survey_values_raw_num_agents();
        double[] model_values = new double[19];
        int fi = in_filename.contains(".") ? in_filename.lastIndexOf('.') : in_filename.length();
        String out_filename = "./output/" + outputFileIdentifier + "/" + in_filename.substring(0, fi) + "_output.csv";
        CSVReader reader = new CSVReader(new FileReader(out_filename));
        List<String[]> lines = reader.readAll();
        reader.close();
        int numCalcSteps = 19;
        int m_ctr = 0;
        if (deleteAutogenTxt) {
            jobNum = 0;
        }
        int selectionStartNum = (12 * jobNum * numCalcSteps) + jobNum + 13;
        int selectionEndNum = 12 * numCalcSteps * (jobNum + 1) + jobNum + 1;
        for (int i = selectionStartNum; i <= selectionEndNum; i = i + 12) {
            String[] row = lines.get(i);
            model_values[m_ctr] = Double.parseDouble(row[6]);
            m_ctr++;
        }
        double numerator = 0.0;
        double denominator = 0.0;
        double sum_obs = 0.0;
        for (int i = 0; i < numCalcSteps; i++){
            sum_obs = sum_obs + survey_values[i];
        }
        double mean_obs = sum_obs / numCalcSteps;
        for (int k = 0; k < numCalcSteps; k++) {
            double numerator_val = (survey_values[k] - model_values[k]) / survey_values[k];
            double sq_num = numerator_val * numerator_val;
            numerator = numerator + sq_num;
            double denominator_val = (survey_values[k] - mean_obs) / mean_obs;
            double sq_den = denominator_val * denominator_val;
            denominator = denominator + sq_den;
        }
        double mnse = 1.0 - (numerator/denominator);
        String output_file_name = "./output/" + outputFileIdentifier + "/mod_nash_sutcliffe_output.csv";
        CSVWriter writer = null;
        try {
            final File file = new File(output_file_name);
            final File parent_directory = file.getParentFile();

            if (null != parent_directory) {
                parent_directory.mkdirs();
            }
            writer = new CSVWriter(new FileWriter(output_file_name, true), ',');
            ArrayList<String> entries = new ArrayList<>();
            ArrayList<String> headers = new ArrayList<>();
            headers.add("Sim_Set");
            entries.add(outputFileIdentifier);
            headers.add("In_Filename");
            entries.add(in_filename);
            headers.add("Job");
            entries.add(String.valueOf(dftABM.getCurrentJob()));
            headers.add("Modified Nash Sutcliffe Efficiency");
            entries.add(String.valueOf(mnse));
            headers.add("A");
            entries.add(String.valueOf(UtilityFunction.getAandBPrime()));
            headers.add("B");
            entries.add(String.valueOf(UtilityFunction.getBandAPrime()));
            headers.add("Exogenous Term Average");
            entries.add(String.valueOf(Household.getExogenousTermSeed()));
            headers.add("Beta");
            entries.add(String.valueOf(ProbabilityOfBehavior.getBeta()));
            String[] headerArray = headers.toArray(new String[0]);
            String[] entryArray = entries.toArray(new String[0]);
            if (!file.exists() || file.length() == 0) {
                writer.writeNext(headerArray);
            }
            writer.writeNext(entryArray);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            writer.flush();
            writer.close();
        }
        numcalcs++;
        return mnse;
    }
}
