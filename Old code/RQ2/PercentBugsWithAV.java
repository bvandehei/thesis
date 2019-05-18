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

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

public class PercentBugsWithAV {

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

   public static void main(String[] args) throws IOException, JSONException {
      FileWriter fileWriter = null;
      Integer i;
      try {
         fileWriter = new FileWriter("datasets.csv");
         fileWriter.append("key,total,Affect Version,Percent");
         fileWriter.append("\n");
         //This URL gets a lits of all apache project keys
         String url = "https://issues.apache.org/jira/rest/api/2/project";
         JSONArray json = readJsonArrayFromUrl(url);
         for( i = 0; i < json.length(); i++) {
            //Iterate through the list of apache project keys
            String key = json.getJSONObject(i).get("key").toString();
            //For each key, get number of issues that are closed bugs
            //Filter just keys so less info is retrieved
            String url2 = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
                           + key + "%22AND%22issueType%22=%22Bug%22AND%22status%22=%22closed%22&fields=key";
            JSONObject json2 = readJsonFromUrl(url2);
            Double total = json2.getDouble("total");
            //For each key, get number of issues that are closed bugs and have an affect version
            //Filter just keys so less info is retrieved
            String url3 = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
                           + key +"%22AND%22issueType%22=%22Bug%22AND%22status%22=%22closed%22AND%22affectedVersion%22IS%20NOT%20NULL&fields=key";
            JSONObject json3 = readJsonFromUrl(url3);
            Double aVersion = json3.getDouble("total");
            //creates a csv that is ordered by key, I ordered by percent after the program finished running
            fileWriter.append(key);
            fileWriter.append(",");
            fileWriter.append(total.toString());
            fileWriter.append(",");
            fileWriter.append(aVersion.toString());
            fileWriter.append(",");
            Double percent = aVersion/total;
            fileWriter.append(percent.toString());
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
   }
}
