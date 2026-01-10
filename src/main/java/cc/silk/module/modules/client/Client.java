package cc.silk.module.modules.client;

import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.BooleanSetting;

public final class Client extends Module {

    public static final BooleanSetting title = new BooleanSetting("Title", true);
    public Client() {
        super("Client", "Settings for the client", -1, Category.CLIENT);

        this.addSettings(
                title
        );
    }

    public boolean getTitle() {
        return title.getValue();
    }

}
