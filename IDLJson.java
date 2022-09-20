package gtc.DSL.CK.IR;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.FileWriter;
import java.io.IOException;

public class IDLJson {
    private JSONArray instances;

    public IDLJson() {
        instances = new JSONArray();
    }

    public void addInstance(JSONObject instance) {
        if (instance != null)
            instances.add(instance);
    }

    public void write(String path) throws IOException {
        System.out.println("Writing to file...");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement je = JsonParser.parseString(instances.toJSONString());
        FileWriter file = new FileWriter(path);
        file.write(gson.toJson(je));
        file.close();
        System.out.println("File saved at " + path );
    }
}
