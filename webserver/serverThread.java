import java.io.*;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.HashMap;
import java.lang.*;



// TODO: 
// 1. Implement 405, 403, 204 errors for cgi files
// 2. Fix payload for testcase 26-28


public class serverThread extends Thread {
    private Socket client;
    
    BufferedReader inFromClient;
    DataOutputStream outToClient;

    // Global Variables -> Enviornment variables
    String PARAMS;
    String CONTENT_LENGTH;
    String FROM;
    String USER_AGENT;
    byte[] cgiOutput;
    

    public serverThread(Socket client)
    {
        this.client = client;

        try{
            client.setSoTimeout(5000);
        }
        catch (SocketException s){
            sendErrorCode(408);
        }

        //Define global variable -> going to be used for communication w/ client later.
        try{
            inFromClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
            outToClient = new DataOutputStream(client.getOutputStream());
        }
        catch(IOException e){
            System.out.println(e.getStackTrace());
            sendErrorCode(500);
        }
        
    }
 
    public void run(){
        String request;
        String[] requestParts;



        try {
            this.client.setSoTimeout(5000); // wait for 5 sec
            request = inFromClient.readLine();
        } catch (SocketTimeoutException e){
            try{
                byte[] byteMessage = "HTTP/1.0 408 Request Timeout".getBytes();
			    outToClient.write(byteMessage);
                close();
                return;
            }
            catch (IOException io) {
                sendErrorCode(500);
                return;
            }
        } catch (IOException io) {
            sendErrorCode(500);
            return;
        } 

        if(request==null){
            sendErrorCode(400);
            return;
        }

        //Extract each part of the request
        requestParts = request.split(" ");
        
        //If the request does not contain all relevant parts, 400 Bad Request
        if(requestParts.length != 3){
            sendErrorCode(400);
            return;
        }

        //each part of the request:
        String method = requestParts[0];
        String file = requestParts[1];
        String modified = ""; 
        
        // Post handler -> helps define error codes.
        if(method.equals("POST")){
            if(!handlePost())
                return;
        }

        


        if(!isValidMethod(requestParts[0]))
            return;

        //verify the version: if not 1.0, 505 
        String version = requestParts[2].split("/")[1];
        if(!version.equals("1.0")){
            sendErrorCode(505);
            return;
        }
        
        try{
            if(inFromClient.ready())
            modified = inFromClient.readLine();
        }catch(IOException e){
            sendErrorCode(500);
            return;
        }
        boolean is304 = false;
        Date modifiedDate = null;


        //This segment is used to compare the IF-Modified-By field in the request. 
        if(!modified.trim().equals("")){
            modified = modified.substring(modified.indexOf(":") + 2);
            try{
                modifiedDate = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'").parse(modified);
                long requestTime = modifiedDate.getTime();

                File new_file = new File("." + file);
                long fileTime = new_file.lastModified();
                
                //convert times to long and compare. 
                if(requestTime>fileTime && !method.equals("HEAD"))
                    is304=true;
            }
            catch(Exception e){
                is304 =false;
            }
        }
        if(is304)
            sendErrorCode(304);

        //check extension for cgi
        String extension = file.substring(file.lastIndexOf(".") + 1); 
        if(method.equals("POST")){
            // If its a post and not .cgi -> return 405.
            if(!(extension.equals("cgi"))){
                sendErrorCode(405);
                return;
            }
        }
        //LAST CHECK
        if(isValidFile(file))
            handleRequest(method,file,200);
    }

    

    //Check each type of method
    public boolean isValidMethod(String method){
        switch(method){
            case "PUT":
                sendErrorCode(501);
                return false;
            case "DELETE":
                sendErrorCode(501);
                return false;
            case "LINK":
                sendErrorCode(501);
                return false;
            case "UNLINK":
                sendErrorCode(501);
                return false;
            case "GET":
                return true;
            case "HEAD":
                return true;
            case "POST":
                return true;
            default:
                sendErrorCode(400);
                return false;
        }
    }

