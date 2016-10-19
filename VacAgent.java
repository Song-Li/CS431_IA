package sol315;
import agent.Action;
import agent.Agent;
import agent.Percept;
import vacworld.*;
import java.io.*;
import java.util.Arrays;
/**
 * the basic strategy has 3 steps:
 * 	1, find the wall
 *  2, follow the wall and find all the edges
 *  3, use BFS to touch all of the unknown points
 * all of the map is stored in a int array. use the last 9 binary 
 * @author sol315
 *
 */
public class VacAgent extends Agent {
	//the state of every gird is present by binary
	//visited|inner|up|right|down|left|dirt|obj 1 space 0|known
	//finally up|right|down|left are useless
	private int[][] map = new int[11][11]; //it should be all 0s
	private int[][] change = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
	//toward = 0 means up, 1 means right, 2 means down, 3 means left
	private int posx = 5, posy = 5, toward = 0,startx, starty, nl = 0, nr = 0;
	private int stage = 0, inner = -1, currStep = -1, startToward;
	private int[][] actions = new int[2][50];
	private boolean newGird = false, started = false;
	
	private boolean canGo(int d){
		int tx = posx + change[d][0], ty = posy + change[d][1];
		if((map[tx][ty] & 1) == 0 || (map[tx][ty] & 2) != 0)//known and not obs
			return false;
		return true;
	}
	private boolean knownRight(){
		int tx = posx + change[(toward + 1) % 4][0], ty = posy + change[(toward + 1) % 4][1];
		return (map[tx][ty] & 2) != 0 ; // known as obs
	}
	private Action Go(int tx, int ty){
		posy = ty;
		posx = tx;
		newGird = true;
		return new GoForward();
	}
	private Action Left(){
		toward = (toward + 3) % 4;
		return new TurnLeft();
	}
	private Action Right(){
		toward = (toward + 1) % 4;
		return new TurnRight();
	}
	private int touchRight(int i, int j){
		for(;j < 11;++ j) 
			if((map[i][j] & 1) != 0) return 0;
		return 1;
	}
	private int touchDown(int i, int j){
		for(;i < 11;++ i)
			if((map[i][j] & 1) != 0) return 0;
		return 1;
	}
	private int touchLeft(int i, int j){
		for(;j >= 0;-- j)
			if((map[i][j] & 1) != 0) return 0;
		return 1;
	}
	private int touchUp(int i, int j){
		for(;i >= 0;-- i)
			if((map[i][j] & 1) != 0) return 0;
		return 1;
	}
	//check every node to see if this node is inside the map and unknown
	//If the top, left, right down direction have known node, this is a inner node
	private void getInner(){
		inner = 0;
		for(int i = 0;i < 11;++ i){
			for(int j = 0;j < 11;++ j){
				if((map[i][j] & 256) == 0 && (map[i][j] & 2) == 0 && touchRight(i,j) + touchLeft(i,j) + touchUp(i,j) + touchDown(i,j) == 0){ // not visited, not wall
					inner ++;
					map[i][j] |= 128; //mark the node as inner node
				}
			}
		}
	}
	private boolean outHere(){
		for(int i = 0;i < 4;++ i){
			int tx = posx + change[i][0], ty = posy + change[i][1];
			if((map[tx][ty] & 1) == 0 || canGo(i)) return true;
		}
		return false;
	}
	
	private boolean inarea(int x, int y){
		return x >= 0 && y >= 0 && x < 11 && y < 11 && (map[x][y] & 2) == 0;
	}
	private int getDir(){
		int dx = actions[0][currStep] - posx, dy = actions[1][currStep] - posy;
		for(int i = 0;i < 4;++ i)
			if(dx == change[i][0] && dy == change[i][1])
				return i;
		return -1;
	}
	//in the stage 2, use BFS get the next touchable unknown node
	private void BFS(){
		currStep = 0;
		int que[][] = new int[3][10000];
		int head = 0, tail = 0, tx, ty;
		boolean found = false;
		int visited[][] = new int[11][11];
		que[0][tail] = posx;
		que[1][tail] = posy;
		que[2][tail ++] = -1;
		while(head != tail){
			for(int j = 0;j < 4;++ j){
				int i = (toward + j) % 4; //go forward is the first priority
				tx = que[0][head] + change[i][0];
				ty = que[1][head] + change[i][1];
				if(inarea(tx, ty) && visited[tx][ty] == 0){
					que[0][tail] = tx;
					que[1][tail] = ty;
					que[2][tail ++] = head;
					visited[tx][ty] = 1;
					if((map[tx][ty] & 128) != 0 && (map[tx][ty] & 256) == 0){
						found = true;
						break;
					}
				}
			}
			if(found) break;
			head ++;
		}
		if(head == tail){// no inner point touchable
			inner = 0;
		}
		tail --;
		while(que[2][tail] != -1){
			actions[0][currStep] = que[0][tail];
			actions[1][currStep ++] = que[1][tail];
			tail = que[2][tail];
		}
		currStep --;
	}

	@Override
	public void see(Percept p) {
		VacPercept vp = (VacPercept)p;
		int tx = posx + change[toward][0], ty = posy + change[toward][1];

		map[tx][ty] |= 1;
		if(vp.seeDirt()) //dirt 0000101
			map[posx][posy] |= 5;
		if(vp.seeObstacle()) //obstacle 0000011
			map[tx][ty] |= 3;
		return ;
	}

	@Override
	public Action selectAction() {
		for(int i = 0;i < 4;++ i){
			int tx = posx + change[i][0] * 5, ty = posy + change[i][1] * 5;
			if(inarea(tx, ty))
				map[tx][ty] |= 2;//mark the upper bound of wall, and it's unknown
		}
		if(!outHere()) stage = 2;
		map[posx][posy] |= 256; //visited
		if((map[posx][posy] & 128) != 0) { //mark inner grid as visited
			map[posx][posy] -= 128;
			inner --;
		}
		int tx = posx + change[toward][0], ty = posy + change[toward][1];
		if((map[tx][ty] & 128) != 0 && (map[tx][ty] & 2) != 0) { //mark inner grid as visited
			map[tx][ty] -= 128;
			inner --;
			BFS();
		}
		if(started && toward == startToward && startx == posx && starty == posy) {
			stage = 2;
			if(nl < 3) {
				stage = 0;// used for case to make sure we find the wall
				started = false;
			}
		}
		if((map[posx][posy] & 4) != 0){// has dirt
			map[posx][posy] ^= 4; //clear the dirt
			return new SuckDirt();
		}
		if(stage == 0){
			if(canGo(toward))
				return Go(tx, ty);
			stage = 1;
			startx = posx;
			starty = posy;
			startToward = (toward + 3) % 4; //turn left
			return Left();
		}else if(stage == 1){
			if(newGird && !knownRight()){
				newGird = false;
				return Right(); //new position turn right
			}
			if(canGo(toward)) {
				started = true;
				return Go(tx, ty);
			}
			else {
				nl ++;// used for case to make sure we find the wall
				return Left();
			}
		}else if(stage == 2){
			if(inner == -1) {
				getInner();
			}
			if(currStep == -1)//currStep is the position of the queue
				BFS();
			if(inner == 0) return new ShutOff();
			int goSpace = getDir();
			if(Math.abs(goSpace - toward) == 2) return Right();
			else if(goSpace - toward == -1 || goSpace - toward == 3) return Left();
			else if(goSpace - toward == 1 || goSpace - toward == -3) return Right();
			else {
				currStep --;
				return Go(tx, ty);  
			}
		}
		return new GoForward();// should never be here
	}

	@Override
	public String getId() {
		return "sol315";
	}
}