
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;

import carcassonne.control.MainController;
import carcassonne.view.util.GameMessage;

/**
 * Carcassonne main class.
 * @author Timur Saglam
 */
public final class Carcassonne {
    private static final int TOOL_TIP_DISMISS_DELAY_IN_MILLISECONDS = 30000;
    private static final String LOOK_AND_FEEL_ERROR = "Could not use Nimbus LookAndFeel. Using default instead (";
    private static final String CLOSING_BRACKET = ").";
    private static final String NIMBUS = "Nimbus";
    private static final String MAC = "Mac";
    private static final String OS_NAME_KEY = "os.name";

    /**
     * Main method that starts the game.
     * @param args are not used.
     */
    public static void main(String[] args) {
        setLookAndFeel();
        ToolTipManager.sharedInstance().setDismissDelay(TOOL_TIP_DISMISS_DELAY_IN_MILLISECONDS);
        new MainController().startGame();
    }

    private Carcassonne() {
        // private constructor ensures non-instantiability!
    }

    /**
     * Tries to set a custom look and feel if the operating system is not Mac OS. This ensures a at least somewhat decent
     * user interface on Windows operating systems.
     */
    private static void setLookAndFeel() {
        if (!System.getProperty(OS_NAME_KEY).startsWith(MAC)) {
            for (LookAndFeelInfo lookAndFeel : UIManager.getInstalledLookAndFeels()) {
                if (NIMBUS.equals(lookAndFeel.getName())) {
                    try {
                        UIManager.setLookAndFeel(lookAndFeel.getClassName());
                    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException exception) {
                        GameMessage.showError(LOOK_AND_FEEL_ERROR + exception.getMessage() + CLOSING_BRACKET);
                    }
                }
            }
        }
    }
}


import carcassonne.model.grid.GridDirection;
import carcassonne.settings.GameSettings;
import carcassonne.view.GlobalKeyBindingManager;

/**
 * Facade for the game controller. Allows to call view classes or potentially external services to make requests
 * regarding the game flow.
 * @author Timur Saglam
 */
public interface ControllerFacade {

    /**
     * Requests to abort the round.
     */
    void requestAbortGame();

    /**
     * Requests to place a meeple on the current selected tile.
     * @param position is the position on the tile where the meeple is to be placed.
     */
    void requestMeeplePlacement(GridDirection position);

    /**
     * Requests to start a new round.
     */
    void requestNewRound();

    /**
     * Requests to skip either the current tile placement or the current meeple placement.
     */
    void requestSkip();

    /**
     * Requests to place a tile on the grid.
     * @param x is the x coordinate of the grid spot to place the tile.
     * @param y is the y coordinate of the grid spot to place the tile.
     */
    void requestTilePlacement(int x, int y);

    /**
     * Getter for the global key binding manager.
     * @return the global key bindings.
     */
    GlobalKeyBindingManager getKeyBindings();

    /**
     * Getter for the global game settings.
     * @return the settings.
     */
    GameSettings getSettings();

}

import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;

import carcassonne.control.state.StateMachine;
import carcassonne.model.ai.ArtificialIntelligence;
import carcassonne.model.ai.RuleBasedAI;
import carcassonne.model.grid.GridDirection;
import carcassonne.settings.GameSettings;
import carcassonne.view.GlobalKeyBindingManager;
import carcassonne.view.ViewFacade;
import carcassonne.view.main.MainView;
import carcassonne.view.secondary.MeepleView;
import carcassonne.view.secondary.TileView;
import carcassonne.view.util.GameMessage;

/**
 * Central class of the game, creates all requires components, receives user input from the user interface in the
 * <code>view package</code>, and controls both the <code>view</code> and the <code>model</code>.
 * @author Timur Saglam
 */
public class MainController implements ControllerFacade {
    private GlobalKeyBindingManager keyBindings;
    private MainView mainView;
    private MeepleView meepleView;
    private final GameSettings settings;
    private final StateMachine stateMachine;
    private TileView tileView;

    /**
     * Basic constructor. Creates the view and the model of the game.
     */
    public MainController() {
        settings = new GameSettings();
        createUserInterface();
        ArtificialIntelligence playerAI = new RuleBasedAI(settings);
        ViewFacade views = new ViewFacade(mainView, tileView, meepleView);
        stateMachine = new StateMachine(views, playerAI, settings);
    }

    /**
     * Getter for the global key binding manager.
     * @return the global key bindings.
     */
    @Override
    public GlobalKeyBindingManager getKeyBindings() {
        return keyBindings;
    }

    /**
     * Getter for the {@link GameSettings}, which grants access to the games settings.
     * @return the {@link GameSettings} instance.
     */
    @Override
    public GameSettings getSettings() {
        return settings;
    }

    /**
     * Requests to abort the round.
     */
    @Override
    public void requestAbortGame() {
        stateMachine.getCurrentState().abortGame();
        stateMachine.setAbortRequested(false);
    }

    /**
     * Signals that a abort request was scheduled. This request wait too long during AI vs. AI gameplay, thus this method
     * requests the state machine to abort on the next state change. This method should not be queued on the state machine
     * thread.
     */
    public void requestAsynchronousAbort() {
        stateMachine.setAbortRequested(true);
    }

    /**
     * Method for the view to call if a user mans a tile with a meeple.
     * @param position is the position the user wants to place on.
     */
    @Override
    public void requestMeeplePlacement(GridDirection position) {
        stateMachine.getCurrentState().placeMeeple(position);
    }

    /**
     * Requests to start a new round with a specific amount of players.
     */
    @Override
    public void requestNewRound() {
        stateMachine.getCurrentState().newRound(settings.getNumberOfPlayers());
    }

    /**
     * Method for the view to call if the user wants to skip a round.
     */
    @Override
    public void requestSkip() {
        stateMachine.getCurrentState().skip();
    }

    /**
     * Method for the view to call if a user places a tile.
     * @param x is the x coordinate.
     * @param y is the y coordinate.
     */
    @Override
    public void requestTilePlacement(int x, int y) {
        stateMachine.getCurrentState().placeTile(x, y);
    }

    /**
     * Shows the main user interface.
     */
    public void startGame() {
        EventQueue.invokeLater(() -> mainView.showUI());
    }

    /**
     * Creates the views and waits on the completion of the creation.
     */
    private final void createUserInterface() {
        try {
            EventQueue.invokeAndWait(() -> {
                ControllerAdapter adapter = new ControllerAdapter(this);
                mainView = new MainView(adapter);
                tileView = new TileView(adapter, mainView);
                meepleView = new MeepleView(adapter, mainView);
                keyBindings = new GlobalKeyBindingManager(adapter, mainView, tileView);
                mainView.addKeyBindings(keyBindings);
                tileView.addKeyBindings(keyBindings);
                meepleView.addKeyBindings(keyBindings);
                settings.registerNotifiable(mainView.getScoreboard());
                settings.registerNotifiable(mainView);
                settings.registerNotifiable(meepleView);
                settings.registerNotifiable(tileView);
            });
        } catch (InvocationTargetException | InterruptedException exception) {
            exception.printStackTrace();
            GameMessage.showError("Could not create user interface: " + exception.getCause().getMessage());
        }
    }
}


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import carcassonne.model.grid.GridDirection;
import carcassonne.settings.GameSettings;
import carcassonne.util.ErrorReportingRunnable;
import carcassonne.view.GlobalKeyBindingManager;

/**
 * ControllerFacade adapter for view classes that manages the AWT/Swing threading for them and delegates all calls to a
 * real controller.
 */
public class ControllerAdapter implements ControllerFacade {

    private final MainController controller;
    private final ExecutorService service;

    /**
     * Creates the controller adapter from a original controller.
     * @param controller is the original to which all calls are delegated to.
     */
    ControllerAdapter(MainController controller) {
        this.controller = controller;
        service = Executors.newSingleThreadExecutor();
    }

    @Override
    public void requestAbortGame() {
        controller.requestAsynchronousAbort(); // require for AI vs. AI play where the thread never pauses
        runInBackground(controller::requestAbortGame);
    }

    @Override
    public void requestMeeplePlacement(GridDirection position) {
        runInBackground(() -> controller.requestMeeplePlacement(position));

    }

    @Override
    public void requestNewRound() {
        runInBackground(controller::requestNewRound);
    }

    @Override
    public void requestSkip() {
        runInBackground(controller::requestSkip);
    }

    @Override
    public void requestTilePlacement(int x, int y) {
        runInBackground(() -> controller.requestTilePlacement(x, y));
    }

    @Override
    public GlobalKeyBindingManager getKeyBindings() {
        return controller.getKeyBindings(); // TODO (HIGH) [THREADING] Should this be on view thread?
    }

    @Override
    public GameSettings getSettings() {
        return controller.getSettings(); // TODO (HIGH) [THREADING] Should this be on view thread?
    }

    private void runInBackground(Runnable task) {
        service.submit(new ErrorReportingRunnable(task, "UI request led to an error:" + System.lineSeparator()));
    }

}


import carcassonne.model.Meeple;
import carcassonne.model.Player;
import carcassonne.model.ai.AbstractCarcassonneMove;
import carcassonne.model.ai.ArtificialIntelligence;
import carcassonne.model.grid.GridDirection;
import carcassonne.model.grid.GridPattern;
import carcassonne.model.grid.GridSpot;
import carcassonne.model.tile.Tile;
import carcassonne.settings.GameSettings;
import carcassonne.view.ViewFacade;
import carcassonne.view.main.MainView;
import carcassonne.view.util.GameMessage;

/**
 * The specific state when a Meeple can be placed.
 * @author Timur Saglam
 */
public class StateManning extends AbstractGameState {
    private final boolean[] noMeeplesNotification;

    /**
     * Constructor of the state.
     * @param stateMachine is the state machine managing this state.
     * @param settings are the game settings.
     * @param views contains the user interfaces.
     * @param playerAI is the current AI strategy.
     */
    public StateManning(StateMachine stateMachine, GameSettings settings, ViewFacade views, ArtificialIntelligence playerAI) {
        super(stateMachine, settings, views, playerAI);
        noMeeplesNotification = new boolean[GameSettings.MAXIMAL_PLAYERS]; // stores whether a player was already notified about a lack of meeples
    }

    /**
     * @see carcassonne.control.state.AbstractGameState#abortGame()
     */
    @Override
    public void abortGame() {
        changeState(StateGameOver.class);
    }

    /**
     * @see carcassonne.control.state.AbstractGameState#newRound()
     */
    @Override
    public void newRound(int playerCount) {
        GameMessage.showWarning("Abort the current game before starting a new one.");
    }

    /**
     * @see carcassonne.control.state.AbstractGameState#placeMeeple()
     */
    @Override
    public void placeMeeple(GridDirection position) {
        if (!round.getActivePlayer().isComputerControlled()) {
            Tile tile = views.getSelectedTile();
            views.onMainView(it -> it.resetMeeplePreview(tile));
            placeMeeple(position, tile);
        }
    }

    /**
     * @see carcassonne.control.state.AbstractGameState#placeTile()
     */
    @Override
    public void placeTile(int x, int y) {
        // do nothing.
    }

    /**
     * @see carcassonne.control.state.AbstractGameState#skip()
     */
    @Override
    public void skip() {
        if (!round.getActivePlayer().isComputerControlled()) {
            skipPlacingMeeple();
        }
    }

    private void placeMeeple(GridDirection position, Tile tile) {
        Player player = round.getActivePlayer();
        if (player.hasFreeMeeples() && tile.allowsPlacingMeeple(position, player, settings)) {
            tile.placeMeeple(player, position, settings);
            views.onMainView(it -> it.setMeeple(tile, position, player));
            updateScores();
            processGridPatterns();
            startNextTurn();
        } else {
            GameMessage.showWarning("You can't place meeple directly on an occupied Castle or Road!");
        }
    }

    private void skipPlacingMeeple() {
        if (!round.getActivePlayer().isComputerControlled()) {
            Tile tile = views.getSelectedTile();
            views.onMainView(it -> it.resetMeeplePreview(tile));
        }
        processGridPatterns();
        startNextTurn();
    }

    // gives the players the points they earned.
    private void processGridPatterns() {
        Tile tile = getSelectedTile();
        for (GridPattern pattern : grid.getModifiedPatterns(tile.getGridSpot())) {
            if (pattern.isComplete()) {
                for (Meeple meeple : pattern.getMeepleList()) {
                    GridSpot spot = meeple.getLocation();
                    views.onMainView(it -> it.removeMeeple(spot.getX(), spot.getY()));
                }
                pattern.disburse(settings.getSplitPatternScore());
                updateScores();
            }
        }
    }

    // starts the next turn and changes the state to state placing.
    private void startNextTurn() {
        if (round.isOver()) {
            changeState(StateGameOver.class);
        } else {
            if (!round.getActivePlayer().isComputerControlled()) {
                views.onMainView(MainView::resetPlacementHighlights);
            }
            round.nextTurn();
            views.onMainView(it -> it.setCurrentPlayer(round.getActivePlayer()));
            changeState(StatePlacing.class);
        }
    }

    private void placeMeepleWithAI() {
        AbstractCarcassonneMove move = playerAI.getCurrentMove().orElseThrow(() -> new IllegalStateException(NO_MOVE));
        if (move.involvesMeeplePlacement()) {
            placeMeeple(move.getMeeplePosition(), move.getOriginalTile());
        } else {
            skipPlacingMeeple();
        }
    }

    /**
     * @see carcassonne.control.state.AbstractGameState#entry()
     */
    @Override
    protected void entry() {
        Player player = round.getActivePlayer();
        Tile selectedTile = getSelectedTile();
        if (player.hasFreeMeeples()) {
            noMeeplesNotification[player.getNumber()] = false; // resets out of meeple message!
            if (player.isComputerControlled()) {
                placeMeepleWithAI();
            } else {
                views.onMainView(it -> it.setMeeplePreview(selectedTile, player));
                views.onMeepleView(it -> it.setTile(selectedTile, player));
            }
        } else {
            if (!noMeeplesNotification[player.getNumber()] && !player.isComputerControlled()) { // Only warn player once until he regains meeples
                GameMessage.showMessage("You have no Meeples left. Regain Meeples by completing patterns to place Meepeles again.");
                noMeeplesNotification[player.getNumber()] = true;
            }
            processGridPatterns();
            startNextTurn();
        }
    }

    /**
     * @see carcassonne.control.state.AbstractGameState#exit()
     */
    @Override
    protected void exit() {
        views.onMeepleView(it -> it.setVisible(false));
    }
}


import carcassonne.model.Player;
import carcassonne.model.Round;
import carcassonne.model.ai.ArtificialIntelligence;
import carcassonne.model.grid.Grid;
import carcassonne.model.grid.GridDirection;
import carcassonne.model.grid.GridSpot;
import carcassonne.model.tile.Tile;
import carcassonne.model.tile.TileStack;
import carcassonne.settings.GameSettings;
import carcassonne.view.ViewFacade;
import carcassonne.view.main.MainView;

/**
 * Is the abstract state of the state machine.
 * @author Timur Saglam
 */
public abstract class AbstractGameState { // TODO (HIGH) [AI] separate human move states from AI moves?

    private final StateMachine stateMachine;
    protected GameSettings settings;
    protected ViewFacade views;
    protected Round round;
    protected TileStack tileStack;
    protected Grid grid;
    protected ArtificialIntelligence playerAI;
    protected static final String NO_MOVE = "No AI move is available!";

    /**
     * Constructor of the abstract state, sets the controller from the parameter, registers the state at the controller and
     * calls the <code>entry()</code> method.
     * @param stateMachine is the state machine managing this state.
     * @param settings are the game settings.
     * @param views contains the user interfaces.
     * @param playerAI is the current AI strategy.
     */
    protected AbstractGameState(StateMachine stateMachine, GameSettings settings, ViewFacade views, ArtificialIntelligence playerAI) {
        this.stateMachine = stateMachine;
        this.settings = settings;
        this.playerAI = playerAI;
        this.views = views;
    }

    /**
     * Starts new round with a specific amount of players.
     */
    public abstract void abortGame();

    /**
     * Starts new round with a specific amount of players.
     * @param playerCount sets the amount of players.
     */
    public abstract void newRound(int playerCount);

    /**
     * Method for the view to call if a user mans a tile with a Meeple.
     * @param position is the placement position.
     */
    public abstract void placeMeeple(GridDirection position);

    /**
     * Method for the view to call if a user places a tile.
     * @param x is the x coordinate.
     * @param y is the y coordinate.
     */
    public abstract void placeTile(int x, int y);

    /**
     * Method for the view to call if the user wants to skip a round.
     */
    public abstract void skip();

    /**
     * Updates the round and the grid object after a new round was started.
     * @param round sets the new round.
     * @param tileStack sets the tile stack.
     * @param grid sets the new grid.
     */
    public void updateState(Round round, TileStack tileStack, Grid grid) {
        this.round = round;
        this.grid = grid;
        this.tileStack = tileStack;
    }

    /**
     * Changes the state of the state machine to a new state.
     * @param stateType is the class of the target state.
     */
    protected void changeState(Class<? extends AbstractGameState> stateType) {
        stateMachine.changeState(stateType); // Encapsulated in a method, as concrete state do not know the state machine
    }

    /**
     * Entry method of the state.
     */
    protected abstract void entry();

    /**
     * Exit method of the state.
     */
    protected abstract void exit();

    /**
     * Starts a new round for a specific number of players.
     * @param playerCount is the specific number of players.
     */
    protected void startNewRound(int playerCount) {
        Grid newGrid = new Grid(settings.getGridWidth(), settings.getGridHeight(), settings.isAllowingEnclaves());
        TileStack tileStack = new TileStack(settings.getTileDistribution(), settings.getStackSizeMultiplier());
        Round newRound = new Round(playerCount, tileStack, newGrid, settings);
        stateMachine.updateStates(newRound, tileStack, newGrid);
        updateScores();
        updateStackSize();
        if (settings.isGridSizeChanged()) {
            settings.setGridSizeChanged(false);
            views.onMainView(MainView::rebuildGrid);
        }
        GridSpot spot = grid.getFoundation(); // starting spot.
        views.onMainView(it -> it.setTile(spot.getTile(), spot.getX(), spot.getY()));
        highlightSurroundings(spot);
        for (int i = 0; i < round.getPlayerCount(); i++) {
            Player player = round.getPlayer(i);
            while (!player.hasFullHand()) {
                player.addTile(tileStack.drawTile());
            }
        }
        views.onMainView(it -> it.setCurrentPlayer(round.getActivePlayer()));
        changeState(StatePlacing.class);
    }

    /**
     * Updates the round and the grid of every state after a new round has been started.
     */
    protected void updateScores() {
        for (int playerNumber = 0; playerNumber < round.getPlayerCount(); playerNumber++) {
            Player player = round.getPlayer(playerNumber);
            views.onScoreboard(it -> it.update(player));
        }
    }

    /**
     * Returns the selected tile of the player. It does not matter if the player is a computer player or not.
     * @return the selected tile, either by a human player or the AI.
     */
    protected Tile getSelectedTile() {
        if (round.getActivePlayer().isComputerControlled()) {
            return playerAI.getCurrentMove().orElseThrow(() -> new IllegalStateException(NO_MOVE)).getOriginalTile();
        }
        return views.getSelectedTile();
    }

    /**
     * Updates the label which displays the current stack size.
     */
    protected void updateStackSize() {
        views.onScoreboard(it -> it.updateStackSize(tileStack.getSize()));
    }

    /**
     * Highlights the surroundings of a {@link GridSpot} on the main view.
     * @param spot is the {@link GridSpot} that determines where to highlight.
     */
    protected void highlightSurroundings(GridSpot spot) {
        for (GridSpot neighbor : grid.getNeighbors(spot, true, GridDirection.directNeighbors())) {
            if (neighbor != null && neighbor.isFree()) {
                views.onMainView(it -> it.setSelectionHighlight(neighbor.getX(), neighbor.getY()));
            }
        }
    }
}


import java.util.HashMap;
import java.util.Map;

import carcassonne.model.Round;
import carcassonne.model.ai.ArtificialIntelligence;
import carcassonne.model.grid.Grid;
import carcassonne.model.tile.TileStack;
import carcassonne.settings.GameSettings;
import carcassonne.view.ViewFacade;

/**
 * State machine for the Carcassonne game state.
 * @author Timur Saglam
 */
public class StateMachine {
    private boolean abortRequested;
    private AbstractGameState currentState;
    private final Map<Class<? extends AbstractGameState>, AbstractGameState> stateMap;
    private final ViewFacade views;

    /**
     * Creates a Carcassonne state machine.
     * @param views is the facade to all user interfaces.
     * @param playerAI is the employed AI for computer-controlled players.
     * @param settings are the game settings.
     */
    public StateMachine(ViewFacade views, ArtificialIntelligence playerAI, GameSettings settings) {
        this.views = views;
        stateMap = new HashMap<>();
        currentState = new StateIdle(this, settings, views, playerAI);
        registerState(currentState);
        registerState(new StateManning(this, settings, views, playerAI));
        registerState(new StatePlacing(this, settings, views, playerAI));
        registerState(new StateGameOver(this, settings, views, playerAI));
    }

    /**
     * Returns the the current game state.
     * @return that state.
     */
    public AbstractGameState getCurrentState() {
        return currentState;
    }

    /**
     * Schedules an asynchronous abort request, meaning the state machine aborts on the next state change. This method
     * should not be called on the state machine thread or executor service, as during AI vs. AI gameplay that request will
     * starve.
     * @param abortOnStateChange set true to schedule an abort request, set to false to cancel it.
     */
    public void setAbortRequested(boolean abortOnStateChange) {
        this.abortRequested = abortOnStateChange;
    }

    /**
     * Registers a specific state with the state machine.
     * @param state is that state.
     */
    private void registerState(AbstractGameState state) {
        if (stateMap.put(state.getClass(), state) != null) {
            throw new IllegalArgumentException("Can't register two states of a kind.");
        }
    }

    /**
     * Changes the state of the state machine.
     * @param stateType specifies the target state to change to.
     */
    /* package-private */ void changeState(Class<? extends AbstractGameState> stateType) {
        currentState.exit();
        if (abortRequested && stateType == StatePlacing.class) {
            abortRequested = false;
            changeState(StateGameOver.class);
        } else {
            currentState = stateMap.get(stateType); // set new state
            if (currentState == null) {
                throw new IllegalStateException("State is not registered: " + stateType);
            }
            currentState.entry();
        }
    }

    /**
     * Updates the round and the grid of every state after a new round has been started.
     * @param newRound is the new round.
     * @param tileStack is the new (and full) stack.
     * @param newGrid is the new (and empty) grid.
     */
    /* package-private */ void updateStates(Round newRound, TileStack tileStack, Grid newGrid) {
        views.onScoreboard(it -> it.rebuild(newRound.getPlayerCount()));
        for (AbstractGameState state : stateMap.values()) {
            state.updateState(newRound, tileStack, newGrid);
        }
    }
}


import java.util.Collection;
import java.util.Optional;

import carcassonne.model.Player;
import carcassonne.model.ai.AbstractCarcassonneMove;
import carcassonne.model.ai.ArtificialIntelligence;
import carcassonne.model.grid.GridDirection;
import carcassonne.model.grid.GridSpot;
import carcassonne.model.tile.Tile;
import carcassonne.settings.GameSettings;
import carcassonne.view.ViewFacade;
import carcassonne.view.main.MainView;
import carcassonne.view.util.GameMessage;

/**
 * The specific state when a Tile can be placed.
 * @author Timur Saglam
 */
public class StatePlacing extends AbstractGameState {

    private static final int SLEEP_DURATION = 10;

    /**
     * Constructor of the state.
     * @param stateMachine is the state machine managing this state.
     * @param settings are the game settings.
     * @param views contains the user interfaces.
     * @param playerAI is the current AI strategy.
     */
    public StatePlacing(StateMachine stateMachine, GameSettings settings, ViewFacade views, ArtificialIntelligence playerAI) {
        super(stateMachine, settings, views, playerAI);
    }

    /**
     * @see carcassonne.control.state.AbstractGameState#abortGame()
     */
    @Override
    public void abortGame() {
        changeState(StateGameOver.class);
    }

    /**
     * @see carcassonne.control.state.AbstractGameState#newRound()
     */
    @Override
    public void newRound(int playerCount) {
        GameMessage.showWarning("Abort the current game before starting a new one.");

    }

    /**
     * @see carcassonne.control.state.AbstractGameState#placeMeeple()
     */
    @Override
    public void placeMeeple(GridDirection position) {
        throw new IllegalStateException("Placing meeples in StatePlacing is not allowed.");
    }

    /**
     * @see carcassonne.control.state.AbstractGameState#placeTile()
     */
    @Override
    public void placeTile(int x, int y) {
        if (!round.getActivePlayer().isComputerControlled()) {
            Tile tile = views.getSelectedTile();
            placeTile(tile, x, y, false);
        }
    }

    /**
     * @see carcassonne.control.state.AbstractGameState#skip()
     */
    @Override
    public void skip() {
        if (!round.getActivePlayer().isComputerControlled()) {
            skipPlacingTile();
        }
    }

    private void skipPlacingTile() {
        getTileToDrop().ifPresent(it -> {
            tileStack.putBack(it);
            if (!round.getActivePlayer().dropTile(it)) {
                throw new IllegalStateException("Cannot drop tile " + it + "from player " + round.getActivePlayer());
            }
        });
        if (!round.getActivePlayer().isComputerControlled()) {
            views.onMainView(MainView::resetPlacementHighlights);
        }
        round.nextTurn();
        views.onMainView(it -> it.setCurrentPlayer(round.getActivePlayer()));
        entry();
    }

    private void placeTile(Tile tile, int x, int y, boolean highlightPlacement) {
        if (grid.place(x, y, tile)) {
            round.getActivePlayer().dropTile(tile);
            views.onMainView(it -> it.setTile(tile, x, y));
            if (highlightPlacement) {
                views.onMainView(view -> view.setPlacementHighlight(x, y));
            }
            GridSpot spot = grid.getSpot(x, y);
            highlightSurroundings(spot);
            changeState(StateManning.class);
        }
    }

    private void placeTileWithAI(Player player) {
        Optional<AbstractCarcassonneMove> bestMove = playerAI.calculateBestMoveFor(player.getHandOfTiles(), player, grid, tileStack);
        if (bestMove.isEmpty()) {
            skipPlacingTile();
        }
        bestMove.ifPresent(it -> {
            Tile tile = it.getOriginalTile();
            tile.rotateTo(it.getRequiredTileRotation());
            placeTile(tile, it.getX(), it.getY(), round.hasHumanPlayers());
        });
    }

    private Optional<Tile> getTileToDrop() {
        if (round.getActivePlayer().isComputerControlled()) {
            Collection<Tile> handOfTiles = round.getActivePlayer().getHandOfTiles();
            if (handOfTiles.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(playerAI.chooseTileToDrop(handOfTiles));
        }
        return Optional.of(views.getSelectedTile());
    }

    private void waitForUI() {
        while (views.isBusy()) {
            try {
                Thread.sleep(SLEEP_DURATION);
            } catch (InterruptedException exception) {
                exception.printStackTrace();
                GameMessage.showError(exception.getCause().getMessage());
            }
        }
    }

    /**
     * @see carcassonne.control.state.AbstractGameState#entry()
     */
    @Override
    protected void entry() {
        Player player = round.getActivePlayer();
        if (!player.hasFullHand() && !tileStack.isEmpty()) {
            player.addTile(tileStack.drawTile());
        }
        updateStackSize();
        if (round.isOver()) {
            changeState(StateGameOver.class);
        } else if (player.isComputerControlled()) {
            waitForUI();
            placeTileWithAI(player);
        } else {
            views.onTileView(it -> it.setTiles(player));
        }
    }

    /**
     * @see carcassonne.control.state.AbstractGameState#exit()
     */
    @Override
    protected void exit() {
        views.onTileView(it -> it.setVisible(false));
    }

}


import carcassonne.model.ai.ArtificialIntelligence;
import carcassonne.model.grid.GridDirection;
import carcassonne.model.grid.GridPattern;
import carcassonne.settings.GameSettings;
import carcassonne.view.ViewFacade;
import carcassonne.view.main.MainView;
import carcassonne.view.menubar.Scoreboard;
import carcassonne.view.util.GameMessage;

/**
 * The specific state where the statistics are shown can be placed.
 * @author Timur Saglam
 */
public class StateGameOver extends AbstractGameState {

    private static final String GAME_OVER_MESSAGE = "The game is over. Winning player(s): ";

    /**
     * Constructor of the state.
     * @param stateMachine is the state machine managing this state.
     * @param settings are the game settings.
     * @param views contains the user interfaces.
     * @param playerAI is the current AI strategy.
     */
    public StateGameOver(StateMachine stateMachine, GameSettings settings, ViewFacade views, ArtificialIntelligence playerAI) {
        super(stateMachine, settings, views, playerAI);
    }

    /**
     * @see carcassonne.control.state.AbstractGameState#abortGame()
     */
    @Override
    public void abortGame() {
        // Do nothing, round is already aborted.
    }

    /**
     * @see carcassonne.control.state.AbstractGameState#newRound()
     */
    @Override
    public void newRound(int playerCount) {
        exit();
        changeState(StateIdle.class);
        startNewRound(playerCount);
    }

    /**
     * @see carcassonne.control.state.AbstractGameState#placeMeeple()
     */
    @Override
    public void placeMeeple(GridDirection position) {
        throw new IllegalStateException("Placing meeples in StateGameOver is not allowed.");
    }

    /**
     * @see carcassonne.control.state.AbstractGameState#placeTile()
     */
    @Override
    public void placeTile(int x, int y) {
        // do nothing.
    }

    /**
     * @see carcassonne.control.state.AbstractGameState#skip()
     */
    @Override
    public void skip() {
        views.onScoreboard(Scoreboard::disable);
        exit();
        changeState(StateIdle.class);
    }

    /**
     * @see carcassonne.control.state.AbstractGameState#entry()
     */
    @Override
    protected void entry() {
        System.out.println("FINAL PATTERNS:"); // TODO (LOW) [PRINT] remove debug output
        for (GridPattern pattern : grid.getAllPatterns()) {
            System.out.println(pattern); // TODO (LOW) [PRINT] remove debug output
            pattern.forceDisburse(settings.getSplitPatternScore());
        }
        updateScores();
        updateStackSize();
        views.onMainView(MainView::resetMenuState);
        GameMessage.showMessage(GAME_OVER_MESSAGE + round.winningPlayers());
        views.showGameStatistics(round);
    }

    /**
     * @see carcassonne.control.state.AbstractGameState#exit()
     */
    @Override
    protected void exit() {
        views.closeGameStatistics();
    }
}

import carcassonne.model.ai.ArtificialIntelligence;
import carcassonne.model.grid.GridDirection;
import carcassonne.settings.GameSettings;
import carcassonne.view.ViewFacade;
import carcassonne.view.main.MainView;
import carcassonne.view.util.GameMessage;

/**
 * The specific state if no game is running.
 * @author Timur Saglam
 */
public class StateIdle extends AbstractGameState {

    /**
     * Constructor of the state.
     * @param stateMachine is the state machine managing this state.
     * @param settings are the game settings.
     * @param views contains the user interfaces.
     * @param playerAI is the current AI strategy.
     */
    public StateIdle(StateMachine stateMachine, GameSettings settings, ViewFacade views, ArtificialIntelligence playerAI) {
        super(stateMachine, settings, views, playerAI);
    }

    /**
     * @see carcassonne.control.state.AbstractGameState#abortGame()
     */
    @Override
    public void abortGame() {
        GameMessage.showMessage("There is currently no game running.");
    }

    /**
     * @see carcassonne.control.state.AbstractGameState#newRound()
     */
    @Override
    public void newRound(int playerCount) {
        startNewRound(playerCount);
    }

    /**
     * @see carcassonne.control.state.AbstractGameState#placeMeeple()
     */
    @Override
    public void placeMeeple(GridDirection position) {
        throw new IllegalStateException("Placing meeples in StateIdle is not allowed.");
    }

    /**
     * @see carcassonne.control.state.AbstractGameState#placeTile()
     */
    @Override
    public void placeTile(int x, int y) {
        // do nothing.
    }

    /**
     * @see carcassonne.control.state.AbstractGameState#skip()
     */
    @Override
    public void skip() {
        throw new IllegalStateException("There is nothing to skip in StateIdle.");
    }

    /**
     * @see carcassonne.control.state.AbstractGameState#entry()
     */
    @Override
    protected void entry() {
        views.onMainView(MainView::resetGrid);
    }

    /**
     * @see carcassonne.control.state.AbstractGameState#exit()
     */
    @Override
    protected void exit() {
        // No exit functions.
    }

}


import java.awt.Image;
import java.util.HashMap;

import carcassonne.model.tile.Tile;

/**
 * Caches scaled images of tiles to improve the performance. When zooming in or out all static images are only rendered
 * ones per zoom level.
 * @author Timur Saglam
 */
public final class TileImageScalingCache {
    private static final int SHIFT_VALUE = 1000;
    private static final HashMap<Integer, CachedImage> cachedImages = new LRUHashMap<>();

    private TileImageScalingCache() {
        // private constructor ensures non-instantiability!
    }

    /**
     * Checks if there is an existing scaled tile image in this cache.
     * @param tile is the tile whose scaled image is requested.
     * @param size is the edge with of the (quadratic) image.
     * @param previewAllowed determines if the cached image may be a preview render or should be a final render.
     * @return true if there is an existing image cached with the specified size.
     */
    public static boolean containsScaledImage(Tile tile, int size, boolean previewAllowed) {
        int key = createKey(tile, size);
        if (previewAllowed) {
            return cachedImages.containsKey(key);
        }
        return cachedImages.containsKey(key) && !cachedImages.get(key).isPreview();
    }

    /**
     * Retrieves an existing scaled image in this cache.
     * @param tile is the tile whose scaled image is requested.
     * @param size is the edge with of the (quadratic) image.
     * @return the scaled image or null if there is none.
     */
    public static Image getScaledImage(Tile tile, int size) {
        return cachedImages.get(createKey(tile, size)).getImage();
    }

    /**
     * Places an scaled image in this cache to enable its reuse.
     * @param image is the scaled image.
     * @param tile is the tile whose scaled image is requested.
     * @param size is the edge with of the scaled image.
     * @param preview determines if the image is a preview render or final render.
     */
    public static void putScaledImage(Image image, Tile tile, int size, boolean preview) {
        cachedImages.put(createKey(tile, size), new CachedImage(image, preview));
    }

    /**
     * Clears the cache, removing all stored tile images. This call might be unsafe with concurrent calls, as there are no
     * guarantees for clearing while putting.
     */
    public static void clear() {
        cachedImages.clear();
    }

    /**
     * Returns the number of cached elements in this cache.
     * @return the number of cached elements.
     */
    public static int size() {
        return cachedImages.size();
    }

    /**
     * Creates a primitive composite key for a tileType type, a size, and a orientation.
     */
    private static int createKey(Tile tile, int size) {
        return size + tile.getType().ordinal() * SHIFT_VALUE + tile.getRotation().ordinal() * SHIFT_VALUE * SHIFT_VALUE;
    }
}


import carcassonne.view.util.GameMessage;

/**
 * Runnable that does not swallow any {@link Throwable} when executed in an executor service. Errors and exceptions are
 * reported as an game message.
 * @see GameMessage
 * @author Timur Saglam
 */
public class ErrorReportingRunnable implements Runnable {

    private static final String EMPTY_MESSAGE = "";
    private final Runnable wrappedTask;
    private final String messagePrefix;

    /**
     * Creates an error-reporting task by wrapping any runnable.
     * @param wrappedTask is the wrapped runnable.
     */
    public ErrorReportingRunnable(Runnable wrappedTask) {
        this(wrappedTask, EMPTY_MESSAGE);
    }

    /**
     * Creates an error-reporting task by wrapping any runnable.
     * @param wrappedTask is the wrapped runnable.
     * @param messagePrefix is a message prefix for the game message.
     */
    public ErrorReportingRunnable(Runnable wrappedTask, String messagePrefix) {
        this.wrappedTask = wrappedTask;
        this.messagePrefix = messagePrefix;
    }

    @Override
    public void run() {
        try {
            wrappedTask.run();
        } catch (Error | RuntimeException exception) {
            exception.printStackTrace();
            reportError(exception);
        } catch (Exception exception) {
            reportError(exception);
        }
    }

    private void reportError(Throwable throwable) {
        throwable.printStackTrace();
        GameMessage.showError(messagePrefix + throwable.getMessage());
    }
}

/**
 * Utility class for the calculation of the Minkowski distance, including Manhattan and Euclidean distance.
 * @author Timur Saglam
 */
public enum MinkowskiDistance {
    COMPASS(Math.pow(2, -0.5), "Compass"),
    MANHATTAN(1, "Diamond"),
    EUCLIDEAN(2, "Round"),
    ROUNDED_SQUARE(Math.pow(2, 1.5), "Rounded Square");

    private final double order;
    private final String description;

    /**
     * Minkowski distance of a certain order.
     * @param order specifies the order.
     */
    MinkowskiDistance(double order, String description) {
        this.order = order;
        this.description = description;
    }

    /**
     * Calculates the distance between two points P(x, y) and Q(x, y).
     * @param x1 is the X-coordinate of P.
     * @param y1 is the Y-coordinate of P.
     * @param x2 is the X-coordinate of Q.
     * @param y2 is the Y-coordinate of Q.
     * @return the distance.
     */
    public double distance(int x1, int y1, int x2, int y2) {
        return calculate(x1, y1, x2, y2, order);
    }

    /**
     * Returns a textual description of the geometric properties of the distance measure.
     * @return the textual description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Calculates the Minkowski distance of a certain order between two points P(x, y) and Q(x, y).
     * @param x1 is the X-coordinate of P.
     * @param y1 is the Y-coordinate of P.
     * @param x2 is the X-coordinate of Q.
     * @param y2 is the Y-coordinate of Q.
     * @param order is the Minkowski distance order.
     * @return the distance.
     */
    public static double calculate(int x1, int y1, int x2, int y2, double order) {
        return root(Math.pow(Math.abs(x1 - x2), order) + Math.pow(Math.abs(y1 - y2), order), order);
    }

