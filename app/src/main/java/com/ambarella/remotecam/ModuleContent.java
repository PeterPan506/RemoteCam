package com.ambarella.remotecam;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 * <p>
 */
public class ModuleContent {

    public static final int MODULE_POS_SETUP = 0;
    public static final int MODULE_POS_CAMERA = 1;
    public static final int MODULE_POS_MEDIA = 2;
    public static final int MODULE_POS_COMMANDS = 3;
    public static final int MODULE_POS_SETTINGS = 4;

    /**
     * An array of sample (dummy) items.
     */
    public static List<ModuleItem> ITEMS = new ArrayList<ModuleItem>();

    static {
        addItem(new ModuleItem(MODULE_POS_SETUP,    "Setup"));
        addItem(new ModuleItem(MODULE_POS_CAMERA,   "Camera"));
        addItem(new ModuleItem(MODULE_POS_MEDIA,    "Media"));
        addItem(new ModuleItem(MODULE_POS_COMMANDS, "Commands"));
        addItem(new ModuleItem(MODULE_POS_SETTINGS, "Settings"));
    }

    private static void addItem(ModuleItem item) {
        ITEMS.add(item);
    }

    /**
     * A dummy item representing a piece of content.
     */
    public static class ModuleItem {
        public int id;
        public String content;

        public ModuleItem(int id, String content) {
            this.id = id;
            this.content = content;
        }

        @Override
        public String toString() {
            return content;
        }
    }
}
