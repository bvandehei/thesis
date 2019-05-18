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

public class JsonJiraTicketReader {

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
      String csvFile = "OriginalDatasets/jira/httpclient_classification_vs_type.csv";
      String line = "";
      String cvsSplitBy = ",";
      FileWriter fileWriter = null;
      Integer comments = 0, i = 0;
      try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
         try {
            fileWriter = new FileWriter("output2.csv");
            br.readLine();
            fileWriter.append("id,clssified,type,title,creation time," +
                      "resolved time,priority,description,comments");
            fileWriter.append("\n");
            while ((line = br.readLine()) != null ) {
               // use comma as separator
               String[] ticketLine = line.split(cvsSplitBy);
               String url = "https://issues.apache.org/jira/rest/api/2/issue/" + ticketLine[0];
               JSONObject json = readJsonFromUrl(url);
               fileWriter.append(ticketLine[0]);
               fileWriter.append(",");
               fileWriter.append(ticketLine[1]);
               fileWriter.append(",");
               fileWriter.append(ticketLine[2]);
               fileWriter.append(",");
               fileWriter.append("\"" + json.getJSONObject("fields")
                                      .get("summary").toString() + "\"" );
               fileWriter.append(",");
               fileWriter.append(json.getJSONObject("fields")
                                      .get("created").toString());
               fileWriter.append(",");
               fileWriter.append(json.getJSONObject("fields")
                                      .get("resolutiondate").toString());
               fileWriter.append(",");
               fileWriter.append(json.getJSONObject("fields").getJSONObject("priority")
                                      .get("name").toString());
               fileWriter.append(",");
               fileWriter.append("\"" + json.getJSONObject("fields")
                                      .get("description").toString() + "\"");
               comments = json.getJSONObject("fields")
                              .getJSONObject("comment").getJSONArray("comments").length();
               for ( i = 0; i < comments; i++) {
                  fileWriter.append(",");
                  fileWriter.append("\"" + json.getJSONObject("fields")
                                        .getJSONObject("comment").getJSONArray("comments")
                                        .getJSONObject(i).get("body").toString() + "\"");
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
