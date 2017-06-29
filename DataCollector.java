package JaipurABM;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import org.apache.commons.math3.stat.regression.SimpleRegression;
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
	public static String outputFileIdentifier;

	public static int numAgentsThisTimeStep = 0;
	public static int numR2calcs = 1;

	public static String out_filename;

	public static List<String> input_parameters;

	public static double CumulativeDemand;
	public int percentConservers;
	public static String txtFileInput;
	public static int numAgents;
	public static int population;
	public static int modelPopulation;
	public static int numConservers;
	private static boolean deleteAutogenTxt;

	/**
	 * Constructor
	 */
	public DataCollector(){}

	public void step(SimState state) {
		int i = in_filename.contains(".") ? in_filename.lastIndexOf('.') : in_filename.length();
		String output_file_name = "./output/" + outputFileIdentifier + "/" + in_filename.substring(0, i) + "_output.csv";
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
			entries.add(outputFileIdentifier);
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
			entries.add(String.valueOf(ProbabilityOfBehavior.getBeta()));
			headers.add("Delta");
			entries.add(String.valueOf(UtilityFunction.parameterDelta));

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
		//generateAgentOutput("AgentOutputs_GAParameterSet10.csv", state);
        numAgents = 0;
		CumulativeDemand = 0.0;
		modelPopulation = 0;
		numConservers = 0;
	}

	public double getConserverRatioThisTimeStep(){
		double numConserversDub = (double)(numConservers);
		double numAgentsDub = (double)(numAgents);
		double ratio = numConserversDub/numAgentsDub;
		return ratio;
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

	public static double calculateR2(int jobNum) throws IOException{
		double[] survey_values = get_survey_values();
		double[] model_values = new double[20];
		int fi = in_filename.contains(".") ? in_filename.lastIndexOf('.') : in_filename.length();
		String out_filename = "./output/" + outputFileIdentifier + "/" + in_filename.substring(0, fi) + "_output.csv";
		CSVReader reader = new CSVReader(new FileReader(out_filename));
		List<String[]> lines = reader.readAll();
		reader.close();

		int m_ctr = 0;
		if(deleteAutogenTxt){
			jobNum = 0;
		}

		for(int i = jobNum * 20; i < (20*(jobNum + 1)); i++){
			String[] row = lines.get(i*12+1);
			model_values[m_ctr] = Double.parseDouble(row[7]);
			m_ctr++;
		}

		SimpleRegression sr = new SimpleRegression();
		for (int k = 0; k < 20; k++){
			sr.addData(survey_values[k], model_values[k]);
		}
		double r2 = sr.getRSquare();

		String output_file_name = "./output/" + outputFileIdentifier + "/r2_output.csv";
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
			entries.add(outputFileIdentifier);
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
			entries.add(String.valueOf(ProbabilityOfBehavior.getBeta()));
			headers.add("Delta");
			entries.add(String.valueOf(UtilityFunction.parameterDelta));

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
		numR2calcs++;
		return r2;
	}

	public static void setDeleteAutogenTxt(boolean delTxt){
		deleteAutogenTxt = delTxt;
	}

//	public static void aggregateResults() throws IOException{
//		File[] files = new File("./output/" + outputFileIdentifier).listFiles();
//
//		List<String> mergedLines = new ArrayList<> ();
//		for (File f : files){
//			Path p = Paths.get(f.getPath());
//			List<String> lines = Files.readAllLines(p, Charset.forName("UTF-8"));
//			if (!lines.isEmpty()) {
//				if (mergedLines.isEmpty()) {
//					mergedLines.add(lines.get(0)); //add header only once
//				}
//				mergedLines.addAll(lines.subList(1, lines.size()));
//			}
//		}
//
//		Path target = Paths.get("./output/" + outputFileIdentifier
//				+ "/" + outputFileIdentifier + "_aggregate.csv");
//		Files.write(target, mergedLines, Charset.forName("UTF-8"));
//	}
//
//	public static void writeGexf() throws IOException {
//		String output_file_name = "./output/jaipur_gephi.gexf";
//
//		try {
//			BufferedWriter writer = new BufferedWriter(new FileWriter(output_file_name));
//			// Write these lines to the file.
//			// ... We call newLine to insert a newline character.
//			writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
//			writer.newLine();
//			writer.write("<gexf xmlns=\"http://www.gexf.net/1.2draft\" version=\"1.2\">");
//			writer.newLine();
//			writer.write("<graph mode=\"static\" defaultedgetype=\"directed\">");
//			writer.newLine();
//
//			writer.write("<attributes class=\"node\" type=\"static\">");
//			writer.newLine();
//			writer.write("<attribute id=\"1\" title=\"is_conserver\" type=\"bool\" />");
//			writer.newLine();
//			writer.write("</attributes>");
//			writer.newLine();
//
//			writer.write("<nodes>");
//			writer.newLine();
//
//			for (Household hh : Household.houseHoldAgents){
//				writer.write("<node id=\"" + String.valueOf(hh.uuid) + "\">");
//				writer.newLine();
//
//				writer.write(String.format("<attvalue id=\"1\" value=\"%s\"/>", hh.isConserver));
//				writer.newLine();
//
////				Color node_color = Color.red;
////				if (hh.isConserver){
////					node_color = Color.blue;
////				}
////				writer.write(String.format("<viz:color r=\"%s\" g=\"%s\" b=\"%s\" a=\"%s\"/>"
////						, node_color.getRed(), node_color.getGreen(), node_color.getBlue(), "0.6"));
////				writer.newLine();
//
//				writer.write("</node>");
//				writer.newLine();
//			}
//
//			writer.write("</nodes>");
//			writer.newLine();
//			writer.write("<edges>");
//			writer.newLine();
//
//			int ectr = 0;
//			for (Household hh_source : Household.houseHoldAgents){
//				for (UUID target_uuid : hh_source.relatedUuids){
//					writer.write(String.format("<edge id=\"%s\" source=\"%s\" target=\"%s\" />",
//							String.valueOf(ectr), String.valueOf(hh_source.uuid), String.valueOf(target_uuid)));
//					writer.newLine();
//					ectr++;
//				}
//			}
//			writer.write("</edges>");
//			writer.newLine();
//			writer.write("</graph>");
//			writer.newLine();
//			writer.write("</gexf>");
//			writer.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//
//	public static void writeJsonData(){
//		JSONArray node_list = new JSONArray();
//		JSONArray link_list = new JSONArray();
//
//		for (Household hh : Household.houseHoldAgents){
//			JSONObject hh_obj = new JSONObject();
//			hh_obj.put("household_size", hh.householdSize);
//			hh_obj.put("is_conserver", hh.isConserver);
//			hh_obj.put("monthly_demand", hh.monthlyDemand);
//			hh_obj.put("id", String.valueOf(hh.uuid));
//			node_list.add(hh_obj);
//
//			for (UUID uuid : hh.relatedUuids){
//				JSONObject hh_link_obj = new JSONObject();
//				hh_link_obj.put("source", String.valueOf(hh.uuid));
//				hh_link_obj.put("target", String.valueOf(uuid));
//				link_list.add(hh_link_obj);
//			}
//		}
//
//		JSONObject total = new JSONObject();
//		total.put("nodes", node_list);
//		total.put("links", link_list);
//
//		try {
//			FileWriter file = new FileWriter("./output/data.json");
//			file.write(total.toJSONString());
//			file.flush();
//			file.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//
//	public static void generateAgentOutput(String output_file_name, SimState state){
//		numAgentsThisTimeStep = numAgentsThisTimeStep + JaipurABM.getNumNewAgents(state.schedule.getTime());
//		CSVWriter writer = null;
//		try {
//			final File file = new File(output_file_name);
//			final File parent_directory = file.getParentFile();
//			if (null != parent_directory) {
//				parent_directory.mkdirs();
//			}
//
//			writer = new CSVWriter(new FileWriter(output_file_name, true));
//			//ArrayList<String> entries = new ArrayList<>();
//			ArrayList<String> headers = new ArrayList<>();
//
//			headers.add("Current Time Step");
//			headers.add("Agent Name");
//			headers.add("Conservation Status");
//			headers.add("Time Step Born");
//			headers.add("Agent Age");
//			headers.add("NC to WC Conversion");
//			headers.add("WC to NC Conversion");
//			headers.add("Family Size");
//			headers.add("Family Ratio Cons:Network");
//			headers.add("Family Delta");
//			headers.add("Friend Size");
//			headers.add("Friend Ratio Cons:Network");
//			headers.add("Friend Delta");
//			headers.add("Acquaintance Size");
//			headers.add("Acquaintance Ratio Cons:Network");
//
//			String[] headerArray = headers.toArray(new String[0]);
//			if (!file.exists() || file.length() == 0) {
//				writer.writeNext(headerArray);
//			}
//			ArrayList<String[]> networkArrayList = new ArrayList<String[]>();
//			int i = 0;
//			for (Household hh : JaipurABM.network) {
//				if (i < numAgentsThisTimeStep) {
//					String[] hhEntry = new String[15];
//					hhEntry[0] = "" + state.schedule.getTime();
//					hhEntry[1] = hh.getVertexName();
//					hhEntry[2] = String.valueOf(hh.isAgentConserver());
//					hhEntry[3] = String.valueOf(hh.getTimeStepBorn());
//					hhEntry[4] = String.valueOf(hh.getAgentAge(state));
//					hhEntry[5] = String.valueOf(hh.has_converted_NC_to_WC_thistimestep());
//					hhEntry[6] = String.valueOf(hh.has_converted_WC_to_NC_thistimestep());
//					hhEntry[7] = String.valueOf(hh.getFamilySize());
//					hhEntry[8] = String.valueOf(hh.getRatioFamCons());
//					hhEntry[9] = String.valueOf(hh.getFamDelta());
//					hhEntry[10] = String.valueOf(hh.getFriendsSize());
//					hhEntry[11] = String.valueOf(hh.getRatioFriendsCons());
//					hhEntry[12] = String.valueOf(hh.getFriendDelta());
//					hhEntry[13] = String.valueOf(hh.getAcqSize());
//					hhEntry[14] = String.valueOf(hh.getRatioAcqCons());
//
//					networkArrayList.add(hhEntry);
//					i++;
//				}
//				else{
//					break;
//				}
//			}
//			writer.writeAll(networkArrayList);
//
//		}catch (IOException e) {
//			e.printStackTrace();
//		}
//
//		try {
//			writer.flush();
//			writer.close();
//		} catch (IOException e) {
//			writer = null;
//		}
//	}
}