    /**
     * Calculates the root of a certain degree of a number.
     */
    private static double root(double number, double degree) {
        return Math.round(Math.pow(number, 1 / degree));
    }

}


import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import carcassonne.view.PaintShop;
import carcassonne.view.util.GameMessage;

/**
 * Utility class for loading image files and creating image icons and buffered images.
 * @author Timur Saglam
 */
public enum ImageLoadingUtil {
    EMBLEM("emblem.png"),
    HIGHLIGHT("highlight.png"),
    LEFT("icons/left.png"),
    NULL_TILE("tiles/Null0.png"),
    RIGHT("icons/right.png"),
    SKIP("icons/skip.png"),
    SPLASH("splash.png");

    private static final int PLACEHOLDER_IMAGE_SIZE = 10;
    private final String path;

    ImageLoadingUtil(String path) {
        this.path = path;
    }

    /**
     * Convenience method that creates a buffered image for the image enumeral.
     * @return the buffered image.
     */
    public BufferedImage createBufferedImage() {
        return createBufferedImage(path);
    }

    /**
     * Convenience method that creates a high-DPI image for the image enumeral.
     * @return the image, which has half of the width and height of the original file.
     */
    public Image createHighDpiImage() {
        return createHighDpiImage(path);
    }

    /**
     * Convenience method that creates a high-DPI image icon for the image enumeral.
     * @return the image icon, which has half of the width and height of the original file.
     */
    public ImageIcon createHighDpiImageIcon() {
        return new ImageIcon(createHighDpiImage(path));
    }

    /**
     * Loads an image from a path and creates a buffered image. Does some performance optimizations.
     * @param path is the relative file path, omitting the resource folder path.
     * @return the buffered image.
     */
    public static BufferedImage createBufferedImage(String path) {
        BufferedImage image = loadBufferedImage(path);
        return makeCompatible(image);
    }

    /**
     * Loads an image from a path and creates a high-DPI image icon.
     * @param path is the relative file path, omitting the resource folder path.
     * @return the image icon, which has half of the width and height of the original file.
     */
    public static ImageIcon createHighDpiImageIcon(String path) {
        return new ImageIcon(createHighDpiImage(path));
    }

    /**
     * Loads an image from a path and creates a high-DPI image.
     * @param path is the relative file path, omitting the resource folder path.
     * @return the image, which has half of the width and height of the original file.
     */
    public static Image createHighDpiImage(String path) {
        BufferedImage fullSize = createBufferedImage(path);
        Image base = makeCompatible(fullSize.getScaledInstance(fullSize.getWidth() / 2, fullSize.getHeight() / 2, Image.SCALE_SMOOTH),
                fullSize.getTransparency());
        return new BaseMultiResolutionImage(base, fullSize);
    }

    /**
     * Creates a high-DPI image from a high-res image.
     * @param image is the high resolution image used as the version with the highest resolution.
     * @param transparency is the {@link Transparency} of the image.
     * @return the image, which has half of the width and height of the original file.
     */
    public static Image createHighDpiImage(Image image) {
        Image base = image.getScaledInstance(image.getWidth(null) / 2, image.getHeight(null) / 2, Image.SCALE_SMOOTH);
        return new BaseMultiResolutionImage(base, image);
    }

    /**
     * Loads an image from a path and creates an image icon.
     * @param path is the relative file path, omitting the resource folder path.
     * @return the image icon.
     */
    public static ImageIcon createImageIcon(String path) {
        return new ImageIcon(ImageLoadingUtil.class.getClassLoader().getResource(path));
    }

    /**
     * Converts a buffered image to a compatible image.
     * @param image is the image to convert.
     * @return the compatible image.
     * @see GraphicsConfiguration#createCompatibleImage(int, int, int)
     */
    public static BufferedImage makeCompatible(BufferedImage image) {
        return makeCompatibleImage(image, image.getWidth(), image.getHeight(), image.getTransparency());
    }

    /**
     * Converts an image to a compatible image.
     * @param image is the image to convert.
     * @param is the transparency of the image, can be received from buffered images with
     * {@link Transparency#getTransparency()}.
     * @return the compatible image.
     * @see GraphicsConfiguration#createCompatibleImage(int, int, int)
     */
    public static Image makeCompatible(Image image, int transparency) {
        return makeCompatibleImage(image, image.getWidth(null), image.getHeight(null), transparency);
    }

    private static BufferedImage makeCompatibleImage(Image image, int width, int height, int transparency) {
        GraphicsConfiguration configuration = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        BufferedImage convertedImage = configuration.createCompatibleImage(width, height, transparency);
        Graphics2D graphics2D = convertedImage.createGraphics();
        graphics2D.drawImage(image, 0, 0, width, height, null);
        graphics2D.dispose();
        return convertedImage;
    }

    private static BufferedImage loadBufferedImage(String path) {
        try {
            return ImageIO.read(PaintShop.class.getClassLoader().getResourceAsStream(path));
        } catch (IOException exception) {
            exception.printStackTrace();
            GameMessage.showError("ERROR: Could not load image loacted at " + path);
            return new BufferedImage(PLACEHOLDER_IMAGE_SIZE, PLACEHOLDER_IMAGE_SIZE, BufferedImage.TYPE_INT_ARGB);
        }
    }
}


import java.util.LinkedHashMap;
import java.util.Map;

/**
 * least recently used cache based on a {@link LinkedHashMap}.
 * @author Timur Saglam
 * @param <K> the key.
 * @param <V> the value.
 */
public class LRUHashMap<K, V> extends LinkedHashMap<K, V> {

    private static final float LOAD_FACTOR = 0.75f; // default for LinkedHashMap
    private static final int INITIAL_SIZE = 100;
    private static final int MAXIMUM_SIZE = 2000;
    private static final long serialVersionUID = -7078586519346306608L;

    /**
     * Creates the hash map.
     */
    public LRUHashMap() {
        super(INITIAL_SIZE, LOAD_FACTOR, true);
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > MAXIMUM_SIZE;
    }
}


import static carcassonne.settings.GameSettings.TILE_RESOLUTION;

import java.awt.Image;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.BufferedImage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;

import carcassonne.model.tile.Tile;
import carcassonne.settings.GameSettings;
import carcassonne.view.PaintShop;

/**
 * Tile scaling utility class that is optimized for concurrent use. It uses a sophisticated locking mechanism and
 * leverages the image caching capabilities of the {@link TileImageScalingCache} and the image scaling capabilities of
 * the {@link FastImageScaler}.
 * @author Timur Saglam
 */
public final class ConcurrentTileImageScaler {
    private static final ConcurrentMap<Integer, Semaphore> semaphores = new ConcurrentHashMap<>();
    private static final int SHIFT_VALUE = 1000;
    private static final int SINGLE_PERMIT = 1;

    private ConcurrentTileImageScaler() {
        // private constructor ensures non-instantiability!
    }

    /**
     * Returns the scaled image of a tile. This method is thread safe and leverages caching.
     * @param tile is the tile whose image is required.
     * @param targetSize is the edge length of the (quadratic) image in pixels.
     * @param fastScaling specifies whether a fast scaling algorithm should be used.
     * @return the scaled {@link Image}.
     */
    public static Image getScaledImage(Tile tile, int targetSize, boolean fastScaling) {
        int lockKey = createKey(tile, targetSize);
        semaphores.putIfAbsent(lockKey, new Semaphore(SINGLE_PERMIT));
        Semaphore lock = semaphores.get(lockKey);
        try {
            lock.acquire();
            return getScaledImageUnsafe(tile, targetSize, fastScaling);
        } catch (InterruptedException exception) {
            exception.printStackTrace();
        } finally {
            lock.release();
        }
        return null;
    }

    /**
     * Returns the scaled multi-resolution image of a tile. This image therefore supports HighDPI graphics such as Retina on
     * Mac OS. This method is thread safe and leverages caching.
     * @param tile is the tile whose image is required.
     * @param targetSize is the edge length of the (quadratic) image in pixels.
     * @param fastScaling specifies whether a fast scaling algorithm should be used.
     * @return the scaled multi-resolution {@link Image}.
     */
    public static Image getScaledMultiResolutionImage(Tile tile, int targetSize, boolean fastScaling) {
        int highDpiSize = Math.min(targetSize * GameSettings.HIGH_DPI_FACTOR, GameSettings.TILE_RESOLUTION);
        Image highDpiImage = getScaledImage(tile, highDpiSize, fastScaling);
        Image defaultImage = getScaledImage(tile, targetSize, fastScaling);
        return new BaseMultiResolutionImage(defaultImage, highDpiImage);
    }

    /**
     * Either scales the full resolution image to the required size or retrieves the cached scaled image. This method is not
     * thread safe.
     */
    private static Image getScaledImageUnsafe(Tile tile, int targetSize, boolean fastScaling) {
        if (TileImageScalingCache.containsScaledImage(tile, targetSize, fastScaling)) {
            return TileImageScalingCache.getScaledImage(tile, targetSize);
        }
        Image largerImage = getOriginalImage(tile, targetSize);
        Image scaledImage = scaleImage(largerImage, targetSize, fastScaling);
        TileImageScalingCache.putScaledImage(scaledImage, tile, targetSize, fastScaling);
        return scaledImage;
    }

    /**
     * Gets a full-size image for a specific tile. Uses caching to reuse image icons.
     */
    private static Image getOriginalImage(Tile tile, int targetSize) {
        int lockKey = createKey(tile, TILE_RESOLUTION);
        semaphores.putIfAbsent(lockKey, new Semaphore(SINGLE_PERMIT));
        Semaphore lock = semaphores.get(lockKey);
        try {
            if (targetSize != TILE_RESOLUTION) {
                lock.acquire();
            }
            return getOriginalImageUnsafe(tile);
        } catch (InterruptedException exception) {
            exception.printStackTrace();
        } finally {
            if (targetSize != TILE_RESOLUTION) {
                lock.release();
            }
        }
        return null;
    }

    /**
     * Loads an image for a specific tile. Uses caching to reuse image icons. This method is not thread safe.
     */
    private static Image getOriginalImageUnsafe(Tile tile) {
        String imagePath = GameSettings.TILE_FOLDER_PATH + tile.getType().name() + tile.getImageIndex() + GameSettings.TILE_FILE_TYPE;
        if (TileImageScalingCache.containsScaledImage(tile, TILE_RESOLUTION, false)) {
            return TileImageScalingCache.getScaledImage(tile, TILE_RESOLUTION);
        }
        if (tile.hasEmblem()) {
            return loadImageAndPaintEmblem(tile, imagePath);
        }
        Image image = ImageLoadingUtil.createBufferedImage(imagePath);
        TileImageScalingCache.putScaledImage(image, tile, TILE_RESOLUTION, false);
        return image;
    }

    /**
     * Loads a tile image for a tile with a certain rotation index and paints its emblem.
     */
    private static Image loadImageAndPaintEmblem(Tile tile, String imagePath) {
        BufferedImage image = ImageLoadingUtil.createBufferedImage(imagePath);
        Image paintedImage = PaintShop.addEmblem(image);
        TileImageScalingCache.putScaledImage(paintedImage, tile, TILE_RESOLUTION, false);
        return paintedImage;
    }

    /**
     * Scales the full resolution image to the required size with either the fast or the smooth scaling algorithm. This
     * method is not thread safe.
     */
    private static Image scaleImage(Image image, int size, boolean fastScaling) {
        if (fastScaling) {
            return FastImageScaler.scaleDown(image, size);
        }
        return image.getScaledInstance(size, size, Image.SCALE_SMOOTH);
    }

