package procrastinate.test;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.StringProperty;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import procrastinate.task.Task;
import procrastinate.ui.UI;

public class UIStub extends UI {
    public List<Task> taskList;
    public UIStub() {
        super(true);
        taskList = new ArrayList<Task>();
    }
    @Override
    public void initialize() {
    }
    @Override
    public void setUpBinding(StringProperty userInput, StringProperty statusLabelText) {
    }
    @Override
    public void setUpStage(Stage primaryStage) {
    }
    @Override
    public void updateTaskList(List<Task> tasks) {
        taskList = tasks;
    }
    @Override
    public TextField getUserInputField() {
        return new TextField();
    }
    @Override
    public void checkForScreenOverlay() {
    }
    @Override
    public void showHelp() {
    }
}