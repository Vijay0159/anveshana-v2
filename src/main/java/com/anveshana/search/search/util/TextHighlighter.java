package com.anveshana.search.search.util;

import java.util.List;

public class TextHighlighter {

    public static String highlight(String content, List<String> terms) {
        if (content == null || content.isBlank() || terms == null || terms.isEmpty()) {
            return content;
        }

        String highlighted = content;

        for (String term : terms) {
            if (term == null || term.isBlank()) continue;
            String regex = "(?i)(" + java.util.regex.Pattern.quote(term) + ")";
            highlighted = highlighted.replaceAll(regex,
                    ConsoleColors.YELLOW_BOLD + "$1" + ConsoleColors.RESET);
        }

        return highlighted;
    }
}
