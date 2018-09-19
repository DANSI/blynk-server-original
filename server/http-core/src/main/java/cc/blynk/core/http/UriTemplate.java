package cc.blynk.core.http;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UriTemplate {

    // Finds parameters in the URL pattern string.
    private static final String URL_PARAM_REGEX = "\\{(\\w*?)\\}";

    // Replaces parameter names in the URL pattern string to match parameters in URLs.
    private static final String URL_PARAM_MATCH_REGEX =
            "\\([%\\\\w-.\\\\~!\\$&'\\\\(\\\\)\\\\*\\\\+,;=:\\\\[\\\\]@]+?\\)";

    // Pattern to match URL pattern parameter names.
    private static final Pattern URL_PARAM_PATTERN = Pattern.compile(URL_PARAM_REGEX);

    // Finds the 'format' portion of the URL pattern string.
    private static final String URL_FORMAT_REGEX = "(?:\\.\\{format\\})$";

    // Replaces the format parameter name in the URL pattern string to
    // match the format specifier in URLs. Appended to the end of the regex string
    // when a URL pattern contains a format parameter.
    private static final String URL_FORMAT_MATCH_REGEX = "(?:\\\\.\\([\\\\w%]+?\\))?";

    // Finds the query string portion withi    private Matcher matcher;n a URL.
    // Appended to the end of the built-up regex string.
    private static final String URL_QUERY_STRING_REGEX = "(?:\\?.*?)?$";

    private final String urlPattern;
    private Matcher matcher;
    private Pattern compiledUrl;

    private final List<String> parameterNames = new ArrayList<>();

    public UriTemplate(String pattern) {
        this.urlPattern = pattern;
        compile();
    }

    public Matcher matcher(String url) {
        return compiledUrl.matcher(url);
    }

    public void compile() {
        acquireParameterNames();
        String parsedPattern = urlPattern.replaceFirst(URL_FORMAT_REGEX, URL_FORMAT_MATCH_REGEX);
        parsedPattern = parsedPattern.replaceAll(URL_PARAM_REGEX, URL_PARAM_MATCH_REGEX);
        this.compiledUrl = Pattern.compile(parsedPattern + URL_QUERY_STRING_REGEX);
    }

    private void acquireParameterNames() {
        Matcher m = URL_PARAM_PATTERN.matcher(urlPattern);

        while (m.find()) {
            parameterNames.add(m.group(1));
        }
    }

    public Map<String, String> extractParameters(Matcher matcher) {
        Map<String, String> values = new HashMap<>();

        for (int i = 0; i < matcher.groupCount(); i++) {
            String value = matcher.group(i + 1);

            if (value != null) {
                values.put(parameterNames.get(i), value);
            }
        }

        return values;
    }

}
