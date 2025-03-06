// PACKAGE
package backend;

// IMPORTS
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

// MAIN CLASS
public class Main {

    // VARIABLES
    static MessageList messageList = new MessageList(); // Saves all the messages to this object.
    static Map<String, String> loggedUsers = new HashMap<>(); // saves users who connected to the server: <IP ADDRESS: USERNAME>
    static Gson gson = new Gson(); // object used for serializing / deserializing JSON

    public static void main(String[] args) throws Exception {

        // == SERVER SETUP ===========================================

        // use the LAN ip (so that people can connect)
        String localHostAddress = getLocalIPAddress();

        // port can be constant, but had it as its own thing just in case
        int serverPort = 8000;

        // start server on the computer's LAN address on the port specified above.
        HttpServer server = HttpServer.create(new InetSocketAddress(localHostAddress, serverPort), 0);

        // == ADDING APIS ===========================================

        // get messages (make /messages)
        server.createContext("/chat", new MessageHandler());

        // goes to actual webpage
        server.createContext("/chatterbox", new WebpageHandler());

        // gives username
        server.createContext("/username", new UsernameGiver());

        // gets static resources
        server.createContext("/resources", new StaticFileHandler());

        // == IDK WHAT THIS IS =================================================================

        server.setExecutor(null);

        // == START THE SERVER ==================================================================
        System.out.print("Starting simple http server... ");
        server.start();
        System.out.println("STARTED!");
        System.out.println("http://" + localHostAddress + ":" + serverPort + "/chatterbox\n" +
                           "(IF FROM CHROMEBOOK, USE IP ADDRESS)\n----------------------------------------");
    }

    // Not too familiar with each object, chatGPT wrote this
    // commented it though
    public static String getLocalIPAddress() {
        try {

            // get all network interfaces
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();

            // go through each network interface (so long as they exist)
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();

                // get the addresses for each network interface
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();

                // go through them
                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();

                    // ignore any loopback addresses
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        // first valid address should be the IP were looking for (we also want it to be in ipv4)
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;  // Return null if no IP address is found
    }

    // handler for "/username" --> gives each user a username on first time login (or when requested).
    static class UsernameGiver implements HttpHandler {

        // *handle --> what to do when called.
        @Override
        public void handle(HttpExchange http) throws IOException {

            String username;
            String clientIp = http.getRemoteAddress().getAddress().getHostAddress();

            // checks if the user had logged in from this machine before
            if (!loggedUsers.containsKey(clientIp)) {
                // gives them a random username if they're new
                username = Message.generateUsername();
                loggedUsers.put(clientIp, username);
            } else {
                // locates and gives back old username if have been on the server before
                username = loggedUsers.get(clientIp);
            }

            // send username to client
            http.sendResponseHeaders(200, username.getBytes().length);
            OutputStream os = http.getResponseBody();
            os.write(username.getBytes());
            os.close();
            System.out.println("[" + username + " logged in.]");
        }
    }

    // handler for "/chatterbox"
    static class WebpageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange http) throws IOException {

            // technically shouldn't get post anytime, but did if just in case
            if ("GET".equals(http.getRequestMethod())) {

                // get byte file path for the html
                String path = "src/frontend/chatterbox.html";

                // encode it in bytew
                byte[] htmlBytes = Files.readAllBytes(Paths.get(path));
                http.getResponseHeaders().set("Content-Type", "text/html");

                // send it to client (code 200 means that all is good!)
                http.sendResponseHeaders(200, htmlBytes.length);
                OutputStream os = http.getResponseBody();
                os.write(htmlBytes);
                os.close();
            }
        }
    }


    // handler for static resources "/resources"
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange http) throws IOException {

            // get filepath
            String filePathString = "src/frontend" + http.getRequestURI().getPath(); // Adjust the resource path
            Path filePath = Paths.get(filePathString);

            // check existence
            if (Files.exists(filePath)) {
                String mimeType = getMimeType(filePath.toString());
                byte[] fileBytes = Files.readAllBytes(filePath);

                // Set the correct content type for the static file
                http.getResponseHeaders().set("Content-Type", mimeType);

                // Send the response headers and the file content
                http.sendResponseHeaders(200, fileBytes.length);
                OutputStream os = http.getResponseBody();
                os.write(fileBytes);
                os.close();
            } else {
                // If the file doesn't exist, return a 404 response
                http.sendResponseHeaders(404, 0);
                OutputStream os = http.getResponseBody();
                os.close();
            }
        }

        // helper method to get MIME type based on file extension --> chatGPT wrote this
        private String getMimeType(String fileName) {

            if (fileName.endsWith(".png")) {
                return "image/png";
            } else if (fileName.endsWith(".ttf")) {
                return "font/ttf";
            } else {
                return "application/octet-stream"; // Default MIME type
            }
        }

    }

    // handler for "/chat"
    static class MessageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange http) throws IOException {

            String responseItem;

            // if the user is trying to read from the server
            if ("GET".equals(http.getRequestMethod())) {

                // give them the whole conversation
                responseItem = gson.toJson(messageList); // gson.toJson() is just a utility method to serialize an object


            }
            // if the user is SENDING a message
            // else if  POST   - get message from body / add to list
            else if ("POST".equals(http.getRequestMethod())) {

                // get WHO is sending the message by IP
                String posterUsername = loggedUsers.get(http.getRemoteAddress().getAddress().getHostAddress());

                // get message from sent data, add to list
                String messageContent = new BufferedReader(new InputStreamReader(http.getRequestBody()))
                        .lines().collect(Collectors.joining("\n"));

                // get the current time formatted
                String formattedTime = Message.getFormattedTime();

//                if (messageContent.equals("user-disconnect")) {
//
//                }

                // create message object
                Message sentMessage = new Message(posterUsername, messageContent, Message.generateID(), formattedTime);

                // add message to message log
                messageList.messages.add(sentMessage);

                // return the completed message object to the client
                responseItem = gson.toJson(sentMessage);

                // print the message in the terminal
                System.out.println(Message.formatMessage(messageList.messages.getLast()));


            } else {
                System.out.println(http.getResponseCode());
                responseItem = "SOMETHING WENT WRONG";
            }
            // else return 400 BAD REQUEST


            /*

            servers for dummies:

            many different server architectures, CRUD is one of them)

            CRUD (CREATE.READ.UPDATE.DELETE.)

            GET -- get resource list or resource by id
            POST -- add new resource and generate id
            PUT -- edit (or add) resource with specific id
            DELETE -- delete with id

            // how to access a server/ message

            [server]/mychat/messages[/message_id]

             +++ GET /mychat/messages -- return list
            GET /mychat/messages/<id>  -- return one message
            +++ POST /mychat/messages  -- add message AND generate id and return message with id
            PUT /mychat/messages/<id>  -- send edited message
            DELETE /mychat/messages/<id>  -- delete

            http codes 200 - ok / 4xx bad request (client problem) / 5xx server error  (3xx redirect)



             */

            // send stuff to client

            String response = responseItem;// "METHOD: " + http.getRequestMethod() + " --> " + responseItem;
            http.sendResponseHeaders(200, response.length());
            OutputStream os = http.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }


    }

}