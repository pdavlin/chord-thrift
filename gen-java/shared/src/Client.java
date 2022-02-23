import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import java.io.UnsupportedEncodingException;


public class Client {

	public static void main(String[] args) throws TException {
		//int port = args[0];
		int port = 9090;
		System.out.println("starting client on port " + Integer.toString(port));

		TTransport transport = new TSocket("localhost", port);
		TProtocol protocol = new TBinaryProtocol(transport);
		Node.Client client = new Node.Client(protocol);

		transport.open();
		if (client.ping()) {
			print("ping successful");
		}
		else {
			print("ping failed");
		}
	}
}
