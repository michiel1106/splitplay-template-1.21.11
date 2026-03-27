package bikerboys.splitplay.Util;

import net.fabricmc.loader.api.*;

import java.util.*;

public class DebugUtils {

    public static void debug(Object debug) {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            System.out.println("[SPLITPLAY DEBUG] " + debug);
        }

    }

}
