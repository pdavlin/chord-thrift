import org.apache.thrift.TException;

public class NodeHandler implements Node.Iface {
	@Override
	public boolean ping() throws TException {return true;}
}
