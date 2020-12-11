// App.java
package com.github.username;

import com.sun.jna.Platform;
import java.io.*; // Import the FileWriter class
import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.util.*;
import org.pcap4j.core.BpfProgram.BpfCompileMode;
import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PacketListener;
import org.pcap4j.core.PcapDumper;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;
import org.pcap4j.core.PcapStat;
import org.pcap4j.core.Pcaps;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.UdpPacket;
import org.pcap4j.util.NifSelector;
import org.pcap4j.packet.IcmpV4CommonPacket;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.IpV4Packet.IpV4Header;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.TcpPacket.TcpHeader;
import org.pcap4j.packet.UdpPacket;

public class App {

    static int UDP_number, TCP_number, ICMP_number, OTHER_number = 0;
    static float total_UDP_byte, total_ICMP_byte, total_OTHER_byte, total_byte = 0;
    static double first_pack_time, last_pack_time = 0;
    static boolean first_packet_time, last_packet_time = false;

    static ArrayList < Flow > flowArrList = new ArrayList < Flow > ();

    public static void main(String[] args) throws PcapNativeException, NotOpenException {

        if (args.length == 0) {
            System.out.println("no argument");
            return;
        }
        File theFile = new File(System.getProperty("user.dir") + "/" + args[0]);
        boolean exists = theFile.exists();
        if (!exists) {
            System.out.println("file not found");
            return;
        }


        final PcapHandle handle;

        handle = Pcaps.openOffline(args[0]);


        PacketListener listener = new PacketListener() {
            public void gotPacket(Packet packet) {

                String lines[] = packet.getPayload().toString().split("\\r?\\n");

                if (first_packet_time == false) {
                    first_pack_time = (double) handle.getTimestamp().getTime();
                    first_packet_time = true;
                }
                last_pack_time = (double) handle.getTimestamp().getTime();

                total_byte = total_byte + (float) packet.length();

                if (packet.get(TcpPacket.class) != null) {
                    IpV4Packet ipPacket = packet.get(IpV4Packet.class);
                    IpV4Header ipHeader = ipPacket.getHeader();
                    TcpPacket tcpPacket = packet.get(TcpPacket.class);
                    TcpHeader tcpHeader = tcpPacket.getHeader();

                    try {
                        boolean flowExists = false;
                        String sip = ipHeader.getSrcAddr().getHostAddress();
                        String dip = ipHeader.getDstAddr().getHostAddress();
                        int sport = tcpHeader.getSrcPort().valueAsInt();
                        int dport = tcpHeader.getDstPort().valueAsInt();

                        boolean syn = tcpHeader.getSyn();
                        boolean fin = tcpHeader.getFin();

                        Flow flow_c = new Flow(sip, sport, dip, dport);
                        for (Flow flow: flowArrList) {
                            if (flow.id().equals(flow_c.id())) {
                                flowExists = true;
                                if (syn) flow.foundSYN = true;
                                if (fin) flow.foundFIN = true;

                                flow.setTime((double) handle.getTimestamp().getTime());
                                flow.incCompletedBytes((double) packet.length());
                                flow.incrementPacks();
                                flow.totalByte += (float) packet.length();
                            }
                        }
                        if (flowExists == false) {
                            if (syn) flow_c.foundSYN = true;
                            if (fin) flow_c.foundFIN = true;
                            flow_c.setTime((double) handle.getTimestamp().getTime());
                            flow_c.incCompletedBytes((double) packet.length());
                            flow_c.incrementPacks();
                            flow_c.totalByte += (float) packet.length();
                            flowArrList.add(flow_c);
                        }
                        flowExists = false;

                    } catch (ArrayIndexOutOfBoundsException e) {
                        e.printStackTrace();
                    }

                    TCP_number = TCP_number + 1;
                } else if (packet.get(UdpPacket.class) != null) {
                    UDP_number = UDP_number + 1;
                    total_UDP_byte += (float) packet.length();
                } else if (packet.get(IcmpV4CommonPacket.class) != null) {
                    ICMP_number = ICMP_number + 1;
                    total_ICMP_byte += (float) packet.length();
                } else {
                    OTHER_number = OTHER_number + 1;
                    total_OTHER_byte += (float) packet.length();
                }

                if (packet.get(UdpPacket.class) != null) {
                    TCP_number = TCP_number + 1;
                }


            }
        };

        try {
            int maxPackets = -1;
            handle.loop(maxPackets, listener);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }



        System.out.println("TCP Summary Table");

        for (Flow f: flowArrList) {
            f.handleNoFIN();
            System.out.println(f.toString());
        }

        System.out.println("");
        System.out.println("Additional Protocols Summary Table");
        System.out.println("UDP, " + UDP_number + ", " + total_UDP_byte);
        System.out.println("ICMP, " + ICMP_number + ", " + total_ICMP_byte);
        System.out.println("Other, " + OTHER_number + ", " + total_OTHER_byte);

        // Cleanup when complete
        handle.close();
    }
}

