package backend;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Main {

    static MessageList messageList = new MessageList();
    static Map<String, String> loggedUsers = new HashMap<>();


    public static void main(String[] args) throws Exception {

        // make server on an ip TODO make auto
        HttpServer server = HttpServer.create(new InetSocketAddress("10.0.0.198", 8000), 0);

        // get messages (make /messages)
        server.createContext("/chat", new MessageHandler());

        // goes to actual webpage
        server.createContext("/chatterbox", new WebpageHandler());

        // gets static resources
        server.createContext("/resources", new StaticFileHandler());

        // idk what this is
        server.setExecutor(null); // creates a default executor
        System.out.print("Starting simple http server... ");
        server.start();
        System.out.println("STARTED!");
    }

    // handler for "/chatterbox
    static class WebpageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange http) throws IOException {

            // technically shouldnt get post anytime, but did if just in case
            if ("GET".equals(http.getRequestMethod())) {

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

                responseItem = "CHATLOGS:\n";

                ///  make list of messages and write into response
                for (Message message : messageList.messages) {
                    responseItem += Message.formatMessage(message) + "\n";
                }

            } // else if  POST   - get message from body / add to list
            else if ("POST".equals(http.getRequestMethod())) {

                String clientIp = http.getRemoteAddress().getAddress().getHostAddress();
                String posterUsername;
                if (loggedUsers.containsKey(clientIp)) {
                    posterUsername = loggedUsers.get(clientIp);
                } else {
                    posterUsername = Message.generateUsername();
                    loggedUsers.put(clientIp, posterUsername);
                }

                // get message, add to list
                String messageContent = new BufferedReader(new InputStreamReader(http.getRequestBody()))
                        .lines().collect(Collectors.joining("\n"));

                String formattedTime = Message.getCurrentTimeStamp();

                messageList.messages.add(new Message(posterUsername, messageContent, Message.generateID(), formattedTime));
                System.out.println(Message.formatMessage(messageList.messages.getLast()));

                responseItem = "MESSAGE SENT!\n";

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

            String response = "METHOD: " + http.getRequestMethod() + " --> " + responseItem;
            http.sendResponseHeaders(200, response.length());
            OutputStream os = http.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }


    }

}