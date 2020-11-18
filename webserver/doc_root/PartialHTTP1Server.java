
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.TimeZone;

public class PartialHTTP1Server implements Runnable{
  
   static final File WEB_ROOT = new File(".");
   static final String DEFAULT_FILE = "index.html";
   static final String FILE_NOT_FOUND = "404.html";
   static final String METHOD_NOT_SUPPORTED = "not_supported.html";
   
   public static int numConnect = 0;
   // TrueFalse mode
   static final boolean TrueFalse = true;
  
   // Client Connection via Socket Class
   private static Socket connect;
  
   public PartialHTTP1Server(Socket c) {
       connect = c;
   }
  
   public static void main(String[] args) {
       
       // port to listen to connection
        try{
            int PORT = Integer.parseInt((args[0]));
            System.out.println(PORT);

            try {
                ServerSocket serverConnect = new ServerSocket(PORT);
                System.out.println("Server started.\nListening for connections on port : " + PORT + " ...\n");
                
                // we listen until user halts server execution
                while (true) {
                       if(numConnect != 50){    //connection made here
                            PartialHTTP1Server myServer = new PartialHTTP1Server(serverConnect.accept());
                            numConnect++;
                            
                            if (TrueFalse) {
                                System.out.println("Connecton opened. (" + new Date() + ")");
                            }
                            
                            // create dedicated thread to manage the client connection
                            Thread thread = new Thread(myServer);
                            thread.start();
                        }
                        else if (numConnect == 50){
                            PrintWriter out = null;
                            out = new PrintWriter(connect.getOutputStream());
                            out.println("HTTP/1.0 503 Service Unavailable\r\n");
            
                            out.flush(); // flush character output stream buffer
                            return;
                        }
                }
                
            } 
            catch (IOException e) {
                System.err.println("Server Connection error : " + e.getMessage());
            }
        }
        catch (NumberFormatException notNumber){
            System.out.println("Not a number input");
        }
   }

   @Override 
   public void run() {
       // we manage our particular client connection
       BufferedReader in = null; PrintWriter out = null; BufferedOutputStream dataOut = null;
       String theFileWeNeededTheMost = null;
      
       try {
           // we read characters from the client via input stream on the socket
           in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
           // we get character output stream to client (for headers)
           out = new PrintWriter(connect.getOutputStream());
           // get binary output stream to client (for requested data)
           dataOut = new BufferedOutputStream(connect.getOutputStream());
          
           // get first line of the request from the client
           String input = in.readLine();
           String secondLine = in.readLine();
           // we parse the request with a string tokenizer
           StringTokenizer parse = new StringTokenizer(input);
           String method = parse.nextToken(); // we get the HTTP method of the client
           // we get file requested
           theFileWeNeededTheMost = parse.nextToken();
          
            System.out.println("THIS IS WHAT METHOD IS: " + method);
            System.out.println("This is the fileweneededthemost: " + theFileWeNeededTheMost);
            System.out.println("input is: " + input);
            System.out.println("Second line is: " + secondLine);

            

            

            //date conversion
            String pattern = ("E, dd MMM yyyy HH:mm:ss z");
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
            simpleDateFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));

            //today's date
            Calendar cal = Calendar.getInstance();
            Date today = cal.getTime();

            String date = simpleDateFormat.format(today);
            System.out.println("current date: " + date);
            
            //next year's date
            cal.add(Calendar.YEAR, 1); // to get previous year add -1
            Date nextYear = cal.getTime();

            String expireDate = simpleDateFormat.format(nextYear);
            System.out.println("one yeat later date: " + expireDate);

           // we support only GET and HEAD and POST methods, we check
           
