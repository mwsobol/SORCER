package sorcer.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Paweł Rubach
 * Created on 5/27/2014.
 */
public class WhitespaceTokenizer {

    public static List<String> tokenize(String input) {
        List<String> matchList = new ArrayList<String>();
        Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
        Matcher regexMatcher = regex.matcher(input);
        while (regexMatcher.find()) {
            if (regexMatcher.group(1) != null) {
                // Add double-quoted string without the quotes
                matchList.add(regexMatcher.group(1));
            } else if (regexMatcher.group(2) != null) {
                // Add single-quoted string without the quotes
                matchList.add(regexMatcher.group(2));
            } else {
                // Add unquoted word
                matchList.add(regexMatcher.group());
            }
        }
        return matchList;
    }

    private List<String> tokens;

    private int pos = 0;

    public WhitespaceTokenizer(String input) {
        tokens = tokenize(input);
    }

    public boolean hasMoreTokens() {
        return (countTokens()>0);
    }

    public int countTokens() {
        return tokens.size()-pos;
    }

    public String nextToken() {
        if (pos<tokens.size()) {
            pos+=1;
            return tokens.get(pos - 1);
        }
        else
            return "";
    }

}

