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
        HttpServer server = HttpServer.create(new InetSocketAddress("10.0.0.198", 8000), 0);
        server.createContext("/chat", new MessageHandler());

        // goes to actual webpage
        server.createContext("/chatterbox", new WebpageHandler());

        // gets static resources
        server.createContext("/resources", new StaticFileHandler());

        server.setExecutor(null); // creates a default executor
        System.out.print("Starting simple http server... ");
        server.start();
        System.out.println("STARTED!");
    }

    static class WebpageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange http) throws IOException {

            if ("GET".equals(http.getRequestMethod())) {
                // Serve the HTML file
                String path = "src/frontend/chatterbox.html"; // Adjust path accordingly
                byte[] htmlBytes = Files.readAllBytes(Paths.get(path));

                // Set content type to text/html
                http.getResponseHeaders().set("Content-Type", "text/html");

                // Send the response headers and the HTML file content
                http.sendResponseHeaders(200, htmlBytes.length);
                OutputStream os = http.getResponseBody();
                os.write(htmlBytes);
                os.close();
            }
        }
    }

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange http) throws IOException {
            // Serve static files (CSS, images, etc.)
            String filePathString = "src/frontend" + http.getRequestURI().getPath(); // Adjust the resource path
            Path filePath = Paths.get(filePathString);

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
            if (fileName.endsWith(".css")) {
                return "text/css";
            } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                return "image/jpeg";
            } else if (fileName.endsWith(".png")) {
                return "image/png";
            } else if (fileName.endsWith(".ttf")) {
                return "font/ttf";
            } else if (fileName.endsWith(".woff")) {
                return "font/woff";
            } else if (fileName.endsWith(".woff2")) {
                return "font/woff2";
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