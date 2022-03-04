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
		TServerTransport serverTransport = new TServerSocket(9090);
		TTransportFactory factory = new TTransportFactory();


		SupernodeHandler supernodeHandler = new SupernodeHandler();

		Supernode.Processor supernodeProcessor = new Supernode.Processor(supernodeHandler);

		TThreadPoolServer.Args serverArgs = new TThreadPoolServer.Args(serverTransport);
                serverArgs.processor(supernodeProcessor); //Set handler
                serverArgs.transportFactory(factory); //Set FramedTransport (for performance)
		//Run server as multithread
		TServer server = new TThreadPoolServer(serverArgs);
		server.serve();
	}
}
