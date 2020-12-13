import java.io.*;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.HashMap;
import java.lang.*;
import java.text.ParseException;

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
    String lastSeen;

    public serverThread(Socket client) {
        this.client = client;

        try {
            client.setSoTimeout(5000);
        } catch (SocketException s) {
            sendErrorCode(408);
        }

        // Define global variable -> going to be used for communication w/ client later.
        try {
            inFromClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
            outToClient = new DataOutputStream(client.getOutputStream());
        } catch (IOException e) {
            System.out.println(e.getStackTrace());
            sendErrorCode(500);
        }

    }

    public void run() {
        String request;
        String[] requestParts;

        try {
            this.client.setSoTimeout(5000); // wait for 5 sec
            request = inFromClient.readLine();
        } catch (SocketTimeoutException e) {
            try {
                byte[] byteMessage = "HTTP/1.0 408 Request Timeout".getBytes();
                outToClient.write(byteMessage);
                close();
                return;
            } catch (IOException io) {
                sendErrorCode(500);
                return;
            }
        } catch (IOException io) {
            sendErrorCode(500);
            return;
        }

        if (request == null) {
            sendErrorCode(400);
            return;
        }

        StringBuffer stringBuffer = new StringBuffer("");
        // for reading one line
        String line = null;
        // keep reading till readLine returns null
        try {
            while ((line = inFromClient.readLine()) != null) {
                // keep appending last line read to buffer
                String delim = "\r\n";

                // helps format for the params.
                if (line.equals("")) {
                    delim = "";
                }
                stringBuffer.append(line + delim);
            }
        } catch (IOException e) {
            System.out.println("");
        }

        requestParts = request.split(" ");

        // If the request does not contain all relevant parts, 400 Bad Request
        if (requestParts.length != 3) {
            sendErrorCode(400);
            return;
        }

        // each part of the request:
        String method = requestParts[0];
        String file = requestParts[1];

        if (file.equals("/"))
            file = "./index.html";
        else
            file = "." + file;

        //* FINDS COOKIE in header
        String CookieVal = "";
        String[] postContent = stringBuffer.toString().split("\r\n");
        if (postContent.length == 4) {
            if (postContent[3].contains("Cookie")) {

                // value of Cookie in header:
                CookieVal = postContent[3].split(":")[1].trim();

                // Validates Cookie -> if valid -> change value in index_seen.html -> set file
                // to index_seen.html
                if (isValidCookie(CookieVal)) {
                    setLastSeen(CookieVal.split("=")[1]);
                    file = "./index_seen.html";
                } else {
                    sendErrorCode(500);
                    return;
                }
            }
        }

        //*Checks to see if request type is valid
        if (!isValidMethod(requestParts[0]))
            return;

        System.out.println(request);
        System.out.println(stringBuffer.toString());

        //* Checks to see if file exists -> handles the request 
        if (isValidFile(file))
            handleRequest(method, file, 200);

        return;

    }

    public boolean isValidCookie(String CookieVal) {

        boolean returnVal = false;
        try {
            String decoded = URLDecoder.decode(CookieVal, "UTF-8");
            if (decoded.split("=")[0].equals("lasttime"))
                if (isValidDateFormat(decoded.split("=")[1]))
                        returnVal = true;
                    else {
                        sendErrorCode(403);
                        returnVal = false;
                    }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            sendErrorCode(500);
            returnVal = false;
        }

        return returnVal;

    }

    /*
     * @param value = DECODED date taken from the Cookie header 
     * Checks to see if time is formated in hh-mm-ss if so, return true else, return false
     */
    public boolean isValidDateFormat(String value) {
        System.out.println(value);
        Date date = null;
        Date now = new Date();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            date = sdf.parse(value);
            System.out.println(sdf.format(date));
            if (!value.equals(sdf.format(date))) {
                date = null;
            }
            if(!sdf.parse(sdf.format(now)).after(sdf.parse(value)))
                date = null;
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        return date != null;
    }

    /*
     * TODO
     * 
     * @param value = DECODED time taken from the Cookie header 
     * Checks to see if time is formated in hh-mm-ss ALSO Checks to see if value parameter is less than current time.
     * 
     * if both are NOT TRUE, return false else return true.
     */
    public boolean isValidTime(String value) {
        value = value.trim();
        System.out.println(value);
        Date date = null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            date = sdf.parse(value);
            System.out.println(sdf.format(date));
            if (!value.equals(sdf.format(date))) {
                date = null;
            }
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        return date != null;
    }

    /*
     * 
     * @param value = DECODED date and time taken from the Cookie header 
     * should replace the value in index_seen.html (%YEAR-%MONTH-%DAY %HOUR-%MINUTE-%SECOND) with the appropriate values.
     * 
     * return: VOID
     * 
    */
    public void setLastSeen(String value) {
        try {
            String decoded = URLDecoder.decode(value, "UTF-8");
            String fullYear = decoded.split(" ")[0];           
            String fullTime = decoded.split(" ")[1];
           
            lastSeen = fullYear + " " + fullTime;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    // *Check each type of method
    public boolean isValidMethod(String method) {
        switch (method) {
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

    // *Check if the file is valid. 403 for unreadable, 404 for not found
    public boolean isValidFile(String file) {
        File f = new File(file);
        Path p = Paths.get(file);
        if (f.exists()) {
            if (Files.isReadable(p)) {
                return true;
            } else {
                sendErrorCode(403);
                return false;
            }
        } else {
            sendErrorCode(404);
            return false;
        }
    }

    /*
        * These are the error codes:
        *200    OK
        *304    Not Modified
        *400    Bad Request
        *403    Forbidden
        *404    Not Found
        *408    Request Timeout
        *500    Internal Server Error
        *501    Not Implemented
        *503    Service Unavailable
        *505    HTTP Version Not Supported
    
        *Function should take in a codet and print its corresponding message:
        *FORMAT:  
        *   - String message = String.format("HTTP/1.0 %s %s", code, ErrorMessage);

    */
    public void sendErrorCode(int code) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'");
        String next_year = dateFormat.format(new Date(System.currentTimeMillis() + 13500000000L));
        String ErrorMessage = "";
        switch (code) {
            case 200:
                ErrorMessage = "OK";
                break;
            case 304:
                ErrorMessage = "Not Modified" + "\r\n" + "Expires: " + next_year;
                break;
            case 400:
                ErrorMessage = "Bad Request";
                break;
            case 403:
                ErrorMessage = "Forbidden";
                break;
            case 404:
                ErrorMessage = "Not Found";
                break;
            case 408:
                ErrorMessage = "Request Timeout";
                break;
            case 500:
                ErrorMessage = "Internal Server Error";
                break;
            case 501:
                ErrorMessage = "Not Implemented";
                break;
            case 503:
                ErrorMessage = "Service Unavailable";
                break;
            case 505:
                ErrorMessage = "HTTP Version Not Supported";
                break;
        }

        // Format the header message.
        String message = String.format("HTTP/1.0 %s %s", code, ErrorMessage);
        try {
            System.out.println(message);
            outToClient.writeBytes(message);
            outToClient.flush();

        } catch (IOException e) {
            System.out.printf("Error sending response to CLIENT");
        }
        close();
    }

    //* HANDLES 200 and 304 REQUESTS ONLY -> Gets called when all checks were validated
    public void handleRequest(String method, String file, int code) {
        String message = "";
        if (code == 200)
            message = String.format("HTTP/1.0 %s %s", 200, "OK");
        if (code == 304)
            message = String.format("HTTP/1.0 %s %s", 304, "Not Modified");

        try {
            outToClient.writeBytes(message + "\r\n" + createHeader(file) + "\r\n" + "\r\n");
            outToClient.flush();

        } catch (IOException e) {
            System.out.printf("Error sending response to client");
        }

        // "file" is a string from the client request.
        try {
            File new_file = new File(file);
            byte[] fileContent = Files.readAllBytes(new_file.toPath());
            if(file.equals("./index_seen.html"))
            {
                String fileContentsStr = new String(fileContent, StandardCharsets.UTF_8);
                fileContentsStr = fileContentsStr.replace("%YEAR-%MONTH-%DAY %HOUR-%MINUTE-%SECOND", lastSeen);
                fileContent = fileContentsStr.getBytes();
            }
            outToClient.write(fileContent);
            System.out.println("wrote to client");
            outToClient.flush();
        } catch (IOException e) {
            System.out.println("it broke");
        }
        close();
    }

    /*
        * Assembles the header
        *FORMAT FOR REFERENCE | REQUEST EXAMPLE: GET resouces/bitcoin.pdf HTTP/1.0
        *    HTTP/1.0 200 OK[CRLF]
        *    Content-Type: application/pdf[CRLF]
        *    Content-Length: 184292[CRLF]
        *    Last-Modified: Tue, 14 Jul 2015 14:13:49 GMT[CRLF]
        *    Content-Encoding: identity[CRLF]
        *    Allow: GET, HEAD, POST[CRLF]
        *    Expires: Fri, 01 Oct 2021 03:44:00 GMT[CRLF]
        *    [CRLF]
    */

    private String createHeader(String fileName) {

        String header = "", extension, MIME = "";

        extension = fileName.substring(fileName.lastIndexOf(".") + 1);

        // Mime Types:
        if (extension.equals("pdf")) {
            MIME = "application/pdf";
        } else if (extension.equals("gzip")) {
            MIME = "application/x-gzip";
        } else if (extension.equals("zip")) {
            MIME = "application/zip";
        } else if (extension.equals("txt")) {
            MIME = "text/plain";
        } else if (extension.equals("html")) {
            MIME = "text/html";
        } else if (extension.equals("gif")) {
            MIME = "image/gif";
        } else if (extension.equals("jpg")) {
            MIME = "image/jpeg";
        } else if (extension.equals("png")) {
            MIME = "image/png";
        } else {
            MIME = "application/octet-stream";
        }
        header += "Content-Type: " + MIME + "\r\n";

        File new_file = new File(fileName);
        long fileSize = new_file.length();
        header += "Content-Length: " + fileSize + "\r\n";

        SimpleDateFormat dateSimpleFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'");
        dateSimpleFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

        header += "Last-Modified: " + dateSimpleFormat.format(new_file.lastModified()) + "\r\n";
        header += "Content-Encoding: identity" + "\r\n";
        header += "Allow: GET, POST, HEAD" + "\r\n";

        long currentTime = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, 24);
        header += "Expires: " + dateSimpleFormat.format(cal.getTime()) + "\r\n";

        SimpleDateFormat cookieDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedCurrentDate = cookieDateFormat.format(new Date(currentTime));
        header += "Set-Cookie: lasttime=" + URLEncoder.encode(formattedCurrentDate, StandardCharsets.UTF_8);

        return header;
    }

    /*
     * Void method for organization Just closes the socket.
     */
    public void close() {
        try {
            client.close();
        } catch (IOException e) {
            System.out.println(e.getStackTrace());
        }
    }
}
