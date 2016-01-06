package co.adrianblan.lightly.helpers;

/**
 * Class which contain static String related utilities.
 */
public class StringUtils {

    /** Takes an int, and returns a humanized String specifying the amount time */
    public static String getHumanizedHours(int hours) {
        if(hours == 0) {
            return "less than an hour";
        } else {
            return hours + " hours";
        }
    }
}
