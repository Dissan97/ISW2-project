package it.torvergata.dissanuddinahmed.utilities;

import it.torvergata.dissanuddinahmed.logging.SeLogger;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class WorkLoader {

    private WorkLoader(){}

    public static @Nullable JSONObject load(String path){
        try {
            String jsonString = new String(Files.readAllBytes(Paths.get(path)));
            return new JSONObject(jsonString);

        } catch (IOException e) {
            SeLogger.getInstance().getLogger().severe(e.getMessage());
            return null;
        }
    }
}