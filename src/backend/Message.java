package backend;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class Message {

    String username;
    String contents;
    String id;
    String formattedTime;

    public Message(String username, String contents, String id, String formattedTime) {
        this.username = username;
        this.contents = contents;
        this.id = id;
        this.formattedTime = formattedTime;
    }



    static String formatMessage(Message message) {
        return ("<" + message.username + "> " + message.contents + "   @" + message.formattedTime);
    }

    static Random random = new Random();
    static String[] adjectives = {
            "Amazing", "Bright", "Curious", "Determined", "Energetic",
            "Fierce", "Graceful", "Honest", "Incredible", "Joyful",
            "Kind", "Lively", "Mysterious", "Noble", "Optimistic",
            "Powerful", "Quick", "Radiant", "Strong", "Thoughtful",
            "Unique", "Vibrant", "Witty", "Xenial", "Youthful", "Zesty"
    };

    static String[] nouns = {
            "Adventure", "Battle", "Castle", "Dream", "Echo",
            "Forest", "Galaxy", "Hero", "Island", "Journey",
            "Kingdom", "Legend", "Mountain", "Night", "Ocean",
            "Pursuit", "Quest", "Reign", "Saga", "Tale",
            "Universe", "Victory", "Warrior", "Xenon", "Yarn", "Zeal"
    };
    static String generateUsername() {
        String randomAdjective = adjectives[random.nextInt(adjectives.length)];
        String randomNoun = nouns[random.nextInt(nouns.length)];

        return (randomAdjective + randomNoun);
    }

    static int messageNumber = 0;

    static String generateID() {
        messageNumber++;
        return String.valueOf(messageNumber);
    }

    public static String getCurrentTimeStamp() {
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        return sdf.format(now);
    }

}
