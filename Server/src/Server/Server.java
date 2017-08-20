/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Server;

/**
 *
 * @author LeThanhLoi
 */
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
//import Server.ServiceThread

public class Server {

    private static int Max_client = 2;
     public static Properties user;
    public static int clientNumber;
    public static String ROOT;

    public static void main(String args[]) throws IOException {
        ServerSocket listener = null;
        System.out.println("Server is waiting to accept user...");

        user = new Properties();
        FileInputStream file = new FileInputStream("src/File/user.properties");
        user.load(file);
        ROOT="home";
        
        listener = new ServerSocket(7777);
        clientNumber = 0;
        try {
            while (true) {

                Socket socketOfServer = listener.accept();
                clientNumber++;
                System.out.println("Number Client: " + clientNumber);
                if (clientNumber <= Max_client) {
                    ServiceThread thread = new ServiceThread(socketOfServer, clientNumber);
                    thread.start();
                } else if(clientNumber > Max_client){
                    PrintWriter write = new PrintWriter(
                            socketOfServer.getOutputStream());
                    write.println("Server overloading!");
                    write.close();
                    socketOfServer.close();
                    clientNumber--;
                }

            }
        } catch (Exception e) {
            clientNumber--;
            e.printStackTrace();
        } finally {
            listener.close();
            System.exit(1);
        }
    }
    
    

}
