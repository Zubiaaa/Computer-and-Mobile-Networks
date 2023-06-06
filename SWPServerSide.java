// Stop & Wait Protocol (Server)
// Note: I have provided citations with each comment in this file

// imported all the required libraries
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.math.BigInteger;
import java.net.SocketAddress;
import java.net.InetAddress;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;
  
public class SWPServerSide
{
    // static variables
    static ByteBuffer orginalData = null;
    static InetAddress ip;
    static int port;
    static int y=0;
    
    // Initialize the datagramPacket for sending the data [37]
    static DatagramPacket DpReceive;
    static DatagramPacket ackPacket;

    // Total Number of Packets to be sent between the client and server
    static int totalNoOfPackets = 10;

    public static void main(String[] args) throws IOException
    {
        // Expected sequence number of the packet to be receive [4, p. 220]
        int expectedSeqNum = 1;
         
        // The data we are going to send and receive
        byte[] receive = new byte[65507];
        byte[] dataForSend = new byte[65535];
        byte[] byteArray= null;

        // call the constructor of SWPMyClient class
        SWPMyClient c1 = new SWPMyClient("localhost", 9876, 1, "file.txt"); 

        //calling static methods of GBNMyClient class to get the server's port number and the window size       
        int portNum = c1.getPort();
        int windowSize = c1.getWindowSize();

        // After sending acknowledgements, store their sequence numbers in this arraylist [4, p. 220]
        ArrayList<Integer> ackSent = new ArrayList<Integer>();

        // sn extracted from each packet
        int sn;
           
        byte[] d = null;
        byte[] n = null;

        // Create a socket to listen at port [30], [33]
        DatagramSocket serverSocket = new DatagramSocket(portNum);

        // check if the packets whose acknowledgement is being sent is the last packet        
        while(!ackSent.contains(totalNoOfPackets))
        {
              System.out.println("Waiting for packet");  
               
              // Create a Datagram packet for receiving the data [37]
              DpReceive = new DatagramPacket(receive, receive.length);
                        
              // invoke the receice call to receive data [30]
              serverSocket.receive(DpReceive);
              
              // copy the original data and the sequence number from the received data into a new array [39]
              d = Arrays.copyOfRange(receive, 0, 8);
              n = Arrays.copyOfRange(receive, 9, 12);

              // convert the byte array into integer [40]
              sn = new BigInteger(n).intValue();
             
              System.out.println("Packet Received with Sequence Number " + sn);

              // if the received sequence number mataches the expected sequence number, print the data to the screen and send acknowledgement to the client
              if (sn == expectedSeqNum)
              {
                 // Print the data to the screen
                 System.out.println("***Printing Data***");
                 System.out.println("  '" + data(d) + "'  ");
                
                 // After matching sequence numbers, increment the expected sequence number
                 expectedSeqNum++;  
                
               
                 // converting the original data received into string
                 String message = data(d).toString(); 

                 // converting the string of data into bytes
                 dataForSend = message.getBytes();
              
                 // Allocating a new byte buffer with a limited capacity of file size plus sequence number size [36]
                 orginalData = ByteBuffer.allocate(dataForSend.length + 4);

                 // combining original data with sequence number [32]
                 orginalData.put(dataForSend).putInt(sn);

                 // Converting bytebuffer into byte array [32]
                 byteArray = orginalData.array();
               
                 // Get Client's address and port number [34]
                 ip = DpReceive.getAddress();
                 port = DpReceive.getPort();
              
                 // Create the datagramPacket for sending the data back to the client (acknowledgement) [37]
                 ackPacket = new DatagramPacket(byteArray, byteArray.length, ip, port);
                    
                 System.out.println("Sending Acknowledgement with Sequence Number: " + sn);

                 // Send the packet data back to the client [30]
                 serverSocket.send(ackPacket);

                 // adding the sequence number (into the arraylist) of that packet which is received and its acknowledgement is sent [38]
                 ackSent.add(sn);
                 
              }

              // else display the out of order message, as the sequence number of the received packet does not matches the expected sequence number
              else
              {
                 System.out.println("Out of Order Packet discarded with Sequence Number: " + sn);      
              
              }
               
              // Clear all the byte arrays after every message
              receive = new byte[65535];
              dataForSend = new byte[65535];
              orginalData = null;
              byteArray = null;
              
           }

           // Closing the Server socket [27]
           serverSocket.close();
                   
    }
  
    // A utility method to convert the byte array and data into a string representation [41]
    public static StringBuilder data(byte[] a)
    {
        if (a == null)
            return null;
        StringBuilder ret = new StringBuilder();
        int i = 0;
        while (i != a.length)
        {
            ret.append((char) a[i]);
            i++;
        }
        return ret;
    }

}