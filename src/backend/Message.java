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
            "Absurd", "Bouncy", "Chubby", "Dizzy", "Eccentric",
            "Fluffy", "Goofy", "Hilarious", "Invisible", "Jiggly",
            "Kooky", "Lumpy", "Mischievous", "Noodle", "Puffy",
            "Quirky", "Rambunctious", "Silly", "Twirly", "Ugly",
            "Vibrant", "Wobbly", "Xtra", "Yummy", "Zany",
            "Awkward", "Baffled", "Chunky", "Dorky", "Extra",
            "Flaky", "Greasy", "Hairy", "Irrational", "Jumpy",
            "Krusty", "Lopsided", "Muggy", "Nutty", "Peculiar",
            "Random", "Slippery", "Twirling", "Unpredictable", "Viral",
            "Wacky", "Xenial", "Yucky", "Zigzag", "Bizarre",
            "Clumsy", "Drippy", "Exaggerated", "Funky", "Gooey",
            "Hooting", "Insane", "Jumpy", "Kooky", "Lopsided",
            "Messy", "Nonsensical", "Overt", "Puddy", "Quacky",
            "Ridiculous", "Soggy", "Tacky", "Unhinged", "Vexed",
            "Wobbly", "Large", "Yowza", "Zipped", "Blundering",
            "Confused", "Dizzying", "Eccentric", "Frightful", "Grumpy",
            "Horrible", "Irritating", "Jelly", "Petty", "Laughable",
            "Muddled", "Nutso", "Overcooked", "Pineapple", "Quirky"
    };

    static String[] nouns = {
            "Bacon", "Banana", "Cabbage", "Disaster", "Egret",
            "Fungus", "Goat", "Hamster", "Igloo", "Jellybean",
            "Kangaroo", "Lobster", "Muffin", "Noodle", "Octopus",
            "Pineapple", "Quokka", "Raccoon", "Shark", "Taco",
            "Unicorn", "Vampire", "Waffle", "Xylophone", "Yogurt",
            "Poptart", "Banjo", "Churro", "Donut", "Eagle",
            "Squirrel", "Giraffe", "Hamburger", "Ice", "Jellyfish",
            "Ketchup", "Lettuce", "Muffin", "Nacho", "Oatmeal",
            "Pinecone", "Quilt", "Raisin", "Snail", "Turtle",
            "Underpants", "Vampire", "Whale", "Xenomorph", "Yeti",
            "Zebra", "Blimp", "Cucumber", "Duck", "Eggplant",
            "Spinner", "Gumbo", "Hoop", "Man", "Jellyfish",
            "Kangaroo", "Llama", "Magma", "Nachos", "Otter",
            "Pickle", "Quiche", "Ravioli", "Sloth", "Taco",
            "Umpire", "Vinegar", "Worm", "Xanadu", "Yogurt",
            "Zucchini", "Bumper", "Clown", "Dinosaur", "Elevator",
            "Fidget", "Goose", "Hotdog", "Invention", "Jigsaw",
            "Kite", "Lollipop", "Marshmallow", "Nutcracker", "Onion",
            "Pickpocket", "Quokka", "Rainbow", "Sushi", "Teapot",
            "Unicorn", "Vacuum", "Whistle", "Napkin", "Yawn"
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
