package brownshome.server.game.tanks;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import brownshome.server.game.DisplayHandler;
import brownshome.server.game.Player;

class World {
	List<Shot> shots = new ArrayList<>();
	Map<Player, Tank> tanks = new HashMap<>();
	boolean[][] map;
	Tank[][] tankMap;
	TankGame game;
	
	Set<Tank> tanksToFire = new HashSet<>();
	Set<Tank> tanksToRollBack = new HashSet<>();
	
	World(boolean[][] map, TankGame game) {
		assert map.length > 0 && map[0].length > 0;
		
		this.game = game;
		
		this.map = map;
		tankMap = new Tank[map.length][map[0].length];
	}

	/**
	 * @param x
	 * @param y
	 * @return null if there is no tank, returns the tank otherwise
	 */
	Tank getTank(int x, int y) {
		return tankMap[y][x];
	}

	boolean isWall(int x, int y) {
		return map[y][x];
	}

	public void moveTank(Player player, Direction direction) {
		Tank tank = tanks.get(player);
		tankMap[tank.getY()][tank.getX()] = null;
		tank.move(direction);
		tankMap[tank.getY()][tank.getX()] = tank;
	}

	public void fireTank(Player player, Direction direction) {
		Tank tank = tanks.get(player);
		
		int x = tank.getX();
		int y = tank.getY();
		x = direction.moveX(x);
		y = direction.moveY(y);
		
		if(isWall(x, y))
			return;
		
		Tank otherTank = getTank(x, y);
		if(otherTank != null) {
			otherTank.kill();
			return;
		}
		
		shots.add(new Shot(x, y, this, direction));
	}

	public void rollBackTanks() {
		for(Tank t : tanksToRollBack) {
			tankMap[t.getY()][t.getX()] = null;
			t.rollBack();
			tankMap[t.getY()][t.getX()] = t;
		}
		
		tanksToRollBack.clear();
	}

	public void fireTanks() {
		for(Tank t : tanksToFire) {
			fireTank(t.getOwner(), t.getDirection());
		}
		
		tanksToFire.clear();
	}

	public void moveShots() {
		for(Iterator<Shot> it = shots.iterator(); it.hasNext(); ) {
			if(it.next().tickShot())
				it.remove();
		}
	}

	public boolean isAlive(Player player) {
		return tanks.containsKey(player);
	}

	public int getWidth() {
		return map[0].length;
	}

	public int getHeight() {
		return map.length;
	}

	public Collection<Tank> getVisibleTanks(Player player) {
		Collection<Tank> visibleTanks = new ArrayList<>();
		
		Tank tank = tanks.get(player);
		
		for(Direction direction : Direction.values()) {
			int x = tank.getX();
			int y = tank.getY();
			
			while(!isWall(x = direction.moveX(x), y = direction.moveY(y))) {
				Tank spottedTank = getTank(x, y);
				if(spottedTank != null) {
					visibleTanks.add(spottedTank);
				}
			}
		}
		
		return visibleTanks;
	}

	void writeWorld(ByteBuffer data) {
		byte buffer = 0;
		int index = 0;
		
		for(boolean[] row : map) {
			for(boolean isWall : row) {
				if(isWall) {
					buffer |= 1 << index++;
					if(index == 8) {
						index = 0;
						data.put(buffer);
						buffer = 0;
					}
				}
			}
		}
		
		if(index != 0) {
			data.put(buffer);
		}
	}

	void spawnTank(Player player) {
		int x, y;
		
		//it's lazy but if there are enough tanks to make a difference then something crazy is happening anyway
		do {
			x = (int) (Math.random() * getWidth());
			y = (int) (Math.random() * getHeight());
		} while(isWall(x, y) || getTank(x, y) != null);
		
		Tank tank = new Tank(x, y, player, this);
		tanks.put(player, tank);
		tankMap[y][x] = tank;
	}

	void displayWorld(DisplayHandler handler) {
		char[][] display = new char[getHeight()][getWidth()];
		
		for(int x = 0; x < getWidth(); x++) {
			for(int y = 0; y < getHeight(); y++) {
				if(isWall(x, y))
					display[y][x] = '#';
				else {
					Tank tank = getTank(x, y);
					if(tank != null) {
						display[y][x] = tank.getCharacter();
					} else
						display[y][x] = ' ';
				}
			}
		}
		
		for(Shot shot : shots) {
			display[shot.y][shot.x] = Shot.BULLET;
		}
		
		handler.putGrid(display);
		handler.print();
	}

	void removeTank(Tank tank) {
		tanks.remove(tank.getOwner());
		tankMap[tank.getY()][tank.getX()] = null;
	}

	void addToRewindList(Tank tank) {
		tanksToRollBack.add(tank);
	}

	Tank getTank(Player player) {
		return tanks.get(player);
	}

	public int getDataSize() {
		return (getWidth() * getHeight() - 1) / Byte.SIZE + 1;
	}
}
