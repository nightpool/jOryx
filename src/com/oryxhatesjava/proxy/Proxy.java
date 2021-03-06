/* oryx-hates-java
 * Copyright (C) 2011-2012 Furyhunter <furyhunter600@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.oryxhatesjava.proxy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * <p>
 * TODO document this type
 * </p>
 * <p>
 * Started Feb 27, 2011
 * </p>
 * 
 * @author Furyhunter
 */
public class Proxy implements Runnable {
    
    private ServerSocket proxyServerSocket = null;
    private Socket clientSocket = null;
    private Socket serverSocket = null;
    
    //86d344c1fad4baf1f5cdd103d1
	public static byte[] CLIENTKEY = new byte[] { (byte) 0x86, (byte) 0xd3,
			0x44, (byte) 0xc1, (byte) 0xfa, (byte) 0xd4, (byte) 0xba,
			(byte) 0xf1, (byte) 0xf5, (byte) 0xcd, (byte) 0xd1, 0x03,
			(byte) 0xd1 };
	// 10c5dc7a8cf87bb18feab6c71f
	public static byte[] SERVERKEY = new byte[] { 0x10, (byte) 0xc5,
			(byte) 0xdc, 0x7a, (byte) 0x8c, (byte) 0xf8, 0x7b, (byte) 0xb1,
			(byte) 0x8f, (byte) 0xea, (byte) 0xb6, (byte) 0xc7, 0x1f };

    /*
     * (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        /*
         * An httpd server needs to be running and hosts needs to point to it in
         * order to fake the server list and make it connect to the localhost;
         * the game connects to the proxy and the proxy connects to the actual
         * game server
         */

        try {
            // Set up server socket
            proxyServerSocket = new ServerSocket(2050);
            System.out.println("Socket opened");
        } catch (UnknownHostException e) {
            System.err.println("This should absolutely NEVER happen. WTF???");
            return;
        } catch (IOException e) {
            System.err.println("Failed to bind 2050: " + e.getMessage());
            return;
        }
        
        // Set up flash policy server
        Thread flashPolicyServer = new Thread(new FlashPolicyServer(),
                "FlashPolicyServer");
        flashPolicyServer.setDaemon(true);
        flashPolicyServer.start();
        
        try {
            while (!proxyServerSocket.isClosed()) {
                System.out.println("Waiting for client connection...");
                
                clientSocket = proxyServerSocket.accept();
                String ipString = clientSocket.getInetAddress()
                        .getHostAddress();
                System.out.println("Client connected to proxy: " + ipString);
                
                System.out.println("Connecting to USEast3 for " + ipString);
                //serverSocket = new Socket("ec2-50-19-47-160.compute-1.amazonaws.com", 2050); // "USEast3"
                serverSocket = new Socket("50.19.47.160", 2050); //use ip instead of host due to hosts block
                System.out.println("Connected to USEast3 for " + ipString);
                
                SiphonHose clientHose = new SiphonHose(
                        clientSocket.getInputStream(),
                        serverSocket.getOutputStream(), CLIENTKEY, "c2s");
                SiphonHose serverHose = new SiphonHose(
                        serverSocket.getInputStream(),
                        clientSocket.getOutputStream(), SERVERKEY, "s2c");
                
                Thread clientThread = new Thread(clientHose, "ClientDaemon");
                Thread serverThread = new Thread(serverHose, "ServerDaemon");
                
                clientThread.start();
                serverThread.start();
                System.out.println("Done setting up siphon hoses, NEEEXT");
            }
        } catch (IOException e) {
            try {
                proxyServerSocket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }
    
    /**
     * <p>
     * Starts the proxy as an application.
     * </p>
     * 
     * @param args
     */
    public static void main(String[] args) {
        Proxy p = new Proxy();
        p.run();
    }
    
}
