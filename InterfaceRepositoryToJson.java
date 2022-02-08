package gtc.DSL.CK.IR;

import gtc.DSL.DAF.CORBAServices;
import gtc.DSL.DAF.NotAvailable;
import gtc.DSL.Debug.Output;
import net.minidev.json.JSONObject;
import org.omg.CORBA.*;
import org.omg.CORBA.TypeCodePackage.BadKind;

import java.io.IOException;
import java.util.*;

public class InterfaceRepositoryToJson {
    private final static List<String> MODULES_TO_EXCLUDE = Arrays.asList("MM", "CORBA", "DGT", "CONFIG", "DCF",
            "INSTRUMENT", "COMMON_INSTRUMENT_TYPES", "GTC", "ALARM");
    private final static List<String> ATTRIBUTES_TO_EXCLUDE = Arrays.asList("version", "state", "magnitudes",
            "properties", "className", "alarms", "configuration", "name", "startTime");

    private static String JSON_FILE_PATH = "";
    private final static String FILE_NAME = "idl.json";

    private void start() throws NotAvailable, IOException {
        IDLJson instances = new IDLJson();

        // Search for all elements in the IDL
        Contained[] contents = CORBAServices.getIR().contents(DefinitionKind.dk_all, false);
        for (Contained containedObject : contents) {
            ModuleDef moduleDef = ModuleDefHelper.narrow(containedObject);
            if (MODULES_TO_EXCLUDE.contains(moduleDef.name())) {
                moduleDef._release();
                containedObject._release();
                continue;
            }

            System.out.println("Reading module " +moduleDef.name() + "...");

            // Search interface definition
            Contained[] containedInterfaces = moduleDef.contents(DefinitionKind.dk_Interface, false);
            for (Contained containedInt : containedInterfaces) {
                InterfaceDef interfaceDef = InterfaceDefHelper.narrow(containedInt);
                if (interfaceDef == null) continue;

                JSONObject instance = parseInstance(interfaceDef);
                instances.addInstance(instance);
                interfaceDef._release();
                containedInt._release();

            }
            moduleDef._release();
            containedObject._release();
        }

        instances.write(JSON_FILE_PATH);
    }

    private JSONObject parseInstance(InterfaceDef interfaceDef) {
        JSONObject instance = new JSONObject();
        System.out.println("\tParsing Interface: " + interfaceDef.name());
        String interfaceName = interfaceDef.name().replace("_ifce", "");
        instance.put("instance", interfaceName);
        instance.put("className", interfaceName);
        try {
            instance.put("monitors", parseMagnitudes( interfaceDef.contents(DefinitionKind.dk_Attribute, false), interfaceName));
        } catch (Exception e) {
            System.out.println("\t\tCORBA Exception while getting the interface description of "
             + interfaceDef.name());
            e.printStackTrace();
            instance.put("monitors", new JSONObject());
        }

        return instance;

    }

    private JSONObject parseMagnitudes(Contained[] attributes, String interfaceName) {
        JSONObject monitors = new JSONObject();
        for (Contained c : attributes) {
            AttributeDescription attDescription = AttributeDescriptionHelper.extract(c.describe().value);
            try {
                // Magnitudes in IDL are specified as READONLY
                if (attDescription.mode != AttributeMode.ATTR_READONLY) continue;

                if (ATTRIBUTES_TO_EXCLUDE.contains(attDescription.name)) continue;

                //if (!attDescription.defined_in.contains(interfaceName)) continue;

                JSONObject monitorConfig = new JSONObject();
                String monitorType = parseMagnitudeType(attDescription.type);
                int typeCode = attDescription.type.kind().value();

                if (typeCode == TCKind._tk_enum) {
                    List<String> values = new ArrayList<>();
                    for (int i = 0; i< attDescription.type.member_count();i++)
                        values.add(attDescription.type.member_name(i));

                    String enumsName = Arrays.toString(values.toArray()).replaceAll("[()\\[\\]]", "");
                    monitorConfig.put("values", enumsName);
                    monitorConfig.put("type", monitorType);
                } else {
                    if (getTypeMap().containsKey(typeCode)) {
                        monitorConfig.put("type", getTypeMap().get(typeCode));
                    } else {
                        monitorConfig.put("type", monitorType);
                    }
                }

                monitors.put(attDescription.name, monitorConfig);
            } catch (Exception e) {
                System.out.println("\t\tError while processing attribute " + attDescription.name);
                e.printStackTrace();
            }
        }
        return monitors;
    }

    private String parseMagnitudeType(TypeCode type) throws BadKind {
        String tmp = type.toString();
        tmp = tmp.substring(tmp.indexOf("=")+1)
                .replace("\n", "")
                .replaceAll("\\.", "")
                .trim();

        if (tmp.indexOf(" ") > 0) {
            String[] enumType = tmp.split(" ");
            if (enumType[0].equals("enum"))
                return "enum_"+enumType[1];
            return enumType[1];
        }
        return tmp;
    }

    public static void main(String[] args) throws Exception
    {
        for (int i = 0; i < args.length; i++)
        {
            if (args[i].compareTo("-o"    )==0) {
                String path = args[++i];
                if (path.charAt(path.length()-1) != '/')
                    JSON_FILE_PATH = path + "/" + FILE_NAME;
                else
                    JSON_FILE_PATH = path + FILE_NAME;
            }
            else if (args[i].compareTo("-debug"      )==0) Output.enableDebug();
            else if (args[i].compareTo("-h"      )==0) {
                System.out.println("usage:[-h] [-o OUTPUT]");
                System.out.println("Script to get the interface definition of IDL in the Interface Repository");
                System.out.println("optional arguments:\n" +
                        "                -h, --help            show this help message and exit\n" +
                        "                -o OUTPUT, <Optional> Set output JSON location");
                System.exit(0);

            }
        }

        if (JSON_FILE_PATH.length() == 0) {
            JSON_FILE_PATH = FILE_NAME;
        }

        try {
            String ir = "users/" + System.getenv("USER")  + "/InterfaceRepository";
            String ns = "corbaname::" + System.getenv("NS_HOST")  + ":" + System.getenv("NS_PORT");
            CORBAServices.init(ns, ir);

            new InterfaceRepositoryToJson().start();

            System.exit(0);

        }catch(NotAvailable ex){
            Output.error("Cannot initialiazed CORBA Services. Make sure environment" +
                    " variables 'NS_HOST' and 'NS_PORT' are set.", ex);
            System.exit(0);
        }
    }

    private static Map<Integer, String> getTypeMap() {
        Map<Integer, String> types = new HashMap<>();
        types.put(1, "void");
        types.put(2, "short");
        types.put(3, "long");
        types.put(4, "ushort");
        types.put(5, "ulong");
        types.put(6, "float");
        types.put(7, "double");
        types.put(8, "boolean");
        types.put(9, "char");
        types.put(10, "octet");
        types.put(11, "char");
        types.put(17, "enum");
        types.put(18, "string");
        types.put(27, "string");
        return types;
    }
}


