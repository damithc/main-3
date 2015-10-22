package procrastinate.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.io.PrintWriter;
import java.io.StringWriter;

public class DialogPopupHandler {

    // ================================================================================
    // Message Strings
    // ================================================================================

    private static final String MESSAGE_TITLE = "Error";
    private static final String MESSAGE_HEADER = "An error has occurred with the following message:";

    // ================================================================================
    // Class variables
    // ================================================================================

    private Stage primaryStage;

    // ================================================================================
    // DialogPopupHandler methods
    // ================================================================================

    protected DialogPopupHandler(Stage primaryStage) {
        // Set up the parent stage to retrieve information if required
        this.primaryStage = primaryStage;
    }

    /**
     * Creates an error dialog that displays the given message string
     *
     * @param message
     */
    protected void createErrorDialogPopup(String message) {
        Alert dialog = new Alert(Alert.AlertType.ERROR);
        dialog.initOwner(primaryStage);

        dialog.setTitle(MESSAGE_TITLE);
        dialog.setHeaderText(MESSAGE_HEADER);
        dialog.setContentText(message);
    }

    /**
     * Creates an error dialog that takes in an Exception and displays the exception message with its stack trace
     * in an expandable region.
     *
     * @param exception
     */
    protected void createErrorDialogPopupWithTrace(Exception exception) {
        Alert dialog = new Alert(Alert.AlertType.ERROR);
        dialog.initOwner(primaryStage);

        // Retrieve the stack trace as String
        StringWriter stringWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stringWriter));
        String stackTrace = stringWriter.toString();

        dialog.setTitle(MESSAGE_TITLE);
        dialog.setHeaderText(MESSAGE_HEADER);
        dialog.setContentText(exception.getMessage());

        // Place the stack trace into a TextArea for display
        TextArea textArea = new TextArea();
        textArea.setWrapText(true);
        textArea.setEditable(false);
        textArea.setText(stackTrace);

        dialog.getDialogPane().setExpandableContent(textArea);
        dialog.showAndWait();
    }
}