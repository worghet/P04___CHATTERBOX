package backend;

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

public class Main {

    static MessageList messageList = new MessageList();
    static Map<String, String> loggedUsers = new HashMap<>();
    static Gson gson = new Gson();

    public static void main(String[] args) throws Exception {

        String localHostAddress = getLocalIPAddress();
        int serverPort = 8000;

        HttpServer server = HttpServer.create(new InetSocketAddress(serverPort), 0);

        // get messages (make /messages)
        server.createContext("/chat", new MessageHandler());

        // goes to actual webpage
        server.createContext("/chatterbox", new WebpageHandler());

        // gives username
        server.createContext("/username", new UsernameGiver());

        // gets static resources
        server.createContext("/resources", new StaticFileHandler());


        // idk what this is
        server.setExecutor(null); // creates a default executor
        System.out.print("Starting simple http server... ");
        server.start();
        System.out.println("STARTED!");
        System.out.println("http://" + localHostAddress + ":" + serverPort + "/chatterbox");
    }

    public static String getLocalIPAddress() {
        try {
            // Get all network interfaces
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();

                // Get all IP addresses for each network interface
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();

                    // Ignore loopback addresses like 127.0.0.1
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();  // Return the first valid IP address found
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;  // Return null if no IP address is found
    }


    static class UsernameGiver implements HttpHandler {

        @Override
        public void handle(HttpExchange http) throws IOException {

            String username;
            String clientIp = http.getRemoteAddress().getAddress().getHostAddress();
            if (!loggedUsers.containsKey(clientIp)) {
                username = Message.generateUsername();
                loggedUsers.put(clientIp, username);
            } else {
                username = loggedUsers.get(clientIp);
            }

            http.sendResponseHeaders(200, username.getBytes().length);
            OutputStream os = http.getResponseBody();
            os.write(username.getBytes());
            os.close();
            System.out.println("[" + username + " logged in.]");
        }
    }

    // handler for "/chatterbox
    static class WebpageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange http) throws IOException {

            // technically shouldnt get post anytime, but did if just in case
            if ("GET".equals(http.getRequestMethod())) {

                // if message list not empty; load it (
                // serve html

                // get byt file path
                String path = "src/frontend/chatterbox.html";
                byte[] htmlBytes = Files.readAllBytes(Paths.get(path));
                http.getResponseHeaders().set("Content-Type", "text/html");

                // send it
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

        // Helper method to get MIME type based on file extension
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

    static class MessageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange http) throws IOException {

            String responseItem;

            if ("GET".equals(http.getRequestMethod())) {

                responseItem = gson.toJson(messageList);


            } // else if  POST   - get message from body / add to list
            else if ("POST".equals(http.getRequestMethod())) {

                String posterUsername = loggedUsers.get(http.getRemoteAddress().getAddress().getHostAddress());

                // get message, add to list
                String messageContent = new BufferedReader(new InputStreamReader(http.getRequestBody()))
                        .lines().collect(Collectors.joining("\n"));

                String formattedTime = Message.getCurrentTimeStamp();

                if (messageContent.equals("user-disconnect")) {

                }

                Message sentMessage = new Message(posterUsername, messageContent, Message.generateID(), formattedTime);
                messageList.messages.add(sentMessage);
                responseItem = gson.toJson(sentMessage);

                System.out.println(Message.formatMessage(messageList.messages.getLast()));


            } else {
                System.out.println(http.getResponseCode());
                responseItem = "SOMETHING WENT WRONG";
            }
            // else return 400 BAD REQUEST


            /*

            CRUD (CREATE.READ.UPDATE.DELETE.)

            GET -- get resource list or resource by id
            POST -- add new resource and generate id
            PUT -- edit (or add) resource with specific id
            DELETE -- delete with id

            [server]/mychat/messages[/message_id]

           +++ GET /mychat/messages -- return list
            GET /mychat/messages/<id>  -- return one message
           +++ POST /mychat/messages  -- add message AND generate id and return message with id
            PUT /mychat/messages/<id>  -- send edited message
            DELETE /mychat/messages/<id>  -- delete

            http codes 200 - ok / 4xx bad request (client problem) / 5xx server error  (3xx redirect)



             */

            String response = responseItem;// "METHOD: " + http.getRequestMethod() + " --> " + responseItem;
            http.sendResponseHeaders(200, response.length());
            OutputStream os = http.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }


    }

}