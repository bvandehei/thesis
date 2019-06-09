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

public class ComputeMetrics {

   public static void main(String[] args) throws IOException {

      String project = args[0];
      String git = args[1];
      System.out.println(args[0] + " " + args[1]);
      String csvSplitBy = ",";
      ArrayList<String> fixCommitFiles = new ArrayList<String>();

      try (BufferedReader br = new BufferedReader(new FileReader("./InputFiles/FixCommitFiles/"+project+"BugInfo.csv"))) {
         String linefromcsv;
         while ( (linefromcsv = br.readLine()) != null) {
            String[] token = linefromcsv.split(csvSplitBy);
            if (!fixCommitFiles.contains(token[4]))
               fixCommitFiles.add(token[4]);
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
      try (BufferedReader br2 = new BufferedReader(new FileReader("./InputFiles/VersionCommitFiles/"+project+"VersionCommitFile.csv"))) {
         String linefromcsv2;
         FileWriter fileWriter = null;
         try {
				    fileWriter = new FileWriter(project+"Metrics.csv");
            fileWriter.append("Version,File Name\n");
            br2.readLine();
            String startDate = "1990-01-01T00:00";
            while ( (linefromcsv2 = br2.readLine()) != null) {
               String [] token2 = linefromcsv2.split(csvSplitBy);
               String versionName = token2[1];
               String versionRelease = token2[2];
               String lastCommit = token2[3];
               String command = "git --git-dir ./GitClones/" + git + "/.git " +
                       "--work-tree /" +git+ "/ checkout " + lastCommit;
               Process proc = Runtime.getRuntime().exec(command);
               proc.waitFor();

               command = "git --git-dir ./GitClones/" + git + "/.git " +
                "ls-tree --full-tree -r --name-only HEAD";
               proc = Runtime.getRuntime().exec(command);
               BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
				       String line = "";
               ArrayList<String> files = new ArrayList<String>();
				       while((line = reader.readLine()) != null) {
                  files.add(line);
               }
				       for ( int i = 0; i < files.size(); i++) {
                  String filename = files.get(i);
                  fileWriter.append(versionName + "," + filename + "\n");
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
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
}
