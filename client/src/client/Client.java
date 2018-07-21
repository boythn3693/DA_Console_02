/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

import java.awt.Container;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Random;
import java.util.Scanner;

/**
 *
 * @author ntdat
 */
public class Client {
    public static int _serverPort = 9000;
    public static String _serverIP = "127.1.0.1";
    private static final int PIECES_OF_FILE_SIZE = 1024;
    
    //---------------------------------------------------------------
    private static int totalTransferred = 0;
    private static StartTime timer;
    private static String fileName = "";
    private static String decodedDataUsingUTF82 = null;
    //---------------------------------------------------------------
    
//    private static void setTimeout(Runnable runnable, int delay, String[] argv) throws InterruptedException{
//        Scanner sc = new Scanner(System.in);
//        char ch = sc.next().charAt(0);
//        Thread timeout = new Thread(() -> {
//            try {
//                Thread.sleep(delay);
//                runnable.run();
//                if(ch=='0'){
//                    Client.main(argv);
//                }
//            }
//            catch (Exception e){
//                System.err.println(e);
//            }
//        });
//        timeout.start();
//        if(ch=='0'){
//            timeout.join();
//        }
//    }
    
    private static String getFileName() {
        return fileName;
    }

    private static void setFileName(String passed_file_name) {
        fileName = passed_file_name;
    }
    
