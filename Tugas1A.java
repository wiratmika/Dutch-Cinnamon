import aima.core.agent.Action;
import aima.core.agent.impl.DynamicAction;
import aima.core.search.framework.HeuristicFunction;
import aima.core.search.framework.Search;
import aima.core.search.framework.SearchAgent;
import aima.core.search.framework.problem.*;
import aima.core.search.framework.qsearch.GraphSearch;
import aima.core.search.informed.AStarSearch;
import aima.core.search.uninformed.IterativeDeepeningSearch;
import aima.core.util.datastructure.XYLocation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Tugas1A {
    public static void main(String[] args) {
        try {
            String strategy = args[0];
            String inputFilename = args[1];
            String outputFilename = args[2];

            File inputFile = new File(inputFilename);
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));

            // Inisialisasi environment
            String input = reader.readLine();
            String[] splitted = input.split(",");
            int m = Integer.parseInt(splitted[0]);
            int n = Integer.parseInt(splitted[1]);

            // Inisialisasi start
            input = reader.readLine();
            splitted = input.split(",");
            XYLocation start = new XYLocation(parseIntegerWithOffset(splitted[0]),
                    parseIntegerWithOffset(splitted[1]));

            Environment env = new Environment(m, n, start);

            // Inisialisasi items
            input = reader.readLine();
            splitted = input.split(" ");
            List<XYLocation> items = parsePositions(splitted);

            // Inisialisasi obstacles
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
                search = new AStarSearch(new GraphSearch(), new JarvisHeuristicFunction());

            System.out.println(env);

            SearchAgent agent = new SearchAgent(problem, search);
            List<Action> actions = agent.getActions();
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

    /**
     * Mengembalikan list yang berisi posisi item/obstacle dari masukan
     */
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
    XYLocation jarvis;
    int m;
    int n;

    /**
     * Membuat environment dengan ukuran m baris dan n kolom serta posisi Jarvis
     * Urutan baris dan kolom dimulai dari 0
     * <p>
     * Keterangan:
     * 0  = tidak ada apapun
     * 1  = terdapat barang (item)
     * -1 = terdapat penghalang (obstacle)
     */
    public Environment(int m, int n, XYLocation jarvis) {
        this.m = m;
        this.n = n;

        squares = new int[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                squares[i][j] = 0;
            }
        }

        this.jarvis = jarvis;
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
     */
    public void setBoard(List<XYLocation> items, List<XYLocation> obstacles) {
        clear();

        for (XYLocation item : items) {
            addItemAt(item);
        }

        for (XYLocation obstacle : obstacles) {
            addObstacleAt(obstacle);
        }
    }

    public int getRowSize() {
        return m;
    }

    public int getColumnSize() {
        return n;
    }

    public XYLocation getJarvis() {
        return jarvis;
    }

    /**
     * Memindahkan Jarvis ke posisi baru
     */
    public void moveJarvis(XYLocation l) {
        jarvis = l;
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
     * Menambahkan obstacle ke posisi l
     */
    public void addObstacleAt(XYLocation l) {
        if (!obstacleExistsAt(l)) {
            squares[l.getXCoOrdinate()][l.getYCoOrdinate()] = -1;
        }
    }

    /**
     * Menghapus item di posisi l
     */
    public void removeItemFrom(XYLocation l) {
        if (squares[l.getXCoOrdinate()][l.getYCoOrdinate()] == 1) {
            squares[l.getXCoOrdinate()][l.getYCoOrdinate()] = 0;
        }
    }

    /**
     * Mengembalikan list yang berisi posisi seluruh item
     */
    public List<XYLocation> getItemPositions() {
        ArrayList<XYLocation> result = new ArrayList<XYLocation>(m * n);
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (itemExistsAt(i, j))
                    result.add(new XYLocation(i, j));
            }
        }
        return result;
    }

    /**
     * Mengembalikan list yang berisi posisi seluruh obstacle
     */
    public List<XYLocation> getObstaclePositions() {
        ArrayList<XYLocation> result = new ArrayList<XYLocation>(m * n);
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (obstacleExistsAt(i, j))
                    result.add(new XYLocation(i, j));
            }
        }
        return result;
    }

    public boolean jarvisExistsAt(XYLocation l) {
        return l.equals(jarvis);
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

    public boolean isValidLocation(XYLocation l) {
        return !outOfBound(l) && !obstacleExistsAt(l);
    }

    /**
     * Mengecek apakah masih ada item yang belum diambil
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
            for (int y = 0; y < n; y++) {
                if (jarvisExistsAt(new XYLocation(x, y)))
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
    /**
     * Goal state: ketika sudah tidak ada item yang tersisa di environment
     */
    public boolean isGoalState(Object state) {
        Environment env = (Environment) state;
        return !env.hasItemsleft();
    }
}

