/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Client;

/**
 *
 * @author LeThanhLoi
 */
import java.io.*;
import java.net.*;
import java.util.Date;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client {

    final String serverHost = "localhost";

    public Socket socketOfClient = null;
    private PrintWriter writer = null;
    private BufferedReader reader = null;

    public void connectToServer()throws UnknownHostException, IOException{
       
            socketOfClient = new Socket("localhost",7777);
            writer = new PrintWriter(socketOfClient.getOutputStream(),true);
            reader = new BufferedReader(new InputStreamReader(socketOfClient.getInputStream()));
       
    }
    
    public void writeOut(String output){
		writer.println(output);
	}
	
	public BufferedReader getRead(){
		return this.reader;
	}
	
	public String readInput() throws IOException{
		return reader.readLine();
	}

	public void disconnect() throws IOException{
		this.writer.close();
		this.reader.close();
		this.socketOfClient.close();
	}
        
}