           if (method.equals("LINK") || method.equals("UNLINK") || method.equals("DELETE") || method.equals("PUT")) {
            
               out.println("HTTP/1.0 501 Not Implemented\r\n");
          
               out.flush(); // flush character output stream buffer
               return;
                             
           } 
           else if (method.equals("GET") || method.equals("HEAD") || method.equals("POST")){
               // GET or HEAD or POST method
               System.out.println("200 ok land");
               if(input.contains("HTTP/")==false){
                    out.println("HTTP/1.0 400 Bad Request\r\n");
            
                    out.flush(); // flush character output stream buffer
                    return;
               }
               //System.out.println("contains HTTP");
               else if(input.contains("HTTP/")==true){
                    if(input.endsWith("HTTP/1.0")==false){
                        out.println("HTTP/1.0 505 HTTP Version Not Supported\r\n");
            
                        out.flush(); // flush character output stream buffer
                        return;
                    }
               }
               //System.out.println("Has HTTP/1.0 at end");

               int numSpaces = 0;
               for (int i = 0; i < input.length(); i++){
                   if (input.charAt(i) == ' '){
                        numSpaces++;
                   }
                   if (numSpaces > 2){
                        out.println("HTTP/1.0 400 Bad Request\r\n");
                
                        out.flush(); // flush character output stream buffer
                        return;
                   }
               }
               //System.out.println("correct number of spaces");
               if (theFileWeNeededTheMost.endsWith("/")) {
                   theFileWeNeededTheMost += DEFAULT_FILE;
               }
               //System.out.println("going to read fileweneedthemost");
               File file = new File(WEB_ROOT, theFileWeNeededTheMost);
               if (!(file.isFile())){
      
                    out.println("HTTP/1.0 404 Not Found\r\n");
                    
                    out.flush(); // flush character output stream buffer
                    return;
               }
              
               if(!(file.canRead())){
   
                    out.println("HTTP/1.0 403 Forbidden\r\n");
                    
                    out.flush(); // flush character output stream buffer
                    return;
               }
      
                int fileLength = (int) file.length();
                String content = getTypeOfContent(theFileWeNeededTheMost);
                System.out.println("HTTP/1.0 200 OK\r");
                System.out.println("Content-type: " + content + "\r");
                System.out.println("Content-length: " + fileLength +"\r");
              
                //last modified is file.lastmodified() [long]
                String lastModifiedDate = simpleDateFormat.format(new Date(file.lastModified()));
                System.out.println("Last-Modified: " + lastModifiedDate + "\r");
                System.out.println("Content-Encoding: " + "\r");
                System.out.println("Allow: " + "\r");

                //expire is 1 year forward from reality
                System.out.println("Expires: " + expireDate + "\r\n");
                
                

               if (method.equals("GET")){ // GET method so we return content
                   byte[] dingDangFileData = readThatDingDangFileData(file, fileLength);
                   if(secondLine.contains("If-Modified-Since: ") && secondLine.contains(" GMT")){
                    String modifyCheck = secondLine.substring(19);
                    
                    //used to compare against last modification date
                    Date modifyLimit = simpleDateFormat.parse(modifyCheck);
                    Date lastDateModified = simpleDateFormat.parse(lastModifiedDate);
                   
                        if (lastDateModified.before(modifyLimit)){
                            out.println("HTTP/1.0 304 Not Modified\r");
                            out.print("Expires: " + expireDate + "\r\n");
                            out.flush();
                            return;
                        }
                  }
                   // send HTTP Headers
                   out.println("HTTP/1.0 200 OK\r");
                   out.println("Content-Type: " + content + "\r");
                   out.println("Content-Length: " + fileLength +"\r");
                 
                   out.println("Last-Modified: " + lastModifiedDate + "\r");
                   out.println("Content-Encoding: identity\r");
                   out.println("Allow: GET, POST, HEAD\r");
                   out.print("Expires: " + expireDate + "\r\n\r\n");
                  
                   out.flush(); // flush character output stream buffer
                  
                   dataOut.write(dingDangFileData, 0, fileLength);
                   dataOut.flush();
                   return;
               }
               else if (method.equals("HEAD")){
                   //byte[] dingDangFileData = readThatDingDangFileData(file, fileLength);
                  
                   // send HTTP Headers
                   out.println("HTTP/1.0 200 OK\r");
                
                   out.println("Content-Type: " + content + "\r");
                   out.println("Content-Length: " + fileLength +"\r");
            
                   out.println("Last-Modified: " + lastModifiedDate + "\r");
                   out.println("Content-Encoding: identity\r");
                   out.println("Allow: GET, POST, HEAD\r");
                   out.print("Expires: " + expireDate + "\r\n\r\n");
                   
                   out.flush(); // flush character output stream buffer
                   return;

               }
               else if (method.equals("POST")){
                   byte[] dingDangFileData = readThatDingDangFileData(file, fileLength);
                   if(secondLine.contains("If-Modified-Since: ") && secondLine.contains(" GMT")){
                    String modifyCheck = secondLine.substring(19);
                   
                    Date modifyLimit = simpleDateFormat.parse(modifyCheck);
                    Date lastDateModified = simpleDateFormat.parse(lastModifiedDate);
          
                        if (lastDateModified.before(modifyLimit)){
                            out.println("HTTP/1.0 304 Not Modified\r");
                            out.print("Expires: " + expireDate + "\r\n");
                            out.flush();
                            return;
                        }
                  }
                   // send HTTP Headers
                   out.println("HTTP/1.0 200 OK\r");
                
                   out.println("Content-Type: " + content + "\r");
                   out.println("Content-Length: " + fileLength +"\r");
           
                   out.println("Last-Modified: " + lastModifiedDate + "\r");
                   out.println("Content-Encoding: identity\r");
                   out.println("Allow: GET, POST, HEAD\r");
                   out.print("Expires: " + expireDate + "\r\n\r\n");
                  
                   out.flush(); // flush character output stream buffer
               
                   dataOut.write(dingDangFileData, 0, fileLength);
                   dataOut.flush();
                   return;
               }              
           }
           else {
            out.println("HTTP/1.0 400 Bad Request\r\n");
          
            out.flush(); // flush character output stream buffer
            return;
           }          
       }
       catch (ParseException STOP){
           System.err.println("error");
       }
       catch (FileNotFoundException fnfe) {
            try {
                fileNotFound(out, dataOut, theFileWeNeededTheMost);
            } 
            catch (IOException ioe) {
                out.println("HTTP/1.0 404 Not Found\r\n");
                out.flush(); // flush character output stream buffer
                System.err.println("Error with file not found exception : " + ioe.getMessage());
                return;
            }
            
        } 
        catch (IOException ioe) {
            out.println("HTTP/1.0 500 Internal Server Error\r\n");
            
            out.flush(); // flush character output stream buffer
                        
            System.err.println("Server error : " + ioe);
            return;
        } 
        finally {
            try {
                in.close();
                out.close();
                dataOut.close();
                connect.close(); // we close socket connection
            }
            catch (Exception e) {
                out.println("HTTP/1.0 500 Internal Server Error\r\n");
            
                out.flush(); // flush character output stream buffer
                
                System.err.println("Error closing stream : " + e.getMessage());
                return;
            }
            
            if (TrueFalse) {
                System.out.println("Connection closed.\n");
                numConnect--;
            }
        }
      
      
   }
  
   private byte[] readThatDingDangFileData(File file, int fileLength) throws IOException {
       FileInputStream fileIn = null;
       byte[] dingDangFileData = new byte[fileLength];
      
       try {
           fileIn = new FileInputStream(file);
           fileIn.read(dingDangFileData);
       } 
       finally {
           if (fileIn != null)
               fileIn.close();
       }
      
       return dingDangFileData;
   }
  
   // return supported MIME Types
   private String getTypeOfContent(String theFileWeNeededTheMost) {
        if (theFileWeNeededTheMost.endsWith(".htm") || theFileWeNeededTheMost.endsWith(".html")){
            if(theFileWeNeededTheMost.endsWith(".html")){
                return "text/html";
            }
            
            else{
                return "text/plain";
            }
        }
        else if (theFileWeNeededTheMost.endsWith(".gif") || theFileWeNeededTheMost.endsWith(".jpeg") || theFileWeNeededTheMost.endsWith(".png")){
            if(theFileWeNeededTheMost.endsWith(".gif")){
                return "image/gif";
            }
            else if(theFileWeNeededTheMost.endsWith(".jpeg")){
                return "image/jpeg";
            }
            else{
                return "image/png";
            }
        }
        else if (theFileWeNeededTheMost.endsWith(".octet-stream") || theFileWeNeededTheMost.endsWith(".pdf") || theFileWeNeededTheMost.endsWith(".x-gzip") || theFileWeNeededTheMost.endsWith(".zip")){
            if((theFileWeNeededTheMost.endsWith(".octet-stream"))){
                return "application/octet-stream";
            }
            else if(theFileWeNeededTheMost.endsWith(".pdf")){
                return "application/pdf";
            }
            else if(theFileWeNeededTheMost.endsWith(".x-gzip")){
                return "application/x-gzip";
            }
            else{
                return "application/zip";
            }
        }
        return "application/octet-stream";
   }
  
   private void fileNotFound(PrintWriter out, OutputStream dataOut, String theFileWeNeededTheMost) throws IOException {
       System.out.println("NOT FOUND");
       //File file = new File(WEB_ROOT, FILE_NOT_FOUND);
       //int fileLength = (int) file.length();
       //String content = "text/html";
       //byte[] dingDangFileData = readThatDingDangFileData(file, fileLength);
      
       out.println("HTTP/1.0 404 File Not Found");
       out.flush(); // flush character output stream buffer
      
    //    dataOut.write(dingDangFileData, 0, fileLength);
    //    dataOut.flush();
      
       if (TrueFalse) {
           System.out.println("File " + theFileWeNeededTheMost + " not found");
       }
   }
  
}
