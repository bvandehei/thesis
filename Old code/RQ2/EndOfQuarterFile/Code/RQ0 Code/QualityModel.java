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
import java.util.List;
import java.util.Hashtable;
import java.util.Set;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

public class QualityModel {
   public static int nextIndex;
   public static Double totalReleases;
   public static Double releasesWithOutDates;
   public static Hashtable<LocalDateTime, Integer> releases;
   public static ArrayList<String> AVNamesWithOutDates;
   public static Double hasAffectVersionAfterFix;
   public static Double hasAffectVersionWithMissingDate;
   public static Double hasAffectVersionWithWeirdName;
   public static Double bugWithProblem;
   public static Double missingCreationOrResolutionDate;
   public static ArrayList<String> weirdNames;

   //This list was created manually by looking generating a list of all AV with
   // missing release date and their names, then searching through which ones
   // seemed like actual releases and which ones seemed like terms for something else (the weird name ones)
   public static void createWeirdNameList() {
      weirdNames = new ArrayList<String>();
      weirdNames.add("experimental-dev");
      weirdNames.add("TBD");
      weirdNames.add("Future");
      weirdNames.add("Adobe Flex SDK Previous");
      weirdNames.add("Adobe Flex SDK Next");
      weirdNames.add("Apache Flex Next");
      weirdNames.add("backlog");
      weirdNames.add("Master");
      weirdNames.add("master");
      weirdNames.add("any");
      weirdNames.add("Backlog");
      weirdNames.add("trunk");
      weirdNames.add("current (nightly)");
      weirdNames.add("unspecified");
      weirdNames.add("future (enh)");
      weirdNames.add("nightly");
      weirdNames.add("Current (Nightly)");
      weirdNames.add("Not Applicable");
      weirdNames.add("Not applicable");
      weirdNames.add("V.Next");
      weirdNames.add("next");
      weirdNames.add("NONE");
      weirdNames.add("Undefined future");
      weirdNames.add("Nightly Builds");
      weirdNames.add("Nightly Build");
      weirdNames.add("later");
      weirdNames.add("trunk-win");
      weirdNames.add("Append Branch");
      weirdNames.add("Product Backlog");
      weirdNames.add("Trunk");
      weirdNames.add("current (nightly)");
      weirdNames.add("all");
      weirdNames.add("Current");
      weirdNames.add("waiting-for-feedback");
      weirdNames.add("wontfix-candidate");
      weirdNames.add("Documentation Deficit");
      weirdNames.add("Issues to be reviewed for 3.x");
      weirdNames.add("Next");
      weirdNames.add("oya");
      weirdNames.add("Live Documentation");
      weirdNames.add("site");
      weirdNames.add("Nightly");
      weirdNames.add("from/to");
      weirdNames.add("FUTURE");
      weirdNames.add("---");
      weirdNames.add("NIGHTLY");
      weirdNames.add("sometime");
      weirdNames.add("Docs");
      weirdNames.add("Nightly build (please specify the date)");
   }

  //Adds a release to the hashtable
   public static void addRelease(String strDate) {
      LocalDate date = LocalDate.parse(strDate);
      LocalDateTime dateTime = date.atStartOfDay();
      releases.put(dateTime, nextIndex);
      nextIndex++;
      return;
   }

   // Finds the release corresponding to a creation or resolution date
   public static Integer findRelease(String strDate) {
      LocalDateTime dateTime = LocalDateTime.parse(strDate);
      Set<LocalDateTime> keys = releases.keySet();
      int i = 1;
      for (LocalDateTime key : keys) {
         if (dateTime.isAfter(key) && (releases.get(key) + 1) > i)
            i = releases.get(key) + 1;
      }
      return i;
   }

