package org.kexie.android.hotfix.plugins.imgui;

import org.ice1000.jimgui.JImGui;
import org.ice1000.jimgui.JImStyle;
import org.ice1000.jimgui.util.JniLoader;
import org.kexie.android.hotfix.plugins.workflow.Context;

public class Imgui {
    public static void init(Context context) {
        Looper looper = Looper.myLooper();
        JniLoader.load();
        JImGui gui = new JImGui(720, 480, "Patch Plugin GUI");
        JImStyle style = gui.getStyle();
        style.setItemSpacingX(2f);
        gui.initBeforeMainLoop();
        looper.post(() -> {

        });
    }
}
