package gtc.DSL.CK.IR;


import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import java.io.File;
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
        // Gson gson = new GsonBuilder().setPrettyPrinting().create();
        // new JSONParser();
        // JsonElement je = JsonParser.parseString(instances.toJSONString());

        File file= new File (path);
        FileWriter fw;
        file.createNewFile();
        fw = new FileWriter(file);
        fw.write(instances.toJSONString());
        fw.close();
        System.out.println("File saved at " + path );
    }
}
