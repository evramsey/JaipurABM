package JaipurABM;

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


}

