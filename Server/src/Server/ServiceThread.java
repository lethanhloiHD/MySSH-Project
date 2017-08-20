/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Server;

import com.mysql.cj.fabric.xmlrpc.Client;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author LeThanhLoi
 */
public class ServiceThread extends Thread {

    private int clientNumber;
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private String userDirectory;
    private ArrayList<String> wd;

    public ServiceThread(Socket socketOfServer, int clientNumber) {

        this.clientNumber = clientNumber;
        this.socket = socketOfServer;
    }

    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(this.socket.getOutputStream(), true);
            boolean login = false;
            while (!login) {

                if (checkUser()) {
                    login = true;
                    writer.println("Sucessfully!");
                    processRequest();
                } else {
                    writer.println("Login failed! Press 'y' to login again or any to exit");
                    if (!reader.readLine().contains("y")) {
                        login = true;
                    }
                }
            }
            processRequest();
            writer.close();
            reader.close();
            socket.close();
            Server.clientNumber--;

        } catch (IOException e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }

    public boolean checkUser() throws IOException {
        writer.println("username:");
        String username = reader.readLine().trim();
        System.out.println("\n" + username);
        System.out.println(Server.user.getProperty(username));
        writer.println("password: ");
        String password = reader.readLine().trim();
        if (password.equals("")) {
            return false;
        }
        if (Server.user.getProperty(username, "").equals(password)) {
            userDirectory = Server.ROOT;
            File fUser = new File(userDirectory);
            fUser.mkdir();
            this.wd = new ArrayList<>();
            return true;
        } else {
            return false;
        }

    }

    public void processRequest() throws IOException {
        String inputLine, outputLine;
        while ((inputLine = reader.readLine().trim()) != null) {
            if (inputLine.contains("exit")) {
                writer.println("GOODBYE!");
                break;
            }

            String[] pices = inputLine.split(" ");
            HashMap<String, Object> command = new HashMap<>();
            command.put("name", pices[0]);
            command.put("option", "");
            ArrayList<String> param = new ArrayList<>();

            for (int i = 1; i < pices.length; i++) {
                if (pices[i].startsWith("-")) {
                    command.put("option", command.get("option") + pices[i]);
                } else if (!pices[i].equals("")) {
                    param.add(pices[i]);
                }
            }

            command.put("param", param);

            outputLine = this.process(command);
            writer.println(outputLine);
        }
    }

    public String process(HashMap<String, Object> command) {
        String result = "";
        try {
            String option = (String) command.get("option");
            ArrayList<String> param = (ArrayList<String>) command.get("param");
            switch ((String) command.get("name")) {

                case "rmdir":
                    result = rmdir(param.get(0));
                    break;
                case "del":
                    if (del(param.get(0))) {
                        result = "The file has been delete";
                    } else {
                        result = "Error !";
                    }
                    break;
                case "mkdir":
                    if (mkdir(param.get(0))) {
                        result = "Created the folder successful !";
                    } else {
                        result = "Error !";
                    }
                    break;
                case "time":
                    result = time();

                    break;
                case "pwd":
                    result = pwd();
                    break;
                case "cd":

                    if (cd(param.get(0))) {
                        result = "Directory has change";
                    } else {
                        result = "Error";
                    }
                    break;
                case "ls":
                    if (param.size() > 0) {
                        result = ls(option, param.get(0));
                    } else {
                        result = ls(option, null);
                    }
                    break;
                case "clone":
                    result = pull(param.get(0));
                    break;
                case "zip":
                    result = zip(param.get(0));
                    break;
                case "copy":
                    copy(param.get(0), param.get(1));
                    break;
                case "echo":
                    result = echo(param.get(0), param.get(1));
                    break;
                default:
                    result = "command not found!!!";
                    break;

            }
        } catch (Exception e) {
            e.printStackTrace();
            result = "Error: command not found!!!";
        }

        return result;
    }

    private String getPath(String path) {

        if (path == null) {
            return this.userDirectory + pwd();
        }
        path = path.trim();
        if (path.startsWith("/")) {
            return this.userDirectory + path;
        } else {
            String[] pices = path.split("/");
            ArrayList<String> pathTemp = (ArrayList<String>) this.wd.clone();

            for (int i = 0; i < pices.length; i++) {
                if (pices[i].equals(".")) {
                    continue;
                }
                if (pices[i].equals("..")) {
                    if (pathTemp.size() == 0) {
                        return null;
                    }
                    pathTemp.remove(pathTemp.size() - 1);
                    continue;
                }
                pathTemp.add(pices[i]);
            }

            String realPath = this.userDirectory;
            for (int i = 0; i < pathTemp.size(); i++) {
                realPath += "/" + pathTemp.get(i);
            }
            return realPath;
        }

    }

    private boolean del(String path) {
        path = this.getPath(path);
        File file = new File(path);
        return file.delete();
    }

    private String rmdir(String path) {
        path = this.getPath(path);
        File dir = new File(path);
        if (dir.isDirectory()) {
            if (dir.list().length == 0) {
                dir.delete();
            }
            return "Done !";
        } else {
            return "Error !!!";
        }
    }

    private boolean mkdir(String path) {
        path = this.getPath(path);
        File newFolder = new File(path);
        return newFolder.mkdir();
    }

    private String time() {
        Date now = new Date();
        return now.toLocaleString();

    }

    private String pwd() {
        String result = "";
        for (int i = 0; i < wd.size(); i++) {
            result += "/" + wd.get(i);
        }
        if (result.equals("")) {
            return "/";
        }
        return result;
    }

    private String ls(String option, String path) {
        String realPath = this.getPath(path);
        File dir = new File(realPath);
        String result = "";
        if (!dir.isDirectory()) {
            return "Directory not exist!";
        } else {

            SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss'-'dd/MM/yyyy");

            if (option.contains("-name")) {
                for (final File fileEntry : dir.listFiles()) {
                    result += String.format("%-20s", fileEntry.getName());
                }

            } else if (option.contains("")) {
                for (final File fileEntry : dir.listFiles()) {
                    result += String.format("%-20s", fileEntry.getName());
                    Date lastModified = new Date(fileEntry.lastModified());
                    result += " " + String.format("%-20s", dateFormat.format(lastModified)) + "  ";
                }
            }
        }

        return result;
    }

    private boolean cd(String path) {
        if (path.startsWith("/")) {
            File dir = new File(this.userDirectory + path);
            if (dir.isDirectory()) {
                wd.clear();
                path = path.substring(1);
                String[] pices = path.split("/");
                for (int i = 0; i < pices.length; i++) {
                    this.wd.add(pices[i]);
                }
                return true;
            } else {
                return false;
            }
        } else {
            String[] pices = path.split("/");
            ArrayList<String> pathTemp = (ArrayList<String>) wd.clone();

            for (int i = 0; i < pices.length; i++) {
                if (pices[i].equals(".")) {
                    continue;
                }
                if (pices[i].equals("..")) {
                    if (pathTemp.size() == 0) {
                        return false;
                    }
                    pathTemp.remove(pathTemp.size() - 1);
                    continue;
                }
                pathTemp.add(pices[i]);
            }

            String newPath = this.userDirectory;
            for (int i = 0; i < pathTemp.size(); i++) {
                newPath += "/" + pathTemp.get(i);
            }
            File newDir = new File(newPath);
            if (newDir.isDirectory()) {
                this.wd = pathTemp;
                return true;
            } else {
                return false;
            }

        }

    }

    private String pull(String str) throws IOException {
        String s = getPath(str);
        File fSource = new File(s);
        String fName = fSource.getName();
        long fSize = fSource.length();
        if (!fSource.isFile()) {
            this.writer.println(fName + " " + fSize);
            return "Error: This is not file!";
        }

        BufferedReader tempFile = new BufferedReader(new FileReader(s));
        char[] buffer = new char[1024];
        int length;
        writer.println(fName + " " + fSize);
        while ((length = tempFile.read(buffer)) > 0) {
            writer.write(buffer, 0, length);
        }
        tempFile.close();

        return "Done";
    }

    public String zip(String str1) {
        String path = getPath(str1);
        File file = new File(path);
        String[] c = file.getName().split("\\.");
        String str2 = file.getParent() + "/" + c[0] + ".zip";

        if (file.isDirectory()) {
            try {
                FolderZiper.zipFolder(path, str2);
                return "Done";
            } catch (Exception ex) {
                Logger.getLogger(ServiceThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (file.isFile()) {
            try {
                FolderZiper.zipFile(path, str2);
                return "Done";
            } catch (Exception ex) {
                Logger.getLogger(ServiceThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return "Error";
    }

    private String copy(String src1, String dest1) throws IOException {
        String src2 = getPath(src1);
        File src = new File(src2);
        String dest2 = getPath(dest1);
        File dest = new File(dest2);

        if (src.isDirectory()) {
            if (!dest.exists()) {
                dest.mkdir();
            } else {
                dest = new File(dest, src.getName());
                dest.mkdir();
            }
            String files[] = src.list();
            for (String file : files) {
                File srcFile = new File(src, file);
                File destFile = new File(dest, file);

                copy(srcFile.getName(), destFile.getName());
            }

        } else {
            InputStream in = new FileInputStream(src);
            OutputStream out = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            in.close();
            out.close();
            return "Done !";
        }
        return "Error !";
    }

    private String echo(String content, String path) throws IOException {

        if (path != null) {
            String filePath = getPath(path);
            File file = new File(filePath);
            if (file.exists()) {
                file.delete();
            }
            FileWriter out = new FileWriter(file);
            out.write(content);
            out.close();
            return "Done !";
        } 
        return content;
    }

}
