import java.util.*;
import java.io.*; 
import java.net.*; 

class PartialHTTP1Server{
    public static void main(String[]args) throws IOException{
        if (args.length != 1) {
            System.err.println("Invalid port number!");
            System.exit(1);
        }
         
        //passing port number as integer, "portNumber"
        int portNumber = Integer.parseInt(args[0]);
         
        try (
            //Create serverSocket w/ port args[0]
            ServerSocket serverSocket = new ServerSocket(Integer.parseInt(args[0]));

            //Accept clientSocket on connection. 
            Socket clientSocket = serverSocket.accept();     

        ) {
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}