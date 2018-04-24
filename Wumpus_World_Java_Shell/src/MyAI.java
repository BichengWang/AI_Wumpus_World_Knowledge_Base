// ======================================================================

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

// FILE:        MyAI.java
//
// AUTHOR:      Abdullah Younis
//
// DESCRIPTION: This file contains your agent class, which you will
//              implement. You are responsible for implementing the
//              'getAction' function and any helper methods you feel you
//              need.
//
// NOTES:       - If you are having trouble understanding how the shell
//                works, look at the other parts of the code, as well as
//                the documentation.
//
//              - You are only allowed to make changes to this portion of
//                the code. Any changes to other portions of the code will
//                be lost when the tournament runs your code.
// ======================================================================

public class MyAI extends Agent {

	/**************** MyWorld ****************/
	
	/**
	 * State: following the AI textbook to descript the possible states
	 *
	 * @author wangbicheng VALID: yes; SATISFIED: maybe; UNSATISFIED: not;
	 */
	enum State {
		YES, MAYBE, NO
	};

	/**
	 * Cell: define the cell on the world
	 *
	 * @author wangbicheng
	 *
	 */
	private class Cell {
		private int x;
		private int y;
		private boolean visited = false; // visited
		private State safe = State.MAYBE; // safety
		private State stench = State.MAYBE; // stench
		private State breeze = State.MAYBE; // breeze
		private State pit = State.MAYBE; // pit
		private State wumpus = State.MAYBE; // wumpus
		private State wall = State.MAYBE; // wall

		Cell(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}

	private final static int MAX_ROW = 10; // possible row
	private final static int MAX_COL = 10; // possible col
	private final static int NEI_NUM = 4; // neighbor number

	// NORTH EAST SOUTH WEST
	private final static int[] dx = { 1, 0, -1, 0 };
	private final static int[] dy = { 0, 1, 0, -1 };

	/**
	 * MyWorld: saving knowledge base, checking available cell
	 *
	 * @author wangbicheng
	 *
	 */
	private class MyWorld {

		private Cell[][] cells;
		private Cell wumpusCell; // cell point to wumpus
		private boolean wumpusFound = false; // found the wumpus
		private boolean wumpusDead = false; // wumpus dead

		public MyWorld() {
			cells = new Cell[MAX_ROW][MAX_COL];
			for (int i = 0; i < MAX_ROW; i++) {
				for (int j = 0; j < MAX_COL; j++) {
					cells[i][j] = new Cell(i, j);
				}
			}

			// initial first cell
			Cell cell = cells[0][0];
			cell.pit = State.NO;
			cell.wumpus = State.NO;
			cell.safe = State.YES;
			cell.wall = State.NO;
			wumpusCell = null;
		}

		/**
		 * printWorld: print the current world states
		 */
		public void printWorld() {
			for (int i = 0; i < MAX_ROW; i++) {
				for (int j = 0; j < MAX_COL; j++) {
					Cell cur = cells[i][j];
					// print breeze
					if (cur.breeze == State.YES) {
						// System.out.print("bY");
					} else if (cur.breeze == State.MAYBE) {
						// System.out.print("b?");
					} else {
						// System.out.print("bN");
					}
					if (cur.stench == State.YES) {
						// System.out.print("sY");
					} else if (cur.stench == State.MAYBE) {
						// System.out.print("s?");
					} else {
						// System.out.print("sN");
					}
					if (cur.pit == State.YES) {
						// System.out.print("pY");
					} else if (cur.pit == State.MAYBE) {
						// System.out.print("p?");
					} else {
						// System.out.print("pN");
					}
					if (cur.wumpus == State.YES) {
						// System.out.print("wY");
					} else if (cur.wumpus == State.MAYBE) {
						// System.out.print("w?");
					} else {
						// System.out.print("wN");
					}
					if (cur.safe == State.YES) {
						// System.out.print("okY");
					} else if (cur.safe == State.MAYBE) {
						// System.out.print("ok?");
					} else {
						// System.out.print("okN");
					}
					if (cur.visited) {
						// System.out.print("vY");
					} else {
						// System.out.print("vN");
					}
					// System.out.print("\t\t");
				}
				// System.out.println();
			}
		}

		/**
		 * saveStatus: save current status into knoledge base, and update knowledge
		 * base.
		 *
		 * @param myAgent
		 * @param stench
		 * @param breeze
		 * @param glitter
		 * @param bump
		 * @param scream
		 */
		public void saveStatus(MyAgent myAgent, boolean stench, boolean breeze, boolean glitter, boolean bump,
		                       boolean scream) {
			if (scream) {
				clearWumpus();
			}

			if (bump) {// mark wall
				markWall(myAgent.x, myAgent.y, myAgent.dir);
			}

			Cell cell = getCell(myAgent.x, myAgent.y);
			if (cell == null || cell.visited) {
				if (cell == null) {
					// System.out.println("Exception: null cell");
				}
				return;
			}

			cell.visited = true;
			if (!isWumpusDead()) {
				markStench(cell, stench);
			}
			markBreeze(cell, breeze);
		}

