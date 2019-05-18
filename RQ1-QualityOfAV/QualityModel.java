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

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

public class QualityModel {

   public static ArrayList<LocalDateTime> releases;
   public static HashMap<String, Integer> closedBugs;
   public static HashMap<String, Integer> closedBugsAndAV;
   public static HashMap<String, Integer> closedBugsAndCreation;
   public static Double hasAffectVersionAfterCreation;
   public static Double noAffectVersion;
   public static Double bugWithProblem;
   public static Double closedAndLinkedBugs;

  //Adds a release to the arraylist
   public static void addRelease(String strDate) {
      LocalDate date = LocalDate.parse(strDate);
      LocalDateTime dateTime = date.atStartOfDay();
      if ( !releases.contains(dateTime))
         releases.add(dateTime);
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
         for (i = 0; i < versions.length(); i++ ) {
            if(versions.getJSONObject(i).has("releaseDate"))
               addRelease(versions.getJSONObject(i).get("releaseDate").toString());
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
                + projName + "%22AND%22issueType%22=%22Bug%22AND(%22status%22=%22closed%22OR"
                + "%22status%22=%22resolved%22)AND%22resolution%22=%22fixed%22&fields=key,versions,created&startAt="
                + i.toString() + "&maxResults=" + j.toString();
         JSONObject json = readJsonFromUrl(url);
         JSONArray issues = json.getJSONArray("issues");
         total = json.getInt("total");
         for ( i = i; i < total && i < j; i++) {
            //Iterate through each bug
            String key = issues.getJSONObject(i%1000).get("key").toString();
            closedBugs.put(key, null);
            //Get creation date corresponding release indexes
            Integer created = findRelease(issues.getJSONObject(i%1000)
                         .getJSONObject("fields").get("created").toString().substring(0,20));
            closedBugsAndCreation.put(key,created);
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
      String csvFile = "GetProjectNamesAndUrls-HelperPrograms/ProjectsAndUrlsEditted.csv";
      String cvsSplitBy = ",";
      Integer i, j, t, k;
      try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
         FileWriter fileWriter = null;
				 try {
				    String linefromcsv, newline, write;
				    //Name of CSV for output
				    fileWriter = new FileWriter(args[0] + "QualityModel.csv");
				    //Header for CSV
				    fileWriter.append("Key,Number of Closed and Linked Bugs," +
              "Number of C/L Bugs with no AV,Percent of C/L Bugs with no AV," +
              "Number of C/L Bugs with First AV after Ticket Creation,Percent of C/L Bugs with First AV after Ticket Creation," +
              "Number of C/L Bugs with a Problem,Percent of C/L Bugs with a Problem");
				    fileWriter.append("\n");

            for ( k = 1; k < Integer.parseInt(args[0]); k++)
               br.readLine();
            //reade from csv one project at a time
            while ((linefromcsv = br.readLine()) != null && k <= Integer.parseInt(args[1])) {
               k++;
               String[] token = linefromcsv.split(cvsSplitBy);
               if ( token.length > 2) {
                  releases = new ArrayList<LocalDateTime>();
                  getReleases(token[0]);

						      closedBugs = new HashMap<String, Integer>();
                  closedBugsAndAV = new HashMap<String, Integer>();
                  closedBugsAndCreation = new HashMap<String, Integer>();
                  getBugInfo(token[0]);
                  ArrayList<String> bugs = new ArrayList<String>(closedBugs.keySet());

                  //check all repos for the bug key
                  for ( i = 2; i < token.length; i++) {
                     String command = "git clone " + token[i];
                     Process proc = Runtime.getRuntime().exec(command);
                     proc.waitFor();
                     String[] token2 = token[i].split("/");
                     String path =  token2[token2.length - 1].substring(0, token2[token2.length - 1].length() - 4);
                     for ( j = 0; j < bugs.size(); j++) {
                        command = "git --git-dir ./" + path +
                         "/.git log --branches --grep="+ bugs.get(j) +" --pretty=format:%cd --date=iso-strict -1";
                        // Read the output
                        proc = Runtime.getRuntime().exec(command);
                        BufferedReader reader =
                           new BufferedReader(new InputStreamReader(proc.getInputStream()));
                        String line = "";
                        if((line = reader.readLine()) != null) {
                           line = line.substring(0, 19);
                           Integer x = findRelease(line);
                           if (closedBugs.get(bugs.get(j)) == null || closedBugs.get(bugs.get(j)) < x)
                               closedBugs.put(bugs.get(j), x);
                        }
                        proc.waitFor();
                     }
                  }

                  hasAffectVersionAfterCreation = 0.0;
						      noAffectVersion = 0.0;
						      bugWithProblem = 0.0;
                  closedAndLinkedBugs = 0.0;
                  for ( j = 0; j < bugs.size(); j++) {
                     if ( closedBugs.get(bugs.get(j)) != null && !(closedBugsAndAV.get(bugs.get(j)) != null &&
                            closedBugsAndAV.get(bugs.get(j)) == closedBugs.get(bugs.get(j)) &&
                            closedBugs.get(bugs.get(j)) == closedBugsAndCreation.get(bugs.get(j)))) {
                        closedAndLinkedBugs++;
                        if (closedBugsAndAV.get(bugs.get(j)) == null) {
                           noAffectVersion++;
                           bugWithProblem++;
                        }
                        else if (closedBugsAndAV.get(bugs.get(j)) > closedBugsAndCreation.get(bugs.get(j))){
                           hasAffectVersionAfterCreation++;
                           bugWithProblem++;
                        }
                     }
                  }

               		write = token[0] + "," + closedAndLinkedBugs.toString() + ","
                          + noAffectVersion.toString() + ",";
                  Double percent = noAffectVersion/closedAndLinkedBugs;
               		write += percent.toString()+ "," + hasAffectVersionAfterCreation.toString() + ",";
                  percent = hasAffectVersionAfterCreation/closedAndLinkedBugs;
               		write += percent.toString() + "," + bugWithProblem.toString() +",";
                  percent = bugWithProblem/closedAndLinkedBugs;
               		write += percent.toString() + "\n";
               		fileWriter.append(write);
                  System.out.println(write + "\n" + token[0]);
               }
            }
         } catch (Exception e) {
            System.out.println("Error in csv writer");
            e.printStackTrace();
         } finally {
            try {
               fileWriter.flush();
               fileWriter.close();
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
