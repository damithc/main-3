package procrastinate.test;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import org.junit.Test;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import procrastinate.FileHandler;
import procrastinate.task.DateAdapter;
import procrastinate.task.Task;
import procrastinate.task.TaskDeserializer;
import procrastinate.task.TaskState;

public class FileHandlerTest {
    private String defaultName;
    private FileHandler handler = null;
    private static Path originalSavePath;
    private static Path originalSaveName;
    private static Path tmpDir;
    private static boolean hasCfg;
    private static boolean hasSave;

    // move config and save file to a tmp folder
    @BeforeClass
    public static void prep() {
        Path cfg = Paths.get("settings.config");
        hasCfg = Files.exists(cfg);

        if (hasCfg) {
            BufferedReader reader = null;
            try {
                tmpDir = Files.createTempDirectory(Paths.get(""), "testtmp");

                reader = new BufferedReader(new FileReader(cfg.toFile()));
                originalSavePath = Paths.get(reader.readLine());
                System.out.println(originalSavePath);
                hasSave = Files.exists(originalSavePath);

                if (hasSave) {
                    originalSaveName = originalSavePath.getFileName();

                    Files.move(originalSavePath, tmpDir.resolve(originalSaveName));
                }

                Files.move(cfg, tmpDir.resolve(cfg));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // move config and save file back to original location
    @AfterClass
    public static void revert() {
        try {
            if (hasSave) {
                Files.move(tmpDir.resolve(originalSaveName), originalSavePath);
            }

            if (hasCfg) {
                Files.move(tmpDir.resolve("settings.config"), Paths.get("settings.config"));
                Files.deleteIfExists(tmpDir);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Before
    public void setup() {
        defaultName = "storage.json";
    }

    @After
    public void tearDown() {
        File config = handler.getConfigFile();
        File save = handler.getSaveFile();
        try {
            Files.deleteIfExists(config.toPath());
            Files.deleteIfExists(save.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        handler = null;

    }

    @Test
    public void fileHandler_DirNotGiven_ShouldMakeFileAtCurrLoc() throws IOException {
        handler = new FileHandler();
        Path p = Paths.get(defaultName);

        assertTrue(Files.exists(p));
        assertTrue(Files.isRegularFile(p));
        assertTrue(Files.isWritable(p));
    }

    @Test
    public void setPath_RelativePathWithFilename_ShouldUpdateCfgAndSaveFileLoc() throws IOException {
        Path newPath = Paths.get("../setpathtest.json");
        handler = new FileHandler();

        handler.setPath(newPath);

        BufferedReader br = new BufferedReader(new FileReader(handler.getConfigFile()));
        String content = br.readLine();
        br.close();

        assertTrue(newPath.equals(handler.getSaveFile().toPath()));
        assertTrue(Files.isSameFile(newPath, handler.getSaveFile().toPath()));
        assertEquals(newPath.toString(), content);
    }

    @Test
    public void setPath_SameDirDiffName_ShouldRemoveOldFile() throws IOException {
        Path newPath = Paths.get("setpathtest.json");
        handler = new FileHandler();

        File oldSave = handler.getSaveFile();
        System.out.println(oldSave.toString());
        handler.setPath(newPath);

        assertTrue(Files.notExists(oldSave.toPath()));;
    }

    @Test
    public void loadTaskState_HasStorageFile_ShouldLoadState() throws IOException {
        BufferedReader br;
        TaskState loadedState;

        handler = new FileHandler();
        // save stubstate to file
        handler.saveTaskState(new TaskStateStub());

        // laod state from file
        loadedState = handler.loadTaskState();

        // mock a json file and load from it
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Task.class, new TaskDeserializer())
                .registerTypeAdapter(Date.class, new DateAdapter())
                .create();

        Type type = new TypeToken<TaskState>() {}.getType();
        br = new BufferedReader(new FileReader(new File(defaultName)));
        TaskState stub = gson.fromJson(br, type);

        assertEquals(stub, loadedState);
    }

    @Test
    public void loadConfig_NoConfigFile_ShouldMakeFile() throws IOException {
        handler = new FileHandler();
        BufferedReader reader = new BufferedReader(new FileReader(Paths.get("settings.config").toFile()));
        String line;
        if ((line = reader.readLine()) != null) {
            assertEquals(defaultName, line);
        } else {
            fail();
        }
        reader.close();
    }

    @Test
    public void loadTaskState_NoStorageFile_ShouldInitAndLoad() throws IOException {
        TaskState loadedState;
        handler = new FileHandler();

        // load state from file
        loadedState = handler.loadTaskState();

        assertEquals(0, loadedState.getTasks().size());
    }
}