    /**
     * Creates a primitive composite key for the tile depiction with a specific edge length.
     */
    private static int createKey(Tile tile, int size) {
        return size + tile.getType().ordinal() * SHIFT_VALUE + tile.getImageIndex() * SHIFT_VALUE * SHIFT_VALUE;
    }
}


import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Utility class for scaling down quadratic images extremely fast. This class enable faster scaling than
 * {@link Image#getScaledInstance(int, int, int)} (5x faster with {@link Image#SCALE_FAST} and 10x faster with
 * {@link Image#SCALE_SMOOTH}). Additionally, the resulting images look better as images generated with
 * {@link Image#SCALE_FAST}. This class is heavily based on an article by Dr. Franz Graf.
 * @see <a href=" https://www.locked.de/fast-image-scaling-in-java/">locked.de/fast-image-scaling-in-java</a>
 * @author Timur Saglam
 */
public final class FastImageScaler {

    private FastImageScaler() {
        // private constructor ensures non-instantiability!
    }

    /**
     * Scales down a quadratic images to a image with a given size.
     * @param image is the original image.
     * @param targetSize is the desired edge length of the scaled image.
     * @return the scaled image.
     */
    public static Image scaleDown(Image image, int targetSize) {
        if (image.getWidth(null) <= targetSize) {
            return image; // do not do anything if image already has target size.
        }
        Image result = scaleByHalf(image, targetSize);
        result = scaleExact(result, targetSize);
        return result;
    }

    /*
     * While the image is larger than 2x the target size: Scale image with factor 0.5 and nearest neighbor interpolation.
     */
    private static BufferedImage scaleByHalf(Image image, int targetSize) {
        int width = image.getWidth(null);
        int height = image.getHeight(null);
        float factor = getBinFactor(width, targetSize);
        width *= factor;
        height *= factor;
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        graphics.drawImage(image, 0, 0, width, height, null);
        graphics.dispose();
        return scaled;
    }

    /*
     * Scale to final target size with bilinear interpolation.
     */
    private static BufferedImage scaleExact(Image image, int targetSize) {
        float factor = targetSize / (float) image.getWidth(null);
        int width = (int) (image.getWidth(null) * factor);
        int height = (int) (image.getHeight(null) * factor);
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(image, 0, 0, width, height, null);
        graphics.dispose();
        return scaled;
    }

    private static float getBinFactor(int width, int targetSize) {
        float factor = 1;
        float target = targetSize / (float) width;
        while (factor / 2 > target) {
            factor /= 2;
        }
        return factor;
    }
}


import java.awt.Image;

/**
 * Data objects to cache images with their scaling information.s
 * @author Timur Saglam
 */
public class CachedImage {
    private final Image image;
    private final boolean preview;

    /**
     * Creates
     * @param image the image to cache, cannot be null.
     * @param preview whether this image was scaled as a preview image or not.
     */
    public CachedImage(Image image, boolean preview) {
        if (image == null) {
            throw new IllegalArgumentException("Cached image cannot be null!");
        }
        this.image = image;
        this.preview = preview;
    }

    public Image getImage() {
        return image;
    }

    public boolean isPreview() {
        return preview;
    }
}


import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import carcassonne.model.terrain.TerrainType;
import carcassonne.model.tile.Tile;
import carcassonne.settings.GameSettings;
import carcassonne.settings.PlayerColor;

/**
 * The class for the player objects. It manages the meeples and the score.
 * @author Timur Saglam
 */
public class Player {
    private final int maximalTiles;
    private int freeMeeples;
    private final int number;
    private int overallScore;
    private Map<TerrainType, Integer> terrainSpecificScores;
    private final GameSettings settings;
    private final List<Tile> handOfTiles;
    private final boolean computerControlled;
    private final List<Meeple> placedMeeples;

    /**
     * Simple constructor.
     * @param number is the number of the player.
     * @param settings are the {@link GameSettings}.
     */
    public Player(int number, GameSettings settings) {
        this.number = number;
        this.settings = settings;
        freeMeeples = GameSettings.MAXIMAL_MEEPLES;
        maximalTiles = settings.getTilesPerPlayer();
        handOfTiles = new ArrayList<>();
        placedMeeples = new ArrayList<>();
        computerControlled = settings.isPlayerComputerControlled(number);
        initializeScores();
    }

    /**
     * Adds points to the players score value and keeps track of the type of score.
     * @param amount is the amount of points the player gets.
     * @param scoreType is the pattern type responsible for the points.
     */
    public void addPoints(int amount, TerrainType scoreType) {
        terrainSpecificScores.put(scoreType, terrainSpecificScores.get(scoreType) + amount);
        overallScore += amount;
    }

    /**
     * Adds a tile to the hand of the player if there is space.
     * @param tile the new tile.
     * @return true if successfully added, false if the hand is full.
     */
    public boolean addTile(Tile tile) {
        if (handOfTiles.size() < maximalTiles) {
            return handOfTiles.add(tile);
        }
        return false;
    }

    /**
     * Drops a tile from the hand of the player.
     * @param tile is the tile to drop.
     * @return true if it was dropped, false if it is not in the hand of the player.
     */
    public boolean dropTile(Tile tile) {
        return handOfTiles.remove(tile);
    }

    /**
     * Convenience method for {@link GameSettings#getPlayerColor(int)}.
     * @return the {@link PlayerColor} of this player.
     */
    public PlayerColor getColor() {
        return settings.getPlayerColor(number);
    }

    /**
     * Getter for the amount of free meeples.
     * @return the amount of free meeples.
     */
    public int getFreeMeeples() {
        return freeMeeples;
    }

    /**
     * Returns the number of placed meeples that cannot be retrieved, meaning placed on a field.
     * @return the number of placed fields meeples.
     */
    public int getUnretrievableMeeples() {
        return (int) placedMeeples.stream().filter(it -> it.getType() == TerrainType.FIELDS).count();
    }

    /**
     * Gives read access to the hand of tiles.
     * @return the hand of tiles.
     */
    public Collection<Tile> getHandOfTiles() {
        return new ArrayList<>(handOfTiles);
    }

    /**
     * Grants access to a meeple.
     * @return the meeple.
     */
    public Meeple getMeeple() {
        if (hasFreeMeeples()) {
            freeMeeples--;
            Meeple meeple = new Meeple(this);
            placedMeeples.add(meeple);
            assert placedMeeples.size() <= GameSettings.MAXIMAL_MEEPLES;
            return meeple;
        }
        throw new IllegalStateException("No unused meeples are left.");
    }

    /**
     * Convenience method for {@link GameSettings#getPlayerName(int)}.
     * @return the name of this player.
     */
    public String getName() {
        return settings.getPlayerName(number);
    }

    /**
     * Getter for number of the player.
     * @return the player number.
     */
    public int getNumber() {
        return number;
    }

    /**
     * Getter for the score of the player.
     * @return the score
     */
    public int getScore() {
        return overallScore;
    }

    /**
     * Getter for a specific terrain score.
     * @param scoreType is the type of the specific terrain score.
     * @return the specific score.
     */
    public int getTerrainScore(TerrainType scoreType) {
        if (terrainSpecificScores.containsKey(scoreType)) {
            return terrainSpecificScores.get(scoreType);
        }
        return -1; // error
    }

    /**
     * Checks whether the player can still place Meeples.
     * @return true if he has at least one free Meeple.
     */
    public boolean hasFreeMeeples() {
        return freeMeeples > 0;
    }

    /**
     * Checks if the hand of the player is full.
     * @return true if full.
     */
    public boolean hasFullHand() {
        return handOfTiles.size() == maximalTiles;
    }

    /**
     * Checks if the hand of the player is full.
     * @return true if full.
     */
    public boolean hasEmptyHand() {
        return handOfTiles.isEmpty();
    }

    /**
     * Returns a meeple after its job is down. Allows the player to place another meeple.
     */
    public void returnMeeple(Meeple meeple) {
        boolean isRemoved = placedMeeples.remove(meeple);
        assert isRemoved;
        freeMeeples++;
    }

    @Override
    public String toString() {
        return "Player[number: " + number + ", score: " + overallScore + ", free meeples: " + freeMeeples + "]";
    }

    private void initializeScores() {
        overallScore = 0;
        terrainSpecificScores = new EnumMap<>(TerrainType.class);
        for (int i = 0; i < TerrainType.values().length - 1; i++) {
            terrainSpecificScores.put(TerrainType.values()[i], 0); // initial scores are zero
        }
    }

    /**
     * Checks whether the player is a human player or an AI player.
     * @return true if it is an AI player.
     */
    public boolean isComputerControlled() {
        return computerControlled;
    }
}


import carcassonne.model.grid.GridDirection;
import carcassonne.model.grid.GridSpot;
import carcassonne.model.terrain.TerrainType;

/**
 * Meeples are the token that a player places on the grid.
 * @author Timur Saglam
 */
public class Meeple {
    private GridSpot location;
    private final Player owner;
    private GridDirection position;

    /**
     * Basic constructor.
     * @param owner is the owner of the meeple.
     */
    public Meeple(Player owner) {
        this.owner = owner;
    }

    /**
     * Getter for the placement location.
     * @return the tile where the meeple is placed.
     */
    public GridSpot getLocation() {
        return location;
    }

    /**
     * Getter for the player that owns the meeple.
     * @return the owner.
     */
    public Player getOwner() {
        return owner;
    }

    /**
     * Getter for the placement position.
     * @return the position on the tile where the meeple is placed.
     */
    public GridDirection getPosition() {
        return position;
    }

    /**
     * Returns the type of the meeple, meaning on which terrain the meeple is placed on.
     * @return the terrain or OTHER if it is not placed.
     */
    public TerrainType getType() {
        if (isPlaced()) {
            return location.getTile().getTerrain(position);
        }
        return TerrainType.OTHER;
    }

    /**
     * Indicates whether the meeple is placed or not.
     * @return true if placed.
     */
    public boolean isPlaced() {
        return location != null;
    }

    /**
     * Collects meeple from tile.
     */
    public void removePlacement() {
        if (location != null) {
            owner.returnMeeple(this); // return me.
            location = null; // mark as unplaced.
        }
    }

    /**
     * Sets the placement location, which is the tile where the meeple is placed.
     * @param placementLocation is the placement location.
     */
    public void setLocation(GridSpot placementLocation) {
        this.location = placementLocation;
    }

    /**
     * Sets the placement position, which is the position on the tile where the meeple is placed.
     * @param placementPosition is the placement position.
     */
    public void setPosition(GridDirection placementPosition) {
        this.position = placementPosition;
    }

    @Override
    public String toString() {
        String placement = "";
        String type = "Unplaced ";
        if (isPlaced()) {
            type = location.getTile().getTerrain(position).toReadableString();
            placement = "placed on (" + location.getX() + "|" + location.getY() + ")" + " at " + position + " ";
        }
        return type + getClass().getSimpleName() + " by Player " + owner.getNumber() + " " + placement;
    }
}


import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import carcassonne.model.grid.Grid;
import carcassonne.model.tile.TileStack;
import carcassonne.settings.GameSettings;

/**
 * An object of the round class simulates a game round. It does not actively control the game. It represents the round
 * and its information in an object.
 * @author Timur Saglam
 */
public class Round {

    private static final String NOTHING = "";
    private static final String SQUARE_BRACKETS = "[\\[\\]]";

    private int activePlayerIndex;
    private final Grid grid;
    private Player[] players;
    private final int playerCount;
    private final TileStack tileStack;

    /**
     * Simple constructor that creates the grid, the tile stack and the players.
     * @param playerCount is the amount of players of the round.
     * @param tileStack is the stack of tiles.
     * @param grid is the grid of the round.
     * @param settings are the {@link GameSettings}.
     */
    public Round(int playerCount, TileStack tileStack, Grid grid, GameSettings settings) {
        this.grid = grid;
        this.playerCount = playerCount;
        this.tileStack = tileStack;
        createPlayers(settings);
    }

    /**
     * Getter for the active players of the round.
     * @return the players whose turn it is.
     */
    public Player getActivePlayer() {
        return players[activePlayerIndex];
    }

    /**
     * Checks if there are any human players in a match.
     * @return true if at least human player is taking part.
     */
    public boolean hasHumanPlayers() {
        return Arrays.stream(players).anyMatch(it -> !it.isComputerControlled());
    }

    /**
     * Getter for a specific players of the round.
     * @param playerNumber is the number of the specific players.
     * @return returns the players.
     */
    public Player getPlayer(int playerNumber) {
        return players[playerNumber];
    }

    /**
     * Getter for the amount of players in the round.
     * @return the amount of players.
     */
    public int getPlayerCount() {
        return playerCount;
    }

    /**
     * Method determines the winning players by the highest score.
     * @return a list of names of the winning players.
     */
    public String winningPlayers() {
        List<String> winnerList = new LinkedList<>();
        int maxScore = 0;
        for (Player player : players) {
            if (player.getScore() >= maxScore) {
                if (player.getScore() > maxScore) {
                    winnerList.clear();
                }
                winnerList.add(player.getName());
                maxScore = player.getScore();
            }
        }
        return winnerList.toString().replaceAll(SQUARE_BRACKETS, NOTHING);
    }

    /**
     * Checks whether the game round is over. A game round is over if the grid is full or the stack of tiles is empty (no
     * tiles left).
     * @return true if the game is over.
     */
    public boolean isOver() {
        return grid.isFull() || tileStack.isEmpty() && Arrays.stream(players).allMatch(Player::hasEmptyHand);
    }

    /**
     * Method the starts the turn of the next players a draws a tile from the stack.
     */
    public void nextTurn() {
        activePlayerIndex = ++activePlayerIndex % players.length;
    }

    /**
     * creates the players objects and sets the first players as active players.
     * @param playerCount is the number of players in the range of [1, <code>GameOptions.MAXIMAL_PLAYERS]</code>.
     */
    private void createPlayers(GameSettings settings) {
        if (playerCount <= 1 || playerCount > GameSettings.MAXIMAL_PLAYERS) {
            throw new IllegalArgumentException(playerCount + " is not a valid players count");
        }
        players = new Player[playerCount]; // initialize the players array.
        for (int i = 0; i < players.length; i++) {
            players[i] = new Player(i, settings); // create the players.
        }
    }
}


import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import carcassonne.settings.GameSettings;

/**
 * Tile utility class. TODO (HIGH) [DESIGN] Maybe move this somewhere else.
 * @author Timur Saglam
 */
public final class TileUtil {
    private final static Map<TileType, Integer> rotations = new HashMap<>();

    private TileUtil() {
        throw new IllegalStateException(); // private constructor for non-instantiability
    }

    /**
     * Determines how often a tile of a specific {@link TileType} can be rotated before it returns to the first rotation.
     * @param type is the specific {@link TileType}.
     * @return the number of possible rotations (between 1 and 3).
     */
    public static int rotationLimitFor(TileType type) {
        return rotations.computeIfAbsent(type, TileUtil::determineRotationLimit);
    }

    /**
     * Determines rotation limit for a tile type based on the available image resources.
     */
    private static int determineRotationLimit(TileType type) {
        int rotations = 0;
        for (int rotation = 0; rotation < TileRotation.values().length; rotation++) {
            String path = GameSettings.TILE_FOLDER_PATH + type.name() + rotation + GameSettings.TILE_FILE_TYPE;
            InputStream file = TileUtil.class.getClassLoader().getResourceAsStream(path);
            if (file != null) {
                rotations++;
            }
        }
        if (rotations == 0) {
            throw new IllegalStateException(type + " tile needs at least one image file!");
        }
        return rotations;
    }

}


import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

/**
 * The stack of tiles for a game.
 * @author Timur Saglam
 */
public class TileStack {
    private final List<Tile> tiles;
    private final Queue<Tile> returnedTiles;
    private final Set<Tile> returnHistory;
    private final int multiplier;
    private final int initialSize;

    /**
     * Basic constructor, creates the tile stack.
     * @param distribution is the tile distribution according which the stack is filled.
     * @param multiplier is the tile stack multiplier, meaning how often the distribution is added to the stack.
     */
    public TileStack(TileDistribution distribution, int multiplicator) {
        this(distribution, multiplicator, null);
    }

    /**
     * Creates a tile stack with a pseudo-random tile order.
     * @param distribution is the tile distribution according which the stack is filled.
     * @param multiplier is the tile stack multiplier, meaning how often the distribution is added to the stack.
     * @param sortingSeed is the seed for the tile order.
     */
    public TileStack(TileDistribution distribution, int multiplier, Long sortingSeed) {
        this.multiplier = multiplier;
        tiles = new LinkedList<>();  // LinkedList is better for a stack here
        returnedTiles = new LinkedList<>();
        returnHistory = new HashSet<>();
        fillStack(distribution);
        initialSize = getSize();
        rotateRandomly();
        if (sortingSeed == null) {
            Collections.shuffle(tiles);
        } else {
            Collections.shuffle(tiles, new Random(sortingSeed));
        }
    }

    /**
     * Draws random tile from the stack and returns it.
     * @return the tile or null if the stack is empty.
     */
    public Tile drawTile() {
        if (tiles.isEmpty() && returnedTiles.isEmpty()) {
            return null; // no tile to draw!
        }
        if (tiles.isEmpty()) {
            return returnedTiles.poll();
        }

        return tiles.remove(0);
    }

    /**
     * Returns the initial size of the stack.
     * @return the size of the full stack.
     */
    public int getInitialSize() {
        return initialSize;
    }

    /**
     * Getter for the size of the stack.
     * @return the amount of tiles on the stack.
     */
    public int getSize() {
        return tiles.size() + returnedTiles.size();
    }

    /**
     * Checks whether the tile stack is empty.
     * @return true if empty.
     */
    public boolean isEmpty() {
        return tiles.isEmpty() && returnedTiles.isEmpty();
    }

    /**
     * Returns a tile that is not placed under the stack.
     * @param tile is the tile to put back under the stack.
     */
    public void putBack(Tile tile) {
        if (tile.isPlaced()) {
            throw new IllegalArgumentException("Cannot return a placed tile!");
        }
        if (returnHistory.add(tile)) { // tiles can only be returned once!
            returnedTiles.add(tile);
        }
    }

    private void fillStack(TileDistribution distribution) {
        for (TileType tileType : TileType.validTiles()) {
            int amount = distribution.getQuantity(tileType) * multiplier;
            for (int i = 0; i < amount; i++) {
                tiles.add(new Tile(tileType));
            }
        }
    }

    private void rotateRandomly() {
        for (Tile tile : tiles) {
            for (int i = 0; i < Math.round(Math.random() * 4 - 0.5); i++) {
                tile.rotateRight(); // Random orientation with equal chance for each orientation.
            }
        }
    }
}


import java.util.Collection;
import java.util.EnumSet;

import javax.swing.ImageIcon;

import carcassonne.model.Meeple;
import carcassonne.model.Player;
import carcassonne.model.grid.CastleAndRoadPattern;
import carcassonne.model.grid.FieldsPattern;
import carcassonne.model.grid.GridDirection;
import carcassonne.model.grid.GridPattern;
import carcassonne.model.grid.GridSpot;
import carcassonne.model.terrain.RotationDirection;
import carcassonne.model.terrain.TerrainType;
import carcassonne.model.terrain.TileTerrain;
import carcassonne.settings.GameSettings;
import carcassonne.util.ConcurrentTileImageScaler;

/**
 * The tile of a grid.
 * @author Timur Saglam
 */
public class Tile {
    private static final int CASTLE_THRESHOLD = 6; // size required for a castle to have an emblem
    protected GridSpot gridSpot;
    protected Meeple meeple;
    private final TileTerrain terrain;
    private final TileType type;
    private TileRotation rotation;
    private final int rotationLimit;

    /**
     * Simple constructor.
     * @param type is the specific {@link TileType} that defines the behavior and state of this tile. It contains the onl
     * hard coded information.
     */
    public Tile(TileType type) {
        if (type == null) {
            throw new IllegalArgumentException("Tile type cannot be null");
        }
        this.type = type;
        terrain = new TileTerrain(type);
        rotation = TileRotation.UP;
        meeple = null;
        rotationLimit = TileUtil.rotationLimitFor(type);
    }

    /**
     * Checks whether the tile has same terrain on a specific side to another tile.
     * @param direction is the specific direction.
     * @param other is the other tile.
     * @return true if it has same terrain.
     */
    public boolean canConnectTo(GridDirection direction, Tile other) {
        return getTerrain(direction) == other.getTerrain(direction.opposite());
    }

    /**
     * Getter for spot where the tile is placed
     * @return the grid spot, or null if it not placed yet.
     * @see isPlaced
     */
    public GridSpot getGridSpot() {
        return gridSpot;
    }

    /**
     * Getter for the tile image. The image depends on the orientation of the tile.
     * @return the image depicting the tile.
     */
    public ImageIcon getIcon() {
        return getScaledIcon(GameSettings.TILE_RESOLUTION, false);
    }

    /**
     * Getter for the scaled tile image. The image depends on the orientation of the tile.
     * @param edgeLength specifies the edge length of the image in pixels.
     * @return the image depicting the tile.
     */
    public ImageIcon getScaledIcon(int edgeLength) {
        return getScaledIcon(edgeLength, false);
    }

    /**
     * Getter for the scaled tile image with HiDPI support. The image depends on the orientation of the tile.
     * @param edgeLength specifies the edge length of the image in pixels.
     * @param fastScaling specifies whether a fast scaling algorithm should be used.
     * @return the image depicting the tile.
     */
    public ImageIcon getScaledIcon(int edgeLength, boolean fastScaling) {
        return new ImageIcon(ConcurrentTileImageScaler.getScaledMultiResolutionImage(this, edgeLength, fastScaling));
    }

    /**
     * Getter for the meeple of the tile.
     * @return the meeple.
     */
    public Meeple getMeeple() {
        return meeple;
    }

    /**
     * return the terrain type on the tile in the specific direction.
     * @param direction is the specific direction.
     * @return the terrain type, or null if the direction is not mapped.
     */
    public TerrainType getTerrain(GridDirection direction) {
        return terrain.at(direction);
    }

    /**
     * Getter for the tile type.
     * @return the type
     */
    public TileType getType() {
        return type;
    }

    public Collection<TileRotation> getPossibleRotations() {
        return EnumSet.allOf(TileRotation.class).stream().filter(it -> it.ordinal() < rotationLimit - 1).toList();
    }

    /**
     * Getter for the rotation.
     * @return the rotation.
     */
    public TileRotation getRotation() {
        return rotation;
    }

    /**
     * Getter for image index, which is the rotation ordinal but limited to the rotation limit (meaning the number of image
     * files for this tile).
     * @return the image index.
     */
    public int getImageIndex() {
        return rotation.ordinal() % rotationLimit;
    }

    /**
     * Checks whether the terrain of the tile connected from a specific grid direction to another specific grid direction.
     * @param from is a specific grid direction.
     * @param to is a specific grid direction.
     * @return true if the terrain connected.
     */
    public boolean hasConnection(GridDirection from, GridDirection to) {
        return terrain.isConnected(from, to);
    }

    /**
     * Checks whether the tile has a meeple.
     * @return true if it has a meeple
     */
    public boolean hasMeeple() {
        return meeple != null;
    }

    /**
     * Checks whether a meeple can be potentially placed on a specific position by its terrain.
     * @param direction is the specific position on the tile.
     * @return if it can be potentially placed. Does not check whether enemy players sit on the pattern.
     */
    public boolean hasMeepleSpot(GridDirection direction) {
        return terrain.getMeepleSpots().contains(direction);
    }

    /**
     * Determines whether this tile has an emblem. Only large castle tiles can have emblems.
     * @return true if it has an emblem, which doubles the points of this tile.
     */
    public final boolean hasEmblem() {
        int castleSize = 0;
        for (GridDirection direction : GridDirection.values()) {
            if (TerrainType.CASTLE.equals(terrain.at(direction))) {
                castleSize++;
            }
        }
        return castleSize >= CASTLE_THRESHOLD;
    }

    /**
     * Checks of tile is a monastery tile, which means it has monastery terrain in the middle of the tile.
     * @return true if is a monastery.
     */
    public boolean isMonastery() {
        return getTerrain(GridDirection.CENTER) == TerrainType.MONASTERY;
    }

    /**
     * Checks if the tile is already placed.
     * @return true if it is placed.
     */
    public boolean isPlaced() {
        return gridSpot != null;
    }

    /**
     * Checks whether a meeple of a specific player can be placed on a specific position on this tile.
     * @param position is the specific position on the tile.
     * @param player is the player in question.
     * @param settings are the game settings to determine if fortifying is allowed.
     * @return true if a meeple can be placed.
     */
    public boolean allowsPlacingMeeple(GridDirection position, Player player, GameSettings settings) {
        TerrainType terrain = getTerrain(position);
        boolean placeable = false;
        if (isPlaced()) { // placing meeples on tiles that are not placed is not possible
            if (terrain == TerrainType.MONASTERY) {
                placeable = true; // you can always place on a monastery
            } else if (terrain != TerrainType.OTHER) {
                GridPattern pattern;
                if (terrain == TerrainType.FIELDS) {
                    pattern = new FieldsPattern(getGridSpot(), position);
                } else { // castle or road:
                    pattern = new CastleAndRoadPattern(getGridSpot(), position, terrain);
                }
                if (pattern.isNotOccupied() || pattern.isOccupiedBy(player) && settings.isAllowingFortifying()) {
                    placeable = true; // can place meeple
                }
                pattern.removeTileTags();
            }
        }
        return placeable;
    }

    /**
     * Places a meeple on the tile, if the tile has not already one placed.
     * @param player is the player whose meeple is going to be set.
     * @param position is the position of the meeple on the tile.
     */
    public void placeMeeple(Player player, GridDirection position, GameSettings settings) {
        placeMeeple(player, position, player.getMeeple(), settings);
    }

    public void placeMeeple(Player player, GridDirection position, Meeple meeple, GameSettings settings) {
        if (this.meeple != null || !allowsPlacingMeeple(position, player, settings)) {
            throw new IllegalArgumentException("Tile can not have already a meeple placed on it: " + toString());
        }
        this.meeple = meeple;
        meeple.setLocation(gridSpot);
        meeple.setPosition(position);
    }

    /**
     * Removes and returns the meeple from the tile. Calls Meeple.removePlacement.
     */
    public void removeMeeple() {
        if (meeple == null) {
            throw new IllegalStateException("Meeple has already been removed.");
        }
        meeple.removePlacement();
        meeple = null;
    }

    /**
     * Turns a tile 90 degree to the left.
     */
    public void rotateLeft() {
        terrain.rotateLeft();
        rotation = rotation.rotate(RotationDirection.LEFT);
    }

    /**
     * Turns a tile 90 degree to the right.
     */
    public void rotateRight() {
        terrain.rotateRight(); // TODO (MEDIUM) [PERFORMANCE] can get fairly expensive when executed often.
        rotation = rotation.rotate(RotationDirection.RIGHT);
    }

    /**
     * Turns a tile 90 degree to the right.
     */
    public void rotateTo(TileRotation targetRotation) {
        while (rotation != targetRotation) {
            rotateRight();
        }
    }

    /**
     * Gives the tile the position where it has been placed.
     * @param spot is the {@link GridSpot} where the tile was placed.
     */
    public void setPosition(GridSpot spot) {
        if (spot == null) {
            throw new IllegalArgumentException("Position can't be null, tile cannot be removed.");
        }
        gridSpot = spot;
    }

    @Override
    public String toString() {
        return type + getClass().getSimpleName() + "[coordinates: " + gridSpot + ", Meeple: " + meeple + "]";
    }
}

import carcassonne.model.terrain.RotationDirection;

/**
 * Enumeration for the tile rotation.
 * @author Timur Saglam
 */
public enum TileRotation {
    UP, // rotate 0 degree
    TILTED_RIGHT, // rotate 90 degree clockwise
    UPSIDE_DOWN, // rotate 180 degree
    TILTED_LEFT; // rotate 90 degree counter-clockwise

    /**
     * Returns the next {@link TileRotation} relative from this one.
     * @param direction determines if it should rotate clockwise or counterclockwise.
     * @return the rotated {@link TileRotation}.
     */
    public TileRotation rotate(RotationDirection direction) {
        int newOrdinal = Math.floorMod(this.ordinal() + direction.getValue(), values().length);
        return values()[newOrdinal];
    }
}


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Encapsulates a {@link TileType} distribution for a {@link TileStack}. Does not contain any tiles.
 * @author Timur Saglam
 */
public class TileDistribution {
    private static final int MAXIMAL_ATTEMPTS = 5;
    private final Map<TileType, Integer> distribution;
    private final Map<TileType, Integer> restorationPoint;

    /**
     * Creates a default distribution.
     * @see TileType
     */
    public TileDistribution() {
        distribution = new HashMap<>();
        restorationPoint = new HashMap<>();
    }

    /**
     * Gives information about the quantity of a tile type in this distribution.
     * @param tileType is the specific tile type.
     * @return number of tiles for this type type.
     */
    public int getQuantity(TileType tileType) {
        if (distribution.containsKey(tileType)) {
            return distribution.get(tileType);
        }
        return tileType.getAmount(); // default amount if not present
    }

    /**
     * Sets the quantity for a tile type.
     * @param tileType is the tile type whose quantity is changed.
     * @param quantity is the number of tiles for the tile type.
     */
    public void setQuantity(TileType tileType, int quantity) {
        distribution.put(tileType, quantity);
    }

    /**
     * Creates a restoration point, allowing to reset the distribution to its current state.
     */
    public void createBackup() {
        restorationPoint.clear();
        distribution.forEach(restorationPoint::put);
    }

    /**
     * Restores the distribution to the state at the last restoration point. Restores default distribution if no restoration
     * point has been created.
     */
    public void restoreLastBackup() {
        distribution.clear();
        restorationPoint.forEach(restorationPoint::put);
    }

    /**
     * Resets the distribution to the default distribution.
     */
    public void reset() {
        distribution.clear();
    }

    /**
     * Shuffles the tile amounts. The shuffle is not completely random as it tries to avoid giving a tile type its original
     * amount.
     */
    public void shuffle() {
        TileType.enabledTiles().forEach(it -> distribution.putIfAbsent(it, it.getAmount()));
        Stack<Integer> tileAmounts = new Stack<>();
        tileAmounts.addAll(distribution.values());
        Collections.shuffle(tileAmounts);
        TileType.enabledTiles().forEach(it -> distribution.put(it, getPseudoRandomAmount(it, tileAmounts)));
    }

    /**
     * Chooses a pseudo-random amount from a stack of amounts for a certain tile type.
     */
    private int getPseudoRandomAmount(TileType tileType, Stack<Integer> randomAmounts) {
        int amount = randomAmounts.peek();
        int attempts = 0;
        while (amount == tileType.getAmount() && attempts < MAXIMAL_ATTEMPTS) {
            Collections.shuffle(randomAmounts);
            amount = randomAmounts.peek();
            attempts++;
        }
        return randomAmounts.pop();
    }
}


import static carcassonne.model.terrain.TerrainType.CASTLE;
import static carcassonne.model.terrain.TerrainType.FIELDS;
import static carcassonne.model.terrain.TerrainType.MONASTERY;
import static carcassonne.model.terrain.TerrainType.OTHER;
import static carcassonne.model.terrain.TerrainType.ROAD;
import static java.util.Arrays.stream;

import java.util.List;
import java.util.Locale;

import carcassonne.model.terrain.TerrainType;

/**
 * Enumeration for the specific type of tiles. It allows easy construction of tile objects. This enum show the entirety
 * of hard-coded data in this game. Everything else is algorithmically calculated from this data (e.g. tile placement,
 * emblems, pattern completion). Values use lower case for easy image importing.
 * @author Timur
 */
public enum TileType { // TODO (MEDIUM) [STYLE] rename enum values and tile image resources.
    Null(0, OTHER, OTHER, OTHER, OTHER, OTHER, OTHER, OTHER, OTHER, OTHER),
    CastleCenter(1, CASTLE, CASTLE, CASTLE, CASTLE, CASTLE, CASTLE, CASTLE, CASTLE, CASTLE),
    CastleCenterEntry(3, CASTLE, CASTLE, ROAD, CASTLE, CASTLE, FIELDS, FIELDS, CASTLE, CASTLE),
    CastleCenterSide(4, CASTLE, CASTLE, FIELDS, CASTLE, CASTLE, FIELDS, FIELDS, CASTLE, CASTLE),
    CastleEdge(5, CASTLE, CASTLE, FIELDS, FIELDS, CASTLE, FIELDS, FIELDS, FIELDS, FIELDS),
    CastleEdgeRoad(4, CASTLE, CASTLE, ROAD, ROAD, CASTLE, FIELDS, FIELDS, FIELDS, ROAD),
    CastleSides(3, CASTLE, FIELDS, CASTLE, FIELDS, FIELDS, FIELDS, FIELDS, FIELDS, FIELDS),
    CastleSidesEdge(2, CASTLE, FIELDS, FIELDS, CASTLE, FIELDS, FIELDS, FIELDS, OTHER, FIELDS),
    CastleTube(2, FIELDS, CASTLE, FIELDS, CASTLE, FIELDS, FIELDS, FIELDS, FIELDS, CASTLE),
    CastleWall(5, CASTLE, FIELDS, FIELDS, FIELDS, FIELDS, FIELDS, FIELDS, FIELDS, FIELDS),
    CastleWallCurveLeft(3, CASTLE, FIELDS, ROAD, ROAD, FIELDS, FIELDS, FIELDS, FIELDS, ROAD),
    CastleWallCurveRight(3, CASTLE, ROAD, ROAD, FIELDS, FIELDS, FIELDS, FIELDS, FIELDS, ROAD),
    CastleWallJunction(3, CASTLE, ROAD, ROAD, ROAD, FIELDS, FIELDS, FIELDS, FIELDS, OTHER),
    CastleWallRoad(4, CASTLE, ROAD, FIELDS, ROAD, FIELDS, FIELDS, FIELDS, FIELDS, ROAD),
    Monastery(4, FIELDS, FIELDS, FIELDS, FIELDS, FIELDS, FIELDS, FIELDS, FIELDS, MONASTERY),
    MonasteryRoad(2, FIELDS, FIELDS, ROAD, FIELDS, FIELDS, FIELDS, FIELDS, FIELDS, MONASTERY),
    Road(7, ROAD, FIELDS, ROAD, FIELDS, FIELDS, FIELDS, FIELDS, FIELDS, ROAD),
    RoadCurve(8, FIELDS, FIELDS, ROAD, ROAD, FIELDS, FIELDS, FIELDS, FIELDS, ROAD),
    RoadJunctionLarge(1, ROAD, ROAD, ROAD, ROAD, FIELDS, FIELDS, FIELDS, FIELDS, OTHER),
    RoadJunctionSmall(3, FIELDS, ROAD, ROAD, ROAD, FIELDS, FIELDS, FIELDS, FIELDS, OTHER),
    RoadCrossLarge(0, ROAD, ROAD, ROAD, ROAD, FIELDS, FIELDS, FIELDS, FIELDS, ROAD),
    RoadCrossSmall(2, FIELDS, ROAD, ROAD, ROAD, FIELDS, FIELDS, FIELDS, FIELDS, ROAD),
    CastleTubeEntries(1, ROAD, CASTLE, ROAD, CASTLE, FIELDS, FIELDS, FIELDS, FIELDS, CASTLE),
    CastleTubeEntry(1, FIELDS, CASTLE, ROAD, CASTLE, FIELDS, FIELDS, FIELDS, FIELDS, CASTLE),
    MonasteryCastle(1, CASTLE, CASTLE, CASTLE, CASTLE, CASTLE, CASTLE, CASTLE, CASTLE, MONASTERY),
    MonasteryJunction(1, ROAD, ROAD, ROAD, ROAD, FIELDS, FIELDS, FIELDS, FIELDS, MONASTERY),
    CastleSidesRoad(2, CASTLE, ROAD, CASTLE, ROAD, FIELDS, FIELDS, FIELDS, FIELDS, ROAD),
    CastleSidesEdgeRoad(1, CASTLE, ROAD, ROAD, CASTLE, FIELDS, FIELDS, FIELDS, OTHER, ROAD),
    CastleSidesQuad(1, CASTLE, CASTLE, CASTLE, CASTLE, OTHER, OTHER, OTHER, OTHER, FIELDS),
    RoadEnd(1, FIELDS, FIELDS, ROAD, FIELDS, FIELDS, FIELDS, FIELDS, FIELDS, OTHER),
    CastleCenterSides(1, FIELDS, FIELDS, CASTLE, FIELDS, OTHER, OTHER, OTHER, OTHER, CASTLE),
    CastleMini(1, FIELDS, FIELDS, ROAD, FIELDS, FIELDS, FIELDS, FIELDS, FIELDS, CASTLE),
    CastleWallEntryLeft(1, CASTLE, FIELDS, FIELDS, ROAD, FIELDS, FIELDS, FIELDS, FIELDS, ROAD),
    CastleWallEntryRight(1, CASTLE, ROAD, FIELDS, FIELDS, FIELDS, FIELDS, FIELDS, FIELDS, ROAD),
    CastleWallEntry(2, CASTLE, FIELDS, ROAD, FIELDS, FIELDS, FIELDS, FIELDS, FIELDS, ROAD);

    private final TerrainType[] terrain;
    private final int amount;

    /**
     * Basic constructor for the terrain type, sets the terrain and the amount.
     * @param amount is the standard amount of tiles of this type in a stack.
     * @param terrain are the terrain types on the tiles of this tile type
     */
    TileType(int amount, TerrainType... terrain) {
        this.amount = amount;
        this.terrain = terrain;
    }

    /**
     * Getter for the standard amount of tiles of this type in a stack.
     * @return the amount of tiles.
     */
    public int getAmount() {
        return amount;
    }

    /**
     * Getter for the terrain types on the tiles of this tile type.
     * @return the terrain types of the tile.
     */
    public TerrainType[] getTerrain() {
        return terrain;
    }

    /**
     * Returns the tile type name with spaces between names in lower case.
     * @return the readable representation, such as "castle wall".
     */
    public String readableRepresentation() {
        return toString().replaceAll("([^_])([A-Z])", "$1 $2").toLowerCase(Locale.UK);
    }

    /**
     * Returns a list of all valid tile types.
     * @return all tile types except {@link TileType#Null}.
     */
    public static List<TileType> validTiles() {
        return stream(values()).filter(it -> it != Null).toList();
    }

    /**
     * Returns a list of all valid tiles that are available in game.
     * @return the list of enabled tiles.
     */
    public static List<TileType> enabledTiles() {
        return stream(values()).filter(it -> it != Null && it.getAmount() > 0).toList();
    }

}


import java.util.Collection;
import java.util.stream.Stream;

import carcassonne.model.Meeple;
import carcassonne.model.Player;
import carcassonne.model.grid.GridDirection;
import carcassonne.model.grid.GridPattern;
import carcassonne.model.grid.GridSpot;
import carcassonne.model.terrain.TerrainType;
import carcassonne.model.tile.Tile;
import carcassonne.model.tile.TileRotation;
import carcassonne.settings.GameSettings;

/**
 * Represents a single move of a player, consisting of a placement of a tile and optionally a placement of a meeple on
 * that tile.
 * @author Timur Saglam
 */
public abstract class AbstractCarcassonneMove implements Comparable<AbstractCarcassonneMove> {

    protected final Player actingPlayer;
    protected int gainedMeeples;
    protected final GridSpot gridSpot;
    protected final GridDirection meeplePosition;
    protected final GameSettings settings;
    protected final TemporaryTile tile;
    private final double value;
    protected double fieldValue;

    /**
     * Creates the move. Does not check if the move is legal.
     * @param tile is the tile placed in the move. Needs to be assigned to a {@link GridSpot}.
     * @param meeplePosition is the position on which the meeple is placed on the tile.
     * @param actingPlayer is the player that is executing the move.
     * @param settings are the game settings.
     */
    public AbstractCarcassonneMove(TemporaryTile tile, GridDirection meeplePosition, Player actingPlayer, GameSettings settings) {
        this.tile = tile;
        this.meeplePosition = meeplePosition;
        this.actingPlayer = actingPlayer;
        this.settings = settings;
        if (!tile.isPlaced()) {
            throw new IllegalStateException("Tile needs to be placed: " + tile);
        }
        fieldValue = Double.NaN; // field score not yet set
        gridSpot = tile.getGridSpot();
        value = calculateValue();
    }

    @Override
    public int compareTo(AbstractCarcassonneMove other) {
        return Double.compare(getValue(), other.getValue());
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof AbstractCarcassonneMove otherMove) {
            return compareTo(otherMove) == 0;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(getValue());
    }

    /**
     * Getter for the actingPlayer making the move.
     * @return the actingPlayer.
     */
    public Player getActingPlayer() {
        return actingPlayer;
    }

    /**
     * Getter for the difference in placed meeples.
     * @return how many more meeples where retrieved than placed.
     */
    public int getGainedMeeples() {
        return gainedMeeples;
    }

    /**
     * Getter for the meeplePosition of the meeple placement.
     * @return the meeplePosition or null if no meeple is placed.
     */
    public GridDirection getMeeplePosition() {
        return meeplePosition;
    }

    /**
     * Getter for the terrain type where the meeple is placed.
     * @return the terrain type or null if no meeple is placed.
     */
    public TerrainType getMeepleType() {
        if (meeplePosition == null) {
            return null;
        }
        return tile.getTerrain(meeplePosition);
    }

    /**
     * Getter for the tile placed in this move.
     * @return the tile with the correct rotation.
     */
    public Tile getOriginalTile() {
        return tile.getOriginal();
    }

    /**
     * Specifies which rotation needs to be applied to the original tile in order to correctly place it.
     * @return the rotation rotation for this move.
     */
    public TileRotation getRequiredTileRotation() {
        return tile.getRotation();
    }

    /**
     * Getter for the combined value of the move for all grid patterns.
     * @return the combined value.
     */
    public double getValue() {
        return value;
    }

    /**
     * Getter for the value of the move regarding field patterns.
     * @return the field value.
     */
    public double getFieldValue() {
        return fieldValue;
    }

    /**
     * Getter for the x-coordinate of the tile placement.
     * @return the x-coordinate on the grid.
     */
    public int getX() {
        return gridSpot.getX();
    }

    /**
     * Getter for the y-coordinate of the tile placement.
     * @return the y-coordinate on the grid.
     */
    public int getY() {
        return gridSpot.getY();
    }

    /**
     * Determines if a meeple is placed as part of this move.
     * @return true if it does.
     */
    public boolean involvesMeeplePlacement() {
        return meeplePosition != null;
    }

    /**
     * Checks whether this move involves placing a meeple on a field.
     * @return true if it is a fields move.
     */
    public boolean isFieldMove() {
        return involvesMeeplePlacement() && tile.getTerrain(meeplePosition) == TerrainType.FIELDS;
    }

    @Override
    public String toString() {
        String meeple = involvesMeeplePlacement() ? tile.getTerrain(meeplePosition) + " on " + meeplePosition : "without meeple";
        return getClass().getSimpleName() + " for " + actingPlayer.getName() + " with value " + value + " (field value: " + fieldValue + "): "
                + tile.getType() + " " + meeple + " " + gridSpot;
    }

    /**
     * Calculates how many meeples the acting player employs on a set of grid patterns.
     * @param patterns are the grid patterns to check.
     * @return the number of employed meeples.
     */
    protected int calculateEmployedMeeples(Collection<GridPattern> patterns) {
        Stream<Meeple> localMeeples = patterns.stream().flatMap(it -> it.getMeepleList().stream());
        return Math.toIntExact(localMeeples.filter(it -> it.getOwner() == actingPlayer).count());
    }

    /**
     * Calculates the value of the move as well as the pure field value.
     * @return the value of the move.
     * @see AbstractCarcassonneMove#getValue()
     * @see AbstractCarcassonneMove#getFieldValue()
     */
    protected abstract double calculateValue();

}

import carcassonne.model.Meeple;
import carcassonne.model.Player;

/**
 * Temporary meeples that prevents
 * @author Timur Saglam
 */
public class TemporaryMeeple extends Meeple {

    public TemporaryMeeple(Player owner) {
        super(owner);
    }

    @Override
    public void removePlacement() {
        throw new IllegalStateException("Temporary meeple is being removed. Should not be used as real meeple.");
    }
}


import java.util.Comparator;

import carcassonne.model.grid.GridSpot;
import carcassonne.util.MinkowskiDistance;

/**
 * Comparator to compare moves with an equal value based on simple heuristic rules.
 * @author Timur Saglam
 */
public class RuleBasedComparator implements Comparator<AbstractCarcassonneMove> {
    private static final int ROUNDING_FACTOR = 100;
    private final GridSpot center;

    private final MinkowskiDistance distanceMeasure;

    /**
     * Creates a rule-based comparator for moves.
     * @param center is the grid spot in the center of the grid, where the foundation tile is placed.
     */
    public RuleBasedComparator(GridSpot center, MinkowskiDistance distanceMeasure) {
        this.center = center;
        this.distanceMeasure = distanceMeasure;
    }

    @Override
    public int compare(AbstractCarcassonneMove firstMove, AbstractCarcassonneMove secondMove) {
        if (firstMove.getGainedMeeples() != secondMove.getGainedMeeples()) {
            // Rule 1: Prefer move with a maximal meeple gain
            return firstMove.getGainedMeeples() - secondMove.getGainedMeeples();
        }
        if (firstMove.involvesMeeplePlacement() != secondMove.involvesMeeplePlacement()) {
            // Rule 2: Prefer move without meeple placement
            return preferFalse(firstMove.involvesMeeplePlacement(), secondMove.involvesMeeplePlacement());
        }
        // Rule 3: Choose in the order of castle > monastery > road > fields
        int moveTypeDifference = compareMoveType(firstMove) - compareMoveType(secondMove);
        if (moveTypeDifference != 0) {
            return moveTypeDifference;
        }
        // Rule 4: Finally, choose closest move to the center of the grid
        return (int) (ROUNDING_FACTOR * (distanceToCenter(secondMove) - distanceToCenter(firstMove)));

    }

    private int compareMoveType(AbstractCarcassonneMove move) {
        if (move.involvesMeeplePlacement()) {
            switch (move.getMeepleType()) {
                case CASTLE:
                    return 3;
                case MONASTERY:
                    return 2;
                case ROAD:
                    return 1;
                default:
            }
        }
        return 0; // fields or no meeple placed
    }

    private double distanceToCenter(AbstractCarcassonneMove move) {
        return distanceMeasure.distance(center.getX(), center.getY(), move.getX(), move.getY());
    }

    private int preferFalse(Boolean first, Boolean second) {
        return second.compareTo(first);
    }

}


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;

import carcassonne.model.Player;
import carcassonne.model.grid.Grid;
import carcassonne.model.tile.Tile;
import carcassonne.model.tile.TileStack;
import carcassonne.settings.GameSettings;

public class RuleBasedAI implements ArtificialIntelligence {
    private static final double REQUIRED_FIELD_VALUE = 12;
    private static final int UPPER_BOUND = 75; // tiles for max required field value
    private static final int LOWER_BOUND = 25; // tiles for min required field value
    private static final double OFFSET = 0.5;
    private static final double MEEPLE_VALUE_FACTOR = 0.5;
    private static final double LAST_MEEPLE_INCENTIVE = 2.5;
    private static final String EMPTY_COLLECTION = "Cannot choose random element from empty collection!";
    private static final double EPSILON = 0.01;
    private final GameSettings settings;
    private final Random random;
    private Optional<AbstractCarcassonneMove> currentMove;

    public RuleBasedAI(GameSettings settings) {
        this.settings = settings;
        random = new Random();
    }

    @Override
    public Optional<AbstractCarcassonneMove> calculateBestMoveFor(Collection<Tile> tiles, Player player, Grid grid, TileStack stack) {
        currentMove = Optional.empty();
        Collection<AbstractCarcassonneMove> possibleMoves = new ArrayList<>();
        for (Tile tile : tiles) {
            possibleMoves.addAll(grid.getPossibleMoves(tile, player, settings));
        }
        // RULE 1: Only consider move with a positive value:
        List<AbstractCarcassonneMove> consideredMoves = possibleMoves.stream().filter(it -> it.getValue() >= 0).toList();
        // RULE 2: Do not place last meeple on fields (except at the end):
        if (player.getFreeMeeples() == 1 && stack.getSize() > settings.getNumberOfPlayers()) {
            consideredMoves = consideredMoves.stream().filter(it -> !it.isFieldMove()).toList();
        }
        // RULE 3: Avoid placing low value fields early in the game:
        consideredMoves = filterEarlyFieldMoves(consideredMoves, stack, player);
        // RULE 4: Find best move based on score value and meeple value
        if (!consideredMoves.isEmpty()) {
            double maximumValue = consideredMoves.stream().mapToDouble(it -> combinedValue(it, stack)).max().getAsDouble();
            Stream<AbstractCarcassonneMove> bestMoves = consideredMoves.stream().filter(it -> combinedValue(it, stack) == maximumValue);
            currentMove = chooseAmongBestMoves(bestMoves.toList(), grid);
        }
        System.out.println(currentMove); // TODO (HIGH) [AI] remove debug output
        return currentMove;
    }

    @Override
    public Tile chooseTileToDrop(Collection<Tile> tiles) {
        return chooseRandom(tiles); // TODO (HIGH) [AI] find a meaningful heuristic
    }

    @Override
    public Optional<AbstractCarcassonneMove> getCurrentMove() {
        return currentMove;
    }

    private Optional<AbstractCarcassonneMove> chooseAmongBestMoves(List<AbstractCarcassonneMove> listOfMoves, Grid grid) {
        RuleBasedComparator comparator = new RuleBasedComparator(grid.getFoundation(), settings.getDistanceMeasure());
        AbstractCarcassonneMove maximum = Collections.max(listOfMoves, comparator);
        List<AbstractCarcassonneMove> bestMoves = listOfMoves.stream().filter(it -> comparator.compare(it, maximum) == 0).toList();
        return Optional.of(chooseRandom(bestMoves));
    }

    private <T> T chooseRandom(Collection<T> elements) {
        Optional<T> randomElement = elements.stream().skip(random.nextInt(elements.size())).findFirst();
        return randomElement.orElseThrow(() -> new IllegalArgumentException(EMPTY_COLLECTION));
    }

    /**
     * Filters field moves if their value is too low. The required value decreases with a shrinking tile stack.
     */
    private List<AbstractCarcassonneMove> filterEarlyFieldMoves(Collection<AbstractCarcassonneMove> moves, TileStack stack, Player player) {
        double tiles = Math.max(LOWER_BOUND, Math.min(stack.getSize(), UPPER_BOUND));
        double variableRequiredValue = REQUIRED_FIELD_VALUE * (tiles / (UPPER_BOUND - LOWER_BOUND) - OFFSET);
        double requiredValue = player.getUnretrievableMeeples() + variableRequiredValue;
        return moves.stream().filter(move -> !move.isFieldMove() || move.getFieldValue() > requiredValue).toList();
    }

    private double combinedValue(AbstractCarcassonneMove move, TileStack stack) {
        double meepleValue = variableMeepleValue(move, stack);
        if (move.getValue() > 0 && move.getValue() + meepleValue <= 0 && move.getActingPlayer().getFreeMeeples() > 1) {
            return EPSILON; // meeple value should only lead to wasted moves if there is only one meeple left
        }
        return move.getValue() + meepleValue;
    }

    /**
     * Calculates the value of the spend and retrieved meeples. Depends on the fill level of the tile stack.
     */
    private double variableMeepleValue(AbstractCarcassonneMove move, TileStack stack) {
        int meepleDifference = move.getGainedMeeples();
        int freeMeeples = move.getActingPlayer().getFreeMeeples();
        if (endIsNear(stack, move.getActingPlayer()) && meepleDifference < 0) {
            return 0;
        }
        double value = 0;
        if (freeMeeples == 0 && meepleDifference > 0 || freeMeeples == 1 && meepleDifference < 0) {
            value += LAST_MEEPLE_INCENTIVE;
        }
        for (int i = 0; i < Math.abs(meepleDifference); i++) {
            value += (GameSettings.MAXIMAL_MEEPLES - freeMeeples) * MEEPLE_VALUE_FACTOR;
        }
        return value * Math.signum(meepleDifference);
    }

    /**
     * The end is near if a player has equal or less moves left than meeples.
     */
    private boolean endIsNear(TileStack stack, Player player) {
        return stack.getSize() / (double) settings.getNumberOfPlayers() <= player.getFreeMeeples();
    }

}


import carcassonne.model.Player;
import carcassonne.model.grid.GridDirection;
import carcassonne.model.grid.GridSpot;
import carcassonne.model.tile.Tile;
import carcassonne.model.tile.TileRotation;
import carcassonne.settings.GameSettings;

/**
 * Tile that is only temporarily placed to analyze possible moves.
 * @author Timur Saglam
 */
public class TemporaryTile extends Tile {

    private final Tile original;

    public TemporaryTile(Tile original) {
        super(original.getType());
        this.original = original;
    }

    public TemporaryTile(Tile original, TileRotation rotation) {
        this(original);
        rotateTo(rotation);
    }

    /**
     * Returns the original tile from which this tile is copied from.
     * @return the original tile.
     */
    public Tile getOriginal() {
        return original;
    }

    @Override
    public void placeMeeple(Player player, GridDirection position, GameSettings settings) {
        super.placeMeeple(player, position, new TemporaryMeeple(player), settings);
    }

    @Override
    public void removeMeeple() {
        meeple = null;
    }

    @Override
    public void setPosition(GridSpot spot) {
        gridSpot = spot; // no null check, allows removing spot
    }

}


import static carcassonne.model.terrain.TerrainType.FIELDS;

import java.util.Collection;

import carcassonne.model.Player;
import carcassonne.model.grid.GridDirection;
import carcassonne.model.grid.GridPattern;
import carcassonne.model.grid.GridSpot;
import carcassonne.settings.GameSettings;

/**
 * Represents a single move of a player which is valued according to the immediate value of the single move when
 * modeling the Carcassonne move as a zero-sum game.
 * @author Timur Saglam
 */
public class ZeroSumMove extends AbstractCarcassonneMove {

    /**
     * Creates the move. Does not check if the move is legal.
     * @param tile is the tile placed in the move. Needs to be assigned to a {@link GridSpot}.
     * @param meeplePosition is the position on which the meeple is placed on the tile.
     * @param actingPlayer is the player that is executing the move.
     * @param settings are the game settings.
     */
    public ZeroSumMove(TemporaryTile tile, GridDirection meeplePosition, Player actingPlayer, GameSettings settings) {
        super(tile, meeplePosition, actingPlayer, settings);
    }

    /**
     * Creates the move without a meeple placement. Does not check if the move is legal.
     * @param tile is the tile placed in the move. Needs to be assigned to a {@link GridSpot}.
     * @param actingPlayer is the player that is executing the move.
     * @param settings are the game settings.
     */
    public ZeroSumMove(TemporaryTile tile, Player actingPlayer, GameSettings settings) {
        this(tile, null, actingPlayer, settings);
    }

    @Override
    protected double calculateValue() {
        gridSpot.removeTile();
        Collection<GridPattern> patterns = gridSpot.getGrid().getLocalPatterns(gridSpot);
        double scoreBefore = patterns.stream().mapToInt(this::zeroSumScore).sum();
        double fieldScoreBefore = patterns.stream().filter(it -> it.getType() == FIELDS).mapToInt(this::zeroSumScore).sum();
        gainedMeeples = calculateEmployedMeeples(patterns);
        gridSpot.place(tile, gridSpot.getGrid().isAllowingEnclaves());
        if (involvesMeeplePlacement()) {
            tile.placeMeeple(actingPlayer, meeplePosition, new TemporaryMeeple(actingPlayer), settings);
        }
        patterns = gridSpot.getGrid().getLocalPatterns(gridSpot);
        double scoreAfter = patterns.stream().mapToInt(this::zeroSumScore).sum();
        double fieldScoreAfter = patterns.stream().filter(it -> it.getType() == FIELDS).mapToInt(this::zeroSumScore).sum();
        gainedMeeples -= calculateEmployedMeeples(patterns);
        tile.removeMeeple();
        fieldValue = fieldScoreAfter - fieldScoreBefore;
        return scoreAfter - scoreBefore;
    }

    private int zeroSumScore(GridPattern pattern) {
        int score = pattern.getScoreFor(actingPlayer); // acting players gain
        for (Player dominantPlayer : pattern.getDominantPlayers()) {
            if (dominantPlayer != actingPlayer) {
                score -= pattern.getScoreFor(dominantPlayer); // other players gain = acting players loss
            }
        }
        return score;
    }
}


import java.util.Collection;
import java.util.Optional;

import carcassonne.model.Player;
import carcassonne.model.grid.Grid;
import carcassonne.model.tile.Tile;
import carcassonne.model.tile.TileStack;

public interface ArtificialIntelligence {
    /**
     * Returns the last calculated best move without recomputing it.
     * @return
     */
    Optional<AbstractCarcassonneMove> getCurrentMove();

    /**
     * Calculates a new best move for a player who places a tile on a grid. This move can be retrieved without recomputing
     * it with {@link ArtificialIntelligence#getCurentMove()}
     * @param tile is the set of tiles to choose from.
     * @param player is the player who places the tile.
     * @param grid is the grid on which the tile should be placed.
     */
    Optional<AbstractCarcassonneMove> calculateBestMoveFor(Collection<Tile> tiles, Player player, Grid grid, TileStack stack);

    /**
     * Determines which tile to drop when the AI is skipping a turn.
     * @param tiles is a list of tiles to drop.
     * @return the best tile to drop.
     */
    Tile chooseTileToDrop(Collection<Tile> tiles);

}


import carcassonne.model.terrain.TerrainType;

/**
 * @author Timur Saglam
 */
public class CastleAndRoadPattern extends GridPattern { // TODO (MEDIUM) [STYLE] use subclasses to make constructors generic (factory?)
    private static final double UNFINISHED_CASTLE_MULTIPLIER = 0.5;

    /**
     * Public constructor for creating road and monastery patterns.
     * @param startingSpot is the starting spot of the pattern.
     * @param startingDirection is the starting direction of the pattern.
     * @param patternType is the type of the pattern.
     * @param grid is the grid the pattern is created on.
     */
    public CastleAndRoadPattern(GridSpot startingSpot, GridDirection startingDirection, TerrainType patternType) {
        super(patternType, patternType == TerrainType.CASTLE ? 2 : 1);
        checkArgs(startingSpot, startingDirection, patternType);
        startingSpot.setTag(startingDirection, this); // initial tag
        add(startingSpot); // initial tile
        complete = buildPattern(startingSpot, startingDirection); // recursive algorithm.
    }

    @Override
    public int getPatternScore() {
        int baseScore = super.getPatternScore();
        if (patternType == TerrainType.CASTLE) {
            int emblems = (int) containedSpots.stream().filter(it -> it.getTile().hasEmblem()).count(); // count emblems
            baseScore += emblems * scoreMultiplier;
            if (!complete) {
                baseScore *= UNFINISHED_CASTLE_MULTIPLIER;
            }
        }
        return baseScore;
    }

    private boolean buildPattern(GridSpot spot, GridDirection startingPoint) {
        boolean isClosed = true;
        for (GridDirection direction : GridDirection.directNeighbors()) { // for every side
            if (spot.getTile().hasConnection(startingPoint, direction)) { // if is connected side
                GridSpot neighbor = spot.getGrid().getNeighbor(spot, direction); // get the neighbor
                if (neighbor == null) { // if it has no neighbor
                    isClosed = false; // open side, can't be finished pattern.
                } else { // continue on neighbors
                    isClosed &= checkNeighbor(spot, neighbor, direction);
                }
            }
        }
        return isClosed;
    }

    private void checkArgs(GridSpot spot, GridDirection direction, TerrainType terrain) {
        if (terrain != TerrainType.CASTLE && terrain != TerrainType.ROAD) {
            throw new IllegalArgumentException("Can only create CastleAndRoadPatterns from type castle or road");
        }
        checkArgs(spot, direction);
    }

    private boolean checkNeighbor(GridSpot startingTile, GridSpot neighbor, GridDirection direction) {
        GridDirection oppositeDirection = direction.opposite();
        if (!neighbor.isIndirectlyTaggedBy(oppositeDirection, this)) { // if neighbor not visited yet
            startingTile.setTag(direction, this);
            neighbor.setTag(oppositeDirection, this); // mark as visited
            add(neighbor); // add to pattern
            return buildPattern(neighbor, oppositeDirection);
        }
        return true;
    }
}


import static carcassonne.model.grid.GridDirection.CENTER;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import carcassonne.model.terrain.TerrainType;
import carcassonne.model.tile.Tile;

/**
 * The class represents a spot on the grid.
 * @author Timur Saglam
 */
public class GridSpot {

    private final Grid grid;
    private final Map<GridDirection, Set<GridPattern>> tagMap; // maps tagged location to the patterns.
    private Tile tile;
    private final int x;
    private final int y;

    /**
     * Creates a grid spot for a specific grid on a specific position.
     * @param grid is the grid.
     * @param x is the x coordinate of the position.
     * @param y is the y coordinate of the position.
     */
    public GridSpot(Grid grid, int x, int y) {
        this.grid = grid;
        this.x = x;
        this.y = y;
        tagMap = new EnumMap<>(GridDirection.class);
        for (GridDirection direction : GridDirection.values()) {
            tagMap.put(direction, new HashSet<>());
        }
    }

    /**
     * Creates list of all patterns that are affected by this spot.
     * @return the list of patterns.
     */
    public Collection<GridPattern> createPatternList() {
        if (isFree()) {
            throw new IllegalStateException("GridSpot is free, cannot create patterns");
        }
        List<GridPattern> results = new LinkedList<>();
        // first, check for castle and road patterns:
        for (GridDirection direction : GridDirection.tilePositions()) {
            TerrainType terrain = tile.getTerrain(direction); // get terrain type.
            if ((terrain == TerrainType.CASTLE || terrain == TerrainType.ROAD) && !isIndirectlyTagged(direction)) {
                results.add(new CastleAndRoadPattern(this, direction, terrain));
            }
        }
        // then, check fields:
        for (GridDirection direction : GridDirection.values()) {
            TerrainType terrain = tile.getTerrain(direction); // get terrain type.
            if (terrain == TerrainType.FIELDS && !isIndirectlyTagged(direction)) {
                results.add(new FieldsPattern(this, direction));
            }
        }
        // then check for monastery patterns:
        addPatternIfMonastery(this, results); // the tile itself
        grid.getNeighbors(this, false, GridDirection.neighbors()).forEach(it -> addPatternIfMonastery(it, results));
        return results; // return all patterns.
    }

    /**
     * Forces to place a tile on the grid spot.
     * @param tile is the tile to place.
     */
    public void forcePlacement(Tile tile) {
        this.tile = tile;
        tile.setPosition(this);
    }

    /**
     * Getter for the tile.
     * @return the tile, or null if the grid spot has no tile.
     */
    public Tile getTile() {
        return tile;
    }

    /**
     * Getter for the grid of which this grid spot is part of.
     * @return the containing grid.
     */
    public Grid getGrid() {
        return grid;
    }

    /**
     * Getter for the x coordinate of the spot.
     * @return the x coordinate.
     */
    public int getX() {
        return x;
    }

    /**
     * Getter for the y coordinate of the spot.
     * @return the y coordinate.
     */
    public int getY() {
        return y;
    }

    /**
     * Determines if this grid spot was recently tagged by any grid pattern on a specified position or a position connected
     * to the specified position.
     * @param tilePosition is the specific position.
     * @return true if not directly or indirectly tagged.
     */
    public boolean isIndirectlyTagged(GridDirection tilePosition) {
        for (GridDirection otherPosition : GridDirection.values()) {
            if (isTagged(otherPosition) && tile.hasConnection(tilePosition, otherPosition)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if this grid spot was recently tagged by a specific grid pattern on a specified position or a position
     * connected to the specified position.
     * @param tilePosition is the specific position.
     * @param tagger is the specific grid pattern.
     * @return true if not directly or indirectly tagged by the grid pattern.
     */
    public boolean isIndirectlyTaggedBy(GridDirection tilePosition, GridPattern tagger) {
        for (GridDirection otherPosition : GridDirection.values()) {
            if (tile.hasConnection(tilePosition, otherPosition) && tagMap.get(otherPosition).contains(tagger)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether the grid spot is free.
     * @return true if free
     */
    public boolean isFree() {
        return tile == null;
    }

    /**
     * Checks whether the grid spot is occupied.
     * @return true if occupied
     */
    public boolean isOccupied() {
        return tile != null;
    }

    /**
     * Checks whether a tile can be placed on this spot. This requires the spot to be free, the tile to be compatible with
     * the neighboring tiles, and being compatible with the enclave game rule if present.
     * @param tile is the tile to place.
     * @param allowEnclaves determines if it is legal to enclose free spots.
     * @return true if the tile can be placed.
     */
    public boolean isPlaceable(Tile tile, boolean allowEnclaves) {
        if (isOccupied()) {
            return false; // can't be placed if spot is occupied.
        }
        int neighborCount = 0;
        for (GridDirection direction : GridDirection.directNeighbors()) { // for every direction
            GridSpot neighbor = grid.getNeighbor(this, direction);
            if (neighbor == null) { // free space
                if (!allowEnclaves && grid.isClosingFreeSpotsOff(this, direction)) {
                    return false; // you can't close off free spaces
                }
            } else { // if there is a neighbor in the direction.
                neighborCount++;
                if (!tile.canConnectTo(direction, neighbor.getTile())) {
                    return false; // if it does not fit to terrain, it can't be placed.
                }
            }
        }
        return neighborCount > 0; // can be placed beneath another tile.
    }

    /**
     * Removes all the tags from the tile.
     */
    public void removeTags() {
        tagMap.values().forEach(Set::clear);
    }

    /**
     * Removes all the tags of a specific pattern from the tile.
     * @param pattern is the specific grid pattern.
     */
    public void removeTagsFrom(GridPattern pattern) {
        tagMap.values().forEach(it -> it.remove(pattern));
    }

    /**
     * Set tile on grid spot if possible.
     * @param tile is the tile to set.
     * @param allowEnclaves determines if it is legal to enclose free spots.
     * @return true if the tile could be placed.
     */
    public boolean place(Tile tile, boolean allowEnclaves) {
        if (isPlaceable(tile, allowEnclaves)) {
            tile.setPosition(this);
            this.tile = tile;
            return true; // tile was successfully placed.
        }
        return false; // tile can't be placed, spot is occupied.
    }

    /**
     * Removes any placed tile from the grid spot, updates the position of the tile.
     */
    public void removeTile() { // TODO (HIGH) [AI] this should be only allowed for temporary tiles.
        if (tile != null) {
            tile.setPosition(null);
            tile = null;
        }
    }

    /**
     * tag the tile as recently checked by grid pattern checks for a specific direction.
     * @param direction is the tag direction.
     * @param tagger is the {@link GridPattern} that tagged the spot.
     */
    public void setTag(GridDirection direction, GridPattern tagger) {
        tagMap.get(direction).add(tagger);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[on: (" + x + "|" + y + "), Occupied:" + isOccupied() + "]";
    }

    private void addPatternIfMonastery(GridSpot spot, List<GridPattern> patternList) {
        if (spot.getTile().getTerrain(CENTER) == TerrainType.MONASTERY && !spot.isIndirectlyTagged(CENTER)) {
            patternList.add(new MonasteryPattern(spot));
        }
    }

    /**
     * Method determines if tile recently was tagged by grid pattern checks on a specific position or not.
     * @param tilePosition is the specific position.
     * @return true if it was tagged.
     */
    private Boolean isTagged(GridDirection direction) {
        return !tagMap.get(direction).isEmpty();
    }
}


import static carcassonne.model.grid.GridDirection.CENTER;
import static carcassonne.model.grid.GridDirection.NORTH_WEST;
import static carcassonne.model.grid.GridDirection.WEST;
import static carcassonne.model.terrain.RotationDirection.LEFT;
import static carcassonne.model.terrain.RotationDirection.RIGHT;
import static carcassonne.model.terrain.TerrainType.CASTLE;
import static carcassonne.model.terrain.TerrainType.FIELDS;

import java.util.LinkedList;
import java.util.List;

import carcassonne.model.tile.Tile;

/**
 * Grid pattern for fields.
 * @author Timur Saglam
 */
public class FieldsPattern extends GridPattern {
    private static final int POINTS_PER_CASTLE = 3;
    private final List<CastleAndRoadPattern> adjacentCastles;
    private final Grid grid;

    /**
     * Creates a new field pattern.
     * @param startingSpot is the {@link GridSpot} where the pattern starts.
     * @param startingDirection is the position on the spot where the pattern starts.
     * @param grid is the correlating {@link Grid}.
     */
    public FieldsPattern(GridSpot startingSpot, GridDirection startingDirection) {
        super(FIELDS, POINTS_PER_CASTLE);
        checkArgs(startingSpot, startingDirection);
        grid = startingSpot.getGrid();
        adjacentCastles = new LinkedList<>();
        checkArgs(startingSpot, startingDirection);
        startingSpot.setTag(startingDirection, this); // initial tag, is needed for adding meeples!
        add(startingSpot); // initial tile
        buildPattern(startingSpot, startingDirection);
        adjacentCastles.forEach(CastleAndRoadPattern::removeOwnTags); // also remove the tile tags of the marked adjacentCastles
    }

    @Override
    public int getPatternScore() {
        return adjacentCastles.size() * scoreMultiplier;
    }

    // adds a grid direction to a list if it has not castle terrain at that direction on the tile.
    private void addIfNotCastle(List<GridDirection> results, Tile tile, GridDirection next) {
        if (tile.getTerrain(next) != CASTLE) {
            results.add(next);
        }
    }

    private void buildPattern(GridSpot spot, GridDirection startingPoint) {
        List<GridDirection> fieldPositions = getFieldPositions(spot.getTile(), startingPoint);
        for (GridDirection position : fieldPositions) { // for every positions of this field on this tile
            countAdjacentCastles(spot, position); // count castles to determine pattern size
            spot.setTag(position, this); // mark as visited
        }
        fieldPositions.forEach(it -> checkNeighbors(spot, it)); // check every possible neighbor
    }

    private void checkNeighbors(GridSpot spot, GridDirection position) {
        for (GridDirection connectionDirection : getFieldConnections(position, spot.getTile())) { // ∀ connection points
            GridSpot neighbor = grid.getNeighbor(spot, connectionDirection); // get the neighbor
            GridDirection oppositeDirection = getFieldOpposite(position, connectionDirection); // get the connecting position on neighbor
            if (neighbor != null && !neighbor.isIndirectlyTagged(oppositeDirection)) { // if not visited
                neighbor.setTag(oppositeDirection, this); // mark as visited
                add(neighbor); // add to pattern
                buildPattern(neighbor, oppositeDirection); // continue building recursively
            }
        }
    }

    // Counts neighboring adjacent castles for a position on at tile. Finds all castle patterns on the tile that are
    // directly adjacent to the field position and saves the complete ones.
    private void countAdjacentCastles(GridSpot spot, GridDirection position) {
        for (GridDirection neighbor : getAdjacentPositions(position)) {
            if (spot.getTile().getTerrain(neighbor) == CASTLE && isUntagged(spot, neighbor)) { // if is unvisited castle
                CastleAndRoadPattern castle = new CastleAndRoadPattern(spot, neighbor, CASTLE);
                if (castle.isComplete()) { // if castle is closed (pattern check)
                    adjacentCastles.add(castle); // remember pattern to count points
                } else {
                    castle.removeOwnTags(); // IMPORTANT, remove tags if not used any further!
                }
            }
        }
    }

    /**
     * Returns every adjacent position on a tile for a specific initial position.
     */
    private List<GridDirection> getAdjacentPositions(GridDirection position) {
        List<GridDirection> neighbors = new LinkedList<>();
        if (position.isSmallerOrEquals(WEST)) {
            neighbors.add(CENTER); // the classic direction are adjacent to the middle
        }
        if (position.isSmallerOrEquals(NORTH_WEST)) { // everything except the middle has these two neighbors:
            neighbors.add(position.nextDirectionTo(LEFT)); // counterclockwise adjacent position
            neighbors.add(position.nextDirectionTo(RIGHT)); // clockwise adjacent position
        } else {
            neighbors.addAll(GridDirection.directNeighbors()); // the middle has the classic directions as neighbors
        }
        return neighbors;

    }

    /**
     * Gives for a specific tile and a specific position on that tile the directions in which the field connects to. If the
     * tile has not the terrain field on this position the result list is empty.
     */
    private List<GridDirection> getFieldConnections(GridDirection position, Tile tile) {
        List<GridDirection> results = new LinkedList<>();
        if (tile.getTerrain(position) == FIELDS) {
            if (position.isSmallerOrEquals(WEST)) {
                results.add(position); // for simple directions just return themselves.
            } else if (position.isSmallerOrEquals(NORTH_WEST)) {
                addIfNotCastle(results, tile, position.nextDirectionTo(LEFT)); // for edges it depends whether the neighboring
                addIfNotCastle(results, tile, position.nextDirectionTo(RIGHT)); // directions have castle terrain or not
            }
        }
        return results;
    }

    // Returns the position on the grid of a neighboring tile on a direction which is directly in contact with a specific
    // position of the first tile.
    private GridDirection getFieldOpposite(GridDirection position, GridDirection neighborDirection) {
        if (position.isSmallerOrEquals(WEST)) {
            return position.opposite(); // top, right, bottom, left are simply inverted
        }
        if (position.isSmallerOrEquals(NORTH_WEST)) {
            if (neighborDirection.isLeftOf(position)) { // neighbor to the left of the corner
                return position.opposite().nextDirectionTo(LEFT).nextDirectionTo(LEFT); // return opposite and two to the right
            }
            return position.opposite().nextDirectionTo(RIGHT).nextDirectionTo(RIGHT); // return opposite and two to the left
        }
        return position; // middle stays middle
    }

    private List<GridDirection> getFieldPositions(Tile tile, GridDirection startingPoint) {
        List<GridDirection> fieldPositions = new LinkedList<>();
        for (GridDirection position : GridDirection.values()) { // for every position on tile
            if (tile.hasConnection(startingPoint, position)) {
                fieldPositions.add(position);
            }
        }
        return fieldPositions;
    }

    private boolean isUntagged(GridSpot spot, GridDirection position) {
        boolean tagged = false;
        for (CastleAndRoadPattern castle : adjacentCastles) {
            tagged |= spot.isIndirectlyTaggedBy(position, castle);
        }
        return !tagged;
    }

}

/**
 *
 */

import static carcassonne.model.grid.GridDirection.CENTER;
import static carcassonne.model.terrain.TerrainType.MONASTERY;

import java.util.List;

/**
 * This class represents a specific kind of grid pattern, the grid patterns for the terrain type MONASTERY.
 * @author Timur Saglam
 */
public class MonasteryPattern extends GridPattern {

    /**
     * Simple constructor that creates the pattern.
     * @param spot is the starting spot of the pattern, containing a monastery tile.
     * @param grid is the grid the pattern is created from.
     */
    public MonasteryPattern(GridSpot spot) {
        super(MONASTERY, 1);
        if (spot.getTile().getTerrain(CENTER) != MONASTERY) {
            throw new IllegalArgumentException("Can't create monastery pattern from non monastery tile");
        }
        buildPattern(spot);
    }

    private void buildPattern(GridSpot monasterySpot) {
        List<GridSpot> neighbors = monasterySpot.getGrid().getNeighbors(monasterySpot, false, GridDirection.neighbors());
        add(monasterySpot); // add monastery
        monasterySpot.setTag(CENTER, this);
        neighbors.forEach(it -> containedSpots.add(it));
        if (neighbors.size() == GridDirection.neighbors().size()) {
            complete = true;
        }
    }
}


import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import carcassonne.model.Meeple;
import carcassonne.model.Player;
import carcassonne.model.terrain.TerrainType;

/**
 * A pattern of connected terrain on tiles of the grid. A grid pattern contains information about the tiles of the
 * pattern and the players involved in the pattern. Also it counts the amount of meeples per player on the tiles of the
 * pattern.
 * @author Timur Saglam
 */
public class GridPattern {

    private boolean disbursed;
    protected boolean complete;
    private final Map<Player, Integer> involvedPlayers;
    private final List<Meeple> meepleList;
    protected final TerrainType patternType;
    protected int scoreMultiplier;
    protected List<GridSpot> containedSpots;

    /**
     * Basic constructor taking only a tile type.
     * @param patternType is the type of the pattern.
     * @param scoreMultiplier is the score multiplier of the pattern.
     */
    protected GridPattern(TerrainType patternType, int scoreMultiplier) {
        this.patternType = patternType;
        this.scoreMultiplier = scoreMultiplier;
        containedSpots = new LinkedList<>();
        meepleList = new LinkedList<>();
        involvedPlayers = new HashMap<>();
    }

    /**
     * Disburses complete patterns. Distributes the score among all dominant players on the pattern. Removes the meeple
     * placement and returns them to the players. Can only be called once in the lifetime of a pattern.
     * @param splitScore determines if shared patterns are scored by splitting the score or awarding full score.
     */
    public void disburse(boolean splitScore) {
        if (complete) {
            distributePatternScore(splitScore);
            meepleList.forEach(it -> it.getLocation().getTile().removeMeeple()); // remove meeples from tiles.
            involvedPlayers.clear();
        }
    }

    /**
     * Disburses incomplete patterns. Distributes the score among all dominant players on the pattern. This should be used
     * at the end of the round.
     * @param splitScore determines if shared patterns are scored by splitting the score or awarding full score.
     */
    public void forceDisburse(boolean splitScore) {
        if (!complete) {
            distributePatternScore(splitScore);
        }
    }

    /**
     * Determines the dominant players, which are the involved players with maximum amount of meeples on this pattern.
     */
    public List<Player> getDominantPlayers() {
        if (involvedPlayers.isEmpty()) {
            return Collections.emptyList();
        }
        int maximum = Collections.max(involvedPlayers.values()); // most meeples on pattern
        return involvedPlayers.keySet().stream().filter(player -> involvedPlayers.get(player) == maximum).toList();
    }

    /**
     * Getter for the meeple list.
     * @return the meeple list.
     */
    public List<Meeple> getMeepleList() {
        return meepleList;
    }

    /**
     * Returns the score of the pattern, independent of which player is dominant.
     * @return the full score.
     */
    public int getPatternScore() {
        return containedSpots.size() * scoreMultiplier;
    }

    /**
     * Returns the type of the pattern in form of a {@link TerrainType}.
     * @return the pattern type.
     */
    public TerrainType getType() {
        return patternType;
    }

    public int getScoreFor(Player player) {
        List<Player> dominantPlayers = getDominantPlayers();
        if (dominantPlayers.contains(player)) {
            return divideScore(getPatternScore(), dominantPlayers);
        }
        return 0;
    }

    /**
     * Returns the current size of the pattern, which equals the amount of added tiles.
     * @return the size.
     */
    public int getSize() {
        return containedSpots.size();
    }

    /**
     * Checks whether the pattern is complete or not. That means there cannot be
     * @return true if complete.
     */
    public boolean isComplete() {
        return complete;
    }

    /**
     * Checks whether no player has set a meeple on the pattern.
     * @return true if the pattern is not occupied, false if not.
     */
    public boolean isNotOccupied() {
        return involvedPlayers.isEmpty();
    }

    /**
     * Checks whether a specific player is involved in the occupation of the pattern. That means he has at least one meeple
     * on the pattern.
     * @param player is the specific player.
     * @return true if he is involved in the occupation of the pattern, false if not.
     */
    public boolean isOccupiedBy(Player player) {
        return involvedPlayers.containsKey(player);
    }

    /**
     * Removes all OWN tags of all tiles of the pattern.
     */
    public void removeOwnTags() {
        containedSpots.forEach(it -> it.removeTagsFrom(this));
    }

    /**
     * Removes all tags of all tiles of the pattern. Needs to be called after ALL patterns of a tile have been created.
     */
    public void removeTileTags() {
        containedSpots.forEach(GridSpot::removeTags);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("GridPattern[type: ");
        builder.append(patternType).append(", size: ").append(getSize()).append(", complete: ").append(complete);
        builder.append(", disbursed: ").append(disbursed).append(", meeples: ").append(meepleList).append(", on: ");
        builder.append(containedSpots.stream().map(it -> "(" + it.getX() + "|" + it.getY() + ")").toList());
        return builder.toString();
    }

    private void distributePatternScore(boolean splitScore) {
        if (!disbursed && !involvedPlayers.isEmpty()) {
            List<Player> dominantPlayers = getDominantPlayers();
            int stake = splitScore ? divideScore(getPatternScore(), dominantPlayers) : getPatternScore();
            for (Player player : dominantPlayers) { // dominant players split the pot
                player.addPoints(stake, patternType);
            }
            disbursed = true;
        }
    }

    // adds meeple from tile to involvedPlayers map if the meeple is involved in the pattern.
    private void addMeepleFrom(GridSpot spot) {
        assert !disbursed;
        Meeple meeple = spot.getTile().getMeeple(); // Meeple on the tile.
        if (!meepleList.contains(meeple) && isPartOfPattern(spot, meeple.getPosition())) {
            Player player = meeple.getOwner(); // owner of the meeple.
            if (involvedPlayers.containsKey(player)) {
                involvedPlayers.put(player, involvedPlayers.get(player) + 1);
            } else {
                involvedPlayers.put(player, 1);
            }
            meepleList.add(meeple);
        }
    }

    private int divideScore(int score, List<Player> dominantPlayers) {
        return (int) Math.ceil(score / (double) dominantPlayers.size());
    }

    private boolean isPartOfPattern(GridSpot spot, GridDirection position) {
        boolean onCorrectTerrain = spot.getTile().getTerrain(position) == patternType;
        boolean onPattern = spot.isIndirectlyTaggedBy(position, this) || patternType == TerrainType.MONASTERY;
        return onCorrectTerrain && onPattern;
    }

    /**
     * Adds a spot to the pattern, saving the tile on the spot, the owner of a potential Meeple on the tile.
     * @param spot is the spot to add.
     */
    protected void add(GridSpot spot) {
        containedSpots.add(spot);
        if (spot.getTile().hasMeeple()) {
            addMeepleFrom(spot);
        }
    }

    /**
     * Checks the usual inputs on being null.
     * @param spot is any grid spot.
     * @param direction is any grid direction.
     * @param grid is any grid.
     */
    protected void checkArgs(GridSpot spot, GridDirection direction) {
        if (spot == null || direction == null) {
            throw new IllegalArgumentException("Arguments can't be null");
        }
    }
}


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import carcassonne.model.Player;
import carcassonne.model.ai.AbstractCarcassonneMove;
import carcassonne.model.ai.TemporaryTile;
import carcassonne.model.ai.ZeroSumMove;
import carcassonne.model.tile.Tile;
import carcassonne.model.tile.TileRotation;
import carcassonne.model.tile.TileType;
import carcassonne.settings.GameSettings;

/**
 * The playing grid class.
 * @author Timur Saglam
 */
public class Grid {
    private static final TileType FOUNDATION_TYPE = TileType.CastleWallRoad;
    private final int width;
    private final int height;
    private final GridSpot[][] spots;
    private GridSpot foundation;
    private final boolean allowEnclaves;

    /**
     * Basic constructor
     * @param width is the grid width.
     * @param height is the grid height.
     * @param allowEnclaves determines if it is legal to enclose free spots.
     */
    public Grid(int width, int height, boolean allowEnclaves) {
        this.width = width;
        this.height = height;
        this.allowEnclaves = allowEnclaves;
        spots = new GridSpot[width][height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                spots[x][y] = new GridSpot(this, x, y);
            }
        }
        placeFoundation(FOUNDATION_TYPE);
    }

    /**
     * Returns list of all patterns on the grid.
     * @return the list of patterns.
     */
    public List<GridPattern> getAllPatterns() {
        List<GridPattern> patterns = new LinkedList<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (spots[x][y].isOccupied()) {
                    patterns.addAll(spots[x][y].createPatternList());
                }
            }
        }
        patterns.forEach(GridPattern::removeTileTags);  // IMPORTANT
        return patterns;
    }

    /**
     * Returns the spot of the first tile of round, the foundation tile.
     * @return the grid spot.
     */
    public GridSpot getFoundation() {
        return foundation;
    }

    /**
     * Getter for the grid height.
     * @return the height
     */
    public int getHeight() {
        return height;
    }

    /**
     * Method checks for patterns on a specific grid spot if it is occupied and additionally the direct neighbors.
     * neighboring tiles.
     * @param spot is the spot to be checked.
     * @return the list of the patterns.
     */
    public Collection<GridPattern> getLocalPatterns(GridSpot spot) {
        Collection<GridPattern> gridPatterns = new ArrayList<>();
        if (spot.isOccupied()) {
            gridPatterns.addAll(spot.createPatternList());
        }
        for (GridSpot neighbor : getNeighbors(spot, false, GridDirection.directNeighbors())) {
            gridPatterns.addAll(neighbor.createPatternList());
        }
        gridPatterns.forEach(GridPattern::removeTileTags); // VERY IMPORTANT!
        return gridPatterns; // get patterns.
    }

    /**
     * Method checks for potentially modified patterns on the grid.
     * @param spot is the spot of the last placed tile.
     * @return the list of the modified patterns.
     */
    public Collection<GridPattern> getModifiedPatterns(GridSpot spot) {
        checkParameters(spot);
        if (spot.isFree()) {
            throw new IllegalArgumentException("Can't check for patterns on an free grid space");
        }
        Collection<GridPattern> modifiedPatterns = spot.createPatternList();
        modifiedPatterns.forEach(GridPattern::removeTileTags); // VERY IMPORTANT!
        return modifiedPatterns; // get patterns.
    }

    /**
     * Returns the neighbor of a specific {@link GridSpot} in a specific direction or null of there is none.
     * @param spot is the {@link GridSpot} from which the neighbor is requested.
     * @param direction is the {@link GridDirection} where the neighbor is.
     * @return the neighboring {@link GridSpot} or null if there is no tile placed.
     */
    public GridSpot getNeighbor(GridSpot spot, GridDirection direction) {
        List<GridSpot> neighbors = getNeighbors(spot, false, direction);
        if (neighbors.isEmpty()) {
            return null; // return null if tile not placed or not on grid.
        }
        return neighbors.get(0);
    }

    /**
     * Returns a list of neighbors of a specific {@link GridSpot} in specific directions.
     * @param spot is the specific {@link GridSpot}.
     * @param allowEmptySpots determines whether empty spots are included or not.
     * @param directions determines the directions where we check for neighbors. If no directions are given, the default
     * {@link GridDirection#neighbors()} is used.
     * @return the list of any neighboring {@link GridSpot}.
     */
    public List<GridSpot> getNeighbors(GridSpot spot, boolean allowEmptySpots, List<GridDirection> directions) {
        checkParameters(spot);
        ArrayList<GridSpot> neighbors = new ArrayList<>();
        for (GridDirection direction : directions) {
            int newX = direction.getX() + spot.getX();
            int newY = direction.getY() + spot.getY();
            if (isOnGrid(newX, newY) && (allowEmptySpots || spots[newX][newY].isOccupied())) {
                neighbors.add(spots[newX][newY]); // return calculated neighbor if valid:
            }
        }
        return neighbors;
    }

    /**
     * Returns a collection all possible and legal moves.
     * @param tile is the tile that is placed during the move.
     * @param player is the player that conducts the move.
     * @param settings are the game settings.
     * @return the collection of all moves.
     */
    public Collection<? extends AbstractCarcassonneMove> getPossibleMoves(Tile tile, Player player, GameSettings settings) {
        checkParameters(tile);
        List<ZeroSumMove> possibleMoves = new ArrayList<>();
        for (TileRotation rotation : tile.getPossibleRotations()) {
            tile.rotateTo(rotation);
            for (int x = 0; x < width; x++) { // TODO (HIGH) [PERFORMANCE] maybe we should track free and occupied spots?
                for (int y = 0; y < height; y++) {
                    possibleMoves.addAll(movesForGridSpot(player, spots[x][y], tile, settings));
                }
            }
        }
        Collections.sort(possibleMoves);
        Collections.reverse(possibleMoves);
        return possibleMoves;
    }

    /**
     * Safe getter for tiles.
     * @param x is the x coordinate
     * @param y is the y coordinate
     * @return the spot
     * @throws IllegalArgumentException if the requested tile is out of grid.
     */
    public GridSpot getSpot(int x, int y) {
        checkParameters(x, y);
        return spots[x][y];
    }

    /**
     * Getter for the grid width.
     * @return the width
     */
    public int getWidth() {
        return width;
    }

    /**
     * Checks whether a spot on the grid would close free spots off in a direction if a tile would be placed there.
     * @param spot is the spot.
     * @param direction is the direction.
     * @return true if it does.
     */
    public boolean isClosingFreeSpotsOff(GridSpot spot, GridDirection direction) {
        boolean[][] visitedPositions = new boolean[width][height];
        visitedPositions[spot.getX()][spot.getY()] = true; // mark starting point as visited
        return !findBoundary(spot, direction, visitedPositions); // start recursion
    }

    /**
     * @return true if this grid allows enclosing free spot with tiles, leading to the free spots forming enclaves.
     */
    public boolean isAllowingEnclaves() {
        return allowEnclaves;
    }

    /**
     * Checks whether the grid is full.
     * @return true if full.
     */
    public boolean isFull() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (spots[x][y].isFree()) {
                    return false; // grid is not full if one position is free
                }
            }
        }
        return true;
    }

    /**
     * Tries to place a tile on a spot on the grid.
     * @param x is the x coordinate
     * @param y is the y coordinate
     * @param tile is the tile to place
     * @return true if it was successful, false if spot is occupied.
     */
    public boolean place(int x, int y, Tile tile) {
        checkParameters(x, y);
        checkParameters(tile);
        return spots[x][y].place(tile, allowEnclaves);
    }

    private void checkParameters(GridSpot spot) {
        if (spot == null) {
            throw new IllegalArgumentException("Spot can't be null!");
        }
        if (!spots[spot.getX()][spot.getY()].equals(spot)) {
            throw new IllegalArgumentException("Spot is not on the grid!");
        }
    }

    /**
     * Error checker method for other methods in this class. It just checks whether specific coordinates are on the grid and
     * throws an error if not.
     * @param x is the x coordinate
     * @param y is the y coordinate
     */
    private void checkParameters(int x, int y) {
        if (!isOnGrid(x, y)) {
            throw new IllegalArgumentException("tile coordinates are out of grid: x=" + x + " & y=" + y);
        }
    }

    /**
     * Error checker method for other methods in this class. It just checks whether specific tile is not null.
     * @param tile the tile to check
     */
    private void checkParameters(Tile tile) {
        if (tile == null) {
            throw new IllegalArgumentException("Tile can't be null.");
        }
        if (tile.getType() == TileType.Null) {
            throw new IllegalArgumentException("Tile from type TileType.Null can't be placed.");
        }
    }

    // method tries to find a path of free grid spaces to the grid border.
    private boolean findBoundary(GridSpot spot, GridDirection direction, boolean[][] visitedPositions) {
        int newX = direction.getX() + spot.getX(); // get coordinates
        int newY = direction.getY() + spot.getY(); // of free space
        if (!isOnGrid(newX, newY)) { // if not on grid
            return true; // found boundary
        }
        if (spots[newX][newY].isFree() && !visitedPositions[newX][newY]) { // if not visited
            visitedPositions[newX][newY] = true; // mark as visited
            for (GridDirection newDirection : GridDirection.directNeighbors()) { // recursion
                if (findBoundary(spots[newX][newY], newDirection, visitedPositions)) {
                    return true; // found boundary
                }
            }
        }
        return false; // has not found boundary
    }

    private List<GridSpot> getNeighbors(GridSpot spot, boolean allowEmptySpots, GridDirection direction) {
        return getNeighbors(spot, allowEmptySpots, List.of(direction));
    }

    /**
     * Checks whether specific coordinates are on the grid.
     * @param x is the x coordinate
     * @param y is the y coordinate
     * @return true if it is on the grid.
     */
    private boolean isOnGrid(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    private List<ZeroSumMove> movesForGridSpot(Player player, GridSpot spot, Tile originalTile, GameSettings settings) {
        List<ZeroSumMove> possibleMoves = new ArrayList<>();
        if (spot.isPlaceable(originalTile, allowEnclaves)) {
            TemporaryTile tile = new TemporaryTile(originalTile, originalTile.getRotation());
            spot.place(tile, allowEnclaves);
            possibleMoves.add(new ZeroSumMove(tile, player, settings));
            if (player.hasFreeMeeples()) {
                for (GridDirection position : GridDirection.values()) {
                    if (tile.hasMeepleSpot(position) && settings.getMeepleRule(tile.getTerrain(position))
                            && tile.allowsPlacingMeeple(position, player, settings)) {
                        possibleMoves.add(new ZeroSumMove(tile, position, player, settings));
                    }
                }
            }
            spot.removeTile();
        }
        return possibleMoves;
    }

    /**
     * Places a specific tile in the middle of the grid.
     * @param tileType is the type of that specific tile.
     */
    private void placeFoundation(TileType tileType) {
        int centerX = (width - 1) / 2;
        int centerY = (height - 1) / 2;
        foundation = spots[centerX][centerY];
        foundation.forcePlacement(new Tile(tileType));
    }
}

import java.util.List;
import java.util.Locale;

import carcassonne.model.terrain.RotationDirection;

/**
 * Enumeration for grid directions and tile positions. It is used either to specify a direction on the grid from a
 * specific tile, or to specify a position on a tile.
 * @author Timur Saglam
 */
public enum GridDirection {
    NORTH,
    EAST,
    SOUTH,
    WEST,
    NORTH_EAST,
    SOUTH_EAST,
    SOUTH_WEST,
    NORTH_WEST,
    CENTER;

    /**
     * Returns the X coordinate of a <code>GridDirection</code>.
     * @return either -1, 0, or 1.
     */
    public int getX() {
        if (this == NORTH_EAST || this == EAST || this == SOUTH_EAST) {
            return 1;
        }
        if (this == NORTH_WEST || this == WEST || this == SOUTH_WEST) {
            return -1;
        }
        return 0;
    }

    /**
     * Returns the Y coordinate of a <code>GridDirection</code>.
     * @return either -1, 0, or 1.
     */
    public int getY() {
        if (this == SOUTH_WEST || this == SOUTH || this == SOUTH_EAST) {
            return 1;
        }
        if (this == NORTH_WEST || this == NORTH || this == NORTH_EAST) {
            return -1;
        }
        return 0;
    }

    /**
     * Checks whether the this grid direction is directly to the left of another grid direction.
     * @param other is the other grid direction.
     * @return true if it is.
     */
    public boolean isLeftOf(GridDirection other) {
        return nextDirectionTo(RotationDirection.RIGHT) == other;
    }

    /**
     * Checks whether the this grid direction is directly to the right of another grid direction.
     * @param other is the other grid direction.
     * @return true if it is.
     */
    public boolean isRightOf(GridDirection other) {
        return nextDirectionTo(RotationDirection.LEFT) == other;
    }

    /**
     * Checks whether the ordinal of a direction is smaller or equal than the ordinal of another direction.
     * @param other is the other direction.
     * @return true if smaller or equal.
     */
    public boolean isSmallerOrEquals(GridDirection other) { // TODO (MEDIUM) [STYLE] Ordinal magic should not be exposed too much
        return ordinal() <= other.ordinal();
    }

    /**
     * Gets the next direction on the specified side of the current direction.
     * @param side sets the side.
     * @return the next direction
     */
    public GridDirection nextDirectionTo(RotationDirection side) {
        if (this == CENTER) {
            return this;
        }
        GridDirection[] cycle = {NORTH, NORTH_EAST, EAST, SOUTH_EAST, SOUTH, SOUTH_WEST, WEST, NORTH_WEST};
        int position = -2; // error case, sum with parameter side is negative
        for (int i = 0; i < cycle.length; i++) {
            if (cycle[i] == this) { // find in cycle
                position = i; // save cycle position
            }
        }
        return cycle[(cycle.length + position + side.getValue()) % cycle.length];
    }

    /**
     * Calculates the opposite <code>GridDirection</code> for a specific <code>GridDirection</code>.
     * @return the opposite <code>GridDirection</code>.
     */
    public GridDirection opposite() {
        if (ordinal() <= 3) { // for NORTH, EAST, SOUTH and WEST:
            return values()[smallOpposite(ordinal())];
        }
        if (ordinal() <= 7) { // for NORTH_EAST, SOUTH_EAST, SOUTH_WEST and NORTH_WEST:
            return values()[bigOpposite(ordinal())];
        }
        return CENTER; // middle is the opposite of itself.
    }

    /**
     * Returns a lower case version of the grid direction with spaces instead of underscores.
     * @return the readable version.
     */
    public String toReadableString() {
        return toString().toLowerCase(Locale.UK).replace('_', ' ');
    }

    private int bigOpposite(int ordinal) {
        return 4 + smallOpposite(ordinal - 4);
    }

    private int smallOpposite(int ordinal) {
        return (ordinal + 2) % 4;
    }

    /**
     * Generates a list of the GridDirections for a direct neighbor on the grid.
     * @return a list of NORTH, EAST, SOUTH and WEST.
     */
    public static List<GridDirection> directNeighbors() {
        return List.of(NORTH, EAST, SOUTH, WEST);
    }

    /**
     * Generates a list of the GridDirections for a indirect neighbor on the grid.
     * @return a list of NORTH_EAST, SOUTH_EAST, SOUTH_WEST and NORTH_WEST.
     */
    public static List<GridDirection> indirectNeighbors() {
        return List.of(NORTH_EAST, SOUTH_EAST, SOUTH_WEST, NORTH_WEST);
    }

    /**
     * Generates a list of the GridDirections for a neighbor on the grid.
     * @return a list of all directions except CENTER.
     */
    public static List<GridDirection> neighbors() {
        return List.of(NORTH, EAST, SOUTH, WEST, NORTH_EAST, SOUTH_EAST, SOUTH_WEST, NORTH_WEST);
    }

    /**
     * Generates a list of the GridDirections for all positions on a tile.
     * @return a list of NORTH, EAST, SOUTH, WEST and CENTER.
     */
    public static List<GridDirection> tilePositions() {
        return List.of(NORTH, EAST, SOUTH, WEST, CENTER);
    }

    /**
     * Generates a list of the GridDirections by row.
     * @return a list of NORTH_WEST, NORTH, NORTH_EAST, WEST, CENTER, EAST, SOUTH_WEST, SOUTH, SOUTH_EAST in that order.
     */
    public static List<GridDirection> byRow() {
        return List.of(NORTH_WEST, NORTH, NORTH_EAST, WEST, CENTER, EAST, SOUTH_WEST, SOUTH, SOUTH_EAST);
    }

    /**
     * Generates a two dimensional list of the GridDirections for their orientation on a tile.
     * @return a 2D list of of NORTH_WEST, WEST, SOUTH_WEST, NORTH, CENTER, SOUTH, NORTH_EAST, EAST and SOUTH_EAST.
     */
    public static GridDirection[][] values2D() {
        return new GridDirection[][] {{NORTH_WEST, WEST, SOUTH_WEST}, {NORTH, CENTER, SOUTH}, {NORTH_EAST, EAST, SOUTH_EAST}};
    }
}


import static carcassonne.model.grid.GridDirection.CENTER;
import static carcassonne.model.grid.GridDirection.EAST;
import static carcassonne.model.grid.GridDirection.NORTH;
import static carcassonne.model.grid.GridDirection.NORTH_EAST;
import static carcassonne.model.grid.GridDirection.NORTH_WEST;
import static carcassonne.model.grid.GridDirection.SOUTH;
import static carcassonne.model.grid.GridDirection.SOUTH_EAST;
import static carcassonne.model.grid.GridDirection.SOUTH_WEST;
import static carcassonne.model.grid.GridDirection.WEST;

import java.awt.Point;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import carcassonne.model.grid.GridDirection;
import carcassonne.model.tile.TileType;

/**
 * Represents the terrain information of a single tile. It consists out of nine different terrain types, one for each
 * grid direction. Every other property, such as the meeple spots and connections between positions, is computed from
 * that information.
 * @author Timur Saglam
 */
public class TileTerrain {
    private Set<GridDirection> meepleSpots;
    private final Map<GridDirection, TerrainType> terrain;

    /**
     * Creates a terrain instance with nine terrain types.
     * @param type is the tile type of the terrain.
     */
    public TileTerrain(TileType type) {
        terrain = new HashMap<>();
        for (int i = 0; i < GridDirection.values().length; i++) {
            terrain.put(GridDirection.values()[i], type.getTerrain()[i]);
        }
        createMeepleSpots();
    }

    /**
     * return the terrain type on the tile in the specific direction.
     * @param direction is the specific direction.
     * @return the terrain type, or null if the direction is not mapped.
     */
    public TerrainType at(GridDirection direction) {
        if (terrain.containsKey(direction)) {
            return terrain.get(direction);
        }
        throw new IllegalArgumentException("TileTerrain not defined at " + direction);
    }

    /**
     * Returns a set of grid directions, where meeples can be placed on this terrain.
     * @return the list of meeple spots.
     */
    public Set<GridDirection> getMeepleSpots() {
        return meepleSpots;
    }

    /**
     * Checks whether two parts of a tile are connected through same terrain.
     * @param from is the part to check from.
     * @param towards is the terrain to check to.
     * @return true if connected, false if not.
     */
    public final boolean isConnected(GridDirection from, GridDirection towards) {
        if (isDirectConnected(from, towards) || from != CENTER && towards != CENTER && isIndirectConnected(from, towards)) {
            return true; // is not from or to middle but indirectly connected (counter)clockwise
        }
        if (terrain.get(from) == TerrainType.FIELDS && terrain.get(towards) == TerrainType.FIELDS) {
            return isImplicitlyConnected(from, towards); // is connected through implicit terrain information
        }
        return false;
    }

    /**
     * Turns a tile 90 degree to the left.
     */
    public void rotateLeft() {
        rotate(List.of(NORTH, WEST, SOUTH, EAST));
        rotate(List.of(NORTH_EAST, NORTH_WEST, SOUTH_WEST, SOUTH_EAST));
    }

    /**
     * Turns a tile 90 degree to the right.
     */
    public void rotateRight() {
        rotate(GridDirection.directNeighbors());
        rotate(GridDirection.indirectNeighbors());
    }

    /**
     * Creates the list of positions on the tile where a meeple can be placed.
     */
    private void createMeepleSpots() {
        meepleSpots = new HashSet<>();
        for (GridDirection position : GridDirection.values()) { // for every spot
            if (terrain.get(position) != TerrainType.OTHER) { // if not checked
                createMeepleSpot(position);
            }
        }
        removeRedundantSpots(GridDirection.directNeighbors(), false); // merge to top, right, bottom, and left
        removeRedundantSpots(GridDirection.indirectNeighbors(), true); // merge to the corners and add already removed anchors
        removeRedundantSpots(GridDirection.directNeighbors(), true); // merge one more time
    }

    /**
     * Creates a single meeple spot.
     */
    private void createMeepleSpot(GridDirection position) {
        List<GridDirection> connectedPositions = Stream.of(GridDirection.values()).filter(it -> isConnected(position, it)).toList();
        Point sum = new Point();
        for (GridDirection connectedPosition : connectedPositions) {
            sum.x += connectedPosition.getX(); // sum up coordinate weights to calculate the center
            sum.y += connectedPosition.getY();
        }
        GridDirection center = GridDirection.values2D()[(int) Math.round(sum.x / 3.0) + 1][(int) Math.round(sum.y / 3.0) + 1];
        if (isConnected(center, position)) {
            meepleSpots.add(center); // add the geometrical pattern center
        } else {
            meepleSpots.add(position); // just add the original position
        }
    }

    /**
     * Checks if the directions are directly connected through the middle
     */
    private boolean isDirectConnected(GridDirection from, GridDirection towards) {
        TerrainType middle = terrain.get(CENTER);
        return terrain.get(from) == middle && terrain.get(towards) == middle;
    }

    /**
     * Checks if the directions are connected clockwise or counter-clockwise around the tile.
     */
    private boolean isIndirectConnected(GridDirection from, GridDirection towards) {
        boolean connected = false;
        for (RotationDirection side : RotationDirection.values()) { // for left and right
            connected |= isIndirectConnected(from, towards, side);
        }
        return connected;
    }

    /**
     * Checks for indirect connection through the specified side from a specific start to a specific destination.
     */
    private boolean isIndirectConnected(GridDirection from, GridDirection towards, RotationDirection side) {
        GridDirection current = from;
        GridDirection next;
        while (current != towards) { // while not at destination:
            next = current.nextDirectionTo(side); // get the next direction
            if (terrain.get(current) != terrain.get(next)) {
                return false; // check if still connected
            }
            current = next; // set new current
        }
        return true; // found connection from start to destination.
    }

    /**
     * Checks for implicit connection, which means connection that is only implicitly represented through the terrain, e.g.
     * because a road does not end in a castle because it passes through the tile.
     */
    private boolean isImplicitlyConnected(GridDirection from, GridDirection towards) {
        boolean connected = false;
        for (GridDirection direction : Arrays.asList(from, towards)) { // for both directions
            GridDirection other = from == direction ? towards : from;
            for (GridDirection corner : GridDirection.indirectNeighbors()) { // for every connected corner:
                if (isDirectConnected(direction, corner) || isIndirectConnected(direction, corner)) { // if connected to corner
                    for (RotationDirection side : RotationDirection.values()) { // to the left and right
                        connected |= isImplicitlyConnected(corner, other, side); // check corner to corner connection
                    }
                }
            }
        }
        return connected;
    }

    /**
     * Checks for implicit connection in a specific direction.
     */
    private boolean isImplicitlyConnected(GridDirection from, GridDirection towards, RotationDirection side) {
        if (from == towards) {
            return true; // is connected
        }
        GridDirection inbetween = from.nextDirectionTo(side); // between this and next corner
        GridDirection nextCorner = inbetween.nextDirectionTo(side); // next corner
        if (hasNoCastleEntry(inbetween)) {
            return isImplicitlyConnected(nextCorner, towards, side);
        }
        return false;
    }

    /**
     * removes redundant meeple spots and optionally adds anchor spots.
     */
    private void removeRedundantSpots(List<GridDirection> anchorDirections, boolean addAnchor) {
        List<GridDirection> removalList = new LinkedList<>();
        for (GridDirection anchor : anchorDirections) {
            GridDirection left = anchor.nextDirectionTo(RotationDirection.LEFT);
            GridDirection right = anchor.nextDirectionTo(RotationDirection.RIGHT);
            if (terrain.get(anchor) == terrain.get(left) && terrain.get(anchor) == terrain.get(right) && meepleSpots.contains(left)
                    && meepleSpots.contains(right)) {
                removalList.add(left);
                removalList.add(right);
                if (addAnchor && !isConnected(anchor, CENTER)) {
                    meepleSpots.add(anchor);
                }
            }
        }
        meepleSpots.removeAll(removalList);
    }

    /**
     * Checks whether this tile terrain has a street passing through the center of the tile. This means the middle is of
     * terrain street and is connected to at least two other sides.
     */
    private boolean hasPassingStreet() {
        return terrain.get(CENTER) == TerrainType.ROAD
                && GridDirection.tilePositions().stream().filter(it -> isDirectConnected(CENTER, it)).count() > 2;
    }

    /**
     * Checks whether the tile terrain has a castle entry towards a specified castle position. This means no street ending
     * towards it.
     */
    private boolean hasNoCastleEntry(GridDirection castlePosition) {
        return terrain.get(castlePosition) == TerrainType.CASTLE && (terrain.get(CENTER) == TerrainType.OTHER || hasPassingStreet());
    }

    /**
     * Rotates the terrain at the specified directions clockwise.
     * @param directions are the specified directions.
     */
    private void rotate(List<GridDirection> directions) {
        TerrainType temporary = terrain.get(directions.get(directions.size() - 1)); // get last one
        for (GridDirection direction : directions) { // rotate terrain through temporary:
            temporary = terrain.put(direction, temporary);
        }
        createMeepleSpots();
    }
}


import java.util.List;

/**
 * Enumeration for the terrain type. Is used to specify the terrain of a tile on its different positions.
 * @author Timur Saglam
 */
public enum TerrainType {
    CASTLE,
    ROAD,
    MONASTERY,
    FIELDS,
    OTHER;

    /**
     * Generates a list of the basic terrain types, which is every terrain except {@link OTHER}.
     * @return a list of CASTLE, ROAD, MONASTERY, FIELDS.
     */
    public static List<TerrainType> basicTerrain() {
        return List.of(CASTLE, ROAD, MONASTERY, FIELDS);
    }

    public String toReadableString() {
        return toString().charAt(0) + toString().substring(1).toLowerCase();
    }
}

/**
 * Rotation direction enumeration. Left for anti-clockwise and right for clockwise.
 * @author Timur Saglam
 */
public enum RotationDirection {
    LEFT(-1),
    RIGHT(1);

    int value;

    RotationDirection(int value) {
        this.value = value;
    }

    /**
     * Returns the numeric value of the rotation direction.
     * @return -1 or 1.
     */
    public int getValue() {
        return value;
    }
}


import java.awt.Color;
import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import carcassonne.model.Player;
import carcassonne.model.terrain.TerrainType;
import carcassonne.model.tile.TileDistribution;
import carcassonne.util.MinkowskiDistance;
import carcassonne.view.NotifiableView;
import carcassonne.view.PaintShop;

/**
 * Class for the management of the Carcassonne game settings.
 * @author Timur Saglam
 */
public class GameSettings {
    // THRESHOLD CONSTANTS:
    public static final int MAXIMAL_PLAYERS = 5;
    public static final int MAXIMAL_TILES_ON_HAND = 5;
    public static final int TILE_RESOLUTION = 500;
    public static final int HIGH_DPI_FACTOR = 2; // maximum supported DPI factor.
    public static final int MAXIMAL_MEEPLES = 7;

    // STRING CONSTANTS
    public static final String TILE_FILE_TYPE = ".png";
    public static final String TILE_FOLDER_PATH = "tiles/";
    private static final String EMPTY = "";
    private static final String MEEPLE_PATH = "meeple/meeple_";
    private static final String PNG = ".png";
    private static final String TEMPLATE = "_template";
    private static final String[] DEFAULT_NAMES = {"You", "Alice", "Bob", "Carol", "Dan"};

    // COLOR CONSTANTS:
    public static final Color UI_COLOR = new Color(190, 190, 190);
    private static final PlayerColor[] DEFAULT_COLORS = {new PlayerColor(30, 26, 197), new PlayerColor(151, 4, 12), new PlayerColor(14, 119, 25),
            new PlayerColor(216, 124, 0), new PlayerColor(96, 0, 147)};

    // COSMETIC:
    private final List<PlayerColor> colors;
    private final List<String> names;

    // GAME SETUP:
    private int gridWidth;
    private int gridHeight;
    private int numberOfPlayers;
    private int stackSizeMultiplier;
    private MinkowskiDistance distanceMeasure;
    private final TileDistribution tileDistribution;
    private final List<Boolean> playerTypes;

    // GAME RULES:
    private boolean allowFortifying;
    private boolean allowEnclaves;
    private int tilesPerPlayer;
    private final Map<TerrainType, Boolean> meepleRules;
    private boolean splitPatternScore;

    // OTHER/INTERNAL
    private boolean gridSizeChanged;
    private final List<NotifiableView> changeListeners;

    /**
     * Creates a settings instance. Instances hold different setting values when one is changed.
     */
    public GameSettings() {
        colors = new ArrayList<>(Arrays.asList(DEFAULT_COLORS));
        names = new ArrayList<>(Arrays.asList(DEFAULT_NAMES));
        playerTypes = new ArrayList<>(Arrays.asList(false, true, true, true, true));
        meepleRules = new HashMap<>();
        TerrainType.basicTerrain().forEach(it -> meepleRules.put(it, true));
        tileDistribution = new TileDistribution();
        setDistanceMeasure(MinkowskiDistance.ROUNDED_SQUARE);
        numberOfPlayers = 2;
        tilesPerPlayer = 1;
        stackSizeMultiplier = 1;
        gridWidth = 29;
        gridHeight = 19;
        allowEnclaves = true;
        changeListeners = new ArrayList<>();
    }

    /**
     * Returns the distance measure used for AI players.
     * @return the specific Minkowski distance.
     */
    public MinkowskiDistance getDistanceMeasure() {
        return distanceMeasure;
    }

    /**
     * Getter for the height of the grid.
     * @return the gridHeight the grid height in tiles.
     */
    public int getGridHeight() {
        return gridHeight;
    }

    /**
     * Getter for the width of the grid.
     * @return the gridWidth the grid width in tiles.
     */
    public int getGridWidth() {
        return gridWidth;
    }

    /**
     * Returns for a specific meeple type if meeple placement is enabled or disabled.
     * @param type it the specific meeple type to query.
     * @return true if placement is enabled.
     */
    public boolean getMeepleRule(TerrainType type) {
        return meepleRules.getOrDefault(type, false);
    }

    /**
     * Returns how many player are playing in the next round.
     * @return the amount of players.
     */
    public int getNumberOfPlayers() {
        return numberOfPlayers;
    }

    /**
     * Returns the {@link PlayerColor} of a specific {@link Player}.
     * @param playerNumber is the number of the {@link Player}.
     * @return the {@link PlayerColor}.
     */
    public PlayerColor getPlayerColor(int playerNumber) {
        return colors.get(playerNumber);
    }

    /**
     * Returns the name of a specific {@link Player}.
     * @param playerNumber is the number of the {@link Player}.
     * @return the name.
     */
    public String getPlayerName(int playerNumber) {
        return names.get(playerNumber);
    }

    /**
     * Returns the value for the split points option.
     * @return true if points of a pattern should be split instead of every player getting the score.
     */
    public boolean getSplitPatternScore() {
        return splitPatternScore;
    }

    /**
     * Returns the multiplier for the tile amounts in a tile stack. When a tile amount is 2 and the stack multiplier is 2
     * the tile stack contains for tiles of this type.
     * @return the stack size multiplier.
     */
    public int getStackSizeMultiplier() {
        return stackSizeMultiplier;
    }

    /**
     * Getter for the current tile distribution.
     * @return the tile distribution.
     */
    public TileDistribution getTileDistribution() {
        return tileDistribution;
    }

    /**
     * Specifies the tiles that each player can hold on his hand.
     * @return the tiles per player.
     */
    public int getTilesPerPlayer() {
        return tilesPerPlayer;
    }

    /**
     * @return determines whether it is legal to enclose free spot with tiles, leading to the free spots forming enclaves.
     */
    public boolean isAllowingEnclaves() {
        return allowEnclaves;
    }

    /**
     * Determines if players are allowed to directly place meeples on patterns they already own.
     * @return true if it is allowed.
     */
    public boolean isAllowingFortifying() {
        return allowFortifying;
    }

    /**
     * Gives information whether the user or the game changed the grid size settings.
     * @return the true if the size was changed.
     */
    public boolean isGridSizeChanged() {
        return gridSizeChanged;
    }

    /**
     * Checks if a player with a certain number is set to be computer-controlled.
     * @param playerNumber is the number of the player.
     * @return true if he is computer-controlled.
     */
    public boolean isPlayerComputerControlled(int playerNumber) {
        return playerTypes.get(playerNumber);
    }

    /**
     * Registers a view element that wants to listen to changes.
     * @param notifiable is the view element.
     */
    public void registerNotifiable(NotifiableView notifiable) {
        changeListeners.add(notifiable);
    }

    /**
     * Determines whether it is legal to enclose free spot with tiles, leading to the free spots forming enclaves.
     * @param allowEnclaves set to true if enclaves are allowed.
     */
    public void setAllowEnclaves(boolean allowEnclaves) {
        this.allowEnclaves = allowEnclaves;
    }

    /**
     * Changes whether players are allowed to directly place meeples on patterns they already own.
     * @param allowFortifying forbids or allows fortifying.
     */
    public void setAllowFortifying(boolean allowFortifying) {
        this.allowFortifying = allowFortifying;
    }

    /**
     * Sets the distance measure used for AI players.
     * @param distanceMeasure is the specific Minkowski distance to set.
     */
    public void setDistanceMeasure(MinkowskiDistance distanceMeasure) {
        this.distanceMeasure = distanceMeasure;
    }

    /**
     * Setter for the height of grid.
     * @param gridHeight the grid height in tiles.
     */
    public void setGridHeight(int gridHeight) {
        this.gridHeight = gridHeight;
        gridSizeChanged = true;
    }

    /**
     * Sets the indicator of grid size change.
     * @param gridSizeChanged the value to set the indicator to.
     */
    public void setGridSizeChanged(boolean gridSizeChanged) {
        this.gridSizeChanged = gridSizeChanged;
    }

    /**
     * Setter for the width of grid.
     * @param gridWidth the grid width in tiles.
     */
    public void setGridWidth(int gridWidth) {
        this.gridWidth = gridWidth;
        gridSizeChanged = true;
    }

    /**
     * Specifies how many player are playing in the next round.
     * @param numberOfPlayers is the amount of players.
     */
    public void setNumberOfPlayers(int numberOfPlayers) {
        this.numberOfPlayers = numberOfPlayers;
    }

    /**
     * Changes the {@link PlayerColor} of a specific {@link Player}.
     * @param color is the new base {@link Color}.
     * @param playerNumber is the number of the {@link Player}.
     */
    public void setPlayerColor(Color color, int playerNumber) {
        colors.set(playerNumber, new PlayerColor(color));
        notifyListeners();
    }

    /**
     * Sets a player with a certain number to be computer-controlled or not.
     * @param computerControlled determines whether he is computer-controlled.
     * @param playerNumber is the number of the player.F
     */
    public void setPlayerComputerControlled(boolean computerControlled, int playerNumber) {
        playerTypes.set(playerNumber, computerControlled);
    }

    /**
     * Changes the name of a specific {@link Player}.
     * @param name is the new name.
     * @param playerNumber is the number of the {@link Player}.
     */
    public void setPlayerName(String name, int playerNumber) {
        names.set(playerNumber, name);
        notifyListeners();
    }

    /**
     * Sets the value for the split points option.
     * @param splitPatternScore determines if points of a pattern should be split instead of every player getting the score.
     */
    public void setSplitPatternScore(boolean splitPatternScore) {
        this.splitPatternScore = splitPatternScore;
    }

    /**
     * Sets the multiplier for the tile amounts in a tile stack. When a tile amount is 2 and the stack multiplier is 2 the
     * tile stack contains for tiles of this type.
     * @param stackSizeMultiplier is the new stack size multiplier.
     */
    public void setStackSizeMultiplier(int stackSizeMultiplier) {
        this.stackSizeMultiplier = stackSizeMultiplier;
    }

    /**
     * Changes how many tiles each player can hold on his hand.
     * @param tilesPerPlayer is the new amount of tiles per player.
     */
    public void setTilesPerPlayer(int tilesPerPlayer) {
        this.tilesPerPlayer = tilesPerPlayer;
    }

    /**
     * Toggles whether for a specific meeple type if meeple placement is enabled or disabled.
     * @param type it the specific meeple type.
     */
    public void toggleMeepleRule(TerrainType type) {
        meepleRules.computeIfPresent(type, (key, enabled) -> !enabled);
    }

    private void notifyListeners() {
        PaintShop.clearCachedImages();
        for (NotifiableView notifiable : changeListeners) {
            EventQueue.invokeLater(notifiable::notifyChange);
        }
    }

    /**
     * Builds the path to the image of a specific meeple type.
     * @param type is the type of terrain the meeple occupies.
     * @param isTemplate specifies whether the template image should be loaded.
     * @return the path as a String.
     */
    public static String getMeeplePath(TerrainType type, boolean isTemplate) { // TODO (MEDIUM) [UTILS] move to image loading utility class?
        return MEEPLE_PATH + type.toString().toLowerCase(Locale.UK) + (isTemplate ? TEMPLATE : EMPTY) + PNG;
    }
}

import java.awt.Color;

/**
 * Supplies the color scheme for a specific color.
 * @author Timur Saglam
 */
public class PlayerColor extends Color {
    private static final long serialVersionUID = 7146171711123557361L;
    private static final int HUE = 0;
    private static final int SATURATION = 1;
    private static final int BRIGHTNESS = 2;
    private static final float BRIGHTEN_FACTOR = 0.75f;
    private static final double DESATURATION_FACTOR = 0.6;

    /**
     * Creates a new player color with RGB values between 0 and 255.
     * @see Color#Color(int, int, int)
     */
    public PlayerColor(int red, int green, int blue) {
        super(red, green, blue);
    }

    /**
     * Creates a new player color based on a existing {@link Color}.
     * @param color is the existing {@link Color}.
     */
    public PlayerColor(Color color) {
        this(color.getRed(), color.getGreen(), color.getBlue());
    }

    /**
     * Returns the lighter and and desaturated version of the player color.
     * @return the light {@link Color}.
     */
    public Color lightColor() {
        float[] hsb = Color.RGBtoHSB(getRed(), getGreen(), getBlue(), null);
        hsb[SATURATION] *= DESATURATION_FACTOR; // reduce saturation
        hsb[BRIGHTNESS] = 1 - (1 - hsb[BRIGHTNESS]) * BRIGHTEN_FACTOR; // increase brightness
        return new Color(Color.HSBtoRGB(hsb[HUE], hsb[SATURATION], hsb[BRIGHTNESS])); // convert to RGB color
    }

    /**
     * Returns the no-alpha version of the player color.
     * @return the {@link Color} without transparency.
     */
    public Color textColor() {
        return new Color(getRGB(), false); // remove transparency
    }
}


import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

import carcassonne.model.Player;
import carcassonne.model.terrain.TerrainType;
import carcassonne.model.tile.Tile;
import carcassonne.settings.GameSettings;
import carcassonne.util.ConcurrentTileImageScaler;
import carcassonne.util.FastImageScaler;
import carcassonne.util.ImageLoadingUtil;

/**
 * This is the Carcassonne paint shop! It paints meeple images and tile highlights! It is implemented as a utility class
 * with static methods to increase performance through avoiding loading images more often that needed.
 * @author Timur Saglam
 */
public final class PaintShop {
    private static final int HIGH_DPI_FACTOR = 2;
    private static final BufferedImage emblemImage = ImageLoadingUtil.EMBLEM.createBufferedImage();
    private static final BufferedImage highlightBaseImage = ImageLoadingUtil.NULL_TILE.createBufferedImage();
    private static final BufferedImage highlightImage = ImageLoadingUtil.HIGHLIGHT.createBufferedImage();
    private static final Map<String, ImageIcon> chachedMeepleImages = new HashMap<>();
    private static final Map<TerrainType, BufferedImage> templateMap = buildImageMap(true);
    private static final Map<TerrainType, BufferedImage> imageMap = buildImageMap(false);
    private static final String KEY_SEPARATOR = "|";
    private static final int MAXIMAL_ALPHA = 255;

    private PaintShop() {
        // private constructor ensures non-instantiability!
    }

    /**
     * Adds the emblem image to the top right of any tile image.
     * @param originalTile is the original tile image without the emblem.
     * @return a copy of the image with an emblem.
     */
    public static Image addEmblem(BufferedImage originalTile) {
        BufferedImage copy = deepCopy(originalTile);
        for (int x = 0; x < emblemImage.getWidth(); x++) {
            for (int y = 0; y < emblemImage.getHeight(); y++) {
                Color emblemPixel = new Color(emblemImage.getRGB(x, y), true);
                Color imagePixel = new Color(copy.getRGB(x, y), true);
                Color blendedColor = blend(imagePixel, emblemPixel, false);
                copy.setRGB(x, y, blendedColor.getRGB());
            }
        }
        return copy;
    }

    /**
     * Clears the meeple image cache. Should be cleared when player colors change.
     */
    public static void clearCachedImages() {
        chachedMeepleImages.clear();
    }

    /**
     * Returns a custom colored highlight image.
     * @param player determines the color of the highlight.
     * @param size is the edge length in pixels of the image.
     * @return the highlighted tile.
     */
    public static ImageIcon getColoredHighlight(Player player, int size, boolean fastScaling) {
        BufferedImage coloredImage = colorMaskBased(highlightBaseImage, highlightImage, player.getColor());
        Image smallImage = scaleDown(coloredImage, size, fastScaling, coloredImage.getTransparency());
        int largeSize = Math.min(size * HIGH_DPI_FACTOR, GameSettings.TILE_RESOLUTION);
        Image largeImage = scaleDown(coloredImage, largeSize, fastScaling, coloredImage.getTransparency());
        return new ImageIcon(new BaseMultiResolutionImage(smallImage, largeImage));
    }

    /**
     * Returns a colored tile image icon.
     * @param tile is the tile to be colored.
     * @param player is the player whose color is used.
     * @param size is the desired tile size.
     * @param fastScaling determines the rendering quality.
     * @return the colored tile image wrapped in a image icon.
     */
    public static ImageIcon getColoredTile(Tile tile, Player player, int size, boolean fastScaling) {
        Image baseImage = ConcurrentTileImageScaler.getScaledImage(tile, GameSettings.TILE_RESOLUTION, fastScaling);
        BufferedImage bufferedBaseImage = bufferedImageOf(baseImage);
        BufferedImage coloredImage = colorMaskBased(bufferedBaseImage, highlightImage, player.getColor());
        Image small = scaleDown(coloredImage, size, fastScaling, coloredImage.getTransparency());
        int largeSize = Math.min(size * HIGH_DPI_FACTOR, GameSettings.TILE_RESOLUTION);
        Image large = scaleDown(coloredImage, largeSize, fastScaling, coloredImage.getTransparency());
        return new ImageIcon(new BaseMultiResolutionImage(small, large));
    }

    /**
     * Returns a custom colored meeple.
     * @param meepleType is the type of the meeple.
     * @param color is the custom color.
     * @param size is the edge length in pixels of the image.
     * @return the colored meeple.
     */
    public static ImageIcon getColoredMeeple(TerrainType meepleType, Color color, int size) {
        String key = createKey(color, meepleType, size);
        if (chachedMeepleImages.containsKey(key)) {
            return chachedMeepleImages.get(key);
        }
        Image paintedMeeple = paintMeeple(meepleType, color.getRGB(), size * HIGH_DPI_FACTOR);
        ImageIcon icon = new ImageIcon(ImageLoadingUtil.createHighDpiImage(paintedMeeple));
        chachedMeepleImages.put(key, icon);
        return icon;
    }

    /**
     * Returns a custom colored meeple.
     * @param meepleType is the type of the meeple.
     * @param player is the {@link Player} whose color is used.
     * @param size is the edge length in pixels of the image.
     * @return the colored meeple.
     */
    public static ImageIcon getColoredMeeple(TerrainType meepleType, Player player, int size) {
        return getColoredMeeple(meepleType, player.getColor(), size);
    }

    /**
     * Creates a non-colored meeple image icon.
     * @param meepleType is the type of the meeple.
     * @param size is the edge length in pixels of the image.
     * @return non-colored meeple image.
     */
    public static ImageIcon getPreviewMeeple(TerrainType meepleType, int size) {
        String key = createKey(meepleType, size);
        if (chachedMeepleImages.containsKey(key)) {
            return chachedMeepleImages.get(key);
        }
        Image preview = imageMap.get(meepleType).getScaledInstance(size * HIGH_DPI_FACTOR, size * HIGH_DPI_FACTOR, Image.SCALE_SMOOTH);
        ImageIcon icon = new ImageIcon(ImageLoadingUtil.createHighDpiImage(preview));
        chachedMeepleImages.put(key, icon);
        return icon;
    }

    /**
     * Blends to colors correctly based on alpha composition. Either blends both colors or applies the second on the first
     * one.
     * @param first is the first color to be applied.
     * @param second is the second color to be applied.
     * @param blendEqually applies the second on the first one of true, blends on alpha values if false.
     * @return the blended color.
     */
    private static Color blend(Color first, Color second, boolean blendEqually) {
        double totalAlpha = blendEqually ? first.getAlpha() + second.getAlpha() : MAXIMAL_ALPHA;
        double firstWeight = blendEqually ? first.getAlpha() : MAXIMAL_ALPHA - second.getAlpha();
        firstWeight /= totalAlpha;
        double secondWeight = second.getAlpha() / totalAlpha;
        double red = firstWeight * first.getRed() + secondWeight * second.getRed();
        double green = firstWeight * first.getGreen() + secondWeight * second.getGreen();
        double blue = firstWeight * first.getBlue() + secondWeight * second.getBlue();
        int alpha = Math.max(first.getAlpha(), second.getAlpha());
        return new Color((int) red, (int) green, (int) blue, alpha);
    }

    /**
     * Converts a given Image into a BufferedImage. This can be costly and should only be done when really required.
     * @param image is the {@link Image} to be converted.
     * @return a {@link BufferedImage}, either the casted original image or a copy.
     */
    private static BufferedImage bufferedImageOf(Image image) {
        if (image instanceof BufferedImage) {
            return (BufferedImage) image;
        }
        BufferedImage bufferedImage = ImageLoadingUtil
                .makeCompatible(new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB));
        Graphics2D graphics = bufferedImage.createGraphics();
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();
        return bufferedImage;
    }

    // prepares the base images and templates
    private static Map<TerrainType, BufferedImage> buildImageMap(boolean isTemplate) {
        Map<TerrainType, BufferedImage> map = new HashMap<>();
        for (TerrainType terrainType : TerrainType.values()) {
            BufferedImage meepleImage = ImageLoadingUtil.createBufferedImage(GameSettings.getMeeplePath(terrainType, isTemplate));
            map.put(terrainType, meepleImage);
        }
        return map;
    }

    private static BufferedImage colorMaskBased(BufferedImage imageToColor, BufferedImage maskImage, Color targetColor) {
        BufferedImage image = deepCopy(imageToColor);
        for (int x = 0; x < maskImage.getWidth(); x++) {
            for (int y = 0; y < maskImage.getHeight(); y++) {
                Color maskPixel = new Color(maskImage.getRGB(x, y), true);
                Color targetPixel = new Color(targetColor.getRed(), targetColor.getGreen(), targetColor.getBlue(), maskPixel.getAlpha());
                Color imagePixel = new Color(image.getRGB(x, y), true);
                Color blendedColor = blend(imagePixel, targetPixel, true);
                image.setRGB(x, y, blendedColor.getRGB());
            }
        }
        return image;
    }

    private static String createKey(Color color, TerrainType meepleType, int size) {
        return createKey(meepleType, size) + color.getRGB();
    }

    private static String createKey(TerrainType meepleType, int size) {
        return meepleType + KEY_SEPARATOR + size + KEY_SEPARATOR;
    }

    // copies a image to avoid side effects.
    private static BufferedImage deepCopy(BufferedImage image) {
        ColorModel model = image.getColorModel();
        boolean isAlphaPremultiplied = model.isAlphaPremultiplied();
        WritableRaster raster = image.copyData(image.getRaster().createCompatibleWritableRaster());
        return ImageLoadingUtil.makeCompatible(new BufferedImage(model, raster, isAlphaPremultiplied, null));
    }

    // Colors a meeple with RGB color.
    private static Image paintMeeple(TerrainType meepleType, int color, int size) {
        BufferedImage image = deepCopy(imageMap.get(meepleType));
        BufferedImage template = templateMap.get(meepleType);
        for (int x = 0; x < template.getWidth(); x++) {
            for (int y = 0; y < template.getHeight(); y++) {
                if (template.getRGB(x, y) == Color.BLACK.getRGB()) {
                    image.setRGB(x, y, color);
                }
            }
        }
        return image.getScaledInstance(size, size, Image.SCALE_SMOOTH);
    }

    private static Image scaleDown(Image image, int size, boolean fastScaling, int transparency) {
        return ImageLoadingUtil.makeCompatible(scaleDown(image, size, fastScaling), transparency);
    }

    private static Image scaleDown(Image image, int size, boolean fastScaling) {
        if (fastScaling) {
            return FastImageScaler.scaleDown(image, size);
        }
        return image.getScaledInstance(size, size, Image.SCALE_SMOOTH);
    }
}

/**
 * Interface for the direct notification of view components.
 * @author Timur Saglam
 */
public interface NotifiableView {
    /**
     * Notifies the notifiable view element about changes in the game options. The changes need to be retrieved directly.
     */
    void notifyChange();
}


import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;

import carcassonne.model.Round;
import carcassonne.model.tile.Tile;
import carcassonne.view.main.MainView;
import carcassonne.view.menubar.Scoreboard;
import carcassonne.view.secondary.MeepleView;
import carcassonne.view.secondary.TileView;
import carcassonne.view.tertiary.GameStatisticsView;
import carcassonne.view.util.GameMessage;

/**
 * Facade class for the different views that manages proper swing threading for the UI access. This is not a traditional
 * facade per se, as it technically grant access to its managed objects.
 * @author Timur Saglam
 */
public class ViewFacade {
    private final MainView mainView;
    private final MeepleView meepleView;
    private final Scoreboard scoreboard;
    private final TileView tileView;
    private GameStatisticsView gameStatistics;
    private Tile selectedTile;
    private int jobCounter;

    /**
     * Creates a view container that encapsulates the access to the three main user interfaces. Can be seen as view facade.
     * @param mainView is the main view showing the grid and the menu bar.
     * @param tileView is the view used for previewing and rotating tiles.
     * @param meepleView is the view used to place meeples.
     */
    public ViewFacade(MainView mainView, TileView tileView, MeepleView placmementView) {
        if (mainView == null || tileView == null || placmementView == null) {
            throw new IllegalArgumentException("View container can only contain non-null views!");
        }
        this.mainView = mainView;
        this.tileView = tileView;
        this.meepleView = placmementView;
        this.scoreboard = mainView.getScoreboard();
    }

    /**
     * Executes a job on the main view. The job is scheduled with the {@link EventQueue}.
     * @param job is the job of form of a {@link Consumer}.
     */
    public void onMainView(Consumer<MainView> job) {
        schedule(() -> job.accept(mainView));
    }

    /**
     * Executes a job on the placement view. The job is scheduled with the {@link EventQueue}.
     * @param job is the job of form of a {@link Consumer}.
     */
    public void onMeepleView(Consumer<MeepleView> job) {
        schedule(() -> job.accept(meepleView));
    }

    /**
     * Executes a job on the scoreboard. The job is scheduled with the {@link EventQueue}.
     * @param job is the job of form of a {@link Consumer}.
     */
    public void onScoreboard(Consumer<Scoreboard> job) {
        schedule(() -> job.accept(scoreboard));
    }

    /**
     * Executes a job on the tile view. The job is scheduled with the {@link EventQueue}.
     * @param job is the job of form of a {@link Consumer}.
     */
    public void onTileView(Consumer<TileView> job) {
        schedule(() -> job.accept(tileView));
    }

    /**
     * Creates and shows a game statistics view for the current round.
     * @param round is the current round.
     */
    public void showGameStatistics(Round round) {
        schedule(() -> gameStatistics = new GameStatisticsView(mainView, round));
    }

    /**
     * Closes the statistics view.
     */
    public void closeGameStatistics() {
        schedule(() -> {
            if (gameStatistics != null) {
                gameStatistics.closeView();
            }
        });
    }

    /**
     * Returns the selected tile from the tile view.
     * @return the selected tile.
     */
    public Tile getSelectedTile() {
        try {
            EventQueue.invokeAndWait(() -> selectedTile = tileView.getSelectedTile());
        } catch (InvocationTargetException | InterruptedException exception) {
            exception.printStackTrace();
            GameMessage.showError("Cannot retrieve selected tile:" + System.lineSeparator() + exception);
        }
        return selectedTile;
    }

    /**
     * Indicates whether there are jobs that are queued but are not completed yet.
     * @return true if there is at least one unfinished job.
     */
    public boolean isBusy() {
        return jobCounter > 0;
    }

    /**
     * Schedules and tracks a job.
     */
    private void schedule(Runnable job) {
        synchronized (this) {
            jobCounter++;
        }
        EventQueue.invokeLater(() -> {
            job.run();
            synchronized (this) {
                jobCounter--;
            }
        });
    }
}


import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;

import carcassonne.control.ControllerFacade;
import carcassonne.view.main.MainView;
import carcassonne.view.main.ZoomMode;
import carcassonne.view.secondary.TileView;

/**
 * Container class for global key bindings. Offers a global action map and input map which are supposed to be used as
 * parents to the local ones.
 * @author Timur Saglam
 */
public class GlobalKeyBindingManager {
    private static final int NO_MODIFIER = 0;
    private final Map<String, Action> actions;
    private final Map<String, KeyStroke> inputs;
    private final List<String> inputToActionKeys;
    private final MainView mainView;
    private final TileView previewUI;
    private final ControllerFacade controller;

    /**
     * Creates the key binding manager.
     * @param controller is the main controller.
     * @param mainView is the main user interface.
     * @param previewUI is the user interface for rotating tiles.
     */
    public GlobalKeyBindingManager(ControllerFacade controller, MainView mainView, TileView previewUI) {
        this.mainView = mainView;
        this.previewUI = previewUI;
        this.controller = controller;
        actions = new HashMap<>();
        inputs = new HashMap<>();
        inputToActionKeys = new ArrayList<>();
        addZoomKeyBindings();
        addRotationBindings();
        addSelectionBindings();
    }

    /**
     * Adds a new global key binding that can be added to input and action maps.
     * @param inputToActionKey is the key that connects the key stroke and the action.
     * @param keyStroke is the key stroke that defines the key to press.
     * @param action is the action to be executed on the key stroke.
     */
    public void addKeyBinding(String inputToActionKey, KeyStroke keyStroke, Action action) {
        actions.put(inputToActionKey, action);
        inputs.put(inputToActionKey, keyStroke);
        inputToActionKeys.add(inputToActionKey);
    }

    /**
     * Adds the global key bindings of the manager to the specified maps.
     * @param inputMap is the map for the key inputs.
     * @param actionMap is the map for the consequential actions.
     */
    public void addKeyBindingsToMaps(InputMap inputMap, ActionMap actionMap) {
        for (String key : inputToActionKeys) {
            inputMap.put(inputs.get(key), key);
            actionMap.put(key, actions.get(key));
        }
    }

    private void addSelectionBindings() {
        // SELECT TILE ABOVE:
        KeyStroke upStroke = KeyStroke.getKeyStroke(KeyEvent.VK_UP, NO_MODIFIER);
        Action selectAboveAction = new AbstractAction() {
            private static final long serialVersionUID = 5619589338409339194L;

            @Override
            public void actionPerformed(ActionEvent event) {
                previewUI.selectAbove();
            }
        };
        addKeyBinding("up", upStroke, selectAboveAction);

        // SELECT TILE BELOW:
        KeyStroke downStroke = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, NO_MODIFIER);
        Action selectBelowAction = new AbstractAction() {
            private static final long serialVersionUID = -8199202670185430564L;

            @Override
            public void actionPerformed(ActionEvent event) {
                previewUI.selectBelow();
            }
        };
        addKeyBinding("down", downStroke, selectBelowAction);
    }

    private void addRotationBindings() {
        // ROTATE TILE LEFT:
        KeyStroke leftStroke = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, NO_MODIFIER);
        Action rotateLeftAction = new AbstractAction() {
            private static final long serialVersionUID = 5619589338409339194L;

            @Override
            public void actionPerformed(ActionEvent event) {
                previewUI.rotateLeft();
            }
        };
        addKeyBinding("left", leftStroke, rotateLeftAction);

        // ROTATE TILE RIGHT:
        KeyStroke rightStroke = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, NO_MODIFIER);
        Action rotateRightAction = new AbstractAction() {
            private static final long serialVersionUID = -8199202670185430564L;

            @Override
            public void actionPerformed(ActionEvent event) {
                previewUI.rotateRight();
            }
        };
        addKeyBinding("right", rightStroke, rotateRightAction);

        // ROTATE TILE RIGHT:
        KeyStroke escapeStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, NO_MODIFIER);
        Action skipAction = new AbstractAction() {
            private static final long serialVersionUID = -596225951682450564L;

            @Override
            public void actionPerformed(ActionEvent event) {
                controller.requestSkip();
            }
        };
        addKeyBinding("escape", escapeStroke, skipAction);
    }

