import java.util.*;

public class XClass {
	private int ID;
	private int committedValue;
	private int tempValue;
	private boolean lock;
	private boolean copy;
	private String lockType;
	private String lockID;
	private ArrayList<XClassHistory> history;
	
	public XClass(int id){
		ID = id;
		committedValue = ID * 10;
		tempValue = committedValue;
		lock = false;
		lockType = "NULL";
		lockID = "NULL";
		copy = false;
		history = new ArrayList<XClassHistory>();
		XClassHistory xh = new XClassHistory(0,committedValue);
		history.add(xh);
		history.add(xh);
		
	}
	
	public int getID(){
		return ID;
	}
	
	public void setID(int id){
		ID = id;
	}
	
	public int getCommittedValue(){
		return committedValue;
	}
	
	public void setCommittedValue(int v){
		committedValue = v;
	}
	
	public int getTempValue(){
		return tempValue;
	}
	
	public void setTempValue(int v){
		tempValue = v;
	}
	
	public boolean isLock(){
		return lock;
	}
	
	public void setLock(boolean tf){
		lock = tf;
		if (tf = false){
			lockType = "NULL";
			lockID = "NULL";
		}
			
	}
	
	public String getLockType(){
		return lockType;
	}
	
	public void setLockType(String lt){
		lockType = lt;
	}
	
	public String getLockID(){
		return lockID;
	}
	
	public void setLockID(String lID){
		lockID = lID;
		
	}
	
	public boolean isCopy(){
		return copy;
	}
	
	public void setCopy(boolean tf){
		copy = tf;
	}
	
	public void doCopy(XClass x){
		ID = x.getID();
		this.setLock(x.isLock());
		lockType = x.getLockType();
		lockID = x.getLockID();
		committedValue = x.getCommittedValue();
		tempValue = x.getTempValue();
		copy = true;
	}
	
	public ArrayList<XClassHistory> getHistory(){
		return history;
	}
}
