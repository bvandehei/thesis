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

public class GetReleasesWithOutDatesInfo {
   public static int nextIndex;
   public static int totalReleases;
   public static int releasesWithOutDates;
   public static Hashtable<LocalDateTime, Integer> releases;
   public static String namesOfReleasesWithOutDates;
   public static String listOfReleasesFollowingMissingDateReleases;
   public static LocalDateTime lastDate;

   public static void addRelease(String strDate) {
      LocalDate date = LocalDate.parse(strDate);
      LocalDateTime dateTime = date.atStartOfDay();
      if(!dateTime.isAfter(lastDate)) {
         System.out.println("ERROR: Date of previous release: " + lastDate.toString()
                             + " Date of next release: " + dateTime.toString());
      }
      releases.put(dateTime, nextIndex);
      lastDate = dateTime;
      nextIndex++;
      return;
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
         lastDate = LocalDateTime.MIN;
         for (i = 0; i < versions.length(); i++ ) {
            totalReleases++;
            if(versions.getJSONObject(i).has("releaseDate"))
               addRelease(versions.getJSONObject(i).get("releaseDate").toString());
            else {
               releasesWithOutDates++;
               namesOfReleasesWithOutDates += versions.getJSONObject(i).get("name").toString() + "|";
               listOfReleasesFollowingMissingDateReleases += nextIndex + "|";
            }
         }
         return;
   }

   public static void main(String[] args) throws IOException, JSONException {

      String csvFile = "datasets.csv";
      String line = "";
      String cvsSplitBy = ",";
      FileWriter fileWriter = null;
      Integer comments = 0, i = 0;
      try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
         try {
            fileWriter = new FileWriter("ReleasesWithOutDatesInfo.csv");
            fileWriter.append("Project,Total Releases,Number Without Release Dates," +
                              ",Percent Without Dates,Names of Releases Without Dates," +
                      "List of Releases That Follow Missing Date Releases");
            fileWriter.append("\n");
            br.readLine();
            while ((line = br.readLine()) != null ) {
               // use comma as separator
               releases = new Hashtable<LocalDateTime, Integer>();
               nextIndex = 1;
               totalReleases = 0;
               releasesWithOutDates = 0;
               namesOfReleasesWithOutDates = "|";
               listOfReleasesFollowingMissingDateReleases = "|";
               String[] ticketLine = line.split(cvsSplitBy);
               System.out.println(ticketLine[0]);
               getReleases(ticketLine[0]);
               fileWriter.append(ticketLine[0] + "," + totalReleases + ","
	                + releasesWithOutDates + "," + ((double)releasesWithOutDates/(double)totalReleases)
                  + "," + namesOfReleasesWithOutDates + "," + listOfReleasesFollowingMissingDateReleases );
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
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
}