    private void addZoomKeyBindings() {
        // ZOOM IN:
        KeyStroke plusStroke = KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, NO_MODIFIER, true);
        Action zoomInAction = new AbstractAction() {
            private static final long serialVersionUID = -4507116452291965942L;

            @Override
            public void actionPerformed(ActionEvent event) {
                mainView.zoomIn(ZoomMode.SMOOTH);
            }
        };
        addKeyBinding("plus", plusStroke, zoomInAction);
        // ZOOM OUT:
        KeyStroke minusStroke = KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, NO_MODIFIER, true);
        Action zoomOutAction = new AbstractAction() {
            private static final long serialVersionUID = 6989306054283945118L;

            @Override
            public void actionPerformed(ActionEvent event) {
                mainView.zoomOut(ZoomMode.SMOOTH);
            }
        };
        addKeyBinding("minus", minusStroke, zoomOutAction);
        // META RELEASED:
        KeyStroke metaReleased = KeyStroke.getKeyStroke(KeyEvent.VK_META, NO_MODIFIER, true);
        Action metaReleasedAction = new AbstractAction() {
            private static final long serialVersionUID = 7691032728389757637L;

            @Override
            public void actionPerformed(ActionEvent event) {
                mainView.updateToChangedZoomLevel(ZoomMode.SMOOTH);
            }
        };
        addKeyBinding("meta", metaReleased, metaReleasedAction);
        // CTRL RELEASED:
        KeyStroke controlReleased = KeyStroke.getKeyStroke(KeyEvent.VK_CONTROL, NO_MODIFIER, true);
        Action controlReleasedAction = new AbstractAction() {
            private static final long serialVersionUID = -6876264975601522997L;

            @Override
            public void actionPerformed(ActionEvent event) {
                mainView.updateToChangedZoomLevel(ZoomMode.SMOOTH);
            }
        };
        addKeyBinding("control", controlReleased, controlReleasedAction);
    }
}


