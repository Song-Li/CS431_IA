package sol315;
import agent.Action;
import agent.Agent;
import agent.Percept;
import java.util.Random;
import pacworld.*;
import java.util.*;

public class PacAgent extends Agent {
	
	private class Position {
		public int x, y;
		public Position(int _x, int _y) {
			x = _x;
			y = _y;
		}
		public Position() {};
	}
	private Position pos = new Position();
	private int[][] map = new int [50][50];
	private Position des = new Position(-1,-1);
	private PacPercept pacPercept;
	private Position[] exploreDes = new Position[120];
	private Set<Position> pacLocation = new HashSet<Position>(); 
	private Set<Position> sendPacs = new HashSet<Position>(); 
	private final int threshhold = 1;
	private int status = -1; //-1 means communication, 0 means exploring, 1 means going to pac, 2 means carrying
	private int size, num_id,inf = 2147480000;
	private int startExplore = -1, endExplore = -1, num_agents = 0, num_cubes = 0;
	private boolean inited = false;
	private Random rand = new Random();

	//After this function , the position of this agent should be updated
	//This function will mark all of the agents' position
	private void markAgents(VisibleAgent[] agents) {
		for (int i = 0;i < agents.length;++ i) {
			if(agents[i].getId() == id) {
				pos.x = agents[i].getX();
				pos.y = agents[i].getY();
			}
		}
	}
	private void markPackages(VisiblePackage[] packages) {
		for(int i = 0;i < packages.length;++ i) {
			int tx = packages[i].getX(), ty = packages[i].getY();
			if(!inarea(tx, ty)) continue;
			map[tx][ty] += 2; //This grid has been minus 1 so we need to plus 2 here
			if(map[tx][ty] > threshhold) {
				sendPacs.add(new Position(tx, ty));
				addPac(new Position(tx, ty));
				map[tx][ty] = -inf;
			}
		}
	}
	private void setDes(int x,int y) {
		des.x = x;
		des.y = y;
	}
	private void print(Object obj) {
		System.out.println(obj);
	}
	private boolean inarea(int x,int y) {
		return x >= 0 && x < size && y >= 0 && y < size;
	}

	private Action explore() {
		Action action = goDes(false);
		if(action == null) {
			if(startExplore >= endExplore) setDes(rand.nextInt(50), rand.nextInt(50));
			else if(exploreDes[startExplore].x == des.x && exploreDes[startExplore].y == des.y) startExplore ++;
			if(startExplore >= endExplore) setDes(rand.nextInt(50), rand.nextInt(50));
			else des = exploreDes[startExplore];
			action = goDes(false);
		}
		return action;
	}
	
	private int getPacDir() {
		if(pos.x == des.x) {
			if(pos.y - des.y == 1) return 0;
			if(pos.y - des.y == -1) return 2;
		}else if(pos.y == des.y) {
			if(pos.x - des.x == 1) return 3;
			if(pos.x - des.x == -1) return 1;			
		}
		return -1;
	}
	
	private int avoidBump(int direction) {
		if(pacPercept.feelBump()) direction = rand.nextInt(4);
		return direction;
	}
	
	private Action goDes(boolean around) {
		if(!around && pos.x == des.x && pos.y == des.y) return null; 
		if(!inarea(des.x, des.y)) return null;
		if(around && getPacDir() != -1) return null;
		int move = -1, max = -1;
		int distance[] = {pos.y - des.y, des.x - pos.x, des.y - pos.y, pos.x - des.x};
		for(int i = 0;i < 4;++ i) {
			if(distance[i] > max) {
				max = distance[i];
				move = i;
			}
		}
		return new Move( avoidBump(move) );
	}
	
