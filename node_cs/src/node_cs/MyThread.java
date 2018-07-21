/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package node_cs;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;
import static node_cs.Node_CS._nodePort;
import static node_cs.Node_CS._serverIP;
import static node_cs.Node_CS._serverPort;

/**
 *
 * @author boythn3693
 */
public class MyThread extends Thread {
    public static boolean _isClose = false;
    
    @Override
    public void run(){
        try {
            Scanner sc = new Scanner(System.in);
            char ch = sc.next().charAt(0);
            if(ch=='0'){
                String _str = "Nod," + _nodePort + ", Disconnect"; // Node 
                Socket sk = new Socket(_serverIP, _serverPort);
                DataOutputStream os = new DataOutputStream(sk.getOutputStream());
                DataInputStream is = new DataInputStream(sk.getInputStream());

                os.writeUTF(_str);
                os.flush();

                String result = is.readUTF();
                System.out.println(result);
                is.close();
                os.close();
                sk.close();
                System.out.println("==> Node is stopped");
                System.exit(0);
            }            
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }
}
