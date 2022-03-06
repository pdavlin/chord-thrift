import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TServer.Args;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TSSLTransportFactory;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.*;
import org.apache.thrift.transport.TSSLTransportFactory.TSSLTransportParameters;
import org.slf4j.helpers.MessageFormatter;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.*;
import java.util.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.UnsupportedEncodingException;


public class SupernodeHandler implements Supernode.Iface {

	public ArrayList<NodeData> nodes = new ArrayList<NodeData>();
	public boolean joinLock = false;

	@Override
	public NodeData join(int port) throws org.apache.thrift.TException {
		System.out.println("Attempting to add node to cluster");
		if (joinLock) {
			System.out.println("Could not acquire joinLock, NACK");
			return null;
		}
		else if (nodes.size() > 0) {
			System.out.println("joinLock acquired");
			//return existing node
			joinLock = true;
			Random r = new Random();
			int i = r.nextInt(nodes.size());	
			// System.out.println("Returning node @ index: " + Integer.toString(i));
			supernodeLog("Returning node at index {}", i);
			return nodes.get(i);
		}
		joinLock = true;
		System.out.println("NodeList empty, returning fresh node data");
		return new NodeData();
	}

	@Override
	public void postJoin(int port) throws org.apache.thrift.TException {
		// System.out.println("postJoin reached. New node at port " + Integer.toString(port));
		supernodeLog("New node established at port {}", port);
		NodeData nodeData = new NodeData();
		nodeData.id = getHashKey(Integer.toString(port));
		nodeData.port = port;
		nodes.add(nodeData);
		//TODO: add method to pretty print node list
		//release joinLock, adding procedure is complete
		// System.out.println("NodeList new length: " + Integer.toString(nodes.size()));
		supernodeLog("NodeList new length {}", nodes.size());
		joinLock = false;
	}

	@Override
	public NodeData getNode() throws org.apache.thrift.TException {
		System.out.println("getNode reached @ supernode");
		if (nodes.size() > 0) {
			//return existing node
			Random r = new Random();
			int i = r.nextInt(nodes.size());	
			// System.out.println("Returning node @ index: " + Integer.toString(i));
			supernodeLog("Returning node at index {}", i);
			return nodes.get(i);
		}
		System.out.println("NodeList is empty, returning new NodeData");
		return new NodeData();
	}

	public static int getHashKey(String input) {
                try {
                        // System.out.println("Attempting to get hash for string: " + input);
						supernodeLog("Attempting to get hash for string: {}", input);
                        MessageDigest md = MessageDigest.getInstance("SHA-1");
                        byte[] encoded = md.digest(input.getBytes("UTF-8"));
                        StringBuffer hex = new StringBuffer();

                        for (int i = 0; i < encoded.length; i++) {
                                String j = Integer.toHexString(0xff & encoded[i]);
                                if (j.length() == 1) {
                                        hex.append('0');
                                }
                                hex.append(j);
                        }
                        return hashToId(hex.toString());
                        //return hex.toString();
                } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                } catch(NoSuchAlgorithmException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                }
        }

        public static int hashToId(String input) {
                char lastChar = input.substring(input.length() - 1).charAt(0);
                return Character.digit(lastChar, 16);
        }	

		public static void supernodeLog(String msg, Object... objects) {
			System.out.println("SN:  " + MessageFormatter.arrayFormat(msg, objects).getMessage());
		}

        public static void main(String[] args) throws TException {
		/*//Start conn for nodes
		System.out.println("Starting Supernode on Port 9090");
                TServerTransport serverTransport = new TServerSocket(9090);
                TTransportFactory factory = new TTransportFactory();


                NodeHandler nodeHandler = new NodeHandler();
                Node.Processor nodeProcessor = new Node.Processor(nodeHandler);

                TThreadPoolServer.Args serverArgs = new TThreadPoolServer.Args(serverTransport);
                serverArgs.processor(nodeProcessor); //Set handler
                serverArgs.transportFactory(factory); //Set FramedTransport (for performance)

                TServer server = new TThreadPoolServer(serverArgs);
                server.serve();*/
				supernodeLog("INITIALIZING SUPERNODE CONNECTION TO SERVER");
                TTransport transport = new TSocket("localhost", 9090);
                TProtocol protocol = new TBinaryProtocol(transport);
                Supernode.Client client = new Supernode.Client(protocol);
                //Try to connect
                transport.open();
                supernodeLog("supernode connected to server");
		while (true);

        }
}

