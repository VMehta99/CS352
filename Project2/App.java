// App.java
package com.github.username;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.util.*;

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
import org.pcap4j.packet.IcmpV4CommonPacket;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.IpV4Packet.IpV4Header;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.TcpPacket.TcpHeader;
import org.pcap4j.packet.UdpPacket;

public class App {

	static int check_number = 0;
    static int UDP_number = 0;
	static int TCP_number = 0;
	static int ICMP_number = 0;
	static int OTHER_number = 0;
	
	static float total_UDP_byte = 0;
	static float total_ICMP_byte = 0;
	static float total_OTHER_byte = 0;
	static float total_byte = 0;

	static double first_pack_time = 0;
	static double last_pack_time = 0;
	static boolean first_packet_time= false;
	static boolean last_packet_time=false;

	// TCP flow object and nested class
	public static class TCPFlow{
		 String sourceIP;
		 int sourcePort;
		 String DestinationIP;
		 int DestinationPort;
		 int numComplete;
		 int numIncomplete;
		 float totalBytes;
		 boolean flag;

		public TCPFlow(String sourceIP, int sourcePort, String DestinationIP, int DestinationPort, int numComplete, int numIncomplete, float totalBytes, boolean flag)
		{
			this.sourceIP = sourceIP;
			this.sourcePort = sourcePort;
			this.DestinationIP = DestinationIP;
			this.DestinationPort = DestinationPort;
			this.numComplete = numComplete;
			this.numIncomplete = numIncomplete;
			this.totalBytes = totalBytes;
			this.flag = flag;
		}
	}

