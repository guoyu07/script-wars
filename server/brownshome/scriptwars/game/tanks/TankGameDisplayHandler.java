package brownshome.scriptwars.game.tanks;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.function.Consumer;

import brownshome.scriptwars.game.Game;
import brownshome.scriptwars.game.GridDisplayHandler;
import brownshome.scriptwars.game.GridItem;
import brownshome.scriptwars.game.Player;

public class TankGameDisplayHandler extends GridDisplayHandler {
	private static final byte PLAYER_ID_BYTE = GridDisplayHandler.FREE_ID;
	
	public static enum StaticSprites {
		NOTHING, WALL;
	}
	
	public static enum DynamicSprites {
		SHOT, AMMO, TANK_START;
	}
	
	public TankGameDisplayHandler(TankGame game) {
		super(game);
	}

	@Override
	protected void updatePlayerTable(Collection<Consumer<ByteBuffer>> oldViewers) {
		super.updatePlayerTable(oldViewers);
		send(getPlayerIDBuffer(), oldViewers);
	}
	
	
	@Override
	protected void handleNewViewers(Collection<Consumer<ByteBuffer>> newViewers) {
		send(getPlayerIDBuffer(), newViewers);
		super.handleNewViewers(newViewers);
	}
	
	private ByteBuffer getPlayerIDBuffer() {
		ByteBuffer buffer = ByteBuffer.allocate(Byte.BYTES + Integer.BYTES * game.getMaximumPlayers());
		buffer.put(PLAYER_ID_BYTE);
		IntBuffer intBuffer = buffer.asIntBuffer();
		
		TankGame game = (TankGame) this.game;
		
		for(Player<?> player : game.getActivePlayers()) {
			intBuffer.put(game.getIndex(player), player.getID());
		}
		
		buffer.rewind();
		return buffer;
	}
	
	@Override
	public TankGame getGame() {
		return (TankGame) super.getGame();
	}
}
