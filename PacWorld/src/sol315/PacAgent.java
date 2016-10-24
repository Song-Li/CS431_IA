package sol315;
import agent.Action;
import agent.Agent;
import agent.Percept;
import java.util.Random;
import pacworld.*;
import java.util.*;

public class PacAgent extends Agent {
	//Tested range from 1 package to 100 packages with 1 destinations to 8 destinations. No bugs found, the average score around 550 to 650
	//For more than 150 packages, this program can rarely clear all the packages. For less than 100 packages and more than 5 agents, this program can almost clear the map
	//This agent has 2 sets, the packages set is used to store the found and aviliable packages. The sending set
	//is used to store the found but not sent to other agents' packages. 
	//1, broadcast a blank message to know how many agents we have
	//2, explore the 4 edges of the map. If in the explore we find any packages, this agent will:
	//	1, put this package to its own package set
	//	2, put this package to its sending set
	//In the explore stage, if an agent find the package set is not empty, this agent will pick the nearest one
	//and if the distance of this agent and the package IS SMALLER than a threshold, 
	//the agent will go to carry this package and tell everyone else this package has been carried
	//If this distance IS BIGGER than a threshold, this agent will explore go this package but not occupied
	//Once an agent found a package, the agent will share this package with every other agents
	//If after exploring the 4 edges, the program is not stoped, agents will explore the inner 4 edges. we will randomly explore the map
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
	//if we find a package, vote this position as positive. If this position is marked more than threshhold, this is a package
	private void markPackages(VisiblePackage[] packages) {
		for(int i = 0;i < packages.length;++ i) {
			int tx = packages[i].getX(), ty = packages[i].getY();
			if(!inarea(tx, ty)) continue;
			map[tx][ty] += 2; //This grid has been minus 1 so we need to plus 2 here
			if(map[tx][ty] > threshhold) {
				sendPacs.add(new Position(tx, ty));//This is a set of found and send packages
				addPac(new Position(tx, ty));//add this package to this position
				map[tx][ty] = -inf;
			}
		}
	}
	private void setDes(int x,int y) {
		des.x = x;
		des.y = y;
	}
	//This is a function to judge if a position is inside this map
	private boolean inarea(int x,int y) {
		return x >= 0 && x < size && y >= 0 && y < size;
	}
	//This function is used to explore the map. First all of the agents will split the four edges of the map, 
	//After that, all of the agents will randomly pick a destination and explore
	private Action explore() {
		Action action = goDes(false);
		if(action == null) {
			if(startExplore >= endExplore) setDes(rand.nextInt(29) + 11, rand.nextInt(29) + 11);
			else if(exploreDes[startExplore].x == des.x && exploreDes[startExplore].y == des.y) startExplore ++;
			if(startExplore >= endExplore) setDes(rand.nextInt(29) + 11, rand.nextInt(29) + 11);
			else des = exploreDes[startExplore];
			action = goDes(false);
		}
		return action;
	}
	//This function is used to get the right direction of package and destination
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
	//This function is used to avoid Bump, just randomly pick a direction and do it
	private int avoidBump(int direction) {
		if(pacPercept.feelBump()) direction = rand.nextInt(4);
		return direction;
	}
	//This function will pick a right move to the des by using the largest distance direction to the des
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
	//mark all 11x11 area minus 1. Which means, if this is a normal point, this position's vote will minus 1
	//In the function markPackages, we will add 2 to the package location
	private void markMap() {
		for(int i = 0;i < 11;++ i) {
			for(int j = 0;j < 11;++ j) {
				int x = pos.x + 5 - i;
				int y = pos.y + 5 - j;
				if(inarea(x,y)) map[x][y] -= 1;
			}
		}
	}
	//Remove a package from both the package set of this agent and the send set of this agent
	private void removePac(Position pac) {
		map[pac.x][pac.y] = -inf;
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
	//Find the nearest package from the package set
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
	//Function to send all the send package set by a format
	private Action sendPacLocation() {
		String res = "";
		for(Position location : sendPacs) {
			res += "" + location.x + ',' + location.y + ',' + (threshhold + 1) + ';';
		}
		if(res.length() == 0) return null;
		return new Say(res);
	}
	//The init explore route is the 4 edges. Put all of the points and the start position of every agents are different
	//Based on their agent number
	private void init() {
		for(int i = 3;i < 50;i += 5) exploreDes[num_cubes ++] = new Position(3, i);
		for(int i = 3;i < 50;i += 5) exploreDes[num_cubes ++] = new Position(i, 48);
		for(int i = 48;i > 0;i -= 5) exploreDes[num_cubes ++] = new Position(48, i);
		for(int i = 48;i > 0;i -= 5) exploreDes[num_cubes ++] = new Position(i, 3);
		
		startExplore = num_id * num_cubes / num_agents;
		endExplore = (num_id + 1) * num_cubes / num_agents;
		inited = true;
	}
	//If an agent want to carry a package, send the occupied message to everyone else
	private Action occupied(Position location) {
		return new Say("" + location.x + ',' + location.y + ',' + -1 + ';');
	}
	//Add package to the package set. 
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
				num_agents ++; //In the init round, everyone broadcast a blank message. If this message is blank, the number of agents should plus one
				continue;
			}
			String[] curPac = message.split(";");
			for(String curr : curPac) {
				String[] currint = curr.split(",");
				int tx = Integer.parseInt(currint[0]), ty = Integer.parseInt(currint[1]),val = Integer.parseInt(currint[2]);
				if(val > threshhold) {//This is a package
					addPac(new Position(tx, ty));//add this package to the package set
					map[tx][ty] += Integer.parseInt(currint[2]);//mark the map
				}
				else removePac(new Position(tx,ty));//If the value is -1, this is a occupied package. remove this package
			}
		}
		size = pp.getWorldSize();
		markAgents( pp.getVisAgents() ); //After this function, this position should be updated
		markMap();
		markPackages( pp.getVisPackages() );
		VisiblePackage Holding = pp.getHeldPackage();
		if(Holding != null) {//If the agent is holding a package, mark it and set the des
			status = 2;
			setDes(Holding.getDestX(), Holding.getDestY());
		}else if(status == 2) {// by some mistake, seems holding sth but not. The status should be 0
			status = 0;
		}
	}
	
	@Override
	public Action selectAction() {
		Action action;
		if(status == -1) {//At first, broadcast a blank message to let everyone know how many agents we have
			status = 0;
			return new Say("");
		}
		if(!inited) init();
		if(status == 0) { //exploring
			if(pacLocation.size() != 0) { //have pacs to carry
				Position tdes = getNearestPac();
				//This if is designed for if the distance is more than a number, give up it. But finally I accept all positions
				if(Math.abs(pos.x - tdes.x) + Math.abs(pos.y - tdes.y) > 80) {
					des = tdes;
					action = goDes(false);//explore the direction of the nearest package
				}
				else {
					des = tdes;
					action = occupied(des);//tell others this package is carried
					removePac(des);
					status = 1;
					if(action != null) return action;
				}
			}
			else action = explore();
		}else { //carrying
			if(sendPacs.size() != 0) { //found new pac when carrying
				action = sendPacLocation();// Send the packages this agent found to everyone else
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