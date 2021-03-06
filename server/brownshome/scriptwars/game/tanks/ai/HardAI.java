package brownshome.scriptwars.game.tanks.ai;

import java.io.IOException;
import java.util.*;

import brownshome.scriptwars.game.tanks.*;

public class HardAI {
	private TankAPI api;
	private Random rand = new Random();
	private LinkedHashSet<Coordinates> priority = new LinkedHashSet<>();
	private Coordinates last = null;
	
	public static void main(String[] args) throws IOException {
		new HardAI(Integer.parseInt(args[0]));
	}
	
	private HardAI(int ID) throws IOException {
		api = new TankAPI(ID, "www.script-wars.com", "Hard AI");
		boolean first = true;
		
		while(api.nextTick()) {
			if(!api.isAlive()) {
				continue;
			}
			
			if(first) {
				fillPriorityList();
				first = false;
			}
			
			if(api.getCurrentPosition().equals(last)) {
				priority.clear();
				fillPriorityList();
			}
			
			last = api.getCurrentPosition();
			
			pathTo(getNextCoord());
			
			if(api.getAmmo() > 0)
				shootBadGuys();
			
			avoidBullets();

			//api.printAction();
		}
	}
	
	private Coordinates getNextCoord() {
		return priority.iterator().next();
	}
	
	private void flagCoord(Coordinates coord) {
		priority.remove(coord);
		priority.add(coord);
	}
	
	private void fillPriorityList() {
		List<Coordinates> array = new ArrayList<>();
		
		World map = api.getMap();
		array.addAll(map.getAmmoPickups());
		
		Collections.shuffle(array);
		priority.addAll(array);
	}
	
	private void avoidBullets() {
		Coordinates coord = api.getCurrentPosition();
		
		if(api.getAction() == Action.MOVE) {
			if(!isSafe(api.getDirection().move(coord))) {
				api.doNothing();
			} else {
				return;
			}
		}
		
		if(isSafe(coord))
			return;
		
		for(Direction direction : Direction.values()) {
			Coordinates newCoord = direction.move(coord);
			World world = api.getMap();
			
			if(!world.isWall(newCoord) && world.getTank(newCoord) == null && isSafe(newCoord)) {
				api.move(direction);
				return;
			}
		}
		
		return;
	}

	private boolean isSafe(Coordinates newCoord) {
		for(Shot shot : api.getVisibleShots()) {
			Coordinates coord = shot.getDirection().move(shot.getPosition());
			for(int x = 0; x < Shot.SPEED; x++) {
				if(coord.equals(newCoord))
					return false;
				
				coord = shot.getDirection().move(coord);
			}
		}
		
		for(Direction direction : Direction.values()) {
			if(api.getMap().getTank(direction.move(newCoord)) != null) {
				return false;
			}
		}
		
		return true;
	}
	
	private void shootBadGuys() {
		int minDistance = Integer.MAX_VALUE;
		Coordinates me = api.getCurrentPosition();
		Direction direction = null;
		
		for(Tank tank : api.getVisibleTanks()) {
			int distance = getDistance(tank);
			Coordinates other = tank.getPosition();
			Direction dir = Direction.getDirection(other.getX() - me.getX(), other.getY() - me.getY());
		
			if(dir != null && distance < minDistance) {
				direction = dir;
				minDistance = distance;
			}
		}
		
		if(direction == null)
			return;
		
		api.shoot(direction);
	}
	
	private int getDistance(Tank tank) {
		return Math.abs(tank.getPosition().getX() - api.getCurrentPosition().getX()) +
				Math.abs(tank.getPosition().getY() - api.getCurrentPosition().getY());
	}
	
	private void pathTo(Coordinates aim) {
		if(aim.equals(api.getCurrentPosition())) {
			return;
		}
		
		World map = api.getMap();
		Set<Coordinates> traversed = new HashSet<>();
		Set<Coordinates> toTraverse = new HashSet<>();
		Set<Coordinates> traverse = new HashSet<>();
		traverse.add(aim);
		
		while(!traverse.isEmpty()) {
			for(Coordinates coord : traverse) {
				for(Direction dir : Direction.values()) {
					Coordinates next = dir.move(coord);
					
					if(api.getCurrentPosition().equals(next)) {
						api.move(dir.opposite());
						return;
					}
					
					if(!map.isWall(next) && map.getTank(next) == null && !traversed.contains(next)) {	
						toTraverse.add(next);
					}
				}
			}
			
			traversed.addAll(traverse);
			traverse = toTraverse;
			toTraverse = new HashSet<>();
		}
		
		//We can't find a way, go to another spot
		flagCoord(aim);
	}
}