import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import carcassonne.view.main.MainView;
import carcassonne.view.main.ZoomMode;

/**
 * Smoothing event and mouse listener for the zoom slider, which smoothes the dragging by limiting the updates to
 * certain steps.
 * @author Timur Saglam
 */
public class ZoomSliderListener extends MouseAdapter implements ChangeListener {

    private static final double SMOOTHING_FACTOR = 5.0; // only update zoom with this step size.
    private final MainView mainView;
    private final JSlider slider;
    private int previousValue;
    private boolean blockingEvents;

    /**
     * Creates the listener.
     * @param mainView is the main user interface, needed for zooming.
     * @param slider is the slider, needed for the values.
     */
    public ZoomSliderListener(MainView mainView, JSlider slider) {
        this.mainView = mainView;
        this.slider = slider;
        previousValue = -1;
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        mainView.setZoom(slider.getValue(), ZoomMode.SMOOTH);
    }

    @Override
    public void stateChanged(ChangeEvent event) {
        int smoothedValue = (int) (Math.round(slider.getValue() / SMOOTHING_FACTOR) * SMOOTHING_FACTOR);
        if (previousValue != smoothedValue && !blockingEvents) { // limit zoom updated when dragging.
            previousValue = smoothedValue;
            mainView.setZoom(smoothedValue, ZoomMode.FAST);
        }
    }

