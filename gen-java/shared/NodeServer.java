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

public class NodeServer {
        

	public static void main(String[] args) throws TException {
		int port = Integer.parseInt(args[0]);	
		System.out.println("NS:  Starting node server at port " + Integer.toString(port));
                TServerTransport serverTransport = new TServerSocket(port);
                TTransportFactory factory = new TTransportFactory();
                NodeHandler nodeHandler = new NodeHandler(port);
                Node.Processor nodeProcessor = new Node.Processor(nodeHandler);
                TThreadPoolServer.Args serverArgs = new TThreadPoolServer.Args(serverTransport);
                serverArgs.processor(nodeProcessor); //Set handler
                serverArgs.transportFactory(factory); //Set FramedTransport (for performance)
                //Run server as multithread
                TServer server = new TThreadPoolServer(serverArgs);
                System.out.println("NS:  Node Server Started");
                server.serve();
	}
}
