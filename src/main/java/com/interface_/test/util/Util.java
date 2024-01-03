package com.interface_.test.util;

import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Util {

    private static final String base = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static String randomString(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("length is not valid");
        }
        Random random = new Random();
        return Stream.iterate(0, i -> i + 1)
                .limit(length)
                .map(i -> random.nextInt(base.length()))
                .map(base::charAt)
                .map(String::valueOf)
                .collect(Collectors.joining());
    }
}