    /**
     * Checks if events are blocked.
     * @return the blockingEvents
     */
    public boolean isBlockingEvents() {
        return blockingEvents;
    }

    /**
     * Blocks or unblocks all events.
     * @param blockingEvents specifies if events are blocked or not.
     */
    public void setBlockingEvents(boolean blockingEvents) {
        this.blockingEvents = blockingEvents;
    }
}


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;

import carcassonne.control.ControllerFacade;

/**
 * A simple listener for the abort game button.
 * @author Timur Saglam
 */
public class AbortRoundListener implements ActionListener {
    private final ControllerFacade controller;
    private final JMenuItem itemNewRound;
    private final JMenuItem itemAbortRound;

    /**
     * Creates the listener.
     * @param controller is the main controller to request actions.
     * @param itemNewRound is the menu item to start a new round.
     * @param itemAbortRound is the menu item to abort the current round.
     */
    public AbortRoundListener(ControllerFacade controller, JMenuItem itemNewRound, JMenuItem itemAbortRound) {
        this.controller = controller;
        this.itemNewRound = itemNewRound;
        this.itemAbortRound = itemAbortRound;
    }

    /**
     * Calls method on main menu bar for aborting the current game.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        if (itemAbortRound.isEnabled()) {
            controller.requestAbortGame();
            itemAbortRound.setEnabled(false);
            itemNewRound.setEnabled(true);
        }
    }
}


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;

import carcassonne.control.ControllerFacade;

/**
 * A simple listener for the new game button.
 * @author Timur Saglam
 */
public class NewRoundListener implements ActionListener {
    private final ControllerFacade controller;
    private final JMenuItem itemNewRound;
    private final JMenuItem itemAbortRound;

    /**
     * Creates the listener.
     * @param controller is the main controller to request actions.
     * @param itemNewRound is the menu item to start a new round.
     * @param itemAbortRound is the menu item to abort the current round.
     */
    public NewRoundListener(ControllerFacade controller, JMenuItem itemNewRound, JMenuItem itemAbortRound) {
        this.controller = controller;
        this.itemNewRound = itemNewRound;
        this.itemAbortRound = itemAbortRound;
    }

    /**
     * Calls method on main menu bar for a new game.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        if (itemNewRound.isEnabled()) {
            controller.requestNewRound();
            itemAbortRound.setEnabled(true);
            itemNewRound.setEnabled(false);
        }
    }
}


import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import carcassonne.control.ControllerFacade;
import carcassonne.settings.GameSettings;
import carcassonne.view.main.MainView;
import carcassonne.view.tertiary.GridSizeDialog;
import carcassonne.view.tertiary.PlayerSettingsView;
import carcassonne.view.tertiary.TileDistributionView;
import carcassonne.view.util.GameMessage;

/**
 * The menu bar for the main view.
 * @author Timur Saglam
 */
public class MainMenuBar extends JMenuBar {
    // ID:
    private static final long serialVersionUID = -599734693130415390L;

    // TEXT:
    private static final String DISTRIBUTION = "Change Tile Distribution";
    private static final String GRID_SIZE = "Change Grid Size";
    private static final String ABORT = "Abort Current Game";
    private static final String GAME = "Game";
    private static final String LARGE_SPACE = "          ";
    private static final String NEW_ROUND = "Start New Round";
    private static final String OPTIONS = "Options";
    private static final String PLAYER_SETTINGS = "Player Settings";
    private static final String VIEW = "View";
    private static final String ABOUT = "About";

    // STATE:
    private final ControllerFacade controller;
    private JMenuItem itemAbortRound;
    private JMenuItem itemNewRound;
    private final MainView mainView;
    private final Scoreboard scoreboard;
    private final GameSettings settings;
    private ZoomSlider slider;
    private final TileDistributionView tileDistributionUI;
    private final PlayerSettingsView playerView;

    /**
     * Simple constructor creating the menu bar.
     * @param controller sets the connection to game the controller.
     * @param mainView is the main view instance.
     */
    public MainMenuBar(ControllerFacade controller, MainView mainView) {
        super();
        this.controller = controller;
        this.mainView = mainView;
        settings = controller.getSettings();
        scoreboard = new Scoreboard(settings, mainView);
        tileDistributionUI = new TileDistributionView(settings);
        playerView = new PlayerSettingsView(settings, scoreboard);
        buildGameMenu();
        buildOptionsMenu();
        buildViewMenu();
        add(new JLabel(LARGE_SPACE));
        add(scoreboard);
    }

    /**
     * Enables the start button and disables the abort button.
     */
    public void enableStart() {
        itemNewRound.setEnabled(true);
        itemAbortRound.setEnabled(false);
    }

    /**
     * Grants access to the scoreboard of the menu bar.
     * @return the scoreboard.
     */
    public Scoreboard getScoreboard() {
        return scoreboard;
    }

    /**
     * Grants access to the zoom slider of the menu bar.
     * @return the slider.
     */
    public ZoomSlider getZoomSlider() {
        return slider;
    }

    // adds labels of the scoreboard to the menu bar.
    private void add(Scoreboard scoreboard) {
        for (JLabel label : scoreboard.getLabels()) {
            add(label);
        }
    }

    private void buildGameMenu() {
        itemNewRound = new JMenuItem(NEW_ROUND);
        itemAbortRound = new JMenuItem(ABORT);
        JMenuItem itemAbout = new JMenuItem(ABOUT);
        itemAbortRound.setEnabled(false);
        itemAbout.addActionListener(event -> GameMessage.showGameInfo());
        itemNewRound.addActionListener(new NewRoundListener(controller, itemNewRound, itemAbortRound));
        itemAbortRound.addActionListener(new AbortRoundListener(controller, itemNewRound, itemAbortRound));
        JMenu menuGame = new JMenu(GAME);
        menuGame.add(itemNewRound);
        menuGame.add(itemAbortRound);
        menuGame.addSeparator();
        menuGame.add(itemAbout);
        add(menuGame);
    }

    private void buildOptionsMenu() {
        JMenu menuOptions = new JMenu(OPTIONS);
        JMenuItem itemPlayerSettings = new JMenuItem(PLAYER_SETTINGS);
        itemPlayerSettings.addActionListener(it -> playerView.setVisible(true));
        menuOptions.add(itemPlayerSettings);
        menuOptions.addSeparator();
        JMenuItem itemGridSize = new JMenuItem(GRID_SIZE);
        GridSizeDialog dialog = new GridSizeDialog(settings);
        itemGridSize.addActionListener(event -> dialog.showDialog());
        menuOptions.add(itemGridSize);
        JMenuItem itemDistribution = new JMenuItem(DISTRIBUTION);
        itemDistribution.addActionListener(event -> tileDistributionUI.setVisible(true));
        menuOptions.add(itemDistribution);
        add(menuOptions);
    }

    private void buildViewMenu() {
        slider = new ZoomSlider(mainView);
        JMenu menuView = new JMenu(VIEW);
        menuView.add(slider.getZoomIn());
        menuView.add(slider);
        menuView.add(slider.getZoomOut());
        add(menuView);
    }
}


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import carcassonne.settings.GameSettings;
import carcassonne.view.main.MainView;
import carcassonne.view.tertiary.PlayerEstheticsView;

/**
 * A mouse adapter and action listener for the UI that allows the selection of a player's colors and name.
 * @author Timur Saglam
 */
public class MenuViewListener extends MouseAdapter implements ActionListener {
    private final PlayerEstheticsView playerView;

    /**
     * Simple constructor that creates the correlating player view.
     * @param playerNumber is the number of the player.
     * @param settings are the {@link GameSettings}.
     * @param mainView is the main user interface.
     */
    public MenuViewListener(int playerNumber, GameSettings settings, MainView mainView) {
        super();
        playerView = new PlayerEstheticsView(playerNumber, settings, mainView);
    }

    @Override
    public void mousePressed(MouseEvent event) {
        playerView.updateAndShow();
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        playerView.updateAndShow();
    }
}


import javax.swing.JMenuItem;
import javax.swing.JSlider;
import javax.swing.SwingConstants;

import carcassonne.view.main.MainView;
import carcassonne.view.main.ZoomMode;

/**
 * Custom {@link JSlider} for the zoom functionality. Additionally, this class creates the zoom in/out menu items.
 * @author Timur Saglam
 */
public class ZoomSlider extends JSlider {
    private static final long serialVersionUID = -5518487902213410121L;

    // SEMANTIC CONSTANTS:
    private static final int MAXIMUM_VALUE = 300;
    private static final int MINIMUM_VALUE = 25;
    private static final int SLIDER_STEP_SIZE = 25;

    // UI CONSTANTS:
    private static final int MAJOR_TICK = 50;
    private static final int MINOR_TICK = 5;
    private static final String ZOOM_OUT = "Zoom Out (-)";
    private static final String ZOOM_IN = "Zoom In (+)";

    // FIELDS:
    private final JMenuItem zoomOut;
    private final JMenuItem zoomIn;
    private final ZoomSliderListener zoomListener;

    /**
     * Creates the slider.
     * @param mainView is the correlating main user interface.f
     */
    public ZoomSlider(MainView mainView) {
        super(MINIMUM_VALUE, MAXIMUM_VALUE, mainView.getZoom());
        setPaintTicks(true);
        setOrientation(SwingConstants.VERTICAL);
        setMinorTickSpacing(MINOR_TICK);
        setMajorTickSpacing(MAJOR_TICK);
        setSnapToTicks(true);
        zoomListener = new ZoomSliderListener(mainView, this);
        addMouseListener(zoomListener);
        addChangeListener(zoomListener);
        zoomIn = new JMenuItem(ZOOM_IN);
        zoomOut = new JMenuItem(ZOOM_OUT);
        zoomIn.addActionListener(event -> {
            setValueSneakily(getValue() + SLIDER_STEP_SIZE);
            mainView.zoomIn(ZoomMode.SMOOTH);
        });
        zoomOut.addActionListener(event -> {
            setValueSneakily(getValue() - SLIDER_STEP_SIZE);
            mainView.zoomOut(ZoomMode.SMOOTH);
        });
    }

    /**
     * Sets the slider value without triggering the zoom slider listener event.
     * @param value is the value to set.
     * @see JSlider#setValue(int)
     */
    public void setValueSneakily(int value) {
        zoomListener.setBlockingEvents(true);
        setValue(value);
        zoomListener.setBlockingEvents(false);
    }

    /**
     * Grants access to a menu item for a zoom out step. It updates the slider when clicked and guarantees non-interference.
     * @return the zoomOut the the
     */
    public JMenuItem getZoomOut() {
        return zoomOut;
    }

    /**
     * Grants access to a menu item for a zoom in step. It updates the slider when clicked and guarantees non-interference.
     * @return the zoomIn menu item.
     */
    public JMenuItem getZoomIn() {
        return zoomIn;
    }
}


import java.awt.Font;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JLabel;

import carcassonne.model.Player;
import carcassonne.settings.GameSettings;
import carcassonne.view.NotifiableView;
import carcassonne.view.main.MainView;

/**
 * Is the scoreboard class of the game. Manages a score label for each player.
 * @author Timur Saglam
 */
public class Scoreboard implements NotifiableView {
    private static final String FONT_TYPE = "Helvetica";
    private static final String TOOL_TIP = "Settings for player ";
    private final JLabel[] scoreLabels;
    private final JLabel stackSizeLabel;
    private final List<JLabel> allLabels;
    private final List<ActionListener> settingsListeners;
    private final GameSettings settings;

    /**
     * Standard constructor. Creates score board.
     * @param settings are the {@link GameSettings}.
     * @param mainView is the main user interface.
     */
    public Scoreboard(GameSettings settings, MainView mainView) { // TODO (MEDIUM) [UI] link with players?
        this.settings = settings;
        scoreLabels = new JLabel[GameSettings.MAXIMAL_PLAYERS];
        settingsListeners = new ArrayList<>();
        for (int i = 0; i < scoreLabels.length; i++) {
            scoreLabels[i] = new JLabel();
            scoreLabels[i].setForeground(settings.getPlayerColor(i).textColor());
            MenuViewListener listener = new MenuViewListener(i, settings, mainView);
            settingsListeners.add(listener);
            scoreLabels[i].addMouseListener(listener);
        }
        stackSizeLabel = new JLabel();
        allLabels = new ArrayList<>(Arrays.asList(scoreLabels));
        allLabels.add(stackSizeLabel);
        for (JLabel label : allLabels) {
            label.setFont(new Font(FONT_TYPE, Font.BOLD, 12));
        }
    }

    /**
     * Disables all the scoreboard labels.
     */
    public void disable() {
        allLabels.forEach(it -> it.setVisible(false));
        stackSizeLabel.setVisible(false);
    }

    /**
     * Grants access to the labels themselves.
     * @return the array of labels.
     */
    public List<JLabel> getLabels() {
        return allLabels;
    }

    /**
     * Only shows the specified amount of labels.
     * @param playerCount is the amount of players to show labels for.
     */
    public void rebuild(int playerCount) {
        for (int i = 0; i < playerCount; i++) {
            scoreLabels[i].setVisible(true);
        }
        stackSizeLabel.setVisible(true);
    }

    /**
     * Updates a specific player label of the scoreboard.
     * @param player is the player whose scoreboard should be updated.
     */
    public void update(Player player) {
        String playerName = player.getName();
        String text = "[" + playerName + ": " + player.getScore() + " points, " + player.getFreeMeeples() + " meeples]    ";
        scoreLabels[player.getNumber()].setText(text);
        scoreLabels[player.getNumber()].setToolTipText(TOOL_TIP + player.getName());
    }

    /**
     * Updates the stack size label.
     * @param stackSize is the updated size of the stack.
     */
    public void updateStackSize(int stackSize) {
        stackSizeLabel.setText("   [Stack Size: " + stackSize + "]");
    }

    /**
     * Grants access to a specific mouse listener of one players settings.
     * @param playerNumber specifies the player.
     * @return the correlating mouse listener.
     */
    public ActionListener getSettingsListener(int playerNumber) {
        return settingsListeners.get(playerNumber);
    }

    @Override
    public void notifyChange() {
        for (int i = 0; i < scoreLabels.length; i++) {
            scoreLabels[i].setForeground(settings.getPlayerColor(i).textColor()); // replace only color and player name:
            scoreLabels[i].setText(scoreLabels[i].getText().replaceFirst("\\[.*?:", "[" + settings.getPlayerName(i) + ":"));
            scoreLabels[i].setToolTipText(TOOL_TIP + settings.getPlayerName(i));
        }
    }
}


import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import carcassonne.model.terrain.TerrainType;
import carcassonne.model.tile.TileDistribution;
import carcassonne.settings.GameSettings;
import carcassonne.util.MinkowskiDistance;
import carcassonne.view.NotifiableView;
import carcassonne.view.menubar.Scoreboard;
import carcassonne.view.util.FixedSizeLabel;
import carcassonne.view.util.MouseClickListener;

/**
 * All-in-one settings UI for the player settings. This includes the player name, the player type and the number of
 * players.
 * @author Timur Saglam
 */
public class PlayerSettingsView extends JDialog implements NotifiableView {

    // GENERAL:
    private static final long serialVersionUID = -4633728570151183720L;

    // UI LAYOUT:
    private static final int PADDING = 5;
    private static final int PLAYER_LABEL_WIDTH = 75;
    private static final int PLAYER_LABEL_POSITION = 1;
    private static final int BUTTON_VERTICAL_STRUT = 200;
    private static final int CLOSE_BUTTON_WIDTH = 125;

    // TEXT:
    private static final String AESTHETIC = "AI Aesthetic:";
    private static final String AESTHETIC_TOOL_TIP = "Affects how AI players place tiles. The effect is most pronounced with only AI players and a bigger tile stack.";
    private static final String HAND_TOOL_TIP = "Number of tiles on the player's hand";
    private static final String HAND = "Hand of Tiles:";
    private static final String MEEPLE_RULES = "Meeple Placement on:";
    private static final String MEEPLE_RULES_TOOL_TIP = "Allow or forbid placing meeples on certain terrain";
    private static final String FORTIFYING = " Allow Fortifying Own Patterns:";
    private static final String FORTIFYING_TOOL_TIP = "Allow or forbid directly placing meeples on own patterns";
    private static final String ENCLAVE = "Allow Enclaves of Free Spots";
    private static final String ENCLAVE_TOOL_TIP = "Allow or forbid enclosing free spots in the grid, leading to these spots forming enclaves.";
    private static final String SCORE_SPLITTING = " Split Score on Shared Patterns:";
    private static final String SCORE_SPLITTING_TOOL_TIP = "Split score among dominant players of pattern instead of warding the full score.";
    private static final String MULTI_TILE = " Tiles";
    private static final String CLASSIC = " Tile (Classic)";
    private static final String CUSTOMIZE = "Customize";
    private static final String AI_PLAYER = "AI player";
    private static final String PLAYERS = "Players:";
    private static final String CLOSE = "Close";
    private static final String TITLE = "Player Settings";
    private static final String COLON = ":";
    private static final String PLAYER = "Player ";
    private static final String SPACE = " ";

    // STATE:
    private final GameSettings settings;
    private final Scoreboard scoreboard;
    private final List<JPanel> playerPanels;
    private final List<JLabel> playerLabels;

    /**
     * Creates the UI and shows it.
     * @param distribution is the {@link TileDistribution} to show in the UI.
     */
    public PlayerSettingsView(GameSettings settings, Scoreboard scoreboard) {
        this.settings = settings;
        this.scoreboard = scoreboard;
        playerPanels = new ArrayList<>();
        playerLabels = new ArrayList<>();
        buildPanels();
        buildWindow();
        settings.registerNotifiable(this);
    }

    @Override
    public void notifyChange() {
        for (int player = 0; player < GameSettings.MAXIMAL_PLAYERS; player++) {
            JLabel label = new FixedSizeLabel(SPACE + settings.getPlayerName(player), PLAYER_LABEL_WIDTH);
            label.setForeground(settings.getPlayerColor(player));
            if (playerLabels.size() == GameSettings.MAXIMAL_PLAYERS) {
                playerPanels.get(player).remove(playerLabels.remove(player));
            }
            playerPanels.get(player).add(label, PLAYER_LABEL_POSITION);
            playerLabels.add(player, label);
        }
        validate();
    }

    private void addWithBox(JPanel panel, JComponent component) {
        panel.add(component);
        panel.add(Box.createRigidArea(new Dimension(0, PADDING)));
    }

    private void buildPanels() {
        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(Color.GRAY);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
        BoxLayout layout = new BoxLayout(mainPanel, BoxLayout.Y_AXIS);
        mainPanel.setLayout(layout);
        addWithBox(mainPanel, createPlayerNumberPanel());
        addWithBox(mainPanel, createHandOfTilePanel());
        addWithBox(mainPanel, createPlayerPanel());
        addWithBox(mainPanel, createAIAestheticPanel());
        addWithBox(mainPanel, createPlacementRulePanel());
        addWithBox(mainPanel, createScoreSplittingPanel());
        addWithBox(mainPanel, createDualPanel());
        mainPanel.add(createCloseButton());
        getContentPane().add(mainPanel);
    }

    private void buildWindow() {
        setTitle(TITLE);
        setResizable(false);
        setModalityType(ModalityType.APPLICATION_MODAL);
        pack();
        setLocationRelativeTo(null);
    }

    private void createAIAestheticButton(MinkowskiDistance distanceMeasure, JPanel panel, ButtonGroup group) {
        JRadioButton button = new JRadioButton(distanceMeasure.getDescription());
        button.setSelected(settings.getDistanceMeasure() == distanceMeasure);
        button.addMouseListener((MouseClickListener) event -> settings.setDistanceMeasure(distanceMeasure));
        group.add(button);
        panel.add(button);
    }

    private JPanel createAIAestheticPanel() {
        JPanel panel = createBasicPanel(AESTHETIC);
        panel.setToolTipText(AESTHETIC_TOOL_TIP);
        ButtonGroup group = new ButtonGroup();
        for (MinkowskiDistance distanceMeasure : MinkowskiDistance.values()) {
            createAIAestheticButton(distanceMeasure, panel, group);
        }
        return panel;
    }

    private JPanel createBasicPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.LEFT));
        panel.setBackground(Color.LIGHT_GRAY);
        panel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));
        return panel;
    }

    private JPanel createBasicPanel(String labelText) {
        JPanel panel = createBasicPanel();
        panel.add(embolden(new JLabel(labelText + SPACE)));
        return panel;
    }

    private JPanel createCloseButton() {
        JButton closeButton = new JButton(CLOSE);
        closeButton.addActionListener(it -> dispose());
        closeButton.setPreferredSize(new Dimension(CLOSE_BUTTON_WIDTH, closeButton.getPreferredSize().height));
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.add(closeButton); // Weirdly, the button needs to be in a panel or it will not be centered.
        return buttonPanel;
    }

    private JPanel createDualPanel() {
        JPanel dualPanel = new JPanel();
        dualPanel.setOpaque(false);
        dualPanel.setLayout(new BoxLayout(dualPanel, BoxLayout.X_AXIS));
        dualPanel.add(createFortificationPanel());
        dualPanel.add(Box.createRigidArea(new Dimension(PADDING, 0)));
        dualPanel.add(createEnclavePanel());
        return dualPanel;
    }

    private JPanel createFortificationPanel() {
        JPanel panel = createBasicPanel(FORTIFYING);
        panel.setToolTipText(FORTIFYING_TOOL_TIP);
        JCheckBox checkBox = new JCheckBox();
        checkBox.setSelected(settings.isAllowingFortifying());
        checkBox.addActionListener(event -> settings.setAllowFortifying(checkBox.isSelected()));
        panel.add(checkBox);
        return panel;
    }

    private JPanel createEnclavePanel() {
        JPanel panel = createBasicPanel(ENCLAVE);
        panel.setToolTipText(ENCLAVE_TOOL_TIP);
        JCheckBox checkBox = new JCheckBox();
        checkBox.setSelected(settings.isAllowingEnclaves());
        checkBox.addActionListener(event -> settings.setAllowEnclaves(checkBox.isSelected()));
        panel.add(checkBox);
        return panel;
    }

    private JPanel createScoreSplittingPanel() {
        JPanel panel = createBasicPanel(SCORE_SPLITTING);
        panel.setToolTipText(SCORE_SPLITTING_TOOL_TIP);
        JCheckBox checkBox = new JCheckBox();
        checkBox.setSelected(settings.getSplitPatternScore());
        checkBox.addActionListener(event -> settings.setSplitPatternScore(checkBox.isSelected()));
        panel.add(checkBox);
        return panel;
    }

    private void createHandOfTileButton(int numberOfTiles, JPanel panel, ButtonGroup group) {
        String description = numberOfTiles + (numberOfTiles == 1 ? CLASSIC : MULTI_TILE);
        JRadioButton button = new JRadioButton(description);
        button.setSelected(settings.getTilesPerPlayer() == numberOfTiles);
        button.addMouseListener((MouseClickListener) event -> settings.setTilesPerPlayer(numberOfTiles));
        group.add(button);
        panel.add(button);
    }

    private JPanel createHandOfTilePanel() {
        JPanel panel = createBasicPanel(HAND);
        panel.setToolTipText(HAND_TOOL_TIP);
        ButtonGroup group = new ButtonGroup();
        for (int tiles = 1; tiles <= GameSettings.MAXIMAL_TILES_ON_HAND; tiles++) {
            createHandOfTileButton(tiles, panel, group);
        }
        return panel;
    }

    private void createPlacementRuleButton(TerrainType type, JPanel panel) {
        JCheckBox button = new JCheckBox(type.toReadableString());
        button.setSelected(settings.getMeepleRule(type));
        button.addMouseListener((MouseClickListener) event -> settings.toggleMeepleRule(type));
        panel.add(button);
    }

    private JPanel createPlacementRulePanel() {
        JPanel panel = createBasicPanel(MEEPLE_RULES);
        panel.setToolTipText(MEEPLE_RULES_TOOL_TIP);
        for (TerrainType type : TerrainType.basicTerrain()) {
            createPlacementRuleButton(type, panel);
        }
        return panel;
    }

    private void createPlayerNumberButton(int numberOfPlayers, JPanel panel, ButtonGroup group) {
        JRadioButton button = new JRadioButton(Integer.toString(numberOfPlayers) + SPACE + PLAYERS);
        button.setSelected(settings.getNumberOfPlayers() == numberOfPlayers);
        button.addMouseListener((MouseClickListener) event -> settings.setNumberOfPlayers(numberOfPlayers));
        group.add(button);
        panel.add(button);
    }

    private JPanel createPlayerNumberPanel() {
        JPanel panel = createBasicPanel(PLAYERS);
        ButtonGroup group = new ButtonGroup();
        for (int numberOfPlayers = 2; numberOfPlayers <= GameSettings.MAXIMAL_PLAYERS; numberOfPlayers++) {
            createPlayerNumberButton(numberOfPlayers, panel, group);
        }
        return panel;
    }

    private JPanel createPlayerPanel() {
        JPanel playerPanel = createBasicPanel();
        playerPanel.setLayout(new BoxLayout(playerPanel, BoxLayout.Y_AXIS));
        for (int i = 0; i < GameSettings.MAXIMAL_PLAYERS; i++) {
            playerPanel.add(createPlayerRow(i));
        }
        notifyChange(); // set label text and color
        return playerPanel;
    }

    private JPanel createPlayerRow(int playerNumber) {
        JPanel panel = new JPanel();
        playerPanels.add(panel);
        panel.setLayout(new FlowLayout(FlowLayout.LEFT));
        panel.setOpaque(false);
        panel.add(embolden(new JLabel(PLAYER + (playerNumber + 1) + COLON)));
        JCheckBox checkBox = new JCheckBox(AI_PLAYER);
        checkBox.setSelected(settings.isPlayerComputerControlled(playerNumber));
        checkBox.addActionListener(event -> settings.setPlayerComputerControlled(checkBox.isSelected(), playerNumber));
        panel.add(checkBox);
        panel.add(Box.createHorizontalStrut(BUTTON_VERTICAL_STRUT));
        JButton configurationButton = new JButton(CUSTOMIZE);
        configurationButton.addActionListener(scoreboard.getSettingsListener(playerNumber));
        panel.add(configurationButton);
        return panel;
    }

    private JLabel embolden(JLabel label) {
        Font font = label.getFont();
        label.setFont(font.deriveFont(font.getStyle() | Font.BOLD));
        return label;
    }
}


import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import carcassonne.model.tile.TileDistribution;
import carcassonne.model.tile.TileType;
import carcassonne.settings.GameSettings;
import carcassonne.view.util.MouseClickListener;
import carcassonne.view.util.ThreadingUtil;

/**
 * User interface that shows all tiles and how often they are used in a standard game (two players, chaos mode
 * disabled.)
 * @author Timur Saglam
 */
public class TileDistributionView extends JDialog {
    private static final long serialVersionUID = 1805511300999150753L;
    private static final String MULTIPLIER = "Tile Stack Size Multiplier: ";
    private static final String BRACKET = "\t (";
    private static final String STACK_SIZE = " tiles on the stack)";
    private static final String SHUFFLE = "Shuffle";
    private static final String RESET = "Reset";
    private static final String ACCEPT = "Accept";
    private static final String TITLE = "Tile Distribution";
    private static final int GRID_WIDTH = 11;
    private static final int GRID_HEIGHT = 3;
    private static final int PADDING = 5;
    private final TileDistribution distribution;
    private final List<TileQuantityPanel> quantityPanels;
    private final GameSettings settings;
    private int stackSizeMultiplier;
    private JLabel sizeLabel;

    /**
     * Creates the UI and shows it.
     * @param distribution is the {@link TileDistribution} to show in the UI.
     */
    public TileDistributionView(GameSettings settings) {
        this.settings = settings;
        distribution = settings.getTileDistribution();
        distribution.createBackup();
        quantityPanels = new ArrayList<>();
        stackSizeMultiplier = settings.getStackSizeMultiplier();
        buildPanel();
        buildWindow();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                distribution.restoreLastBackup();
            }

            @Override
            public void windowActivated(WindowEvent event) {
                distribution.createBackup();
                updateFromDistribution();
            }
        });
    }

    /**
     * Recalculates the stack size preview.
     */
    public void updateStackSizePreview() {
        if (sizeLabel != null) {
            Integer size = calculateStackSize(stackSizeMultiplier);
            sizeLabel.setText(BRACKET + size + STACK_SIZE);
            validate();
        }
    }

    /*
     * Builds the main panel and lays out its subcomponents in a grid.
     */
    private void buildPanel() {
        JPanel tilePanel = new JPanel();
        tilePanel.setBackground(Color.GRAY);
        setBackground(Color.GRAY);
        tilePanel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.weighty = 1;
        builtTilePanels(tilePanel, constraints);
        constraints.gridwidth = 6;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        tilePanel.add(buildMultiplierPanel(), constraints);
        constraints.gridwidth = 1;
        constraints.gridx = 8;
        for (JButton button : createButtons()) {
            tilePanel.add(button, constraints);
            constraints.gridx++;
        }
        getContentPane().add(tilePanel);
    }

    private void builtTilePanels(JPanel tilePanel, GridBagConstraints constraints) {
        for (TileType tileType : TileType.enabledTiles()) {
            TileQuantityPanel quantityPanel = new TileQuantityPanel(tileType, distribution.getQuantity(tileType), this);
            quantityPanels.add(quantityPanel);
            tilePanel.add(quantityPanel, constraints);
            constraints.gridx++;
            if (constraints.gridx >= GRID_WIDTH) {
                constraints.gridx = 0;
                constraints.gridy++;
            }
        }
    }

    private JPanel buildMultiplierPanel() {
        JPanel multiplierPanel = new JPanel();
        multiplierPanel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));
        multiplierPanel.setBackground(Color.LIGHT_GRAY);
        multiplierPanel.add(new JLabel(MULTIPLIER));
        ButtonGroup group = new ButtonGroup();
        sizeLabel = new JLabel();
        updateStackSizePreview();
        for (int multiplier = 1; multiplier <= 4; multiplier++) {
            createMultiplierButton(multiplier, multiplierPanel, group);
        }
        multiplierPanel.add(sizeLabel);
        return multiplierPanel;
    }

    private void createMultiplierButton(int multiplier, JPanel multiplierPanel, ButtonGroup group) {
        JRadioButton button = new JRadioButton(multiplier + "x");
        button.setSelected(settings.getStackSizeMultiplier() == multiplier);
        button.addMouseListener((MouseClickListener) event -> {
            stackSizeMultiplier = multiplier;
            updateStackSizePreview();
        });
        group.add(button);
        multiplierPanel.add(button);
    }

    private List<JButton> createButtons() {
        JButton shuffleButton = new JButton(SHUFFLE);
        shuffleButton.addMouseListener((MouseClickListener) event -> {
            ThreadingUtil.runAndCallback(() -> {
                applyChangesToDistribution();
                distribution.shuffle();
            }, this::updateFromDistribution);
        });
        JButton resetButton = new JButton(RESET);
        resetButton.addMouseListener((MouseClickListener) event -> ThreadingUtil.runAndCallback(distribution::reset, this::updateFromDistribution));
        JButton acceptButton = new JButton(ACCEPT);
        acceptButton.addMouseListener((MouseClickListener) event -> {
            dispose();
            ThreadingUtil.runInBackground(() -> {
                settings.setStackSizeMultiplier(stackSizeMultiplier);
                applyChangesToDistribution();
            });
        });
        return List.of(shuffleButton, resetButton, acceptButton);
    }

    private void applyChangesToDistribution() {
        for (TileQuantityPanel panel : quantityPanels) {
            if (panel.getQuantity() >= 0) {
                distribution.setQuantity(panel.getTileType(), panel.getQuantity());
            }
        }
    }

    private void updateFromDistribution() {
        for (TileQuantityPanel panel : quantityPanels) {
            panel.setQuantity(distribution.getQuantity(panel.getTileType()));
        }
    }

    private int calculateStackSize(int multiplier) {
        int size = 0;
        for (TileQuantityPanel panel : quantityPanels) {
            if (panel.getQuantity() >= 0) {
                size += panel.getQuantity();
            } else {
                size += distribution.getQuantity(panel.getTileType());
            }

        }
        return size * multiplier;
    }

    /*
     * Shows and resizes the window.
     */
    private void buildWindow() {
        setTitle(TITLE);
        setResizable(false);
        pack();
        setSize(getWidth() + PADDING * GRID_WIDTH, getHeight() + PADDING * GRID_HEIGHT);
        setLocationRelativeTo(null);
        setModalityType(ModalityType.APPLICATION_MODAL);
    }
}


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;

import carcassonne.control.ControllerFacade;
import carcassonne.model.Round;
import carcassonne.view.GlobalKeyBindingManager;
import carcassonne.view.main.MainView;
import carcassonne.view.util.MouseClickListener;

/**
 * A class for the game statistics view that shows the final scores of a round.
 * @author Timur Saglam
 */
public class GameStatisticsView extends JDialog {
    private static final long serialVersionUID = 2862334382605282126L; // generated UID
    private static final int ADDITIONAL_VERTICLE_SIZE = 100; // ensures that all text is readable
    static final int SCORE_COLUMN = 5;
    static final Color HEADER_COLOR = new Color(220, 220, 220);
    static final Color BODY_COLOR = new Color(237, 237, 237);
    private final ControllerFacade controller;
    private JButton buttonClose;
    private JTable table;

    /**
     * Creates the view and extracts the data from the current round.
     * @param mainView is the main user interface.
     * @param round is the current round.
     * @mainView is the main user interface.
     */
    public GameStatisticsView(MainView mainView, Round round) {
        super(mainView);
        controller = mainView.getController();
        buildTable(round);
        buildButtonClose();
        buildFrame();
        addKeyBindings(controller.getKeyBindings());
    }

    /**
     * Hides and disposes the view.
     */
    public void closeView() {
        setVisible(false);
        dispose();
    }

    /**
     * Adds the global key bindings to this UI.
     * @param keyBindings are the global key bindings.
     */
    private void addKeyBindings(GlobalKeyBindingManager keyBindings) {
        InputMap inputMap = table.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = table.getActionMap();
        keyBindings.addKeyBindingsToMaps(inputMap, actionMap);
    }

    private void buildButtonClose() {
        buttonClose = new JButton("Close");
        buttonClose.addMouseListener((MouseClickListener) event -> {
            setVisible(false);
            controller.requestSkip();
        });
    }

    private void buildFrame() {
        setLayout(new BorderLayout());
        add(table.getTableHeader(), BorderLayout.PAGE_START);
        add(table, BorderLayout.CENTER);
        add(buttonClose, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        setMinimumSize(new Dimension(getSize().width + ADDITIONAL_VERTICLE_SIZE, getSize().height));
        setResizable(false);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                controller.requestSkip();
            }
        });
    }

    private void buildTable(Round round) {
        table = new JTable(new GameStatisticsModel(round));
        // Columns:
        TableColumnModel model = table.getColumnModel();
        CellRenderer renderer = new CellRenderer(round);
        for (int i = 0; i < model.getColumnCount(); i++) {
            model.getColumn(i).setCellRenderer(renderer);
        }
        // Header:
        JTableHeader header = table.getTableHeader();
        header.setDefaultRenderer(new HeaderRenderer());
        header.setReorderingAllowed(false);
        table.setBackground(BODY_COLOR);
    }
}


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.text.NumberFormat;

import javax.swing.BorderFactory;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.NumberFormatter;

import carcassonne.model.tile.Tile;
import carcassonne.model.tile.TileType;
import carcassonne.view.util.MouseClickListener;

/**
 * Panel that depicts the quantity of a tile type. Shows a image of the tile and a text field for the quantity.
 * @author Timur Saglam
 */
