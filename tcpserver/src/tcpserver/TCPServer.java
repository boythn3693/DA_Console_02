/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tcpserver;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 *
 * @author dongluu
 */
public class TCPServer {
    //Gán Port cố định cho Server
    public static final int _serverPort = 9000;
    
    public static void main(String argv[]) throws Exception {
          ArrayList<ConnectList> _connectList = new ArrayList<>();
          try {
            ServerSocket serverSocket = new ServerSocket(_serverPort);
            System.out.println("Đang chờ kết nối từ Client hoặc Node...");
            DataInputStream is = null;
            DataOutputStream os = null;
            while (true) {
                Socket socket = serverSocket.accept();
                //System.out.println("IP của máy kết nối đến: " + socket.getRemoteSocketAddress());
                is = new DataInputStream(socket.getInputStream());
                os = new DataOutputStream(socket.getOutputStream());
                String _str = is.readUTF();
                // cut chuoi string thanh IP
                String IPAddr = String.valueOf(socket.getRemoteSocketAddress()); 
                String Check_If_Node = _str.substring(0,3);   
                System.out.println("--------------------------------------------");
                System.out.println("Danh sách file từ Node:");
                if(Check_If_Node.equals("Nod")) // ket noi tu Node
                {
                    String _port = _str.substring(4,9); // cut get _port //Nod,2000, tenfile1[,tenfile2[,...]]
                    _str = _str.substring(10); // remove dấu phẩy
                    //System.out.println(_str);
                    List<String> items = Arrays.asList(_str.split(","));
                    Collections.sort(items, new Comparator<String>() {
                        @Override
                        public int compare(String str1, String str2)
                        {
                            return str1.trim().compareTo(str2.trim());
                        }
                    });
                    _str = String.join(",", items);
                    int f_loc = IPAddr.lastIndexOf("\\"); //Vị trí đầu
                    int l_loc = IPAddr.lastIndexOf(":"); //Vị trí cuối
                    IPAddr = IPAddr.substring(f_loc+2, l_loc);
                    
                    boolean CheckDupIP = false;//Ip on list
                    for(ConnectList _listKN:_connectList)
                    {
                        if( _listKN._ip.equals(IPAddr) && _listKN._port.equals(_port) && !_str.equals("Disconnect") )
                        {
                            CheckDupIP = true;
                        }
                    }
                    
                    if(CheckDupIP == false)
                    {
                        if(_str.equals("Disconnect")){
                            for(ConnectList _item : _connectList)
                            {
                                if(_item.getIp().equals(IPAddr)&&_item.getPort().equals(_port)){
                                    //index = _connectList.indexOf(_item);
                                    _connectList.remove(_item);
                                    break;
                                }
                            }
                        } else {
                            _connectList.add(new ConnectList(IPAddr, _port,_str));
                        }                       
                    }
                    else
                    {
                        for(ConnectList _listKN : _connectList)
                        {
                            if( _listKN._ip.equals(IPAddr) && _listKN._port.equals(_port) )
                            {
                                _listKN._ip = IPAddr;
                                _listKN._port = _port;
                                _listKN._fileName = _str;
                            }
                        }
                    }
                    
                    if(_str.equals("Disconnect")){
                        _str = "Node đã ngắt kết nối với Server\n";
                    } else {
                        _str = "Node kết nối đến Server thành công!\n";
                    }
                    
                    for(ConnectList _listKN : _connectList)
                    {
                        System.out.println("Địa chỉ IP của Node: "+_listKN._ip+" , Port: "+ _listKN._port);
                        System.out.println("Danh sách File của Node: "+_listKN._fileName+"\n");                       
                    }                   
                }
                else  // từ Client
                { 
                    _str = "";
                    for(ConnectList _listKN : _connectList)
                    {
                        System.out.println("Địa chỉ IP Node: "+_listKN._ip+" , Port: "+ _listKN._port);
                        System.out.println("Danh sách File của Node: "+_listKN._fileName+"\n");
                        if(_str == "")
                        {
                             _str = "IP Node: "+_listKN._ip+ " , Port: " +_listKN._port+"\n";
                             _str = _str + "Tên File: "+_listKN._fileName+ "\n";
                        }                     
                        else {
                             _str = _str + "IP Node: "+_listKN._ip+":" +_listKN._port+ "\n";
                             _str = _str + "Tên File: "+_listKN._fileName+ "\n";
                        } 
                    }
                    if( _str.equals("")){
                        _str = "Client kết nối đến Server thành công!\n";
                    }
                }
                                            
                os.writeUTF(_str);
                os.flush();
                os.close();
                is.close();
                
                System.out.println("--------------------------------------------");
            }
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }
}
