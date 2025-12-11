package me.darragh.voxaltsapi.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for parsing account information from account strings.
 *
 * @author darraghd493
 * @since 1.00.
 */
public class AccountParser {
    private static final Pattern LINE_PATTERN = Pattern.compile(
            "\\[(?<username>[^]]+)](?<email>[^:]+):(?<password>[^|]+)\\|\\s*mctoken:\\s*(?<mctoken>.+)",
            Pattern.CASE_INSENSITIVE
    );

    public static AccountInformation parse(String input) {
        Matcher matcher = LINE_PATTERN.matcher(input);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Input string does not match expected format");
        }

        return new AccountInformation(
                matcher.group("username"),
                matcher.group("email"),
                matcher.group("password").trim(),
                matcher.group("mctoken").trim()
        );
    }

    public static boolean isValidFormat(String input) {
        return LINE_PATTERN.matcher(input).matches();
    }

    public record AccountInformation(
            String username,
            String email,
            String password,
            String mctoken
    ) {
    }
}