   //Finds the exact release for affect versions
   public static Integer findExactRelease(String strDate) {
      LocalDate date = LocalDate.parse(strDate);
      LocalDateTime dateTime = date.atStartOfDay();
      Set<LocalDateTime> keys = releases.keySet();
      int i = -1;
      for (LocalDateTime key : keys) {
         if (dateTime.isEqual(key))
            i = releases.get(key);
      }
      if ( i == -1) {
         System.out.println("Did not find exact release for date " + strDate);
         System.exit(-1);
      }
      return i;
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

   //Fills the hash table with release dates and corresponding indexes
   //Ignores releases with missing dates
   public static void getReleases(String projName) throws IOException, JSONException  {
         Integer i;
         String url = "https://issues.apache.org/jira/rest/api/2/project/" + projName;
         JSONObject json = readJsonFromUrl(url);
         JSONArray versions = json.getJSONArray("versions");

         for (i = 0; i < versions.length(); i++ ) {
            totalReleases++;
            if(versions.getJSONObject(i).has("releaseDate"))
               addRelease(versions.getJSONObject(i).get("releaseDate").toString());
            else {
               releasesWithOutDates++;
            }
         }
         return;
   }


   public static void calculateBugInfo(String projName) throws IOException, JSONException {
      Integer j = 0, i = 0, total = 1, k = 0;
      //Get JSON API for closed bugs w/ AV in the project
      do {
         //Only gets a max of 1000 at a time, so must do this multiple times if bugs >1000
         j = i + 1000;
         String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
                + projName + "%22AND%22issueType%22=%22Bug%22AND%22status%22=%22closed%22"
                + "AND%22affectedVersion%22IS%20NOT%20NULL%20ORDER%20BY%20%22resolutiondate"
                + "%22ASC&fields=key,resolutiondate,versions,created&startAt="
                + i.toString() + "&maxResults=" + j.toString();
         JSONObject json = readJsonFromUrl(url);
         JSONArray issues = json.getJSONArray("issues");
         total = json.getInt("total");
         for ( i = i; i < total && i < j; i++) {
            //Iterate through each bug
            Integer problem = 0, problemHasAffectVersionAfterFix = 0,
                    problemHasAffectVersionWithMissingDate = 0, problemHasAffectVersionWithWeirdName = 0;
            //check each bug has a creation/resolution date
            if (issues.getJSONObject(i%1000).getJSONObject("fields").has("created") &&
                issues.getJSONObject(i%1000).getJSONObject("fields").has("resolutiondate") &&
                issues.getJSONObject(i%1000).getJSONObject("fields").get("created").toString() != "null" &&
                issues.getJSONObject(i%1000).getJSONObject("fields").get("resolutiondate").toString() != "null") {
               //Get creation and resolution date corresponding release indexes
               Integer created = findRelease(issues.getJSONObject(i%1000)
                         .getJSONObject("fields").get("created").toString().substring(0,20));
               Integer resolved = findRelease(issues.getJSONObject(i%1000)
                         .getJSONObject("fields").get("resolutiondate").toString().substring(0,20));

                //Get list of AV recorded for the bug
               JSONArray versions = issues.getJSONObject(i%1000).getJSONObject("fields")
                         .getJSONArray("versions");
               for ( k = 0; k < versions.length(); k++) {
                  //Check if that AV has a release date
                  if (versions.getJSONObject(k).has("releaseDate")) {
                     Integer x = findExactRelease(versions.getJSONObject(k)
                         .get("releaseDate").toString());
                     //If release date exists, check if its after resolutiondate
                     if ( x > resolved) {
                        //If so mark a problem
                        //These are both set to 1 so if multiple AV for the same
                        // bug have a problem, the bug is not counted as
                        // problematic more than once
                        problem = 1;
                        problemHasAffectVersionAfterFix = 1;
                     }
                  }
                  //If there is no release date, there is a problem
                  else {
                     String name = versions.getJSONObject(k).get("name").toString();
                     problem = 1;
                     //If the name of the AV is in weird list, then its a weird name problem
                     if (weirdNames.contains(name)) {
                        problemHasAffectVersionWithWeirdName = 1;
                     }
                    //If the AV name is not in weird list, then just missing date problem
                     else {
                        problemHasAffectVersionWithMissingDate = 1;
                        if(!AVNamesWithOutDates.contains(name))
                           AVNamesWithOutDates.add(name);
                     }
                  }
               }
            }
            //If the ceation or resolution date was missing, mark problematic
            else {
               problem = 1;
               missingCreationOrResolutionDate++;
            }
            //add the problems to the total counts for the project
            bugWithProblem += problem;
            hasAffectVersionAfterFix += problemHasAffectVersionAfterFix;
            hasAffectVersionWithMissingDate += problemHasAffectVersionWithMissingDate;
            hasAffectVersionWithWeirdName +=problemHasAffectVersionWithWeirdName;
         }
      } while (i < total);
      return;
   }

   public static void main(String[] args) throws IOException, JSONException {
      FileWriter fileWriter = null;
      Integer i, j;
      String str;
      createWeirdNameList();
      try {
         //Name of CSV for output
         fileWriter = new FileWriter("QualityModel.csv");
         //Header for CSV
         fileWriter.append("Key,Closed Bugs,Closed Bugs w/o Affect Version,Percent w/o AV,"
                         + "Total Releases,Releases without Release Dates,Percent Releases w/o Dates,"
                         + "Num Bugs w/ AV after Fix,Percent Bugs w/ AV after Fix (out of Bugs w/ AV),"
                         + "Num Bugs w/ AV w/o Release Dates,Percent Bugs w/ AV w/o Release Date (out of Bugs w/ AV),"
                         + "Num Bugs w/ AV w/ Weird Names, Percent Bugs w/ AV w/ Weird Names (out of Bugs w/ AV),"
                         + "Num Bugs Missing Creation/Resolution Date,Percent Bugs Missing Creation/Resolution Date(out of Bugs w/ AV),"
                         + "Total Bugs w/ AV and a Problem,Percent Closed Bugs w/ AV and a Problem(out of Bugs w/ AV),"
                         + "Percent Closed Bugs w/ Problem Including Missing AV(out of Closed Bugs)");
         fileWriter.append("\n");
         //This URL gets a lits of all apache project keys
         String url = "https://issues.apache.org/jira/rest/api/2/project";
         JSONArray json = readJsonArrayFromUrl(url);
         for( i = 0; i < json.length(); i++) {
            //reinitialize all variables for each project
            releases = new Hashtable<LocalDateTime, Integer>();
            nextIndex = 1;
            totalReleases = 0.0;
            releasesWithOutDates = 0.0;
            AVNamesWithOutDates = new ArrayList<String>();
            hasAffectVersionAfterFix = 0.0;
            hasAffectVersionWithMissingDate = 0.0;
            hasAffectVersionWithWeirdName = 0.0;
            bugWithProblem = 0.0;
            missingCreationOrResolutionDate = 0.0;
            //Iterate through the list of apache project keys
            String key = json.getJSONObject(i).get("key").toString();
            System.out.println(key);
            //For each key, get number of issues that are closed bugs
            //Filter just keys so less info is retrieved
            String url2 = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
                           + key + "%22AND%22issueType%22=%22Bug%22AND%22status%22=%22closed%22&fields=key";
            JSONObject json2 = readJsonFromUrl(url2);
            Double totalBugs = json2.getDouble("total");
            if ( totalBugs >= 100) {
               //For each key, get number of issues that are closed bugs and have an affect version
               //Filter just keys so less info is retrieved
               String url3 = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
                           + key +"%22AND%22issueType%22=%22Bug%22AND%22status%22=%22closed%22AND%22affectedVersion%22IS%20NOT%20NULL&fields=key";
               JSONObject json3 = readJsonFromUrl(url3);
               Double aVersion = json3.getDouble("total");
               //Get the releases for the project
               getReleases(key);
               //Get info on the affect versions on the bugs for each key
               calculateBugInfo(key);
               //creates a csv that is ordered by key, I ordered by percent w/ AV after the program finished running
               fileWriter.append(key);
               fileWriter.append(",");
               fileWriter.append(totalBugs.toString());
               fileWriter.append(",");
               Double missingAV = totalBugs - aVersion;
               fileWriter.append(missingAV.toString());
               fileWriter.append(",");
               Double percent = missingAV/totalBugs;
               fileWriter.append(percent.toString());

               fileWriter.append(",");
               fileWriter.append(totalReleases.toString());
               fileWriter.append(",");
               fileWriter.append(releasesWithOutDates.toString());
               fileWriter.append(",");
               percent = releasesWithOutDates/totalReleases;
               fileWriter.append(percent.toString());

               fileWriter.append(",");
               fileWriter.append(hasAffectVersionAfterFix.toString());
               fileWriter.append(",");
               percent = hasAffectVersionAfterFix/aVersion;
               fileWriter.append(percent.toString());

               fileWriter.append(",");
               fileWriter.append(hasAffectVersionWithMissingDate.toString());
               fileWriter.append(",");
               percent = hasAffectVersionWithMissingDate/aVersion;
               fileWriter.append(percent.toString());

               //Uncomment to get names of AV w/ missing release dates
               /* fileWriter.append(",");
               str = "|";
               for (j = 0; j < AVNamesWithOutDates.size(); j++) {
                  str += AVNamesWithOutDates.get(j) + "|";
               }
						   fileWriter.append(str); */

               fileWriter.append(",");
               fileWriter.append(hasAffectVersionWithWeirdName.toString());
               fileWriter.append(",");
               percent = hasAffectVersionWithWeirdName/aVersion;
               fileWriter.append(percent.toString());

               fileWriter.append(",");
               fileWriter.append(missingCreationOrResolutionDate.toString());
               fileWriter.append(",");
               percent = missingCreationOrResolutionDate/aVersion;
               fileWriter.append(percent.toString());

               fileWriter.append(",");
						   fileWriter.append(bugWithProblem.toString());
               fileWriter.append(",");
               percent = bugWithProblem/aVersion;
               fileWriter.append(percent.toString());
               fileWriter.append(",");
               percent = (bugWithProblem + (totalBugs - aVersion))/totalBugs;
               fileWriter.append(percent.toString());

               fileWriter.append("\n");
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
}