    // Check if the file is valid. 403 for unreadable, 404 for not found
    public boolean isValidFile(String file){

        String extension = file.substring(file.lastIndexOf(".") + 1); 


        File f = new File("."+file);
        Path p = Paths.get("."+file);
            if(f.exists()) { 
                if(Files.isReadable(p)){
                    
                    // Used for checking if the CGI can execute:
                    if(!f.canExecute() && extension.equals("cgi")){
                        sendErrorCode(403);
                        return false;
                    }
                
                    return true;
                }
                else{
                    sendErrorCode(403);
                    return false;
                }
            }
            else{
                sendErrorCode(404);
                return false;
            }
    }

    /*
    These are the error codes:
        200    OK
        204    No Content
        304    Not Modified
        400    Bad Request
        403    Forbidden
        404    Not Found
        405    Method Now Allowed
        408    Request Timeout
        411    Length Required
        500    Internal Server Error
        501    Not Implemented
        503    Service Unavailable
        505    HTTP Version Not Supported
    
        Function should take in a codet and print its corresponding message:
        FORMAT:  
           - String message = String.format("HTTP/1.0 %s %s", code, ErrorMessage);
    */
    public void sendErrorCode(int code)
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'");
        String next_year = dateFormat.format(new Date(System.currentTimeMillis() + 13500000000L));
        String ErrorMessage="";
        switch(code){
            case 200:
                ErrorMessage="OK";
                break;
            case 204:
                ErrorMessage="No Content";
                break;
            case 304:
                ErrorMessage="Not Modified" + "\r\n" + "Expires: " + next_year ;
                break;
            case 400:
                ErrorMessage="Bad Request";
                break;
            case 403:
                ErrorMessage="Forbidden";
                break;
            case 404:
                ErrorMessage="Not Found";
                break;
            case 405:
                ErrorMessage="Method Not Allowed";
                break;
            case 408:
                ErrorMessage="Request Timeout";
                break;
            case 411:
                ErrorMessage="Length Required";
                break;
            case 500:
                ErrorMessage="Internal Server Error";
                break;
            case 501:
                ErrorMessage="Not Implemented";
                break;
            case 503:
                ErrorMessage="Service Unavailable";
                break;
            case 505:
                ErrorMessage="HTTP Version Not Supported";
                break;
        }
        
