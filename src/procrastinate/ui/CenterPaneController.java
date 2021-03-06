//@@author A0121597B
package procrastinate.ui;

import java.util.List;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import procrastinate.task.Task;
import procrastinate.ui.UI.ScreenView;

/**
 * <h1>This class controls the StackPane in the main window's BorderPane center region.</h1>
 *
 * It is also in-charge of all the different screens and overlays, including the:
 * <li>     Creation of all the different screens/overlays
 * <li>     Showing/Hiding of overlays
 * <li>     Switching of screens
 * <li>     Updating of screens
 * <li>     Transitions that occur in the center region of the window
 *
 */
public class CenterPaneController {

    // ================================================================================
    // Message Strings
    // ================================================================================

    private static final String MESSAGE_UNABLE_RECOGNISE_SCREEN_TYPE = "Unable to recognise ScreenType";

    private static final String SELECTOR_SCROLL_BAR = ".scroll-bar";
    private static final String SELECTOR_SCROLL_PANE = "#scrollPane";

    private static final String TRANSITION_CUE_POINT_END = "end";

    // ================================================================================
    // Animation Values
    // ================================================================================

    private static final double OPACITY_FULL = 1;
    private static final double OPACITY_ZERO = 0;

    // Time values are in milliseconds
    private static final double TIME_HELP_SCREEN_FADEIN = 150;
    private static final double TIME_HELP_SCREEN_FADEOUT = 200;

    private static final double TIME_SPLASH_SCREEN_FADE = 4000;
    private static final double TIME_SPLASH_SCREEN_FULL_OPACITY = 3000;
    private static final double TIME_SPLASH_SCREEN_INTERRUPT = 2700;

    // ================================================================================
    // Class Variables
    // ================================================================================

    private static double xOffset_, yOffset_;

    private CenterScreen currentScreen_;

    private ImageOverlay currentOverlay_;

    private Timeline splashScreenTimeline_;

    private Node mainScreenNode_;
    private Node mainAllScreenNode_;
    private Node doneScreenNode_;
    private Node searchScreenNode_;
    private Node summaryScreenNode_;

    private Node helpOverlayNode_;
    private Node splashOverlayNode_;

    private DoneScreen doneScreen_;
    private MainScreen mainScreen_;
    private MainScreen mainAllScreen_;
    private SearchScreen searchScreen_;
    private SummaryScreen summaryScreen_;

    private HelpOverlay helpOverlay_;
    private SplashOverlay splashOverlay_;

    private StackPane centerStackPane_;

    // ================================================================================
    // CenterPaneController Constructor
    // ================================================================================

    protected CenterPaneController(StackPane centerStackPane) {
        assert(centerStackPane != null);
        this.centerStackPane_ = centerStackPane;
        createScreens();
        createOverlays();
        setToSummaryScreen();
    }

    // ================================================================================
    // CenterScreen Methods
    // ================================================================================

    /**
     * Switches the current screen in the StackPane that is set within the
     * center region of the BorderPane of the main window.
     *
     * @param taskList    that contains the tasks related to each screen
     * @param screenView  corresponding to the screen to switch to upon update
     */
    protected void updateScreen(List<Task> taskList, ScreenView screenView) {
        assert(taskList != null);
        updateSummaryAndMainScreens(taskList, screenView);

        switch (screenView) {
            case SCREEN_DONE : {
                if (currentScreen_ != doneScreen_) {
                    startScreenSwitchSequence(doneScreenNode_, doneScreen_);
                }

                doneScreen_.updateTaskList(taskList);
                break;
            }

            case SCREEN_MAIN : {
                if (currentScreen_ != mainScreen_) {
                    startScreenSwitchSequence(mainScreenNode_, mainScreen_);
                }

                mainScreen_.updateTaskList(taskList);
                break;
            }

            case SCREEN_MAIN_ALL : {
                if (currentScreen_ != mainAllScreen_) {
                    startScreenSwitchSequence(mainAllScreenNode_, mainAllScreen_);
                }

                mainAllScreen_.updateTaskList(taskList);
                break;
            }

            case SCREEN_SEARCH : {
                if (currentScreen_ != searchScreen_) {
                    startScreenSwitchSequence(searchScreenNode_, searchScreen_);
                }

                searchScreen_.updateTaskList(taskList);
                break;
            }

            case SCREEN_SUMMARY : {
                if (currentScreen_ != summaryScreen_) {
                    // Special exception for summary screen, which requires the
                    // entire screen to be loaded before summarising can start.
                    switchToSummaryScreen();
                }

                summaryScreen_.updateTaskList(taskList);

                if (!summaryScreen_.isSummarising()) {
                    if (!centerStackPane_.getChildren().contains(splashOverlayNode_)) {
                        startScreenSwitchSequenceNoAnimation(mainScreenNode_, mainScreen_);
                    }
                }
                break;
            }

            default:
                System.out.println(MESSAGE_UNABLE_RECOGNISE_SCREEN_TYPE);
                break;
        }
    }

