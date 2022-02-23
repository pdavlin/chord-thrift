import org.apache.thrift.TException;

public class MultiplyHandler implements Multiply.Iface {
	@Override
		public boolean ping() throws TException {return true;}
	@Override
	public int multiply_1(Numbers values) throws TException {
	return values.x * values.y;
	}
	@Override
	public int multiply_2(int x, int y) throws TException {
	return x * y;
	}	
}
