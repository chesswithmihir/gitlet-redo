package com.mihirm.gitlet;

//import netscape.javascript.JSObject;
//import org.json.JSONArray;
import java.util.Date;
import java.util.Scanner;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
//import org.w3c.dom.ls.LSOutput;

import java.io.*;
//import java.sql.Array;


public class Main {

    public static void main(String[] args) throws Exception {
        if (args[0].equals("init")) {
            gitInit();
        } else if (args[0].equals("add")) {
            gitAdd(args);
        } else if (args[0].equals("commit")) {
            gitCommit(args);
        } else if (args[0].equals("checkout")) {
            gitCheckout(args);
        } else if (args[0].equals("log")) {
            gitLog(args);
        } else {
            System.out.println("Please enter a valid function call");
        }
    }


    /**
     * GIT FUNCTIONS
     * */
    public static void gitInit() throws IOException, ParseException {
        File theDir = new File("./.gitlet");
        if (!theDir.exists()) {
            theDir.mkdirs();
            // Also create all JSON files where they need to go
            // Maybe have a ./.git/json folder to place all the .json files
            File JSONFolder = new File("./.gitlet/json");
            JSONFolder.mkdir();

            // commits.json
            File commits = new File("./.gitlet/json/commits.json");
            commits.createNewFile();
            initCommitsJSON();

            // version.json
            File version = new File("./.gitlet/json/version.json");
            version.createNewFile();
            writeVersionJSON(new JSONObject());

            //add.json
            File add = new File("./.gitlet/json/add.json");
            add.createNewFile();
            writeAddJSON(new JSONObject());


            // Files directory
            File filesTracked = new File("./.gitlet/files/");
            filesTracked.mkdir();

            // head file
            File head = new File("./.gitlet/json/head");
            head.createNewFile();
            writeHead("null\n");
            String[] commit = new String[] {"commit", "Initializing new git repository."};
            gitCommit(commit);
        } else {
            System.out.println("This directory is already initialized by git");
        }
    }


    public static void gitAdd(String[] args) throws IOException, ParseException {
        if (args.length != 2) {
            System.out.println("Incorrect statement, must follow 'git add filename'.");
        } else {
            String stringname = args[1];
            File filename = new File(stringname);
            if (!filename.exists()) {
                System.out.println("Cannot add file that does not exist.");
            } else {
                JSONObject addedFiles = readAddJSON();
                Scanner scanner = new Scanner(filename);
                String contents = "";
                while (scanner.hasNextLine()) {
                    contents += scanner.nextLine();
                }
                System.out.println(contents);
                addedFiles.put(stringname, contents);
                writeAddJSON(addedFiles);
            }
        }
    }

    public static void gitCommit(String[] args) throws IOException, ParseException {
        // TODO: git commit "committing changes to hello.txt"
        JSONArray listOfCommits = readCommitsJSON();
        if (args.length > 2) {
            System.exit(2);
        }

        JSONObject versions = readVersionJSON();
        JSONObject added = readAddJSON();

        // iterate through added
        for (Object keyStr : added.keySet()) {
            System.out.println("key: " + keyStr + ", value " + added.get(keyStr));
            if (versions.containsKey(keyStr)) {
                int versionNum = ((Long) versions.get(keyStr)).intValue();
                versionNum++;
                versions.put(keyStr, (Integer) versionNum);
            } else {
                Integer temp = 1;
                versions.put(keyStr, (Integer) temp);

            }
        }


        String message = args[1];
        JSONObject commitObj = new JSONObject();
        String date = new Date().toString();
        String newId = Utils.sha1(date);
        commitObj.put("ID", newId);
        commitObj.put("Date", (date));
        commitObj.put("Parent", readHead());
        commitObj.put("Message", message);
        commitObj.put("Files", versions);
        listOfCommits.add(commitObj);
        writeCommitsJSON(listOfCommits);

        // checking version nums
        for (Object keyStr : versions.keySet()) {
            System.out.println("key: " + keyStr + ", value " + versions.get(keyStr));
            if (versions.get(keyStr) instanceof Integer) {
                writeToFile((String) keyStr, (Integer) versions.get(keyStr), (String) added.get(keyStr));
            }
        }

        //write new contents
        writeVersionJSON(versions);
        writeAddJSON(new JSONObject());
        writeHead(newId);
        //clear remove hashmap?


    }