    public static void main(String[] args) throws PcapNativeException, NotOpenException {
		// Check for argument
        if (args.length == 0){
            System.out.println("no argument");
            return;
		}
		// Check if file exists
        System.out.println("Working Directory = " + System.getProperty("user.dir") + "/");
        File theFile = new File(System.getProperty("user.dir") + "/" + args[0]);
        boolean exists = theFile.exists();
        if(exists){
            System.out.println(args[0] + " exists");
        }
        else{
            System.out.println("file not found");
            return;
		}
		
		// TCP flow arraylist
		ArrayList<TCPFlow> list = new ArrayList<TCPFlow>();
        
       	final PcapHandle handle;

	handle = Pcaps.openOffline(args[0] );
	
	System.out.println("handle: " + handle);	

		// packets
        PacketListener listener = new PacketListener() {
            public void gotPacket(Packet packet) {
	                					
			if(first_packet_time==false)
			{
			first_pack_time = (double)handle.getTimestamp().getTime();
			System.out.println("first_pack_time: " + first_pack_time);
			first_packet_time=true;
			}
			last_pack_time = (double)handle.getTimestamp().getTime();
              					
			check_number = 1+ check_number;
		 	total_byte = total_byte + (float)packet.length();
			
			if(packet.get(TcpPacket.class)!=null){
				//obtain header
				IpV4Packet ipPacket = packet.get(IpV4Packet.class);
				IpV4Header ipHeader = ipPacket.getHeader();
				TcpPacket tcpPacket = packet.get(TcpPacket.class);
				TcpHeader tcpHeader = tcpPacket.getHeader();

				//obtain various values
				String srcAddr = ipHeader.getSrcAddr().getHostAddress();
				String dstAddr = ipHeader.getDstAddr().getHostAddress();
				int srcPort = tcpHeader.getSrcPort().valueAsInt();
				int dstPort = tcpHeader.getDstPort().valueAsInt();
				boolean syn = tcpHeader.getSyn();
				boolean fin = tcpHeader.getFin();
				
				System.out.println("list size: " + list.size());
				if(list.size() == 0){
					TCPFlow flow = new TCPFlow(srcAddr, srcPort, dstAddr, dstPort, 0, 0, 0, true);
					list.add(flow);
				}

				else{
					// Check if flow is complete
					boolean present = true;
					for(int i = 0; i < list.size(); i++){
						System.out.println("sourceIP: " + list.get(i).sourceIP + " vs " + "srcAddr: " + srcAddr);
						System.out.println((list.get(i).sourceIP).equals(srcAddr));
						System.out.println("sourcePort: " + list.get(i).sourcePort + " vs " + "srcPort: " + srcPort);
						System.out.println("DestinationIP: " + list.get(i).DestinationIP + " vs " + "dstAddr: " + dstAddr);
						System.out.println((list.get(i).DestinationIP).equals(dstAddr));
						System.out.println("DestinationPort: " + list.get(i).DestinationPort + " vs " + "dstPort: " + dstPort);
						System.out.println("syn: " + syn);
						System.out.println("fin: " + fin);
						// Check if flow already present in list
						if( (list.get(i).sourceIP).equals(srcAddr) == true ){
							System.out.println("first case true");
							if( (list.get(i).sourcePort) == srcPort ){
								System.out.println("second case true");
								if( (list.get(i).DestinationIP).equals(dstAddr) == true ){
									System.out.println("third case true");
									if( (list.get(i).DestinationPort) == (dstPort) ){
										System.out.println("fourth case true");
										// Add to number of complete/incomplete flows

										// Add to total bytes
										list.get(i).totalBytes += (float)packet.length();
										present = true;
										break;
									}
									else{
										System.out.println("fourth case false");
										present = false;
										continue;
										
									}
								}
								else{
									System.out.println("third case false");
									present = false;
									continue;
								}
							}
							else{
								System.out.println("second case false");
								present = false;
								continue;
							}
						}
						else{
							System.out.println("first case false");
							present = false;
							continue;
						}
					}
					if (present == false){
						TCPFlow flow = new TCPFlow(srcAddr, srcPort, dstAddr, dstPort, 0, 0, 0, true);
						list.add(flow);
						present = true;
					}
				}

				// TCPFlow flow = new TCPFlow(srcAddr, srcPort, dstAddr, dstPort);
				// list.add(flow);
				
			    TCP_number = TCP_number +1 ;
			
			}

			else if(packet.get(UdpPacket.class)!=null){
			    UDP_number = UDP_number + 1 ;
			    total_UDP_byte += (float)packet.length();
			}

			else if(packet.get(IcmpV4CommonPacket.class)!=null){
				ICMP_number = ICMP_number + 1 ;
				total_ICMP_byte += (float)packet.length();
			 }

			else{
				OTHER_number = OTHER_number + 1 ;
				total_OTHER_byte += (float)packet.length();
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
	
	System.out.println( "Total number of packets, "+  check_number);
	
	System.out.println( "Total number of TCP packets, " + TCP_number);
	System.out.println( "Total bandwidth of the packet trace in Mbps, " + total_byte/total_time/125000.0);
	// System.out.println( "Total bytes of something, " + total_byte);
	

	// TCP Summary table
	System.out.println("TCP Summary Table");
	for(int i = 0; i < list.size(); i++){
		System.out.print( "sourceIP: " + list.get(i).sourceIP + ", ");
		System.out.print( "sourcePort: " + list.get(i).sourcePort + ", ");
		System.out.print( "DestinationIP: " + list.get(i).DestinationIP + ", ");
		System.out.print( "DestinationPort: " + list.get(i).DestinationPort + ", ");
		System.out.print( "numComplete: " + list.get(i).numComplete + ", ");
		System.out.print( "numIncomplete: " + list.get(i).numIncomplete + ", ");
		System.out.println( "totalBytes: " + list.get(i).totalBytes);
	}
	
	System.out.println();

	// additional protocols summary
	System.out.println( "Additional Protocols Summary Table");
	System.out.println( "UDP, " + UDP_number + ", " + total_UDP_byte);
	System.out.println( "ICMP, " + ICMP_number + ", " + total_ICMP_byte);
	System.out.println( "Other, " + OTHER_number + ", " + total_OTHER_byte);
        // Cleanup when complete
        handle.close();
    }
}

// class TCPFlow(String sourceIP, int sourcePort, String DestinationIP, int DestinationPort)
// 	{
// 		this.sourceIP = sourceIP;
// 		this.sourcePort = sourcePort;
// 		this.DestinationIP = DestinationIP;
// 		this.DestinationPort = DestinationPort;
// 	}
