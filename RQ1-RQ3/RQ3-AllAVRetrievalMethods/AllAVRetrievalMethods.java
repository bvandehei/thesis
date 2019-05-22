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

import org.json.*;

public class AllAVRetrievalMethods {
   public static HashMap<String, Double> ProjectAndP;
   public static HashMap<String, String> BugAndFixHash;
   public static HashMap<String, String> FixHashAndBIC;
   public static HashMap<String, LocalDateTime> FixHashAndBICDate;
   public static HashMap<String, ArrayList<LocalDateTime>> FixHashAndBICSDates;
   public static HashMap<String, Integer> FixHashAndBICVersion;
   public static HashMap<String, ArrayList<Integer>> FixHashAndBICSVersions;
   public static HashMap<Integer, String> BugAndKey;
   public static HashMap<Integer, Integer> BugAndAV;
   public static HashMap<Integer, Integer> BugAndCreation;
   public static HashMap<Integer, Integer> BugAndFix;
   public static HashMap<Integer, String> releaseNames;
   public static HashMap<Integer, String> releaseID;
   public static ArrayList<LocalDateTime> releases;

  //Adds a release to the arraylist
   public static void addRelease(String strDate) {
      LocalDateTime dateTime = LocalDateTime.parse(strDate);
      releases.add(dateTime);
      return;
   }

   // Finds the release corresponding to a creation or resolution date
   public static Integer findRelease(LocalDateTime dateTime) {
      int i;
      int index = 0;
      for (i = 0; i < releases.size(); i++ ) {
         // if date is after this release, the set to next index
         if (dateTime.isAfter(releases.get(i)))
            index = i + 1;
      }
      //acount for 0 offset
      return index + 1;
   }

