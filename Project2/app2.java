// App.java
package com.github.username;

import java.io.IOException;
import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.List;

import com.sun.jna.Platform;

import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PacketListener;
import org.pcap4j.core.PcapDumper;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.PcapStat;
import org.pcap4j.core.BpfProgram.BpfCompileMode;
import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;
import org.pcap4j.core.Pcaps;
import org.pcap4j.packet.Packet;
import org.pcap4j.util.NifSelector;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.UdpPacket;
import org.pcap4j.packet.IcmpV4CommonPacket;


public class App {

static int check_number = 0;
static int UDP_number = 0;
static int TCP_number = 0;
static float total_byte = 0;

static double first_pack_time = 0;
static double last_pack_time = 0;
static boolean first_packet_time= false;
static boolean last_packet_time=false;

//create list that holds all tcpFLows
static List<String> tcpList = new ArrayList<String>();

//holds udp bytes
static float udpBytes;

//holds icmp bytes and packets
static float icmpBytes = 0;
static int ICMP_number = 0;

//holds other bytes and packets
static float otherBytes = 0;
static int OTHER_number = 0;

//completed and uncompleted flow
static int completedFlow = 0;
static int incompletedFlow = 0;
   
   class TCPFLOW{
    public String SourceIP;
    public int SourcePort;
    public int DestinationPort;
    public String DestinationIP;
    public int completedFlows;
    public int incompletedFlows;
    public float totalBytes;
    public float avgBandwidth;
        

    public TCPFLOW(String SourceIP, int SourcePort, String DestinationIP, int DestinationPort, int completedFlows, int incompletedFlows, float totalBytes, float avgBandwidth){
           this.SourceIP = SourceIP;
           this.SourcePort = SourcePort;
           this.DestinationIP = DestinationIP;
           this.DestinationPort = DestinationPort;
           this.completedFlows = completedFlows;
           this.incompletedFlows = incompletedFlows;
           this.totalBytes = totalBytes;
           this.avgBandwidth = avgBandwidth;
       }
       
       
     
   }
public static void main(String[] args) throws PcapNativeException, NotOpenException {
System.out.println("Let's start analysis ");
// New code below here

final PcapHandle handle;

handle = Pcaps.openOffline(args[0]);

System.out.println(handle);



PacketListener listener = new PacketListener() {
public void gotPacket(Packet packet) {
if(first_packet_time==false){
first_pack_time = (double)handle.getTimestamp().getTime();
first_packet_time=true;
}
last_pack_time = (double)handle.getTimestamp().getTime();

check_number = 1+ check_number;
total_byte = total_byte + (float)packet.length();

if(packet.get(TcpPacket.class)!=null){
TCP_number = TCP_number +1 ;




//gets first 4 columns
IpV4Packet ipPacket = packet.get(IpV4Packet.class);
//IpV4Header ipHeader = ipPacket.getHeader();
TcpPacket tcpPacket = packet.get(TcpPacket.class);
//TcpHeader tcpHeader = tcpPacket.getHeader();
String dip = ipPacket.getHeader().getDstAddr().getHostAddress();
String sip = ipPacket.getHeader().getSrcAddr().getHostAddress();
int sP = tcpPacket.getHeader().getSrcPort().valueAsInt();
int dP = tcpPacket.getHeader().getDstPort().valueAsInt();

//gets total bytes of TCP packet
float totalBytes = (float) total_byte;

if(tcpPacket.getHeader().getSyn() || tcpPacket.getHeader().getFin()) {
//add to complete flows
 completedFlow = 1;
}else{
  incompletedFlow = 1;// add to incomplete flows
                    }

//gets avg bandwith of TCP packet
//(CompleteBytes) / ((end - Start) / 1000000) / 125000
float avgBandwith = (float) ((float) (totalBytes) / ((last_pack_time - first_pack_time) / 1000000) / 125000);


//creates TCPFLOW object
TCPFLOW tcpFlow = new TCPFLOW(sip, sP, dip, dP, completedFlow, incompletedFlow, totalBytes, avgBandwith);

//adds tcpFlow to list
tcpList.add(tcpFlow.toString());


}
if(packet.get(UdpPacket.class)!=null){
UDP_number = UDP_number + 1 ;
udpBytes = udpBytes + total_byte;
                }
//checks icmp packet?
if(packet.get(IcmpV4CommonPacket.class) != null){
     // for icmp? notsure if this is correct
	ICMP_number = ICMP_number + 1;
	icmpBytes = icmpBytes + total_byte;
                }
 else{ // all other flows

       }

//write better 
//checks other packets
if((packet.get(TcpPacket.class)==null) && (packet.get(UdpPacket.class)==null) && (packet.get(IcmpV4CommonPacket.class) == null)) {
	OTHER_number = OTHER_number + 1;
	otherBytes = otherBytes + total_byte;
}




}
};

try {
int maxPackets = -1;
handle.loop(maxPackets, listener);
} catch (InterruptedException e) {
e.printStackTrace();
}

double total_time = last_pack_time - first_pack_time;
total_time = total_time/1000.0;

/*
System.out.println( "Total number of packets, "+  check_number);
System.out.println( "Total number of UDP packets, " + UDP_number);
System.out.println( "Total number of TCP packets, " + TCP_number);
System.out.println( "Total bandwidth of the packet trace in Mbps, " + total_byte/total_time/125000.0);
*/


//print out all tcpFlows
for(int i = 0; i < tcpList.size(); i++) {
	System.out.println(tcpList.get(i));
}

//print out the additional summary table
System.out.println("UDP " + UDP_number + " " + udpBytes);
System.out.println("ICMP " + ICMP_number + " " + icmpBytes);
System.out.println("Other " + OTHER_number + " " + otherBytes);


// Cleanup when complete
handle.close();
}
}