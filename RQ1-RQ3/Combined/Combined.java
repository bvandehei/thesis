import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.lang.Math;


public class Combined {
   public static HashMap<String, Integer> BugAndFV;
   public static HashMap<String, Integer> BugAndCreation;
   public static HashMap<String, String> TrainBugVersionAndSZZ0;
   public static HashMap<String, String> TrainBugVersionAndSZZ25;
   public static HashMap<String, String> TrainBugVersionAndSZZ50;
   public static HashMap<String, String> TrainBugVersionAndSZZ75;
   public static HashMap<String, String> TrainBugVersionAndSZZ100;
   public static HashMap<String, String> TrainBugVersionAndActual;
   public static HashMap<String, String> TestBugVersionAndSZZ0;
   public static HashMap<String, String> TestBugVersionAndSZZ25;
   public static HashMap<String, String> TestBugVersionAndSZZ50;
   public static HashMap<String, String> TestBugVersionAndSZZ75;
   public static HashMap<String, String> TestBugVersionAndSZZ100;
   public static HashMap<String, String> TestBugVersionAndActual;

   public static void main(String[] args) throws IOException {
      String csvFile = "Projects.csv";
      String cvsSplitBy = ",";
      String linefromtrain;
      String trainMethods = "../RQ3-AllAVRetrievalMethods/TrainMethodsResults.csv";

      try (BufferedReader trBugs = new BufferedReader(new FileReader(trainMethods))) {
         TrainBugVersionAndSZZ0 = new HashMap<String, String>();
         TrainBugVersionAndSZZ25 = new HashMap<String, String>();
         TrainBugVersionAndSZZ50 = new HashMap<String, String>();
         TrainBugVersionAndSZZ75 = new HashMap<String, String>();
         TrainBugVersionAndSZZ100 = new HashMap<String, String>();
         TrainBugVersionAndActual = new HashMap<String, String>();

         trBugs.readLine();
         while ( (linefromtrain = trBugs.readLine()) != null) {
            String[] token = linefromtrain.split(cvsSplitBy);
            String key = token[0] + "|" + token[1];
            TrainBugVersionAndSZZ0.put(key, token[2]);
            TrainBugVersionAndSZZ25.put(key, token[3]);
            TrainBugVersionAndSZZ50.put(key, token[4]);
            TrainBugVersionAndSZZ75.put(key, token[5]);
            TrainBugVersionAndSZZ100.put(key, token[6]);
            TrainBugVersionAndActual.put(key, token[7]);
         }
      } catch (IOException e) {
         e.printStackTrace();
      }

      String linefromtest;
      String testMethods = "../RQ3-AllAVRetrievalMethods/MethodsResults.csv";
      try (BufferedReader tsBugs = new BufferedReader(new FileReader(testMethods))) {
         TestBugVersionAndSZZ0 = new HashMap<String, String>();
         TestBugVersionAndSZZ25 = new HashMap<String, String>();
         TestBugVersionAndSZZ50 = new HashMap<String, String>();
         TestBugVersionAndSZZ75 = new HashMap<String, String>();
         TestBugVersionAndSZZ100 = new HashMap<String, String>();
         TestBugVersionAndActual = new HashMap<String, String>();

         tsBugs.readLine();
         while ( (linefromtrain = tsBugs.readLine()) != null) {
            String[] token = linefromtrain.split(cvsSplitBy);
            String key = token[1] + "|" + token[4];
            TestBugVersionAndSZZ0.put(key, token[13]);
            TestBugVersionAndSZZ25.put(key, token[14]);
            TestBugVersionAndSZZ50.put(key, token[15]);
            TestBugVersionAndSZZ75.put(key, token[16]);
            TestBugVersionAndSZZ100.put(key, token[17]);
            TestBugVersionAndActual.put(key, token[18]);
         }
      } catch (IOException e) {
         e.printStackTrace();
      }

      try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
         String linefromcsv, linefrombug, linefromppam;

         while ( (linefromcsv = br.readLine()) != null) {
            String[] token = linefromcsv.split(cvsSplitBy);
            FileWriter fileWriterTrain = null;
            FileWriter fileWriterTest = null;
            try {
               String trainName = token[0] + "CombinedTrainSet.csv";
               String testName = token[0] + "CombinedTestSet.csv";
					     //Name of CSV for output
					     fileWriterTrain = new FileWriter(trainName);
					     fileWriterTest = new FileWriter(testName);
               fileWriterTrain.append("Bug ID,Version Name,Days From Version to FV,LOC touched from Version to FV,(i -FV)/(OV-FV),SZZ0,SZZ25,SZZ50,SZZ75,SZZ100,SZZB,Actual\n");
               fileWriterTest.append("Bug ID,Version Name,Days From Version to FV,LOC touched from Version to FV,(i -FV)/(OV-FV),SZZ0,SZZ25,SZZ50,SZZ75,SZZ100,SZZB,Actual\n");

               String bugfilename = "../RQ3-AllAVRetrievalMethods/CreateInputFiles/OrderedBugOutputFiles/"
                                    + token[0] + "BugInfoOrdered.csv";
               //Get Bug Info for each project
               try (BufferedReader brBugs = new BufferedReader(new FileReader(bugfilename))) {
                  BugAndCreation = new HashMap<String, Integer>();
                  BugAndFV = new HashMap<String, Integer>();
                  brBugs.readLine();
                  while ( (linefrombug = brBugs.readLine()) != null) {
                     String[] tokenBug = linefrombug.split(cvsSplitBy);
                     BugAndCreation.put(tokenBug[0], Integer.parseInt(tokenBug[4]));
                     BugAndFV.put(tokenBug[0], Integer.parseInt(tokenBug[5]));

                  }
               } catch (IOException e) {
                  e.printStackTrace();
               }

               String ppamTrain = "../PPAM/PPAMs/" + token[0] + "PPAMTrainSet.csv";
               try (BufferedReader ppamBugs = new BufferedReader(new FileReader(ppamTrain))) {
                  ppamBugs.readLine();
                  while ( (linefromppam = ppamBugs.readLine()) != null) {
                     String[] ppamtoken = linefromppam.split(cvsSplitBy);
                     String key = ppamtoken[0] + "|" + ppamtoken[2];
                     if (ppamtoken[2].equals("not released"))
                        key = ppamtoken[0] + "|null";
                     if (!TrainBugVersionAndActual.get(key).equals(ppamtoken[5])) {
                        System.out.println("Error actual dont match " + key);
                        System.exit(-1);
                     }
                     Integer vIndex = Integer.parseInt(ppamtoken[1]);
                     Double OVFV = 1.0;
                     if ((BugAndFV.get(ppamtoken[0]) - BugAndCreation.get(ppamtoken[0])) != 0)
                        OVFV = (double)(BugAndFV.get(ppamtoken[0]) - BugAndCreation.get(ppamtoken[0]));
                     Double proportion = ((double)(BugAndFV.get(ppamtoken[0]) - vIndex)) / OVFV;

                     //SZZ Cold Start Best will always be 0 since 5 projects best is 0, it will
                     // always be the most common for any combination of 9 projects
                     fileWriterTrain.append(ppamtoken[0]+","+ppamtoken[2]+","+ppamtoken[3]+","+ppamtoken[4]
                         +","+proportion.toString()+","+TrainBugVersionAndSZZ0.get(key)+","+
                         TrainBugVersionAndSZZ25.get(key)+","+TrainBugVersionAndSZZ50.get(key)+","+
                         TrainBugVersionAndSZZ75.get(key)+","+TrainBugVersionAndSZZ100.get(key)+","+
                         TrainBugVersionAndSZZ0.get(key)+","+ppamtoken[5]+"\n");

                  }
               } catch (IOException e) {
                  e.printStackTrace();
               }
               String ppamTest = "../PPAM/PPAMs/" + token[0] + "PPAMTestSet.csv";
               try (BufferedReader ppamBugs = new BufferedReader(new FileReader(ppamTest))) {
                  ppamBugs.readLine();
                  while ( (linefromppam = ppamBugs.readLine()) != null) {
                     String[] ppamtoken = linefromppam.split(cvsSplitBy);
                     String key = ppamtoken[0] + "|" + ppamtoken[2];
                     if (ppamtoken[2].equals("not released"))
                        key = ppamtoken[0] + "|null";
                     if (!TestBugVersionAndActual.get(key).equals(ppamtoken[5])) {
                        System.out.println("Error actual dont match " + key);
                        System.exit(-1);
                     }
                     Integer vIndex = Integer.parseInt(ppamtoken[1]);
                     Double OVFV = 1.0;
                     if ((BugAndFV.get(ppamtoken[0]) - BugAndCreation.get(ppamtoken[0])) != 0)
                        OVFV = (double)(BugAndFV.get(ppamtoken[0]) - BugAndCreation.get(ppamtoken[0]));
                     Double proportion = ((double)(BugAndFV.get(ppamtoken[0]) - vIndex)) / OVFV;
                     fileWriterTest.append(ppamtoken[0]+","+ppamtoken[2]+","+ppamtoken[3]+","+ppamtoken[4]
                         +","+proportion.toString()+","+TestBugVersionAndSZZ0.get(key)+","+
                         TestBugVersionAndSZZ25.get(key)+","+TestBugVersionAndSZZ50.get(key)+","+
                         TestBugVersionAndSZZ75.get(key)+","+TestBugVersionAndSZZ100.get(key)+","+
                         TestBugVersionAndSZZ0.get(key)+","+ppamtoken[5]+"\n");

                  }
               } catch (IOException e) {
                  e.printStackTrace();
               }
         } catch (Exception e) {
            System.out.println("Error in csv writer");
            e.printStackTrace();
         } finally {
            try {
               fileWriterTrain.flush();
               fileWriterTrain.close();
               fileWriterTest.flush();
               fileWriterTest.close();

            } catch (IOException e) {
               System.out.println("Error while flushing/closing fileWriter !!!");
               e.printStackTrace();
            }
         }
}
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
}
