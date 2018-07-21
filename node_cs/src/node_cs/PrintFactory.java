/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package node_cs;

/**
 *
 * @author MitsuyoRai
 */
class PrintFactory {
    public PrintFactory() {
    }

    public static void printCurrentStatistics(int totalTransferred, int previousSize, StartTime timer, double previousTimeElapsed) {
        printSpace();
        printSpace();
        printSeperator();
        System.out.println("Thống kê mốc");

        int sizeDifference = totalTransferred / 1000 - previousSize;
        double difference = timer.getTimeElapsed() - previousTimeElapsed;
        double throughput = totalTransferred / 1000 / timer.getTimeElapsed();


        System.out.println("Vừa nhận được: " + sizeDifference + "Kb");
        System.out.println("Đã nhận được " + totalTransferred / 1000 + "Kb");
        System.out.println("Time taken so far: " + timer.getTimeElapsed() / 1000 + " Seconds");
        System.out.println("Thông lượng trung bình :" + throughput + "Mbps");
        System.out.println("Thông lượng cho lần cuối: " + sizeDifference / difference + "Mbps");

        printSeperator();
        printSpace();
        printSpace();

    }

    public static void printSpace() {
        System.out.println();
    }

    public static void printSeperator() {
        System.out.println("------------------------------");
    }
}
