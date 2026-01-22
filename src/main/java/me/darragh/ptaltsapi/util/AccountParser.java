package me.darragh.ptaltsapi.util;

import lombok.Value;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for parsing account information from account strings.
 *
 * @author darraghd493
 * @since 1.0.0
 */
@UtilityClass
public final class AccountParser {
    private static final Pattern LINE_PATTERN = Pattern.compile(
            "\\[(?<username>[^]]+)](?<email>[^:]+):(?<password>[^|]+)\\|\\s*mctoken:\\s*(?<mctoken>.+)",
            Pattern.CASE_INSENSITIVE
    );

    public static @NotNull AccountInformation parse(@NotNull String input) throws IllegalArgumentException {
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

    public static boolean isValidFormat(@NotNull String input) {
        return LINE_PATTERN.matcher(input).matches();
    }

    @Value
    public static class AccountInformation {
        @NotNull String username;
        @NotNull String email;
        @NotNull String password;
        @NotNull String mctoken;
    }
}
