
import java.io.*;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class serverThread extends Thread {
    private Socket client;
    
    BufferedReader inFromClient;
    DataOutputStream outToClient;

    public serverThread(Socket client)
    {
        this.client = client;

        try{
            client.setSoTimeout(5000);
        }
        catch (SocketException s){
            sendErrorCode(408);
        }

        try{
            inFromClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
            outToClient = new DataOutputStream(client.getOutputStream());
        }
        catch(IOException e){
            System.out.println(e.getStackTrace());
            sendErrorCode(500);
        }
        
    }

 
    public void run() {
        String request;
        String[] requestParts;

        try {
            request = inFromClient.readLine();
        } catch (IOException e) {
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

        String method = requestParts[0];
        String file = requestParts[1];
        String modified = ""; 

        if(!isValidMethod(requestParts[0]))
            return;

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

        if(!modified.trim().equals("")){
            modified = modified.substring(modified.indexOf(":") + 2);

            try{

                modifiedDate = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'").parse(modified);
                long requestTime = modifiedDate.getTime();

                File new_file = new File("." + file);
                long fileTime = new_file.lastModified();
                
                if(requestTime>fileTime && !method.equals("HEAD"))
                    is304=true;
            }
            catch(Exception e){
                is304 =false;
                System.out.println("yer");
            }
            
        }
        if(is304)
            sendErrorCode(304);

        //LAST CHECK -- DO NOT MOVE THIS LOL
        if(isValidFile(file))
            handleRequest(method,file,200);
        
        

    }


    //REWRITE
    public boolean isValidMethod(String method){
        String[] commandList = {"PUT", "DELETE", "LINK", "UNLINK", "GET", "HEAD", "POST"};

        boolean found = false;
        for(int i = 0; i < commandList.length; i++)
            if(method.equals(commandList[i]))
                found = true;
        
        //Check if the command is a valid HTTP/1.0 command 
        if(!found)
        {
            sendErrorCode(400);
            return false;
        }

        if(!method.equals("GET") && !method.equals("POST") && !method.equals("HEAD"))
        {
            sendErrorCode(501);
            return false;
        }

        return true;

    }

    public boolean isValidFile(String file){
        File f = new File("."+file);
        Path p = Paths.get("."+file);
            if(f.exists()) { 
                if(Files.isReadable(p)){
                    // sendErrorCode(200,file);
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
        304    Not Modified
        400    Bad Request
        403    Forbidden
        404    Not Found
        408    Request Timeout
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
            case 408:
                ErrorMessage="Request Timeout";
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

        String message = String.format("HTTP/1.0 %s %s", code, ErrorMessage);

       

        try{
            System.out.println(message);
            outToClient.writeBytes(message);
            outToClient.flush();

        }catch(IOException e){
            System.out.printf("Error sending response to CLIENT");
        }
        close();
    }



    //HANDLE REQUEST ONLY WHEN 200 CODE
    public void handleRequest(String method,String file, int code)
    {   
        String message="";

        if(code ==200)
            message = String.format("HTTP/1.0 %s %s", 200, "OK");
        if(code == 304)
            message = String.format("HTTP/1.0 %s %s", 304, "Not Modified");

        // System.out.println(createHeader(file));
       

        try{
            // System.out.println(message);
            outToClient.writeBytes(message + "\r\n" + createHeader(file) + "\r\n" + "\r\n") ;
            outToClient.flush();

        }catch(IOException e){
            System.out.printf("Error sending response to client");
        }

        // "file" is a string from the client request. 
        if((method.equals("GET") || method.equals("POST")) && code==200){
            try{
                File new_file = new File("." + file);
                byte[] fileContent = Files.readAllBytes(new_file.toPath());
                outToClient.write(fileContent);
                outToClient.flush();
            }
            catch(IOException e){
                System.out.println("FUCK");
            }


        }
        close();
    }



    private String createHeader(String fileName){

        String header = "", extension, MIME = "";
        
        extension = fileName.substring(fileName.lastIndexOf(".") + 1);
        
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
        else if (extension.equals("html")){
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
        header += "Content-Type: " + MIME + "\r\n";


        File new_file = new File("." + fileName);
        long fileSize = new_file.length();
        header += "Content-Length: " + fileSize + "\r\n";

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
            System.out.println("Error closing socket:\n" + e.getStackTrace());
        }
    }
}
