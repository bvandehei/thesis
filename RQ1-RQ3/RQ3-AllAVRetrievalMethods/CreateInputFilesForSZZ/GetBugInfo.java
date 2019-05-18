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

public class GetBugInfo {

   public static ArrayList<LocalDateTime> releases;
   public static HashMap<String, Integer> closedBugs;
   public static HashMap<String, String> closedBugsAndFix;
   public static HashMap<String, String> closedBugsAndDirectory;
   public static HashMap<String, String> closedBugsAndHash;
   public static HashMap<String, Integer> closedBugsAndAV;
   public static HashMap<String, Integer> closedBugsAndCreation;
   public static HashMap<String, String> closedBugsAndCreationDate;
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

         FileWriter fileWriter = null;
				 try {
            fileWriter = null;
            String outname = projName + "VersionInfo.csv";
				    //Name of CSV for output
				    fileWriter = new FileWriter(outname);
            fileWriter.append("Index,Version ID,Version Name,Date");
            fileWriter.append("\n");
            for ( i = 0; i < releases.size(); i++) {
               Integer index = i + 1;
               fileWriter.append(index.toString());
               fileWriter.append(",");
               fileWriter.append(releaseID.get(releases.get(i)));
               fileWriter.append(",");
               fileWriter.append(releaseNames.get(releases.get(i)));
               fileWriter.append(",");
               fileWriter.append(releases.get(i).toString());
               fileWriter.append("\n");
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
            Integer created = findRelease(issues.getJSONObject(i%1000)
                         .getJSONObject("fields").get("created").toString().substring(0,20));
            closedBugsAndCreation.put(key,created);
            String creationdate = issues.getJSONObject(i%1000)
                         .getJSONObject("fields").get("created").toString();
            creationdate = creationdate.replace("T", " ").replace(".000", " ");
            closedBugsAndCreationDate.put(key,creationdate);
            String resolution = issues.getJSONObject(i%1000)
                         .getJSONObject("fields").get("resolutiondate").toString();
            closedBugsAndResolution.put(key,resolution.replace("T", " ").replace(".000", " "));
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
      String cvsSplitBy = ",";
      Integer i, j, t;
      try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
         String linefromcsv, newline, write;
         while ( (linefromcsv = br.readLine()) != null) {
            FileWriter fileWriter = null;
				    try {
               fileWriter = null;
               String[] token = linefromcsv.split(cvsSplitBy);
               String outname = token[0] + "BugInfo.csv";
				       //Name of CSV for output
				       fileWriter = new FileWriter(outname);
				       //Header for CSV
				       fileWriter.append("Bug ID,Bug Resolution Date,Ticket Creation Date,Fix Date,Hash,Git Directory");
				       fileWriter.append("\n");

               releases = new ArrayList<LocalDateTime>();
               getReleases(token[0]);

						   closedBugs = new HashMap<String, Integer>();
						   closedBugsAndFix = new HashMap<String, String>();
						   closedBugsAndDirectory = new HashMap<String, String>();
						   closedBugsAndCreationDate = new HashMap<String, String>();
						   closedBugsAndHash = new HashMap<String, String>();
               closedBugsAndAV = new HashMap<String, Integer>();
               closedBugsAndCreation = new HashMap<String, Integer>();
               closedBugsAndResolution = new HashMap<String, String>();
               getBugInfo(token[0]);
               ArrayList<String> bugs = new ArrayList<String>(closedBugs.keySet());

               //check all repos for the bug key
               for ( i = 1; i < token.length; i++) {
                  String command = "git clone " + token[i];
                  Process proc = Runtime.getRuntime().exec(command);
                  proc.waitFor();
                  String[] token2 = token[i].split("/");
                  String path =  token2[token2.length - 1].substring(0, token2[token2.length - 1].length() - 4);
                  for ( j = 0; j < bugs.size(); j++) {
                     command = "git --git-dir ./" + path +
                     "/.git log --branches --grep="+ bugs.get(j) +" --pretty=format:%cd%H --date=iso -1";
                      // Read the output
                      proc = Runtime.getRuntime().exec(command);
                      BufferedReader reader =
                        new BufferedReader(new InputStreamReader(proc.getInputStream()));
                      String line = "";
                      if((line = reader.readLine()) != null) {
                          proc.waitFor();
                          String datePart = line.substring(0, 25);
                          String hash = line.substring(25);
                          Integer x = findRelease(datePart.substring(0,19).replace(" ", "T"));
                          if (closedBugs.get(bugs.get(j)) == null || closedBugs.get(bugs.get(j)) < x) {
                               closedBugsAndFix.put(bugs.get(j), datePart);
                               closedBugsAndHash.put(bugs.get(j), hash);
                               closedBugsAndDirectory.put(bugs.get(j), path);
                               closedBugs.put(bugs.get(j), x);
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
                          closedBugsAndAV.get(bugs.get(j)) == closedBugs.get(bugs.get(j)))) {
                         write = bugs.get(j) + "," + closedBugsAndResolution.get(bugs.get(j))
                          + "," + closedBugsAndCreationDate.get(bugs.get(j)) + "," +
                          closedBugsAndFix.get(bugs.get(j)) + "," +
                          closedBugsAndHash.get(bugs.get(j))+ "," +
                          closedBugsAndDirectory.get(bugs.get(j)) + "\n";
                         System.out.println(write);
                         fileWriter.append(write);
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
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
}
