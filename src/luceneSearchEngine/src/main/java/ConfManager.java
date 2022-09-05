import org.ini4j.Ini;
import org.ini4j.IniPreferences;

import java.io.File;
import java.io.IOException;
import java.util.prefs.Preferences;

public class ConfManager {
    private Ini ini;
    private Preferences prefs;
    private String confPath;

    public ConfManager(String confPath) throws IOException {
        File f = new File(confPath);
        if (f.exists() && !f.isDirectory()) {
            this.confPath = confPath;
            ini = new Ini(new File(confPath));
        } else {
            System.err.println("config file does not exist");
            return;
        }
        prefs = new IniPreferences(ini);


    }

    public String readConf(String path, String key) {
        return prefs.node(path).get(key, null);
    }

    public void writeConf(String path, String key, String value) throws IOException {
        ini.put(path, key, value);
        ini.store();
    }

    public void writeConf(String path, String key, int value) throws IOException {
        ini.put(path, key, value);
        ini.store();
    }


}
