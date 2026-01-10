package cc.silk.utils.keybinding.simulation;

import lombok.experimental.UtilityClass;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

import static cc.silk.SilkClient.shouldUseMouseEvent;

@UtilityClass
public final class ClickSimulator {
    public static void leftClick() {
        if (shouldUseMouseEvent) {
            User32.INSTANCE.mouse_event(User32.MOUSEEVENTF_LEFTDOWN, 0, 0, 0, 0);
            User32.INSTANCE.mouse_event(User32.MOUSEEVENTF_LEFTUP, 0, 0, 0, 0);
        } else {
            MinecraftClient mc = MinecraftClient.getInstance();
            KeyBinding attack = mc.options.attackKey;

            InputUtil.Key key = attack.getDefaultKey();

            KeyBinding.setKeyPressed(key, true);
            KeyBinding.onKeyPressed(key);

            mc.execute(() -> KeyBinding.setKeyPressed(key, false));
        }
    }
}