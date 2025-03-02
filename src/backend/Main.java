package backend;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Main {

    static MessageList messageList = new MessageList();
    static Map<String, String> loggedUsers = new HashMap<String, String>();


    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("10.0.0.198", 8000), 0);
        server.createContext("/chat", new MyHandler());

        server.setExecutor(null); // creates a default executor
        System.out.print("Starting simple http server... ");
        server.start();
        System.out.println("STARTED!");
    }

    static class MyHandler implements HttpHandler {
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
                }
                else {
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

            }
            else {
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