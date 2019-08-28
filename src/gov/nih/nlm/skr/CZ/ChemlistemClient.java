package gov.nih.nlm.skr.CZ;
import java.net.Socket;
import java.net.UnknownHostException;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileReader;

/**
 * ChemlistemClient - A Client program for Chemlistem Socket Server
 *
 * Usage:
 *    java [classpath] ChemlistemClient file0 [filen...] 
 *
 *
 * Created: Thu Oct 12 10:42:45 
 *
 * @author <a href="mailto:wrogers@nlm.nih.gov">Willie Rogers</a>
 * @version 1.0
 */
public class ChemlistemClient {

  /** The remote host */
  static String host = "ii-server2";    
  /** The same port as used by the server */
  static int port = 32000;

  /**
   * Creates a new <code>ChemlistemClient</code> instance.
   */
  public ChemlistemClient() {
  }

  /** 
   * Write request to server
   *
   * @param socket socket connected to server
   * @param message request message
   */
  private static void writeRequest (Socket socket, String message)
    throws IOException
  {
    PrintWriter out = new PrintWriter(socket.getOutputStream());
    out.println(message);
    out.flush();
  }

  /** 
   * Read response from server
   *
   * @param socket  socket connected to server
   * @return contents of server response
   */
  private static String readResponse (Socket socket)
    throws IOException
  {
    BufferedReader input =
      new BufferedReader
      (new InputStreamReader
       (socket.getInputStream()));
    String line = "";
    StringBuffer result = new StringBuffer();
    int length = 0;
    while ((line = input.readLine()) != null) {
      // debug: System.out.println("line: " + line);
      if (line.equals("EOF"))
	break;
      result.ensureCapacity(length + line.length());
      result.append(line).append("\n");
      length += line.length();
    }	
    return result.toString();
  }

  /**
   * Read specified file contents into string, converting newlines to
   * spaces.
   *
   * @param filename name of input file
   * @return string containing contents of file.
   */
  public static String readFile(String filename)
  {
    try {
      StringBuffer messagebuf = new StringBuffer();
      BufferedReader br = new BufferedReader(new FileReader(filename));
      String line;
      while ((line = br.readLine()) != null) {
	messagebuf.append(line).append(" ");
      }
      br.close();
      return messagebuf.toString();
    } catch (FileNotFoundException fnfe) {
      throw new RuntimeException(fnfe);
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    } 
  }

  /**
   * Setup socket to server
   * @param host server hostname
   * @param port server port
   * @return socket
   */
  public static Socket setupSocket(String host, int port) {
    Socket socket = null;
    try {
      socket = new Socket(host, port);
    } catch (java.net.UnknownHostException e) {
      System.err.println("Unknown host exception: " + 
			 e.getMessage());
    } catch (IOException ex) {
      System.err.println("Server Connection-IOException: " + 
			 ex.getMessage());
    } catch (SecurityException ex) {
      System.err.println("Server Connection-SecurityException: " + 
			 ex.getMessage());
    }
    return socket;
  }
  
  /**
   * Send message to server and read response
   *
   * @param message message to send to server
   * @return string containing response or null
   */
  public static String converse(Socket socket, String message) {
    // send request and read response
    try {
      writeRequest(socket, message);
      String result = readResponse(socket);
      return result;
    } catch (IOException ex) {
      System.err.println("Server Connection-IOException: " + 
			 ex.getMessage());
      return null;
    }
  }

  /**
   * main program
   *
   * @param args command line arguments
   */
  public static void main(String args[])
  {
    // if (args.length > 0) {
      try {
	Socket socket = setupSocket(host, port);
	// for (String arg: args) {
	  // String message = readFile(arg);
		String message = readFile("Q:\\LHC_Projects\\Caroline\\NEW_2017\\czbrat2\\25449120.txt");
	  System.out.println("message: " + message);
	  String result = converse(socket, message);
	  System.out.println("-*- result -*-");
	  System.out.println(result);
	  System.out.flush();
	// }
	socket.close();
      } catch (IOException ex) {
	System.err.println("Server Connection-IOException: " + 
			   ex.getMessage());
      }
   /*  } else {
      System.err.println("usage: ChemlistemClient filename");
    } */
  }
}