		/**
		 * check if current cell is safe to o
		 *
		 * @param x
		 * @param y
		 * @return cell is safe or may not
		 */
		public boolean isCellAvaliable(int x, int y) {
			// // // // System.out.print("x:" + x + "y:" + y + "avaliable ?");

			Cell cell = getCell(x, y);
			if (cell == null) {
				// // // // // System.out.println("N");
				return false;
			}

			if (cell.visited) {
				// // // System.out.println("isCellAvaliable Exception: check cell is not edge");
				return false;
			}

			markCellSafe(cell);

			if (cell.safe == State.YES) {
				// // // // System.out.println("Y");
				return true;
			}

			// no need to do it
			// if(cell.breeze == State.YES) {
			// checkWumpus(cell);
			// }
			// if(cell.stench == State.YES) {
			// checkPit(cell);
			// }
			// if (cell.safe == State.YES) {
			// // // // System.out.println("Y");
			// return true;
			// }

			// // // System.out.println("N");
			return false;
		}

		/**
		 * getNeighborCell:
		 *
		 * @param cell
		 * @param number
		 * @return the Nth neighbor of cell. If not, return null
		 */
		public Cell getNeighborCell(Cell cell, int number) {
			int x = cell.x + dx[number];
			int y = cell.y + dy[number];
			Cell res = getCell(x, y);
			return res;
		}

		public Cell[] getNeighborCells(Cell cell) {
			Cell[] res = new Cell[4];
			for (int i = 0; i < 4; i++)
				res[i] = getNeighborCell(cell, i);
			return res;
		}

		/**
		 * getCell:
		 *
		 * @param x
		 * @param y
		 * @return cell
		 */
		public Cell getCell(int x, int y) {
			if (x < 0 || y < 0 || x >= MAX_ROW || y >= MAX_COL) {
				return null;
			}
			Cell res = cells[x][y];
			if (res.wall == State.YES) {
				return null;
			}
			return res;
		}

		/**
		 * isVisited:
		 *
		 * @param x
		 * @param y
		 * @return
		 */
		public boolean isVisited(int x, int y) {
			Cell res = getCell(x, y);
			if (res == null) {
				// // // System.out.println("visited exception");
				return false;
			}
			return res.visited;
		}

		public boolean isWumpusDead() {
			return this.wumpusDead;
		}

		public boolean isWumpusFound() {
			return this.wumpusFound;
		}

		public Cell getWumpusCell() {
			return this.wumpusCell;
		}

		/**
		 * if it is possible kill wumpus will generate new road
		 *
		 * @return false: wumpus not found || wumpus dead wumpus cell may or must be pit
		 *         wumpus is near from wall true: others
		 */
		public boolean needKillWumpus() {
			if (!this.wumpusFound || this.wumpusDead) {
				return false;
			}
			Cell wumpus = getWumpusCell();
			if (wumpus.pit != State.NO) {
				return false;
			}
			// if neighbors is not visited and possible safe, return true
			for (int i = 0; i < NEI_NUM; i++) {
				Cell nei = getNeighborCell(wumpus, i);
				if (nei != null && nei.visited == false && nei.safe != State.NO) {
					return true;
				}
			}
			return false;
		}

		/**
		 * markBreeze: mark breeze in current cell once found then, according to current
		 * cell's breeze to inference the neighbors if they are possible pit.
		 *
		 * @param cell
		 * @param breeze
		 */
		private void markBreeze(Cell cell, boolean breeze) {
			if (breeze) {
				if (cell.breeze == State.MAYBE) {
					cell.breeze = State.YES;
					markNeiPit(cell);
				} else if (cell.breeze == State.NO) {
					// System.out.println("markBreeze Exception: breeze true false");
				}
			} else {
				if (cell.breeze == State.MAYBE) {
					cell.breeze = State.NO;
					markNeiPit(cell);
				} else if (cell.breeze == State.YES) {
					// System.out.println("markBreeze Exception: breeze false true");
				}
			}
			return;
		}

