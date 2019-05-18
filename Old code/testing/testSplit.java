public class testSplit { 
    public static void main(String args[]) 
    { 
        String str = "KEY,NAME,,,,,,,,,"; 
        String[] arrOfStr = str.split(","); 
  
        for (String a : arrOfStr) 
            System.out.println(a); 
    } 
}
