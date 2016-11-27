package joni.lehtinen.fi.simpleaccounting;

/**
 * Interface for fragments that use CreateActivity as a host and save data to database
 */
public interface AddItem {

    /**
     * Save form data to database
     */
    void saveToDatabase();

    /**
     * Checks if all values on form are valid, if not then highlight those that aren't
     * @return
     */
    boolean isFormValid();
}