class JarvisActionsFunction implements ActionsFunction {
    /**
     * Action function:
     * - Ambil item (selalu dijalankan jika Jarvis berada di location yang terdapat item)
     * - Pindah ke atas
     * - Pindah ke kanan
     * - Pindah ke bawah
     * - Pindah ke kiri
     */
    public Set<Action> actions(Object state) {
        Environment env = (Environment) state;

        Set<Action> actions = new LinkedHashSet<Action>();
        XYLocation jarvis = env.jarvis;

        if (env.itemExistsAt(jarvis)) {
            actions.add(new JarvisAction(JarvisAction.PICK, jarvis));
        } else {
            XYLocation newLocation = new XYLocation(jarvis.getXCoOrdinate() - 1, jarvis.getYCoOrdinate());
            if (env.isValidLocation(newLocation)) {
                actions.add(new JarvisAction(JarvisAction.MOVE_UP, newLocation));
            }

            newLocation = new XYLocation(jarvis.getXCoOrdinate() + 1, jarvis.getYCoOrdinate());
            if (env.isValidLocation(newLocation)) {
                actions.add(new JarvisAction(JarvisAction.MOVE_DOWN, newLocation));
            }

            newLocation = new XYLocation(jarvis.getXCoOrdinate(), jarvis.getYCoOrdinate() - 1);
            if (env.isValidLocation(newLocation)) {
                actions.add(new JarvisAction(JarvisAction.MOVE_LEFT, newLocation));
            }

            newLocation = new XYLocation(jarvis.getXCoOrdinate(), jarvis.getYCoOrdinate() + 1);
            if (env.isValidLocation(newLocation)) {
                actions.add(new JarvisAction(JarvisAction.MOVE_RIGHT, newLocation));
            }
        }

        return actions;
    }
}

class JarvisResultFunction implements ResultFunction {
    /**
     * Result function:
     * - Ambil item         : barang akan dihapus dari location tersebut
     * - Pindah ke atas     : Jarvis pindah ke posisi (x - 1, y)
     * - Pindah ke kanan    : Jarvis pindah ke posisi (x, y + 1)
     * - Pindah ke bawah    : Jarvis pindah ke posisi (x + 1, y)
     * - Pindah ke kiri     : Jarvis pindah ke posisi (x, y - 1)
     */
    public Object result(Object s, Action a) {
        if (a instanceof JarvisAction) {
            JarvisAction ja = (JarvisAction) a;
            Environment env = (Environment) s;
            Environment newEnv = new Environment(env.getRowSize(), env.getColumnSize(), env.getJarvis());
            newEnv.setBoard(env.getItemPositions(), env.getObstaclePositions());

            if (ja.getName() == JarvisAction.PICK)
                newEnv.removeItemFrom(ja.getLocation());
            else if (ja.getName() == JarvisAction.MOVE_UP ||
                    ja.getName() == JarvisAction.MOVE_RIGHT ||
                    ja.getName() == JarvisAction.MOVE_DOWN ||
                    ja.getName() == JarvisAction.MOVE_LEFT)
                newEnv.moveJarvis(ja.getLocation());
            s = newEnv;
        }

        return s;
    }
}

/**
 * Heuristic function: mengembalikan jarak terjauh item menggunakan Manhattan distance
 */
class JarvisHeuristicFunction implements HeuristicFunction {
    public double h(Object state) {
        Environment env = (Environment) state;
        XYLocation position = env.getJarvis();
        List<XYLocation> itemPositions = env.getItemPositions();
        double maxDistance = Double.MIN_VALUE;

        for (XYLocation itemPosition : itemPositions) {
            maxDistance = Math.max(maxDistance,
                    manhattanDistance(position, itemPosition));
        }

        return maxDistance;
    }

    public int manhattanDistance(XYLocation position, XYLocation itemPosition) {
        return Math.abs(position.getXCoOrdinate() - itemPosition.getXCoOrdinate()) +
                Math.abs(position.getYCoOrdinate() - itemPosition.getYCoOrdinate());
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
    /**
     * Step cost function: 1 untuk pindah ke posisi baru, 0 untuk ambil item
     */
    public double c(Object stateFrom, Action action, Object stateTo) {
        JarvisAction ja = (JarvisAction) action;
        return ja.getName() == JarvisAction.PICK ? 0 : 1;
    }
}
