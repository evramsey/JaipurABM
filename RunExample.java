package JaipurABM; /**
 * Created by lizramsey on 11/20/16.
 */


import java.io.*;
public class RunExample {
    public static void main(String[] args) throws IOException {
        long startTime = System.currentTimeMillis();
       // JaipurABM.run_jobs(double A, double B, double beta, double delta, double stdDevDelta, int n_jobs,
            //String population_file, String outputFileName, boolean deleteAutogen, int network (1 = original, 2 = ws small world)
        JaipurABM.run_jobs(0.97, 0.73, 0.12, 0.6, 0.03, 2, "/Users/lizramsey/Documents/workspace/JaipurABM/AgentPopulation.csv",
                "CalculatingTime", true, 1);
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        System.out.println("Time to run: " + totalTime);
        System.exit(0);
    }
}
