import java.util.*;

public class Transaction {
	
	enum Attribute
	{
		ReadOnly,
		ReadWrite
	}
	
	private int timestamp = 0;
	private String ID = "";
	private ArrayList<Operation> operations;
	private Transaction.Attribute attribute;
	private Map<Integer,Integer> readOnlyList;
	
	public Transaction(String id, int ts, Attribute att, Map<Integer,Site> sites){
		operations =new ArrayList<Operation>();
		ID = id;
		timestamp = ts; 
		attribute = att;
		readOnlyList = new HashMap<Integer,Integer>();
		if (att == Attribute.ReadOnly){
			for (int i=1;i<=sites.size();i++)
				for (int j=0;j<sites.get(i).getXClass().size();j++)
					if (!readOnlyList.containsKey(sites.get(i).getXClass().get(j).getID()) && !sites.get(i).isDown())
						readOnlyList.put(sites.get(i).getXClass().get(j).getID(), sites.get(i).getXClass().get(j).getCommittedValue());
		}
	}
	public ArrayList<Operation> getOperation(){
		return operations;
	}
	public Attribute getAttribute(){
		return attribute;
	}

	public String getID(){
		return ID;
	}

	public int getTimeStamp(){
		return timestamp;
	}

	public Collection<Operation> getOperations(){
		return operations;
	}
	
	public void insertOperation(Operation op){
		operations.add(op);
	}

	public void removeOperation(int location){
		if (location > operations.size())
			return;
		operations.remove(location);
	}

	public void replace(ArrayList<Operation> newOp){
		operations = newOp;
	}
	
	public Map<Integer,Integer> getReadOnlyList(){
		return readOnlyList;
	}
}
