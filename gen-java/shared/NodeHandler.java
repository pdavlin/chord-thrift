import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.thrift.*;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import java.io.UnsupportedEncodingException;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TServer.Args;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TSSLTransportFactory;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.*;
import org.apache.thrift.transport.TSSLTransportFactory.TSSLTransportParameters;
import org.apache.thrift.TException;
import org.apache.thrift.*;
import java.util.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.UnsupportedEncodingException;
import java.lang.Math;

public class NodeHandler implements Node.Iface {

	private static ArrayList<NodeData> nodes;
	private static HashMap<String, String> bookList;
	private static NodeData currentNode = new NodeData();
	private static NodeData predNode = new NodeData();

	public NodeHandler(int port) {
		currentNode.port = port;
		nodes = new ArrayList<NodeData>(4);
		bookList = new HashMap<String, String>();
		try {
			initNode();
		} catch (TException e) {
			e.printStackTrace();
		}
	}

	public void initNode() throws TException {
		System.out.println("INITIALIZING NODE CONNECTION TO SUPERNODE");
		//int port = Integer.parseInt(args[0]);
		//currentNode.port = port;
		currentNode.id = getHashKey(Integer.toString(currentNode.port));
		//boolean server_start = startNodeServer(port);

		TTransport transport = new TSocket("localhost", 9090);
		TProtocol protocol = new TBinaryProtocol(transport);
		Supernode.Client client = new Supernode.Client(protocol);
		//Try to connect
		transport.open();
		System.out.println("Node Connected to SN");

		//Start local server for node mesh
		//Check for sys.arg
		//TODO: error checking for arg length
		System.out.println("Node @ port:" + Integer.toString(currentNode.port) + " configuring");
		System.out.println("Node ID: " + currentNode.id);

		//ask supernode about who to talk to to get successor/predecessor
		//client.getNode();
		NodeData nodeData = client.join(currentNode.port);
		if (nodeData.port != 0) {
			//nodes.add(nodeData);
			//findSuccessor(currentNode.id);
			int key = currentNode.id;
                        System.out.println("Connecting to fingerNode: " + Integer.toString(nodeData.id) + ":" + Integer.toString(nodeData.port));
                        TTransport nodeTransport = new TSocket("localhost", nodeData.port);
                        TProtocol nodeProtocol = new TBinaryProtocol(nodeTransport);
                        Node.Client nodeClient = new Node.Client(nodeProtocol);
                        //Try to connect
                        System.out.println("opening transport");
                        nodeTransport.open();
                        System.out.println("Calling client.findSuccessor");
                        NodeData n = nodeClient.findSuccessor(key);
                        nodes.add(n);
                        nodeTransport.close();
                        System.out.println("Calling client.findPred");
                        //NodeData predNode = nodeClient.findPred(key);
                        predNode = findPred(key);
                        System.out.println("Key: " + Integer.toString(key) + " <> Successor: " + Integer.toString(n.id));
                        System.out.println("Key: " + Integer.toString(key) + " <> Predecessor: " + Integer.toString(predNode.id));
			
                        TTransport predTransport = new TSocket("localhost", predNode.port);
                        TProtocol predProtocol = new TBinaryProtocol(predTransport);
                        Node.Client predClient = new Node.Client(predProtocol);
                        //Try to connect
                        predTransport.open();
                        predClient.setNodeSuccessor(currentNode);
                        //predClient.updateDHT();
                        predTransport.close();
			//Update current node finger table
		       	updateDHT();	
			predTransport.open();
			//Update other node finger tables
                        predClient.updateDHT();
                        predTransport.close();
			client.postJoin(currentNode.port);
		}
		else {
			System.out.println("Empty node returned - building fresh finger table");
			predNode = currentNode;
			for (int i = 0; i < 4; i++) {
				int key = currentNode.id + (int)(Math.pow(2, i));
				key = key % 16;
				currentNode.finger = key;
				nodes.add(currentNode);
				System.out.println(Integer.toString(i) + ": " + Integer.toString(key) + " <> Successor: " + Integer.toString(currentNode.id));
			}
			System.out.println("New Node finger table size: " + Integer.toString(nodes.size()));
			client.postJoin(currentNode.port);
		}
	}