   public static void main(String[] args) throws IOException {
      String csvFile = "CreateInputFilesForSZZ/Projects.csv";
      String cvsSplitBy = ",";

     try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
         ProjectAndP = new HashMap<String, Double>();
         String linefromcsv;
         while ( (linefromcsv = br.readLine()) != null) {
               Integer totalBugs= 0;
               String[] token = linefromcsv.split(cvsSplitBy);
               String bugfilename = "CreateInputFiles/OrderedBugOutputFiles/"+ token[0] + "BugInfoOrdered.csv";
               //Get Bug Info for each project
               try (BufferedReader brBugs = new BufferedReader(new FileReader(bugfilename))) {
                  String linefrombug;
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
               Double Psum = 0.0;
               Double Ptotal = 0.0;
               for ( int i = 1; i <= (totalBugs/ 3); i++) {
                  Double TV2FV = 1.0;
                  if ( BugAndFix.get(i) - BugAndCreation.get(i) != 0)
                     TV2FV = (double)(BugAndFix.get(i) - BugAndCreation.get(i));
                  Psum += ((double)(BugAndFix.get(i) - BugAndAV.get(i)) / TV2FV);
                  Ptotal = (double)i;
               }
               ProjectAndP.put(token[0], Psum/Ptotal);
         }
     } catch (IOException e) {
         e.printStackTrace();
     }

     try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
         String linefromcsv, linefromversion, linefrombug, linefromhash;
         Integer totalBugs = 0, totalVersions = 0;
         FileWriter fileWriter = null;
         FileWriter fileWriter2 = null;
         FileWriter fileWriterTrain = null;
         FileWriter fileWriterTest = null;
         FileWriter fileWriterTrainResults = null;
         FileWriter fileWriterSZZBest = null;
         try {
            //Name of CSV for output
				    fileWriter = new FileWriter("MethodsResults.csv");
				    fileWriter2 = new FileWriter("MethodsStatistics.csv");
				    fileWriterTrain = new FileWriter("TrainBugs.csv");
				    fileWriterTest = new FileWriter("TestBugs.csv");
				    fileWriterTrainResults = new FileWriter("TrainMethodsResults.csv");
				    fileWriterSZZBest = new FileWriter("SZZBest.csv");
            //Header for CSV
            fileWriter.append("Project Key,Bug ID,Bug Order,Version ID,Version Name," +
                              "Simple,Proportion0.5,Proportion1,Proportion2,Merge,Cold Start Proportion," +
                              "SZZu,SZZuB,SZZu0,SZZu25,SZZu50,SZZu75,SZZu100,Actual Bugginess");
            fileWriter.append("\n");
            fileWriter2.append("Project Key,Method,Precision,Recall,F1,MCC,Kappa");
            fileWriter2.append("\n");
            fileWriterTrainResults.append("Bug ID,Version Name,SZZu0,SZZu25,SZZu50,SZZu75,SZZu100,Actual\n");
            fileWriterSZZBest.append("Project,Percent of Best SZZ\n");

            while ( (linefromcsv = br.readLine()) != null) {
               String[] token = linefromcsv.split(cvsSplitBy);

               System.out.println(token[0]);

               //First get Bug id and fix commit Hash
               String hashfilename = "CreateInputFilesForSZZ/CSVFiles/"+ token[0] + "BugInfo.csv";
               try (BufferedReader brHash = new BufferedReader(new FileReader(hashfilename))) {
                 BugAndFixHash = new HashMap<String, String>();
                 brHash.readLine();
                 while ( (linefromhash = brHash.readLine()) != null) {
                    String[] tokenHash = linefromhash.split(cvsSplitBy);
                    BugAndFixHash.put(tokenHash[0], tokenHash[4]);
                 }
               } catch (IOException e) {
                 e.printStackTrace();
               }

               //Next get fix commit Hash and oldest hash introducer
               FixHashAndBIC = new HashMap<String, String>();
               FixHashAndBICDate = new HashMap<String, LocalDateTime>();
               FixHashAndBICSDates = new HashMap<String, ArrayList<LocalDateTime>>();
               for (int h = 1; h < token.length; h++) {
                  String[] token2 = token[h].split("/");
                  String path =  token2[token2.length - 1].substring(0, token2[token2.length - 1].length() - 4);
                  hashfilename = "FixAndIntroducingCommitsFiles/"+ path + "_fix_and_introducers_pairs_1.json";

                  try (BufferedReader brHash = new BufferedReader(new FileReader(hashfilename))) {
                     StringBuilder sb = new StringBuilder();
                     String result = "";
                     String line = brHash.readLine();
                     while (line != null) {
                       sb.append(line);
                       line = brHash.readLine();
                     }
                     result = sb.toString();
                     JSONArray jarr = new JSONArray(result);
                     for ( int j = 0; j < jarr.length(); j++) {
                        JSONArray pair = jarr.getJSONArray(j);
                        String fix = pair.getString(0);
                        String bic = pair.getString(1);
                        String command = "git --git-dir ./CreateInputFilesForSZZ/GitClones/" + path + "/.git " +
                                 "show -s --format=%cd --date=iso-strict " + bic;
                        Process proc = Runtime.getRuntime().exec(command);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                        String date;
                        if((date = reader.readLine()) != null) {
                           String datePart = date.substring(0, 19);
                           LocalDateTime datetime = LocalDateTime.parse(datePart);
                           if ( FixHashAndBICSDates.get(fix) == null) {
                              FixHashAndBICSDates.put(fix, new ArrayList<LocalDateTime>());
                              FixHashAndBICSDates.get(fix).add(datetime);
                           }
                           else
                              FixHashAndBICSDates.get(fix).add(datetime);
                           if ( FixHashAndBIC.get(fix) == null || datetime.isBefore(FixHashAndBICDate.get(fix))) {
                              FixHashAndBIC.put(fix, bic);
                              FixHashAndBICDate.put(fix, datetime);
                           }
                        }
                       proc.waitFor();
                     }
                  } catch (IOException e) {
                    e.printStackTrace();
                  }
               }

               String versionfilename = "CreateInputFiles/VersionOutputFiles/"+ token[0] + "VersionInfo.csv";
               //Get Versions info for each project
               try (BufferedReader brVersions = new BufferedReader(new FileReader(versionfilename))) {
                 releaseNames = new HashMap<Integer, String>();
                 releaseID = new HashMap<Integer, String>();
                 releases = new ArrayList<LocalDateTime>();
                 brVersions.readLine();
                 while ( (linefromversion = brVersions.readLine()) != null) {
                    String[] tokenVersion = linefromversion.split(cvsSplitBy);
                    releaseID.put(Integer.parseInt(tokenVersion[0]), tokenVersion[1]);
                    releaseNames.put(Integer.parseInt(tokenVersion[0]), tokenVersion[2]);
                    addRelease(tokenVersion[3]);
                 }
                 // order releases by date
                 Collections.sort(releases, new Comparator<LocalDateTime>(){
                    @Override
                    public int compare(LocalDateTime o1, LocalDateTime o2) {
                      return o1.compareTo(o2);
                    }
                 });
                 totalVersions = releaseID.size();
               } catch (IOException e) {
                 e.printStackTrace();
               }

               //Now get version associated with BIC
               ArrayList<String> fixKeys = new ArrayList<String>(FixHashAndBICDate.keySet());
               FixHashAndBICVersion = new HashMap<String, Integer>();
               FixHashAndBICSVersions = new HashMap<String, ArrayList<Integer>>();
               for ( int v = 0; v < fixKeys.size(); v++) {
                  FixHashAndBICVersion.put(fixKeys.get(v), findRelease(FixHashAndBICDate.get(fixKeys.get(v))));
                  FixHashAndBICSVersions.put(fixKeys.get(v), new ArrayList<Integer>());
                  for ( int r = 0; r < FixHashAndBICSDates.get(fixKeys.get(v)).size(); r++) {
                      FixHashAndBICSVersions.get(fixKeys.get(v)).add(findRelease(FixHashAndBICSDates.get(fixKeys.get(v)).get(r)));
                  }
                  Collections.sort(FixHashAndBICSVersions.get(fixKeys.get(v)));
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
               Long lav_5TP = 0L, lav_5TN = 0L, lav_5FP = 0L, lav_5FN = 0L;
               Long lav2TP = 0L, lav2TN = 0L, lav2FP = 0L, lav2FN = 0L;
               Long mTP = 0L, mTN = 0L, mFP = 0L, mFN = 0L;
               Long csTP = 0L, csTN = 0L, csFP = 0L, csFN = 0L;
               Long szzTP = 0L, szzTN = 0L, szzFP = 0L, szzFN = 0L;
               Long szzBTP = 0L, szzBTN = 0L, szzBFP = 0L, szzBFN = 0L;
               Long ZTP = 0L, ZTN = 0L, ZFP = 0L, ZFN = 0L;
               Long TFTP = 0L, TFTN = 0L, TFFP = 0L, TFFN = 0L;
               Long fifTP = 0L, fifTN = 0L, fifFP = 0L, fifFN = 0L;
               Long SFTP = 0L, SFTN = 0L, SFFP = 0L, SFFN = 0L;
               Long HunTP = 0L, HunTN = 0L, HunFP = 0L, HunFN = 0L;
Integer train = 0;
               for ( i = 1; i <= (totalBugs/ 3); i++) {
                  Double TV2FV = 1.0;
                  if ( BugAndFix.get(i) - BugAndCreation.get(i) != 0)
                     TV2FV = (double)(BugAndFix.get(i) - BugAndCreation.get(i));
                  Psum += ((double)(BugAndFix.get(i) - BugAndAV.get(i)) / TV2FV);
                  Ptotal = (double)i;
train += 1;
                  fileWriterTrain.append(BugAndKey.get(i) + "\n");
                  //FIND KAPPA FOR EACH PROJECT

                  for (j = 1 ; j <= BugAndFix.get(i); j++) {
                     String Zp, TFp, Fifp, SFp, Hunp, actual;
                     Integer hunV, SFV, FifV,TFV, ZV;
                     if ( FixHashAndBICSVersions.containsKey(BugAndFixHash.get(BugAndKey.get(i))) ){
                        Double increment = (double)FixHashAndBICSVersions.get(BugAndFixHash.get(BugAndKey.get(i))).size() / 4.0;
                        hunV = FixHashAndBICSVersions.get(BugAndFixHash.get(BugAndKey.get(i))).get(0);
                        SFV = FixHashAndBICSVersions.get(BugAndFixHash.get(BugAndKey.get(i))).get((int)Math.floor(increment));
                        FifV = FixHashAndBICSVersions.get(BugAndFixHash.get(BugAndKey.get(i))).get((int)Math.floor(increment*2));
                        TFV = FixHashAndBICSVersions.get(BugAndFixHash.get(BugAndKey.get(i))).get((int)Math.floor(increment*3));
                        ZV = FixHashAndBICSVersions.get(BugAndFixHash.get(BugAndKey.get(i))).get(FixHashAndBICSVersions.get(BugAndFixHash.get(BugAndKey.get(i))).size() - 1);
                     }
                     else {
                        hunV = SFV = FifV = TFV = ZV = 1;
                     }
                     if (j < BugAndAV.get(i) || j >= BugAndFix.get(i))
                        actual = "No";
                     else
                        actual = "Yes";
                     if (j >=  ZV  && j < BugAndFix.get(i))
                        Zp = "Yes";
                     else
                        Zp = "No";
                     if (j >=  TFV && j < BugAndFix.get(i))
                        TFp = "Yes";
                     else
                        TFp = "No";
                     if (j >=  FifV  && j < BugAndFix.get(i))
                        Fifp = "Yes";
                     else
                        Fifp = "No";
                     if (j >=  SFV  && j < BugAndFix.get(i))
                        SFp = "Yes";
                     else
                        SFp = "No";
                     if (j >=  hunV && j < BugAndFix.get(i))
                        Hunp = "Yes";
                     else
                        Hunp = "No";

                     if (actual.equals("Yes")) {
                        if (Zp.equals("Yes"))
                           ZTP++;
                        else
                           ZFN++;
                        if (TFp.equals("Yes"))
                           TFTP++;
                        else
                           TFFN++;
                        if (Fifp.equals("Yes"))
                           fifTP++;
                        else
                           fifFN++;
                        if (SFp.equals("Yes"))
                           SFTP++;
                        else
                           SFFN++;
                        if (Hunp.equals("Yes"))
                           HunTP++;
                        else
                           HunFN++;
                     }
                     else {
                        if (Zp.equals("Yes"))
                           ZFP++;
                        else
                           ZTN++;
                        if (TFp.equals("Yes"))
                           TFFP++;
                        else
                           TFTN++;
                        if (Fifp.equals("Yes"))
                           fifFP++;
                        else
                           fifTN++;
                        if (SFp.equals("Yes"))
                           SFFP++;
                        else
                           SFTN++;
                        if (Hunp.equals("Yes"))
                           HunFP++;
                        else
                           HunTN++;
                     }
                     fileWriterTrainResults.append(BugAndKey.get(i) + "," +
                       releaseNames.get(j) + "," + Zp + "," + TFp + ","
                       + Fifp + "," + SFp + "," + Hunp + ","+ actual + "\n");
                  }
               }
System.out.println("train " + train.toString());

               Double observedP = (double)(ZTP + ZTN) / (double)(ZTP+ ZFN + ZFP + ZTN);
               Double aP = (double)((ZTP + ZFN) * (ZTP + ZFP)) / (double)(ZTP+ ZFN + ZFP + ZTN);
               Double bP = (double)((ZFP + ZTN) * (ZFN + ZTN)) / (double)(ZTP+ ZFN + ZFP + ZTN);
               Double expectedP = (aP + bP) / (double)(ZTP+ ZFN + ZFP + ZTN);
               Double kappaP0 = (observedP - expectedP)/(1 - expectedP);

               observedP = (double)(TFTP + TFTN) / (double)(TFTP+ TFFN + TFFP + TFTN);
               aP = (double)((TFTP + TFFN) * (TFTP + TFFP)) / (double)(TFTP+ TFFN + TFFP + TFTN);
               bP = (double)((TFFP + TFTN) * (TFFN + TFTN)) / (double)(TFTP+ TFFN + TFFP + TFTN);
               expectedP = (aP + bP) / (double)(TFTP+ TFFN + TFFP + TFTN);
               Double kappaP25 = (observedP - expectedP)/(1 - expectedP);

               observedP = (double)(fifTP + fifTN) / (double)(fifTP+ fifFN + fifFP + fifTN);
               aP = (double)((fifTP + fifFN) * (fifTP + fifFP)) / (double)(fifTP+ fifFN + fifFP + fifTN);
               bP = (double)((fifFP + fifTN) * (fifFN + fifTN)) / (double)(fifTP+ fifFN + fifFP + fifTN);
               expectedP = (aP + bP) / (double)(fifTP+ fifFN + fifFP + fifTN);
               Double kappaP50 = (observedP - expectedP)/(1 - expectedP);

               observedP = (double)(SFTP + SFTN) / (double)(SFTP+ SFFN + SFFP + SFTN);
               aP = (double)((SFTP + SFFN) * (SFTP + SFFP)) / (double)(SFTP+ SFFN + SFFP + SFTN);
               bP = (double)((SFFP + SFTN) * (SFFN + SFTN)) / (double)(SFTP+ SFFN + SFFP + SFTN);
               expectedP = (aP + bP) / (double)(SFTP+ SFFN + SFFP + SFTN);
               Double kappaP75 = (observedP - expectedP)/(1 - expectedP);

               observedP = (double)(HunTP + HunTN) / (double)(HunTP+ HunFN + HunFP + HunTN);
               aP = (double)((HunTP + HunFN) * (HunTP + HunFP)) / (double)(HunTP+ HunFN + HunFP + HunTN);
               bP = (double)((HunFP + HunTN) * (HunFN + HunTN)) / (double)(HunTP+ HunFN + HunFP + HunTN);
               expectedP = (aP + bP) / (double)(HunTP+ HunFN + HunFP + HunTN);
               Double kappaP100 = (observedP - expectedP)/(1 - expectedP);

               Integer bestPercent = -1;
               if ( kappaP0 >= kappaP25 && kappaP0 >= kappaP50 && kappaP0 >= kappaP75 && kappaP0 >= kappaP100)
                  bestPercent = 0;
               if ( kappaP25 >= kappaP0 && kappaP25 >= kappaP50 && kappaP25 >= kappaP75 && kappaP25 >= kappaP100)
                  bestPercent = 25;
               if ( kappaP50 >= kappaP0 && kappaP50 >= kappaP25 && kappaP50 >= kappaP75 && kappaP50 >= kappaP100)
                  bestPercent = 50;
               if ( kappaP75 >= kappaP0 && kappaP75 >= kappaP25 && kappaP75 >= kappaP50 && kappaP75 >= kappaP100)
                  bestPercent = 75;
               if ( kappaP100 >= kappaP0 && kappaP100 >= kappaP25 && kappaP100 >= kappaP50 && kappaP100 >= kappaP75)
                  bestPercent = 100;
System.out.println("Best kappa: " + bestPercent.toString());
               fileWriterSZZBest.append(token[0] + ","+ bestPercent.toString() + "\n");
System.out.println("All kappas: " + kappaP0.toString() + " " + kappaP25.toString() + " " + kappaP50.toString() + " " + kappaP75.toString() + " " + kappaP100.toString());


               ZTP = 0L; ZTN = 0L; ZFP = 0L; ZFN = 0L;
               TFTP = 0L; TFTN = 0L; TFFP = 0L; TFFN = 0L;
               fifTP = 0L; fifTN = 0L; fifFP = 0L; fifFN = 0L;
               SFTP = 0L; SFTN = 0L; SFFP = 0L; SFFN = 0L;
               HunTP = 0L; HunTN = 0L; HunFP = 0L; HunFN = 0L;
Integer test = 0;
               for ( i = ((totalBugs / 3) + 1); i <= (totalBugs * 2 / 3) ; i++) {
test += 1;
                  fileWriterTest.append(BugAndKey.get(i) +"\n");
                  for (j = 1 ; j <= BugAndFix.get(i); j++) {
                     String simple, proportion0_5, proportion1, proportion2, merge, coldStart,szz,szzB,szz0, szz25,szz50,szz75,szz100, actual;
                     if (j < BugAndAV.get(i) || j >= BugAndFix.get(i))
                        actual = "No";
                     else
                        actual = "Yes";
                     if (j >= BugAndCreation.get(i) && j < BugAndFix.get(i))
                        simple = "Yes";
                     else
                        simple = "No";

                     Double P = Psum/Ptotal;
                     if (Math.abs(P-ProjectAndP.get(token[0])) > 0.0001)
                        System.out.println("Different Ps for " + token[0] + " " + P.toString() + " " + ProjectAndP.get(token[0]).toString());
                     Double LAV = (double)BugAndFix.get(i) - (P/2);
                     if (BugAndFix.get(i) - BugAndCreation.get(i) != 0)
                        LAV = (double)BugAndFix.get(i) - ((P/2) * (double)(BugAndFix.get(i) - BugAndCreation.get(i)));
                     if (j >= LAV  && j < BugAndFix.get(i))
                        proportion0_5 = "Yes";
                     else
                        proportion0_5 = "No";

                     LAV = (double)BugAndFix.get(i) - P;
                     if (BugAndFix.get(i) - BugAndCreation.get(i) != 0)
                        LAV = (double)BugAndFix.get(i) - (P * (double)(BugAndFix.get(i) - BugAndCreation.get(i)));
                     if (j >= LAV  && j < BugAndFix.get(i))
                        proportion1 = "Yes";
                     else
                        proportion1 = "No";

                     LAV = (double)BugAndFix.get(i) - (P*2);
                     if (BugAndFix.get(i) - BugAndCreation.get(i) != 0)
                        LAV = (double)BugAndFix.get(i) - ((P*2) * (double)(BugAndFix.get(i) - BugAndCreation.get(i)));
                     if (j >= LAV  && j < BugAndFix.get(i))
                        proportion2 = "Yes";
                     else
                        proportion2 = "No";

                     LAV = (double)BugAndFix.get(i) - P;
                     if (BugAndFix.get(i) - BugAndCreation.get(i) != 0)
                        LAV = (double)BugAndFix.get(i) - (P * (double)(BugAndFix.get(i) - BugAndCreation.get(i)));
                     if ((j >= LAV || j >= BugAndCreation.get(i)) && j < BugAndFix.get(i))
                        merge = "Yes";
                     else
                        merge = "No";

                     Double coldStartP = 0.0;
                     Double totalP = 0.0;
                     for (Map.Entry<String, Double> entry : ProjectAndP.entrySet()) {
                        if (entry.getKey() != token[0]) {
                           totalP = totalP + 1.0;
                           coldStartP = coldStartP + entry.getValue();
                        }
                     }
                     coldStartP = coldStartP / totalP;
                     LAV = (double)BugAndFix.get(i) - coldStartP;
                     if (BugAndFix.get(i) - BugAndCreation.get(i) != 0)
                        LAV = (double)BugAndFix.get(i) - (coldStartP * (double)(BugAndFix.get(i) - BugAndCreation.get(i)));
                     if ((j >= LAV || j >= BugAndCreation.get(i)) && j < BugAndFix.get(i))
                        coldStart = "Yes";
                     else
                        coldStart = "No";

                     int IV = 1;
                     if (FixHashAndBICVersion.containsKey(BugAndFixHash.get(BugAndKey.get(i))))
                        IV = FixHashAndBICVersion.get(BugAndFixHash.get(BugAndKey.get(i)));
                     if (j >=  IV  && j < BugAndFix.get(i))
                        szz = "Yes";
                     else
                        szz = "No";

                     Double increment = 0.0;
                     if ( FixHashAndBICSVersions.containsKey(BugAndFixHash.get(BugAndKey.get(i)))) {
                        increment = (double)FixHashAndBICSVersions.get(BugAndFixHash.get(BugAndKey.get(i))).size() / 4.0;
                        if (bestPercent == 100)
                           IV = FixHashAndBICSVersions.get(BugAndFixHash.get(BugAndKey.get(i))).get(0);
                        else if (bestPercent == 75)
                           IV = FixHashAndBICSVersions.get(BugAndFixHash.get(BugAndKey.get(i))).get((int)Math.floor(increment));
                        else if (bestPercent == 50)
                           IV = FixHashAndBICSVersions.get(BugAndFixHash.get(BugAndKey.get(i))).get((int)Math.floor(increment*2));
                        else if (bestPercent == 25)
                           IV = FixHashAndBICSVersions.get(BugAndFixHash.get(BugAndKey.get(i))).get((int)Math.floor(increment*3));
                        else
                           IV = FixHashAndBICSVersions.get(BugAndFixHash.get(BugAndKey.get(i)))
                                                        .get(FixHashAndBICSVersions.get(BugAndFixHash.get(BugAndKey.get(i))).size() - 1);
                     }
                     else {
                        IV = 1;
                     }
                     if (j >=  IV  && j < BugAndFix.get(i))
                        szzB = "Yes";
                     else
                        szzB = "No";

                     IV = 1;
                     if (FixHashAndBICVersion.containsKey(BugAndFixHash.get(BugAndKey.get(i))))
                        IV = FixHashAndBICSVersions.get(BugAndFixHash.get(BugAndKey.get(i))).get(0);
                     if (j >=  IV  && j < BugAndFix.get(i))
                        szz100 = "Yes";
                     else
                        szz100 = "No";

                     IV = 1;
                     if (FixHashAndBICVersion.containsKey(BugAndFixHash.get(BugAndKey.get(i))))
                        IV = FixHashAndBICSVersions.get(BugAndFixHash.get(BugAndKey.get(i))).get((int)Math.floor(increment));
                     if (j >=  IV  && j < BugAndFix.get(i))
                        szz75 = "Yes";
                     else
                        szz75 = "No";

                     IV = 1;
                     if (FixHashAndBICVersion.containsKey(BugAndFixHash.get(BugAndKey.get(i))))
                        IV = FixHashAndBICSVersions.get(BugAndFixHash.get(BugAndKey.get(i))).get((int)Math.floor(increment*2));
                     if (j >=  IV  && j < BugAndFix.get(i))
                        szz50 = "Yes";
                     else
                        szz50 = "No";

                     IV = 1;
                     if (FixHashAndBICVersion.containsKey(BugAndFixHash.get(BugAndKey.get(i))))
                        IV = FixHashAndBICSVersions.get(BugAndFixHash.get(BugAndKey.get(i))).get((int)Math.floor(increment*3));
                     if (j >=  IV  && j < BugAndFix.get(i))
                        szz25 = "Yes";
                     else
                        szz25 = "No";

                     IV = 1;
                     if (FixHashAndBICVersion.containsKey(BugAndFixHash.get(BugAndKey.get(i))))
                        IV = FixHashAndBICSVersions.get(BugAndFixHash.get(BugAndKey.get(i)))
                                                     .get(FixHashAndBICSVersions.get(BugAndFixHash.get(BugAndKey.get(i))).size() - 1);
                     if (j >=  IV  && j < BugAndFix.get(i))
                        szz0 = "Yes";
                     else
                        szz0 = "No";

                     fileWriter.append(token[0] + "," + BugAndKey.get(i) + "," +
                       i.toString() + "," + releaseID.get(j) + "," + releaseNames.get(j)
                       + "," + simple + "," + proportion0_5 + "," + proportion1 + "," + proportion2
                       + "," + merge + "," + coldStart + "," + szz + "," + szzB + "," + szz0 + "," + szz25 + ","
                       + szz50 + "," + szz75 + "," + szz100 + "," + actual + "\n");

                     if (actual.equals("Yes")) {
                        if (simple.equals("Yes"))
                           sTP++;
                        else
                           sFN++;
                        if (proportion0_5.equals("Yes"))
                           lav_5TP++;
                        else
                           lav_5FN++;
                        if (proportion1.equals("Yes"))
                           lavTP++;
                        else
                           lavFN++;
                        if (proportion2.equals("Yes"))
                           lav2TP++;
                        else
                           lav2FN++;
                        if (merge.equals("Yes"))
                           mTP++;
                        else
                           mFN++;
                        if (coldStart.equals("Yes"))
                           csTP++;
                        else
                           csFN++;
                        if (szz.equals("Yes"))
                           szzTP++;
                        else
                           szzFN++;
                        if (szzB.equals("Yes"))
                           szzBTP++;
                        else
                           szzBFN++;
                        if (szz0.equals("Yes"))
                           ZTP++;
                        else
                           ZFN++;
                        if (szz25.equals("Yes"))
                           TFTP++;
                        else
                           TFFN++;
                        if (szz50.equals("Yes"))
                           fifTP++;
                        else
                           fifFN++;
                        if (szz75.equals("Yes"))
                           SFTP++;
                        else
                           SFFN++;
                        if (szz100.equals("Yes"))
                           HunTP++;
                        else
                           HunFN++;
                     }
                     else {
                        if (simple.equals("Yes"))
                           sFP++;
                        else
                           sTN++;
                        if (proportion0_5.equals("Yes"))
                           lav_5FP++;
                        else
                           lav_5TN++;
                        if (proportion1.equals("Yes"))
                           lavFP++;
                        else
                           lavTN++;
                        if (proportion2.equals("Yes"))
                           lav2FP++;
                        else
                           lav2TN++;
                        if (merge.equals("Yes"))
                           mFP++;
                        else
                           mTN++;
                        if (coldStart.equals("Yes"))
                           csFP++;
                        else
                           csTN++;
                        if (szz.equals("Yes"))
                           szzFP++;
                        else
                           szzTN++;
                        if (szzB.equals("Yes"))
                           szzBFP++;
                        else
                           szzBTN++;
                        if (szz0.equals("Yes"))
                           ZFP++;
                        else
                           ZTN++;
                        if (szz25.equals("Yes"))
                           TFFP++;
                        else
                           TFTN++;
                        if (szz50.equals("Yes"))
                           fifFP++;
                        else
                           fifTN++;
                        if (szz75.equals("Yes"))
                           SFFP++;
                        else
                           SFTN++;
                        if (szz100.equals("Yes"))
                           HunFP++;
                        else
                           HunTN++;
                     }
                  }
               }
System.out.println("test " +test.toString());
               Double precision = (double)sTP /(double)(sTP + sFP);
               Double recall = (double)sTP /(double)(sTP + sFN);
               Double F1 = (double)(precision * recall *2) / (double)(precision + recall);
               Double MCC = (double)(sTP * sTN - sFP * sFN) / (Math.sqrt((sTP + sFP) * (sFN + sTN) * (sFP + sTN) * (sTP + sFN)));
               Double observed = (double)(sTP + sTN) / (double)(sTP+ sFN + sFP + sTN);
               Double a = (double)((sTP + sFN) * (sTP + sFP)) / (double)(sTP+ sFN + sFP + sTN);
               Double b = (double)((sFP + sTN) * (sFN + sTN)) / (double)(sTP+ sFN + sFP + sTN);
               Double expected = (a + b) / (double)(sTP+ sFN + sFP + sTN);
               Double kappa = (observed - expected)/(1 - expected);
               fileWriter2.append(token[0] +  "," + "Simple" + "," + precision.toString() + "," + recall.toString()
                  + "," + F1.toString() +"," + MCC.toString() + "," + kappa.toString() + "\n");

               precision = (double)lav_5TP /(double)(lav_5TP + lav_5FP);
               recall = (double)lav_5TP /(double)(lav_5TP + lav_5FN);
               F1 = (precision * recall * 2) / (precision + recall);
               MCC = (double)((lav_5TP * lav_5TN) - (lav_5FP * lav_5FN)) / (Math.sqrt((double)((lav_5TP + lav_5FP)*(lav_5TP + lav_5FN)*(lav_5TN + lav_5FP)*(lav_5TN+lav_5FN))));
               observed = (double)(lav_5TP + lav_5TN) / (double)(lav_5TP+ lav_5FN + lav_5FP + lav_5TN);
               a = (double)((lav_5TP + lav_5FN) * (lav_5TP + lav_5FP)) / (double)(lav_5TP+ lav_5FN + lav_5FP + lav_5TN);
               b = (double)((lav_5FP + lav_5TN) * (lav_5FN + lav_5TN)) / (double)(lav_5TP+ lav_5FN + lav_5FP + lav_5TN);
               expected = (a + b) / (double)(lav_5TP+ lav_5FN + lav_5FP + lav_5TN);
               kappa = (observed - expected)/(1 - expected);
               fileWriter2.append(token[0] +  "," + "Proportion0.5" + "," + precision.toString() + "," + recall.toString()
                  + "," + F1.toString() +"," + MCC.toString() + "," + kappa.toString() + "\n");

               precision = (double)lavTP /(double)(lavTP + lavFP);
               recall = (double)lavTP /(double)(lavTP + lavFN);
               F1 = (precision * recall * 2) / (precision + recall);
               MCC = (double)((lavTP * lavTN) - (lavFP * lavFN)) / (Math.sqrt((double)((lavTP + lavFP)*(lavTP + lavFN)*(lavTN + lavFP)*(lavTN+lavFN))));
               observed = (double)(lavTP + lavTN) / (double)(lavTP+ lavFN + lavFP + lavTN);
               a = (double)((lavTP + lavFN) * (lavTP + lavFP)) / (double)(lavTP+ lavFN + lavFP + lavTN);
               b = (double)((lavFP + lavTN) * (lavFN + lavTN)) / (double)(lavTP+ lavFN + lavFP + lavTN);
               expected = (a + b) / (double)(lavTP+ lavFN + lavFP + lavTN);
               kappa = (observed - expected)/(1 - expected);
               fileWriter2.append(token[0] +  "," + "Proportion1" + "," + precision.toString() + "," + recall.toString()
                  + "," + F1.toString() +"," + MCC.toString() + "," + kappa.toString() + "\n");

               precision = (double)lav2TP /(double)(lav2TP + lav2FP);
               recall = (double)lav2TP /(double)(lav2TP + lav2FN);
               F1 = (precision * recall * 2) / (precision + recall);
               MCC = (double)((lav2TP * lav2TN) - (lav2FP * lav2FN)) / (Math.sqrt((double)((lav2TP + lav2FP)*(lav2TP + lav2FN)*(lav2TN + lav2FP)*(lav2TN+lav2FN))));
               observed = (double)(lav2TP + lav2TN) / (double)(lav2TP+ lav2FN + lav2FP + lav2TN);
               a = (double)((lav2TP + lav2FN) * (lav2TP + lav2FP)) / (double)(lav2TP+ lav2FN + lav2FP + lav2TN);
               b = (double)((lav2FP + lav2TN) * (lav2FN + lav2TN)) / (double)(lav2TP+ lav2FN + lav2FP + lav2TN);
               expected = (a + b) / (double)(lav2TP+ lav2FN + lav2FP + lav2TN);
               kappa = (observed - expected)/(1 - expected);
               fileWriter2.append(token[0] +  "," + "Proportion2" + "," + precision.toString() + "," + recall.toString()
                  + "," + F1.toString() +"," + MCC.toString() + "," + kappa.toString() + "\n");

               precision = (double)mTP /(double)(mTP + mFP);
               recall = (double)mTP /(double)(mTP + mFN);
               F1 = (precision * recall * 2) / (precision + recall);
               MCC = (double)((mTP * mTN) - (mFP * mFN)) / (Math.sqrt((double)((mTP + mFP)*(mTP + mFN)*(mTN + mFP)*(mTN+mFN))));
               observed = (double)(mTP + mTN) / (double)(mTP+ mFN + mFP + mTN);
               a = (double)((mTP + mFN) * (mTP + mFP)) / (double)(mTP+ mFN + mFP + mTN);
               b = (double)((mFP + mTN) * (mFN + mTN)) / (double)(mTP+ mFN + mFP + mTN);
               expected = (a + b) / (double)(mTP+ mFN + mFP + mTN);
               kappa = (observed - expected)/(1 - expected);
               fileWriter2.append(token[0] +  "," + "Merge" + "," + precision.toString() + "," + recall.toString()
                  + "," + F1.toString() +"," + MCC.toString() + "," + kappa.toString() + "\n");

               precision = (double)csTP /(double)(csTP + csFP);
               recall = (double)csTP /(double)(csTP + csFN);
               F1 = (precision * recall * 2) / (precision + recall);
               MCC = (double)((csTP * csTN) - (csFP * csFN)) / (Math.sqrt((double)((csTP + csFP)*(csTP + csFN)*(csTN + csFP)*(csTN+csFN))));
               observed = (double)(csTP + csTN) / (double)(csTP+ csFN + csFP + csTN);
               a = (double)((csTP + csFN) * (csTP + csFP)) / (double)(csTP+ csFN + csFP + csTN);
               b = (double)((csFP + csTN) * (csFN + csTN)) / (double)(csTP+ csFN + csFP + csTN);
               expected = (a + b) / (double)(csTP+ csFN + csFP + csTN);
               kappa = (observed - expected)/(1 - expected);
               fileWriter2.append(token[0] +  "," + "Cold Start Proportion" + "," + precision.toString() + "," + recall.toString()
                  + "," + F1.toString() +"," + MCC.toString() + "," + kappa.toString() + "\n");

               precision = (double)szzTP /(double)(szzTP + szzFP);
               recall = (double)szzTP /(double)(szzTP + szzFN);
               F1 = (precision * recall * 2) / (precision + recall);
               MCC = (double)((szzTP * szzTN) - (szzFP * szzFN)) / (Math.sqrt((double)((szzTP + szzFP)*(szzTP + szzFN)*(szzTN + szzFP)*(szzTN+szzFN))));
               observed = (double)(szzTP + szzTN) / (double)(szzTP+ szzFN + szzFP + szzTN);
               a = (double)((szzTP + szzFN) * (szzTP + szzFP)) / (double)(szzTP+ szzFN + szzFP + szzTN);
               b = (double)((szzFP + szzTN) * (szzFN + szzTN)) / (double)(szzTP+ szzFN + szzFP + szzTN);
               expected = (a + b) / (double)(szzTP+ szzFN + szzFP + szzTN);
               kappa = (observed - expected)/(1 - expected);
               fileWriter2.append(token[0] +  "," + "SZZu" + "," + precision.toString() + "," + recall.toString()
                  + "," + F1.toString() +"," + MCC.toString() + "," + kappa.toString() + "\n");

               precision = (double)szzBTP /(double)(szzBTP + szzBFP);
               recall = (double)szzBTP /(double)(szzBTP + szzBFN);
               F1 = (precision * recall * 2) / (precision + recall);
               MCC = (double)((szzBTP * szzBTN) - (szzBFP * szzBFN)) / (Math.sqrt((double)((szzBTP + szzBFP)*(szzBTP + szzBFN)*(szzBTN + szzBFP)*(szzBTN+szzBFN))));
               observed = (double)(szzBTP + szzBTN) / (double)(szzBTP+ szzBFN + szzBFP + szzBTN);
               a = (double)((szzBTP + szzBFN) * (szzBTP + szzBFP)) / (double)(szzBTP+ szzBFN + szzBFP + szzBTN);
               b = (double)((szzBFP + szzBTN) * (szzBFN + szzBTN)) / (double)(szzBTP+ szzBFN + szzBFP + szzBTN);
               expected = (a + b) / (double)(szzBTP+ szzBFN + szzBFP + szzBTN);
               kappa = (observed - expected)/(1 - expected);
               fileWriter2.append(token[0] +  "," + "SZZuBest" + "," + precision.toString() + "," + recall.toString()
                  + "," + F1.toString() +"," + MCC.toString() + "," + kappa.toString() + "\n");

               precision = (double)ZTP /(double)(ZTP + ZFP);
               recall = (double)ZTP /(double)(ZTP + ZFN);
               F1 = (precision * recall * 2) / (precision + recall);
               MCC = (double)((ZTP * ZTN) - (ZFP * ZFN)) / (Math.sqrt((double)((ZTP + ZFP)*(ZTP + ZFN)*(ZTN + ZFP)*(ZTN+ZFN))));
               observed = (double)(ZTP + ZTN) / (double)(ZTP+ ZFN + ZFP + ZTN);
               a = (double)((ZTP + ZFN) * (ZTP + ZFP)) / (double)(ZTP+ ZFN + ZFP + ZTN);
               b = (double)((ZFP + ZTN) * (ZFN + ZTN)) / (double)(ZTP+ ZFN + ZFP + ZTN);
               expected = (a + b) / (double)(ZTP+ ZFN + ZFP + ZTN);
               kappa = (observed - expected)/(1 - expected);
               fileWriter2.append(token[0] +  "," + "SZZu0" + "," + precision.toString() + "," + recall.toString()
                  + "," + F1.toString() +"," + MCC.toString() + "," + kappa.toString() + "\n");

               precision = (double)TFTP /(double)(TFTP + TFFP);
               recall = (double)TFTP /(double)(TFTP + TFFN);
               F1 = (precision * recall * 2) / (precision + recall);
               MCC = (double)((TFTP * TFTN) - (TFFP * TFFN)) / (Math.sqrt((double)((TFTP + TFFP)*(TFTP + TFFN)*(TFTN + TFFP)*(TFTN+TFFN))));
               observed = (double)(TFTP + TFTN) / (double)(TFTP+ TFFN + TFFP + TFTN);
               a = (double)((TFTP + TFFN) * (TFTP + TFFP)) / (double)(TFTP+ TFFN + TFFP + TFTN);
               b = (double)((TFFP + TFTN) * (TFFN + TFTN)) / (double)(TFTP+ TFFN + TFFP + TFTN);
               expected = (a + b) / (double)(TFTP+ TFFN + TFFP + TFTN);
               kappa = (observed - expected)/(1 - expected);
               fileWriter2.append(token[0] +  "," + "SZZu25" + "," + precision.toString() + "," + recall.toString()
                  + "," + F1.toString() +"," + MCC.toString() + "," + kappa.toString() + "\n");

               precision = (double)fifTP /(double)(fifTP + fifFP);
               recall = (double)fifTP /(double)(fifTP + fifFN);
               F1 = (precision * recall * 2) / (precision + recall);
               MCC = (double)((fifTP * fifTN) - (fifFP * fifFN)) / (Math.sqrt((double)((fifTP + fifFP)*(fifTP + fifFN)*(fifTN + fifFP)*(fifTN+fifFN))));
               observed = (double)(fifTP + fifTN) / (double)(fifTP+ fifFN + fifFP + fifTN);
               a = (double)((fifTP + fifFN) * (fifTP + fifFP)) / (double)(fifTP+ fifFN + fifFP + fifTN);
               b = (double)((fifFP + fifTN) * (fifFN + fifTN)) / (double)(fifTP+ fifFN + fifFP + fifTN);
               expected = (a + b) / (double)(fifTP+ fifFN + fifFP + fifTN);
               kappa = (observed - expected)/(1 - expected);
               fileWriter2.append(token[0] +  "," + "SZZu50" + "," + precision.toString() + "," + recall.toString()
                  + "," + F1.toString() +"," + MCC.toString() + "," + kappa.toString() + "\n");

               precision = (double)SFTP /(double)(SFTP + SFFP);
               recall = (double)SFTP /(double)(SFTP + SFFN);
               F1 = (precision * recall * 2) / (precision + recall);
               MCC = (double)((SFTP * SFTN) - (SFFP * SFFN)) / (Math.sqrt((double)((SFTP + SFFP)*(SFTP + SFFN)*(SFTN + SFFP)*(SFTN+SFFN))));
               observed = (double)(SFTP + SFTN) / (double)(SFTP+ SFFN + SFFP + SFTN);
               a = (double)((SFTP + SFFN) * (SFTP + SFFP)) / (double)(SFTP+ SFFN + SFFP + SFTN);
               b = (double)((SFFP + SFTN) * (SFFN + SFTN)) / (double)(SFTP+ SFFN + SFFP + SFTN);
               expected = (a + b) / (double)(SFTP+ SFFN + SFFP + SFTN);
               kappa = (observed - expected)/(1 - expected);
               fileWriter2.append(token[0] +  "," + "SZZu75" + "," + precision.toString() + "," + recall.toString()
                  + "," + F1.toString() +"," + MCC.toString() + "," + kappa.toString() + "\n");

               precision = (double)HunTP /(double)(HunTP + HunFP);
               recall = (double)HunTP /(double)(HunTP + HunFN);
               F1 = (precision * recall * 2) / (precision + recall);
               MCC = (double)((HunTP * HunTN) - (HunFP * HunFN)) / (Math.sqrt((double)((HunTP + HunFP)*(HunTP + HunFN)*(HunTN + HunFP)*(HunTN+HunFN))));
               observed = (double)(HunTP + HunTN) / (double)(HunTP+ HunFN + HunFP + HunTN);
               a = (double)((HunTP + HunFN) * (HunTP + HunFP)) / (double)(HunTP+ HunFN + HunFP + HunTN);
               b = (double)((HunFP + HunTN) * (HunFN + HunTN)) / (double)(HunTP+ HunFN + HunFP + HunTN);
               expected = (a + b) / (double)(HunTP+ HunFN + HunFP + HunTN);
               kappa = (observed - expected)/(1 - expected);
               fileWriter2.append(token[0] +  "," + "SZZu100" + "," + precision.toString() + "," + recall.toString()
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
               fileWriterTrain.flush();
               fileWriterTrain.close();
               fileWriterTest.flush();
               fileWriterTest.close();
               fileWriterTrainResults.flush();
               fileWriterTrainResults.close();
               fileWriterSZZBest.flush();
               fileWriterSZZBest.close();
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
