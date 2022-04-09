package tk.meowmc.slippery;

import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tk.meowmc.slippery.config.SlipperyConfig;

public class Slippery implements ModInitializer {

	public static final String MODID = "slippery";

	public static Identifier id(String path) {
		return new Identifier(MODID, path);
	}

	@Override
	public void onInitialize() {
		SlipperyConfig.register();
	}
}
