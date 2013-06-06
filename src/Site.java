import java.util.*;

public class Site {
	private int ID;
	private ArrayList<XClass> Xs;
	private LockManager LM;
	private DataManager DM;
	private boolean failed;
	private String lockmsg;
	private String lockByTid;

	
	public Site(int id){
		ID = id;
		Xs = new ArrayList<XClass>();
		LM = new LockManager();
		DM = new DataManager();
		failed = false;
		initial_XClass();
		lockmsg = "NULL";
//		backupID = -1;
		lockByTid = "NULL";
	}
	
	public void initial_XClass(){
		for (int i = 1; i <= 10; i++) {
			XClass X = new XClass(i * 2);
			this.Xs.add(X);
			
		}
		for (int i = 1; i < 20; i += 2) {
			int temp = ((1 + i) % 10 == 0) ? 10 : (1 + i) % 10;
			if (temp == ID) {
				XClass X = new XClass(i);
				this.Xs.add(X);
				
			}
		}
	}
	
	public boolean isDown(){
		return failed;
	}
	
	public int getID(){
		return ID;
	}

	public ArrayList<XClass> getXClass(){
		return Xs;
	}
	
	public String getLockMsg(){
		return lockmsg;
	}

	public void setLockMsg(String tid){
		lockmsg = tid;
	}
	
	public void resetLockMsg(){
		lockmsg = "NULL";
	}
	
	public int readOnly(int XID, String tid, int beginTimeStamp){
		for(int i = 0; i < Xs.size(); i++){
			//find x
			if(Xs.get(i).getID() == XID){
					return DM.readOnly(Xs.get(i).getHistory(), beginTimeStamp);
			}
		}
		//not find x
		System.out.println("Read X" + XID + " from site" + ID + " fails, because it doesn't exist.");
		return -1;
	}
	
	public int readData(int XID, String tid){
		for(int i = 0; i < Xs.size(); i++){
			
			//find x
			if(Xs.get(i).getID() == XID){
				
				//x is not locked
				if(Xs.get(i).getLockType() != "Write"){
					LM.setRead(Xs.get(i), tid);
					this.resetLockMsg();
					return DM.readPreviousData(Xs.get(i));
				}
				//x is locked by itself
				else if(Xs.get(i).getLockID().contains(tid)){
					
					this.resetLockMsg();
					return DM.readData(Xs.get(i));
				}
				//x is locked by others
				else{
					this.setLockMsg(Xs.get(i).getLockID());
					return -1;
				}
			}
		}
		//not find x
		System.out.println("Read X" + XID + " from site" + ID + " fails, because it doesn't exist.");
		return -1;
	}
	
	public int writeData(int XID, int v, String tid){
		for(int i = 0; i < Xs.size(); i++){
			
			//find x
			if(Xs.get(i).getID() == XID){
				//x is not locked
				if(!Xs.get(i).isLock()){
					LM.setWrite(Xs.get(i), tid);
					DM.writeData(Xs.get(i), v);
					this.resetLockMsg();
					return v;
				}
				//x is locked by itself
				else if(Xs.get(i).getLockID().contains(tid)){
					//upgrade to write lock
					LM.setWrite(Xs.get(i), tid);
					DM.writeData(Xs.get(i), v);
					this.resetLockMsg();
					return v;
				}
				//x is locked by others
				else{
					this.setLockMsg(Xs.get(i).getLockID());
					return -1;
				}
			}
		}
		//not find x
		System.out.println("Write X" + XID + " into site" + ID + " fails, because it doesn't exist.");
		return -1;
	}
	
	public String fail(){
		failed = true;
		DM.fail(LM, Xs);
		return lockByTid;
	}
	
	public ArrayList<String> recovery(Site target){
		ArrayList<String> a = new ArrayList<String>();
		failed = false;
		
		if(!target.isDown()){
			lockByTid = "NULL";
			return DM.recovery(Xs, target.getXClass());
			
		}
		else {
			System.out.println("target site" + target.getID() + " is down");
			return a;
		}
	}
	
	public void Abort(int XID){
		for(int i = 0; i < Xs.size(); i++){
			if(Xs.get(i).getID() == XID){
				DM.abort(LM, Xs.get(i));
			}
		}	
	}
	
	public void AbortT(String tid){
		for(int i = 0; i < Xs.size(); i++){
			if(Xs.get(i).getLockID().contains(tid)){
				DM.abort(LM, Xs.get(i));
			}
		}	
	}
	
	public String ByLock(int XID){
		for(int i = 0; i < Xs.size(); i++){
			if(Xs.get(i).getID() == XID){
				return "X" + XID + " is " + Xs.get(i).getLockType() 
					+ " locked by "  + Xs.get(i).getLockID();
			}
		}
		return "X" + XID + " doesn't exist in this site."; 
	}
	
	public String ToString(){
		String s;
		if(failed){
			s = "This site is down.\n";
		}
		else{
			s = "This site is up.\n";
		}
		for(int i = 0; i < Xs.size(); i++){
			s += "X" + Xs.get(i).getID() + " = " 
					+ Xs.get(i).getCommittedValue() + "\n";
		}
		return s;
	}
	
	public boolean hasX(int intX){
		for (int i=0;i<Xs.size();i++){
			if (Xs.get(i).getID() == intX)
				return true;
		}
		return false;
			
	}
	
	public void accessSite(String t_id){
		
		lockByTid = t_id;
//		for (int i=0;i<accessedTransactions.size();i++)
//			System.out.println("QQQQQQQQQQQQQQQQQQQ"+accessedTransactions.get(i));
	}
	
	public void commit(int intX, int timeStamp){
		for (int i=0;i<Xs.size();i++){
			if (Xs.get(i).getID() == intX){
				DM.commitData(Xs.get(i), timeStamp);
				LM.release(Xs.get(i));
			}
		}
	}
}
