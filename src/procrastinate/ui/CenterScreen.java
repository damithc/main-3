package procrastinate.ui;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
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
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

public abstract class CenterScreen extends VBox {

    protected static final String MESSAGE_UNABLE_TO_DETERMINE_TYPE = "Unable to determine TaskType for adding.";
    protected static final String MESSAGE_UNABLE_TO_RECOGNISE_NODE = "Unable to recognise Node.";

    protected static final String EVENT_DATE_SEPARATOR_GENERAL = "\nto ";
    protected static final String EVENT_DATE_SEPARATOR_SAME_DAY = " to ";
    protected static final String FRIENDLY_DATE_OR_TIME_SEPARATOR = " ";

    protected static final String SELECTOR_CATEGORY_VBOX = "#categoryVBox";

    protected static final String FX_BACKGROUND_IMAGE_NULL = "-fx-background-image: null;";
    protected static String FX_BACKGROUND_IMAGE_NO_TASKS; // will be initialised later on.

    private static final String UI_NUMBER_SEPARATOR = ". ";

    private static final double OPACITY_ZERO = 0;
    private static final double OPACITY_FULL = 1;

    private static final String DATE_TODAY = "Today";
    private static final String DATE_TOMORROW = "Tomorrow";

    // ================================================================================
    // Class variables
    // ================================================================================

    protected IntegerProperty taskCount = new SimpleIntegerProperty(1);
    protected StringProperty taskCountFormatted = new SimpleStringProperty();
    protected StringProperty taskCountString = new SimpleStringProperty();

    protected SimpleDateFormat dateFormatWithFriendlyDayAndYear = new SimpleDateFormat("EEE d MMM''yy h:mma");
    protected SimpleDateFormat dateFormat = new SimpleDateFormat("d MMM");
    protected SimpleDateFormat timeFormat = new SimpleDateFormat("h:mma");
    protected SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy");

    private Node node;

    private Date today;
    private Date currentDate;
    private Date endOfWeek;

    // ================================================================================
    // FXML field variables
    // ================================================================================

    @FXML private VBox mainVBox;

    // ================================================================================
    // CenterScreen Constructor
    // ================================================================================

    protected CenterScreen(String filePath) {
        loadLayout(filePath);
        setupBinding();
    }

    // ================================================================================
    // Init methods
    // ================================================================================

    /**
     * Setup the various categories that tasks can fall under
     */
    protected abstract void createCategories();

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
     * Creates a formatted shared task counter for use when adding tasks onto the screen
     */
    private void setupBinding() {
        taskCountString.bindBidirectional(taskCount, new NumberStringConverter());
        taskCountFormatted.bind(Bindings.concat(taskCountString).concat(UI_NUMBER_SEPARATOR));
    }

    // ================================================================================
    // Animation methods
    // ================================================================================

    protected FadeTransition generateFadeInTransition(Node nodeToFade, int fadeInTime) {
        FadeTransition transition = new FadeTransition(Duration.millis(fadeInTime), nodeToFade);
        transition.setFromValue(OPACITY_ZERO);
        transition.setToValue(OPACITY_FULL);
        transition.setInterpolator(Interpolator.EASE_IN);
        return transition;
    }

    protected FadeTransition generateFadeOutTransition(Node nodeToFade, int fadeOutTime) {
        FadeTransition transition = new FadeTransition(Duration.millis(fadeOutTime), nodeToFade);
        transition.setFromValue(OPACITY_FULL);
        transition.setToValue(OPACITY_ZERO);
        transition.setInterpolator(Interpolator.EASE_IN);
        return transition;
    }

    // ================================================================================
    // Utility methods
    // ================================================================================

    protected void setMainVBoxBackgroundImage(VBox mainVBox, String value) {
        mainVBox.setStyle(value);
    }

    protected void updateDates() {
        today = Date.from(getInstantFromLocalDateTime(getDateTimeStartOfToday()));    // To get today's Date at 0000hrs
        currentDate = new Date();
        endOfWeek = getEndOfWeekDate(today);
    }

    protected LocalDateTime getDateTimeStartOfToday() {
        return LocalDate.now().atStartOfDay();
    }

    /**
     * Converts a LocalDateTime to an Instant
     * @param localDateTime to be converted
     * @return Instant generated from the given LocalDateTime
     */
    protected Instant getInstantFromLocalDateTime(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    }

    protected boolean checkIfEventEndSameYear(Date date, Date today) {
        return yearFormat.format(today).equals(yearFormat.format(date));
    }

    protected boolean checkIfStartAndEndSameDay(Date firstDate, Date secondDate) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(firstDate);
        int firstDay = calendar.get(Calendar.DAY_OF_YEAR);
        calendar.setTime(secondDate);
        int secondDay = calendar.get(Calendar.DAY_OF_YEAR);
        return firstDay == secondDay;
    }

    protected String getFriendlyDayFormatThisWeek(Date date) {
        LocalDateTime startingDateTime = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        if (startingDateTime.getDayOfMonth() == getDateTimeStartOfToday().getDayOfMonth()) {
            return DATE_TODAY;
        } else if (startingDateTime.getDayOfMonth() == getDateTimeStartOfToday().plusDays(1).getDayOfMonth()) {
            return DATE_TOMORROW;
        } else {
            return getDayOfWeek(date);
        }
    }

    protected String getDayOfWeek(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE");
        return dateFormat.format(date);
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

    // ================================================================================
    // Getter methods
    // ================================================================================

    protected Node getNode() {
        return this.node;
    }

    protected VBox getMainVBox() {
        return this.mainVBox;
    }

    protected Date getToday() {
        return today;
    }

    protected Date getCurrentDate() {
        return currentDate;
    }

    protected Date getEndOfWeek() {
        return endOfWeek;
    }
}
