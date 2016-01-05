package cc.blynk.utils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Copy/paste from
 * https://github.com/spring-projects/spring-framework/blob/master/spring-web/src/main/java/org/springframework/web/util/UriTemplate.java
 */
public class UriTemplate {

    private final List<String> variableNames;

    private final Pattern matchPattern;

    private final String uriTemplate;


    /**
     * Construct a new {@code UriTemplate} with the given URI String.
     * @param uriTemplate the URI template string
     */
    public UriTemplate(String uriTemplate) {
        this.uriTemplate = uriTemplate;

        TemplateInfo info = TemplateInfo.parse(uriTemplate);
        this.variableNames = Collections.unmodifiableList(info.getVariableNames());
        this.matchPattern = info.getMatchPattern();
    }


    /**
     * Return the names of the variables in the template, in order.
     * @return the template variable names
     */
    public List<String> getVariableNames() {
        return this.variableNames;
    }


    /**
     * Indicate whether the given URI matches this template.
     * @param uri the URI to match to
     * @return {@code true} if it matches; {@code false} otherwise
     */
    public boolean matches(String uri) {
        if (uri == null) {
            return false;
        }
        Matcher matcher = this.matchPattern.matcher(uri);
        return matcher.matches();
    }

    /**
     * Match the given URI to a map of variable values. Keys in the returned map are variable names,
     * values are variable values, as occurred in the given URI.
     * <p>Example:
     * <pre class="code">
     * UriTemplate template = new UriTemplate("http://example.com/hotels/{hotel}/bookings/{booking}");
     * System.out.println(template.match("http://example.com/hotels/1/bookings/42"));
     * </pre>
     * will print: <blockquote>{@code {hotel=1, booking=42}}</blockquote>
     * @param uri the URI to match to
     * @return a map of variable values
     */
    public Map<String, String> match(String uri) {
        Map<String, String> result = new LinkedHashMap<>(this.variableNames.size());
        Matcher matcher = this.matchPattern.matcher(uri);
        if (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                String name = this.variableNames.get(i - 1);
                String value = matcher.group(i);
                result.put(name, value);
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return this.uriTemplate;
    }


    /**
     * Helper to extract variable names and regex for matching to actual URLs.
     */
    private static class TemplateInfo {

        private final List<String> variableNames;

        private final Pattern pattern;


        private TemplateInfo(List<String> vars, Pattern pattern) {
            this.variableNames = vars;
            this.pattern = pattern;
        }

        private static TemplateInfo parse(String uriTemplate) {
            int level = 0;
            List<String> variableNames = new ArrayList<String>();
            StringBuilder pattern = new StringBuilder();
            StringBuilder builder = new StringBuilder();
            for (int i = 0 ; i < uriTemplate.length(); i++) {
                char c = uriTemplate.charAt(i);
                if (c == '{') {
                    level++;
                    if (level == 1) {
                        // start of URI variable
                        pattern.append(quote(builder));
                        builder = new StringBuilder();
                        continue;
                    }
                }
                else if (c == '}') {
                    level--;
                    if (level == 0) {
                        // end of URI variable
                        String variable = builder.toString();
                        int idx = variable.indexOf(':');
                        if (idx == -1) {
                            pattern.append("(.*)");
                            variableNames.add(variable);
                        }
                        else {
                            if (idx + 1 == variable.length()) {
                                throw new IllegalArgumentException(
                                        "No custom regular expression specified after ':' " +
                                                "in \"" + variable + "\"");
                            }
                            String regex = variable.substring(idx + 1, variable.length());
                            pattern.append('(');
                            pattern.append(regex);
                            pattern.append(')');
                            variableNames.add(variable.substring(0, idx));
                        }
                        builder = new StringBuilder();
                        continue;
                    }
                }
                builder.append(c);
            }
            if (builder.length() > 0) {
                pattern.append(quote(builder));
            }
            return new TemplateInfo(variableNames, Pattern.compile(pattern.toString()));
        }

        private static String quote(StringBuilder builder) {
            return builder.length() != 0 ? Pattern.quote(builder.toString()) : "";
        }

        public List<String> getVariableNames() {
            return this.variableNames;
        }

        public Pattern getMatchPattern() {
            return this.pattern;
        }
    }

}
