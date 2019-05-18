import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.lang.InterruptedException;

public class RunShellCommandFromJava {

    public static void main(String[] args) throws IOException, InterruptedException {

        String command = "git clone https://github.com/apache/ace.git";

        Process proc = Runtime.getRuntime().exec(command);
        proc.waitFor();   
        command = "git --git-dir ./ace/.git log --branches --grep=ACE-457 --pretty=format:%ad --date=iso-strict -1";
                // Read the output
        proc = Runtime.getRuntime().exec(command);
        BufferedReader reader =
              new BufferedReader(new InputStreamReader(proc.getInputStream()));
              
        String line = "";
        while((line = reader.readLine()) != null) {
            System.out.print(line + "\n");
        }   
        proc.waitFor();
    }
}
