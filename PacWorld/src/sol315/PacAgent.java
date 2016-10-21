package sol315;
import agent.Action;
import agent.Agent;
import agent.Percept;
import java.util.Random;
import pacworld.*;

import java.io.*;
import java.util.*;

public class PacAgent extends Agent {
	
	private class Position {
		public int x, y;
		public Position(int _x, int _y) {
			x = _x;
			y = _y;
		}
		public Position() {};
	    public String toString() {
	    	return "Agent " + id + ": Loc=(" + x + ", " + y + ")";
		}
	}
	private Position pos = new Position();
	private int[][] map = new int [50][50];
	private Position des = new Position(25,25);
	private int inf = 2147483647;
	private Position prePos = new Position(-1,-1);
	
	private Set<Position> pacLocation = new HashSet<Position>(); 
	private final int threshhold = 2;
	private int status = 0; //0 means exploring, 1 means going to pac, 2 means carrying
	private int size;

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
				map[tx][ty] = -inf;
				pacLocation.add(new Position(tx, ty));
			}
		}
	}
	private boolean inarea(int x,int y) {
		return x >= 0 && x < size && y >= 0 && y < size;
	}
	private void print(Object object) {
		System.out.println(object);
	}
	private Position findUnknown() {
		for(int i = 0;i < size;++ i) {
			for(int j = 0;j < size;++ j) {
				if(Math.abs(map[i][j]) < threshhold) {
					return new Position(i,j);
				}
			}
		}
		return new Position(25,25);
	}
	private Action explore() {
		Action action = goToDes();
		if(action == null) {
			des = findUnknown();
			action = goToDes();
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
		if(prePos.x == pos.x && prePos.y == pos.y) 
			direction = ( direction + 1 ) % 4; //If not moved last step, turn right
		return direction;
	}
	
	private Action goToDes() {
		int move = -1, max = -1;
		if(getPacDir() != -1) return null;
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
	
	public PacAgent(int id){
		this.id = "Agent" + id;
	}

	@Override
	public void see(Percept p) {
		PacPercept pp = (PacPercept)p;
		size = pp.getWorldSize();
		markAgents( pp.getVisAgents() ); //After this function, this position should be updated
		markMap();
		markPackages( pp.getVisPackages() );
		VisiblePackage Holding = pp.getHeldPackage();
		if(Holding != null) {
			des.x = Holding.getDestX();
			des.y = Holding.getDestY();
		}
	}
	
	@Override
	public Action selectAction() {
		Action action;
		if(pacLocation.size() != 0 && status == 0) {
			des = getNearestPac();
			removePac(des);
			//sendPacLocation();
			status = 1;
		}
		if(status == 0) {
			action = explore();
		}else {
			action = goToDes();
			if(action == null) {
				if(status == 1) {
					status = 2;
					prePos.x = prePos.y = -1;
					action = new Pickup(getPacDir());
				}else {
					status = 0;
					prePos.x = prePos.y = -1;
					action = new Dropoff(getPacDir());
				}
			}
		}
		prePos.x = pos.x;
		prePos.y = pos.y;
		return action;
	}
}
