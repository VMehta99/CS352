### CS352
Internet Technology Fall 2020

# WebServer (project 3)
*Currently implemented: (11/18/2020)*

- All functionality from Project 1

## Structure:

* HttpServerTester.jar
* PartialHTTP1Server.java
* *cgi_bin* -> Test cases
* *doc_root* -> *resources*, index.html

# Project 1
*Currently implemented: (09/21/2020)*

- Takes port number through args[0]
- Creates server using ```new ServerSocket```
- Accepts server call using ```.accept()```
- Throws error if port is in use/invalid port

*Needs to be implemented:*
- hand off the created Socket to a Thread that handles all the client's communication
- limit number of simultanious connections to 50
- handle GET, POST and HEAD commands
- support MIME types

## How to run
1. ```javac PartialHTTP1Server```
2. ```java PartialHTTP1Server 3456```
3. ```java -jar HTTPServerTester.jar localhost 3456```

## Structure:

* HttpServerTester.jar
* *doc_root* -> PartialHTTP1Server.java

