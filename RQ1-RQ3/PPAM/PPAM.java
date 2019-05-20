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
import java.time.temporal.ChronoUnit;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

public class PPAM {

   public static ArrayList<LocalDateTime> releases;
   public static HashMap<String, Integer> closedBugs;
   public static HashMap<String, String> closedBugsAndClasses;
   public static HashMap<String, String> closedBugsAndRepo;
   public static HashMap<String, Integer> closedBugsAndAV;
   public static HashMap<String, Integer> closedBugsAndCreation;
   public static HashMap<String, LocalDateTime> closedBugsAndFixDate;
   public static HashMap<String, String> closedBugsAndResolution;
   public static HashMap<LocalDateTime, String> releaseNames;
   public static HashMap<LocalDateTime, String> releaseID;

  //Adds a release to the arraylist
   public static void addRelease(String strDate, String name, String id) {
      LocalDate date = LocalDate.parse(strDate);
      LocalDateTime dateTime = date.atStartOfDay();
      if (!releases.contains(dateTime))
         releases.add(dateTime);
      releaseNames.put(dateTime, name);
      releaseID.put(dateTime, id);
      return;
   }

   // Finds the release corresponding to a creation or resolution date
   public static Integer findRelease(String strDate) {
      LocalDateTime dateTime = LocalDateTime.parse(strDate);
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

   //Finds the exact release for affect versions
   public static Integer findExactRelease(String strDate) {
      LocalDate date = LocalDate.parse(strDate);
      LocalDateTime dateTime = date.atStartOfDay();
      int i = releases.indexOf(dateTime);
      if ( i == -1) {
         System.out.println("Did not find exact release for date " + strDate);
         System.exit(-1);
      }
      //account for 0 offset
      return i + 1;
   }

   private static String readAll(Reader rd) throws IOException {
      StringBuilder sb = new StringBuilder();
      int cp;
      while ((cp = rd.read()) != -1) {
         sb.append((char) cp);
      }
      return sb.toString();
   }

   public static JSONArray readJsonArrayFromUrl(String url) throws IOException, JSONException {
      InputStream is = new URL(url).openStream();
      try {
         BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
         String jsonText = readAll(rd);
         JSONArray json = new JSONArray(jsonText);
         return json;
       } finally {
         is.close();
       }
   }

   public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
      InputStream is = new URL(url).openStream();
      try {
         BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
         String jsonText = readAll(rd);
         JSONObject json = new JSONObject(jsonText);
         return json;
       } finally {
         is.close();
       }
   }

   //Fills the arraylist with releases dates and orders them
   //Ignores releases with missing dates
   public static void getReleases(String projName) throws IOException, JSONException  {
         Integer i;
         String url = "https://issues.apache.org/jira/rest/api/2/project/" + projName;
         JSONObject json = readJsonFromUrl(url);
         JSONArray versions = json.getJSONArray("versions");
         releaseNames = new HashMap<LocalDateTime, String>();
         releaseID = new HashMap<LocalDateTime, String> ();
         for (i = 0; i < versions.length(); i++ ) {
            String name = "";
            String id = "";
            if(versions.getJSONObject(i).has("releaseDate")) {
               if (versions.getJSONObject(i).has("name"))
                  name = versions.getJSONObject(i).get("name").toString();
               if (versions.getJSONObject(i).has("id"))
                  id = versions.getJSONObject(i).get("id").toString();
               addRelease(versions.getJSONObject(i).get("releaseDate").toString(),
                          name,id);
            }
         }
         // order releases by date
         Collections.sort(releases, new Comparator<LocalDateTime>(){
            @Override
            public int compare(LocalDateTime o1, LocalDateTime o2) {
                return o1.compareTo(o2);
            }
         });
         return;
   }


