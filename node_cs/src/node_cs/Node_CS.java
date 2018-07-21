/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package node_cs;

import java.io.ByteArrayOutputStream;
import java.util.Scanner;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 *
 * @author boythn3693
 */
public class Node_CS {

    //F:\\HocTap\\HCDH\\HK2\\MMTNC\\MMT\\Node_Test\\Node + 1_2_3
    public static String _Paths = "F:\\HocTap\\HCDH\\HK2\\MMTNC\\MMT\\Node_Test\\Node2";
    public static int _serverPort = 9000;
    public static int _nodePort = 2000;
    public static String _serverIP = "127.1.0.1";
    public static String _nodeIP = "127.1.0.1";
    private static final int PIECES_OF_FILE_SIZE = 1024;
    
    //------------------------------------------------------------------------------------
    private static int totalTransferred = 0;
    private static final double previousTimeElapsed = 0;
    private static final int previousSize = 0;
    private static final int sendRate = 100;
    private static String hostName;
    private static int port;
    private static String fileName;
    private static String destFileName;
    private static StartTime timer = null;
    private static int retransmitted = 0;
    //------------------------------------------------------------------------------------
    
    static Map<String, String> map = new HashMap<String, String>();
    
    private static String[] getListFile() {
        File dir = new File(_Paths);
        if (dir.exists()) {
            String[] files = dir.list();
            return files;
        }
        return null;
    }

    private static String[] getListFile(String _path) {
        File dir = new File(_path);
        if (dir.exists()) {
            String[] files = dir.list();
            return files;
        }
        return null;
    }
    
