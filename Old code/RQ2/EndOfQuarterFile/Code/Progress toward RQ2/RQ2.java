import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Hashtable;
import java.util.Set;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

public class RQ2 {
   public static int nextIndex;
   public static Hashtable<LocalDateTime, Integer> releases;

   public static void addRelease(String strDate) {
      LocalDate date = LocalDate.parse(strDate);
      LocalDateTime dateTime = date.atStartOfDay();
      releases.put(dateTime, nextIndex);
      nextIndex++;
      return;
   }

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

   public static Integer findExactRelease(String strDate) {
      LocalDate date = LocalDate.parse(strDate);
      LocalDateTime dateTime = date.atStartOfDay();
      Set<LocalDateTime> keys = releases.keySet();
      int i = -1;
      for (LocalDateTime key : keys) {
         if (dateTime.isEqual(key))
            i = releases.get(key);
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

   public static void getReleases(String projName) throws IOException, JSONException  {
         Integer i;
         String url = "https://issues.apache.org/jira/rest/api/2/project/" + projName;
         JSONObject json = readJsonFromUrl(url);
         JSONArray versions = json.getJSONArray("versions");
         for (i = 0; i < versions.length(); i++ ) {
            if(versions.getJSONObject(i).has("releaseDate"))
               addRelease(versions.getJSONObject(i).get("releaseDate").toString());
         }
         return;
   }

   public static void getBugInfoToCSV(String projName) throws IOException, JSONException {
      FileWriter fileWriter = null;
      String fileName = projName + "BugsInfo.csv";
      Integer j = 0, i = 0, total = 1, k = 0;
      try {
         fileWriter = new FileWriter(fileName);
         fileWriter.append("Key,Ticket Creation Date,Resolved Date,Recorded AV"
                            + ",Assumed AV,Recorded AV with Missing Dates");
         fileWriter.append("\n");
         //This URL gets a lits of all apache project keys
         do {
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
               Integer lowestAV;
               String recordedAV = "|";
               String assumedAV = "|";
               String missingDate = "|";
               fileWriter.append(issues.getJSONObject(i%1000).get("key").toString());
               fileWriter.append(",");
               Integer created = findRelease(issues.getJSONObject(i%1000)
                         .getJSONObject("fields").get("created").toString().substring(0,20));
               fileWriter.append(created.toString());
               fileWriter.append(",");
               Integer resolved = findRelease(issues.getJSONObject(i%1000)
                         .getJSONObject("fields").get("resolutiondate").toString().substring(0,20));
               fileWriter.append(resolved.toString());
               JSONArray versions = issues.getJSONObject(i%1000).getJSONObject("fields")
                                           .getJSONArray("versions");
               lowestAV = created;
               for ( k = 0; k < versions.length(); k++) {
                  if (versions.getJSONObject(k).has("releaseDate")) {
                      Integer x = findExactRelease(versions.getJSONObject(k)
                                    .get("releaseDate").toString());
                      recordedAV += x.toString() + "|";
                      if ( lowestAV > x)
                         lowestAV = x;
                  }
                  else
                     missingDate += versions.getJSONObject(k).get("name") + "|";
               }
               for ( lowestAV = lowestAV; lowestAV <= resolved; lowestAV++)
                  assumedAV += lowestAV.toString() + "|";
               fileWriter.append(",");
               fileWriter.append(recordedAV);
               fileWriter.append(",");
               fileWriter.append(assumedAV);
               fileWriter.append(",");
               fileWriter.append(missingDate);
							 fileWriter.append("\n");
            }
         } while (i < total);
         return;
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

   public static void main(String[] args) throws IOException, JSONException {
      releases = new Hashtable<LocalDateTime, Integer>();
      nextIndex = 1;
      String projectName = "PLUTO";
      getReleases(projectName);
      Set<LocalDateTime> keys = releases.keySet();
      for (LocalDateTime key : keys) {
         System.out.println("Key: " + key + "  Value: " + releases.get(key));
      }
      getBugInfoToCSV(projectName);

   }


}
