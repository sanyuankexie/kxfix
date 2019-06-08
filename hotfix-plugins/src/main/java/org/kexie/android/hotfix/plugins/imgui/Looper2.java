package org.kexie.android.hotfix.plugins.imgui;

import org.ice1000.jimgui.JImGui;
import org.ice1000.jimgui.JImGuiGen;
import org.ice1000.jimgui.JImGuiIOGen;
import org.ice1000.jimgui.JImStyle;
import org.ice1000.jimgui.util.JniLoader;
import org.kexie.android.hotfix.plugins.workflow.Context;

public class Looper2 {

    private static final int LIMIT = 3;

    private final Context context;

    private Looper2(Context context) {
        this.context = context;
        JniLoader.load();
    }

    public static Looper2 make(Context context) {
        return new Looper2(context);
    }

    public void loop() throws InterruptedException {
        try (JImGui gui = new JImGui(720, 480, "Patch Plugin GUI")) {
            JImStyle style = gui.getStyle();
            style.setItemSpacingX(2f);
            gui.initBeforeMainLoop();
            while (!gui.windowShouldClose()) {
                long deltaTime = (long) (JImGuiIOGen.getDeltaTime() * 1000);
                Thread.sleep(LIMIT - deltaTime < 0 ? 0 : (LIMIT - deltaTime));
                gui.initNewFrame();
                gui.begin("Task");
                gui.text(context.getTaskName());
                JImGuiGen.end();
                gui.render();
            }
        }
        //任务已完成,退出子程序
        System.exit(0);
    }
}