public class TileQuantityPanel extends JPanel {
    private static final long serialVersionUID = 6368792603603753333L;
    private static final int TILE_SIZE = 100;
    private static final int BORDER_SIZE = 2;
    private static final String CLICK_TO_ROTATE = "Click to rotate the tile of type ";
    private final TileType type;
    private JTextField textField;

    /**
     * Creates a quantity panel for a certain tile type.
     * @param type is the tile type.
     * @param initialQuantity is the initial quantity depicted in the text field.
     */
    public TileQuantityPanel(TileType type, int initialQuantity, TileDistributionView ui) {
        this.type = type;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, BORDER_SIZE));
        setBackground(Color.LIGHT_GRAY);
        createTileLabel(type);
        createTextField(initialQuantity, ui);
    }

    /**
     * Getter for the tile type this panel is depicting.
     * @return the tile type.
     */
    public TileType getTileType() {
        return type;
    }

    /**
     * Getter for the quantity specified in the text field of the panel.
     * @return the specified quantity or -1 if it is empty.
     */
    public int getQuantity() {
        String text = textField.getText();
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    /**
     * Setter to update the quantity specified in the text field of the panel.
     * @param quantity is the new updated quantity.
     */
    public void setQuantity(int quantity) {
        textField.setText(Integer.toString(quantity));
    }

    private void createTileLabel(TileType type) {
        Tile tile = new Tile(type);
        JLabel label = new JLabel(tile.getScaledIcon(TILE_SIZE));
        label.setToolTipText(CLICK_TO_ROTATE + type.readableRepresentation());
        Font font = label.getFont();
        label.setFont(font.deriveFont(font.getStyle() | Font.BOLD));
        label.addMouseListener((MouseClickListener) event -> {
            tile.rotateRight();
            label.setIcon(tile.getScaledIcon(TILE_SIZE));
        });
        add(label, BorderLayout.NORTH);
    }

    private void createTextField(int initialQuantity, TileDistributionView ui) {
        NumberFormatter formatter = new NumberFormatter(NumberFormat.getIntegerInstance());
        formatter.setMinimum(0);
        formatter.setMaximum(99);
        formatter.setAllowsInvalid(true);
        formatter.setCommitsOnValidEdit(true);
        textField = new JFormattedTextField(formatter);
        textField.addPropertyChangeListener(event -> ui.updateStackSizePreview());
        setQuantity(initialQuantity);
        add(textField, BorderLayout.SOUTH);
    }
}

import java.awt.BorderLayout;
import java.awt.Font;
import java.text.NumberFormat;

import javax.swing.Box;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.text.NumberFormatter;

import carcassonne.settings.GameSettings;
import carcassonne.view.util.GameMessage;

/**
 * Custom dialog for changing the grid size.
 * @author Timur Saglam
 */
public class GridSizeDialog extends JPanel {

    private static final long serialVersionUID = 6533357898928866596L;
    private static final String BRACKET = ")";
    private static final String TOTAL_TILES = "(spots to place tiles: ";
    private static final String INVALID_SIZE = "Invalid grid size!";
    private static final String CROSS = " x ";
    private static final String TITLE = "Carcassonne";
    private static final String WIDTH = "Width:";
    private static final String HEIGHT = "Height:";
    private static final String NOT_CORRECT = " is not a valid grid sizes!";
    private static final String MESSAGE = "<html>Changes to the grid size will affect the next game. Choose values between 3 and 99.<br/>Large grids may affect the performance!</html>";
    private static final int MIN_VALUE = 3;
    private static final int GAP = 5;
    private static final int SPACE = 100;
    private static final int MAX_VALUE = 99;
    private static final int TEXT_FIELD_COLUMNS = 3;
    private final GameSettings settings;
    private final JFormattedTextField heightInput;
    private final JFormattedTextField widthInput;
    private final JLabel numberOfTiles;

    /**
     * Creates a dialog to change the grid size.
     * @param settings are the {@link GameSettings} that will receive the new grid size.
     */
    public GridSizeDialog(GameSettings settings) {
        this.settings = settings;
        numberOfTiles = new JLabel();
        numberOfTiles.setHorizontalAlignment(SwingConstants.CENTER);
        Font font = new Font(numberOfTiles.getFont().getName(), Font.ITALIC, numberOfTiles.getFont().getSize());
        numberOfTiles.setFont(font);
        widthInput = new JFormattedTextField(createNumberFormatter());
        widthInput.setColumns(TEXT_FIELD_COLUMNS);
        heightInput = new JFormattedTextField(createNumberFormatter());
        heightInput.setColumns(TEXT_FIELD_COLUMNS);
        widthInput.addPropertyChangeListener(event -> updateNumberOfTiles());
        heightInput.addPropertyChangeListener(event -> updateNumberOfTiles());
        setLayout(new BorderLayout(GAP, GAP));
        add(new JLabel(MESSAGE), BorderLayout.NORTH);
        JPanel subPanel = new JPanel();
        subPanel.add(new JLabel(WIDTH));
        subPanel.add(widthInput);
        subPanel.add(Box.createHorizontalStrut(SPACE));
        subPanel.add(new JLabel(HEIGHT));
        subPanel.add(heightInput);
        add(subPanel, BorderLayout.CENTER);
        add(numberOfTiles, BorderLayout.SOUTH);
    }

    /**
     * Shows the grid size dialog and waits for the user input which is then sent to the game settings.
     */
    public void showDialog() {
        widthInput.setText(Integer.toString(settings.getGridWidth()));
        heightInput.setText(Integer.toString(settings.getGridHeight()));
        updateNumberOfTiles(settings.getGridWidth(), settings.getGridHeight());
        int result = JOptionPane.showConfirmDialog(null, this, TITLE, JOptionPane.OK_CANCEL_OPTION, JOptionPane.DEFAULT_OPTION,
                GameMessage.getGameIcon());
        if (result == JOptionPane.OK_OPTION) {
            processUserInput();
        }
    }

    private void updateNumberOfTiles(int width, int height) {
        int maxTiles = width * height;
        numberOfTiles.setText(TOTAL_TILES + maxTiles + BRACKET);
    }

    private void updateNumberOfTiles() {
        if (widthInput.isEditValid() && heightInput.isEditValid()) {
            try {
                updateNumberOfTiles(Integer.parseInt(widthInput.getText()), Integer.parseInt(heightInput.getText()));
            } catch (NumberFormatException exception) {
                numberOfTiles.setText(INVALID_SIZE);
            }
        } else {
            numberOfTiles.setText(INVALID_SIZE);
        }
    }

    /**
     * Parses the text input to valid integers and sends them to the settings.
     */
    private void processUserInput() {
        try {
            settings.setGridWidth(Integer.parseInt(widthInput.getText()));
            settings.setGridHeight(Integer.parseInt(heightInput.getText()));
        } catch (NumberFormatException exception) {
            GameMessage.showWarning(widthInput.getText() + CROSS + heightInput.getText() + NOT_CORRECT);
        }
    }

    /**
     * Creates a number formatter that enforces the valid minimum and maximum values for the text fields.
     */
    private NumberFormatter createNumberFormatter() {
        NumberFormatter formatter = new NumberFormatter(NumberFormat.getInstance());
        formatter.setCommitsOnValidEdit(true);
        formatter.setValueClass(Integer.class);
        formatter.setMinimum(MIN_VALUE);
        formatter.setMaximum(MAX_VALUE);
        return formatter;
    }
}

import javax.swing.table.AbstractTableModel;

import carcassonne.model.Round;
import carcassonne.model.terrain.TerrainType;

/**
 * Model class for the game statistics view. Acts as an adapter that offers the data of the round to the game statistics
 * view.
 * @author Timur Saglam
 */
public class GameStatisticsModel extends AbstractTableModel {

    private static final long serialVersionUID = -7138458001360243937L; // generated UID
    private final Round round;
    private static final String[] HEADER = {"Player", "Castle", "Road", "Monastery", "Field", "SCORE"};

    /**
     * Creates the game statistics model with the current round.
     * @param round is the current round.
     */
    public GameStatisticsModel(Round round) {
        super();
        this.round = round;
    }

    @Override
    public int getColumnCount() {
        return HEADER.length;
    }

    @Override
    public String getColumnName(int column) {
        return HEADER[column];
    }

    @Override
    public int getRowCount() {
        return round.getPlayerCount();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex == 0) {
            return round.getPlayer(rowIndex).getName();
        }
        if (columnIndex == 5) {
            return round.getPlayer(rowIndex).getScore();
        }
        return round.getPlayer(rowIndex).getTerrainScore(TerrainType.values()[columnIndex - 1]);
    }
}


import java.awt.Component;
import java.awt.Font;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Custom renderer that customizes the header of the game statistics table.
 * @author Timur Saglam
 */
public class HeaderRenderer extends DefaultTableCellRenderer {
    private static final long serialVersionUID = 7334741847662350544L; // generated UID

    /**
     * Creates the header renderer.
     */
    public HeaderRenderer() {
        setHorizontalAlignment(SwingConstants.CENTER); // Center text in cells.
    }

    /**
     * @see javax.swing.table.DefaultTableCellRenderer#getTableCellRendererComponent(javax.swing.JTable, java.lang.Object,
     * boolean, boolean, int, int)
     */
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        component.setBackground(GameStatisticsView.HEADER_COLOR);
        Font font = component.getFont();
        if (column == GameStatisticsView.SCORE_COLUMN) {
            font = font.deriveFont(Font.BOLD);
        }
        component.setFont(font.deriveFont((float) font.getSize() + 1));
        return component;
    }
}

import java.awt.Component;
import java.awt.Font;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

import carcassonne.model.Round;

/**
 * Custom renderer that customizes the cells of the game statistics table.
 * @author Timur Saglam
 */
public class CellRenderer extends DefaultTableCellRenderer {
    private static final long serialVersionUID = 1280736206678504709L; // generated UID
    private final Round round;

    /**
     * Creates the renderer.
     * @param round is the round which is depicted in the game statistics table.
     */
    CellRenderer(Round round) {
        this.round = round;
        setHorizontalAlignment(SwingConstants.CENTER); // Center text in cells.
    }

    /**
     * @see javax.swing.table.DefaultTableCellRenderer#getTableCellRendererComponent(javax.swing.JTable, java.lang.Object,
     * boolean, boolean, int, int)
     */
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (column == GameStatisticsView.SCORE_COLUMN) {
            component.setFont(component.getFont().deriveFont(Font.BOLD));
        }
        component.setForeground(round.getPlayer(row).getColor().textColor());
        component.setBackground(column == 0 ? GameStatisticsView.HEADER_COLOR : GameStatisticsView.BODY_COLOR);
        return component;
    }
}

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import carcassonne.model.terrain.TerrainType;
import carcassonne.settings.GameSettings;
import carcassonne.view.PaintShop;
import carcassonne.view.main.MainView;
import carcassonne.view.util.GameMessage;
import carcassonne.view.util.ThreadingUtil;

/**
 * Custom UI for the cosmetic player settings. Allows changing the name and the color of a player.
 * @author Timur Saglam
 */
public class PlayerEstheticsView extends JDialog implements ChangeListener, ActionListener {
    private static final int MEEPLE_PREVIEW_SIZE = 30;
    private static final long serialVersionUID = 1293883978626527260L; // generated serial UID
    private static final String CHANGE_COLOR = "Choose Meeple Color:";
    private static final String EMPTY_NAME = "The player name cannot be empty!";
    private static final String ACCEPT_CHANGES = "Accept Changes";
    private static final String CHANGE_NAME = "Choose Player Name:";
    private JColorChooser colorChooser;
    private Map<TerrainType, JLabel> labelMap;
    private final GameSettings settings;
    private final int playerNumber;
    private JTextField nameTextField;

    /**
     * Creates a new player settings UI for a specific player.
     * @param playerNumber is the number of the player.
     * @param settings are the game settings which are modified based on the user interaction with this UI.
     * @param mainView is the main user interface.
     */
    public PlayerEstheticsView(int playerNumber, GameSettings settings, MainView mainView) {
        super(mainView);
        this.playerNumber = playerNumber;
        this.settings = settings;
        setLayout(new BorderLayout());
        createNamePanel();
        createCloseButton();
        setModalityType(ModalityType.APPLICATION_MODAL);
    }

    /**
     * Updates the name and color of the correlating player and then makes the UI visible.
     */
    public void updateAndShow() {
        if (colorChooser == null) { // create JColorChooser on demand for optimal performance
            createColorChooser();
            pack(); // adapt UI size to content
            setLocationRelativeTo(null); // place UI in the center of the screen
        }
        colorChooser.setColor(settings.getPlayerColor(playerNumber));
        nameTextField.setText(settings.getPlayerName(playerNumber));
        setVisible(true);
    }

    @Override
    public void stateChanged(ChangeEvent event) {
        for (TerrainType terrain : TerrainType.basicTerrain()) {
            ThreadingUtil.runAndCallback(() -> PaintShop.getColoredMeeple(terrain, colorChooser.getColor(), MEEPLE_PREVIEW_SIZE),
                    it -> labelMap.get(terrain).setIcon(it)); // Update the preview labels.
        }
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (nameTextField.getText().isEmpty()) {
            GameMessage.showMessage(EMPTY_NAME);
        } else {
            ThreadingUtil.runAndCallback(() -> {
                settings.setPlayerName(nameTextField.getText(), playerNumber);
                settings.setPlayerColor(colorChooser.getColor(), playerNumber);
            }, () -> setVisible(false));
        }
    }

    private void createNamePanel() {
        nameTextField = new JTextField();
        JPanel namePanel = new JPanel();
        namePanel.setLayout(new BorderLayout());
        namePanel.add(nameTextField);
        namePanel.setBorder(BorderFactory.createTitledBorder(CHANGE_NAME));
        add(namePanel, BorderLayout.NORTH);
    }

    private void createColorChooser() {
        colorChooser = new JColorChooser();
        colorChooser.setBorder(BorderFactory.createTitledBorder(CHANGE_COLOR));
        colorChooser.getSelectionModel().addChangeListener(this);
        colorChooser.setPreviewPanel(createMeeplePreview());
        add(colorChooser, BorderLayout.CENTER);
    }

    private JPanel createMeeplePreview() {
        JPanel previewPanel = new JPanel();
        labelMap = new HashMap<>();
        for (TerrainType terrain : TerrainType.basicTerrain()) {
            JLabel label = new JLabel();
            labelMap.put(terrain, label);
            previewPanel.add(label);
        }
        return previewPanel;
    }

    private void createCloseButton() {
        JButton closeButton = new JButton(ACCEPT_CHANGES);
        closeButton.addActionListener(this);
        add(closeButton, BorderLayout.SOUTH);
    }
}


import javax.swing.JButton;

import carcassonne.control.ControllerFacade;
import carcassonne.model.grid.GridDirection;

/**
 * This is a simple class derived form JButton, which stores (additionally to the JButton functions) the coordinates of
 * the button on the button grid. It also uses a little hack to allow the view to set the background color of a
 * placementButton (while using Nimbus LookAndFeel), even if it is disabled. Without the hack it's not so easy to
 * accomplish that functionality.
 * @author Timur Saglam
 */
public class PlacementButton extends JButton {
    private static final long serialVersionUID = -4580099806988033224L;
    private static final String WINDOWS_10 = "Windows 10";
    private static final String OS_NAME = "os.name";
    private static final String MAC = "Mac";
    private boolean enabled; // own enabled variable for fixing the isEnabled() method.

    /**
     * Simple constructor calling the <codeJButton>JButton()</code> constructor.
     * @param controller is the controller of the view.
     * @param direction is the direction of the correlating meeple of the button on the tile.
     */
    public PlacementButton(ControllerFacade controller, GridDirection direction) {
        super();
        addMouseListener(new PlacementButtonMouseAdapter(direction, controller, this));
    }

    /**
     * Method checks whether the button is enabled or not. On MAC OS X it uses the normal JButton functionality. On other
     * systems it checks a custom variable set by the custom setEnabled method.
     * @return true if the button is enabled.
     */
    public boolean isHackyEnabled() {
        String osName = System.getProperty(OS_NAME);
        if (osName.startsWith(MAC) || WINDOWS_10.equals(osName)) {
            return isEnabled(); // normal function on mac os x
        }
        // own implementation to fix the functionality which is destroyed by the hack. If the
        // original isEnabled method is overwritten, it breaks some functionality (e.g.updating
        // the background):
        return enabled;
    }

    @Override
    public void setEnabled(boolean value) {
        String osName = System.getProperty(OS_NAME);
        if (osName.startsWith(MAC) || WINDOWS_10.equals(osName)) {
            super.setEnabled(value); // normal function on mac os x
        } else {
            // Hacky method, some variated code from the class javax.swing.AbstractButton.
            if (!value && model.isRollover()) {
                model.setRollover(false);
            }
            enabled = value; // set own enabled variable.
            repaint();
        }
    }
}


import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.SwingUtilities;

import carcassonne.control.ControllerFacade;
import carcassonne.model.grid.GridDirection;

/**
 * The class is a specific mouse adapter for a <code>MeepleButton</code>. It connects the buttons with the
 * <code>MainController</code>. The class is a subclass of <code>java.awt.event.MouseAdapter</code>.
 * @author Timur Saglam
 */
public class PlacementButtonMouseAdapter extends MouseAdapter {

    private final GridDirection direction;
    private final ControllerFacade controller;
    private final PlacementButton button;

    /**
     * Basic constructor with the button and the controller to set.
     * @param direction is the direction on a tile the adapter places meeples on.
     * @param controller sets the controller that is notified.
     * @param button is the button which uses the adapter.
     */
    public PlacementButtonMouseAdapter(GridDirection direction, ControllerFacade controller, PlacementButton button) {
        super();
        this.direction = direction;
        this.controller = controller;
        this.button = button;
    }

    /**
     * Method for processing mouse clicks on the <code>MeepleButton</code> of the class. notifies the
     * <code>MainController</code> of the class.
     * @param event is the <code>MouseEvent</code> of the click.
     */
    @Override
    public void mouseClicked(MouseEvent event) {
        if (button.isHackyEnabled() && SwingUtilities.isLeftMouseButton(event)) {
            controller.requestMeeplePlacement(direction);
        }
    }

}


import java.awt.GridBagLayout;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import carcassonne.control.ControllerFacade;
import carcassonne.control.MainController;
import carcassonne.model.Player;
import carcassonne.view.GlobalKeyBindingManager;
import carcassonne.view.NotifiableView;
import carcassonne.view.main.MainView;

/**
 * Super class for all other smaller view beneath the main view.
 * @author Timur Saglam
 */
public abstract class SecondaryView extends JDialog implements NotifiableView {
    private static final int INITIAL_X = 100;
    private static final int INITIAL_Y = 150;
    private static final long serialVersionUID = 4056347951568551115L;
    protected ControllerFacade controller;
    protected Player currentPlayer;
    protected JPanel dialogPanel;

    /**
     * Constructor for the class. Sets the controller of the view and the window title.
     * @param controller sets the {@link MainController}.
     * @param ui is the main graphical user interface.
     */
    protected SecondaryView(ControllerFacade controller, MainView ui) {
        super(ui);
        dialogPanel = new JPanel(new GridBagLayout());
        this.controller = controller;
        buildFrame();
    }

    /**
     * Adds the global key bindings to this UI.
     * @param keyBindings are the global key bindings.
     */
    public void addKeyBindings(GlobalKeyBindingManager keyBindings) {
        InputMap inputMap = dialogPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = dialogPanel.getActionMap();
        keyBindings.addKeyBindingsToMaps(inputMap, actionMap);
    }

    @Override
    public void notifyChange() {
        if (currentPlayer != null) { // only if UI is in use.
            dialogPanel.setBackground(currentPlayer.getColor().lightColor());
        }
    }

    /*
     * Builds the frame and sets its properties.
     */
    private void buildFrame() {
        getContentPane().add(dialogPanel);
        setResizable(false);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setLocation(INITIAL_X, INITIAL_Y);
    }

    /**
     * Sets the current player and updates the background color.
     * @param currentPlayer is the player who is supposed to play his turn.
     */
    protected void setCurrentPlayer(Player currentPlayer) {
        this.currentPlayer = currentPlayer;
        dialogPanel.setBackground(currentPlayer.getColor().lightColor());
        setBackground(currentPlayer.getColor().lightColor());
    }

    /**
     * Show the secondary UI and bring it to the front.
     */
    protected void showUI() {
        pack();
        setVisible(true);
        toFront(); // sets the focus on the secondary view, removes need for double clicks
    }

}


import java.awt.Color;
import java.awt.GridBagConstraints;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;

import carcassonne.control.ControllerFacade;
import carcassonne.model.Player;
import carcassonne.model.grid.GridDirection;
import carcassonne.model.terrain.TerrainType;
import carcassonne.model.tile.Tile;
import carcassonne.settings.GameSettings;
import carcassonne.util.ImageLoadingUtil;
import carcassonne.view.main.MainView;
import carcassonne.view.util.MouseClickListener;
import carcassonne.view.util.ThreadingUtil;

/**
 * A view for the placement of Meeples on the Tile that was placed previously.
 * @author Timur Saglam
 */
public class MeepleView extends SecondaryView {
    private static final long serialVersionUID = 1449264387665531286L;
    private Map<GridDirection, JButton> meepleButtons;
    private Color defaultButtonColor;
    private Tile tile;

    /**
     * Creates the view.
     * @param controller is the game controller.
     * @param ui is the main view.
     */
    public MeepleView(ControllerFacade controller, MainView ui) {
        super(controller, ui);
        buildButtonSkip();
        buildButtonGrid();
        pack();
    }

    /**
     * Sets the tile of the view, updates the view and then makes it visible. Should be called to show the view. The method
     * implements the template method pattern using the method <code>update()</code>.
     * @param tile sets the tile.
     * @param currentPlayer sets the color scheme according to the player.
     */
    public void setTile(Tile tile, Player currentPlayer) {
        if (tile == null) {
            throw new IllegalArgumentException("Tried to set the tile of the " + getClass().getSimpleName() + " to null.");
        }
        this.tile = tile;
        setCurrentPlayer(currentPlayer);
        ThreadingUtil.runAndCallback(this::updatePlacementButtons, this::showUI);
    }

    // build button grid
    private void buildButtonGrid() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridwidth = 1;
        meepleButtons = new HashMap<>();
        int index = 0;
        for (GridDirection direction : GridDirection.byRow()) {
            JButton button = new PlacementButton(controller, direction);
            button.setToolTipText("Place Meeple on the " + direction.toReadableString() + " of the tile.");
            constraints.gridx = index % 3; // from 0 to 2
            constraints.gridy = index / 3 + 1; // from 1 to 3
            dialogPanel.add(button, constraints);
            meepleButtons.put(direction, button);
            index++;
        }
    }

    private void buildButtonSkip() {
        JButton buttonSkip = new JButton(ImageLoadingUtil.SKIP.createHighDpiImageIcon());
        buttonSkip.setToolTipText("Don't place meeple and preserve for later use");
        defaultButtonColor = buttonSkip.getBackground();
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridwidth = 3;
        dialogPanel.add(buttonSkip, constraints);
        buttonSkip.addMouseListener((MouseClickListener) event -> controller.requestSkip());
    }

    /**
     * Updates the meepleButtons to reflect the current placement options.
     */
    private void updatePlacementButtons() {
        for (GridDirection direction : GridDirection.values()) {
            TerrainType terrain = tile.getTerrain(direction);
            boolean placeable = tile.hasMeepleSpot(direction) && controller.getSettings().getMeepleRule(terrain);
            JButton button = meepleButtons.get(direction);
            if (placeable) {
                button.setIcon(ImageLoadingUtil.createHighDpiImageIcon(GameSettings.getMeeplePath(terrain, false)));
            } else {
                button.setIcon(ImageLoadingUtil.createHighDpiImageIcon(GameSettings.getMeeplePath(TerrainType.OTHER, false)));
            }
            if (placeable && tile.allowsPlacingMeeple(direction, currentPlayer, controller.getSettings())) {
                button.setEnabled(true);
                button.setBackground(defaultButtonColor);
            } else {
                button.setEnabled(false);
                button.setBackground(currentPlayer.getColor().lightColor());
            }
        }
    }
}

import java.awt.GridBagConstraints;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.border.Border;

import carcassonne.control.ControllerFacade;
import carcassonne.model.Player;
import carcassonne.model.tile.Tile;
import carcassonne.model.tile.TileType;
import carcassonne.settings.GameSettings;
import carcassonne.util.ImageLoadingUtil;
import carcassonne.view.main.MainView;
import carcassonne.view.util.MouseClickListener;
import carcassonne.view.util.ThreadingUtil;

/**
 * view class for the tile orientation. It lets the user look at the tile to place and rotate it both right and left.
 * @author Timur Saglam
 */
public class TileView extends SecondaryView {
    private static final long serialVersionUID = -5179683977081970564L;
    private static final int BOTTOM_SPACE = 5;
    private static final int VERTICAL_SPACE = 10;
    private static final double SELECTION_FACTOR = 0.9;
    private static final int SELECTION_BORDER_WIDTH = 3;
    private static final String NO_TILE_TO_SELECT = "has no Tile to select!";
    private static final String TOOL_TIP = "Tile of type ";
    private final int selectionSize;
    private final int defaultSize;
    private JButton buttonRotateLeft;
    private JButton buttonRotateRight;
    private JButton buttonSkip;
    private List<JLabel> tileLabels;
    private List<Tile> tiles;
    private int selectionIndex;

    /**
     * Simple constructor which uses the constructor of the <code>Smallview</code>.
     * @param controller is the game controller.
     * @param ui is the main view.
     */
    public TileView(ControllerFacade controller, MainView ui) {
        super(controller, ui);
        buildContent();
        pack();
        selectionSize = dialogPanel.getWidth() - VERTICAL_SPACE;
        defaultSize = (int) (selectionSize * SELECTION_FACTOR);
    }

    /**
     * Returns the tile correlating to the selected tile label.
     * @return the tile.
     */
    public Tile getSelectedTile() {
        if (selectionIndex < tiles.size()) {
            return tiles.get(selectionIndex);
        }
        throw new IllegalStateException(getClass().getSimpleName() + NO_TILE_TO_SELECT);
    }

    /**
     * If the UI is active, rotates the tile to the left.
     */
    public void rotateLeft() {
        if (isVisible() && selectionIndex < tiles.size()) {
            tiles.get(selectionIndex).rotateLeft();
            updateTileLabel(selectionIndex);
        }
    }

    /**
     * If the UI is active, rotates the tile to the right.
     */
    public void rotateRight() {
        if (isVisible() && selectionIndex < tiles.size()) {
            tiles.get(selectionIndex).rotateRight();
            updateTileLabel(selectionIndex);
        }
    }

    /**
     * Selects the next tile label above the current selected one.
     */
    public void selectAbove() {
        if (selectionIndex > 0) {
            selectTileLabel(selectionIndex - 1);
        }
    }

    /**
     * Selects the next tile label below the current selected one.
     */
    public void selectBelow() {
        if (selectionIndex + 1 < tiles.size()) {
            selectTileLabel(selectionIndex + 1);
        }

    }

    /**
     * Sets the tiles of the view to the tiles of the current player, updates the view and then makes it visible. Should be
     * called to show the view.
     * @param currentPlayer is the active player.
     */
    public void setTiles(Player currentPlayer) {
        tiles.clear();
        if (!currentPlayer.getHandOfTiles().isEmpty()) {
            tiles.addAll(currentPlayer.getHandOfTiles());
            setCurrentPlayer(currentPlayer);
            ThreadingUtil.runAndCallback(this::updatePreviewLabels, this::showUI);
        }
    }

    @Override
    public void notifyChange() {
        super.notifyChange();
        if (!tiles.isEmpty()) {
            updateTileLabel(selectionIndex);
        }
    }

    /**
     * Selects a specific tile label, increasing its size and adding a border. Resets the previous selection.
     * @param index is the index of the label. If the index is not valid nothing will happen.
     */
    private void selectTileLabel(int index) {
        if (index < tiles.size()) {
            int oldSelection = selectionIndex;
            selectionIndex = index;
            updateTileLabel(index);
            updateTileLabel(oldSelection);
        }
    }

    // build the view content
    private void buildContent() {
        // create buttons:
        buttonSkip = new JButton(ImageLoadingUtil.SKIP.createHighDpiImageIcon());
        buttonRotateLeft = new JButton(ImageLoadingUtil.LEFT.createHighDpiImageIcon());
        buttonRotateRight = new JButton(ImageLoadingUtil.RIGHT.createHighDpiImageIcon());
        // set tool tips:
        buttonSkip.setToolTipText("Don't place tile and skip turn");
        buttonRotateLeft.setToolTipText("Rotate left");
        buttonRotateRight.setToolTipText("Rotate right");
        // set listeners:
        buttonSkip.addMouseListener((MouseClickListener) event -> controller.requestSkip());
        buttonRotateLeft.addMouseListener((MouseClickListener) event -> rotateLeft());
        buttonRotateRight.addMouseListener((MouseClickListener) event -> rotateRight());
        // set constraints:
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.VERTICAL;
        constraints.weightx = 1; // keeps buttons evenly positioned.
        // add buttons:
        dialogPanel.add(buttonRotateLeft, constraints);
        dialogPanel.add(buttonSkip, constraints);
        dialogPanel.add(buttonRotateRight, constraints);
        // change constraints and add label:
        constraints.gridy = 1;
        constraints.gridwidth = 3;
        ImageIcon defaultImage = new Tile(TileType.Null).getScaledIcon(50);
        tileLabels = new ArrayList<>();
        tiles = new ArrayList<>();
        for (int i = 0; i < GameSettings.MAXIMAL_TILES_ON_HAND; i++) {
            JLabel label = new JLabel(defaultImage);
            tileLabels.add(label);
            constraints.gridy++;
            final int index = i;
            label.addMouseListener((MouseClickListener) event -> selectTileLabel(index));
            dialogPanel.add(label, constraints);
        }
        constraints.gridy++;
        dialogPanel.add(Box.createVerticalStrut(BOTTOM_SPACE), constraints);
    }

    // Updates the image of a specific tile label.
    private void updateTileLabel(int index) {
        boolean singleTile = tiles.size() == 1;
        boolean selected = index == selectionIndex; // is the label selected or not?
        ImageIcon icon = tiles.get(index).getScaledIcon(selected || singleTile ? selectionSize : defaultSize);
        tileLabels.get(index).setToolTipText(TOOL_TIP + tiles.get(index).getType().readableRepresentation());
        tileLabels.get(index).setIcon(icon);
        tileLabels.get(index).setBorder(selected && !singleTile ? createSelectionBorder() : null);
    }

    // Resets the selection index and adapts the tile labels to the given amount of tiles.
    private void updatePreviewLabels() {
        selectionIndex = 0;
        for (int i = tiles.size(); i < GameSettings.MAXIMAL_TILES_ON_HAND; i++) {
            tileLabels.get(i).setVisible(false);
        }
        for (int i = 0; i < tiles.size(); i++) {
            tileLabels.get(i).setVisible(true);
            updateTileLabel(i);
        }
        pack();
    }

    // Creates the selection border. The color is always up to date.
    private Border createSelectionBorder() {
        return BorderFactory.createLineBorder(currentPlayer.getColor().textColor(), SELECTION_BORDER_WIDTH);
    }
}

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * Utility interface that allows declaring functional mouse click listeners. By providing default methods for the other
 * method signatures defined by {@link MouseListener}, this becomes a functional interface. This interface can be used
 * like this: <code>addMouseListener((MouseClickListener)(e)->doSomething())</code>
 * @author Timur Saglam
 */
public interface MouseClickListener extends MouseListener {

    @Override
    default void mousePressed(MouseEvent event) {
        // do nothing as only clicks are listened to.
    }

    @Override
    default void mouseClicked(MouseEvent event) {
        // do nothing as only clicks are listened to.
    }

    @Override
    default void mouseEntered(MouseEvent event) {
        // do nothing as only clicks are listened to.
    }

    @Override
    default void mouseExited(MouseEvent event) {
        // do nothing as only clicks are listened to.
    }
}


import java.awt.Dimension;

import javax.swing.Box.Filler;

/**
 * Creates an invisible component that's always of a specified size but still allows changing that size manually.
 * @author Timur Saglam
 */
public class RigidRectangle extends Filler {

    private static final long serialVersionUID = 8847635761705081422L;

    /**
     * Creates a rigid rectangle.
     * @param dimension specifies the width and height of the rectangle.
     */
    public RigidRectangle(Dimension dimension) {
        super(dimension, dimension, dimension);
    }

    /**
     * Creates a rigid square.
     * @param sideLength is the square.
     */
    public RigidRectangle(int sideLength) {
        this(new Dimension(sideLength, sideLength));
    }

    /**
     * Changes the fixed size of the rectangle.
     * @param dimension specifies the width and height of the rectangle.
     */
    public void changeShape(Dimension dimension) {
        changeShape(dimension, dimension, dimension);
    }

    /**
     * Changes the fixed size of the rectangle.
     * @param sideLength is the square.
     */
    public void changeShape(int sideLength) {
        changeShape(new Dimension(sideLength, sideLength));
    }
}


import javax.swing.JScrollPane;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import carcassonne.view.main.LayeredScrollPane;

/**
 * Utility class for functionality regarding the {@link LookAndFeel} and the {@link UIManager}.
 * @author Timur Saglam
 */
public final class LookAndFeelUtil {
    private static final String WINDOWS = "Windows";
    private static final String OS_NAME_PROPERTY = "os.name";

    private LookAndFeelUtil() {
        // private constructor ensures non-instantiability!
    }

    /**
     * Creates a modified {@link JScrollPane} that uses the default {@link LookAndFeel} instead of the current one (e.g.
     * Nimbus). If the operating system is not Windows, a normal {@link JScrollPane} is created, as the Nimbus scroll bars
     * are not deeply broken on decent operating systems.
     * @return the modified {@link JScrollPane} that always has good looking scroll bars.
     */
    public static LayeredScrollPane createModifiedScrollpane() {
        if (System.getProperty(OS_NAME_PROPERTY).startsWith(WINDOWS)) {
            return createScrollpaneWithFixedScrollbars();
        }
        return new LayeredScrollPane();
    }

    /*
     * Switches the LookAndFeel to the default one, creates a JScrollPane with that LookAndFeel and then switches back to
     * the original LookAndFeel.
     */
    private static LayeredScrollPane createScrollpaneWithFixedScrollbars() {
        LayeredScrollPane modifiedScrollPane = null;
        try {
            LookAndFeel previousLF = UIManager.getLookAndFeel();
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            modifiedScrollPane = new LayeredScrollPane();
            UIManager.setLookAndFeel(previousLF);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException exception) {
            exception.printStackTrace();
        }
        return modifiedScrollPane;
    }
}


import javax.swing.Icon;
import javax.swing.JOptionPane;

import carcassonne.model.tile.Tile;
import carcassonne.model.tile.TileType;
import carcassonne.util.ImageLoadingUtil;

/**
 * Message class for showing the user small messages.
 * @author Timur Saglam
 */
public final class GameMessage {
    private static final String ABOUT = """
            The board game Carcassonne is created by Klaus-Jürgen Wrede and published by Hans im Glück.
            This computer game based on the board game is developed by Timur Sağlam.""";

    private static final int GAME_ICON_SIZE = 75;
    private static final String TITLE = "Carcassonne";

    private GameMessage() {
        // private constructor ensures non-instantiability!
    }