    /**
     * Used to keep both the Summary and Main screens up to date with one another.
     *
     * @param taskList     to update the screen with
     * @param screenView   must correspond to currentScreen and be either SCREEN_MAIN
     *                     or SCREEN_SUMMARY for any updates to take place.
     */
    private void updateSummaryAndMainScreens(List<Task> taskList, ScreenView screenView) {
        if (currentScreen_ == mainScreen_ && screenView == ScreenView.SCREEN_MAIN) {
            summaryScreen_.updateTaskList(taskList);
        } else if (currentScreen_ == summaryScreen_ && screenView == ScreenView.SCREEN_SUMMARY) {
            mainScreen_.updateTaskList(taskList);
            mainAllScreen_.updateTaskList(taskList);
        }
    }

    // Used at startup so that highlighting can start immediately from the first very operation
    protected void initialUpdateMainScreen(List<Task> taskList) {
        mainScreen_.updateTaskList(taskList);
        mainAllScreen_.updateTaskList(taskList);
    }

    // Handle to pass search string between classes
    protected void receiveSearchStringAndPassToSearchScreen(String searchString) {
        searchScreen_.updateSearchHeaderLabelText(searchString);
    }

    // Methods below for scrolling current screen with key input. Scroll bar
    // value is incremented/decremented twice to enable the user scroll faster
    protected void scrollDownCurrentScreen() {
        ScrollPane currScrollPane = ((ScrollPane) (currentScreen_.getNode().lookup(SELECTOR_SCROLL_PANE)));

        ScrollBar currScrollBar = (ScrollBar) currScrollPane.lookup(SELECTOR_SCROLL_BAR);
        currScrollBar.increment();
        currScrollBar.increment();
    }

    protected void scrollUpCurrentScreen() {
        ScrollPane currScrollPane = ((ScrollPane) (currentScreen_.getNode().lookup(SELECTOR_SCROLL_PANE)));

        ScrollBar currScrollBar = (ScrollBar) currScrollPane.lookup(SELECTOR_SCROLL_BAR);
        currScrollBar.decrement();
        currScrollBar.decrement();
    }

    // ================================================================================
    // ImageOverlay Methods
    // ================================================================================

    // A handle to help switch between pages of the HelpOverlay if it is
    // currently being shown.
    protected void showNextHelpPage() {
        if (currentOverlay_ != helpOverlay_) {
            return;
        }
        helpOverlay_.nextPage();
    }

    /**
     * Starts the fade out transition that lasts for TIME_HELP_SCREEN_FADEOUT
     * seconds if the stack contains it and it is the current overlay screen.
     * Each call will create a new FadeTransition to be used for fading the
     * overlay out.
     */
    protected void hideHelpOverlay() {
        if (currentOverlay_ != helpOverlay_ || !centerStackPane_.getChildren().contains(helpOverlayNode_)) {
            return;
        }

        FadeTransition helpOverlayFadeOut = getFadeOutTransition(TIME_HELP_SCREEN_FADEOUT, helpOverlayNode_);
        helpOverlayFadeOut.setOnFinished(e -> {
            centerStackPane_.getChildren().remove(helpOverlayNode_);
            currentOverlay_ = null;
        });
        helpOverlayFadeOut.play();
    }

    /**
     * Fast-forwards the fade animation if user starts typing before TIME_SPLASH_SCREEN_INTERRUPT.
     * The splash screen is automatically removed from the centerStackPane once it has finished
     * playing.
     */
    protected void hideSplashOverlay() {
        if (currentOverlay_ == splashOverlay_ && centerStackPane_.getChildren().contains(splashOverlayNode_)) {
            Duration interruptTime = Duration.millis(TIME_SPLASH_SCREEN_INTERRUPT);
            // Only fast forward the timeline if the current time of the
            // animation is smaller than the given interrupt time. Else, just
            // wait for the animation to end.
            if (splashScreenTimeline_.getCurrentTime().lessThan(interruptTime)) {
                splashScreenTimeline_.jumpTo(Duration.millis(TIME_SPLASH_SCREEN_INTERRUPT));
            }

            splashScreenTimeline_.jumpTo(Duration.millis(TIME_SPLASH_SCREEN_FADE));
        }
    }

