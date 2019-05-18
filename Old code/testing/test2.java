import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.lang.InterruptedException;

public class test2 {

    public static void main(String[] args) throws IOException, InterruptedException {
       String[] token = {"https://github.com/apache/ace.git"};
       Integer i = 0;
                     String command = "git clone " + token[i];
                     Process proc = Runtime.getRuntime().exec(command);
                     proc.waitFor();  
                     String[] token2 = token[i].split("/");
                     String path =  token2[token2.length - 1].substring(0, token2[token2.length - 1].length() - 4);
                     command = "git --git-dir ./" + path +
                      "/.git log --branches --grep=ACE-457 --pretty=format:%ad --date=iso-strict -1";
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
