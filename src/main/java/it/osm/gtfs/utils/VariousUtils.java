package it.osm.gtfs.utils;

import java.text.Normalizer;
import java.util.Map;
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

    public static <T, E> T getKeysByValue(Map<T, E> map, E value) {
        for (Map.Entry<T, E> entry : map.entrySet()) {
            if (entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
