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
import shared.*;

public class Server {

	public static void main(String[] args) throws TException {
		TServerTransport serverTransport = new TServerSocket(9090);
		TTransportFactory factory = new TTransportFactory();


		NodeHandler nodeHandler = new NodeHandler();
		Node.Processor nodeProcessor = new Node.Processor();

		TServer.Args serverArgs = new TServer.Args(serverTransport);
		serverArgs.processor(nodeProcessor);
		serverArgs.trasportFactory(factory);

		TServer server = new TSimpleServer(serverArgs);
		server.serve();
	}
}
