package procrastinate.ui;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.SequentialTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.converter.NumberStringConverter;
import procrastinate.task.Deadline;
import procrastinate.task.Event;
import procrastinate.task.Task;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CenterScreen extends VBox {

    // ================================================================================
    // Message strings
    // ================================================================================

    private static final String CATEGORY_OVERDUE = "Overdue";
    private static final String CATEGORY_THIS_WEEK = "This Week";
    private static final String CATEGORY_FUTURE = "Future";
    private static final String CATEGORY_DREAMS = "Dreams";
    private static final String CATEGORY_DONE = "Done";

    private static final String SUBCATEGORY_TODAY = "Today";
    private static final String SUBCATEGORY_TOMORROW = "Tomorrow";

    private static final String LOCATION_EMPTY_VIEW = "images/no-tasks.png";

    private static final String MESSAGE_UNABLE_TO_DETERMINE_TYPE = "Unable to determine TaskType for adding.";
    private static final String MESSAGE_UNABLE_TO_RECOGNISE_NODE = "Unable to recognise Node.";

    private static final String EVENT_DATE_SEPARATOR_GENERAL = "\nto ";
    private static final String EVENT_DATE_SEPARATOR_SAME_DAY = " to ";
    private static final String TIME_SEPARATOR = " ";
    private static final String UI_NUMBER_SEPARATOR = ". ";

    private static final String SELECTOR_CATEGORY_VBOX = "#categoryVBox";

    // Time values used are in milliseconds
    private static final int TIME_TRANSITION_CATEGORY_FADE_IN = 250;
    private static final int TIME_TRANSITION_CATEGORY_FADE_OUT = 200;
    private static final int TIME_TRANSITION_SUBCATEGORY_FADE_IN = 200;
    private static final int TIME_TRANSITION_SUBCATEGORY_FADE_OUT = 150;

    private static final double OPACITY_ZERO = 0;
    private static final double OPACITY_FULL = 1;

    private static final String FX_BACKGROUND_IMAGE_NULL = "-fx-background-image: null;";
    private static String FX_BACKGROUND_IMAGE_NO_TASKS; // will be initialised later on.

    // ================================================================================
    // Class variables
    // ================================================================================

    private Node node;

    // Nodes are used to add them onto the screen
    private Node overdueNode;
    private Node thisWeekNode;
    private Node futureNode;
    private Node dreamsNode;
    private Node doneNode;

    private int[] subcategoryVisibilityTracker; // used to determine if the subcategory is to be faded in or out.

    private ArrayList<Node> nodeList = new ArrayList<>();
    private ArrayList<VBox> thisWeekSubcategories = new ArrayList<>();

    // The main variables to call when adding tasks since they act as a task list for a TaskEntry to be displayed
    private VBox overdueTaskList;
    private VBox thisWeekTaskList;
    private VBox futureTaskList;
    private VBox dreamsTaskList;
    private VBox doneTaskList;

    private IntegerProperty taskCount = new SimpleIntegerProperty(1);
    private StringProperty taskCountFormatted = new SimpleStringProperty();
    private StringProperty taskCountString = new SimpleStringProperty();

    private SimpleDateFormat dateFormatWithYear = new SimpleDateFormat("d MMM''yy h:mma");
    private SimpleDateFormat dateFormat = new SimpleDateFormat("d MMM");
    private SimpleDateFormat timeFormat = new SimpleDateFormat("h:mma");
    private SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy");

    private Date today;
    private Date currentDate;
    private Date endOfWeek;

    // ================================================================================
    // FXML field variables
    // ================================================================================

    @FXML private VBox mainVBox;

    // ================================================================================
    // CenterScreen methods
    // ================================================================================

    protected CenterScreen(String filePath) {
        loadLayout(filePath);
        createCategories();
        setupBinding();
    }

    /**
     * The list of tasks displayed is updated by removing all previously added tasks and re-adding them back to allow
     * the line number to be sorted by category and not insertion time.
     *
     * Dreams are directly added via this method but Deadlines and Events are passed to two different
     * addTask methods depending on their (start) dates.
     * @param taskList List of Tasks to be added onto the screen
     */
    protected void updateTaskList(List<Task> taskList) {
        updateDates();
        clearTaskList();

        Date taskDate;
        boolean isSameYear;

        for (Task task : taskList) {
            taskCount.set(taskCount.get() + 1);

            switch (task.getType()) {

                case DEADLINE: {
                    taskDate = ((Deadline) task).getDate();
                    isSameYear = yearFormat.format(today).equals(yearFormat.format(taskDate));
                    if (isSameYear) {
                        addTaskWithSameYear(task, taskDate);
                    } else {
                        addTaskWithDifferentYear(task, taskDate);
                    }
                    break;
                }

                case EVENT: {
                    taskDate = ((Event) task).getStartDate();
                    isSameYear = yearFormat.format(today).equals(yearFormat.format(taskDate));
                    if (isSameYear) {
                        addTaskWithSameYear(task, taskDate);
                    } else {
                        addTaskWithDifferentYear(task, taskDate);
                    }
                    break;
                }

                case DREAM: {
                    TaskEntry taskEntry = new TaskEntry(taskCountFormatted.get(), task.getDescription());
                    if (task.isDone()) {
                        doneTaskList.getChildren().add(taskEntry.getEntryDisplay());
                    } else {
                        dreamsTaskList.getChildren().add(taskEntry.getEntryDisplay());
                    }
                    break;
                }

                default: {
                    System.out.println(MESSAGE_UNABLE_TO_DETERMINE_TYPE);
                }
            }
        }
        updateDisplay();
    }

    /**
     * Updates the display using fade transitions.
     * When the program is first initialised, all categories are faded in and shown.
     * After the user executes a command, empty categories are faded out and
     * non-empty categories are faded in.
     */
    private void updateDisplay() {
        setMainVBoxBackgroundImage(FX_BACKGROUND_IMAGE_NULL);

        SequentialTransition sequentialTransition = new SequentialTransition();
        for (Node node : nodeList) {
            // Remove empty nodes if it is on screen, else add non-empty nodes back into screen.
            if (node.equals(thisWeekNode)) {
                // Need to take care of special case with 'This Week' category
                ParallelTransition parallelTransition = new ParallelTransition();
                int totalTasksThisWeek = 0;
                for (int i=0; i<thisWeekSubcategories.size(); i++) {
                    totalTasksThisWeek += thisWeekSubcategories.get(i).getChildren().size();
                    addOrRemoveThisWeekSubcategories(parallelTransition, i);
                }
                // Next, to settle the main parent node for all the subcategories
                addOrRemoveThisWeekNode(sequentialTransition, parallelTransition, totalTasksThisWeek);
            // Next, settle all the other nodes
            } else if (((VBox) node.lookup(SELECTOR_CATEGORY_VBOX)).getChildren().isEmpty()) {
                removeNodeIfEmptyAndInDisplay(sequentialTransition, node);
            } else {
                addNodeIfNotEmptyAndNotInDisplay(sequentialTransition, node);
            }
        }
        sequentialTransition.setOnFinished(checkEmpty -> checkIfMainVBoxIsEmpty());
        sequentialTransition.play();
    }

    private void addTaskWithSameYear(Task task, Date date) {
        String dateString;

        switch (task.getType()) {

            case DEADLINE: {
                dateString = dateFormat.format(date)
                            + TIME_SEPARATOR
                            + timeFormat.format(date);
                String taskCount = taskCountFormatted.get();
                TaskEntry taskEntry = new TaskEntry(taskCount, task.getDescription(), dateString);
                if (task.isDone()) {
                    doneTaskList.getChildren().add(taskEntry.getEntryDisplay());
                } else if (date.before(currentDate)) {
                    overdueTaskList.getChildren().add(taskEntry.getEntryDisplay());
                } else if (date.before(endOfWeek)) {
                    addThisWeekTask(task, date, taskCount);
                } else {
                    futureTaskList.getChildren().add(taskEntry.getEntryDisplay());
                }
                break;
            }

            case EVENT: {
                Date endDate = ((Event) task).getEndDate();
                boolean isSameEndYear = checkIfEventEndSameYear(endDate);
                if (isSameEndYear) {
                    if (checkIfStartAndEndSameDay(date, endDate)) {
                        dateString = dateFormat.format(date)
                                + TIME_SEPARATOR
                                + timeFormat.format(date)
                                + EVENT_DATE_SEPARATOR_SAME_DAY
                                + timeFormat.format(endDate);
                    } else {
                        dateString = dateFormat.format(date)
                                + TIME_SEPARATOR
                                + timeFormat.format(date)
                                + EVENT_DATE_SEPARATOR_GENERAL
                                + dateFormat.format(endDate)
                                + TIME_SEPARATOR
                                + timeFormat.format(endDate);
                    }
                } else {
                    dateString = dateFormatWithYear.format(date)
                                + EVENT_DATE_SEPARATOR_GENERAL
                                + dateFormatWithYear.format(endDate);
                }
                String taskCount = taskCountFormatted.get();
                TaskEntry taskEntry = new TaskEntry(taskCount, task.getDescription(), dateString);
                if (task.isDone()) {
                    doneTaskList.getChildren().add(taskEntry.getEntryDisplay());
                } else if (date.before(currentDate)) {
                    overdueTaskList.getChildren().add(taskEntry.getEntryDisplay());
                } else if (date.before(endOfWeek)) {
                    addThisWeekTask(task, date, taskCount);
                } else {
                    futureTaskList.getChildren().add(taskEntry.getEntryDisplay());
                }
                break;
            }

            default: {
                System.out.println(MESSAGE_UNABLE_TO_DETERMINE_TYPE);
            }
        }
    }

    private void addTaskWithDifferentYear(Task task, Date date) {
        String dateString;

        switch (task.getType()) {

            case DEADLINE: {
                dateString = dateFormatWithYear.format(date);
                TaskEntry taskEntry = new TaskEntry(taskCountFormatted.get(), task.getDescription(), dateString);
                if (task.isDone()) {
                    doneTaskList.getChildren().add(taskEntry.getEntryDisplay());
                } else if (date.before(today)) {
                    overdueTaskList.getChildren().add(taskEntry.getEntryDisplay());
                } else {
                    futureTaskList.getChildren().add(taskEntry.getEntryDisplay());
                }
                break;
            }

            case EVENT: {
                Date endDate = ((Event) task).getEndDate();
                dateString = dateFormatWithYear.format(date)
                            + EVENT_DATE_SEPARATOR_GENERAL
                            + dateFormatWithYear.format(endDate);
                TaskEntry taskEntry = new TaskEntry(taskCountFormatted.get(), task.getDescription(), dateString);
                if (task.isDone()) {
                    doneTaskList.getChildren().add(taskEntry.getEntryDisplay());
                } else if (date.before(today)) {
                    overdueTaskList.getChildren().add(taskEntry.getEntryDisplay());
                } else {
                    futureTaskList.getChildren().add(taskEntry.getEntryDisplay());
                }
                break;
            }

            default: {
                System.out.println(MESSAGE_UNABLE_TO_DETERMINE_TYPE);
            }
        }
    }

    /**
     * Iterates through the list of subcategories and find the corresponding date of the task to go into.
     * If it is unable to find one, it will add the task into the 'Future' category instead.
     * @param taskEntry to be added
     * @param date of the task due
     */
    private void addThisWeekTask(Task task, Date startDate, String taskCount) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(today);
        calendar.add(Calendar.DATE, 1);
        Date deadline = calendar.getTime();

        boolean isAdded = false;
        switch (task.getType()) {

            case DEADLINE: {
                TaskEntry taskEntry = new TaskEntry(taskCount, task.getDescription(), timeFormat.format(startDate));
                for (VBox vBox : thisWeekSubcategories) {
                    if (startDate.before(deadline)) {
                        vBox.getChildren().add(taskEntry.getEntryDisplay());
                        isAdded = true;
                        break;
                    } else {
                        calendar.add(Calendar.DATE, 1);
                        deadline = calendar.getTime();
                    }
                }
                if (!isAdded) {
                    futureTaskList.getChildren().add(taskEntry.getEntryDisplay());
                }
                break;
            }

            case EVENT: {
                Date endDate = ((Event) task).getEndDate();
                String dateString;
                boolean isSameEndYear = checkIfEventEndSameYear(endDate);
                if (isSameEndYear) {
                    if (checkIfStartAndEndSameDay(startDate, endDate)) {
                        dateString = timeFormat.format(startDate)
                                + EVENT_DATE_SEPARATOR_SAME_DAY
                                + timeFormat.format(endDate);
                    } else if (endDate.before(endOfWeek)) {
                    dateString = getFriendlyDayFormat(startDate)
                            + TIME_SEPARATOR
                            + timeFormat.format(startDate)
                            + EVENT_DATE_SEPARATOR_GENERAL
                            + getFriendlyDayFormat(endDate)
                            + TIME_SEPARATOR
                            + timeFormat.format(endDate);
                    } else {
                        dateString = getFriendlyDayFormat(startDate)
                                + TIME_SEPARATOR
                                + timeFormat.format(startDate)
                                + EVENT_DATE_SEPARATOR_GENERAL
                                + dateFormat.format(endDate)
                                + TIME_SEPARATOR
                                + timeFormat.format(endDate);
                    }
                } else {
                    dateString = getFriendlyDayFormat(startDate)
                                + TIME_SEPARATOR
                                + timeFormat.format(startDate)
                                + EVENT_DATE_SEPARATOR_GENERAL
                                + dateFormatWithYear.format(endDate);
                }
                TaskEntry taskEntry = new TaskEntry(taskCount, task.getDescription(), dateString);
                for (VBox vBox : thisWeekSubcategories) {
                    if (startDate.before(deadline)) {
                        vBox.getChildren().add(taskEntry.getEntryDisplay());
                        isAdded = true;
                        break;
                    } else {
                        calendar.add(Calendar.DATE, 1);
                        deadline = calendar.getTime();
                    }
                }
                if (!isAdded) {
                    futureTaskList.getChildren().add(taskEntry.getEntryDisplay());
                }
                break;
            }

            default:
                System.out.println(MESSAGE_UNABLE_TO_DETERMINE_TYPE);
                break;
        }
    }

    // ================================================================================
    // Animation methods
    // ================================================================================

    private FadeTransition generateFadeInTransition(Node nodeToFade, int fadeInTime) {
        FadeTransition transition = new FadeTransition(Duration.millis(fadeInTime), nodeToFade);
        transition.setFromValue(OPACITY_ZERO);
        transition.setToValue(OPACITY_FULL);
        transition.setInterpolator(Interpolator.EASE_IN);
        return transition;
    }

    private FadeTransition generateFadeOutTransition(Node nodeToFade, int fadeOutTime) {
        FadeTransition transition = new FadeTransition(Duration.millis(fadeOutTime), nodeToFade);
        transition.setFromValue(OPACITY_FULL);
        transition.setToValue(OPACITY_ZERO);
        transition.setInterpolator(Interpolator.EASE_IN);
        return transition;
    }

    /**
     * Determines the correct position for each node to be added back to
     * @param node to be added
     */
    private void addNodeBackToScreen(Node node) {
        String nodeName = determineNodeName(node);
        switch (nodeName) {

            case CATEGORY_OVERDUE: {
                // Just need to add to the front
                mainVBox.getChildren().add(0, node);
                break;
            }

            case CATEGORY_THIS_WEEK: {
                if (mainVBox.getChildren().contains(overdueNode)) {
                    // Check if the 'Overdue' node is on screen or not and adds this node after it
                    mainVBox.getChildren().add(mainVBox.getChildren().indexOf(overdueNode) + 1, node);
                } else {
                    // Else this node would take precedence at the top.
                    mainVBox.getChildren().add(0, node);
                }
                break;
            }

            case CATEGORY_FUTURE: {
                if (mainVBox.getChildren().contains(overdueNode) && mainVBox.getChildren().contains(thisWeekNode)) {
                    // Check if 'Overdue' and 'This Week' nodes are added before. This node takes position after them
                    mainVBox.getChildren().add(mainVBox.getChildren().indexOf(thisWeekNode) + 1, node);
                } else if (mainVBox.getChildren().contains(overdueNode) && !mainVBox.getChildren().contains(thisWeekNode)) {
                    // Then check if either one is available
                    mainVBox.getChildren().add(mainVBox.getChildren().indexOf(overdueNode) + 1, node);
                } else if (mainVBox.getChildren().contains(thisWeekNode)) {
                    mainVBox.getChildren().add(mainVBox.getChildren().indexOf(thisWeekNode) + 1, node);
                } else {
                    // Else it will go to the top
                    mainVBox.getChildren().add(0, node);
                }
                break;
            }

            case CATEGORY_DREAMS: {
                // Only needs to check if there is a lower node than it, where there is one 1 - doneNode.
                if (mainVBox.getChildren().contains(doneNode)) {
                    mainVBox.getChildren().add(mainVBox.getChildren().indexOf(doneNode), node);
                } else {
                    mainVBox.getChildren().add(node);
                }
                break;
            }

            case CATEGORY_DONE: {
                // Takes position at the bottom of the list
                mainVBox.getChildren().add(node);
                break;
            }

            default: {
                System.out.println(MESSAGE_UNABLE_TO_RECOGNISE_NODE);
            }
        }
    }

    private void addNodeIfNotEmptyAndNotInDisplay(SequentialTransition sequentialTransition, Node node) {
        if (!mainVBox.getChildren().contains(node)) {
            FadeTransition fadeIn = generateFadeInTransition(node, TIME_TRANSITION_CATEGORY_FADE_IN);
            addNodeBackToScreen(node);
            sequentialTransition.getChildren().add(fadeIn);
        }
    }

    private void addOrRemoveThisWeekNode(SequentialTransition sequentialTransition, ParallelTransition parallelTransition, int totalTasksThisWeek) {
        if (totalTasksThisWeek == 0 && mainVBox.getChildren().contains(thisWeekNode)) {
            // If there are no tasks within all the subcategories, remove the node if it is contained in the mainVBox
            FadeTransition fadeOut = generateFadeOutTransition(thisWeekNode, TIME_TRANSITION_CATEGORY_FADE_OUT);
            fadeOut.setOnFinished(done -> mainVBox.getChildren().remove(thisWeekNode));
            sequentialTransition.getChildren().add(parallelTransition);
            sequentialTransition.getChildren().add(fadeOut);
        } else if (totalTasksThisWeek != 0 && !mainVBox.getChildren().contains(thisWeekNode)){
            // Else if there are some tasks and yet it is not contained in the mainVBox, fade it in.
            FadeTransition fadeIn = generateFadeInTransition(thisWeekNode, TIME_TRANSITION_CATEGORY_FADE_IN);
            addNodeBackToScreen(thisWeekNode);
            sequentialTransition.getChildren().add(fadeIn);
        } else {
            // Else just fade the subcategories
            sequentialTransition.getChildren().add(parallelTransition);
        }
    }

    private void addOrRemoveThisWeekSubcategories(ParallelTransition parallelTransition, int i) {
        // Each element of subcategoryVisibilityTracker corresponds to the subcategory at a particular
        // index, '0' indicates visible/faded in and '1' indicates it has been faded out previously.
        if (thisWeekSubcategories.get(i).getChildren().isEmpty()) {
            // 2 cases, either it has been faded in or not faded in previously.
            if (subcategoryVisibilityTracker[i] == 0) {
                // If faded out previously/not faded in yet, just remove away from the view
                thisWeekTaskList.getChildren().remove(thisWeekSubcategories.get(i).getParent());
            } else {
                // If faded in, set it up to fade out since it has been emptied.
                Node parentNode = thisWeekSubcategories.get(i).getParent();
                FadeTransition fadeOut = generateFadeOutTransition(parentNode, TIME_TRANSITION_SUBCATEGORY_FADE_OUT);
                fadeOut.setOnFinished(done -> thisWeekTaskList.getChildren().remove(parentNode));
                parallelTransition.getChildren().add(fadeOut);
                subcategoryVisibilityTracker[i] = 0;
            }
        } else if (!(thisWeekSubcategories.get(i).getChildren().isEmpty()) && (subcategoryVisibilityTracker[i] == 0)) {
            // All non-empty and faded out should be faded back in.
            FadeTransition fadeIn = generateFadeInTransition(thisWeekSubcategories.get(i).getParent(), TIME_TRANSITION_SUBCATEGORY_FADE_IN);
            parallelTransition.getChildren().add(fadeIn);
            subcategoryVisibilityTracker[i] = 1;
        } else {
            // Other cases can just ignore.
        }
    }

    private void removeNodeIfEmptyAndInDisplay(SequentialTransition sequentialTransition, Node node) {
        if (mainVBox.getChildren().contains(node)) {
            FadeTransition fadeOut = generateFadeOutTransition(node, TIME_TRANSITION_CATEGORY_FADE_OUT);
            fadeOut.setOnFinished(done -> mainVBox.getChildren().remove(node));
            sequentialTransition.getChildren().add(fadeOut);
        }
    }

    // ================================================================================
    // Utility methods
    // ================================================================================

    /**
     * Used when updating the task list, removes all tasks and resets the task counter
     */
    private void clearTaskList() {
        resetTaskCount();
        resetTaskList();
        generateThisWeekSubcategories();
    }

    /**
     * Generates the relative date sub-headers for the remaining days of the week and places them
     * in the task list for 'This Week'.
     */
    private void generateThisWeekSubcategories() {
        LocalDateTime startingDateTime = getDateTimeStartOfToday();
        ArrayList<Node> thisWeekDateBoxes = new ArrayList<>();
        int count = 1;
        while (!(getInstantFromLocalDateTime(startingDateTime)).equals(endOfWeek.toInstant())) {
            DateBox newDateBox;
            if (count == 1) {
                newDateBox = new DateBox(SUBCATEGORY_TODAY);
            } else if (count == 2) {
                newDateBox = new DateBox(SUBCATEGORY_TOMORROW);
            } else {
                newDateBox = new DateBox(startingDateTime.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault()));
            }
            VBox newDateVBox = newDateBox.getTaskListVBox();
            thisWeekSubcategories.add(newDateVBox);
            thisWeekDateBoxes.add(newDateBox.getDateBox());
            startingDateTime = startingDateTime.plusDays(1);
            count++;
        }
        if (subcategoryVisibilityTracker == null || (subcategoryVisibilityTracker.length != thisWeekDateBoxes.size())) {
            subcategoryVisibilityTracker = new int[thisWeekDateBoxes.size()];
        }
        thisWeekTaskList.getChildren().addAll(thisWeekDateBoxes);
    }

    private void resetTaskCount() {
        taskCount.set(0);
    }

    private void resetTaskList() {
        overdueTaskList.getChildren().clear();
        thisWeekTaskList.getChildren().clear();
        futureTaskList.getChildren().clear();
        dreamsTaskList.getChildren().clear();
        doneTaskList.getChildren().clear();

        thisWeekSubcategories.clear();
    }

    private String determineNodeName(Node node) {
        if (node.equals(overdueNode)) {
            return CATEGORY_OVERDUE;
        } else if (node.equals(thisWeekNode)) {
            return CATEGORY_THIS_WEEK;
        } else if (node.equals(futureNode)) {
            return CATEGORY_FUTURE;
        } else {
            return CATEGORY_DREAMS;
        }
    }

    private void updateDates() {
        today = Date.from(getInstantFromLocalDateTime(getDateTimeStartOfToday()));    // To get today's Date at 0000hrs
        currentDate = new Date();
        endOfWeek = getEndOfWeekDate(today);
    }

    private LocalDateTime getDateTimeStartOfToday() {
        return LocalDate.now().atStartOfDay();
    }

    /**
     * Converts a LocalDateTime to an Instant
     * @param localDateTime to be converted
     * @return Instant generated from the given LocalDateTime
     */
    private Instant getInstantFromLocalDateTime(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    }

    /**
     * Generates the date of the end of the week for task date comparisons
     * @param today Current date at 0000hrs
     * @return Date of next Monday at 0000hrs for comparing tasks due this week
     */
    private Date getEndOfWeekDate(Date today) {
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.setTime(today);
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        calendar.add(Calendar.DAY_OF_WEEK, 1);
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        return calendar.getTime();
    }

    private boolean checkIfEventEndSameYear(Date date) {
        return yearFormat.format(today).equals(yearFormat.format(date));
    }

    private boolean checkIfStartAndEndSameDay(Date firstDate, Date secondDate) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(firstDate);
        int firstDay = calendar.get(Calendar.DAY_OF_YEAR);
        calendar.setTime(secondDate);
        int secondDay = calendar.get(Calendar.DAY_OF_YEAR);
        return firstDay == secondDay;
    }

    private String getDayOfWeek(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE");
        return dateFormat.format(date);
    }

    private String getFriendlyDayFormat(Date date) {
        LocalDateTime startingDateTime = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        if (startingDateTime.getDayOfMonth() == getDateTimeStartOfToday().getDayOfMonth()) {
            return SUBCATEGORY_TODAY;
        } else if (startingDateTime.getDayOfMonth() == getDateTimeStartOfToday().plusDays(1).getDayOfMonth()) {
            return SUBCATEGORY_TOMORROW;
        } else {
            return getDayOfWeek(date);
        }
    }

    private void checkIfMainVBoxIsEmpty() {
        if (FX_BACKGROUND_IMAGE_NO_TASKS == null) {
            String image = MainScreen.class.getResource(LOCATION_EMPTY_VIEW).toExternalForm();
            FX_BACKGROUND_IMAGE_NO_TASKS = "-fx-background-image: url('" + image + "');";
        }
        if (mainVBox.getChildren().isEmpty()) {
            mainVBox.setStyle(FX_BACKGROUND_IMAGE_NO_TASKS);
        }
    }

    private void setMainVBoxBackgroundImage(String value) {
        mainVBox.setStyle(value);
    }

    // ================================================================================
    // Init methods
    // ================================================================================

    private void loadLayout(String filePath) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(filePath));
        loader.setController(this); // Required due to different package declaration from Main
        try {
            node = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Setup the various categories that tasks can fall under
     */
    private void createCategories() {
        // Create all the different categories(by time frame) for entries to go into
        CategoryBox overdueBox = new CategoryBox(CATEGORY_OVERDUE);
        CategoryBox thisWeekBox = new CategoryBox(CATEGORY_THIS_WEEK);
        CategoryBox futureBox = new CategoryBox(CATEGORY_FUTURE);
        CategoryBox dreamsBox = new CategoryBox(CATEGORY_DREAMS);
        CategoryBox doneBox = new CategoryBox(CATEGORY_DONE);

        this.overdueNode = overdueBox.getCategoryBox();
        this.overdueTaskList = overdueBox.getTaskListVBox();
        nodeList.add(overdueNode);

        this.thisWeekNode = thisWeekBox.getCategoryBox();
        this.thisWeekTaskList = thisWeekBox.getTaskListVBox();
        nodeList.add(thisWeekNode);

        this.futureNode = futureBox.getCategoryBox();
        this.futureTaskList = futureBox.getTaskListVBox();
        nodeList.add(futureNode);

        this.dreamsNode = dreamsBox.getCategoryBox();
        this.dreamsTaskList = dreamsBox.getTaskListVBox();
        nodeList.add(dreamsNode);

        // Since 'Done' tasks are not shown on start up, it is not added into the nodeList.
        this.doneNode = doneBox.getCategoryBox();
        this.doneTaskList = doneBox.getTaskListVBox();
        nodeList.add(doneNode);
    }

    /**
     * Creates a formatted shared task counter for use when adding tasks onto the screen
     */
    private void setupBinding() {
        taskCountString.bindBidirectional(taskCount, new NumberStringConverter());
        taskCountFormatted.bind(Bindings.concat(taskCountString).concat(UI_NUMBER_SEPARATOR));
    }

    // ================================================================================
    // Getter methods
    // ================================================================================

    protected Node getNode() {
        return this.node;
    }

    protected VBox getMainVBox() {
        return this.mainVBox;
    }
}
