package JaipurABM; /**
 * Created by lizramsey on 11/20/16.
 */


import java.io.*;
public class RunExample {
    public static void main(String[] args) throws IOException {
       // JaipurABM.run_jobs(double A, double B, double beta, double delta, int n_jobs, String population_file);
        JaipurABM.run_jobs(0.73, 0.97, 0.12, 0.6, 0.03, 3, "/Users/lizramsey/Documents/workspace/JaipurABM/PopTest.csv", "TestingR2outputs06192017");
        System.exit(0);
    }
}
