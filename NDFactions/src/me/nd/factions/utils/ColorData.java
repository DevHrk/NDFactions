package me.nd.factions.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorData {

    public static HashMap<String, ColorData> FACTION_ACTIONBAR = new HashMap<>();

    private String message, destaqueColor, principalColor;
    private int colorPosition;
    private String newMessage;
    private List<String> textSegments; // Store text segments without color codes
    private List<String> colorCodes; // Store color codes in order
    private int visibleLength; // Length of visible text (without color codes)

    // Regex to match Minecraft color codes (&c or §c)
    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("([&§][0-9a-fk-or])");

    public ColorData(String message, String destaqueColor, String principalColor) {
        this.message = message;
        this.destaqueColor = destaqueColor;
        this.principalColor = principalColor;
        this.colorPosition = 0;
        this.textSegments = new ArrayList<>();
        this.colorCodes = new ArrayList<>();
        parseMessage();
        this.newMessage = message; // Initialize with original message
    }

    // Parse the message into text segments and color codes
    private void parseMessage() {
        Matcher matcher = COLOR_CODE_PATTERN.matcher(message);
        int lastIndex = 0;
        visibleLength = 0;

        // Split message into segments and color codes
        while (matcher.find()) {
            String segment = message.substring(lastIndex, matcher.start());
            if (!segment.isEmpty()) {
                textSegments.add(segment);
                visibleLength += segment.length();
            }
            colorCodes.add(matcher.group());
            lastIndex = matcher.end();
        }

        // Add the final segment
        String finalSegment = message.substring(lastIndex);
        if (!finalSegment.isEmpty()) {
            textSegments.add(finalSegment);
            visibleLength += finalSegment.length();
        }

        // If no color codes, treat the entire message as one segment
        if (textSegments.isEmpty()) {
            textSegments.add(message);
            visibleLength = message.length();
        }
    }

    @SuppressWarnings("unused")
	public void next() {
        if (visibleLength == 0) {
            this.newMessage = message;
            return;
        }

        if (colorPosition >= visibleLength) {
            colorPosition = 0;
        }

        StringBuilder sb = new StringBuilder();
        int currentPos = 0; // Tracks position in visible text
		int segmentIndex = 0;
        int segmentPos = 0; // Position within current segment

        // Iterate through text segments and reapply color codes
        for (int i = 0; i < textSegments.size(); i++) {
            String segment = textSegments.get(i);
            // Add color code before this segment if it exists
            if (i < colorCodes.size()) {
                sb.append(colorCodes.get(i));
            }

            // Process each character in the segment
            for (int j = 0; j < segment.length(); j++) {
                if (currentPos == colorPosition) {
                    sb.append(destaqueColor).append(segment.charAt(j));
                } else {
                    sb.append(principalColor).append(segment.charAt(j));
                }
                currentPos++;
            }
        }

        this.newMessage = sb.toString();
        this.colorPosition++;
    }

    public String getMessage() {
        return this.newMessage;
    }
}