import aima.core.agent.Action;
import aima.core.agent.impl.DynamicAction;
import aima.core.environment.eightpuzzle.EightPuzzleBoard;
import aima.core.environment.nqueens.NQueensBoard;
import aima.core.search.framework.HeuristicFunction;
import aima.core.search.framework.Search;
import aima.core.search.framework.SearchAgent;
import aima.core.search.framework.problem.*;
import aima.core.search.uninformed.IterativeDeepeningSearch;
import aima.core.util.datastructure.XYLocation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Tugas1A {
    public static void main(String[] args) {
        try {
            String strategy = args[0];
            String inputFilename = args[1];
            String outputFilename = args[2];

            File inputFile = new File(inputFilename);
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));

            // Initialize environment
            String input = reader.readLine();
            String[] splitted = input.split(",");
            int m = Integer.parseInt(splitted[0]);
            int n = Integer.parseInt(splitted[1]);

            // Initialize start position
            input = reader.readLine();
            splitted = input.split(",");
            XYLocation start = new XYLocation(parseIntegerWithOffset(splitted[0]),
                                              parseIntegerWithOffset(splitted[1]));

            Environment env = new Environment(m, n, start);

            // Initialize items
            input = reader.readLine();
            splitted = input.split(" ");
            List<XYLocation> items = parsePositions(splitted);

            // Initialize obstacles
            input = reader.readLine();
            splitted = input.split(" ");
            List<XYLocation> obstacles = parsePositions(splitted);

            env.setBoard(items, obstacles);

            Problem problem = new Problem(env,
                    new JarvisActionsFunction(),
                    new JarvisResultFunction(),
                    new JarvisGoalTest(),
                    new JarvisStepCostFunction());

            Search search = null;

            if (strategy.equals("ids"))
                search = new IterativeDeepeningSearch();
            else if (strategy.equals("a*"))
                search = new IterativeDeepeningSearch();

            SearchAgent agent = new SearchAgent(problem, search);
            List<Action> actions = agent.getActions();

            // Print result
            StringBuilder sb = new StringBuilder();
            String nl = System.lineSeparator();
            sb.append((int) Double.parseDouble(agent.getInstrumentation().getProperty("pathCost"))).append(nl);
            sb.append(agent.getInstrumentation().getProperty("nodesExpanded")).append(nl);

            int actionsSize = actions.size();
            for (int i = 0; i < actionsSize; i++) {
                JarvisAction ja = (JarvisAction) actions.get(i);
                sb.append(ja.getName());
                if (i < actionsSize - 1)
                    sb.append(nl);
            }

            List<String> lines = Arrays.asList(sb.toString());

            Path outputFile = Paths.get(outputFilename);
            Files.write(outputFile, lines, Charset.forName("UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<XYLocation> parsePositions(String[] inputPositions) {
        List<XYLocation> positions = new ArrayList<XYLocation>(inputPositions.length);

        for (String position : inputPositions) {
            String[] positionSplitted = position.substring(1, position.length() - 1).split(",");
            positions.add(new XYLocation(
                    parseIntegerWithOffset(positionSplitted[0]),
                    parseIntegerWithOffset(positionSplitted[1])));

        }

        return positions;
    }

    private static int parseIntegerWithOffset(String input) {
        return Integer.parseInt(input) - 1;
    }
}

class Environment {
    int[][] squares;
    XYLocation position;
    int m;
    int n;

    /**
     * Membuat environment dengan ukuran m baris dan n kolom.
     * Urutan baris dan kolom dimulai dari 0.
     *
     * Keterangan:
     * 0  = tidak ada apapun
     * 1  = terdapat barang (item)
     * -1 = terdapat penghalang (obstacle)
     */
    public Environment(int m, int n, XYLocation position) {
        this.m = m;
        this.n = n;

        squares = new int[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                squares[i][j] = 0;
            }
        }

        this.position = position;
    }

    /**
     * Mengosongkan environment
     */
    public void clear() {
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                squares[i][j] = 0;
            }
        }
    }

    /**
     * Mengisi environment dengan items dan obstacles
     * TODO: Mungkin bisa pake foreach
     */
    public void setBoard(List<XYLocation> items, List<XYLocation> obstacles) {
        clear();

        for (int i = 0; i < items.size(); i++) {
            addItemAt(items.get(i));
        }

        for (int i = 0; i < obstacles.size(); i++) {
            addObstacleAt(obstacles.get(i));
        }
    }

    public int getRowSize() {
        return m;
    }

    public int getColumnSize() {
        return n;
    }

    public XYLocation getPosition() {
        return position;
    }

    public void movePosition(XYLocation l) {
        if (isValidPosition(l)) {
            position = l;
        } else {
            System.out.println("Something wrong happens!");
        }
    }

    /**
     * Menambahkan item ke posisi l
     */
    public void addItemAt(XYLocation l) {
        if (!itemExistsAt(l)) {
            squares[l.getXCoOrdinate()][l.getYCoOrdinate()] = 1;
        }
    }

    /**
     * Menambahkan obstace ke posisi l
     */
    public void addObstacleAt(XYLocation l) {
        if (!obstacleExistsAt(l)) {
            squares[l.getXCoOrdinate()][l.getYCoOrdinate()] = -1;
        }
    }

    public void removeItemFrom(XYLocation l) {
        if (squares[l.getXCoOrdinate()][l.getYCoOrdinate()] == 1) {
            squares[l.getXCoOrdinate()][l.getYCoOrdinate()] = 0;
        }
    }

    /**
     * Mengembalikan List yang berisi posisi seluruh barang
     */
    public List<XYLocation> getItemPositions() {
        ArrayList<XYLocation> result = new ArrayList<XYLocation>();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (itemExistsAt(i, j))
                    result.add(new XYLocation(i, j));
            }
        }
        return result;
    }

    public List<XYLocation> getObstaclePositions() {
        ArrayList<XYLocation> result = new ArrayList<XYLocation>();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (obstacleExistsAt(i, j))
                    result.add(new XYLocation(i, j));
            }
        }
        return result;
    }

    public boolean agentExistsAt(XYLocation l) {
        return l.equals(position);
    }

    public boolean itemExistsAt(XYLocation l) {
        return itemExistsAt(l.getXCoOrdinate(), l.getYCoOrdinate());
    }

    private boolean itemExistsAt(int x, int y) {
        return squares[x][y] == 1;
    }

    public boolean obstacleExistsAt(XYLocation l) {
        return obstacleExistsAt(l.getXCoOrdinate(), l.getYCoOrdinate());
    }

    private boolean obstacleExistsAt(int x, int y) {
        return squares[x][y] == -1;
    }

    public boolean outOfBound(XYLocation l) {
        return outOfBound(l.getXCoOrdinate(), l.getYCoOrdinate());
    }

    public boolean outOfBound(int x, int y) {
        return x < 0 || x >= m || y < 0 || y >= n;
    }

    public boolean isValidPosition(XYLocation l) {
        return !outOfBound(l) && !obstacleExistsAt(l);
    }

    /**
     * Mengecek apakah masih ada barang di lab yang belum diambil
     */
    public boolean hasItemsleft() {
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (squares[i][j] == 1) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        for (int x = 0; x < m; x++) {
            for (int y= 0; y < n; y++) {
                if (agentExistsAt(new XYLocation(x, y)))
                    buf.append('J');
                else if (itemExistsAt(x, y))
                    buf.append('o');
                else if (obstacleExistsAt(x, y))
                    buf.append('x');
                else
                    buf.append('-');
            }
            buf.append("\n");
        }
        return buf.toString();
    }
}