   public static void getBugInfo(String projName) throws IOException, JSONException {
          Integer j = 0, i = 0, total = 1, k = 0;
      //Get JSON API for closed bugs w/ AV in the project
      do {
         //Only gets a max of 1000 at a time, so must do this multiple times if bugs >1000
         j = i + 1000;
         String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
                + projName + "%22AND%22issueType%22=%22Bug%22AND(%22status%22=%22closed%22"
                + "OR%22status%22=%22resolved%22)AND%22resolution%22=%22fixed%22%20ORDER%20BY%20%22resolutiondate"
                + "%22ASC&fields=key,resolutiondate,versions,created&startAt="
                + i.toString() + "&maxResults=" + j.toString();

         JSONObject json = readJsonFromUrl(url);
         JSONArray issues = json.getJSONArray("issues");
         total = json.getInt("total");
         for ( i = i; i < total && i < j; i++) {
            //Iterate through each bug
            String key = issues.getJSONObject(i%1000).get("key").toString();
            closedBugs.put(key, null);
            //Get creation date corresponding release indexes
            String createdStr = issues.getJSONObject(i%1000)
                         .getJSONObject("fields").get("created").toString().substring(0,20);
            Integer created = findRelease(createdStr);
            closedBugsAndCreation.put(key,created);

            String resolution = issues.getJSONObject(i%1000)
                         .getJSONObject("fields").get("resolutiondate").toString();
            closedBugsAndResolution.put(key,resolution);
            //Get list of AV recorded for the bug
            if( issues.getJSONObject(i%1000).getJSONObject("fields").has("versions")) {
               JSONArray versions = issues.getJSONObject(i%1000).getJSONObject("fields")
                .getJSONArray("versions");
               closedBugsAndAV.put(key, null);
               for ( k = 0; k < versions.length(); k++) {
                  if (versions.getJSONObject(k).has("releaseDate")) {
                     Integer x = findExactRelease(versions.getJSONObject(k)
                         .get("releaseDate").toString());
                     if ( closedBugsAndAV.get(key) == null || x < closedBugsAndAV.get(key)) {
                        closedBugsAndAV.put(key, x);
                     }
                  }
               }
            }
            else {
               closedBugsAndAV.put(key, null);
            }
         }
      } while (i < total);
      return;
   }

