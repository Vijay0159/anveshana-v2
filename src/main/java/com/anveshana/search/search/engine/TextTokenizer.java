package com.anveshana.search.search.engine;

import java.util.Arrays;
import java.util.List;

public class TextTokenizer {

    public static List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return Arrays.stream(text.toLowerCase().split("\\W+"))
                .filter(token -> !token.isBlank())
                .toList();
    }
}
