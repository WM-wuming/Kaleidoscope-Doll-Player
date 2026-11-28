package kaleidoscopeDollPlayer;

import kaleidoscopeDollPlayer.network.ModMessages;
import kaleidoscopeDollPlayer.registry.*;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KaleidoscopeDollPlayer implements ModInitializer {
	public static final String MOD_ID = "kaleidoscope_doll_player";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModDataComponents.register();
		// 注册模组内容（方块先于物品：玩偶物品是 BlockItem，需要方块实例）
		ModBlocks.registerModBlocks();
		ModItems.registerModItems();
		ModBlockEntities.registerBlockEntities();
		ModSounds.registerSounds();
		ModItemGroups.registerItemGroups();
		ModMessages.registerC2SPackets();
	}
}
