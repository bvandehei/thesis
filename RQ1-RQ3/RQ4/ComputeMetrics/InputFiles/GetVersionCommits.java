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
import java.time.temporal.ChronoUnit;

public class GetVersionCommits {

   public static void main(String[] args) throws IOException {

      String csvFile = "Projects.csv";
      String csvSplitBy = ",";
      try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
         String linefromcsv;
         while ( (linefromcsv = br.readLine()) != null) {
            String[] token = linefromcsv.split(csvSplitBy);
            FileWriter fileWriter = null;
            try (BufferedReader br2 = new BufferedReader(new FileReader("VersionInfoFiles/"+token[0]+"VersionInfo.csv"))) {
               String linefromcsv2;
               try {
				          fileWriter = new FileWriter(token[0]+"VersionCommitFile.csv");
                  fileWriter.append("Index,Version Name,Release Date,Last Commit\n");
                  String[] token2 = token[1].split("/");
                  String path =  token2[token2.length - 1].substring(0, token2[token2.length - 1].length() - 4);
                  br2.readLine();
                  while ( (linefromcsv2 = br2.readLine()) != null) {
                      String [] token3 = linefromcsv2.split(csvSplitBy);
                      String command = "git --git-dir ../GitClones/" + path + "/.git " +
                       "log --until " + token3[3] + " --pretty=format:%H -1";
                      Process proc = Runtime.getRuntime().exec(command);
                      BufferedReader reader =
				               new BufferedReader(new InputStreamReader(proc.getInputStream()));
                      String hash = reader.readLine();
                      proc.waitFor();
                      fileWriter.append(token3[0] + "," + token3[2] + "," + token3[3] + "," + hash + "\n");
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
      } catch (IOException e) {
         e.printStackTrace();
      }

   }

}
