package data;

import java.io.IOException;
import java.util.Map;

/**
 * Interface for data managers handling loading and saving of data.
 * @param <K> Key type for the data map
 * @param <V> Value type (Model object) for the data map
 */
public interface DataManager<K, V> {

    /**
     * Loads data from the specified file path into a map.
     *
     * @param filePath The path to the data file.
     * @return A map containing the loaded data.
     * @throws IOException If an error occurs during file reading.
     */
    Map<K, V> load(String filePath) throws IOException;

    /**
     * Saves the provided data map to the specified file path.
     *
     * @param filePath The path to the data file.
     * @param dataMap The map containing the data to save.
     * @throws IOException If an error occurs during file writing.
     */
    void save(String filePath, Map<K, V> dataMap) throws IOException;
}