class Flow {

    String sourceIP;
    int sourcePort;
    String destIP;
    int destPort;
    int packetComplete;
    int packetInComplete;
    int packetUk;
    double avgBandwidth;
    boolean foundSYN = false;
    boolean foundFIN = false;
    boolean firstTimeCheck = false;
    boolean firstFin = true;
    double firstTime;
    double lastTime;
    double totalByte;
    double inCompletedBytes;
    double completedBytes;

    Flow(String sip, int sport, String dip, int dport) {
        this.sourceIP = sip;
        this.destIP = dip;
        this.sourcePort = sport;
        this.destPort = dport;
        this.totalByte = 0;
        this.packetComplete = 0;
        this.packetInComplete = 0;
        this.packetUk = 0;
        this.avgBandwidth = 0;
    }

    void incrementPacks() {
        if (this.foundSYN == false && this.foundFIN == false) this.packetInComplete += 1;

        if (this.foundSYN && this.foundFIN == false)
            this.packetUk += 1;
        else if (this.foundSYN == false && this.foundFIN) {
            this.packetInComplete += 1;
            this.foundFIN = false;
        }
        if (this.foundSYN && this.foundFIN) {
            this.packetComplete = this.packetUk + 1;
            this.packetUk = 0;
            this.foundFIN = false;
            this.foundSYN = false;
        }
    }

    void handleNoFIN() {
        if (this.packetUk > 0) {
            this.packetInComplete = this.packetUk;
        }
    }

    void setTime(double t) {
        if (firstTimeCheck == false && this.foundSYN) {
            this.firstTime = t;
            this.firstTimeCheck = true;
        }
        if (firstTimeCheck && this.foundSYN && this.foundFIN && this.firstFin) {
            this.lastTime = t;
            this.firstFin = false;
        }
    }

    void incCompletedBytes(double b) {
        if (this.foundSYN && this.foundFIN) {
            this.completedBytes = this.inCompletedBytes + b;
        }
        if (this.foundSYN && this.foundFIN == false) {
            this.inCompletedBytes += b;
        }
    }

    public String id() {
        return (sourceIP + ", " + sourcePort + ", " + destIP + ", " + destPort);
    }

    public String toString() {
        double totalTime = this.lastTime - this.firstTime;
        totalTime = totalTime / 1000000;
        this.avgBandwidth = (double) this.completedBytes / (totalTime) / 125000.0;
        if (this.packetComplete == 0)
            return sourceIP + ", " + sourcePort + ", " + destIP + ", " + destPort + ", " + packetComplete + ", " + packetInComplete;
        else
            return sourceIP + ", " + sourcePort + ", " + destIP + ", " + destPort + ", " + packetComplete + ", " + packetInComplete + ", " + totalByte + ", " + avgBandwidth;
    }
}