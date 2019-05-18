import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.lang.Math;

public class RQ3SimpleAndLAV {

   public static HashMap<Integer, String> BugAndKey;
   public static HashMap<Integer, Integer> BugAndAV;
   public static HashMap<Integer, Integer> BugAndCreation;
   public static HashMap<Integer, Integer> BugAndFix;
   public static HashMap<Integer, String> releaseNames;
   public static HashMap<Integer, String> releaseID;


   public static void main(String[] args) throws IOException {
      String csvFile = "CreateInputFiles/Projects.csv";
      String cvsSplitBy = ",";

      try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
         String linefromcsv, linefromversion, linefrombug;
         Integer totalBugs = 0, totalVersions = 0;
         FileWriter fileWriter = null;
         FileWriter fileWriter2 = null;
         try {
            //Name of CSV for output
				    fileWriter = new FileWriter("SimpleAndLAVResults.csv");
				    fileWriter2 = new FileWriter("SimpleAndLAVStatistics.csv");
            //Header for CSV
            fileWriter.append("Project Key,Bug ID,Bug Order,Version ID,Version Name,Simple,LAV,Actual Bugginess");
            fileWriter.append("\n");
            fileWriter2.append("Project Key,Method,Precision,Recall,F1,MCC,Kappa");
            fileWriter2.append("\n");
            while ( (linefromcsv = br.readLine()) != null) {
               String[] token = linefromcsv.split(cvsSplitBy);
               String versionfilename = "CreateInputFiles/VersionOutputFiles/"+ token[0] + "VersionInfo.csv";
               //Get Versions info for each project
               try (BufferedReader brVersions = new BufferedReader(new FileReader(versionfilename))) {
                 releaseNames = new HashMap<Integer, String>();
                 releaseID = new HashMap<Integer, String>();
                 brVersions.readLine();
                 while ( (linefromversion = brVersions.readLine()) != null) {
                    String[] tokenVersion = linefromversion.split(cvsSplitBy);
                    releaseID.put(Integer.parseInt(tokenVersion[0]), tokenVersion[1]);
                    releaseNames.put(Integer.parseInt(tokenVersion[0]), tokenVersion[2]);
                 }
                 totalVersions = releaseID.size();
               } catch (IOException e) {
                 e.printStackTrace();
               }

               String bugfilename = "CreateInputFiles/OrderedBugOutputFiles/"+ token[0] + "BugInfoOrdered.csv";
               //Get Bug Info for each project
               try (BufferedReader brBugs = new BufferedReader(new FileReader(bugfilename))) {
                  Integer bugOrder = 1;
                  BugAndKey = new HashMap<Integer, String>();
                  BugAndAV = new HashMap<Integer, Integer>();
                  BugAndCreation = new HashMap<Integer, Integer>();
                  BugAndFix = new HashMap<Integer, Integer>();
                  brBugs.readLine();
                  while ( (linefrombug = brBugs.readLine()) != null) {
                     String[] tokenBug = linefrombug.split(cvsSplitBy);
                     BugAndKey.put(bugOrder, tokenBug[0]);
                     BugAndAV.put(bugOrder, Integer.parseInt(tokenBug[3]));
                     BugAndCreation.put(bugOrder, Integer.parseInt(tokenBug[4]));
                     BugAndFix.put(bugOrder, Integer.parseInt(tokenBug[5]));
                     bugOrder++;
                  }
                  totalBugs = BugAndKey.size();
               } catch (IOException e) {
                  e.printStackTrace();
               }

               Integer i = 1, j = 1;
               Double Psum = 0.0, Ptotal = 0.0;
               Long sTP = 0L, sTN = 0L, sFP = 0L, sFN = 0L;
               Long lavTP = 0L, lavTN = 0L, lavFP = 0L, lavFN = 0L;

               for ( i = 1; i < (totalBugs / 2); i++) {
                  Double TV2FV = 1.0;
                  if ( BugAndFix.get(i) - BugAndCreation.get(i) != 0)
                     TV2FV = (double)(BugAndFix.get(i) - BugAndCreation.get(i));
                  Psum += ((double)(BugAndFix.get(i) - BugAndAV.get(i)) / TV2FV);
                  Ptotal = (double)i;
               }
               for ( i = (totalBugs / 2); i <= totalBugs; i++) {
                  for (j = 1 ; j <= BugAndFix.get(i); j++) {
                     String simple, LAVbuggy, actual;
                     if (j < BugAndAV.get(i) || j >= BugAndFix.get(i))
                        actual = "No";
                     else
                        actual = "Yes";
                     if (j >= BugAndCreation.get(i) && j < BugAndFix.get(i))
                        simple = "Yes";
                     else
                        simple = "No";
                     Double P = Psum/Ptotal;
                     Double LAV = (double)BugAndFix.get(i) - P;
                     if (BugAndFix.get(i) - BugAndCreation.get(i) != 0)
                        LAV = (double)BugAndFix.get(i) - (P * (double)(BugAndFix.get(i) - BugAndCreation.get(i)));
                     if ((j >= LAV || j >= BugAndCreation.get(i)) && j < BugAndFix.get(i))
                        LAVbuggy = "Yes";
                     else
                        LAVbuggy = "No";
                     fileWriter.append(token[0] + "," + BugAndKey.get(i) + "," +
                       i.toString() + "," + releaseID.get(j) + "," + releaseNames.get(j)
                       + "," + simple + "," + LAVbuggy + "," + actual + "\n");

                     if (actual.equals("Yes")) {
                        if (simple.equals("Yes"))
                           sTP++;
                        else
                           sFN++;
                        if (LAVbuggy.equals("Yes"))
                           lavTP++;
                        else
                           lavFN++;
                     }
                     else {
                        if (simple.equals("Yes"))
                           sFP++;
                        else
                           sTN++;
                        if (LAVbuggy.equals("Yes"))
                           lavFP++;
                        else
                           lavTN++;
                     }
                  }
               }

               Double precision = (double)sTP /(double)(sTP + sFP);
               Double recall = (double)sTP /(double)(sTP + sFN);
               Double F1 = (double)(precision * recall) / (double)(precision + recall);
               Double MCC = (double)(sTP * sTN - sFP * sFN) / (Math.sqrt((sTP + sFP) * (sFN + sTN) * (sFP + sTN) * (sTP + sFN)));
               Double observed = (double)(sTP + sTN) / (double)(sTP+ sFN + sFP + sTN);
               Double a = (double)((sTP + sFN) * (sTP + sFP)) / (double)(sTP+ sFN + sFP + sTN);
               Double b = (double)((sFP + sTN) * (sFN + sTN)) / (double)(sTP+ sFN + sFP + sTN);
               Double expected = (a + b) / (double)(sTP+ sFN + sFP + sTN);
               Double kappa = (observed - expected)/(1 - expected);
               fileWriter2.append(token[0] +  "," + "Simple" + "," + precision.toString() + "," + recall.toString()
                  + "," + F1.toString() +"," + MCC.toString() + "," + kappa.toString() + "\n");


               precision = (double)lavTP /(double)(lavTP + lavFP);
               recall = (double)lavTP /(double)(lavTP + lavFN);
               F1 = (precision * recall) / (precision + recall);
               MCC = (double)((lavTP * lavTN) - (lavFP * lavFN)) / (Math.sqrt((double)((lavTP + lavFP)*(lavTP + lavFN)*(lavTN + lavFP)*(lavTN+lavFN))));
               observed = (double)(lavTP + lavTN) / (double)(lavTP+ lavFN + lavFP + lavTN);
               a = (double)((lavTP + lavFN) * (lavTP + lavFP)) / (double)(lavTP+ lavFN + lavFP + lavTN);
               b = (double)((lavFP + lavTN) * (lavFN + lavTN)) / (double)(lavTP+ lavFN + lavFP + lavTN);
               expected = (a + b) / (double)(lavTP+ lavFN + lavFP + lavTN);
               kappa = (observed - expected)/(1 - expected);
               fileWriter2.append(token[0] +  "," + "LAV" + "," + precision.toString() + "," + recall.toString()
                  + "," + F1.toString() +"," + MCC.toString() + "," + kappa.toString() + "\n");

            }
         } catch (Exception e) {
            System.out.println("Error in csv writer");
            e.printStackTrace();
         } finally {
            try {
               fileWriter.flush();
               fileWriter.close();
               fileWriter2.flush();
               fileWriter2.close();
            } catch (IOException e) {
               System.out.println("Error while flushing/closing fileWriter !!!");
               e.printStackTrace();
            }
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
}