		/**
		 * markStench: mark stench in current cell once found then, according to current
		 * cell's stench to inference the neighbors if they are possible pit.
		 *
		 * @param cell
		 * @param stench
		 */
		private void markStench(Cell cell, boolean stench) {
			if (stench) {
				if (cell.stench == State.MAYBE) {
					cell.stench = State.YES;
					markNeiWumpus(cell);
				} else if (cell.stench == State.NO) {
					// System.out.println("markStench Exception: mark stench yes no");
				}
			} else {
				if (cell.stench == State.MAYBE) {
					cell.stench = State.NO;
					markNeiWumpus(cell);
				} else if (cell.stench == State.YES) {
					// System.out.println("markStench Exception: mark stench no yes");
				}
			}
			return;
		}

		/**
		 *
		 * @param cell
		 * @param breeze
		 */
		private void markNeiPit(Cell cell) {
			// cell maybe breeze cannot cause inference
			if (cell.breeze == State.MAYBE) {
				// System.out.println("markNeiPit Exception: impossible cause mark neighbor pit by current possible breeze");
			}

			Cell[] neis = new Cell[4];
			for (int i = 0; i < NEI_NUM; i++) {
				neis[i] = getNeighborCell(cell, i);
			}

			if (cell.breeze == State.NO) {// cell breeze NO inference
				for (int i = 0; i < NEI_NUM; i++) {
					if (neis[i] == null || neis[i].visited) {
						continue;
					}
					neis[i].pit = State.NO;
				}
			} else {// cell breeze YES inference
				for (int i = 0; i < NEI_NUM; i++) {
					if (neis[i] == null || neis[i].visited) {
						continue;
					}
					checkPitAndMark(neis[i]);
				}
			}
			return;
		}

		/**
		 * markNeiWumpus: once make sure cell's stench, markNeiWumpus will update
		 * neighbor information
		 *
		 * @param cell
		 */
		private void markNeiWumpus(Cell cell) {
			// if stench is MAYBE, it cannot cause neighbor wumpus inference
			if (cell.stench == State.MAYBE) {
				// System.out.println("markNeiWumpus Exception: cell.stench == State.MAYBE");
				return;
			}

			// wumpus already found or dead
			if (this.wumpusDead) {
				// System.out.println("markNeiWumpus Exception: wumpusFound");
				return;
			}

			Cell[] neis = new Cell[4];
			for (int i = 0; i < NEI_NUM; i++) {
				neis[i] = getNeighborCell(cell, i);
			}

			if (cell.stench == State.NO) {// mark if stench is NO
				for (int i = 0; i < NEI_NUM; i++) {
					if (neis[i] == null || neis[i].visited) {
						continue;
					}
					markWumpus(neis[i], false);
				}
			} else {// else if stench is YES
				for (int i = 0; i < NEI_NUM; i++) {
					Cell cur = neis[i];
					if (cur == null || cur.visited) {
						continue;
					}
					if (isWumpusFound()) {
						break;
					}
					checkWumpusAndMark(cur);
				}
			}
			return;
		}

		/**
		 * checkPitAndMark: check cell is possible a pit according to neighbors.
		 *
		 * @param cell
		 */
		private void checkPitAndMark(Cell cell) {
			// if pit YES or NO
			if (cell.pit != State.MAYBE) {
				return;
			}

			// if with wumpus, pit is impossible
			// if (cell.wumpus == State.YES) {
			// markPit(cell, false);
			// return;
			// }

			// check pit == maybe
			Cell[] neis = new Cell[4];

			for (int i = 0; i < NEI_NUM; i++) {
				Cell nei = getNeighborCell(cell, i);
				neis[i] = nei;
				if (nei == null) {
					continue;
				}
				// check if neighor no breeze, cur is impossible pit
				if (nei.breeze == State.NO) {
					markPit(cell, false);
					return;
				}
			}

			// check if neighbor have breeze
			for (int i = 0; i < NEI_NUM; i++) {
				Cell nei = neis[i];
				if (nei == null)
					continue;
				if (nei.breeze == State.YES) {
					// check if the breeze is from cur cell
					if (isBreezeFromCell(nei, cell) == State.YES) {
						markPit(cell, true);
						return;
					}
				}
			}
			// pit MAYBE, do nothing
			return;
		}

