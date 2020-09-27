import java.util.*;
import java.io.*; 
import java.net.*; 
import java.util.concurrent.*;

public class PartialHTTP1Server extends Thread{
    private Socket socket;
 
    public PartialHTTP1Server(Socket socket) {
        this.socket = socket;
    }
 
    //TESTER: REVERSES TEXT
    public void run() {
        try {
            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
 
            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);
 
 
            String text;
 
            //Reverses text until exit - FOR TESTING ONLY
            do {
                text = reader.readLine();
                String reverseText = new StringBuilder(text).reverse().toString();
                writer.println("Server: " + reverseText);
 
            } while (!text.equals("exit"));
 
            socket.close();
        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) return;
 
        int port = Integer.parseInt(args[0]);
 
        try (ServerSocket serverSocket = new ServerSocket(port)) {
 
            System.out.println("Server is listening on port " + port);
 
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected");
                
                //CREATE THREAD HERE:
               PartialHTTP1Server serverThread = new PartialHTTP1Server(socket);
                serverThread.start();
                // serverThread.join();
                // try{
                //     serverThread.join();
                // }
                // catch(InterruptedException e){
                //  System.out.println("Thread exception: " + e.getMessage());

                // }
            }
 
        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}

