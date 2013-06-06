import java.util.*;

public class DataManager {
	
	/**
	 * @param X
	 * @return
	 */
	public int readData(XClass X){
		return X.getCommittedValue();
	}

	public int readOnly(ArrayList<XClassHistory> XHistory, int beginTimeStamp){
		int ROvalue = -1;
		for (int i=0;i<XHistory.size()-1;i++){
//			System.out.println(XHistory.get(i).getTimeStamp()+" "+XHistory.get(i).getValue()+" ");		
			if (XHistory.get(i).getTimeStamp() <= beginTimeStamp && XHistory.get(i+1).getTimeStamp() > beginTimeStamp)
				ROvalue = XHistory.get(i).getValue();
		}
		if (ROvalue == -1 && beginTimeStamp >= XHistory.get(XHistory.size()-1).getTimeStamp())
			ROvalue = XHistory.get(XHistory.size()-1).getValue();
		return ROvalue;
	}
	/**
	 * @param X
	 * @return
	 */
	public int readPreviousData(XClass X){
		return X.getTempValue();
	}

	/**
	 * @param X
	 * @param v
	 */
	public void writeData(XClass X, int v){
//		X.setPreviousValue(X.getValue());
		X.setTempValue(v);
	}
	
	/**
	 * @param X
	 */
	public void commitData(XClass X, int timeStamp){
		X.setCommittedValue(X.getTempValue());
		XClassHistory xh = new XClassHistory(timeStamp, X.getCommittedValue());
		X.getHistory().add(xh);
	}
	
	/**
	 * @param LM
	 * @param Xs
	 */
	public void fail(LockManager LM, ArrayList<XClass> Xs){
		for(int i  = 0; i < Xs.size(); i++)
		{
			if(Xs.get(i).isLock())
			{
//				LM.release(Xs.get(i));
				Xs.get(i).setTempValue(Xs.get(i).getCommittedValue());
			}
		}
	}
	
	ArrayList<String> a;
	
	/**
	 * @param Xs
	 * @param tXs
	 * @return
	 */
	public ArrayList<String> recovery(ArrayList<XClass> Xs, ArrayList<XClass> tXs){
		for(int i  = 0; i < Xs.size(); i++){
			for(int j  = 0; j < tXs.size(); j++){
				if(Xs.get(i).getID() == tXs.get(j).getID()){
					Xs.get(i).doCopy(tXs.get(j));
					Xs.get(i).setCopy(false);
					Xs.get(i).setLock(false);
					Xs.get(i).setLockID("NULL");
					Xs.get(i).setLockType("NULL");
				}
			}
		}
		return a;
	}

	/**
	 * @param LM
	 * @param X
	 */
	public void abort(LockManager LM, XClass X){
		X.setTempValue(X.getCommittedValue());
		LM.release(X);
	}
}
