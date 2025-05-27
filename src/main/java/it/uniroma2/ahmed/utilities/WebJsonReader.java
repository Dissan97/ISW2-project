package it.uniroma2.ahmed.utilities;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

public class WebJsonReader {

    private WebJsonReader() {}

    private static @NotNull String readAll(@NotNull Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    public static @NotNull JSONObject readJsonFromUrl(String url) throws IOException, JSONException,
            URISyntaxException {
        InputStream is = new URI(url).toURL().openStream();
        BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        String jsonText = readAll(rd);
        return new JSONObject(jsonText);
    }
}
