// Go-Back-N Protocol (Client)
// Note: I have provided citations with each comment in this file

// imported all the required libraries
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.util.Timer;
import java.util.TimerTask;
import java.util.ArrayList;
import java.nio.ByteBuffer;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;
import java.lang.*;  
public class GBNMyClient
{
    // static variables
    static int port;
    static String hostName;

    // Initialize the client socket [30], [33]
    static DatagramSocket clientSocket = null;
    
    // size of the sliding Window
    static int windowSize;
    
    static String filePath;
    static File file;

    // sequence number of the next packet to be sent [4, p. 220]
    static int nextSeqNum=1;

    //  sequence number of the oldest unacknowledged packet [4, p. 220]
    static int base=1;

    // create a timeout by enabling a SO_TIMEOUT with 1000ms timeout[30]
    static int timeout=1000;

    // Total Number of Packets to be sent between the client and server
    static int totalNoOfPackets = 10;

    // sn extracted from each packet
    static int sn;

    static byte[] d = null;
    static byte[] n = null;

    // Storing sequence numbers of all the already Ack'd packets [4, p. 220]
    static ArrayList<Integer> alreadyAck = new ArrayList<Integer>();

    // Storing sequence numbers of all the sent, not yet Ack'd packets [4, p. 220]
    static ArrayList<Integer> sentNotYetAck = new ArrayList<Integer>();

    // Storing sequence numbers of all the packets whose acknowledgement is received [4, p. 220]
    static ArrayList<Integer> ackReceived = new ArrayList<Integer>();

    // The data we are going to send and receive
    static byte buf[] = null;
    static byte[] receive = new byte[65535];
    static byte[] orginalData = new byte[6000];

    // bytebuffer to combine original data with sequence number [31]
    static ByteBuffer combinedData;
    
    // Constructor takes a Hostname, port number, window size, and file path (where file.txt is saved) as arguments
    public GBNMyClient(String hostName, int port, int windowSize, String filePath) 
    {
        this.hostName = hostName;
        this.port = port;
        this.windowSize = windowSize;
        this.filePath = filePath;
       
    }

