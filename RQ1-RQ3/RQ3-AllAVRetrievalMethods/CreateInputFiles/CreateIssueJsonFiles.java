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

public class CreateIssueJsonFiles {


   public static void main(String[] args) throws IOException, JSONException {
      String csvFile = "Projects.csv";
      String cvsSplitBy = ",";
      try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
         String linefromcsv, newline, write;
         while ( (linefromcsv = br.readLine()) != null) {
            String[] token = linefromcsv.split(cvsSplitBy);
            String inputFile = "CSVFiles/" + token[0] + "BugInfo.csv";
            for (int j = 1; j < token.length; j++) {
               try (BufferedReader br2 = new BufferedReader(new FileReader(inputFile))) {
                  FileWriter fileWriter = null;
				          try {
                     fileWriter = null;
                     String[] token2 = token[j].split("/");
                     String path =  token2[token2.length - 1].substring(0, token2[token2.length - 1].length() - 4);
                     String outname = path + "_issue_list.json";
				             //Name of CSV for output
				             fileWriter = new FileWriter(outname);
                     String linefrombug;
                     fileWriter.append("{");
                     br2.readLine();
                     Integer temp = 0;
                     while ((linefrombug = br2.readLine()) != null) {
                         String[] tokenBug = linefrombug.split(cvsSplitBy);
                         if ( tokenBug[5].equals(path) && temp != 0) {
                            fileWriter.append(", \"" + tokenBug[0] +"\": {\"creationdate\": \""+
                               tokenBug[2]+"\", \"resolutiondate\": \""+ tokenBug[1]+"\", \"hash\": \""
                               + tokenBug[4]+"\", \"commitdate\": \" "+ tokenBug[3]+" \"}");
                         }
                         else if ( tokenBug[5].equals(path) && temp == 0) {
                            fileWriter.append("\"" + tokenBug[0] +"\": {\"creationdate\": \""+
                               tokenBug[2]+"\", \"resolutiondate\": \""+ tokenBug[1]+"\", \"hash\": \""
                               + tokenBug[4]+"\", \"commitdate\": \" "+ tokenBug[3]+" \"}");
                            temp++;
                         }
                     }
                     fileWriter.append("}");
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
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
}
