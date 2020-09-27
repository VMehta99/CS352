import java.io.*; 
import java.net.*; 

public class client { 

    public static void main(String[]args) throws Exception {
        String sentence;
        String modifiedSentence;

        BufferedReader inFromUser = 
            new BufferedReader(new InputStreamReader(System.in));

        System.out.println("From client: " + inFromUser.readLine());

        Socket clientSocket = new Socket("localhost",4200);

        DataOutputStream outToServer = 
            new DataOutputStream(clientSocket.getOutputStream());

        BufferedReader inFromServer = 
            new BufferedReader(new 
            InputStreamReader(clientSocket.getInputStream()));
        
        sentence = inFromUser.readLine();
        outToServer.writeBytes(sentence);

        modifiedSentence = inFromServer.readLine();

        System.out.println("SERVER: "+ modifiedSentence);

        clientSocket.close();
    }

}