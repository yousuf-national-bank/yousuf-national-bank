package bank.web;

import bank.Bank;
import bank.FileStorage;

/** Wraps the Bank engine + persistence so route handlers can mutate and save in one call.
 *  Uses the same file-based storage as the console app (bankdata.ser) — no extra
 *  driver jar needed, so the web server runs with nothing but the JDK. */
final class AppState {
    final Bank bank;
    private final String dataPath;

    private AppState(Bank bank, String dataPath) {
        this.bank = bank;
        this.dataPath = dataPath;
    }

    static AppState start(String bankName) {
        String dir = System.getenv("BANK_DATA_DIR"); // set this to a persistent volume path when deployed
        String path = (dir == null || dir.isEmpty()) ? "webbankdata.ser" : dir.replaceAll("/+$", "") + "/webbankdata.ser";
        Bank loaded = FileStorage.load(path);
        Bank bank = (loaded != null) ? loaded : new Bank(bankName);
        return new AppState(bank, path);
    }

    void persist() {
        FileStorage.save(bank, dataPath);
    }
}
