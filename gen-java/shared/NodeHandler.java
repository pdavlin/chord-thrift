import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.thrift.TApplicationException;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.slf4j.helpers.MessageFormatter;

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

	public void initNode() throws TException {
		// int port = Integer.parseInt(args[0]);
		// currentNode.port = port;
		currentNode.id = getHashKey(Integer.toString(currentNode.port));
		loggingNodeId = Integer.toString(currentNode.id);
		// boolean server_start = startNodeServer(port);

		nodeLog("INITIALIZING NODE CONNECTION TO SUPERNODE");

		TTransport transport = new TSocket("localhost", 9090);
		TProtocol protocol = new TBinaryProtocol(transport);
		Supernode.Client client = new Supernode.Client(protocol);
		// Try to connect
		nodeLog("opening transport on port {}", 9090);
		transport.open();
		nodeLog("Node Connected to SN");

		// Start local server for node mesh
		// Check for sys.arg
		// TODO: error checking for arg length
		nodeLog("Configuring node {} at port {}", currentNode.id, currentNode.port);

		// ask supernode about who to talk to to get successor/predecessor
		// client.getNode();
		// NodeData nodeData = client.join(currentNode.port);
		NodeData nodeData = new NodeData();
		try {
			nodeData = client.join(currentNode.port);
		} catch (TApplicationException tae) {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException ie) {
				nodeLog("Interrupted");
			}
			nodeData = client.join(currentNode.port);
		}
		if (nodeData.port != 0) {
			// nodes.add(nodeData);
			// findSuccessor(currentNode.id);
			int key = currentNode.id;
			nodeLog("Connecting to fingerNode {}: port {}", nodeData.id, nodeData.port);
			TTransport nodeTransport = new TSocket("localhost", nodeData.port);
			TProtocol nodeProtocol = new TBinaryProtocol(nodeTransport);
			Node.Client nodeClient = new Node.Client(nodeProtocol);
			// Try to connect
			nodeLog("opening transport on port {}", nodeData.port);
			nodeTransport.open();
			nodeLog("Calling client.findSuccessor");
			NodeData n = nodeClient.findSuccessor(key);
			nodes.add(n);
			nodeLog("closing transport on port {}", nodeData.port);
			nodeTransport.close();
			nodeLog("Calling client.findPred");
			// NodeData predNode = nodeClient.findPred(key);
			predNode = findPred(key);

			nodeLog("Key {} successor: {}", key, n.id);

			TTransport predTransport = new TSocket("localhost", predNode.port);
			TProtocol predProtocol = new TBinaryProtocol(predTransport);
			Node.Client predClient = new Node.Client(predProtocol);
			// Try to connect
			nodeLog("opening transport on port {}", predNode.port);
			predTransport.open();
			predClient.setNodeSuccessor(currentNode);
			// predClient.updateDHT();
			nodeLog("closing transport on port {}", predNode.port);
			predTransport.close();
			// Update current node finger table
			updateDHT();
			nodeLog("opening transport on port {}", predNode.port);
			predTransport.open();
			// Update other node finger tables
			predClient.updateDHT();
			nodeLog("closing transport on port {}", predNode.port);
			predTransport.close();
			client.postJoin(currentNode.port);
		} else {
			nodeLog("Empty node returned - building fresh finger table");
			predNode = currentNode;
			for (int i = 0; i < 4; i++) {
				int key = currentNode.id + (int) (Math.pow(2, i));
				key = key % 16;
				currentNode.finger = key;
				nodes.add(currentNode);
				nodeLog("Key {} successor: {}", key, currentNode.id);
			}
			nodeLog("New Node finger table size: {}", nodes.size());
			client.postJoin(currentNode.port);
		}
	}

	@Override
	public boolean setBook(java.lang.String book_title, java.lang.String genre) throws org.apache.thrift.TException {
		int key = getHashKey(book_title);
		String fullHash = getHash(book_title);
		NodeData node = findSuccessor(key);
		if (currentNode.id == key || node.id == currentNode.id) {
			nodeLog("bookList.put key: {} on node {}", key, currentNode.id);
			bookList.put(fullHash, genre);
			return true;
		}
		else {
			TTransport transport = new TSocket("localhost", node.port);
			TProtocol protocol = new TBinaryProtocol(transport);
			Node.Client client = new Node.Client(protocol);
			// Try to connect
			nodeLog("opening transport on port {}", node.port);
			transport.open();
			nodeLog("attempting client.setBook()");

			
			boolean n = client.setBook(book_title, genre);
			nodeLog("client.setBook() returned");
			nodeLog("closing transport on port {}", node.port);
			transport.close();
		}
		return true;
	}

	@Override
	public java.lang.String get(java.lang.String book_title) throws org.apache.thrift.TException {
		nodeLog("Node: {} attempting to get() for book {}", currentNode.port, book_title);
		nodeLog("Title hash key: {}", getHashKey(book_title));
		String genre = "";
		int key = getHashKey(book_title);
		String fullHash = getHash(book_title);
		NodeData node = findSuccessor(key);
		if (currentNode.id == key || node.id == currentNode.id) {
			nodeLog("bookList.get key: {} on node {}", key, currentNode.id);
			if (bookList.containsKey(fullHash)) {
				genre = bookList.get(fullHash);
			}
			else {
				nodeLog("key: {} does not exist on node: {}", key, currentNode.id);
			}
		}
		else {
			TTransport transport = new TSocket("localhost", node.port);
			TProtocol protocol = new TBinaryProtocol(transport);
			Node.Client client = new Node.Client(protocol);
			// Try to connect
			nodeLog("opening transport on port {}", node.port);
			transport.open();
			nodeLog("attempting client.get()");

			
			String n = client.get(book_title);
			nodeLog("client.get() returned");
			nodeLog("closing transport on port {}", node.port);
			transport.close();
		}
		nodeLog("genre for book:  {} :: {}", book_title, genre);
		return genre;
	}

	@Override
	public void updateDHT() throws org.apache.thrift.TException {
		nodeLog("Updating DHT for {}:{}", currentNode.id, currentNode.port);
		nodeLog("Current Finger Table size: {}", nodes.size());
		for (int i = 1; i < 4; i++) {
			nodeLog("Trying to get fingertable node {}", i);
			// NodeData fingerNode = nodes.get(i);
			int key = currentNode.id + (int) (Math.pow(2, i));
			key = key % 16;
			NodeData n = findSuccessor(key);
			nodeLog("NodeData n returned, trying to add to finger table");
			try {
				nodes.set(i, n);
			} catch (IndexOutOfBoundsException e) {
				nodes.add(n);
			}
			nodeLog("{}: Key {} successor: {}", i, key, n.id);
		}
		printNodesInTable("updateDHT completed: ");
	}

	@Override
	public NodeData findSuccessor(int key) throws TException {
		nodeLog("Finding successor for key {}", key);
		// Key found
		if (key == currentNode.id) {
			nodeLog("Current node matches key {}", key);
			return currentNode;
		}
		// Only node in cluster will hold all keys
		if (nodes.size() == 0 || nodes.size() == 1) {
			nodeLog("No nodes in finger table, this node will hold all keys");
			predNode = currentNode;
			return currentNode;
		}
		NodeData predecessor = findPred(key);

		nodeLog("predecessor found: {}", predecessor.id);
		if (predecessor.id == currentNode.id) {
			return nodes.get(0);
		}
		if (predecessor.port == currentNode.port) {
			return nodes.get(0);
		}

		/*for (int i = 0; i < nodes.size(); i++) {
			int fingerkey = currentNode.id + (int) (Math.pow(2, i));
                        fingerkey = fingerkey % 16;
			NodeData finger = nodes.get(i);
			if (fingerkey == predecessor.id) {
				return finger;
			}
		}*/

		TTransport transport = new TSocket("localhost", predecessor.port);
		TProtocol protocol = new TBinaryProtocol(transport);
		Node.Client client = new Node.Client(protocol);
		// Try to connect
		nodeLog("opening transport on port {}", predecessor.port);
		if (!transport.isOpen()) {
			transport.open();
		}
		nodeLog("attempting client.getNodeSuccessor()");


		// TODO: locking here when adding node 2 to cluster
		NodeData n = client.getNodeSuccessor();
		nodeLog("client.getNodeSuccessor() returned");
		nodeLog("closing transport on port {}", predecessor.port);
		transport.close();
		// return findPred(key);
		return n;
	}

	@Override
	public NodeData findPred(int key) {

		printNodesInTable("findPred");

		int currentId = currentNode.id;
		nodeLog("Finding predecessor for key {}", key);
		// Compare node to successor
		nodeLog("Current Node: {}", currentId);

		NodeData tempNode = nodes.get(0);
		int tempId = tempNode.id;
		nodeLog("successor ID: {}", tempNode.id);

		if (currentId < tempId) {
			nodeLog("Current Id {} < tempId {} ", currentId, tempId);
			if (key > currentId && key <= tempId) {
				nodeLog("Key > Current Id {} and <= tempId {} ", currentId, tempId);
				nodeLog("Predecessor for key {}: {}", key, currentId);
				return currentNode;
			}
			
			else {
				tempNode = findClosestPrecedingFinger(key);
			}
		}
		if (currentId > tempId) {
			nodeLog("Current Id {} > tempId {} ", currentId, tempId);
			if (currentId < key || key <= tempId) {
				nodeLog("Key > Current Id {} or <= tempId {} ", currentId, tempId);
				nodeLog("Predecessor for key {}: {}", key, currentId);
				return currentNode;
			}
			//return currentNode;
		}

		nodeLog("Predecessor for key {}: {}", key, tempNode.id);
		return tempNode;
	}

	private void printNodesInTable(String logPoint) {
		String nodeListTemp = "Current nodes in table at {}: ";
		for (int i = 0; i < nodes.size(); i++) {
			NodeData n = nodes.get(i);
			nodeListTemp = nodeListTemp + Integer.toString(n.id) + " ";
		}
		nodeLog(nodeListTemp, logPoint);
	}

	@Override
	public NodeData findClosestPrecedingFinger(int key) {
		nodeLog("Finding closest finger for key {} at node ID {}", key, currentNode.id);
		printNodesInTable("findClosestPrecedingFinger");
		for (int i = nodes.size() - 1; i >= 0; i--) {
			NodeData f = nodes.get(i);
			nodeLog("finger {} = {}", i, f.id);
			/*if (currentId < tempId) {
				nodeLog("Current Id {} < tempId {} ", currentId, tempId);
				if (key > currentId && key <= tempId) {
					nodeLog("Key > Current Id {} and <= tempId {} ", currentId, tempId);
					nodeLog("Predecessor for key {}: {}", key, currentId);
					return currentNode;
				}
				
				else {
					tempNode = findClosestPrecedingFinger(key);
				}
			}*/
			if (currentNode.id >= key) {
				nodeLog("Current Id {} > key {} ", currentNode.id, key);
				if (f.id > currentNode.id || f.id <= key) {
					nodeLog("f.id > Current Id {} or <= key {} ", currentNode.id, key);
					nodeLog("Predecessor for key {}: {}", key, currentNode.id);
					return f;
				}
				//return currentNode;
			}	
			//else (f.id > currentNode.id && f.id >= key) {
			else if (f.id <= key) {
				return f;
			}
		}
		return currentNode;
	}

	@Override
	public NodeData getNodeSuccessor() {
		nodeLog("getNodeSuccessor reached at node {}: {}", currentNode.id, currentNode.port);
		printNodesInTable("getNodeSuccessor: ");
		if (nodes.size() > 0) {
			NodeData s = nodes.get(0);
			nodeLog("getNodeSuccessor: {}:{} -> {}:{}", currentNode.id, currentNode.port, s.id, s.port);
			return s;
		} else {
			nodeLog("No successor node at {}:{}", currentNode.id, currentNode.port);
			return null;
		}
	}

	@Override
	public void setNodeSuccessor(NodeData successor) {
		if (nodes.size() > 0) {
			nodes.set(0, successor);
			nodeLog("Node {} successor updated to {}", successor.id);
		}
	}
	
	@Override
	public void directUpdateDHT(int s, int i) {
		NodeData f = nodes.get(i);
		if (s < i) {
			if (s >= currentNode.id && s < f.id) {
				NodeData p = predNode;
				//TODO: connct to pred node?
				//p.directUpdateDHT(s, i);
			}

		}
		else {

		}
	}

	public static int getHashKey(String input) {
		try {
			nodeLog("Attempting to get hash for string: {}", input);
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
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}


	public static String getHash(String input) {
		try {
			nodeLog("Attempting to get hash for string: {}", input);
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
			return hex.toString();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	public static int hashToId(String input) {
		char lastChar = input.substring(input.length() - 1).charAt(0);
		return Character.digit(lastChar, 16);
	}

	public static void nodeLog(String msg, Object... objects) {
		String prepend = loggingNodeId != null ? "N" + loggingNodeId + ":  " : "NEW: ";
		System.out.println(prepend + MessageFormatter.arrayFormat(msg, objects).getMessage());
	}

	public static void main(String[] args) throws TException {

	}
}
