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
		System.out.println("STARTING CLIENT");
		TTransport transport = new TSocket("localhost", 9090);
		TProtocol protocol = new TBinaryProtocol(transport);
		Multiply.Client client = new Multiply.Client(protocol);
		//Try to connect
		transport.open();
		//What you need to do.
		int ret = client.multiply_2(3, 5);	
		System.out.println("Ret " + Integer.toString(ret));
	}
}
