package za.redbridge.simulator.config;

public abstract class Config {

    protected static boolean checkFieldPresent(Object field, String name) {
        if (field != null) {
            return true;
        }
        System.out.println("Field '" + name + "' not present, using default");
        return false;
    }
}