		/**
		 * checkWumpusAndMark:
		 *
		 * @param cell
		 */
		private void checkWumpusAndMark(Cell cell) {
			if (cell.wumpus != State.MAYBE) {
				return;
			}

			// check maybe
			Cell[] neis = new Cell[4];

			// cache two possible stench
			int firstStench = -1;
			int secondStench = -1;
			int stenchNum = 0;
			for (int i = 0; i < NEI_NUM; i++) {
				// get neighbor
				Cell nei = getNeighborCell(cell, i);
				neis[i] = nei;
				if (nei == null) {
					continue;
				}
				// if any no stench nei, cur is not wumpus
				if (nei.stench == State.NO) {
					markWumpus(cell, false);
					return;
				} else if (nei.stench == State.YES) {
					if (stenchNum == 0) {
						firstStench = i;
					} else {
						secondStench = i;
					}
					stenchNum++;
				}
			}

			// impossible
			if (stenchNum == 0) {
				// System.out.println("checkWumpusAndMark Exception: stenchNum == 0 at" + cell.x + ":" + cell.y);
				return;
			}

			// if 1 nei
			if (stenchNum == 1) {
				isStenchFromCell(neis[firstStench], cell);
				return;
			}

			// if 3 nei is stench
			if (stenchNum == 3) {
				markWumpus(cell, true);
				return;
			}

			// if 2 stench
			// if opposite position
			if ((secondStench - firstStench) == 2) {
				markWumpus(cell, true);
				return;
			}

			// if next position
			Cell diag = getDiagCell(neis[firstStench], cell, neis[secondStench]);
			if (diag.wumpus == State.NO) {
				markWumpus(cell, true);
				return;
			}

			return;
		}

		/**
		 * isBreezeFromCell: if cur cause the breeze of breezeCell
		 *
		 * @param breezeCell
		 * @param cur
		 * @return if cur possible pit without mark cur cell
		 */
		private State isBreezeFromCell(Cell breezeCell, Cell cur) {
			Cell[] neis = new Cell[4];
			for (int i = 0; i < NEI_NUM; i++) {
				// get neighbor
				neis[i] = getNeighborCell(breezeCell, i);
				if (neis[i] == null || neis[i] == cur) {
					continue;
				}
				// if nei pit maybe or yes, cur pit maybe
				if (neis[i].pit != State.NO) {
					return State.MAYBE;
				}
			}
			// if no other possible cause, cur is pit
			return State.YES;
		}

		/**
		 * isStenchFromCell: inference if the cell cause stenchCell has stench maybe it
		 * is impossible get at yes result
		 *
		 * @param stenchCell
		 * @param cur
		 * @return the inference state without mark wumpus
		 */
		private State isStenchFromCell(Cell stenchCell, Cell cur) {
			Cell[] neis = new Cell[4];
			for (int i = 0; i < NEI_NUM; i++) {
				// get neighbor
				neis[i] = getNeighborCell(stenchCell, i);
				if (neis[i] == null || neis[i] == cur) {
					continue;
				}
				// if nei wumpus maybe or yes, cur wumpus maybe
				if (neis[i].wumpus != State.NO) {
					return State.MAYBE;
				}
			}
			// if no other possible cause, cur is wumpus
			return State.YES;
		}

		/**
		 * markPit: mark pit, and inference
		 *
		 * @param cell
		 * @param pit
		 */
		private void markPit(Cell cell, boolean pit) {
			if (pit) {
				// // no possible both wumpus and pit
				// if (cell.wumpus == State.MAYBE) {
				// markWumpus(cell, false);
				// } else if (cell.wumpus == State.YES) {
				// // // System.out.println("markPit Exception: wumpus pit yes");
				// }
				if (cell.pit == State.MAYBE) {
					setPit(cell);
				} else if (cell.pit == State.NO) {
					// System.out.println("markPit Exception: pit no yes");
				}
			} else {
				if (cell.pit == State.MAYBE) {
					cell.pit = State.NO;
				} else if (cell.pit == State.YES) {
					// System.out.println("markPit Exception: pit yes no");
				}
			}
		}

		/**
		 * markWumpus: mark wumpus, and inference
		 *
		 * @param cell
		 * @param wumpus
		 */
		private void markWumpus(Cell cell, boolean wumpus) {
			if (wumpus) {
				// // no possible both wumpus and pit
				// if (cell.pit == State.MAYBE) {
				// markPit(cell, false);
				// } else if (cell.pit == State.YES) {
				// // // System.out.println("markWumpus Exception: wumpus pit yes");
				// }
				if (cell.wumpus == State.MAYBE) {
					setWumpus(cell);
				} else if (cell.wumpus == State.NO) {
					// System.out.println("markWumpus Exception: wumpus no yes");
				}
			} else {
				if (cell.wumpus == State.MAYBE) {
					cell.wumpus = State.NO;
				} else if (cell.wumpus == State.YES) {
					// System.out.println("markWumpus Exception: wumpus yes no");
				}
			}
		}

		private void setPit(Cell cell) {
			// if (cell.pit == State.NO) {
			// // // System.out.println("setPit Exception: pit is NO");
			// }
			// if (cell.wumpus == State.YES) {
			// // // System.out.println("setPit Exception: wumpus is YES");
			// }
			// cell.wumpus = State.NO;
			cell.pit = State.YES;
			for (int i = 0; i < NEI_NUM; i++) {
				Cell nei = getNeighborCell(cell, i);
				if (nei == null) {
					continue;
				}
				if (nei.breeze == State.NO) {
					// // System.out.println("setPit Exception: neighbor breeze is NO");
				}
				nei.breeze = State.YES;
			}
			return;
		}

