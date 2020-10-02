
import java.io.*;
import java.net.*;
 
public class serverThread extends Thread {
    private Socket client;
    
    BufferedReader inFromClient;
    DataOutputStream outToClient;


    public serverThread(Socket client)
    {
        this.client = client;

        try{
            inFromClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
            outToClient = new DataOutputStream(client.getOutputStream());
        }catch(IOException e){
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

        isValidMethod(requestParts[0]);

        String version = requestParts[2].split("/")[1];
        if(!version.equals("1.0")){
            sendErrorCode(505);
            return;
        }
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

        //Check if one of the 3valid commands
        if(!method.equals("GET") && !method.equals("POST") && !method.equals("HEAD"))
        {
            sendErrorCode(501);
            return false;
        }

        return true;

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
        String ErrorMessage="";
        switch(code){
            case 200:
                ErrorMessage="OK";
                break;
            case 304:
                ErrorMessage="Not Modified";
                break;
            case 400:
                ErrorMessage="Bad Request";
                System.out.println("fuck you");
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
            System.out.printf("Error sending response to client");
        }
        close();
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
