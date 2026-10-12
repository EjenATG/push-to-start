import java.util.Random;
import java.util.Scanner;
import java.util.ArrayList;

public class MazeGen
{
    public static void main(String[] args) {
        // Pathing :3
        Random rand = new Random();
        Scanner inp = new Scanner(System.in);

        System.out.print("Insert Maze Size = ");
        int side = inp.nextInt();
        boolean[][] maze = new boolean[side][side];
        ArrayList<ArrayList<Integer>> trail = new ArrayList<ArrayList<Integer>>();
        ArrayList<ArrayList<Integer>> deadends = new ArrayList<ArrayList<Integer>>();

        int px = 1;
        int py = rand.nextInt((side - 2) / 2) * 2 + 1;
        int curTrail = 0;

        int ex = px;
        int ey = py;

        maze[py][px] = true;

        while (true) {
            ArrayList<Integer> dirs = new ArrayList<Integer>();
            
            if (px<side-2) if (!maze[py][px+2]) dirs.add(0);
            if (py<side-2) if (!maze[py +2][px]) dirs.add(1);
            if (px>1) if (!maze[py][px-2]) dirs.add(2);
            if (py>1) if (!maze[py-2][px]) dirs.add(3);
            
            if (dirs.size() > 0) {
                switch(dirs.get(rand.nextInt(0,dirs.size()))) {
                    case 0:
                        px+=2;
                        maze[py][px] = true;
                        maze[py][px-1] = true;
                        break;
                    case 1:
                        py+=2;
                        maze[py][px] = true;
                        maze[py-1][px] = true;
                        break;
                    case 2:
                        px-=2;
                        maze[py][px] = true;
                        maze[py][px+1] = true;
                        break;
                    case 3:
                        py-=2;
                        maze[py][px] = true;
                        maze[py+1][px] = true;
                        break;
                }
                ArrayList<Integer> newTrail = new ArrayList<Integer>();
                newTrail.add(py);
                newTrail.add(px);
                trail.add(newTrail);
                curTrail = trail.size() - 1;
            } else {
                ArrayList<Integer> deadTrail = new ArrayList<Integer>();
                deadTrail.add(py);
                deadTrail.add(px);
                deadends.add(deadTrail);
                trail.remove(curTrail);
                if (trail.size() > 0) {
                    curTrail = rand.nextInt(0,trail.size());
                    py = trail.get(curTrail).get(0);
                    px = trail.get(curTrail).get(1);
                } else break;
            }
        }
        
        // Exit
        boolean[][] visited = new boolean[side][side];

        ArrayList<ArrayList<Integer>> queue = new ArrayList<ArrayList<Integer>>();
        deadends = new ArrayList<ArrayList<Integer>>();
        ArrayList<Integer> start = new ArrayList<Integer>();
        start.add(ey);
        start.add(ex);
        queue.add(start);

        while (queue.size() > 0) {
            ArrayList<Integer> current = queue.remove(0);
            int cy = current.get(0);
            int cx = current.get(1);

            if (visited[cy][cx]) continue;
            visited[cy][cx] = true;
            int neighbours = 0;

            if (cy > 0 && maze[cy - 1][cx]) {
                neighbours++;

                if (!visited[cy - 1][cx]) {
                    ArrayList<Integer> next = new ArrayList<Integer>();
                    next.add(cy - 1);
                    next.add(cx);
                    queue.add(next);
                }
            }

            if (cy < side - 1 && maze[cy + 1][cx]) {
                neighbours++;

                if (!visited[cy + 1][cx]) {
                    ArrayList<Integer> next = new ArrayList<Integer>();
                    next.add(cy + 1);
                    next.add(cx);
                    queue.add(next);
                }
            }

            if (cx > 0 && maze[cy][cx - 1]) {
                neighbours++;

                if (!visited[cy][cx - 1]) {
                    ArrayList<Integer> next = new ArrayList<Integer>();
                    next.add(cy);
                    next.add(cx - 1);
                    queue.add(next);
                }
            }

            if (cx < side - 1 && maze[cy][cx + 1]) {
                neighbours++;
                if (!visited[cy][cx + 1]) {
                    ArrayList<Integer> next = new ArrayList<Integer>();
                    next.add(cy);
                    next.add(cx + 1);
                    queue.add(next);
                }
            }

            if (neighbours == 1 && !(cy == ey && cx == ex)) {
                ArrayList<Integer> dead = new ArrayList<Integer>();
                dead.add(cy);
                dead.add(cx);
                deadends.add(dead);
            }
        }

        if (deadends.size() > 0) {
            int chosen = rand.nextInt(deadends.size());

            py = deadends.get(chosen).get(0);
            px = deadends.get(chosen).get(1);
        } else System.out.println("We're trapped in the digital circus fellas.");
     
        // Generation :O
        while (true) {

            try {
                if (System.getProperty("os.name").contains("Windows")) {
                    // Run CMD's cls command directly inside the terminal process
                    new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
                } else {
                    // Run Unix clear command
                    new ProcessBuilder("clear").inheritIO().start().waitFor();
                }
            } catch (Exception e) {
                // Fallback just in case
                System.out.print("\033[H\033[2J\033[3J");
                System.out.flush();
            }

            Display(maze, side, py, px, ey, ex);
            for (int o = 1; o < side + 2; o++) System.out.print("|||");
            String move = inp.nextLine();
            switch(move.trim().toLowerCase()) {
                case "w":
                    if (ey > 0) if (maze[ey-1][ex]) ey--;
                    break;
                case "a":
                    if (ex > 0) if (maze[ey][ex-1]) ex--;
                    break;
                case "s":
                    if (ey < side-1) if (maze[ey+1][ex]) ey++;
                    break;
                case "d":
                    if (ex < side-1) if (maze[ey][ex+1]) ex++;
                    break;
                default:
                    break;
            }
            if (ey == py && ex == px) {
                System.out.println("\n======================\n\n   Congratulations!\n\n======================");
                inp.close();
                break;
            }
        }
    }

    static boolean canCarve(boolean[][] maze, int x, int y, int dx, int dy) {
        int nx = x + dx * 2;
        int ny = y + dy * 2;

        if (nx < 0 || nx >= maze[0].length ||
            ny < 0 || ny >= maze.length) {
            return false;
        }

        return !maze[ny][nx];
    }

    public static void Display(boolean[][] maze, int side, int py, int px, int ey, int ex) {
        System.out.print("\033[0m");
        for (int i = 0; i < side; i++) {
            System.out.print("|||");
            for (int j = 1; j < side; j++) {
                if (maze[i][j]) {
                    if (i == py && j == px) {
                        System.out.print("\033[0;31m"+"-E-"+"\033[0m");
                    } else if (i == ey && j == ex) {
                        System.out.print("\033[0;32m"+"-U-"+"\033[0m");
                    } else {
                        System.out.print("   ");
                    }
                } else {
                    System.out.print("|||");
                }
            }
            System.out.println("|||");
        }
    }
}