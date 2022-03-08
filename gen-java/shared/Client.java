import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import java.io.UnsupportedEncodingException;


public class Client {

	public static void main(String[] args) throws TException {
		int port = 9090;
		System.out.println("starting client on port " + Integer.toString(port));
		System.out.println("INITIALIZING NODE CONNECTION TO SUPERNODE");
                TTransport transport = new TSocket("localhost", 9090);
                TProtocol protocol = new TBinaryProtocol(transport);
                Supernode.Client client = new Supernode.Client(protocol);
                //Try to connect to supernode
                transport.open();
                System.out.println("Node Connected to SN");

		//Retrieve standard node connection
		NodeData nodeData = client.getNode();
		System.out.println("getNode returned Port: " + Integer.toString(nodeData.port));
		//Connect to regular node
                TTransport nodeTransport = new TSocket("localhost", nodeData.port);
                TProtocol nodeProtocol = new TBinaryProtocol(nodeTransport);
                Node.Client nodeClient = new Node.Client(nodeProtocol);
                //Try to connect to supernode
                nodeTransport.open();	
		System.out.println("connected to Node server on port: " + Integer.toString(nodeData.port));
	
		Scanner scanner = new Scanner(System.in);
		//Testing loop
		while (true) {
			/*System.out.println("Enter Key to get succ/pred for: ");
			int id = scanner.nextInt();
			nodeClient.findSuccessor(id);
			nodeClient.findPred(id);*/
			System.out.println("Enter a command: ");
			System.out.println("to get a book: get;<title>");
			System.out.println("to set/update a book: set;<title>;genre");
			String bookData = scanner.nextLine();
			String[] bookDataArray = bookData.split(";");
			String method = bookDataArray[0];
			String title = bookDataArray[1];
			String genre = "";
			if (bookDataArray.length > 2) {
				genre = bookDataArray[2];

			}
			if (method.compareTo("get") == 0) {
				nodeClient.get(title);
			}
			else {
				nodeClient.setBook(title, genre);
			}
		}
		//String title = nodeClient.get("A Tale of Two Cities");
		//System.out.println("nodeClient.get() returned: " + title);
	}
}
