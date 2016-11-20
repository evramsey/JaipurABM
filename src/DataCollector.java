import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


import com.opencsv.CSVReader;
import sim.engine.SimState;
import sim.engine.Steppable;
import com.opencsv.CSVWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.util.UUID;
import java.util.stream.Collectors;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.charset.Charset;

import java.io.BufferedWriter;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.awt.Color;
import java.util.stream.DoubleStream;

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

	public static String out_filename;

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

		//getDataAtTimeStep(state.schedule.getTime());

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

	public static double[] get_survey_values(){
		double[] survey_values = new double[20];
		survey_values[0] = 0.005050505;
		survey_values[1] = 0.005050505;
		survey_values[2] = 0.005050505;
		survey_values[3] = 0.015151515;
		survey_values[4] = 0.015151515;
		survey_values[5] = 0.015151515;
		survey_values[6] = 0.02020202;
		survey_values[7] = 0.02020202;
		survey_values[8] = 0.04040404;
		survey_values[9] = 0.04040404;
		survey_values[10] = 0.045454545;
		survey_values[11] = 0.065656566;
		survey_values[12] = 0.085858586;
		survey_values[13] = 0.095959596;
		survey_values[14] = 0.111111111;
		survey_values[15] = 0.126262626;
		survey_values[16] = 0.176767677;
		survey_values[17] = 0.232323232;
		survey_values[18] = 0.262626263;
		survey_values[19] = 0.262626263;
		return survey_values;
	}

	public static double calculateR2() throws IOException{
		double[] survey_values = get_survey_values();
		double[] model_values = new double[20];


		int fi = in_filename.contains(".") ? in_filename.lastIndexOf('.') : in_filename.length();
		String out_filename = "./output/" + time_simulation_start + "/" + in_filename.substring(0, fi) + "_output.csv";
		CSVReader reader = new CSVReader(new FileReader(out_filename));
		List<String[]> lines = reader.readAll();
		reader.close();

		try{
			File file = new File(out_filename);
			if(file.delete()){
				System.out.println(file.getName() + " is deleted!");
			}else{
				System.out.println("Delete operation is failed.");
			}
		}catch(Exception e){
			e.printStackTrace();
		}

		int m_ctr = 0;
		for(int i = 0; i < 20; i++){
			String[] row = lines.get(i*12+1);
			model_values[m_ctr] = Double.parseDouble(row[7]);
			m_ctr++;
		}

		SimpleRegression sr = new SimpleRegression();
		for (int k = 0; k < 20; k++){
			sr.addData(survey_values[k], model_values[k]);
		}
		double r2 = sr.getRSquare();

		String output_file_name = "./output/" + time_simulation_start + "/r2_output.csv";
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

			headers.add("R2");
			entries.add(String.valueOf(r2));

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

			String[] headerArray = headers.toArray(new String[0]);
			String[] entryArray = entries.toArray(new String[0]);

			if (!file.exists() || file.length() == 0){
				writer.writeNext(headerArray);
			}
			writer.writeNext(entryArray);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			writer.flush();
			writer.close();
		}
		return r2;
	}

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

	public static void writeGexf() throws IOException {
		String output_file_name = "./output/jaipur_gephi.gexf";

		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(output_file_name));
			// Write these lines to the file.
			// ... We call newLine to insert a newline character.
			writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			writer.newLine();
			writer.write("<gexf xmlns=\"http://www.gexf.net/1.2draft\" version=\"1.2\">");
			writer.newLine();
			writer.write("<graph mode=\"static\" defaultedgetype=\"directed\">");
			writer.newLine();

			writer.write("<attributes class=\"node\" type=\"static\">");
			writer.newLine();
			writer.write("<attribute id=\"1\" title=\"is_conserver\" type=\"bool\" />");
			writer.newLine();
			writer.write("</attributes>");
			writer.newLine();

			writer.write("<nodes>");
			writer.newLine();

			for (Household hh : Household.houseHoldAgents){
				writer.write("<node id=\"" + String.valueOf(hh.uuid) + "\">");
				writer.newLine();

				writer.write(String.format("<attvalue id=\"1\" value=\"%s\"/>", hh.isConserver));
				writer.newLine();

//				Color node_color = Color.red;
//				if (hh.isConserver){
//					node_color = Color.blue;
//				}
//				writer.write(String.format("<viz:color r=\"%s\" g=\"%s\" b=\"%s\" a=\"%s\"/>"
//						, node_color.getRed(), node_color.getGreen(), node_color.getBlue(), "0.6"));
//				writer.newLine();

				writer.write("</node>");
				writer.newLine();
			}

			writer.write("</nodes>");
			writer.newLine();
			writer.write("<edges>");
			writer.newLine();

			int ectr = 0;
			for (Household hh_source : Household.houseHoldAgents){
				for (UUID target_uuid : hh_source.relatedUuids){
					writer.write(String.format("<edge id=\"%s\" source=\"%s\" target=\"%s\" />",
							String.valueOf(ectr), String.valueOf(hh_source.uuid), String.valueOf(target_uuid)));
					writer.newLine();
					ectr++;
				}
			}
			writer.write("</edges>");
			writer.newLine();
			writer.write("</graph>");
			writer.newLine();
			writer.write("</gexf>");
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void writeJsonData(){
		JSONArray node_list = new JSONArray();
		JSONArray link_list = new JSONArray();

		for (Household hh : Household.houseHoldAgents){
			JSONObject hh_obj = new JSONObject();
			hh_obj.put("household_size", hh.householdSize);
			hh_obj.put("is_conserver", hh.isConserver);
			hh_obj.put("monthly_demand", hh.monthlyDemand);
			hh_obj.put("id", String.valueOf(hh.uuid));
			node_list.add(hh_obj);

			for (UUID uuid : hh.relatedUuids){
				JSONObject hh_link_obj = new JSONObject();
				hh_link_obj.put("source", String.valueOf(hh.uuid));
				hh_link_obj.put("target", String.valueOf(uuid));
				link_list.add(hh_link_obj);
			}
		}

		JSONObject total = new JSONObject();
		total.put("nodes", node_list);
		total.put("links", link_list);

		try {
			FileWriter file = new FileWriter("./output/data.json");
			file.write(total.toJSONString());
			file.flush();
			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}