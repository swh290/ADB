
public class Operation {
	
	enum ActionType{
		read,
		write,
		commit
	}
	
	ActionType opType = ActionType.read;
	private int value = -1;
	private int target = -1;
	private int timeStamp = 0;
	
	public Operation(ActionType type,int v,int location,int ts){
		opType = type;
		value = v;
		target = location;
		timeStamp = ts;
	}
	
	public ActionType getOperationType(){
		return opType;
	}
	
	public void setActionType(ActionType at){
		opType = at;
	}
	
	public int getValue(){
		return value;
	}
	
	public void setValue(int v){
		value = v;
	}
	
	public int getTarget(){
		return target;
	}
	
	public void setTarget(int location){
		target = location;
	}
	
	public int getTimeStamp(){
		return timeStamp;
	}
	
	public void setTimeStamp(int ts){
		timeStamp = ts;
	}
}
