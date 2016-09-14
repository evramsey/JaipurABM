import java.io.*;
import java.io.File;
import java.util.*;

public class scanInputCSV{


	//you'll probably need to make separate methods for each file you read in. this one will be for population
	public static int[][] popScan(String dataSourceFile) {
		File fileName = new File(dataSourceFile);
		if (!fileName.exists()){
			System.out.println("ScanInputCSV file not found, better luck next time!");
			System.exit(1);
		}

		Scanner lineCountScanner;
		Scanner fileScanner;   
		try {
			lineCountScanner = new Scanner(fileName);
		} 
		catch (FileNotFoundException e) {
			System.out.println("No such file");
			lineCountScanner = null;
			System.exit(1);
		}
		try {
			fileScanner = new Scanner(fileName);
		} 
		catch (FileNotFoundException e) {
			System.out.println("no such file");
			fileScanner = null;
			System.exit(1);
		}

		//count the number of lines
		int numLines = getNumberLinesInFile(lineCountScanner);
		if (fileScanner.hasNextLine()){
			fileScanner.nextLine();
		} else{
			System.out.println("the princess is in another castle, and your file is probably with her");
		}
		int[][] dataArray = new int[numLines-1][2];// numLines -1 to skip title lines
		//read in data from csv, put into array
		int i = 0;
		while(fileScanner.hasNextLine()){
			String line = fileScanner.nextLine();
			Scanner lineScanner = new Scanner(line);
			lineScanner.useDelimiter(",");//for csv file
			while(lineScanner.hasNextInt()){
				int month = lineScanner.nextInt();
				int pop = lineScanner.nextInt();
				dataArray[i][0] = month;
				dataArray[i][1] = pop;
			}
			i++;
		}    
		return dataArray;
	}

	public static int getNumberLinesInFile(Scanner fileScanner){
		int count = 0;
		while(fileScanner.hasNextLine())
		{
			count++;
			fileScanner.nextLine();
		}
		return count;
	}


	/**
	 * Evaluates the existence of a file
	 * @param filename the name of the file being evaluated
	 * @return true, if successful
	 */
	public static boolean doesFileExist(String filename){
		File f = new File(filename);
		if(f.exists()) {
			return true;
		}
		return false;
	}