    /**
     * Shows a custom error message.
     * @param messageText is the message text.
     */
    public static void showError(String messageText) {
        show(messageText, JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Shows a custom plain message.
     * @param messageText is the message text.
     */
    public static void showMessage(String messageText) {
        show(messageText, JOptionPane.DEFAULT_OPTION);
    }

    /**
     * Shows a custom warning message.
     * @param messageText is the message text.
     */
    public static void showWarning(String messageText) {
        show(messageText, JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Creates a game icon for dialogs.
     * @return the game {@link Icon}.
     */
    public static Icon getGameIcon() {
        return new Tile(TileType.Null).getScaledIcon(GAME_ICON_SIZE);
    }

    public static void showGameInfo() {
        JOptionPane.showMessageDialog(null, ABOUT, TITLE, JOptionPane.DEFAULT_OPTION, ImageLoadingUtil.SPLASH.createHighDpiImageIcon());
    }

    private static void show(String messageText, int type) {
        JOptionPane.showMessageDialog(null, messageText, TITLE, type, getGameIcon());
    }
}


import java.awt.Dimension;

import javax.swing.JLabel;

public class FixedSizeLabel extends JLabel {

    private static final long serialVersionUID = 2548111454775629470L;
    private final int fixedWidth;

    public FixedSizeLabel(String text, int fixedWidth) {
        super(text);
        this.fixedWidth = fixedWidth;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(fixedWidth, super.getPreferredSize().height);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(fixedWidth, super.getMinimumSize().height);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(fixedWidth, super.getMaximumSize().height);
    }

}


import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import javax.swing.SwingWorker;

/**
 * Utility class for threading with AWT/Swing user interfaces.
 * @author Timur Saglam
 */
public final class ThreadingUtil {
    private ThreadingUtil() {
        // private constructor ensures non-instantiability!
    }

    /**
     * Helps view classes to execute a task in the background where there is no result to be returned.
     * @param task is the task to execute.
     */
    public static void runInBackground(Runnable task) {
        SwingWorker<?, ?> worker = new SwingWorker<Boolean, Object>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                task.run();
                return true;
            }
        };
        worker.execute();
    }

    /**
     * Helps view classes to execute a task in the background and call-back on completion.
     * @param <T> is the return type of the task.
     * @param task is the task to be executed.
     * @param callback is the callback with the result on completion.
     */
    public static <T> void runAndCallback(Callable<T> task, Consumer<T> callback) {
        SwingWorker<T, ?> worker = new SwingWorker<>() {
            @Override
            protected T doInBackground() throws Exception {
                return task.call();
            }

            @Override
            protected void done() {
                try {
                    callback.accept(get());
                } catch (InterruptedException | ExecutionException exception) {
                    exception.printStackTrace();
                    GameMessage.showError(exception.getCause().getMessage());
                }
            }
        };
        worker.execute();
    }

    /**
     * Helps view classes to execute a task in the background and call-back on completion.
     * @param task is the task to be executed.
     * @param callback is the callback on completion.
     */
    public static void runAndCallback(Runnable task, Runnable callback) {
        runAndCallback(() -> {
            task.run();
            return true; // no return value required, true as default
        }, it -> callback.run());
    }
}


import java.awt.Component;
import java.awt.Rectangle;
import java.util.stream.Stream;

import javax.swing.JLayeredPane;
import javax.swing.JScrollPane;
import javax.swing.OverlayLayout;

/**
 * {@link JScrollPane} that depicts the layers of a {@link JLayeredPane}. It is just a scroll pane that manages its own
 * layered pane.
 * @author Timur Saglam
 */
public class LayeredScrollPane extends JScrollPane {
    private static final long serialVersionUID = 7863596860273426396L;
    private static final int SCROLL_SPEED = 15;
    private final JLayeredPane layeredPane;

    /**
     * Creates a layered scroll pane and centers it for a certain grid size.
     */
    public LayeredScrollPane() {
        layeredPane = new JLayeredPane();
        layeredPane.setLayout(new OverlayLayout(layeredPane));
        setViewportView(layeredPane);
        getVerticalScrollBar().setUnitIncrement(SCROLL_SPEED);
        getHorizontalScrollBar().setUnitIncrement(SCROLL_SPEED);
    }

    /**
     * Adds any number of components as layers in the given order.
     * @param components the components to add as layers.
     */
    public void addLayers(Component... components) {
        for (int i = 0; i < components.length; i++) {
            layeredPane.add(components[i], i);
        }
    }

    /**
     * Adds a zoom listener that listens to the mouse wheel and then calls one of two zoom functions.
     * @param zoomIn the function to zoom in.
     * @param zoomOut the function to zoom out.
     */
    public void addZoomListener(Runnable zoomIn, Runnable zoomOut) {
        layeredPane.addMouseWheelListener(new MouseWheelZoomListener(zoomIn, zoomOut, layeredPane.getParent()));
    }

    /**
     * Removes any number of layers if they are part of this scroll pane.
     * @param components are the components and therefore layers to remove.
     */
    public void removeLayers(Component... components) {
        Stream.of(components).forEach(layeredPane::remove);
    }

    /**
     * Repaint the layered pane and its layers.
     */
    public void repaintLayers() {
        layeredPane.repaint();
    }

    /**
     * Centers the scroll pane view to show the center of the grid. Since this method revalidates the viewport it can be
     * expensive if the scroll pane contains complex content (e.g. a very large grid).
     */
    public void validateAndCenter() { // TODO (HIGH) [ZOOMING] zoom based on view center and not grid center?
        validate(); // IMPORTANT: required to allow proper centering
        centerScrollBars(getHorizontalScrollBar().getMaximum(), getVerticalScrollBar().getMaximum());
    }

    /**
     * Centers the scroll bars by using the grid dimensions.
     * @param width is the width of the tile grid in pixels.
     * @param height is the height of the tile grid in pixels.
     */
    private void centerScrollBars(int width, int height) {
        Rectangle view = getViewport().getViewRect();
        getHorizontalScrollBar().setValue(Math.max(0, (width - view.width) / 2));
        getVerticalScrollBar().setValue(Math.max(0, (height - view.height) / 2));
    }
}


import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;

import carcassonne.control.ControllerFacade;
import carcassonne.model.Player;
import carcassonne.model.grid.GridDirection;
import carcassonne.model.terrain.TerrainType;
import carcassonne.model.tile.Tile;
import carcassonne.view.util.RigidRectangle;

/**
 * A UI component that contains multiple meeple depiction panels.
 * @author Timur Saglam
 */
public class MeepleLayer extends JPanel {
    private static final long serialVersionUID = -843137441362337953L;
    private final List<MeepleDepictionPanel> meeplePanels;
    private final MeepleDepictionPanel[][] meeplePanelGrid;
    private final JComponent[][] placeholderGrid;
    private final List<RigidRectangle> placeholders;
    private int zoomLevel;
    private final transient ControllerFacade controller;

    /**
     * Creates the meeple layer.
     * @param controller is the main controller.
     * @param gridWidth is the width of the grid in tiles.
     * @param gridHeight is the height of the grid in tile.
     * @param zoomLevel is the zoom level, and therefore the tile size.
     */
    public MeepleLayer(ControllerFacade controller, int gridWidth, int gridHeight, int zoomLevel) {
        this.controller = controller;
        this.zoomLevel = zoomLevel;
        setOpaque(false);
        setLayout(new GridBagLayout());
        synchronizeLayerSizes(gridWidth, gridHeight, zoomLevel);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.weightx = 1; // evenly distributes meeple grid
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.BOTH;
        meeplePanels = new ArrayList<>();
        meeplePanelGrid = new MeepleDepictionPanel[gridWidth][gridHeight]; // build array of labels.
        placeholders = new ArrayList<>();
        placeholderGrid = new JComponent[gridWidth][gridHeight];
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                constraints.gridx = x;
                constraints.gridy = y;
                RigidRectangle rectangle = new RigidRectangle(zoomLevel);
                placeholderGrid[x][y] = rectangle;
                placeholders.add(rectangle);
                add(rectangle, constraints);
            }
        }
    }

    private void initializeLazily(int x, int y) {
        if (meeplePanelGrid[x][y] == null) {
            remove(placeholderGrid[x][y]);
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.weightx = 1; // evenly distributes meeple grid
            constraints.weighty = 1;
            constraints.fill = GridBagConstraints.BOTH;
            constraints.gridx = x;
            constraints.gridy = y;
            meeplePanelGrid[x][y] = new MeepleDepictionPanel(zoomLevel, controller);
            meeplePanels.add(meeplePanelGrid[x][y]);
            add(meeplePanelGrid[x][y], constraints); // add label with constraints
            validate();
        }
    }

    /**
     * Adapts the meeples in this layer to a new zoom level.
     * @param zoomLevel is the new zoom level.
     */
    public void changeZoomLevel(int zoomLevel) {
        this.zoomLevel = zoomLevel;
        placeholders.stream().forEach(it -> it.changeShape(zoomLevel));
        meeplePanels.forEach(it -> it.setSize(zoomLevel));
    }

    /**
     * Enables the meeple preview on a specific panel correlating to a tile.
     * @param x is the x-coordinate of that panel.
     * @param y is the y-coordinate of that panel.
     * @param tile is the correlating tile.
     * @param currentPlayer is the player who is currently active.
     */
    public void enableMeeplePreview(int x, int y, Tile tile, Player currentPlayer) {
        initializeLazily(x, y);
        meeplePanelGrid[x][y].setMeeplePreview(tile, currentPlayer);
    }

    /**
     * Places a meeple on a panel (correlating to a tile) on a specific position.
     * @param x is the x-coordinate of that panel.
     * @param y is the y-coordinate of that panel.
     * @param terrain determines the meeple type.
     * @param position is the position where the meeple is placed on the panel.
     * @param owner is the player who owns the meeple.
     */
    public void placeMeeple(int x, int y, TerrainType terrain, GridDirection position, Player owner) {
        initializeLazily(x, y);
        meeplePanelGrid[x][y].placeMeeple(terrain, position, owner);
    }

    /**
     * Refreshes all meeple labels in this layer. This updates the images to color changes.
     */
    public void refreshLayer() {
        meeplePanels.stream().forEach(MeepleDepictionPanel::refreshAll);
    }

    /**
     * Resets all meeples in the layer.
     */
    public void resetLayer() {
        meeplePanels.stream().forEach(MeepleDepictionPanel::resetAll);
    }

    /**
     * Resets all meeple in a specific panel correlating to a tile.
     * @param x is the x-coordinate of that panel.
     * @param y is the y-coordinate of that panel.
     */
    public void resetPanel(int x, int y) {
        meeplePanelGrid[x][y].resetAll();
    }

    /**
     * Adapts the size of this layer to the tile grid.
     * @param gridWidth is the width of the grid in tiles.
     * @param gridHeight is the height of the grid in tile.
     * @param zoomLevel is the zoom level, and therefore the tile size.
     */
    public void synchronizeLayerSizes(int gridWidth, int gridHeight, int zoomLevel) {
        Dimension layerSize = new Dimension(gridWidth * zoomLevel, gridHeight * zoomLevel);
        setMaximumSize(layerSize);
        setPreferredSize(layerSize);
        setMinimumSize(layerSize);
    }

}


/**
 * Enumeration for the three zoom modes. Determines the image scaling quality and what images are scaled.
 * @author Timur Saglam
 */
public enum ZoomMode {
    /**
     * Smooth image scaling, takes longer.
     */
    SMOOTH,

    /**
     * Fast image scaling, but with highlight scaling.
     */
    FAST,
}


import static carcassonne.view.main.ZoomMode.FAST;
import static carcassonne.view.main.ZoomMode.SMOOTH;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.ActionMap;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

import carcassonne.control.ControllerFacade;
import carcassonne.model.Player;
import carcassonne.model.grid.GridDirection;
import carcassonne.model.tile.Tile;
import carcassonne.view.GlobalKeyBindingManager;
import carcassonne.view.NotifiableView;
import carcassonne.view.PaintShop;
import carcassonne.view.menubar.MainMenuBar;
import carcassonne.view.menubar.Scoreboard;
import carcassonne.view.util.LookAndFeelUtil;

/**
 * The main user interface, showing the grid and the menu bar.
 * @author Timur Saglam
 */
public class MainView extends JFrame implements NotifiableView {
    private static final long serialVersionUID = 5684446992452298030L; // generated UID

    // ZOOM CONSTANTS:
    private static final int DEFAULT_ZOOM_LEVEL = 125;
    private static final int MAX_ZOOM_LEVEL = 300;
    private static final int MIN_ZOOM_LEVEL = 25;
    private static final int ZOOM_STEP_LARGE = 25;
    private static final int ZOOM_STEP_SMALL = 5;

    // UI CONSTANTS:
    private static final Dimension MINIMAL_WINDOW_SIZE = new Dimension(640, 480);

    // FIELDS:
    private final ControllerFacade controller;
    private Player currentPlayer;
    private int gridHeight;
    private int gridWidth;
    private MeepleLayer meepleLayer;
    private TileLayer tileLayer;
    private MainMenuBar menuBar;
    private LayeredScrollPane scrollPane;
    private int zoomLevel;

    /**
     * Constructor of the main view. creates the view, menu bar, and scoreboard.
     * @param controller sets the connection to the game controller.
     */
    public MainView(ControllerFacade controller) {
        this.controller = controller;
        gridWidth = controller.getSettings().getGridWidth();
        gridHeight = controller.getSettings().getGridHeight();
        zoomLevel = DEFAULT_ZOOM_LEVEL;
        buildFrame();
    }

    /**
     * Adds the global key bindings to this UI.
     * @param keyBindings are the global key bindings.
     */
    public void addKeyBindings(GlobalKeyBindingManager keyBindings) {
        InputMap inputMap = scrollPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = scrollPane.getActionMap();
        keyBindings.addKeyBindingsToMaps(inputMap, actionMap);
    }

    /**
     * Resets the tile grid and the meeple grid to return to the initial state.
     */
    public void resetGrid() {
        meepleLayer.resetLayer();
        tileLayer.resetLayer();
        validate(); // might not be required all the time but is fast so it does not matter
    }

    /**
     * Rebuilds the grid to adapt it to a changed grid size. Therefore, the grid size is updated before rebuilding.
     */
    public void rebuildGrid() {
        gridWidth = controller.getSettings().getGridWidth();
        gridHeight = controller.getSettings().getGridHeight();
        scrollPane.removeLayers(meepleLayer, tileLayer);
        tileLayer = new TileLayer(controller, gridHeight, gridWidth, zoomLevel);
        meepleLayer = new MeepleLayer(controller, gridWidth, gridHeight, zoomLevel);
        scrollPane.addLayers(meepleLayer, tileLayer);
        scrollPane.validateAndCenter();
    }

    /**
     * Removes meeple on a tile on the grid.
     */
    public void removeMeeple(int x, int y) {
        checkCoordinates(x, y);
        meepleLayer.resetPanel(x, y);
    }

    /**
     * Resets the meeple preview on one specific {@link Tile}.
     * @param tile is the specific {@link Tile}.
     */
    public void resetMeeplePreview(Tile tile) {
        checkParameters(tile);
        int x = tile.getGridSpot().getX();
        int y = tile.getGridSpot().getY();
        checkCoordinates(x, y);
        meepleLayer.resetPanel(x, y);
    }

    /**
     * Resets the state of the menu to allows restarting.
     */
    public void resetMenuState() {
        menuBar.enableStart(); // TODO (MEDIUM) [UI] Find better solution.
    }

    /**
     * Shows the UI and centers the scrollpane view.
     */
    public void showUI() {
        setVisible(true);
        scrollPane.validateAndCenter();
    }

    /**
     * Returns the controller with which this UI is communicating.
     * @return the controller instance.
     */
    public ControllerFacade getController() {
        return controller;
    }

    /**
     * Grants access to the scoreboard.
     * @return the scoreboard.
     */
    public Scoreboard getScoreboard() {
        return menuBar.getScoreboard(); // TODO (MEDIUM) [UI] Find better solution.
    }

    /**
     * Grants access to the current zoom level of the UI.
     * @return the zoom level.
     */
    public synchronized int getZoom() {
        return zoomLevel;
    }

    /**
     * Zooms in if the maximum zoom level has not been reached.
     * @param mode determines the zoom mode, which affects image quality and performance.
     * @param updateHightlights determines if the highlight tiles are also scaled.
     */
    public synchronized void zoomIn(ZoomMode mode) {
        if (zoomLevel < MAX_ZOOM_LEVEL) {
            zoomLevel += mode == FAST ? ZOOM_STEP_SMALL : ZOOM_STEP_LARGE;
            updateToChangedZoomLevel(mode);
            menuBar.getZoomSlider().setValueSneakily(zoomLevel); // ensures the slider is updated when using key bindings.
        }
    }

    /**
     * Zooms out if the minimum zoom level has not been reached.
     * @param updateHightlights determines if the highlight tiles are also scaled.
     */
    public synchronized void zoomOut(ZoomMode mode) {
        if (zoomLevel > MIN_ZOOM_LEVEL) {
            zoomLevel -= mode == FAST ? ZOOM_STEP_SMALL : ZOOM_STEP_LARGE;
            updateToChangedZoomLevel(mode);
            menuBar.getZoomSlider().setValueSneakily(zoomLevel); // ensures the slider is updated when using key bindings.
        }
    }

    /**
     * Updates the zoom level if it is in the valid range.
     * @param level is the zoom level.
     * @param mode determines the zoom mode, which affects image quality and performance.
     */
    public synchronized void setZoom(int level, ZoomMode mode) {
        if (level <= MAX_ZOOM_LEVEL && level >= MIN_ZOOM_LEVEL) {
            zoomLevel = level;
            updateToChangedZoomLevel(mode);
        }
    }

    /**
     * Updates the view to a changed zoom level. Centers the view.
     * @param mode determines the zoom mode, which affects image quality and performance.
     */
    public void updateToChangedZoomLevel(ZoomMode mode) {
        if (currentPlayer != null && mode == SMOOTH) { // only update highlights when there is an active round
            tileLayer.refreshHighlight(PaintShop.getColoredHighlight(currentPlayer, zoomLevel, mode == FAST));
        } else {
            tileLayer.resetPlacementHighlights();
        }
        tileLayer.changeZoomLevel(zoomLevel, mode == FAST); // Executed in parallel for improved performance
        meepleLayer.synchronizeLayerSizes(gridWidth, gridHeight, zoomLevel); // IMPORTANT: Ensures that the meeples are on the tiles.
        meepleLayer.changeZoomLevel(zoomLevel);
        scrollPane.validateAndCenter();
        scrollPane.repaintLayers(); // IMPORTANT: Prevents meeples from disappearing.
    }

    /**
     * Notifies the the main view about a (new) current player. This allows the UI to adapt color schemes to the player.
     * @param currentPlayer is the current {@link Player}.
     */
    public void setCurrentPlayer(Player currentPlayer) {
        if (this.currentPlayer == null) {
            scrollPane.validateAndCenter(); // ensures centered view on game start for heterogenous multi-monitor setups
        }
        this.currentPlayer = currentPlayer;
        ImageIcon newHighlight = PaintShop.getColoredHighlight(currentPlayer, zoomLevel, false);
        tileLayer.refreshHighlight(newHighlight);
    }

    /**
     * Highlights a position on the grid to indicate that the tile is a possible placement spot.
     * @param x is the x coordinate.
     * @param y is the y coordinate.
     */
    public void setSelectionHighlight(int x, int y) {
        checkCoordinates(x, y);
        tileLayer.highlightTile(x, y);
    }

    /**
     * Highlights a position on the grid to indicate that an AI player recently placed the tile.
     * @param x is the x coordinate.
     * @param y is the y coordinate.
     */
    public void setPlacementHighlight(int x, int y) {
        checkCoordinates(x, y);
        tileLayer.highlightTile(x, y, currentPlayer);
        scrollPane.repaintLayers();
    }

    /**
     * Highlights a position on the grid to indicate that an AI player recently placed the tile.
     * @param x is the x coordinate.
     * @param y is the y coordinate.
     */
    public void resetPlacementHighlights() {
        tileLayer.resetPlacementHighlights();
        scrollPane.repaintLayers();
    }

    /**
     * Draws meeple on a tile on the grid.
     * @param tile is the tile where the meeple gets drawn.
     * @param position is the position on the tile where the meeple gets drawn.
     * @param owner is the player that owns the meeple.
     */
    public void setMeeple(Tile tile, GridDirection position, Player owner) {
        checkParameters(tile, position, owner);
        int x = tile.getGridSpot().getX();
        int y = tile.getGridSpot().getY();
        checkCoordinates(x, y);
        meepleLayer.placeMeeple(x, y, tile.getTerrain(position), position, owner);
        scrollPane.repaintLayers(); // This is required!
    }

    /**
     * Enables the meeple preview on one specific {@link Tile}.
     * @param tile is the specific {@link Tile}.
     * @param currentPlayer determines the color of the preview.
     */
    public void setMeeplePreview(Tile tile, Player currentPlayer) {
        checkParameters(tile, currentPlayer);
        int x = tile.getGridSpot().getX();
        int y = tile.getGridSpot().getY();
        checkCoordinates(x, y);
        meepleLayer.enableMeeplePreview(x, y, tile, currentPlayer);
        scrollPane.repaintLayers(); // This is required! Removing this will paint black background.
    }

    /**
     * Draws the tile on a specific position on the view.
     * @param tile is the tile.
     * @param x is the x coordinate.
     * @param y is the y coordinate.
     */
    public void setTile(Tile tile, int x, int y) {
        checkParameters(tile);
        checkCoordinates(x, y);
        tileLayer.placeTile(tile, x, y);
    }

    /**
     * Refreshes the meeple labels to get the new colors.
     */
    @Override
    public void notifyChange() {
        if (currentPlayer != null) {
            setCurrentPlayer(currentPlayer);
            tileLayer.refreshPlacementHighlights();
        }
        meepleLayer.refreshLayer();
    }

    private void buildFrame() {
        tileLayer = new TileLayer(controller, gridHeight, gridWidth, zoomLevel);
        meepleLayer = new MeepleLayer(controller, gridWidth, gridHeight, zoomLevel);
        menuBar = new MainMenuBar(controller, this);
        setJMenuBar(menuBar);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        scrollPane = LookAndFeelUtil.createModifiedScrollpane();
        scrollPane.addLayers(meepleLayer, tileLayer);
        scrollPane.addZoomListener(() -> zoomIn(FAST), () -> zoomOut(FAST));
        add(scrollPane, BorderLayout.CENTER);
        setMinimumSize(MINIMAL_WINDOW_SIZE);
        addWindowListener(new WindowMaximizationAdapter(this));
        pack();
    }

    private void checkCoordinates(int x, int y) {
        if (x < 0 && x >= gridWidth || y < 0 && y >= gridHeight) {
            throw new IllegalArgumentException("Invalid label grid position (" + x + ", " + y + ")");
        }
    }

    private void checkParameters(Object... parameters) {
        for (Object parameter : parameters) {
            if (parameter == null) {
                throw new IllegalArgumentException("Parameters such as Tile, Meeple, and Player cannot be null!");
            }
        }
    }
}

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import carcassonne.control.ControllerFacade;
import carcassonne.model.Player;
import carcassonne.model.tile.Tile;
import carcassonne.model.tile.TileType;
import carcassonne.view.PaintShop;

/**
 * Is a simple class derived form JLabel, which stores (additionally to the JLabel functions) the coordinates of the
 * label on the label grid.
 * @author Timur Saglam
 */
public class TileDepiction {
    private Tile tile;
    private final Tile defaultTile;
    private final Tile highlightTile;
    private final JLabel label;
    private ImageIcon coloredHighlight;
    private int tileSize;
    private Player recentlyPlaced;

    /**
     * Simple constructor calling the <codeJLabel>JLabel(ImageIcon image)</code> constructor.
     * @param tileSize is the initial edge length of the tile according to the zoom level.
     * @param defaultTile is the tile that determines the default look.
     * @param highlightTile is the tile that determines the highlight look.
     * @param controller is the controller of the view.
     * @param x sets the x coordinate.
     * @param y sets the y coordinate.
     */
    public TileDepiction(int tileSize, Tile defaultTile, Tile highlightTile, ControllerFacade controller, int x, int y) {
        this.defaultTile = defaultTile;
        this.highlightTile = highlightTile;
        this.tileSize = tileSize;
        label = new JLabel();
        reset();
        label.addMouseListener(new MouseAdapter() {
            /**
             * Method for processing mouse clicks on the <code>TileImage</code> of the class. notifies the
             * <code>MainController</code> of the class.
             * @param e is the <code>MouseEvent</code> of the click.
             */
            @Override
            public void mouseClicked(MouseEvent event) {
                controller.requestTilePlacement(x, y);
            }

            @Override
            public void mouseEntered(MouseEvent event) {
                if (highlightTile.equals(tile)) {
                    label.setIcon(coloredHighlight);
                }
            }

            @Override
            public void mouseExited(MouseEvent event) {
                if (highlightTile.equals(tile)) {
                    setTile(highlightTile);
                }
            }
        });
    }

    /**
     * Shows a {@link Tile} image on this label.
     * @param tile is the {@link Tile} that provides the image.
     */
    public final void setTile(Tile tile) {
        this.tile = tile;
        if (recentlyPlaced == null) {
            label.setIcon(tile.getScaledIcon(tileSize));
        } else {
            label.setIcon(PaintShop.getColoredTile(tile, recentlyPlaced, tileSize, false));
        }
    }

    /**
     * Adapts the size of the tile to make the tile label larger or smaller.
     * @param tileSize is the new tile size in pixels.
     * @param preview determines if the size adjustment is part of the preview or final.
     */
    public void setTileSize(int tileSize, boolean preview) {
        this.tileSize = tileSize;
        if (recentlyPlaced == null) {
            label.setIcon(tile.getScaledIcon(tileSize, preview));
        } else {
            label.setIcon(PaintShop.getColoredTile(tile, recentlyPlaced, tileSize, preview));
        }
    }

    /**
     * Sets a colored mouseover highlight.
     * @param coloredHighlight is the {@link ImageIcon} depicting the highlight.
     */
    public void setColoredHighlight(ImageIcon coloredHighlight) {
        this.coloredHighlight = coloredHighlight;
    }

    /**
     * Enables the colored mouseover highlight.
     */
    public void highlightSelection() {
        setTile(highlightTile);
    }

    /**
     * Enables the colored highlight that indicated the recent placement.
     * @param player is this player that placed the tile.
     */
    public void highlightPlacement(Player player) {
        recentlyPlaced = player;
        setTile(tile);
    }

    /**
     * Resets the colored highlight that indicated the recent placement.
     */
    public void resetPlacementHighlight() {
        recentlyPlaced = null;
        setTile(tile);
    }

    /**
     * Disables the colored mouseover highlight and sets this tile to the default tile.
     */
    public final void refresh() {
        setTile(tile);
    }

    /**
     * Disables the colored mouseover highlight and sets this tile to the default tile.
     */
    public final void reset() {
        if (tile != defaultTile) {
            recentlyPlaced = null;
            setTile(defaultTile);
        }
    }

    /**
     * Grants access to the {@link JLabel} of this label.
     * @return the tile {@link JLabel}.
     */
    public JLabel getLabel() {
        return label;
    }

    /**
     * Returns the currently depicted tile.
     * @return the tile, which can be of {@link TileType#Null}.
     */
    public Tile getTile() {
        return tile;
    }
}


import java.awt.Component;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

/**
 * Extended {@link MouseWheelListener} that calls one of two zoom functions on a mouse wheel event with CTRL or META
 * pressed. All other events are delegated to a dedicated parent component.
 * @author Timur Saglam
 */
public class MouseWheelZoomListener implements MouseWheelListener {
    private static final int ZOOM_IN_THRESHOLD = -2;
    private static final int ZOOM_OUT_THRESHOLD = 2;
    private final Runnable zoomIn;
    private final Runnable zoomOut;
    private final Component parent;
    private double scrollProgress;

    /**
     * Creates the listener.
     * @param zoomIn is the zoom-in function to call on a fitting mouse wheel event.
     * @param zoomOut is the zoom-out function to call on a fitting mouse wheel event.
     * @param parent is the dedicated parent component where non-zoom events are delegated to.
     */
    public MouseWheelZoomListener(Runnable zoomIn, Runnable zoomOut, Component parent) {
        super();
        this.zoomIn = zoomIn;
        this.zoomOut = zoomOut;
        this.parent = parent;
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent event) { // TODO (HIGH) [PERFORMANCE] block here if busy
        if (event.isControlDown() || event.isMetaDown()) {
            scrollProgress += event.getPreciseWheelRotation() * event.getScrollAmount(); // works for touchpads and mouse wheels
            if (scrollProgress >= ZOOM_OUT_THRESHOLD) {
                zoomOut.run();
                scrollProgress = 0;
            } else if (scrollProgress <= ZOOM_IN_THRESHOLD) {
                zoomIn.run();
                scrollProgress = 0;
            }
        } else {
            parent.dispatchEvent(event); // delegate event to parent to prevent normal scroll actions.
        }
    }
}


import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;

import carcassonne.control.ControllerFacade;
import carcassonne.control.MainController;
import carcassonne.model.Player;
import carcassonne.model.grid.GridDirection;
import carcassonne.model.terrain.TerrainType;
import carcassonne.model.tile.Tile;

/**
 * {@link JPanel} encapsulates the the nine {@link MeepleDepiction}s of a specific {@link Tile}.
 * @author Timur Saglam
 */
public class MeepleDepictionPanel extends JPanel {

    private static final int GRID_INDEX_OFFSET = 1;
    private static final long serialVersionUID = -1475325065701922699L;
    private final ControllerFacade controller;
    private final Map<GridDirection, MeepleDepiction> labels;
    private Dimension size;

    /**
     * Creates the meeple panel.
     * @param scalingFactor is the initial scaling factor.
     * @param controller is the responsible {@link MainController}.
     */
    public MeepleDepictionPanel(int scalingFactor, ControllerFacade controller) {
        this.controller = controller;
        labels = new HashMap<>(GridDirection.values().length);
        setOpaque(false);
        setLayout(new GridBagLayout());
        size = new Dimension(scalingFactor, scalingFactor);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.weightx = 1; // evenly distributes meeple grid
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.BOTH;
        for (GridDirection direction : GridDirection.values()) {
            MeepleDepiction meepleLabel = new MeepleDepiction(scalingFactor, controller, direction);
            constraints.gridx = direction.getX() + GRID_INDEX_OFFSET;
            constraints.gridy = direction.getY() + GRID_INDEX_OFFSET;
            labels.put(direction, meepleLabel);
            add(meepleLabel.getLabel(), constraints);
        }
    }

    /**
     * Places a meeple in a specific position on this panel.
     * @param terrain specifies the meeple type.
     * @param position is the specific position where the meeple is placed, correlating to the position on the tile.
     * @param owner is the player that owns the meeple.
     */
    public void placeMeeple(TerrainType terrain, GridDirection position, Player owner) {
        labels.get(position).setIcon(terrain, owner);
    }

    /**
     * Enables the meeple preview on one all meeples of a specific {@link Tile}.
     * @param tile is the specific {@link Tile}.
     * @param currentPlayer determines the color of the preview.
     */
    public void setMeeplePreview(Tile tile, Player currentPlayer) {
        for (GridDirection direction : GridDirection.values()) {
            TerrainType terrain = tile.getTerrain(direction);
            if (tile.hasMeepleSpot(direction) && tile.allowsPlacingMeeple(direction, currentPlayer, controller.getSettings())
                    && controller.getSettings().getMeepleRule(terrain)) {
                labels.get(direction).setPreview(tile.getTerrain(direction), currentPlayer);
            }
        }
    }

    /**
     * Updates the size of the panel with a scaling factor.
     * @param scalingFactor is the scaling factor in pixels.
     */
    public void setSize(int scalingFactor) {
        size = new Dimension(scalingFactor, scalingFactor);
        labels.values().forEach(it -> it.setMeepleSize(scalingFactor));
    }

    /**
     * Refreshes all meeple labels in this panel. This updates the images to color changes.
     */
    public void refreshAll() {
        labels.values().forEach(MeepleDepiction::refresh);
    }

    /**
     * Resets all meeples in this panel.
     */
    public void resetAll() {
        labels.values().forEach(MeepleDepiction::reset);
    }

    @Override
    public Dimension getPreferredSize() {
        return size;
    }

    @Override
    public Dimension getMaximumSize() {
        return size;
    }
}


import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

import carcassonne.control.ControllerFacade;
import carcassonne.model.Player;
import carcassonne.model.tile.Tile;
import carcassonne.model.tile.TileType;
import carcassonne.settings.GameSettings;

/**
 * A UI component that contains multiple tile panels.
 * @author Timur Saglam
 */
public class TileLayer extends JPanel {
    private static final long serialVersionUID = 1503933201337556131L;
    private final List<TileDepiction> placementHighlights;
    private final transient List<TileDepiction> tileLabels;
    private final transient TileDepiction[][] tileDepictionGrid;

    /**
     * Creates the tile layer.
     * @param controller is the main controller.
     * @param gridWidth is the width of the grid in tiles.
     * @param gridHeight is the height of the grid in tile.
     * @param zoomLevel is the zoom level, and therefore the tile size.
     */
    public TileLayer(ControllerFacade controller, int gridHeight, int gridWidth, int zoomLevel) {
        setBackground(GameSettings.UI_COLOR);
        setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        tileLabels = new ArrayList<>();
        placementHighlights = new ArrayList<>();
        tileDepictionGrid = new TileDepiction[gridWidth][gridHeight]; // build array of labels.
        Tile defaultTile = new Tile(TileType.Null);
        Tile highlightTile = new Tile(TileType.Null);
        defaultTile.rotateRight();
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                tileDepictionGrid[x][y] = new TileDepiction(zoomLevel, defaultTile, highlightTile, controller, x, y);
                tileLabels.add(tileDepictionGrid[x][y]);
                constraints.gridx = x;
                constraints.gridy = y;
                add(tileDepictionGrid[x][y].getLabel(), constraints); // add label with constraints
            }
        }
    }

    /**
     * Adapts the layer to a new zoom level.
     * @param zoomLevel is the zoom level, and therefore also the tile size.
     * @param preview determines if the tiles are rendered in preview mode (fast but ugly).
     */
    public void changeZoomLevel(int zoomLevel, boolean preview) {
        tileLabels.parallelStream().forEach(it -> it.getTile().getScaledIcon(zoomLevel)); // pre-load scaled images
        tileLabels.stream().forEach(it -> it.setTileSize(zoomLevel, preview));
    }

    /**
     * Highlights a specific tile.
     * @param x is the x-coordinate of that tile.
     * @param y is the y-coordinate of that tile.
     */
    public void highlightTile(int x, int y) {
        tileDepictionGrid[x][y].highlightSelection();
    }

    /**
     * Highlights a specific tile.
     * @param x is the x-coordinate of that tile.
     * @param y is the y-coordinate of that tile.
     */
    public void highlightTile(int x, int y, Player player) {
        placementHighlights.add(tileDepictionGrid[x][y]);
        tileDepictionGrid[x][y].highlightPlacement(player);
    }

    /**
     * Places a tile, updating the correlating tile label.
     * @param tile is the tile to place.
     * @param x is the x-coordinate of that tile.
     * @param y is the y-coordinate of that tile.
     */
    public void placeTile(Tile tile, int x, int y) {
        tileDepictionGrid[x][y].setTile(tile);
    }

    /**
     * Refreshes the highlight image.
     * @param newHighlight is the new image.
     */
    public void refreshHighlight(ImageIcon newHighlight) {
        tileLabels.stream().forEach(it -> it.setColoredHighlight(newHighlight));
    }

    /**
     * Refreshes the placement highlights.
     * @param newHighlight is the new image.
     */
    public void refreshPlacementHighlights() {
        placementHighlights.forEach(TileDepiction::refresh);
    }

    public void resetPlacementHighlights() {
        placementHighlights.forEach(TileDepiction::resetPlacementHighlight);
        placementHighlights.clear();
    }

    /**
     * Resets every tile label in this layer.
     */
    public void resetLayer() {
        tileLabels.stream().forEach(TileDepiction::reset);
    }
}


import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import carcassonne.control.ControllerFacade;
import carcassonne.control.MainController;
import carcassonne.model.Player;
import carcassonne.model.grid.GridDirection;
import carcassonne.model.terrain.TerrainType;
import carcassonne.view.PaintShop;

/**
 * Special {@link JLabel} for showing meeples.
 * @author Timur Saglam
 */
public class MeepleDepiction {
    private static final int MEEPLE_SCALING_THRESHOLD = 100;
    private static final int INITIAL_MEEPLE_SIZE = 25;
    private Player player;
    private final MouseAdapter mouseAdapter;
    private TerrainType terrain;
    private final JLabel label;
    private boolean preview;
    private int meepleSize;

    /**
     * Creates a blank meeple label.
     * @param controller is the {@link MainController} of the game.
     * @param direction is the {@link GridDirection} where the meeple label sits on the tile.
     */
    public MeepleDepiction(int scalingFactor, ControllerFacade controller, GridDirection direction) {
        updateMeepleSize(scalingFactor);
        terrain = TerrainType.OTHER;
        label = new JLabel(PaintShop.getPreviewMeeple(terrain, meepleSize));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        preview = false;
        mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (SwingUtilities.isLeftMouseButton(event)) {
                    controller.requestMeeplePlacement(direction);
                }
            }

            @Override
            public void mouseEntered(MouseEvent event) {
                setMeepleIcon();
            }

            @Override
            public void mouseExited(MouseEvent event) {
                setPreviewIcon();
            }
        };
    }

    /**
     * Refreshes its icon by getting the newest image from the {@link PaintShop}.
     */
    public void refresh() {
        if (terrain != TerrainType.OTHER && !preview) {
            setMeepleIcon();
        }
    }

    /**
     * Resets the label, which means it displays nothing.
     */
    public void reset() {
        terrain = TerrainType.OTHER;
        setPreviewIcon();
        label.removeMouseListener(mouseAdapter);
    }

    /**
     * Sets the icon of the meeple label according to the {@link Player} and terrain type.
     * @param terrain is the terrain type and affects the meeple type.
     * @param player is the {@link Player}, which affects the color.
     */
    public void setIcon(TerrainType terrain, Player player) {
        this.terrain = terrain;
        this.player = player;
        preview = false;
        refresh();
    }

    /**
     * Sets the specific {@link TerrainType} as meeple placement preview, which means a transparent image of the correlating
     * meeple.
     * @param terrain is the specific {@link TerrainType}.
     * @param player is the {@link Player} who is currently active.
     */
    public void setPreview(TerrainType terrain, Player player) {
        this.terrain = terrain;
        this.player = player;
        preview = true;
        label.addMouseListener(mouseAdapter);
        setPreviewIcon();
    }

    /**
     * Grants access to the {@link JLabel} itself.
     * @return the {@link JLabel}
     */
    public JLabel getLabel() {
        return label;
    }

    /**
     * Specifies the size of the meeple and therefore the meeple label.
     * @param scalingFactor the new size in percent. Might be limited by the MEEPLE_SCALING_THRESHOLD. Set to 100 for the
     * original size.
     */
    public void setMeepleSize(int scalingFactor) {
        updateMeepleSize(scalingFactor);
        if (terrain == TerrainType.OTHER || preview) {
            setPreviewIcon();
        } else {
            setMeepleIcon();
        }
    }

    // calculates the meeple size from the scaling factor, which also represents the tile size.
    private final void updateMeepleSize(int scalingFactor) {
        int limitedFactor = scalingFactor > MEEPLE_SCALING_THRESHOLD ? MEEPLE_SCALING_THRESHOLD : scalingFactor;
        meepleSize = INITIAL_MEEPLE_SIZE * limitedFactor / 100;
    }

    private void setMeepleIcon() {
        label.setIcon(PaintShop.getColoredMeeple(terrain, player, meepleSize));
    }

    private void setPreviewIcon() { // empty icon TileTerrain.OTHER
        label.setIcon(PaintShop.getPreviewMeeple(terrain, meepleSize));
    }
}


import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

/**
 * {@link WindowAdapter} that fixes an issue with proper window maximization on Windows 10. When adding this adapter to
 * a {@link JFrame} it ensures its proper maximization (once) on activation. Just another little hack to fix Windows
 * issues.
 * @author Timur Saglam
 */
public class WindowMaximizationAdapter extends WindowAdapter {
    private final JFrame frame;
    private boolean maximized;

    /**
     * Creates the adapter for a specific {@link JFrame}
     * @param frame is the specific {@link JFrame} to be maximized.
     */
    public WindowMaximizationAdapter(JFrame frame) {
        this.frame = frame;
        maximized = false;
    }

    @Override
    public void windowActivated(WindowEvent event) {
        if (!maximized) {
            maximized = true;
            frame.setExtendedState(frame.getExtendedState() | Frame.MAXIMIZED_BOTH);
        }

    }
}

