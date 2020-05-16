import java.io.*;
import java.io.File;
import java.util.*;

/**
 * Read in various csv files
 */
public class scanInputCSV{


    /**
     * Read in population .csv file
     * @param dataSourceFile
     * @return aryay of population data
     */
	public static int[][] popScan(String dataSourceFile) {
		File fileName = checkAndCreateFile(dataSourceFile);
		Scanner lineCountScanner = createScanner(fileName);
		Scanner fileScanner = createScanner(fileName);
		//count the number of lines
		int numLines = getNumberLinesInFile(lineCountScanner);
		if (fileScanner.hasNextLine()){
			fileScanner.nextLine();
		} else{
			System.out.println("Population file cannot be found; check file path");
		}
		int[][] dataArray = new int[numLines-1][2]; // numLines -1 to skip title lines
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

	/**
	 * Take in file scanner and return number of lines in the file
	 * @param fileScanner
	 * @return the number of lines in file
	 */
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
     * Ensure file exists at specified filepath and either return error message or the desired File
     * @param dataSourceFile
     * @return desired file name
     */
	public static File checkAndCreateFile(String dataSourceFile) {
		File fileName = new File(dataSourceFile);
		if (!fileName.exists()) {
			System.out.println(dataSourceFile + " file not found");
			System.exit(1);
		}
		return fileName;
	}

    /**
     * Create a new file scanner and catch any associated errors
     * @param fileName
     * @return the newly created scanner
     */
	public static Scanner createScanner(File fileName){
		Scanner newScanner;
		try {
			newScanner = new Scanner(fileName);
		}
		catch (FileNotFoundException e) {
			System.out.println("No such file");
			newScanner = null;
			System.exit(1);
		}
		return newScanner;
	}
}

