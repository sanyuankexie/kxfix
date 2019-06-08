package org.kexie.android.hotfix.plugins.imgui;

import org.ice1000.jimgui.JImGui;
import org.ice1000.jimgui.JImGuiGen;
import org.ice1000.jimgui.JImGuiIOGen;
import org.ice1000.jimgui.JImStyle;
import org.ice1000.jimgui.util.JniLoader;
import org.kexie.android.hotfix.plugins.workflow.Context;

public class Looper {

    private static final int LIMIT = 3;

    private final Context context;

    private Looper(Context context) {
        this.context = context;
        JniLoader.load();
    }

    public static Looper make(Context context) {
        return new Looper(context);
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
                StringBuilder builder = new StringBuilder();
                for (String name : context.getTaskQueue()) {
                    builder.append(name).append('\n');
                }
                gui.text(builder.toString());
                JImGuiGen.end();
                gui.render();
            }
        }
        //任务已完成,退出子程序
        System.exit(0);
    }
}
