import java.io.*;
import java.net.*;


class PartialHttp1Server {
    public static void main(String[]args) throws Exception{ 
        
        if (args.length < 1) return;
        // serverThread.initErrorCodes();
        int port = Integer.parseInt(args[0]);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
        
            System.out.println("Listening on port " + port);

            while(true){
                Socket socket = serverSocket.accept();
                System.out.println("New client connected");
                
                serverThread t = new serverThread(socket);
                t.start();
                t.join();
            }
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

    
}   