	@Override
	public boolean setBook(java.lang.String book_title, java.lang.String genre) throws org.apache.thrift.TException {
		return true;
	}

	@Override
	public java.lang.String get(java.lang.String book_title) throws org.apache.thrift.TException {
		System.out.println("Node:" + Integer.toString(currentNode.port) + " attempting get() for book " + book_title);
		System.out.println("book_title hash: " + Integer.toString(getHashKey(book_title)));
		return "test";
	}

	@Override
	public void updateDHT() throws org.apache.thrift.TException {
		System.out.println("updating DHT for node ID:Port -> " + Integer.toString(currentNode.id) + ":" + Integer.toString(currentNode.port));
		System.out.println("Current Finger Table size: " + Integer.toString(nodes.size()));
		for (int i = 1; i < 4; i++) {
			System.out.println("Trying to get fingertable node " + Integer.toString(i));
			//NodeData fingerNode = nodes.get(i);
			int key = currentNode.id + (int)(Math.pow(2, i));
			key = key % 16;
			NodeData n = findSuccessor(key);
			System.out.println("NodeData n returned, trying to add to finger table");
			try {
				nodes.set(i, n);
			} catch (IndexOutOfBoundsException e) {
				nodes.add(n);
			}
			System.out.println("Key: " + Integer.toString(key) + " <> Successor: " + Integer.toString(n.id));
		}
	}


	@Override
	public NodeData findSuccessor(int key) throws TException {
		System.out.println("Finding Successor for key: " + Integer.toString(key));
		//Key found
		if (key == currentNode.id) {
			System.out.println("Current node matches key: " + Integer.toString(key));
			return currentNode;
		}
		//Only node in cluster will hold all keys
		if (nodes.size() == 0) {
			System.out.println("No nodes in finger table, this node will hold all keys");
			predNode = currentNode;
			return currentNode;
		}
		NodeData predecessor = findPred(key);


		if (predecessor.id == currentNode.id) {
			return nodes.get(0);
		}
		/*NodeData succ = nodes.get(0);
		if (predecessor.id == succ.id) {
			return succ;
		}*/
		TTransport transport = new TSocket("localhost", predecessor.port);
		TProtocol protocol = new TBinaryProtocol(transport);
		Node.Client client = new Node.Client(protocol);
		//Try to connect
		transport.open();
		System.out.println("attempting client.getNodeSuccessor()");

		//TODO: locking here when adding node 2 to cluster
		NodeData n = client.getNodeSuccessor();
		System.out.println("client.getNodeSuccessor() returned");
		transport.close();
		//return findPred(key);
		return n;
	}

	@Override
	public NodeData findPred(int key) {
		System.out.println("Finding Predecessor for key: " + Integer.toString(key));
		//Compare node to successor
		System.out.println("Current Node: " + Integer.toString(currentNode.id));
		NodeData tempNode = nodes.get(0);
		//TODO: HOW DO I DO THIS LOGIC CORRECTLY I AM LOCKING MY STUFFFFFFFFFFFF
		//predNode being returned as currentNode incorrectly?
		System.out.println("successor ID: " + Integer.toString(tempNode.id));
		/*if (currentNode.id > tempNode.id) {
			System.out.println("currentNode.id > tempNode.id");
			if (key <= tempNode.id || key >= currentNode.id) {
				System.out.println("key <= tempNode.id || key >= currentNode.id");
				return currentNode;
			}
			else {
				tempNode = findClosestPrecedingFinger(key);
				return tempNode;
			}
		}
		else {
			System.out.println("currentNode.id <= tempNode.id");
			if (key > currentNode.id || key <= tempNode.id) {
				System.out.println("key > currentNode.id || key <= tempNode.id");
				return currentNode;
			}
			else {
				tempNode = findClosestPrecedingFinger(key);
				return tempNode;
			}
		}*/
		if (currentNode.id < tempNode.id) {
			if (key > currentNode.id && key <= tempNode.id) {
				return currentNode;
			}
		}
		if (currentNode.id > tempNode.id) {
			//if (key > currentNode.id && key <= tempNode.id) {
				return currentNode;
			
			/*if (key <= tempNode.id && key > currentNode.id) {
				System.out.println("Making call to FCPF");
				tempNode = findClosestPrecedingFinger(key);
				System.out.println("FCPF current node: " + Integer.toString(currentNode.id));
				System.out.println("FCPF node id found: " + Integer.toString(tempNode.id));*/
			//}
		}
		System.out.println("while key<> loop exited, returning tempNode: " + Integer.toString(tempNode.id));
		return tempNode;
	}