    public static void setUp() throws IOException {
        try {       
            DatagramSocket socket = new DatagramSocket(_nodePort);
            byte[] receivedData = new byte[PIECES_OF_FILE_SIZE];
            //File file;
            //while (true) {
                DatagramPacket pk = new DatagramPacket(receivedData, receivedData.length);
                socket.receive(pk);
                InetAddress inetAddress = InetAddress.getLocalHost();
                System.out.println("Đã nhận yêu cầu tải tập tin từ Client !");     
                setPort(pk.getPort());
                fileName = new String(pk.getData());
                setFileName(fileName);
                setDestFile(_Paths + "\\" + fileName);
                // nhận kết nối từ client
                String saveFileAs = getDestFileName();
                byte[] saveFileAsData = saveFileAs.getBytes("UTF-8");

                System.out.println("Bắt đầu gửi File!");
                DatagramPacket fileStatPacket = new DatagramPacket(saveFileAsData, saveFileAsData.length, inetAddress, getPort());
                socket.send(fileStatPacket);                
                // Create a byte array to store file
                //InputStream inFromFile = new FileInputStream(file);

                //File file = new File(getDestFileName());
                //System.out.println(file.length());
                int file_length = Integer.parseInt(map.get(getFileName().trim()));                
                //byte[] fileByteArray = new byte[(int)file.length()];
                byte[] dataSend = new byte[file_length];
                FileInputStream fis = new FileInputStream(getDestFileName().trim());
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                fis.read(dataSend);
                baos.write(dataSend);
                

                startTimer();
                beginTransfer(socket, dataSend, inetAddress);
                String finalStatString = getFinalStatistics(dataSend, retransmitted);
                sendServerFinalStatistics(socket, inetAddress, finalStatString);
            //}
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    private static void sendServerFinalStatistics(DatagramSocket socket, InetAddress address, String finalStatString) throws UnsupportedEncodingException {
        byte[] bytesData;
        // convert string to bytes so we can send
        bytesData = finalStatString.getBytes("UTF-8");
        DatagramPacket statPacket = new DatagramPacket(bytesData, bytesData.length, address, getPort());
        try {
            socket.send(statPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void beginTransfer(DatagramSocket socket, byte[] fileByteArray, InetAddress address) throws IOException {
        int sequenceNumber = 0;
        boolean flag;
        int ackSequence = 0;
        System.out.println("Dung lượng file: "+fileByteArray.length);
        for (int i = 0; i < fileByteArray.length; i = i + 1021) {
            sequenceNumber += 1;
            // Create message
            byte[] message = new byte[PIECES_OF_FILE_SIZE];
            message[0] = (byte) (sequenceNumber >> 8);
            message[1] = (byte) (sequenceNumber);

            if ((i + 1021) >= fileByteArray.length) {
                flag = true;
                message[2] = (byte) (1);
            } else {
                flag = false;
                message[2] = (byte) (0);
            }

            if (!flag) {
                System.arraycopy(fileByteArray, i, message, 3, 1021);
            } else { // If it is the last message
                System.arraycopy(fileByteArray, i, message, 3, fileByteArray.length - i);
            }

            int randomInt = shouldThisPacketBeSent();

            DatagramPacket sendPacket = new DatagramPacket(message, message.length, address, getPort());
            if (randomInt <= sendRate) {
                socket.send(sendPacket);
            }

            totalTransferred = gatherTotalDataSentSoFarStatistic(sendPacket);

            if (Math.round(totalTransferred / 1000) % 50 == 0) {
                PrintFactory.printCurrentStatistics(totalTransferred, previousSize, timer, previousTimeElapsed);
            }

            System.out.println("Sent: Sequence number = " + sequenceNumber);

            // For verifying the the packet
            boolean ackRec;

            // The acknowledgment is not correct
            while (true) {
                // Create another packet by setting a byte array and creating
                // data gram packet
                byte[] ack = new byte[2];
                DatagramPacket ackpack = new DatagramPacket(ack, ack.length);

                try {
                    // set the socket timeout for the packet acknowledgment
                    socket.setSoTimeout(50);
                    socket.receive(ackpack);
                    ackSequence = ((ack[0] & 0xff) << 8) + (ack[1] & 0xff);
                    ackRec = true;
                }
                // we did not receive an ack
                catch (SocketTimeoutException e) {
                    System.out.println(e.getMessage());
                    System.out.println("Socket timed out waiting for the ");
                    ackRec = false;
                }

                // everything is ok so we can move on to next packet
                // Break if there is an acknowledgment next packet can be sent
                if ((ackSequence == sequenceNumber) && (ackRec)) {
                    System.out.println("Ack received: Sequence Number = " + ackSequence);
                    break;
                }

                // Re send the packet
                else {
                    socket.send(sendPacket);
                    System.out.println("Resending: Sequence Number = " + sequenceNumber);
                    // Increment retransmission counter
                    retransmitted += 1;
                }
            }
        }
    }

    private static int gatherTotalDataSentSoFarStatistic(DatagramPacket sendPacket) {
        totalTransferred = sendPacket.getLength() + totalTransferred;
        totalTransferred = Math.round(totalTransferred);

        return totalTransferred;
    }

    private static int shouldThisPacketBeSent() {
        Random randomGenerator = new Random();

        return randomGenerator.nextInt(100);
    }

    private static String getFinalStatistics(byte[] fileByteArray, int retransmitted) {

        double fileSizeKB = (fileByteArray.length) / PIECES_OF_FILE_SIZE;
        double transferTime = timer.getTimeElapsed() / 1000;
        double fileSizeMB = fileSizeKB / 1000;
        double throughput = fileSizeMB / transferTime;

        System.out.println("File " + getFileName() + " đã được nhận");
        PrintFactory.printSpace();
        PrintFactory.printSpace();
        System.out.println("Thống kê chuyển file");
        PrintFactory.printSeperator();
        System.out.println("File " + getFileName() + " đã nhận thành công.");
        System.out.println("Kích thước của file: " + totalTransferred / 1000 + " KB");
        System.out.println("Tổng số được chuyển: " + totalTransferred / 1000 / 1000 + " MB");
        System.out.println("Thời gian chuyển: " + timer.getTimeElapsed() / 1000 + "s");
        System.out.printf("Thông lượng là %.2f MB mỗi giây\n", +throughput);
        System.out.println("Số lần truyền lại: " + retransmitted);
        PrintFactory.printSeperator();

        return "Kích thước File: " + fileSizeMB + "mb\n"
                + "Thông lượng: " + throughput + " Mbps"
                + "\nTổng thời gian chuyển: " + transferTime + "s";
    }

    private static void startTimer() {
        timer = new StartTime();
    }


    private static int getPort() {
        return port;
    }

    private static void setPort(int passed_port) {
        port = passed_port;
    }

    private static String getFileName() {
        return fileName;
    }

    private static void setFileName(String passed_file_name) {
        fileName = passed_file_name;
    }

    private static void setDestFile(String passed_dest_file) {
        destFileName = passed_dest_file;
    }

    private static String getDestFileName() {
        return destFileName;
    }

    private static String getHostname() {
        return hostName;
    }

    private static void setHostname(String passed_host_name) {
        hostName = passed_host_name;
    }
    
    //START UDP

    //END UDP
    /**
     * @param argv the command line arguments
     */
    public static void main(String argv[]) throws Exception {
        Scanner sc = new Scanner(System.in);
        System.out.println("------------------Kết nối với Server------------------------------");
        System.out.print("Nhập IP Server: ");
        _serverIP = sc.nextLine();
        System.out.print("Nhập Port Server: ");
        _serverPort = sc.nextInt();
        System.out.println("------------------------------------------------------------------");        
        
        char ch = '0';
        System.out.println("Nhập: 0 => Thoát ");
        System.out.println("\t1 => Chọn đường dẫn cố định: " + _Paths);
        System.out.println("\t2 => Nhập đường dẫn đến thư mục chứa file.");
        System.out.print("Nhập: ");
        ch = sc.next().charAt(0);
        String[] files = null;
        
        do {
            switch (ch) {
                case '1':
                    files = getListFile(); //Lấy danh sách file trên Node
                    break;
                case '2':
                    System.out.print("Tạo Port cho Node: ");
                    sc.nextLine();
                    _nodePort =  Integer.parseInt(sc.nextLine());
                    System.out.print("Nhập Đường dẫn đến thư mục chứa file: ");
                    //sc.nextLine();
                    _Paths = sc.nextLine();
                    files = getListFile(_Paths);
                    break;
                default:
                    System.out.println("Vui lòng nhập lại!");
            }
            if (ch == '1' || ch == '2') {
                ch = '0';
            } else {
                System.out.print("Nhập: ");
                ch = sc.next().charAt(0);
            }
        } while (ch != '0');

        if (files == null) {
            System.out.println("Đường dẫn này không đúng! ");
        } else {
            try {
                String _str = "Nod," + _nodePort; // Node or Client 
                for (String file : files) {
                    boolean flag;
                    flag = file.contains(".");
                    if (flag == true) {
                        File f = new File(_Paths+"\\"+file);
                        map.put(file.trim(), String.valueOf(f.length()).trim());
                        _str = _str + ", " + file;
                    }
                }
                
                //System.out.println(PIECES_OF_FILE_SIZE);
                
                Socket socket = new Socket(_serverIP, _serverPort);
                DataOutputStream os = new DataOutputStream(socket.getOutputStream());
                DataInputStream is = new DataInputStream(socket.getInputStream());

                os.writeUTF(_str);
                os.flush();

                String result = is.readUTF();
                System.out.println(result);
                is.close();
                os.close();
                socket.close();
            } catch (IOException ex) {
                System.out.println(ex.getMessage());
            }

            // Mở kết nối UDP chờ Client kết nối đến
            System.out.println("Đang chờ Client kết nối đến, Nhấn phím 0 để thoát!");
            
            //Tạo thread mới để nhận lệnh nếu Disconnect với Server
            MyThread myThread = new MyThread();
            myThread.start();
            //Setup nhận chuyển file
            setUp();
        }
    }            
}