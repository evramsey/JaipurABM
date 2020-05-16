/**
 *  Run Agent-Based Model from an external function, allowing control of parameters
 */

import java.io.*;
public class ModelRunner {
    public static void main(String[] args) throws IOException {
        long startTime = System.currentTimeMillis();
        //dftABM.runJobs(avgConnections, double A, double B, double beta, double exogTerm, double exogTermDrought
        //int numSkippedTalkSteps, int numSKippedUtilUpdateSteps, int n_jobs,
        //String population_file, String outputFileName, boolean deleteAutogen,
        //int networkStructure)
        dftABM.runJobs(48, 0.971, 0.402, 100, 0.681, 0.170, 2, 176, 30,
                "/Users/lizramsey/Documents/workspace/dftABM/src/AgentPopulation_1997_2015.csv",
                "/B2/"+startTime, false, 3);
        long endTime = System.currentTimeMillis();
        long totalTime =(endTime - startTime)/1000;
        System.out.println("Time to run: " + totalTime + " seconds");
        System.exit(0);
    }
}
