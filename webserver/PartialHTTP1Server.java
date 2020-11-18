import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.RejectedExecutionHandler;


class PartialHTTP1Server {
    public static void main(String[]args) throws Exception{ 
        
        if (args.length < 1) return;
        // serverThread.initErrorCodes();
        int port = Integer.parseInt(args[0]);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            RejectedExecutionHandler reject_handler = new MyRejectedExecutionHandler();
            ThreadPoolExecutor executor = new ThreadPoolExecutor(5, 50, 5, TimeUnit.SECONDS, new SynchronousQueue<>(), reject_handler);
            //executor.setRejectedExecutionHandler(reject_handler);
        
            System.out.println("Listening on port " + port);

            while(true){
                Socket socket = serverSocket.accept();
                System.out.println("New client connected");

                if(executor.getActiveCount() >= 50){ // if there are more than 50 threads, throw a 503 error 
                    PrintWriter printer = new PrintWriter(socket.getOutputStream(), true);
                    printer.println("HTTP/1.0 503 Service Unavailable\r\n");
                    printer.flush();
                    printer.close();
                    try{
                        socket.close();
                    }catch(Exception e){
                    }
                }
                
                serverThread t = new serverThread(socket);
                executor.execute(t);  
                //t.start();
                //t.join();
                
            }
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

    
}   
class MyRejectedExecutionHandler implements RejectedExecutionHandler{
    public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor){
    }
  }