class JarvisGoalTest implements GoalTest {
    public boolean isGoalState(Object state) {
        Environment environment = (Environment) state;
        return !environment.hasItemsleft();
    }
}

class JarvisActionsFunction implements ActionsFunction {
    public Set<Action> actions(Object state) {
        Environment env = (Environment) state;

        Set<Action> actions = new LinkedHashSet<Action>();
        XYLocation jarvis = env.position;

        if (env.itemExistsAt(jarvis)) {
            actions.add(new JarvisAction(JarvisAction.PICK, jarvis));
        } else {
            XYLocation newLocation = new XYLocation(jarvis.getXCoOrdinate() - 1, jarvis.getYCoOrdinate());
            if (env.isValidPosition(newLocation)) {
                actions.add(new JarvisAction(JarvisAction.MOVE_UP, newLocation));
            }

            newLocation = new XYLocation(jarvis.getXCoOrdinate() + 1, jarvis.getYCoOrdinate());
            if (env.isValidPosition(newLocation)) {
                actions.add(new JarvisAction(JarvisAction.MOVE_DOWN, newLocation));
            }

            newLocation = new XYLocation(jarvis.getXCoOrdinate(), jarvis.getYCoOrdinate() - 1);
            if (env.isValidPosition(newLocation)) {
                actions.add(new JarvisAction(JarvisAction.MOVE_LEFT, newLocation));
            }

            newLocation = new XYLocation(jarvis.getXCoOrdinate(), jarvis.getYCoOrdinate() + 1);
            if (env.isValidPosition(newLocation)) {
                actions.add(new JarvisAction(JarvisAction.MOVE_RIGHT, newLocation));
            }
        }

        return actions;
    }
}

