package it.osm.gtfs.utils;

import java.text.Normalizer;
import java.util.regex.Pattern;

public class VariousUtils {
    public static String removeAccents(String input) {
        if (input == null) {
            return null;
        }

        String normalizedString = Normalizer.normalize(input, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalizedString).replaceAll("");
    }
}