	//mark all 11x11 area minus 1
	private void markMap() {
		for(int i = 0;i < 11;++ i) {
			for(int j = 0;j < 11;++ j) {
				int x = pos.x + 5 - i;
				int y = pos.y + 5 - j;
				if(inarea(x,y)) map[x][y] -= 1;
			}
		}
	}
	private void removePac(Position pac) {
		for(Position pos : pacLocation) {
			if(pos.x == pac.x && pos.y == pac.y){
				pacLocation.remove(pos);
				break;
			}
		}
		for(Position pos : sendPacs) {
			if(pos.x == pac.x && pos.y == pac.y){
				sendPacs.remove(pos);
				break;
			}
		}
		map[pac.x][pac.y] = -inf;
	}
	private Position getNearestPac() {
		int distance = -1, min = inf;
		Position ret = new Position();
		for(Position i : pacLocation) {
			distance = Math.abs(i.x - pos.x) + Math.abs(i.y - pos.y);
			if(distance < min) {
				distance = min;
				ret.x = i.x;
				ret.y = i.y;
			}
		}
		return ret;
	}
	private Action sendPacLocation() {
		String res = "";
		for(Position location : sendPacs) {
			res += "" + location.x + ',' + location.y + ',' + (threshhold + 1) + ';';
		}
		if(res.length() == 0) return null;
		return new Say(res);
	}
	private void init() {
		for(int i = 5;i < 50;i += 5) exploreDes[num_cubes ++] = new Position(5, i);
		for(int i = 5;i < 50;i += 5) exploreDes[num_cubes ++] = new Position(i, 45);
		for(int i = 50;i > 0;i -= 5) exploreDes[num_cubes ++] = new Position(45, i);
		for(int i = 50;i > 0;i -= 5) exploreDes[num_cubes ++] = new Position(i, 5);
		startExplore = num_id * num_cubes / num_agents;
		endExplore = (num_id + 1) * num_cubes / num_agents;
		inited = true;
	}
	private Action occupied(Position location) {
		return new Say("" + location.x + ',' + location.y + ',' + -1 + ';');
	}
	private boolean addPac(Position location) {
		if(map[location.x][location.y] < -20) return false;
		for(Position pos : pacLocation) {
			if(pos.x == location.x && pos.y == location.y) return false;
		}
		pacLocation.add(location);
		return true;
	}
	
	public PacAgent(int id){
		this.id = "Agent" + id;
		num_id = id;
	}

	@Override
	public void see(Percept p) {
		PacPercept pp = (PacPercept)p;
		pacPercept = pp;
		String[] messages = pp.getMessages();
		for(String message : messages) {
			if(message.length() == 0) {
				num_agents ++;
				continue;
			}
			String[] curPac = message.split(";");
			for(String curr : curPac) {
				String[] currint = curr.split(",");
				int tx = Integer.parseInt(currint[0]), ty = Integer.parseInt(currint[1]),val = Integer.parseInt(currint[2]);
				if(val > threshhold) {
					addPac(new Position(tx, ty));
					map[tx][ty] += Integer.parseInt(currint[2]);
				}
				else removePac(new Position(tx,ty));
			}
		}
		size = pp.getWorldSize();
		markAgents( pp.getVisAgents() ); //After this function, this position should be updated
		markMap();
		markPackages( pp.getVisPackages() );
		VisiblePackage Holding = pp.getHeldPackage();
		if(Holding != null) {
			status = 2;
			setDes(Holding.getDestX(), Holding.getDestY());
		}else if(status == 2) {// by some mistake, seems holding sth but not. The status should be 0
			status = 0;
		}
	}
	
	@Override
	public Action selectAction() {
		Action action;

		if(status == -1) {
			status = 0;
			return new Say("");
		}
		if(!inited) init();
		if(status == 0) { //exploring
			if(pacLocation.size() != 0) { //have pacs to carry
				Position tdes = getNearestPac();
				if(Math.abs(pos.x - tdes.x) + Math.abs(pos.y - tdes.y) > 100) action = explore();
				else {
					des = tdes;
					action = occupied(des);
					removePac(des);
					status = 1;
					if(action != null) return action;
				}
			}
			action = explore();
		}else { //carrying
			if(sendPacs.size() != 0) { //found new pac when carrying
				action = sendPacLocation();
				sendPacs.clear();
				return action;
			}
			action = goDes(true);
			if(action == null) {
				if(status == 1) {
					status = 2;
					action = new Pickup(getPacDir());
				}else {
					status = 0;
					action = new Dropoff(getPacDir());
				}
			}
		}
		return action;
	}
}