    public static void writeToFile(String file, Integer versionNum, String contents) throws FileNotFoundException {
        String filename = versionNum + file;
        PrintWriter pw = new PrintWriter("./.gitlet/files/" + filename);
        pw.write(contents);
        pw.flush();
        pw.close();
    }

    public static void gitCheckout(String[] args) throws IOException, ParseException {
        if (args.length == 3) {
            if (args[1].equals("--")) {
                gitCheckoutFile(args);
            } else {
                System.out.println("Invalid call to git checkout");
                System.exit(1);
            }
        }
    }

    public static void gitCheckoutFile(String[] args) throws IOException, ParseException {
        String stringname = args[2];
        JSONObject version = readVersionJSON();
        int verNum = ((Long) version.get(stringname)).intValue();
        System.out.println("Version Number: " + verNum);
        String trackedName = verNum + stringname;
        File tracked = new File("./.gitlet/files/" + trackedName);
        File filename = new File(stringname);

        String contents = "";
        Scanner scanTracked = new Scanner(tracked);
        while (scanTracked.hasNextLine()) {
            contents += scanTracked.nextLine();
        }
        System.out.println("Contents of " + trackedName + ": " + contents);

        PrintWriter writer = new PrintWriter(filename);
        writer.print(contents);
        writer.flush();
        writer.close();
    }


    public static void gitLog(String[] args) throws IOException, ParseException {
        String currentHead = readHead();
        String parentHead = "";
        JSONArray listOfCommits = readCommitsJSON();

        // TODO remove temp
        System.out.println("Log Of Commits");
        System.out.println("-----------------");
        while (!currentHead.equals("null")) {
            for (int i = 0; i < listOfCommits.size(); i++) {
                JSONObject currCommit = (JSONObject) listOfCommits.get(i);
                if (currCommit.get("ID").equals(currentHead)) {
                    System.out.println("Commit ID: " + currCommit.get("ID"));
                    System.out.println("Date: " + currCommit.get("Date"));
                    System.out.println("Message: " + currCommit.get("Message"));
                    System.out.println("-----------------");
                    currentHead = (String) currCommit.get("Parent");
                    break;
                }
            }
        }
    }

    /**
     * JSON FUNCTIONS
     * */

    public static JSONObject readAddJSON() throws IOException, ParseException {
        Object obj = new JSONParser().parse(new FileReader("./.gitlet/json/add.json"));
        return (JSONObject) obj;
    }

    public static void writeAddJSON(JSONObject jo) throws FileNotFoundException {
        PrintWriter pw = new PrintWriter("./.gitlet/json/add.json");
        pw.write(jo.toJSONString());
        pw.flush();
        pw.close();
    }

    public static JSONArray readCommitsJSON() throws IOException, ParseException {
        Object obj = new JSONParser().parse(new FileReader("./.gitlet/json/commits.json"));
        return (JSONArray) obj;
    }

    public static void initCommitsJSON() throws FileNotFoundException {
        PrintWriter pw = new PrintWriter("./.gitlet/json/commits.json");
        pw.write(new JSONArray().toJSONString());
        pw.flush();
        pw.close();
    }


    public static void writeCommitsJSON(JSONArray ja) throws FileNotFoundException {
        PrintWriter pw = new PrintWriter("./.gitlet/json/commits.json");
        pw.write(ja.toJSONString());
        pw.flush();
        pw.close();
    }


    public static JSONObject readVersionJSON() throws IOException, ParseException {
        Object obj = new JSONParser().parse(new FileReader("./.gitlet/json/version.json"));
        return (JSONObject) obj;
    }


    public static void writeVersionJSON(JSONObject jo) throws IOException, ParseException {
        PrintWriter pw = new PrintWriter("./.gitlet/json/version.json");
        pw.write(jo.toJSONString());
        pw.flush();
        pw.close();
    }

    public static String readHead() throws FileNotFoundException {
        Scanner scanner = new Scanner(new File("./.gitlet/json/head"));
        String headID = scanner.nextLine();
        return headID;
    }

    public static void writeHead(String newID) throws FileNotFoundException {
        File file = new File("./.gitlet/json/head");
        PrintWriter writer = new PrintWriter(file);
        writer.print(newID);
        writer.flush();
        writer.close();
    }
}