	@Override
	public NodeData findClosestPrecedingFinger(int key) {
		System.out.println("Finding closest finger for key: " + Integer.toString(key) + " @ node ID: " + Integer.toString(currentNode.id));
		for (int i = nodes.size() - 1; i >= 0; i--) {
			NodeData f = nodes.get(i);
			if (f.id > currentNode.id && f.id < key) {
				return f;
			}
		}
		return currentNode;
	}

	@Override
	public NodeData getNodeSuccessor() {
		System.out.println("getNodeSuccessor reached @ node " + Integer.toString(currentNode.id) + ":" + Integer.toString(currentNode.port));
		if (nodes.size() > 0) {
			NodeData s = nodes.get(0);
			System.out.println("getNodeSuccessor: " + Integer.toString(currentNode.id) + ":" + Integer.toString(currentNode.port) + " -> " + Integer.toString(s.id) + ":" + Integer.toString(s.port));
			return s;
		}
		else {
			System.out.println("No successor node @ node:Port -> " + Integer.toString(currentNode.id) + ":" + Integer.toString(currentNode.port));
			return null;
		}
	}

	@Override
	public void setNodeSuccessor(NodeData successor) {
		if (nodes.size() > 0) {
			nodes.set(0, successor);
			System.out.println("Node " + Integer.toString(currentNode.id) + " Successor updated to " + Integer.toString(successor.id));
		}
	}


	
	public static int getHashKey(String input) {
		try {
			System.out.println("Attempting to get hash for string: " + input);
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


	

	public static void main(String[] args) throws TException {
		/*nodes = new ArrayList<NodeData>(4);
		bookList = new HashMap<String, String>();

		System.out.println("INITIALIZING NODE CONNECTION TO SUPERNODE");
		//int port = Integer.parseInt(args[0]);
		//currentNode.port = port;
		currentNode.id = getHashKey(Integer.toString(currentNode.port));
		//boolean server_start = startNodeServer(port);

		TTransport transport = new TSocket("localhost", 9090);
		TProtocol protocol = new TBinaryProtocol(transport);
		Supernode.Client client = new Supernode.Client(protocol);
		//Try to connect
		transport.open();
		System.out.println("Node Connected to SN");

		//Start local server for node mesh
		//Check for sys.arg
		//TODO: error checking for arg length
		System.out.println("Node @ port:" + Integer.toString(currentNode.port) + " configuring");
		System.out.println("Node ID: " + currentNode.id);

		//ask supernode about who to talk to to get successor/predecessor
		//client.getNode();
		NodeData nodeData = client.join(currentNode.port);
		if (nodeData.port != 0) {
			//nodes.add(nodeData);
			//boolean test = testNodeClientConn(nodeData);
			//testGetSuccessor(nodeData);
		}
		else {
			System.out.println("Empty node returned - building fresh finger table");
			predNode = currentNode;
			for (int i = 0; i < 4; i++) {
				int key = currentNode.id + (int)(Math.pow(2, i));
				key = key % 16;
				currentNode.finger = key;
				nodes.add(currentNode);
				System.out.println(Integer.toString(i) + ": " + Integer.toString(key) + " <> Successor: " + Integer.toString(currentNode.id));
			}
		}
		client.postJoin(currentNode.port);
		while(true);*/

	}
}
/*
	private static void testGetSuccessor(NodeData fingerNode) throws org.apache.thrift.TException {
		System.out.println("testGetSuccessor ID:Port -> " + Integer.toString(currentNode.id) + ":" + Integer.toString(currentNode.port));
		//System.out.println("testGetPred ID:Port -> " + Integer.toString(currentNode.id) + ":" + Integer.toString(currentNode.port));
		int i = 0;
		//for (int i = 0; i < 4; i++) {
		//	NodeData fingerNode = nodes.get(0);
			//int key = currentNode.id + (int)(Math.pow(2, i));
			//key = key % 16;
			int key = currentNode.id;
			System.out.println("Connecting to fingerNode: " + Integer.toString(fingerNode.id) + ":" + Integer.toString(fingerNode.port));
			TTransport transport = new TSocket("localhost", fingerNode.port);
			TProtocol protocol = new TBinaryProtocol(transport);
			Node.Client client = new Node.Client(protocol);
			//Try to connect
			System.out.println("opening transport");
			transport.open();
			System.out.println("Calling client.findSuccessor");
			NodeData n = client.findSuccessor(key);
			System.out.println("Calling client.findPred");
			NodeData predNode = client.findPred(key);
			nodes.add(n);
			transport.close();
			System.out.println("Key: " + Integer.toString(key) + " <> Successor: " + Integer.toString(n.id));


			TTransport predTransport = new TSocket("localhost", predNode.port);
			TProtocol predProtocol = new TBinaryProtocol(predTransport);
			Node.Client predClient = new Node.Client(predProtocol);
			//Try to connect
			predTransport.open();
			predClient.setNodeSuccessor(currentNode);
			predClient.updateDHT();
			predTransport.close();			
			//System.out.println("Key: " + Integer.toString(key) + " <> Predecessor: " + Integer.toString(n.id));
		//}
	}*/


	/*public static boolean startNodeServer(int port) throws TException {
		System.out.println("Starting node server at port " + Integer.toString(port));
		TServerTransport serverTransport = new TServerSocket(port);
                TTransportFactory factory = new TTransportFactory();
                NodeHandler nodeHandler = new NodeHandler();
                Node.Processor nodeProcessor = new Node.Processor(nodeHandler);
                TThreadPoolServer.Args serverArgs = new TThreadPoolServer.Args(serverTransport);
                serverArgs.processor(nodeProcessor); //Set handler
                serverArgs.transportFactory(factory); //Set FramedTransport (for performance)
                //Run server as multithread
                TServer server = new TThreadPoolServer(serverArgs);
		System.out.println("Node Server Started");
                //server.serve();
		return true;
	}*/
/*public static boolean testNodeClientConn(NodeData node) throws TException {
		System.out.println("INITIALIZIZING NODE " + Integer.toString(currentNode.port) + " CONNECTION TO NODE @ PORT: " + Integer.toString(node.port));
		TTransport transport = new TSocket("localhost", node.port);
		TProtocol protocol = new TBinaryProtocol(transport);
		Node.Client client = new Node.Client(protocol);
		//Try to connect
		transport.open();
		System.out.println("Nodes connected");
		//String getTest = client.get("testbook");
		//System.out.println("testNodeClientConn getTest == " + getTest);
		//Build original finger table
		nodes.clear();
		for (int i = 0; i < 4; i++) {
			int key = currentNode.id + (int)(Math.pow(2, i));
			key = key % 16;
			NodeData n = client.findSuccessor(key);
			nodes.add(n);
			System.out.println("Key: " + Integer.toString(key) + " <> Successor: " + Integer.toString(n.id));
		}
		transport.close();
		return true;
	}*/

