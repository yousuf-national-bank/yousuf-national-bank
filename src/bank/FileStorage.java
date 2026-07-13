package bank;

import java.io.*;

/** Saves/loads the whole Bank object graph to a single file using Java serialization. */
public final class FileStorage {
    private static final String DEFAULT_FILE = "bankdata.ser";

    private FileStorage() {}

    public static void save(Bank bank) {
        save(bank, DEFAULT_FILE);
    }

    public static void save(Bank bank, String path) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path))) {
            oos.writeObject(bank);
        } catch (IOException e) {
            System.out.println("Warning: could not save data (" + e.getMessage() + ")");
        }
    }

    public static Bank load() {
        return load(DEFAULT_FILE);
    }

    public static Bank load(String path) {
        File f = new File(path);
        if (!f.exists()) return null;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            return (Bank) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Warning: could not load existing data (" + e.getMessage() + "). Starting fresh.");
            return null;
        }
    }
}
