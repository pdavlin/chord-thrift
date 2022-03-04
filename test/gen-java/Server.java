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

public class Server {

	public static void main(String[] args) throws TException {
		System.out.println("STARTING SERVER");
		//Create Thrift server socket Server.java
		TServerTransport serverTransport = new TServerSocket(9090);
		TTransportFactory factory = new TTransportFactory();
		//Create service request handler
		MultiplyHandler handler = new MultiplyHandler();
		Multiply.Processor processor = new Multiply.Processor(handler);
		//Set server arguments
		TThreadPoolServer.Args serverArgs = new TThreadPoolServer.Args(serverTransport);
		serverArgs.processor(processor); //Set handler
		serverArgs.transportFactory(factory); //Set FramedTransport (for performance)
		//Run server as a single thread
		TServer server = new TThreadPoolServer(serverArgs);
		server.serve(); 
	}
}