	//TODO: complete this guy to read in values from a csv file of initializing data
	public static void readInData(String dataSourceFile){
		File fileName = new File(dataSourceFile);
		if (!fileName.exists()){
			System.out.println("DataSourceFile file not found, better luck next time!");
			System.exit(1);
		}
		Scanner lineCountScanner;
		Scanner fileScanner;   
		try {
			lineCountScanner = new Scanner(fileName);
		} 
		catch (FileNotFoundException e) {
			System.out.println("No such file");
			lineCountScanner = null;
			System.exit(1);
		}
		try {
			fileScanner = new Scanner(fileName);
		} 
		catch (FileNotFoundException e) {
			System.out.println("no such file");
			fileScanner = null;
			System.exit(1);
		}
		//skip first three lines of input folder
		if (fileScanner.hasNextLine()){
			fileScanner.nextLine();
		}
		if (fileScanner.hasNextLine()){
			fileScanner.nextLine();
		}
		if (fileScanner.hasNextLine()){
			fileScanner.nextLine();
		}
		if (fileScanner.hasNextInt()){
			JaipurABM.jobs = fileScanner.nextInt();
			System.out.println("stringJobNumber: " + JaipurABM.jobs);
		}
		if (fileScanner.hasNextLine()){
			fileScanner.nextLine();
		}
		if (fileScanner.hasNextLine()){
			fileScanner.nextLine();
		}
		if (fileScanner.hasNextInt()){
			//int numSteps = fileScanner.nextInt();
			JaipurABM.numStepsInMain = fileScanner.nextInt();
			System.out.println("numStepsMain: " + JaipurABM.numStepsInMain);
		}
		if (fileScanner.hasNextLine()){
			fileScanner.nextLine();
		}
		if (fileScanner.hasNextLine()){
			fileScanner.nextLine();
		}
		if (fileScanner.hasNextDouble()){
			JaipurABM.averageHouseholdSize = fileScanner.nextDouble();
			System.out.println("avgHouseholdSize: " + JaipurABM.averageHouseholdSize);
		}
		if (fileScanner.hasNextLine()){
			fileScanner.nextLine();
		}
		if (fileScanner.hasNextLine()){
			fileScanner.nextLine();
		}
		if (fileScanner.hasNextLine()){
			fileScanner.nextLine();
		}
		if (fileScanner.hasNextLine()){
			JaipurABM.graphStructure = fileScanner.nextLine();
			System.out.println(JaipurABM.graphStructure);
		}
		if (fileScanner.hasNextLine()){
			fileScanner.nextLine();
		}
		if (fileScanner.hasNextLine()){
			fileScanner.nextLine();
		}
		if(fileScanner.hasNextLine()){
			JaipurABM.outputFileName = fileScanner.nextLine();
		}
		
		if (fileScanner.hasNextLine()){
			fileScanner.nextLine();
		}
		if (fileScanner.hasNextLine()){
			fileScanner.nextLine();
		}
		if (fileScanner.hasNextLine()){
			fileScanner.nextLine();
		}
		if (fileScanner.hasNextLine()){
			fileScanner.nextLine();
		}
		if (fileScanner.hasNextDouble()){
			Household.percentConservers = fileScanner.nextDouble();
		}
		if (fileScanner.hasNextLine()){
			fileScanner.nextLine();
		}
		if (fileScanner.hasNextLine()){
			fileScanner.nextLine();
		}
		if (fileScanner.hasNextLine()){
			fileScanner.nextLine();
		}
		if (fileScanner.hasNextLine()){
			fileScanner.nextLine();
		}
		if (fileScanner.hasNextLine()){
			fileScanner.nextLine();
		}
		if (fileScanner.hasNextDouble()){
			double aAndBPrime = fileScanner.nextDouble();
			UtilityFunction.a = aAndBPrime;
			UtilityFunction.bPrime = aAndBPrime;
			System.out.println("a and b prime: " + UtilityFunction.a + " " + UtilityFunction.bPrime);
		}
		if (fileScanner.hasNextLine()){
			String test = fileScanner.nextLine();
		}
		if (fileScanner.hasNextLine()){
			String test = fileScanner.nextLine();
		}
		if (fileScanner.hasNextDouble()){
			double bAndAPrime = fileScanner.nextDouble();
			UtilityFunction.b = bAndAPrime;
			UtilityFunction.aPrime = bAndAPrime;
			System.out.println("b and a prime: " + UtilityFunction.b + " " + UtilityFunction.aPrime);
		}
		if (fileScanner.hasNextLine()){
			fileScanner.nextLine();
		}		
		if (fileScanner.hasNextLine()){
			fileScanner.nextLine();
		}
		if (fileScanner.hasNextDouble()){
			UtilityFunction.exogenousTerm = fileScanner.nextDouble();
			System.out.println("exog term found");
		}
		if (fileScanner.hasNextLine()){
			fileScanner.nextLine();
		}
		if (fileScanner.hasNextLine()){
			fileScanner.nextLine();
		}
		if (fileScanner.hasNextDouble()){
			ProbabilityOfBehavior.beta = fileScanner.nextDouble();
			System.out.println("beta: " + ProbabilityOfBehavior.beta);
		}
		if (fileScanner.hasNextLine()){
			fileScanner.nextLine();
		}
		if (fileScanner.hasNextLine()){
			fileScanner.nextLine();
		}
		if (fileScanner.hasNextDouble()){
			UtilityFunction.parameterDelta = fileScanner.nextDouble();
			System.out.println("parameter delta: " + UtilityFunction.parameterDelta);
		}
		if (fileScanner.hasNextLine()){
			fileScanner.nextLine();
		}
		if (fileScanner.hasNextLine()){
			fileScanner.nextLine();
		}

		if (fileScanner.hasNextInt()){
			JaipurABM.numStepsSkippedToUpdateUtilityFunctions = fileScanner.nextInt();

		}
		if (fileScanner.hasNextLine()){
			fileScanner.nextLine();
		}
		if (fileScanner.hasNextLine()){
			fileScanner.nextLine();
		}
		if (fileScanner.hasNextInt()){
			System.out.println("talk function skip updated");
			JaipurABM.numStepsSkippedToUpdateTalkFunction = fileScanner.nextInt();
		}
	}
}

