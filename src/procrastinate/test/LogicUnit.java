//@@author A0080485B
package procrastinate.test;

import java.io.IOException;

import procrastinate.Logic;

public class LogicUnit extends Logic {
    public LogicUnit(UIStub uiStub) {
        ui = uiStub;
    }
    @Override
    protected void initTaskEngine() {
        try {
            taskEngine = new TaskEngineUnit();
        } catch (IOException e) {
        }
    }
}
