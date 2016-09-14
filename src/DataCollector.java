import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


import sim.engine.SimState;
import sim.engine.Steppable;
import com.opencsv.CSVWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.channels.FileChannel;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.stream.Collectors;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.charset.Charset;

/**
 * 
 * @author lizramsey
 *collects demand, population, and number of agents after each time step (at 0.01, 1.01, etc.)
 *puts those into a txt file
 *for all input files, include an extra line of values at the bottom; i.e.
 *if there are 100 timesteps required, add one extra line to make 101 timesteps, so the dataCollector
 *can collect data at 100.1
 *
 */
public class DataCollector implements Steppable{
	public static String in_filename;
	public static String time_simulation_start;

	public static List<String> input_parameters;

	public static double CumulativeDemand;
	public int percentConservers;
	public static String txtFileInput;
	public static int numAgents;
	public static int population;
	public static int modelPopulation;
	public static int numConservers;

	/**
	 * Constructor
	 */
	public DataCollector(){}

	public void step(SimState state) {
		JaipurABM jaipurWaterUse = (JaipurABM) state;

		int i = in_filename.contains(".") ? in_filename.lastIndexOf('.') : in_filename.length();
		String output_file_name = "./output/" + time_simulation_start + "/" + in_filename.substring(0, i) + "_output.csv";

		getDataAtTimeStep(state.schedule.getTime());

		double ratio = getConserverRatioThisTimeStep();

        CSVWriter writer = null;
        try {

			final File file = new File(output_file_name);
			final File parent_directory = file.getParentFile();

			if (null != parent_directory)
			{
				parent_directory.mkdirs();
			}

            writer = new CSVWriter(new FileWriter(output_file_name, true), ',');
			ArrayList<String> entries = new ArrayList<>();
			ArrayList<String> headers = new ArrayList<>();

			headers.add("Sim_Set");
			entries.add(time_simulation_start);
			headers.add("In_Filename");
			entries.add(in_filename);

			headers.add("Job");
			entries.add(String.valueOf(JaipurABM.getCurrentJob()));
			headers.add("Timestep");
			entries.add(String.valueOf(state.schedule.getTime()));
			headers.add("Population");
			entries.add(String.valueOf(modelPopulation));
			headers.add("Agents");
			entries.add(String.valueOf(numAgents));
			headers.add("Conservers");
			entries.add(String.valueOf(numConservers));
			headers.add("Conserver_Ratio");
			entries.add(String.valueOf(ratio));
			headers.add("Cumulative_Demand");
			entries.add(String.valueOf(CumulativeDemand));

			headers.add("A");
			entries.add(String.valueOf(UtilityFunction.a));
			headers.add("A_Prime");
			entries.add(String.valueOf(UtilityFunction.aPrime));
			headers.add("B");
			entries.add(String.valueOf(UtilityFunction.b));
			headers.add("B_Prime");
			entries.add(String.valueOf(UtilityFunction.bPrime));
			headers.add("Exogenous_Term");
			entries.add(String.valueOf(UtilityFunction.exogenousTerm));
			headers.add("Beta");
			entries.add(String.valueOf(ProbabilityOfBehavior.beta));
			headers.add("Delta");
			entries.add(String.valueOf(UtilityFunction.parameterDelta));
			headers.add("Skipped_Steps_Utility");
			entries.add(String.valueOf(JaipurABM.numStepsSkippedToUpdateUtilityFunctions));
			headers.add("Skipped_Steps_Talk");
			entries.add(String.valueOf(JaipurABM.numStepsSkippedToUpdateTalkFunction));

//            // feed in your array (or convert your data to an array)
//            String entryString = JaipurABM.getCurrentJob() + "\t" + state.schedule.getTime() + "\t" +
//                    modelPopulation + "\t" + numAgents + "\t" + numConservers + "\t" + ratio +"\t"+ CumulativeDemand;

			String[] headerArray = headers.toArray(new String[0]);
			String[] entryArray = entries.toArray(new String[0]);

			if (!file.exists() || file.length() == 0){
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
        }

        numAgents = 0;
		CumulativeDemand = 0.0;
		modelPopulation = 0;
		numConservers = 0;
		ratio = 0;
	}

	public static void updateTxtFile(String s){
		txtFileInput = txtFileInput + s;
	}

	public double getConserverRatioThisTimeStep(){
		double numConserversDub = (double)(numConservers);
		double numAgentsDub = (double)(numAgents);
		double ratio = numConserversDub/numAgentsDub;
		return ratio;
	}

	public void getDataAtTimeStep(double timeStep){
		List<Household> nHouseholdsAtTs = Household.houseHoldAgents.stream()
				.filter(h -> h.timeStepBorn <= timeStep)
				.collect(Collectors.toList());

//		CumulativeDemand 	= nHouseholdsAtTs.stream().mapToDouble(i -> i.getThisHouseholdDemand()).sum();
//		modelPopulation 	= nHouseholdsAtTs.stream().mapToInt(i -> i.householdSize).sum();
//		numAgents 			= nHouseholdsAtTs.size();
//		numConservers 		= (int) nHouseholdsAtTs.stream().filter(i -> i.isConserver == true).count();
	}

    public void writeData(String data,String strFilePath)
    {

    }

//    public static void aggregateResults(){
//		File[] files = new File("./output/" + time_simulation_start).listFiles();
//		//If this pathname does not denote a directory, then listFiles() returns null.
//		try(FileChannel outputChannel = new FileOutputStream("./output/" + time_simulation_start
//				+ "/" + time_simulation_start + "_aggregate.csv").getChannel()) {
//			long position = 0;
//			for (File file : files) {
//				if (file.isFile()) {
//					try(FileChannel inputChannel = new FileInputStream(new File(file.getPath())).getChannel()){
//						position += inputChannel.transferTo(0, inputChannel.size(), outputChannel);
//					}
//				}
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}

	public static void aggregateResults() throws IOException{
		File[] files = new File("./output/" + time_simulation_start).listFiles();

		List<String> mergedLines = new ArrayList<> ();
		for (File f : files){
			Path p = Paths.get(f.getPath());
			List<String> lines = Files.readAllLines(p, Charset.forName("UTF-8"));
			if (!lines.isEmpty()) {
				if (mergedLines.isEmpty()) {
					mergedLines.add(lines.get(0)); //add header only once
				}
				mergedLines.addAll(lines.subList(1, lines.size()));
			}
		}

		Path target = Paths.get("./output/" + time_simulation_start
				+ "/" + time_simulation_start + "_aggregate.csv");
		Files.write(target, mergedLines, Charset.forName("UTF-8"));
	}

}