package com.ash.auth_service.util;

import java.util.Random;

public class UserIdGenerator {

    private static final String ALPHANUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final Random RANDOM = new Random();

    public static String generateUserId(String email,String phone) {
        String base;

        if (email != null && !email.isEmpty()) {
            base = email.split("@")[0];
        } else if (phone != null && phone.length() >= 4) {
            base = phone.substring(phone.length() - 4);
        } else {
            base = "USER";
        }

        return base+ generateRandom(5);
    }

    private static String generateRandom(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUM.charAt(RANDOM.nextInt(ALPHANUM.length())));
        }
        return sb.toString();
    }

}