		private void setWumpus(Cell cell) {
			// if (cell.wumpus != State.NO) {
			// // // System.out.println("setWumpus Exception: wumpus is NO");
			// }
			// if (cell.pit == State.YES) {
			// // // System.out.println("setWumpus Exception: pit is YES");
			// }

			// set all other not wumpus
			for (int i = 0; i < MAX_ROW; i++) {
				for (int j = 0; j < MAX_COL; j++) {
					Cell cur = getCell(i, j);
					if (cur == null || cur == cell) {
						continue;
					}
					cur.wumpus = State.NO;
				}
			}
			cell.wumpus = State.YES;
			this.wumpusCell = cell;
			// set neighbors stench
			for (int i = 0; i < NEI_NUM; i++) {
				Cell nei = getNeighborCell(cell, i);
				if (nei == null) {
					continue;
				}
				if (nei.stench == State.NO) {
					// // System.out.println("setWumpus Exception: neighbor stench is NO");
				}
				nei.stench = State.YES;
			}
			this.wumpusFound = true;
		}

		private void markNeiSafe(Cell cell) {
			for (int i = 0; i < NEI_NUM; i++) {
				// get neighbor
				Cell nei = getNeighborCell(cell, i);
				if (nei == null)
					continue;
				markCellSafe(nei);
			}
			return;
		}

		private void markCellSafe(Cell cell) {
			if (cell.pit == State.NO && cell.wumpus == State.NO) {
				cell.safe = State.YES;
				return;
			} else if (cell.pit == State.YES || cell.wumpus == State.YES) {
				cell.safe = State.NO;
			}
		}

		/**
		 * getDiagCell:
		 *
		 * @param cell
		 * @param cur
		 * @param cell2
		 * @return digital cell
		 */
		private Cell getDiagCell(Cell cell, Cell cur, Cell cell2) {
			int x = cell.x;
			if (x == cur.x) {
				x = cell2.x;
			} else if (x == cell2.x) {
				x = cur.x;
			}
			int y = cell.y;
			if (y == cur.y) {
				y = cell2.y;
			} else if (y == cell2.y) {
				y = cur.y;
			}
			return cells[x][y];
		}

		/**
		 * clearWumpus: once wumpus dead clear wumpus and stench in the world and mark
		 * the current place no wumpus no pit, neighbor no stench no breeze
		 */
		private void clearWumpus() {
			for (int i = 0; i < MAX_ROW; i++) {
				for (int j = 0; j < MAX_COL; j++) {
					Cell cell = getCell(i, j);
					if (cell == null) {
						continue;
					}
					cell.stench = State.NO;
					cell.wumpus = State.NO;
				}
			}
			this.wumpusDead = true;
		}

		/**
		 * markWall: mark the wall cells
		 *
		 * @param x
		 * @param y
		 * @param dir
		 */
		private void markWall(int x, int y, Direction dir) {
			int startX = 0;
			int startY = 0;
			if (dir == Direction.NORTH) {
				startX = x + 1;
			} else if (dir == Direction.EAST) {
				startY = y + 1;
			} else {
				// DO NOTHING
				return;
			}
			for (int i = startX; i < MAX_ROW; i++) {
				for (int j = startY; j < MAX_COL; j++) {
					Cell cur = cells[i][j];
					cur.visited = false;
					cur.safe = State.NO;
					cur.pit = State.NO;
					cur.wumpus = State.NO;
					cur.wall = State.YES;
				}
			}
		}
	}

	private MyWorld myWorld;

	/**************** Agent status ****************/
	enum Direction {
		NORTH, EAST, SOUTH, WEST
	};

	/**
	 * MyAgent: AI Agent
	 *
	 * @author Chenyang Ren
	 *
	 */
	class MyAgent {
		int x = 0;
		int y = 0;
		Direction dir = Direction.EAST;
		// Action list
		private int[][] fourdir = new int[][] { { 1, 0 }, { 0, 1 }, { 0, -1 }, { -1, 0 } };
		private Set<String> edges = new HashSet<String>();
		private Queue<Action> queue = new LinkedList<Action>();
		private int[][][] totaldir = new int[][][] { { { 1, 0 }, { 0, -1 }, { 0, 1 }, { -1, 0 } },
			{ { 0, 1 }, { 1, 0 }, { -1, 0 }, { 0, -1 } }, { { -1, 0 }, { 0, 1 }, { 0, -1 }, { 1, 0 } },
			{ { 0, -1 }, { -1, 0 }, { 1, 0 }, { 0, 1 } }
		};