   public static void main(String[] args) throws IOException, JSONException {
      String csvFile = "Projects.csv";
      String trainFile = "../RQ3-AllAVRetrievalMethods/TrainBugs.csv";
      String testFile = "../RQ3-AllAVRetrievalMethods/TestBugs.csv";
      String cvsSplitBy = ",";
      Integer i, j, t;
      ArrayList<String> trainBugs = new ArrayList<String>();
      ArrayList<String> testBugs = new ArrayList<String>();
      try (BufferedReader br = new BufferedReader(new FileReader(trainFile))) {
         String linefromcsv;
         while ( (linefromcsv = br.readLine()) != null) {
            trainBugs.add(linefromcsv);
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
      try (BufferedReader br = new BufferedReader(new FileReader(testFile))) {
         String linefromcsv;
         while ( (linefromcsv = br.readLine()) != null) {
            testBugs.add(linefromcsv);
         }
      } catch (IOException e) {
         e.printStackTrace();
      }

      try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
         String linefromcsv, newline, write;
         Integer num = Integer.parseInt(args[0]);

         int n = 0;
         while ( (linefromcsv = br.readLine()) != null) {
            if ( num == n) {
            FileWriter fileWriterTrain = null;
            FileWriter fileWriterTest = null;
				    try {
               fileWriterTrain = null;
               fileWriterTest = null;
               String[] token = linefromcsv.split(cvsSplitBy);
               String outnameTrain = token[0] + "PPAMTrainSet.csv";
               String outnameTest = token[0] + "PPAMTestSet.csv";
				       //Name of CSV for output
				       fileWriterTrain = new FileWriter(outnameTrain);
				       fileWriterTest = new FileWriter(outnameTest);
				       //Header for CSV
				       fileWriterTrain.append("Bug ID,Version Index,Version Name,Days from Version to Ticket Creation,LOC touched from Version to Ticket Creation,Bugginess");
				       fileWriterTrain.append("\n");
				       fileWriterTest.append("Bug ID,Version Index,Version Name,Days from Version to Ticket Creation,LOC touched from Version to Ticket Creation,Bugginess");
				       fileWriterTest.append("\n");
               releases = new ArrayList<LocalDateTime>();
               getReleases(token[0]);

						   closedBugs = new HashMap<String, Integer>();
						   closedBugsAndClasses = new HashMap<String, String>();
						   closedBugsAndRepo = new HashMap<String, String>();
               closedBugsAndAV = new HashMap<String, Integer>();
               closedBugsAndCreation = new HashMap<String, Integer>();
               closedBugsAndFixDate = new HashMap<String, LocalDateTime>();
               closedBugsAndResolution = new HashMap<String, String>();
               getBugInfo(token[0]);
               ArrayList<String> bugs = new ArrayList<String>(closedBugs.keySet());

               //check all repos for the bug key
               for ( i = 1; i < token.length; i++) {
                  //String command = "git clone " + token[i];
                  //Process proc = Runtime.getRuntime().exec(command);
                  //proc.waitFor();
                  String[] token2 = token[i].split("/");
                  String path =  token2[token2.length - 1].substring(0, token2[token2.length - 1].length() - 4);
                  for ( j = 0; j < bugs.size(); j++) {
                     String command = "git --git-dir ./GitClones/" + path +
                     "/.git log --branches --grep="+ bugs.get(j) +" --pretty=format:%cd%H --date=iso-strict -1";
                      // Read the output
                      Process proc = Runtime.getRuntime().exec(command);
                      BufferedReader reader =
                        new BufferedReader(new InputStreamReader(proc.getInputStream()));
                      String line = "";
                      if((line = reader.readLine()) != null) {
                          proc.waitFor();
                          String datePart = line.substring(0, 19);
                          String hash = line.substring(25);
                          Integer x = findRelease(datePart);
                          LocalDateTime fixDateTime = LocalDateTime.parse(datePart);
                          if (closedBugs.get(bugs.get(j)) == null || closedBugs.get(bugs.get(j)) < x
                              || closedBugsAndFixDate.get(bugs.get(j)).isBefore(fixDateTime)) {
                               closedBugs.put(bugs.get(j), x);
                               closedBugsAndFixDate.put(bugs.get(j),fixDateTime);
                               command = "git --git-dir ./GitClones/" + path + "/.git " +
                                 "diff --name-only " + hash + "^ " + hash;
                               proc = Runtime.getRuntime().exec(command);
                               reader =
				                         new BufferedReader(new InputStreamReader(proc.getInputStream()));
                               line = "";
                               String classes = "";
                               while((line = reader.readLine()) != null) {
                                  classes += line + "|";
                               }
                               proc.waitFor();
                               closedBugsAndClasses.put(bugs.get(j), classes);
                               closedBugsAndRepo.put(bugs.get(j), path);
                          }
                      }
                      else {
                        proc.waitFor();
                      }
                  }
               }

               for ( j = 0; j < bugs.size(); j++) {

                  if ( closedBugs.get(bugs.get(j)) != null && closedBugsAndAV.get(bugs.get(j)) != null
                         && closedBugsAndAV.get(bugs.get(j)) <= closedBugsAndCreation.get(bugs.get(j))
                         && closedBugsAndAV.get(bugs.get(j)) <= closedBugs.get(bugs.get(j)) &&
                         !(closedBugsAndAV.get(bugs.get(j)) == closedBugsAndCreation.get(bugs.get(j)) &&
                          closedBugsAndAV.get(bugs.get(j)) == closedBugs.get(bugs.get(j))) &&
                          (trainBugs.contains(bugs.get(j)) || testBugs.contains(bugs.get(j)))) {
System.out.println(bugs.get(j));
                     String[] classes = closedBugsAndClasses.get(bugs.get(j)).split("\\|");
                     ArrayList<Long> linesChangedPerClass = new ArrayList<Long>();
                     for (int cn = 0; cn < classes.length; cn++) {
                        linesChangedPerClass.add(cn, 0L);
                     }
                     for ( Integer v = closedBugs.get(bugs.get(j)) - 1; v >=0; v--) {

                         Long days = 0L;
                         if (v != (closedBugs.get(bugs.get(j)) - 1) )
                            days = ChronoUnit.DAYS.between(releases.get(v), closedBugsAndFixDate.get(bugs.get(j)));
                         Integer vIndex = v + 1;
                         String buggy = "No";
                         if ( (v+1) >= closedBugsAndAV.get(bugs.get(j)) && (v+1) < closedBugs.get(bugs.get(j)))
                            buggy = "Yes";

                         Long locTouched = 0L;
                         for ( int c = 0; c < classes.length; c++) {
                             Integer spaces =( classes[c].length() - classes[c].replaceAll(" ", "").length()) + 3;
                             String command = "";
                             if ( v == (closedBugs.get(bugs.get(j)) -1 )){
                                command = "git --git-dir ./GitClones/" + closedBugsAndRepo.get(bugs.get(j)) + "/.git " +
				                                  "log --stat=5050,5000 --since " + closedBugsAndFixDate.get(bugs.get(j)) + " --until " +
                                          closedBugsAndFixDate.get(bugs.get(j)) + " | grep " +
                                          "\"^ " + classes[c] + "[ \\t][ \\t]*|[ \\t][ \\t]*\\d\\d*[ \\t][+-]*$\"" + " | awk \'{print $"+ spaces.toString()+"}\'";
                             }
                             else if ( v == (closedBugs.get(bugs.get(j)) -2 )){
                                command = "git --git-dir ./GitClones/" + closedBugsAndRepo.get(bugs.get(j)) + "/.git " +
				                                  "log --stat=5050,5000 --since " + releases.get(v) + " --until " +
                                          closedBugsAndFixDate.get(bugs.get(j)) + " | grep " +
                                          "\"^ " + classes[c] + "[ \\t][ \\t]*|[ \\t][ \\t]*\\d\\d*[ \\t][+-]*$\"" + " | awk \'{print $"+ spaces.toString()+"}\'";
                             }
                             else{
                                command = "git --git-dir ./GitClones/" + closedBugsAndRepo.get(bugs.get(j)) + "/.git " +
				                                  "log --stat=5050,5000 --since " + releases.get(v) + " --until " +
                                          releases.get(v+1) + " | grep " +
                                          "\"^ " + classes[c] + "[ \\t][ \\t]*|[ \\t][ \\t]*\\d\\d*[ \\t][+-]*$\"" + " | awk \'{print $"+ spaces.toString()+"}\'";
                             }

                             String path = "./" + closedBugsAndRepo.get(bugs.get(j)) + "/.git";
				                     Process proc2 = Runtime.getRuntime().exec(new String[] {"/bin/sh", "-c", command});
				                     BufferedReader reader2 =
														  new BufferedReader(new InputStreamReader(proc2.getInputStream()));
				                     String line = "";
				                     while((line = reader2.readLine()) != null) {
                                if (line.length() > 0)
				                           linesChangedPerClass.set(c, linesChangedPerClass.get(c) + Long.parseLong(line));
				                     }
				                     proc2.waitFor();
                         }
                         for (int cn = 0; cn < classes.length; cn++) {
                            locTouched += linesChangedPerClass.get(cn);
                         }
                         String vname = "not released";
                         if ( v < releases.size())
                             vname = releaseNames.get(releases.get(v));
                         if (trainBugs.contains(bugs.get(j))) {
                            fileWriterTrain.append(bugs.get(j) + "," + vIndex.toString() +
                              "," + vname + "," + days.toString() + "," +
                              locTouched.toString() + "," + buggy + "\n");
                         }
                         else if (testBugs.contains(bugs.get(j))) {
                            fileWriterTest.append(bugs.get(j) + "," + vIndex.toString() +
                              "," + vname + "," + days.toString() + "," +
                              locTouched.toString() + "," + buggy + "\n");
                         }
                     }
                  }
               }
            } catch (Exception e) {
               System.out.println("Error in csv writer");
               e.printStackTrace();
            } finally {
               try {
                  fileWriterTest.flush();
                  fileWriterTest.close();
                  fileWriterTrain.flush();
                  fileWriterTrain.close();
               } catch (IOException e) {
                  System.out.println("Error while flushing/closing fileWriter !!!");
                  e.printStackTrace();
               }
            }
         }
         n++;
}
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
}