    /**
     * Shows the HelpOverlay only if the HelpOverlay is not present. Each call
     * creates a new FadeTransition to be used for fading the overlay in.
     */
    protected void showHelpOverlay() {
        if (currentOverlay_ == helpOverlay_ || centerStackPane_.getChildren().contains(helpOverlay_)) {
            return;
        }

        currentOverlay_ = helpOverlay_;
        centerStackPane_.getChildren().add(helpOverlayNode_);
        helpOverlayNode_.toFront();

        FadeTransition helpOverlayFadeIn = getFadeInTransition(TIME_HELP_SCREEN_FADEIN, helpOverlayNode_);
        helpOverlayFadeIn.play();
    }

    /**
     * Shows the SplashOverlay which is only used at start-up. The main
     * animation of the overlay is contained within the
     * buildSplashScreenAnimation method.
     */
    protected void showSplashOverlay() {
        currentOverlay_ = splashOverlay_;
        centerStackPane_.getChildren().add(splashOverlayNode_);

        buildSplashScreenAnimation();
        splashScreenTimeline_.play();
    }

    // ================================================================================
    // Transition Methods
    // ================================================================================

    /**
     * Creates a splash screen that maintains full opacity for
     * TIME_SPLASH_SCREEN_FULL_OPACITY seconds before completely fading out in
     * (TIME_SPLASH_SCREEN_FADE-TIME_SPLASH_SCREEN_FULL_OPACITY) seconds or
     * until the user starts to type.
     */
    private void buildSplashScreenAnimation() {
        Duration fullOpacityDuration = Duration.millis(TIME_SPLASH_SCREEN_FULL_OPACITY);
        KeyValue fullOpacityKeyValue = new KeyValue(splashOverlayNode_.opacityProperty(), OPACITY_FULL);
        KeyFrame fullOpacityFrame = new KeyFrame(fullOpacityDuration, fullOpacityKeyValue);

        Duration zeroOpacityDuration = Duration.millis(TIME_SPLASH_SCREEN_FADE);
        KeyValue zeroOpacityKeyValue = new KeyValue(splashOverlayNode_.opacityProperty(), OPACITY_ZERO);
        KeyFrame zeroOpacityFrame = new KeyFrame(zeroOpacityDuration, zeroOpacityKeyValue);

        splashScreenTimeline_ = new Timeline(fullOpacityFrame, zeroOpacityFrame);
        splashScreenTimeline_.setOnFinished(e -> {
                                              centerStackPane_.getChildren().remove(splashOverlayNode_);
                                              currentOverlay_ = null;
                                              if (!summaryScreen_.isSummarising()) {
                                                  startScreenSwitchSequenceNoAnimation(mainScreenNode_, mainScreen_);
                                              }
        });
    }

    private FadeTransition getFadeOutTransition(double timeInMs, Node transitingNode) {
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(timeInMs), transitingNode);

        fadeTransition.setFromValue(OPACITY_FULL);
        fadeTransition.setToValue(OPACITY_ZERO);
        fadeTransition.setInterpolator(Interpolator.EASE_OUT);