class JarvisResultFunction implements ResultFunction {
    public Object result(Object s, Action a) {
        if (a instanceof JarvisAction) {
            JarvisAction ja = (JarvisAction) a;
            Environment env = (Environment) s;
            Environment newEnv = new Environment(env.getRowSize(), env.getColumnSize(), env.getPosition());
            newEnv.setBoard(env.getItemPositions(), env.getObstaclePositions());

            if (ja.getName() == JarvisAction.PICK)
                newEnv.removeItemFrom(ja.getLocation());
            else if (ja.getName() == JarvisAction.MOVE_UP ||
                    ja.getName() == JarvisAction.MOVE_RIGHT ||
                    ja.getName() == JarvisAction.MOVE_DOWN ||
                    ja.getName() == JarvisAction.MOVE_LEFT)
                newEnv.movePosition(ja.getLocation());
            s = newEnv;
        }

        return s;
    }
}

/**
 * Fungsi heuristik yang akan mengembalikan jarak terdekat
 * Jarvis dengan item yang ada menggunakan manhattan distance
 */
class JarvisHeuristicFunction implements HeuristicFunction {
//        for (int i = 1; i < 9; i++) {
//            XYLocation loc = board.getLocationOf(i);
//            retVal += evaluateManhattanDistanceOf(i, loc);
//        }
//        return retVal;
//
//    }
//
//    public int evaluateManhattanDistanceOf(int i, XYLocation loc) {
//        int retVal = -1;
//        int xpos = loc.getXCoOrdinate();
//        int ypos = loc.getYCoOrdinate();
//        switch (i) {
//
//            case 1:
//                retVal = Math.abs(xpos - 0) + Math.abs(ypos - 1);
//                break;
//            case 2:
//                retVal = Math.abs(xpos - 0) + Math.abs(ypos - 2);
//                break;
//            case 3:
//                retVal = Math.abs(xpos - 1) + Math.abs(ypos - 0);
//                break;
//            case 4:
//                retVal = Math.abs(xpos - 1) + Math.abs(ypos - 1);
//                break;
//            case 5:
//                retVal = Math.abs(xpos - 1) + Math.abs(ypos - 2);
//                break;
//            case 6:
//                retVal = Math.abs(xpos - 2) + Math.abs(ypos - 0);
//                break;
//            case 7:
//                retVal = Math.abs(xpos - 2) + Math.abs(ypos - 1);
//                break;
//            case 8:
//                retVal = Math.abs(xpos - 2) + Math.abs(ypos - 2);
//                break;
//
//        }
//        return retVal;
//    }

    public double h(Object state) {
        Environment env = (Environment) state;
        XYLocation position = env.getPosition();
        List<XYLocation> itemPositions = env.getItemPositions();
        for (XYLocation itemPosition : itemPositions) {
            manhattanDis
        }

        return board.getNumberOfAttackingPairs();
    }
}

class JarvisAction extends DynamicAction {
    public static final String MOVE_UP = "ATAS";
    public static final String MOVE_RIGHT = "KANAN";
    public static final String MOVE_DOWN = "BAWAH";
    public static final String MOVE_LEFT = "KIRI";
    public static final String PICK = "AMBIL";

    public static final String ATTRIBUTE_JARVIS_LOC = "location";

    public JarvisAction(String type, XYLocation loc) {
        super(type);
        setAttribute(ATTRIBUTE_JARVIS_LOC, loc);
    }

    public XYLocation getLocation() {
        return (XYLocation) getAttribute(ATTRIBUTE_JARVIS_LOC);
    }

    public int getX() {
        return getLocation().getXCoOrdinate();
    }

    public int getY() {
        return getLocation().getYCoOrdinate();
    }
}

class JarvisStepCostFunction implements StepCostFunction {
    public double c(Object stateFrom, Action action, Object stateTo) {
        JarvisAction ja = (JarvisAction) action;
        return ja.getName() == JarvisAction.PICK ? 0 : 1;
    }
}
