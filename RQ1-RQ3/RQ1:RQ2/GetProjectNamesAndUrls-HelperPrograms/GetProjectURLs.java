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
import java.util.concurrent.TimeUnit;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

public class GetProjectURLs {

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
      String csvFile = "ProjectsEditted.csv";
      String cvsSplitBy = ",";
      try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
         FileWriter fileWriter = null;
				 try {
				    Integer i, j = 0, k =0, p = 1;
				    String linefromcsv, newline;
				    //Name of CSV for output
				    fileWriter = new FileWriter("ProjectsAndUrls.csv");
				    //Header for CSV
				    fileWriter.append("Key,Name,URL");
				    fileWriter.append("\n");
            br.readLine();
            while ((linefromcsv = br.readLine()) != null ) {
               p=1;
               String[] token = linefromcsv.split(cvsSplitBy);
               if ((j % 10) == 0) {
                  TimeUnit.MINUTES.sleep(1);
               }
               j++;
               String url = "https://api.github.com/search/repositories?q="
                      + token[1].replaceAll(" ", "%20") + "+user:apache&per_page=100";
               newline = token[0] + "," + token[1];
               JSONObject json = readJsonFromUrl(url);
               Integer n = json.getInt("total_count");
               JSONArray urls = json.getJSONArray("items");
               for ( k = 0; k < n && k < 100; k++) {
                  newline += "," + urls.getJSONObject(k).get("clone_url").toString();
               }
               n = n - 100;
               while ( n > 0) {
                  if ((j % 10) == 0) {
                     TimeUnit.MINUTES.sleep(1);
                  }
                  j++;
                  String url2 = "https://api.github.com/search/repositories?q="
                      + token[1].replaceAll(" ", "%20") + "+user:apache&per_page=100&page=" + (++p).toString();
                  JSONObject json2 = readJsonFromUrl(url2);
                  JSONArray urls2 = json.getJSONArray("items");
                  for ( k = 0; k < n && k < 100; k++) {
                     newline += "," + urls2.getJSONObject(k).get("clone_url").toString();
                  }
                  n = n - 100;
               }
               fileWriter.append(newline);
               System.out.println(newline);
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
