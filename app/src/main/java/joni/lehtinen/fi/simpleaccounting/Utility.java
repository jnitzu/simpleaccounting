package joni.lehtinen.fi.simpleaccounting;

import java.util.ArrayList;
import java.util.List;

/**
 * Class for static utility methods
 */
public class Utility {

    /**
     * Formatting method to concat field names with comma and and word
     * @param isEmpty Is field specified by field name empty
     * @param fieldName Field name for corresponding isEmpty field
     * @param and String literal for and word
     * @return Formatted string that has field names seperated by comma and last by and
     */
    public static String createIsEmptyString(boolean[] isEmpty, String[] fieldName, String and){
        StringBuilder builder = new StringBuilder();
        List<String> emptyFields = new ArrayList<>();

        // Find out what fields are empty
        for (int i = 0; i < isEmpty.length; i++){
            if(isEmpty[i]){
                emptyFields.add(fieldName[i].toLowerCase());
            }
        }

        // Add comma between names
        for (int i = 0; i < emptyFields.size(); i++){
            builder.append(emptyFields.get(i));
            if(i != emptyFields.size() - 1){
                builder.append(", ");
            }
        }

        // Add and word
        if(emptyFields.size() > 1){
            int index = builder.lastIndexOf(",");
            builder.replace(index, index + 1, " " + and);
        }

        // Set first letter to uppercase
        builder.replace(0,1,builder.substring(0,1).toUpperCase());
        return builder.toString();
    }
}
