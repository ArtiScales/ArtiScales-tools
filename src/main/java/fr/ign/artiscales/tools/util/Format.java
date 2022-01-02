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
     */
    public static double getDoubleFromCommaFormattedString(SimpleFeature sf, String field) {
        if (sf == null || sf.getAttribute(field) == null)
            return 0.0;
        try {
            return (double) sf.getAttribute(field);
        } catch (ClassCastException c) {
            try {
                return (int) sf.getAttribute(field);
            } catch (ClassCastException cc) {
                try {
                    return sf.getAttribute(field) != null ? NumberFormat.getInstance(Locale.FRANCE).parse((String) sf.getAttribute(field)).doubleValue() : 0.0;
                } catch (ParseException e) {
                    e.printStackTrace();
                    System.err.println("getDoubleFromCommaFormattedString(): return 0");
                    return 0.0;
                }
            }
        }
    }

    /**
     * Get rid of that comma infecting some French datasets
     *
     * @param sf    SimpleFeature incriminated
     * @param field String attribute that represents a comma separated double
     * @return the value
     */
    public static float getFloatFromCommaFormattedString(SimpleFeature sf, String field) {
        if (sf == null || sf.getAttribute(field) == null)
            return 0.0f;
        try {
            return (float) sf.getAttribute(field);
        } catch (ClassCastException c) {
            try {
                return (int) sf.getAttribute(field);
            } catch (ClassCastException cc) {
                try {
                    return (float) ((double) sf.getAttribute(field));
                } catch (ClassCastException ccc) {
                    try {
                        return sf.getAttribute(field) != null ? NumberFormat.getInstance(Locale.FRANCE).parse((String) sf.getAttribute(field)).floatValue() : 0.0f;
                    } catch (ParseException e) {
                        e.printStackTrace();
                        System.err.println("getDoubleFromCommaFormattedString(): return 0");
                        return 0.0f;
                    }
                }
            }

        }
    }
}