        //Format the header message.
        String message = String.format("HTTP/1.0 %s %s", code, ErrorMessage);
        try{
            System.out.println(message);
            outToClient.writeBytes(message);
            outToClient.flush();

        }catch(IOException e){
            System.out.println(e.getMessage());
        }
        close();
    }

     public boolean verifyPayload(String file){
        // Parameter for verifying payload output
        String parameterString = PARAMS;  
     

        // Runs CGI File -> gets output and verifies output. 
        try{
            ProcessBuilder cgiScriptString = new ProcessBuilder("." + file);
            Process cgiProcess = cgiScriptString.start();
            byte[] parameterByte = parameterString.getBytes(); 
            OutputStream cgiOutputStream = cgiProcess.getOutputStream();
            cgiOutputStream.write(parameterByte);
            cgiOutputStream.close();
            InputStream cgiInputStream = cgiProcess.getInputStream();
            InputStreamReader cgiInputStreamReader = new InputStreamReader(cgiInputStream);
            BufferedReader cgiBufferedReader = new BufferedReader(cgiInputStreamReader);
            StringBuffer stringBuffer = new StringBuffer();
            String cgiString;

            // read inputstream and save as string
            while((cgiString = cgiBufferedReader.readLine()) != null){  
                stringBuffer.append(cgiString);
            }
            cgiString = stringBuffer.toString();

            // CHECK: If the payload is null  -> false 
            if(cgiString.length()==0 || cgiString==null)
                return false;

            cgiInputStream.close();
        }
        catch(IOException e){
            System.out.println("");
        }
       
        
        return true;

    }



    //HANDLES 200 and 304 REQUESTS ONLY -> Gets called when all checks were validated.
    public void handleRequest(String method,String file, int code){   

        if(method.equals("POST") && !verifyPayload(file)){
            sendErrorCode(204);
            return;
        }

        String message="";
        if(code ==200)
            message = String.format("HTTP/1.0 %s %s", 200, "OK");
        if(code == 304)
            message = String.format("HTTP/1.0 %s %s", 304, "Not Modified");

        if(method.equals("HEAD")){
            try{
                outToClient.writeBytes(message + "\r\n" + createHeader(file, method) + "\r\n" + "\r\n");
                outToClient.flush();

            }catch(IOException e){
                System.out.printf("Error sending response to client");
            }
        }


        if((method.equals("GET") || method.equals("POST")) && code==200){
            try{
                //check extension for cgi
                String extension = file.substring(file.lastIndexOf(".") + 1); 
            
                if(method.equals("POST")){
                    ProcessBuilder cgiScriptString = new ProcessBuilder("." + file);
                    Map<String, String> env = cgiScriptString.environment();      
                    
                    // set enviornment variables
                    try {
                        env.put("CONTENT_LENGTH", CONTENT_LENGTH);
                        env.put("SCRIPT_NAME", file);
                        env.put("HTTP_FROM", FROM);
                        env.put("HTTP_USER_AGENT", USER_AGENT);
                    } catch (Exception e) {
                        sendErrorCode(500);
                    }

                    Process cgiProcess = cgiScriptString.start();

                    //INSERT PARAMATER
                    String parameterString = PARAMS;   

                    //convert parameters to bytes to write             
                    byte[] parameterByte = parameterString.getBytes(); 

                    //put paramater into cgi and run
                    OutputStream cgiOutputStream = cgiProcess.getOutputStream();
                    cgiOutputStream.write(parameterByte);
                    cgiOutputStream.close();

                    //obtain inputStream of cgi so it can be read with bufferedReader
                    InputStream cgiInputStream = cgiProcess.getInputStream();
                    InputStreamReader cgiInputStreamReader = new InputStreamReader(cgiInputStream);
                    BufferedReader cgiBufferedReader = new BufferedReader(cgiInputStreamReader);
                    StringBuffer stringBuffer = new StringBuffer();
                    String cgiString;
                    
                    // read inputstream and save as string
                    while((cgiString = cgiBufferedReader.readLine()) != null){  
                        stringBuffer.append(cgiString);
                        stringBuffer.append(System.getProperty("line.separator"));
                    }
                    cgiString = stringBuffer.toString();
                    System.out.println(cgiString);
                    cgiInputStream.close();
                    
                    //convert output from string to bytes and write header
                    cgiOutput = cgiString.getBytes(); 
                    try{
                        outToClient.writeBytes(message + "\r\n" + createHeader(file, method) + "\r\n" + "\r\n");
                        outToClient.flush();
            
                    }catch(IOException e){
                        System.out.printf("Error sending response to client");
                    }

                    //write payload
                    outToClient.write(cgiOutput);
                    outToClient.flush();
                }
                else if(method.equals("GET") || method.equals("HEAD")){
                    //write header
                    try{
                        outToClient.writeBytes(message + "\r\n" + createHeader(file, method) + "\r\n" + "\r\n");
                        outToClient.flush();
            
                    }catch(IOException e){
                        System.out.printf("Error sending response to client");
                    }
                    //write payload
                    File new_file = new File("." + file);
                    byte[] fileContent = Files.readAllBytes(new_file.toPath());
                    outToClient.write(fileContent);
                    outToClient.flush();
                }
            }
            catch(IOException e){
                System.out.println("it broke");
            }


        }
        close();
    }


    // Return false if bad post request -> No Content-Length or No Content-Type
    public boolean handlePost(){
        StringBuffer stringBuffer = new StringBuffer("");
        // for reading one line
        String line = null;
        // keep reading till readLine returns null
        try{
            while ((line = inFromClient.readLine()) != null) {
                // keep appending last line read to buffer
                String delim = "\r\n";

                // helps format for the params. 
                if(line.equals("")){
                    delim = "";
                }
                stringBuffer.append(line + delim);
            }
        }
        catch(IOException e){
            System.out.println("");
        }


    // Splits stringBuffer by \r\n.
        String [] postContent = stringBuffer.toString().split("\r\n");
        String Params;

   
        
    //    EDGE CASES:
    //      1. If no params
    //      2. If no content length -> 411 error
    //      3. if no Content Type -> 500 error
        
        String From = postContent[0].split(":")[1].trim();
        String UserAgent = postContent[1].split(":")[1].trim();
        

        // Edge 3
        String ContentType = postContent[2];
        if(!ContentType.contains("Content-Type")){
            sendErrorCode(500);
            return false;
        }
        ContentType = postContent[2].split(":")[1].trim();


        // Edge 2
        String ContentLength = postContent[3];
        if(!ContentLength.contains("Content-Length")){
            sendErrorCode(411);
            return false;
        }
        ContentLength = postContent[3].split(":")[1].trim();
        
        // Edge 1
        if(postContent.length==4)
            Params = "";
        else
            Params = postContent[4];
   
        PARAMS = decode(Params).trim();
        CONTENT_LENGTH = ContentLength.trim();
        FROM = From.trim();
        USER_AGENT = UserAgent.trim();

        return true;
    }


    private String decode(String param){
        String decoded = "";
        boolean skip = false;

        for (int i = 0; i < param.length(); i++) {
            // skips the next character, as to not overlap
            if(skip){
                skip = false;
                continue;
            }
            if(i == param.length() - 1){
                decoded += param.charAt(i);
                return decoded;
            }
            // for each char in the payload, add '!' before each reserved character
            if(param.charAt(i) == '!'){
                decoded += param.charAt(i+1);
                skip = true;
            }
            // if normal character, just append to output
            else{
                decoded += param.charAt(i);
            }
        }

        return decoded;
    }


    // Assembles the header
    /*
        FORMAT FOR REFERENCE | REQUEST EXAMPLE: GET resouces/bitcoin.pdf HTTP/1.0
            HTTP/1.0 200 OK[CRLF]
            Content-Type: application/pdf[CRLF]
            Content-Length: 184292[CRLF]
            Last-Modified: Tue, 14 Jul 2015 14:13:49 GMT[CRLF]
            Content-Encoding: identity[CRLF]
            Allow: GET, HEAD, POST[CRLF]
            Expires: Fri, 01 Oct 2021 03:44:00 GMT[CRLF]
            [CRLF]
    */

    private String createHeader(String fileName, String method){
        

        String header = "", extension, MIME = "";
        
        extension = fileName.substring(fileName.lastIndexOf(".") + 1);
        
        //Mime Types:
        if (extension.equals("pdf")){
            MIME = "application/pdf";
        }  
        else if (extension.equals("gzip")){
            MIME = "application/x-gzip";
        }
        else if (extension.equals("zip")){
            MIME = "application/zip";
        }
        else if (extension.equals("txt")){
            MIME = "text/plain";
        }
        else if (extension.equals("html") || extension.equals("cgi")){
            MIME = "text/html";
        }
        else if (extension.equals("gif")){
            MIME = "image/gif";
        }
        else if (extension.equals("jpg")){
            MIME = "image/jpeg";
        }
        else if (extension.equals("png")){
            MIME = "image/png";
        }
        else {
            MIME = "application/octet-stream";
        }
        File new_file = new File("." + fileName);
        long fileSize = new_file.length();
        if(method.equals("GET") || method.equals("HEAD")){
            header += "Content-Type: " + MIME + "\r\n";
            header += "Content-Length: " + fileSize + "\r\n";
        }

        if(method.equals("POST")){
            header += "Content-Length: " + Integer.toString(cgiOutput.length) + "\r\n";
            System.out.println("contentlength: " + cgiOutput.length);
            header += "Content-Type: " + MIME + "\r\n";
        }

        SimpleDateFormat dateSimpleFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'");
        dateSimpleFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

        header += "Last-Modified: " + dateSimpleFormat.format(new_file.lastModified()) + "\r\n";
        header += "Content-Encoding: identity" + "\r\n";
        header += "Allow: GET, POST, HEAD" + "\r\n";

        long currentTime = System.currentTimeMillis();;
        Date currentDate = new Date(currentTime + 31540000000L);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, 24);
        header += "Expires: " + dateSimpleFormat.format(cal.getTime());

        return header;
    }

    /*
        Void method for organization
        Just closes the socket. 
    */
    public void close()
    {
        try {
            client.close();
        } catch (IOException e) {
            System.out.println(e.getStackTrace());
        }
    }
}