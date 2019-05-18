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

public class JsonBugzillaTicketReader {

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

   public static void main(String[] args) throws IOException, JSONException {
      String csvFile = "OriginalDatasets/bugzilla/rhino_classification_vs_type.csv";
      String line = "";
      String cvsSplitBy = ",";
      FileWriter fileWriter = null;
      Integer comments = 0, i = 0;
      try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
         try {
            fileWriter = new FileWriter("output.csv");
            br.readLine();
            fileWriter.append("id,clssified,type,title,creation time," +
                      "resolved time,priority,severity,description,comments");
            fileWriter.append("\n");
            while ((line = br.readLine()) != null) {
               // use comma as separator
               String[] ticketLine = line.split(cvsSplitBy);
               String url = "https://bugzilla.mozilla.org/rest/bug/" + ticketLine[0];
               JSONObject json = readJsonFromUrl(url);
               fileWriter.append(ticketLine[0]);
               fileWriter.append(",");
               fileWriter.append(ticketLine[1]);
               fileWriter.append(",");
               fileWriter.append(ticketLine[2]);
               fileWriter.append(",");
               fileWriter.append("\"" + json.getJSONArray("bugs").getJSONObject(0)
                                      .get("summary").toString() + "\"" );
               fileWriter.append(",");
               fileWriter.append(json.getJSONArray("bugs").getJSONObject(0)
                                      .get("creation_time").toString());
               fileWriter.append(",");
               fileWriter.append(json.getJSONArray("bugs").getJSONObject(0)
                                      .get("cf_last_resolved").toString());
               fileWriter.append(",");
               fileWriter.append(json.getJSONArray("bugs").getJSONObject(0)
                                      .get("priority").toString());
               fileWriter.append(",");
               fileWriter.append(json.getJSONArray("bugs").getJSONObject(0)
                                      .get("severity").toString());

               comments = json.getJSONArray("bugs").getJSONObject(0)
                                      .getInt("comment_count");
               url = "https://bugzilla.mozilla.org/rest/bug/"
                             + ticketLine[0] + "/comment";
               json = readJsonFromUrl(url);
               for ( i = 0; i < comments; i++) {
                  fileWriter.append(",");
                  fileWriter.append("\"" + json.getJSONObject("bugs").getJSONObject(ticketLine[0])
                                        .getJSONArray("comments").getJSONObject(i)
                                        .get("text").toString() + "\"");
               }
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
