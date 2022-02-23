import org.apache.thrift.TException;

public class SupernodeHandler implements Supernode.Iface {
	@Override
	public boolean ping() throws TException {return true;}
}
