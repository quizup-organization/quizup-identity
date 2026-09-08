package io.github.quizup.identity.domain.model;

import java.util.Random;

public final class NameGenerator {

    private static final String[] ADJECTIVES = {
            "Swift", "Brave", "Calm", "Bold", "Bright",
            "Cool", "Dark", "Epic", "Fast", "Fierce",
            "Glad", "Grand", "Happy", "Iron", "Jolly",
            "Kind", "Lucky", "Mega", "Noble", "Proud",
            "Quick", "Rare", "Sharp", "Sly", "Smart",
            "Soft", "Stern", "Super", "True", "Wild"
    };

    private static final String[] ANIMALS = {
            "Bear", "Cat", "Crow", "Deer", "Eagle",
            "Falcon", "Fox", "Hawk", "Jaguar", "Lion",
            "Lynx", "Otter", "Owl", "Panda", "Panther",
            "Raven", "Shark", "Tiger", "Wolf", "Zebra"
    };

    private static final Random random = new Random();

    private NameGenerator() {
        //
    }

    /**
     * Génère un nom du type "SwiftFox" (sans nombre)
     */
    public static String generate() {
        String adj    = ADJECTIVES[random.nextInt(ADJECTIVES.length)];
        String animal = ANIMALS[random.nextInt(ANIMALS.length)];
        return adj + animal + (10 + random.nextInt(90));
    }
}