    public static void setUp(String _ipNode, int _portNode, String _path, String _fileName) throws IOException {
        //DatagramSocket socket = new DatagramSocket(_portNode);

        byte[] sendData = new byte[PIECES_OF_FILE_SIZE];
        byte[] receiveData = new byte[PIECES_OF_FILE_SIZE];
        
        String sentence = _fileName;
        sendData = sentence.getBytes("UTF-8");
        //Connect đến Node
        DatagramSocket ds = new DatagramSocket();
        InetAddress iaddr = InetAddress.getByName(_ipNode);
        DatagramPacket pk = new DatagramPacket(sendData, sendData.length, iaddr, _portNode);
        ds.send(pk);
        System.out.println("Gửi yêu Node cầu truyền tập tin... ");
        
        //Nhận thông tin xác nhận từ Node khi đã nhận dc file
        DatagramPacket receiveFileNameChoicePacket = new DatagramPacket(receiveData, receiveData.length);
        ds.receive(receiveFileNameChoicePacket);        
        
        try {
            decodedDataUsingUTF82 = new String(receiveData, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        String savedFileName = decodedDataUsingUTF82.trim();
        fileName = savedFileName;
        setFileName(savedFileName);
        //File file = new File(fileName);
        //FileOutputStream outToFile = new FileOutputStream(file);

        // lưu file vào đường dẫn
        String filePath = _path + "\\" + _fileName;
        FileOutputStream outToFile = new FileOutputStream(filePath.trim());
//        byte[] data = receiveFileNameChoicePacket.getData();
//        outToFile.write(data);
//        outToFile.flush();
//        ds.close();
        acceptTransferOfFile(outToFile, ds);

        byte[] finalStatData = new byte[PIECES_OF_FILE_SIZE];
        DatagramPacket receivePacket = new DatagramPacket(finalStatData, finalStatData.length);
        ds.receive(receivePacket);
        printFinalStatistics(finalStatData);
    }
    
    private static void printFinalStatistics(byte[] finalStatData) {
        try {
            String decodedDataUsingUTF8 = new String(finalStatData, "UTF-8");
            PrintFactory.printSpace();
            PrintFactory.printSpace();
            System.out.println("Thống kê chuyển file");
            PrintFactory.printSeperator();
            System.out.println("Tệp đã được lưu: " + getFileName());
            System.out.println("Thống kê chuyển file");
            System.out.println("" + decodedDataUsingUTF8.trim());
            PrintFactory.printSeperator();

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
    
    private static void sendAck(int findLast, DatagramSocket socket, InetAddress address, int port) throws IOException {
        // send acknowledgement
        byte[] ackPacket = new byte[2];
        ackPacket[0] = (byte) (findLast >> 8);
        ackPacket[1] = (byte) (findLast);
        // the datagram packet to be sent
        DatagramPacket acknowledgement = new DatagramPacket(ackPacket,
                ackPacket.length, address, port);
        socket.send(acknowledgement);
        System.out.println("Sent ack: Sequence Number = " + findLast);
    }

    private static void acceptTransferOfFile(FileOutputStream outToFile, DatagramSocket socket) throws IOException {

        // last message flag
        boolean flag;
        int sequenceNumber = 0;
        int findLast = 0;

        while (true) {
            byte[] message = new byte[PIECES_OF_FILE_SIZE];
            byte[] fileByteArray = new byte[1021];

            // Receive packet and retrieve message
            DatagramPacket receivedPacket = new DatagramPacket(message, message.length);
            socket.setSoTimeout(0);
            socket.receive(receivedPacket);

            message = receivedPacket.getData();
            totalTransferred = receivedPacket.getLength() + totalTransferred;
            totalTransferred = Math.round(totalTransferred);

            // start the timer at the point transfer begins
            if (sequenceNumber == 0) {
                timer = new StartTime();
            }

            if (Math.round(totalTransferred / 1000) % 50 == 0) {
                double previousTimeElapsed = 0;
                int previousSize = 0;
                PrintFactory.printCurrentStatistics(totalTransferred, previousSize,
                        timer, previousTimeElapsed);
            }
            // Get port and address for sending acknowledgment
            InetAddress address = receivedPacket.getAddress();
            int port = receivedPacket.getPort();

            // Retrieve sequence number
            sequenceNumber = ((message[0] & 0xff) << 8) + (message[1] & 0xff);
            // Retrieve the last message flag
            // a returned value of true means we have a problem
            flag = (message[2] & 0xff) == 1;
            // if sequence number is the last one +1, then it is correct
            // we get the data from the message and write the message
            // that it has been received correctly
            if (sequenceNumber == (findLast + 1)) {

                // set the last sequence number to be the one we just received
                findLast = sequenceNumber;

                // Retrieve data from message
                System.arraycopy(message, 3, fileByteArray, 0, 1021);

                // Write the message to the file and print received message
                
                
                outToFile.write(fileByteArray);
                System.out.println("Received: Sequence number:" + findLast);

                // Send acknowledgement
                sendAck(findLast, socket, address, port);
            } else {
                System.out.println("Số thứ tự dự kiến: "
                        + (findLast + 1) + " đã nhận được "
                        + sequenceNumber + ". DISCARDING");
                // Re send the acknowledgement
                sendAck(findLast, socket, address, port);
            }

            // Check for last message
            if (flag) {
                outToFile.close();
                break;
            }
        }
    }
    
    public static void main(String argv[]) throws Exception {
        //Setup kết nối đến Server và Node
        boolean _cn_sv = false;
        Scanner scanner = new Scanner(System.in);
        char sc = '0';
        System.out.println("Nhập: 0-> Thoát");
        System.out.println("\t1-> Kết nối đến Server");
        System.out.println("\t2-> Kết nối đến Node");
        System.out.print("Nhập: ");
        sc = scanner.next().charAt(0);
        do {
            switch (sc) {
                case '1':
                    try {
                        if(!_cn_sv){
                            System.out.println("------------------Kết nối với Server------------------------------");
                            scanner.nextLine();
                            System.out.print("Nhập IP Server: ");
                            _serverIP = scanner.nextLine();
                            System.out.print("Nhập Port Server: ");
                            _serverPort = scanner.nextInt();
                            System.out.println("------------------------------------------------------------------"); 
                        }
                            
                        Socket socket = new Socket(_serverIP, _serverPort);
                        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                        dos.writeUTF("Clt"); // clt la client de server phan biet giua node va client
                        dos.flush();
                        DataInputStream dis = new DataInputStream(socket.getInputStream());
                        String result = dis.readUTF();
                        System.out.println(result);
                        dis.close();
                        dos.close();
                        socket.close();
                    } catch (IOException ex) {
                        System.out.println("Server chưa mở kết nối!");
                    }
                    break;
                case '2': // kết nối Server Node 
                    String _ipNode, _path, _fileName;//F:\\HocTap\\HCDH\\HK2\\MMTNC\\MMT\\Client_Nhan_File
                    int _portNode; // ip cua node
                    scanner.nextLine();
                    System.out.print("Nhập IP:   ");
                    _ipNode = scanner.nextLine();
                    System.out.print("Nhập Port: ");
                    _portNode = scanner.nextInt();
                    scanner.nextLine();
                    System.out.print("Nhập Tên:  ");
                    _fileName = scanner.nextLine();
                    
                    System.out.print("Nhập đường dẫn thư mục lưu file: ");
                    _path = scanner.nextLine();
                    
                    setUp(_ipNode, _portNode, _path, _fileName);
                    break;
                default:
                    System.out.println("Vui lòng nhập lại!");
            }
            _cn_sv = true;
            System.out.print("Nhập: ");
            sc = scanner.next().charAt(0);
        } while (sc != '0');
    }
}