		public void checkbump(boolean bump) {
			if (bump) {
				if (myAgent.dir == Direction.NORTH) {
					myAgent.x--;
				} else if (myAgent.dir == Direction.EAST) {
					myAgent.y--;
				} else if (myAgent.dir == Direction.SOUTH) {
					myAgent.x++;
				} else {
					myAgent.y++;
				}
				queue.clear();
			}
		}

		public Action getAction(boolean glitter) {
			// update edges
			edges.remove(myAgent.x + ":" + myAgent.y);
			for (int i = 0; i < 4; i++) {
				Cell neighborcell = myWorld.getCell(myAgent.x + fourdir[i][0], myAgent.y + fourdir[i][1]);
				if (neighborcell != null && !neighborcell.visited) {
					edges.add(neighborcell.x + ":" + neighborcell.y);
				}
			}

			int tempx = myAgent.x, tempy = myAgent.y, tempcost = 0;
			Direction tempdir = myAgent.dir;
			Action[][] actions = new Action[][] { { Action.FORWARD }, { Action.TURN_LEFT, Action.FORWARD },
				{ Action.TURN_RIGHT, Action.FORWARD }, { Action.TURN_LEFT, Action.TURN_LEFT, Action.FORWARD }
			};
			List<Queue<String>> list = new LinkedList<Queue<String>>();
			List<Queue<String>> list2 = new LinkedList<Queue<String>>();
			HashMap<String, List<Action>> map = new HashMap<String, List<Action>>();
			HashMap<String, Direction> dirmap = new HashMap<String, Direction>();

			// grab glitter
			if (glitter) {
				queue.clear();
				boolean ava = false;

				while (!ava) {
					if (myWorld.getCell(tempx, tempy) != null) {
						if (tempx == 0 && tempy == 0) {
							queue.add(Action.GRAB);
							queue.addAll(map.get(tempx + ":" + tempy));
							queue.add(Action.CLIMB);
							// myWorld.printWorld();
							changestatus(queue.peek());
							return queue.poll();
						} else if (myWorld.getCell(tempx, tempy).visited) {
							int[][] nextdir;
							if (tempdir == Direction.NORTH) {
								nextdir = totaldir[0];
							} else if (tempdir == Direction.EAST) {
								nextdir = totaldir[1];
							} else if (tempdir == Direction.SOUTH) {
								nextdir = totaldir[2];
							} else {
								nextdir = totaldir[3];
							}
							for (int i = 0; i < 4; i++) {
								int newx = tempx + nextdir[i][0], newy = tempy + nextdir[i][1];
								if (!map.containsKey(newx + ":" + newy) && myWorld.getCell(newx, newy) != null) {
									int curcost;
									if (i < 2) {
										curcost = i + 1;
									} else if (i == 2) {
										curcost = i;
									} else {
										curcost = i - 1;
									}

									if (tempcost + curcost >= list.size() || list.get(tempcost + curcost) == null) {
										int index = tempcost + curcost;
										while (index >= list.size()) {
											list.add(new LinkedList<String>());
										}
									}
									list.get(tempcost + curcost).add(newx + ":" + newy);

									List<Action> tempactions;
									if (map.containsKey(tempx + ":" + tempy)) {
										tempactions = new LinkedList<Action>(map.get(tempx + ":" + tempy));
									} else {
										tempactions = new LinkedList<Action>();
									}
									tempactions.addAll(Arrays.asList(actions[i]));
									map.put(newx + ":" + newy, tempactions);

									if (i == 0) {
										dirmap.put(newx + ":" + newy, tempdir);
									} else if (i == 1 || i == 2) {
										dirmap.put(newx + ":" + newy, changedir(actions[i][0], tempdir));
									} else {
										dirmap.put(newx + ":" + newy,
										           changedir(actions[i][1], changedir(actions[i][0], tempdir)));
									}
								}
							}
						}
					}

					for (int i = tempcost; i < list.size(); i++) {

						if (list.get(i) != null && !list.get(i).isEmpty()) {
							String[] newpoint = list.get(i).poll().split(":");
							tempx = Integer.parseInt(newpoint[0]);
							tempy = Integer.parseInt(newpoint[1]);
							tempdir = dirmap.get(tempx + ":" + tempy);
							tempcost = i;
							break;
						}
					}
				}
			}
			// check stack is empty
			if (!queue.isEmpty()) {
				// myWorld.printWorld();
				if (queue.peek() == Action.SHOOT) {
					flag = true;
				}
				changestatus(queue.peek());
				return queue.poll();
			}

			boolean ava = false;
			while (!ava) {
				// for search the next lowest cost cell
				if (myWorld.getCell(tempx, tempy) != null) {
					if (edges.contains(tempx + ":" + tempy)) {
						ava = myWorld.isCellAvaliable(tempx, tempy);
						if (ava) {
							queue.addAll(map.get(tempx + ":" + tempy));
							// myWorld.printWorld();
							changestatus(queue.peek());
							return queue.poll();
						}
					} else if (myWorld.getCell(tempx, tempy).visited) {
						int[][] nextdir;
						if (tempdir == Direction.NORTH) {
							nextdir = totaldir[0];
						} else if (tempdir == Direction.EAST) {
							nextdir = totaldir[1];
						} else if (tempdir == Direction.SOUTH) {
							nextdir = totaldir[2];
						} else {
							nextdir = totaldir[3];
						}
						for (int i = 0; i < 4; i++) {
							int newx = tempx + nextdir[i][0], newy = tempy + nextdir[i][1];
							if (!map.containsKey(newx + ":" + newy) && myWorld.getCell(newx, newy) != null) {
								int curcost;
								if (i < 2) {
									curcost = i + 1;
								} else if (i == 2) {
									curcost = i;
								} else {
									curcost = i - 1;
								}

								List<Queue<String>> templist;
								if (newx == tempx) {
									templist = list;
								} else {
									templist = list2;
								}

								if (tempcost + curcost >= templist.size() || templist.get(tempcost + curcost) == null) {
									int index = tempcost + curcost;
									while (index >= templist.size()) {
										templist.add(new LinkedList<String>());
									}
								}
								templist.get(tempcost + curcost).add(newx + ":" + newy);

								List<Action> tempactions;
								if (map.containsKey(tempx + ":" + tempy)) {
									tempactions = new LinkedList<Action>(map.get(tempx + ":" + tempy));
								} else {
									tempactions = new LinkedList<Action>();
								}
								tempactions.addAll(Arrays.asList(actions[i]));
								map.put(newx + ":" + newy, tempactions);

								if (i == 0) {
									dirmap.put(newx + ":" + newy, tempdir);
								} else if (i == 1 || i == 2) {
									dirmap.put(newx + ":" + newy, changedir(actions[i][0], tempdir));
								} else {
									dirmap.put(newx + ":" + newy,
									           changedir(actions[i][1], changedir(actions[i][0], tempdir)));
								}
							}
						}
					}
				}

				boolean find = false;
				for (int i = tempcost; i < list.size(); i++) {
					if (list.get(i) != null && !list.get(i).isEmpty()) {
						String[] newpoint = list.get(i).poll().split(":");
						tempx = Integer.parseInt(newpoint[0]);
						tempy = Integer.parseInt(newpoint[1]);
						tempdir = dirmap.get(tempx + ":" + tempy);
						tempcost = i;
						find = true;
						break;
					}
				}
				if (!find) {
					for (int i = 0; i < list2.size(); i++) {
						if (list2.get(i) != null && !list2.get(i).isEmpty()) {
							String[] newpoint = list2.get(i).poll().split(":");
							tempx = Integer.parseInt(newpoint[0]);
							tempy = Integer.parseInt(newpoint[1]);
							tempdir = dirmap.get(tempx + ":" + tempy);
							tempcost = i;
							find = true;
							break;
						}
					}
				}

				if (!find) {
					if (myWorld.isWumpusFound() && !myWorld.isWumpusDead() && myWorld.needKillWumpus()) {
						queue.clear();
						Cell wumpuscell = myWorld.getWumpusCell();
						ava = false;

						tempx = myAgent.x;
						tempy = myAgent.y;
						tempcost = 0;
						tempdir = myAgent.dir;
						map.clear();
						list.clear();
						dirmap.clear();
						while (!ava) {
							if (myWorld.getCell(tempx, tempy) != null) {
								if (tempx == wumpuscell.x && tempy == wumpuscell.y) {
									List<Action> temp = map.get(tempx + ":" + tempy);
									temp.remove(temp.size() - 1);
									queue.addAll(temp);
									queue.add(Action.SHOOT);
									changestatus(queue.peek());
									return queue.poll();
								} else if (myWorld.getCell(tempx, tempy).visited) {
									int[][] nextdir;
									if (tempdir == Direction.NORTH) {
										nextdir = totaldir[0];
									} else if (tempdir == Direction.EAST) {
										nextdir = totaldir[1];
									} else if (tempdir == Direction.SOUTH) {
										nextdir = totaldir[2];
									} else {
										nextdir = totaldir[3];
									}
									for (int i = 0; i < 4; i++) {
										int newx = tempx + nextdir[i][0], newy = tempy + nextdir[i][1];
										if (!map.containsKey(newx + ":" + newy) && (myWorld.getCell(newx, newy) != null
										        || (newx == wumpuscell.x && newy == wumpuscell.y))) {
											int curcost;
											if (i < 2) {
												curcost = i + 1;
											} else if (i == 2) {
												curcost = i;
											} else {
												curcost = i - 1;
											}

											if (tempcost + curcost >= list.size()
											        || list.get(tempcost + curcost) == null) {
												int index = tempcost + curcost;
												while (index >= list.size()) {
													list.add(new LinkedList<String>());
												}
											}
											list.get(tempcost + curcost).add(newx + ":" + newy);

											List<Action> tempactions;
											if (map.containsKey(tempx + ":" + tempy)) {
												tempactions = new LinkedList<Action>(map.get(tempx + ":" + tempy));
											} else {
												tempactions = new LinkedList<Action>();
											}
											tempactions.addAll(Arrays.asList(actions[i]));
											map.put(newx + ":" + newy, tempactions);

											if (i == 0) {
												dirmap.put(newx + ":" + newy, tempdir);
											} else if (i == 1 || i == 2) {
												dirmap.put(newx + ":" + newy, changedir(actions[i][0], tempdir));
											} else {
												dirmap.put(newx + ":" + newy,
												           changedir(actions[i][1], changedir(actions[i][0], tempdir)));
											}
										}
									}
								}
							}

							for (int i = tempcost; i < list.size(); i++) {

								if (list.get(i) != null && !list.get(i).isEmpty()) {
									String[] newpoint = list.get(i).poll().split(":");
									tempx = Integer.parseInt(newpoint[0]);
									tempy = Integer.parseInt(newpoint[1]);
									tempdir = dirmap.get(tempx + ":" + tempy);
									tempcost = i;
									break;
								}
							}
						}
					} else {
						if (map.containsKey("0:0")) {
							queue.addAll(map.get("0:0"));
						}
						queue.add(Action.CLIMB);
						changestatus(queue.peek());
						return queue.poll();
					}
				}
			}
			// no way to choice
			// bfs come back to start point
			// stack
			// myWorld.printWorld();
			changestatus(queue.peek());
			return queue.poll();
		}

