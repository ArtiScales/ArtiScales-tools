package fr.ign.artiscales.tools.util;

import org.opengis.feature.simple.SimpleFeature;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public class Format {

    /**
     * Get rid of that comma infecting some French datasets
     *
     * @param sf    SimpleFeature incriminated
     * @param field String attribute that represents a comma separated double
     * @return the value
     * @throws ParseException if the beginning of the attribute value cannot be parsed.
     */
    public static double getDoubleFromCommaFormattedString(SimpleFeature sf, String field) throws ParseException {
        if (sf.getAttribute(field) == null)
            return 0.0;
        try {
            return (double) sf.getAttribute(field);
        } catch (ClassCastException c) {
            try {
                return (int) sf.getAttribute(field);
            } catch (ClassCastException cc) {
                return sf.getAttribute(field) != null ? NumberFormat.getInstance(Locale.FRANCE).parse((String) sf.getAttribute(field)).doubleValue() : 0.0;
            }
        }
    }
}