    public static void main(String args[]) throws IOException
    {
        // Create the socket object for carrying the data [30], [33]
        clientSocket = new DatagramSocket();
        
        // call the constructor of SWPMyClient class
        GBNMyClient c = new GBNMyClient("localhost", 1234, 5, "file.txt");
        
        // convert the hostname of server into IP address [34]
        InetAddress IPAddress = InetAddress.getByName(hostName);
     
        // Create a File object and pass the filepath as a parameter [35]
        file = new File(filePath);

        // Creating an object of FileInputStream to read from a file [35]
        FileInputStream fl = new FileInputStream(file);  

        buf = new byte[(int)file.length()];
 
        // Reading file content to byte array [35]
        fl.read(buf); 
        
        // loop
        while (true)
        { 
          
            // Checking if the sliding window is full by using 2 conditions [4, p. 221]
            // Checking if base sequence number and next sequence number are less than and equal to the sequence number of the last packet
            while(nextSeqNum - base < windowSize && sentNotYetAck.size()<windowSize && base<=totalNoOfPackets && nextSeqNum<=totalNoOfPackets)
            {  
              // Allocating a new byte buffer with a limited capacity of file size plus sequence number size [36]
              combinedData = ByteBuffer.allocate(buf.length + 4);

              // combining original data with sequence number [32]
              combinedData.put(buf).putInt(nextSeqNum);
            
              // Converting bytebuffer into byte array [32]
              orginalData = combinedData.array();
            
              // Create the datagramPacket for sending the data [37]	    
              DatagramPacket DpSend = new DatagramPacket(orginalData, orginalData.length, IPAddress, port);              
            
              // Removing all the elements of this bytebuffer for putting the data and sequence number for next datagram packet [31]
              combinedData.clear();
              
              
              // invoke the send call to send data to the server [30]
              clientSocket.send(DpSend);

              // adding the sequence number (into the arraylist) of the packet sent [38]
              sentNotYetAck.add(nextSeqNum);
              
              System.out.println(" Packet Sent with Sequence Number: " + nextSeqNum);

              // incrementing the next sequence number by one [4, p. 221]
              nextSeqNum++;
               
            }
            
            // Create the datagramPacket for receiving the data [37]
            DatagramPacket DpReceive = new DatagramPacket(receive, receive.length);
            
            
            
            //Receive and initiate timer
            
            // initiate a timer and receive an acknowledgement within the timer  
            try
            {
                
                   //Set timeout value
                   clientSocket.setSoTimeout(timeout);
                 
                   // receive the packet [30]
                   clientSocket.receive(DpReceive);
                 
                   // copy the original data and the sequence number from the received data into a new array [39]
                   d = Arrays.copyOfRange(receive, 0, 8);
                   n = Arrays.copyOfRange(receive, 9, 12);
            
                   // convert the byte array into integer [40]
                   sn = new BigInteger(n).intValue();

                   // adding the sequence number (into the arraylist) of the packet received [38]
                   ackReceived.add(sn);
                   
              // if timer goes off (socket timeout exception), retransmit that packet 
            } catch (SocketTimeoutException s)
              {  
                 System.out.println("\n" + " Packet Lost with Sequence Number: " + base);
                  
                 // Retransmitting all the packets in the current sliding window
                 System.out.println("\n" + " ***Retransmission*** ");
                 //1st timeout
                 timeout+=500;
    	       
                 for(int i=base; i<nextSeqNum; i++)
                 {
                      System.out.println("Retransmitting Packet in the Sliding Window with Sequence Number: " + i);
                      combinedData.put(buf).putInt(i);
                              
                      orginalData = combinedData.array();
                  
                      combinedData.clear();
                      
                      // Create the datagramPacket for sending the data	    
                      DatagramPacket DpSend = new DatagramPacket(orginalData, orginalData.length, IPAddress, port);

                      // if the arraylist 'sent but not acknowledged packets' the sequence number of the last packet 
                      if(!sentNotYetAck.contains(totalNoOfPackets))
                      {
                        // resend [30]
                        clientSocket.send(DpSend);
                      }
                 }
                   
                 // initiate a timer and receive an acknowledgement within the timer
                 clientSocket.setSoTimeout(timeout);
                      
                    try
                    {
                        
                            //Set timeout value
                            clientSocket.setSoTimeout(timeout);
                        
                            // receive the packet [30]
                            clientSocket.receive(DpReceive);
                       
                            // copy the original data and the sequence number from the received data into a new array [39]
                            d = Arrays.copyOfRange(receive, 0, 8);
                            n = Arrays.copyOfRange(receive, 9, 12);
                         
                            // convert the byte array into integer [40]
                            sn = new BigInteger(n).intValue();

                            // adding the sequence number (into the arraylist) of the packet received [38]
                            ackReceived.add(sn);

                      // If again timer goes off (socket timeout exception) and still packet is not received, stop transmitting packets in the window [27]    
                    } catch (SocketTimeoutException t)
                    {     
                          //Not received the retransmitted packet, hence not retransmittng it again (ignoring that packet)
                          System.out.println("Did not Receive the Retransmitted Packet with Sequence Number: " + base + "\nExiting..." + "\n");
                                  
                    }
                          
                   
              }

            // add the sequence number of the received packet into the arraylist [38]
            alreadyAck.add(base);
              
            // remove the sequence number of the received packet from the sent not yet acknowledged arraylist [38]
            sentNotYetAck.remove(sentNotYetAck.indexOf(base)); 

            // Clear the byte arrays after sending/receiving each packet
            receive = new byte[65535];
            orginalData = null;
            
            // If the received acknowledgements arraylist is not empty, display acknowledgement received message, original data received and sequence number of the oldest packet received
            if(!ackReceived.isEmpty())
            {  
              System.out.println( "Acknowledgement Received: Client said " + "'"+ data(d) +"' " + "(Sequence Number = " + ackReceived.get(0) + ")");

              // remove the sequence number of the oldest packet
              ackReceived.remove(0);     
            }

            // If the received sequence number is the last packet, close the client socket and break the while loop [27]
            if(sn == totalNoOfPackets)
            {
                   System.out.println("Finished Transmission with sequence number: " + sn);
                   clientSocket.close();
                   break;
            }

            // If the sequence number (of the oldest unacknowledged packet) is the last packet, close the client socket and break the while loop [27]
            if(base == totalNoOfPackets)
            {
                   clientSocket.close();
                   break;
            } 

            // Increament that sequence number by one
            base++;
               
        }
        // Closing the Client socket [27]
        clientSocket.close();
        
    }
 

    // A utility method to convert the data of byte array into a string representation [41]
    public static StringBuilder data(byte[] a)
    {
        if (a == null)
            {return null;}
        StringBuilder ret = new StringBuilder();
        int y = 0;
        while (y != a.length)
        {
            ret.append((char) a[y]);
            y++;
        }
        return ret;
    }

       public static int getPort()
       {
            return port;       
       }       

       public static int getWindowSize()
       {
            return windowSize;       
       }          
    
           
}