        return fadeTransition;
    }

    private FadeTransition getFadeInTransition(double timeInMs, Node transitingNode) {
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(timeInMs), transitingNode);

        fadeTransition.setFromValue(OPACITY_ZERO);
        fadeTransition.setToValue(OPACITY_FULL);
        fadeTransition.setInterpolator(Interpolator.EASE_IN);

        return fadeTransition;
    }

    /**
     * Combines the screen changing transitions of the outgoing and incoming
     * screens by playing them in sequence.
     *
     * @param nodeToSwitchIn    must be the corresponding Node of the CenterScreen passed in.
     * @param screenToSwitchIn  the CenterScreen to be switched in and should not be contained
     *                          within the centerStackPane.
     */
    private void startScreenSwitchSequence(Node nodeToSwitchIn, CenterScreen screenToSwitchIn) {
        ParallelTransition incomingScreenTransition = screenToSwitchIn.getScreenSwitchInSequence();
        incomingScreenTransition.setOnFinished(incoming -> currentScreen_ = screenToSwitchIn);

        SequentialTransition outgoingScreenTransition = currentScreen_.getScreenSwitchOutSequence();
        outgoingScreenTransition.setOnFinished(outgoing -> {
            centerStackPane_.getChildren().remove(currentScreen_.getNode());
            centerStackPane_.getChildren().add(nodeToSwitchIn);
            incomingScreenTransition.play();
        });
        outgoingScreenTransition.play();
    }

    private void startScreenSwitchSequenceNoAnimation(Node nodeToSwitchIn, CenterScreen screenToSwitchIn) {
        ParallelTransition incomingScreenTransition = screenToSwitchIn.getScreenSwitchInSequence();
        incomingScreenTransition.setOnFinished(incoming -> currentScreen_ = screenToSwitchIn);

        centerStackPane_.getChildren().remove(currentScreen_.getNode());
        centerStackPane_.getChildren().add(nodeToSwitchIn);

        incomingScreenTransition.jumpTo(TRANSITION_CUE_POINT_END);
        incomingScreenTransition.play();
    }


    // Exception case for switching to SummaryScreen, which wouldn't show
    // correctly if the screen switch transition of the outgoing screen is
    // played together.
    private void switchToSummaryScreen() {
        centerStackPane_.getChildren().add(summaryScreenNode_);
        centerStackPane_.getChildren().remove(currentScreen_.getNode());

        summaryScreen_.getScreenSwitchInSequence().play();

        currentScreen_ = summaryScreen_;
    }

    // ================================================================================
    // Init Methods
    // ================================================================================

    private void createOverlays() {
        createHelpOverlay();
        createSplashOverlay();
    }

    private void createScreens() {
        createMainScreen();
        createMainAllScreen();
        createDoneScreen();
        createSearchScreen();
        createSummaryScreen();
    }

    private void createHelpOverlay() {
        this.helpOverlay_ = new HelpOverlay();
        this.helpOverlayNode_ = helpOverlay_.getNode();
    }

    private void createSplashOverlay() {
        this.splashOverlay_ = new SplashOverlay();
        this.splashOverlayNode_ = splashOverlay_.getNode();
    }

    private void createMainScreen() {
        this.mainScreen_ = new MainScreen();
        this.mainScreenNode_ = mainScreen_.getNode();
        addMouseDragListeners(mainScreenNode_);
    }

    private void createMainAllScreen() {
        this.mainAllScreen_ = new MainScreen();
        this.mainAllScreenNode_ = mainAllScreen_.getNode();
        addMouseDragListeners(mainAllScreenNode_);
    }

    private void createDoneScreen() {
        this.doneScreen_ = new DoneScreen();
        this.doneScreenNode_ = doneScreen_.getNode();
        addMouseDragListeners(doneScreenNode_);
    }

    private void createSearchScreen() {
        this.searchScreen_ = new SearchScreen();
        this.searchScreenNode_ = searchScreen_.getNode();
        addMouseDragListeners(searchScreenNode_);
    }

    private void createSummaryScreen() {
        this.summaryScreen_ = new SummaryScreen();
        this.summaryScreenNode_ = summaryScreen_.getNode();
        addMouseDragListeners(summaryScreenNode_);
    }

    private void setToSummaryScreen() {
        centerStackPane_.getChildren().add(summaryScreenNode_);
        currentScreen_ = summaryScreen_;
    }

    // @@author A0121597B-reused
    // Required since each screen node is wrapped inside a scrollPane.
    private void addMouseDragListeners(Node screenNode) {
        Node scrollPaneNode = ((ScrollPane) screenNode.lookup(SELECTOR_SCROLL_PANE)).getContent();

        scrollPaneNode.setOnMousePressed((mouseEvent) -> {
            xOffset_ = mouseEvent.getSceneX();
            yOffset_ = mouseEvent.getSceneY();
        });

        scrollPaneNode.setOnMouseDragged((mouseEvent) -> {
            centerStackPane_.getScene().getWindow().setX(mouseEvent.getScreenX() - xOffset_);
            centerStackPane_.getScene().getWindow().setY(mouseEvent.getScreenY() - yOffset_);
        });
    }

    // ================================================================================
    // Getter Methods
    // ================================================================================

    //@@author generated
    protected Node getDoneScreenNode() {
        return doneScreenNode_;
    }

    protected Node getMainScreenNode() {
        return mainScreenNode_;
    }

    protected Node getSearchScreenNode() {
        return searchScreenNode_;
    }

    protected Node getSummaryScreenNode() {
        return summaryScreenNode_;
    }

    protected Node getHelpOverlayNode() {
        return helpOverlayNode_;
    }

    protected Node getSplashOverlayNode() {
        return splashOverlayNode_;
    }

    protected CenterScreen getCurrentScreen() {
        return currentScreen_;
    }

    protected ImageOverlay getCurrentOverlay() {
        return currentOverlay_;
    }

}
