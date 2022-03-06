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
	private static String loggingNodeId;

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
	public static void nodeLog(String msg) {
		System.out.println("NODE " + loggingNodeId + ": " + msg);
	}
	
	
	public void initNode() throws TException {
		//int port = Integer.parseInt(args[0]);
		//currentNode.port = port;
		currentNode.id = getHashKey(Integer.toString(currentNode.port));
		loggingNodeId = Integer.toString(currentNode.id);
		//boolean server_start = startNodeServer(port);
		
		nodeLog("INITIALIZING NODE CONNECTION TO SUPERNODE");

		TTransport transport = new TSocket("localhost", 9090);
		TProtocol protocol = new TBinaryProtocol(transport);
		Supernode.Client client = new Supernode.Client(protocol);
		//Try to connect
		transport.open();
		nodeLog("Node Connected to SN");

		//Start local server for node mesh
		//Check for sys.arg
		//TODO: error checking for arg length
		nodeLog("Node @ port:" + Integer.toString(currentNode.port) + " configuring");
		nodeLog("Node ID: " + currentNode.id);

		//ask supernode about who to talk to to get successor/predecessor
		//client.getNode();
		NodeData nodeData = client.join(currentNode.port);
		if (nodeData.port != 0) {
			//nodes.add(nodeData);
			//findSuccessor(currentNode.id);
			int key = currentNode.id;
                        nodeLog("Connecting to fingerNode: " + Integer.toString(nodeData.id) + ":" + Integer.toString(nodeData.port));
                        TTransport nodeTransport = new TSocket("localhost", nodeData.port);
                        TProtocol nodeProtocol = new TBinaryProtocol(nodeTransport);
                        Node.Client nodeClient = new Node.Client(nodeProtocol);
                        //Try to connect
                        nodeLog("opening transport");
                        nodeTransport.open();
                        nodeLog("Calling client.findSuccessor");
                        NodeData n = nodeClient.findSuccessor(key);
                        nodes.add(n);
                        nodeTransport.close();
                        nodeLog("Calling client.findPred");
                        //NodeData predNode = nodeClient.findPred(key);
                        predNode = findPred(key);
                        nodeLog("Key: " + Integer.toString(key) + " <> Successor: " + Integer.toString(n.id));
                        nodeLog("Key: " + Integer.toString(key) + " <> Predecessor: " + Integer.toString(predNode.id));
			
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
			nodeLog("Empty node returned - building fresh finger table");
			predNode = currentNode;
			for (int i = 0; i < 4; i++) {
				int key = currentNode.id + (int)(Math.pow(2, i));
				key = key % 16;
				currentNode.finger = key;
				nodes.add(currentNode);
				nodeLog(Integer.toString(i) + ": " + Integer.toString(key) + " <> Successor: " + Integer.toString(currentNode.id));
			}
			nodeLog("New Node finger table size: " + Integer.toString(nodes.size()));
			client.postJoin(currentNode.port);
		}
	}

	@Override
	public boolean setBook(java.lang.String book_title, java.lang.String genre) throws org.apache.thrift.TException {
		return true;
	}

	@Override
	public java.lang.String get(java.lang.String book_title) throws org.apache.thrift.TException {
		nodeLog("Node:" + Integer.toString(currentNode.port) + " attempting get() for book " + book_title);
		nodeLog("book_title hash: " + Integer.toString(getHashKey(book_title)));
		return "test";
	}

	@Override
	public void updateDHT() throws org.apache.thrift.TException {
		nodeLog("updating DHT for node ID:Port -> " + Integer.toString(currentNode.id) + ":" + Integer.toString(currentNode.port));
		nodeLog("Current Finger Table size: " + Integer.toString(nodes.size()));
		for (int i = 1; i < 4; i++) {
			nodeLog("Trying to get fingertable node " + Integer.toString(i));
			//NodeData fingerNode = nodes.get(i);
			int key = currentNode.id + (int)(Math.pow(2, i));
			key = key % 16;
			NodeData n = findSuccessor(key);
			nodeLog("NodeData n returned, trying to add to finger table");
			try {
				nodes.set(i, n);
			} catch (IndexOutOfBoundsException e) {
				nodes.add(n);
			}
			nodeLog("Key: " + Integer.toString(key) + " <> Successor: " + Integer.toString(n.id));
		}
	}


	@Override
	public NodeData findSuccessor(int key) throws TException {
		nodeLog("Finding Successor for key: " + Integer.toString(key));
		//Key found
		if (key == currentNode.id) {
			nodeLog("Current node matches key: " + Integer.toString(key));
			return currentNode;
		}
		//Only node in cluster will hold all keys
		if (nodes.size() == 0) {
			nodeLog("No nodes in finger table, this node will hold all keys");
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
		nodeLog("attempting client.getNodeSuccessor()");

		//TODO: locking here when adding node 2 to cluster
		NodeData n = client.getNodeSuccessor();
		nodeLog("client.getNodeSuccessor() returned");
		transport.close();
		//return findPred(key);
		return n;
	}

	@Override
	public NodeData findPred(int key) {
		nodeLog("Finding Predecessor for key: " + Integer.toString(key));
		//Compare node to successor
		nodeLog("Current Node: " + Integer.toString(currentNode.id));
		NodeData tempNode = nodes.get(0);
		//TODO: HOW DO I DO THIS LOGIC CORRECTLY I AM LOCKING MY STUFFFFFFFFFFFF
		//predNode being returned as currentNode incorrectly?
		nodeLog("successor ID: " + Integer.toString(tempNode.id));
		/*if (currentNode.id > tempNode.id) {
			nodeLog("currentNode.id > tempNode.id");
			if (key <= tempNode.id || key >= currentNode.id) {
				nodeLog("key <= tempNode.id || key >= currentNode.id");
				return currentNode;
			}
			else {
				tempNode = findClosestPrecedingFinger(key);
				return tempNode;
			}
		}
		else {
			nodeLog("currentNode.id <= tempNode.id");
			if (key > currentNode.id || key <= tempNode.id) {
				nodeLog("key > currentNode.id || key <= tempNode.id");
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
				nodeLog("Making call to FCPF");
				tempNode = findClosestPrecedingFinger(key);
				nodeLog("FCPF current node: " + Integer.toString(currentNode.id));
				nodeLog("FCPF node id found: " + Integer.toString(tempNode.id));*/
			//}
		}
		nodeLog("while key<> loop exited, returning tempNode: " + Integer.toString(tempNode.id));
		return tempNode;
	}

	@Override
	public NodeData findClosestPrecedingFinger(int key) {
		nodeLog("Finding closest finger for key: " + Integer.toString(key) + " @ node ID: " + Integer.toString(currentNode.id));
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
		nodeLog("getNodeSuccessor reached @ node " + Integer.toString(currentNode.id) + ":" + Integer.toString(currentNode.port));
		if (nodes.size() > 0) {
			NodeData s = nodes.get(0);
			nodeLog("getNodeSuccessor: " + Integer.toString(currentNode.id) + ":" + Integer.toString(currentNode.port) + " -> " + Integer.toString(s.id) + ":" + Integer.toString(s.port));
			return s;
		}
		else {
			nodeLog("No successor node @ node:Port -> " + Integer.toString(currentNode.id) + ":" + Integer.toString(currentNode.port));
			return null;
		}
	}

	@Override
	public void setNodeSuccessor(NodeData successor) {
		if (nodes.size() > 0) {
			nodes.set(0, successor);
			nodeLog("Node " + Integer.toString(currentNode.id) + " Successor updated to " + Integer.toString(successor.id));
		}
	}


	
	public static int getHashKey(String input) {
		try {
			nodeLog("Attempting to get hash for string: " + input);
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

		nodeLog("INITIALIZING NODE CONNECTION TO SUPERNODE");
		//int port = Integer.parseInt(args[0]);
		//currentNode.port = port;
		currentNode.id = getHashKey(Integer.toString(currentNode.port));
		//boolean server_start = startNodeServer(port);

		TTransport transport = new TSocket("localhost", 9090);
		TProtocol protocol = new TBinaryProtocol(transport);
		Supernode.Client client = new Supernode.Client(protocol);
		//Try to connect
		transport.open();
		nodeLog("Node Connected to SN");

		//Start local server for node mesh
		//Check for sys.arg
		//TODO: error checking for arg length
		nodeLog("Node @ port:" + Integer.toString(currentNode.port) + " configuring");
		nodeLog("Node ID: " + currentNode.id);

		//ask supernode about who to talk to to get successor/predecessor
		//client.getNode();
		NodeData nodeData = client.join(currentNode.port);
		if (nodeData.port != 0) {
			//nodes.add(nodeData);
			//boolean test = testNodeClientConn(nodeData);
			//testGetSuccessor(nodeData);
		}
		else {
			nodeLog("Empty node returned - building fresh finger table");
			predNode = currentNode;
			for (int i = 0; i < 4; i++) {
				int key = currentNode.id + (int)(Math.pow(2, i));
				key = key % 16;
				currentNode.finger = key;
				nodes.add(currentNode);
				nodeLog(Integer.toString(i) + ": " + Integer.toString(key) + " <> Successor: " + Integer.toString(currentNode.id));
			}
		}
		client.postJoin(currentNode.port);
		while(true);*/

	}
}
/*
	private static void testGetSuccessor(NodeData fingerNode) throws org.apache.thrift.TException {
		nodeLog("testGetSuccessor ID:Port -> " + Integer.toString(currentNode.id) + ":" + Integer.toString(currentNode.port));
		//nodeLog("testGetPred ID:Port -> " + Integer.toString(currentNode.id) + ":" + Integer.toString(currentNode.port));
		int i = 0;
		//for (int i = 0; i < 4; i++) {
		//	NodeData fingerNode = nodes.get(0);
			//int key = currentNode.id + (int)(Math.pow(2, i));
			//key = key % 16;
			int key = currentNode.id;
			nodeLog("Connecting to fingerNode: " + Integer.toString(fingerNode.id) + ":" + Integer.toString(fingerNode.port));
			TTransport transport = new TSocket("localhost", fingerNode.port);
			TProtocol protocol = new TBinaryProtocol(transport);
			Node.Client client = new Node.Client(protocol);
			//Try to connect
			nodeLog("opening transport");
			transport.open();
			nodeLog("Calling client.findSuccessor");
			NodeData n = client.findSuccessor(key);
			nodeLog("Calling client.findPred");
			NodeData predNode = client.findPred(key);
			nodes.add(n);
			transport.close();
			nodeLog("Key: " + Integer.toString(key) + " <> Successor: " + Integer.toString(n.id));


			TTransport predTransport = new TSocket("localhost", predNode.port);
			TProtocol predProtocol = new TBinaryProtocol(predTransport);
			Node.Client predClient = new Node.Client(predProtocol);
			//Try to connect
			predTransport.open();
			predClient.setNodeSuccessor(currentNode);
			predClient.updateDHT();
			predTransport.close();			
			//nodeLog("Key: " + Integer.toString(key) + " <> Predecessor: " + Integer.toString(n.id));
		//}
	}*/


	/*public static boolean startNodeServer(int port) throws TException {
		nodeLog("Starting node server at port " + Integer.toString(port));
		TServerTransport serverTransport = new TServerSocket(port);
                TTransportFactory factory = new TTransportFactory();
                NodeHandler nodeHandler = new NodeHandler();
                Node.Processor nodeProcessor = new Node.Processor(nodeHandler);
                TThreadPoolServer.Args serverArgs = new TThreadPoolServer.Args(serverTransport);
                serverArgs.processor(nodeProcessor); //Set handler
                serverArgs.transportFactory(factory); //Set FramedTransport (for performance)
                //Run server as multithread
                TServer server = new TThreadPoolServer(serverArgs);
		nodeLog("Node Server Started");
                //server.serve();
		return true;
	}*/
/*public static boolean testNodeClientConn(NodeData node) throws TException {
		nodeLog("INITIALIZIZING NODE " + Integer.toString(currentNode.port) + " CONNECTION TO NODE @ PORT: " + Integer.toString(node.port));
		TTransport transport = new TSocket("localhost", node.port);
		TProtocol protocol = new TBinaryProtocol(transport);
		Node.Client client = new Node.Client(protocol);
		//Try to connect
		transport.open();
		nodeLog("Nodes connected");
		//String getTest = client.get("testbook");
		//nodeLog("testNodeClientConn getTest == " + getTest);
		//Build original finger table
		nodes.clear();
		for (int i = 0; i < 4; i++) {
			int key = currentNode.id + (int)(Math.pow(2, i));
			key = key % 16;
			NodeData n = client.findSuccessor(key);
			nodes.add(n);
			nodeLog("Key: " + Integer.toString(key) + " <> Successor: " + Integer.toString(n.id));
		}
		transport.close();
		return true;
	}*/