		private void changestatus(Action action) {
			if (action == Action.FORWARD) {
				if (myAgent.dir == Direction.NORTH) {
					myAgent.x++;
				} else if (myAgent.dir == Direction.EAST) {
					myAgent.y++;
				} else if (myAgent.dir == Direction.SOUTH) {
					myAgent.x--;
				} else {
					myAgent.y--;
				}
			}
			if (action == Action.TURN_RIGHT || action == Action.TURN_LEFT) {
				myAgent.dir = changedir(action, myAgent.dir);
			}
		}

		private Direction changedir(Action action, Direction dir) {
			if (action == Action.TURN_RIGHT) {
				if (dir == Direction.EAST) {
					return Direction.SOUTH;
				} else if (dir == Direction.SOUTH) {
					return Direction.WEST;
				} else if (dir == Direction.NORTH) {
					return Direction.EAST;
				} else {
					return Direction.NORTH;
				}
			} else {
				if (dir == Direction.EAST) {
					return Direction.NORTH;
				} else if (dir == Direction.SOUTH) {
					return Direction.EAST;
				} else if (dir == Direction.NORTH) {
					return Direction.WEST;
				} else {
					return Direction.SOUTH;
				}
			}
		}
	}

	private MyAgent myAgent;

	// private boolean forward;
	// private int step;
	// enum Direction {
	// NORTH, EAST, SOUTH, WEST
	// };
	// private Direction dir;
	boolean flag = false;

	public MyAI() {
		// ======================================================================
		// YOUR CODE BEGINS
		// ======================================================================
		this.myWorld = new MyWorld();
		this.myAgent = new MyAgent();
		// ======================================================================
		// YOUR CODE ENDS
		// ======================================================================
	}

	public Action getAction(boolean stench, boolean breeze, boolean glitter, boolean bump, boolean scream) {
		// ======================================================================
		// YOUR CODE BEGINS
		// ======================================================================
		myAgent.checkbump(bump);
		myWorld.saveStatus(myAgent, stench, breeze, glitter, bump, scream);
		Action temp = myAgent.getAction(glitter);
//		if (flag == true) {
//			// System.out.println(temp);
//			if (temp != Action.SHOOT) {
//				flag = false;
//			}
//		}
		return temp;
		// ======================================================================
		// YOUR CODE ENDS
		// ======================================================================
	}
	// ======================================================================
	// YOUR CODE BEGINS
	// ======================================================================

	// ======================================================================
	// YOUR CODE ENDS
	// ======================================================================}
	// ======================================================================
	// YOUR CODE BEGINS
	// ======================================================================

	// ======================================================================
	// YOUR CODE ENDS
	// ======================================================================
}
