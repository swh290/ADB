
public class LockManager {

	public void setRead(XClass X, String tID){
		X.setLock(true);
		X.setLockType("Read");
		X.setLockID(tID);
	}
	
	public void setWrite(XClass X, String tID){
		X.setLock(true);
		X.setLockType("Write");
		X.setLockID(tID);
	}
	
	public void release(XClass X)
	{
		X.setLock(false);
		X.setLockType("NULL");
		X.setLockID("NULL");
	}
}
