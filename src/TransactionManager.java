import java.util.*;
import java.io.*;

public class TransactionManager{
	
	private String outPutFile = ""; //output path
	private String current_state=""; //current state
	private int currentTimeStamp =0; //current Time Stamp
	private Map<String, ArrayList<Operation>> waitList;//a map structure to hold all kind of waiting operation from all transactions
	private Map<String, Transaction> transactions; //transactions in progress
	private Map<Integer, Site> sites; //all sites
	/**
	 * @param output_Filename
	 * initialize TransactionManager
	 */
	public TransactionManager(String output_Filename){
		sites = new HashMap<Integer, Site>();
		transactions = new HashMap<String, Transaction>();
		waitList = new HashMap<String, ArrayList<Operation>>();
		outPutFile = output_Filename;
		//initialize sites
		for(int i=1;i<=10;i++){
			Site s = new Site(i);
			addSite(s);
		}
		
	}
	
	//
	/**
	 * @param site
	 * add site into sites
	 */
	public void addSite(Site site){
		sites.put(site.getID(), site);
		printOutput("Add site " + site.getID());
	}
	
	/**
	 * @param action
	 * read and process input command
	 */
	public void Do(String action){
		//increase time stamp value
		currentTimeStamp++;
		//determine which action to perform
		action = action.trim();
		String[] actions = action.split(";");
		for(int i = 0;i < actions.length; i++){
			//make it lower case
			String temp = actions[i].toLowerCase();
			temp = temp.trim();
			//identify command
			
			if (temp.contains("//")){
				
			}
			//begin a new transaction
			if(temp.contains("begin(")){		
				Begin(temp,currentTimeStamp,Transaction.Attribute.ReadWrite);
			}
			//begin a new RO transaction
			else if(temp.contains("beginro(")){
				Begin(temp,currentTimeStamp,Transaction.Attribute.ReadOnly);
			}
			//Write command
			else if(temp.contains("w(")){
				Write(temp);
			}
			//Read command
			else if(temp.substring(0, 2).compareTo("r(")==0){
				Read(temp);
			}
			//End and Commit Transaction
			else if(temp.contains("end(")){
				int index = temp.indexOf("(");
				String id = temp.substring(index+1, temp.length()-1);
				for (int j=1;j<=sites.size();j++)
					for (int k=0;k<sites.get(j).getXClass().size();k++){
						if(sites.get(j).getXClass().get(k).getLockID() == id)
							sites.get(j).getXClass().get(k).setLock(false);
					}
				Commit("commit(" + id +")");	
			}
			//Abort a Transaction
			else if(temp.contains("abort(")){
				Abort(temp);
			}
			//fail a site
			else if(temp.contains("fail(")){
				fail(temp);
				
			}
			//recover a site
			else if(temp.contains("recover")){
				recovery(temp);
			}
			//print committed X in site
			else if(temp.contains("dump(")){
				Dump(temp);
			}
			else{
				printOutput("Unrecognized command " + temp);	
			}
		
		}
		
		current_state = action;
	}
	
	/**
	 * @param _id
	 * @param timestamp
	 * @param attri
	 * begin a new transaction
	 */
	private void Begin(String _id, int timestamp, Transaction.Attribute attri){
			
			try{
				//get transaction id
				int index = _id.indexOf("(");
				String id = _id.substring(index+1, _id.length()-1);
				
				
				if(transactions.containsKey(id)){//if transaction is already exist
					String msg = "Transaction " + id.toUpperCase() + " already existed in the collection.";
					printOutput(msg);
				}
				else{//create a new transaction object	
					Transaction t = new Transaction(id, timestamp, attri, sites);
					transactions.put(id, t);//insert into map
					printOutput("Transaction " + id.toUpperCase() + " begins");
				}
			}
			catch(Exception e){
				System.out.println("Error in Begin-"+ e.getMessage());
			}
		}
	
	/**
	 * @param inputCommand
	 * process Write
	 */
	private void Write(String inputCommand){
		
		try{
			printOutput("Processing " + inputCommand.toUpperCase());
			
			Transaction t  = null;
			//filter some redundant character
			int index = inputCommand.indexOf("(");
			inputCommand = inputCommand.substring(index+1, inputCommand.length()-1);
			String[] elements = inputCommand.split(",");//split the content by "," separator
			String t_id = elements[0];//transaction id
			t_id = t_id.trim();
			
			String x_id = elements[1];//x's id
			x_id = x_id.substring(x_id.indexOf("x")+1);
			x_id = x_id.trim();  
			
			String x_value = elements[2];
			x_value = x_value.substring(0, x_value.length());
			x_value = x_value.trim();
			//convert both string into int
			int intValue = Integer.parseInt(x_value);
			int intX = Integer.parseInt(x_id);	
			
			//check if transaction is valid 
			if(transactions.containsKey(t_id)){
				t = (Transaction)transactions.get(t_id);
				if(t.getAttribute() == Transaction.Attribute.ReadOnly){
					printOutput("Transaction " + t_id.toUpperCase() + " doesn't allow to write.");
					return;
				}
			}
			else{
				printOutput("Transaction " + t_id.toUpperCase() + " not found");
				return;
			}
			
			int flag = 0;

			flag = writeToData(intX,intValue,t_id);
				
			//insert the new operation into the record
			if(flag == 10){
				Operation op = new Operation(Operation.ActionType.write,intValue,intX,0);
				insertToWaitList(op,t_id);
				printOutput("Write fail because all sites are down, wait for recovery");
			}
				
			else{
				Operation op = new Operation(Operation.ActionType.write, intValue,intX,currentTimeStamp);
				t.insertOperation(op);
			}
			
		}
		catch(Exception e){
			printOutput("Error in Write-"+ e.getMessage());	
		}
		
	}
	
	/**
	 * @param intX
	 * @param intValue
	 * @param t_id
	 * @return
	 * select target sites to write
	 */
	private int writeToData(int intX,int intValue,String t_id){
		int returnVlaue = 0;
		int flag = 0;
		String lockBy = "NULL";

		for (int i=1;i<=sites.size();i++){
			if(!sites.get(i).isDown()){
				if (sites.get(i).hasX(intX)){
					returnVlaue = sites.get(i).writeData(intX, intValue, t_id);
					
					if (returnVlaue == -1)
						lockBy = sites.get(i).getLockMsg();
				}
			}
			else flag++;
		}
		
		if(returnVlaue == -1){
			printOutput("Write X" + intX + " failed, because it is locked by transaction " + lockBy);
			
			//abort if needed
			if(!isAbort(lockBy,t_id)){
				//if not abort insert into waiting list
				printOutput("Transaction " + t_id.toUpperCase()  + " waits");
				Operation op = new Operation(Operation.ActionType.write,intValue,intX,0);
				insertToWaitList(op,t_id);
				
			}
			return -1;
		}
		else{
			//return how many site are down
			return flag;
		}
	
	}

	/**
	 * @param inputCommand
	 * process Read command
	 */
	private void Read(String inputCommand){
		printOutput("Processing " + inputCommand.toUpperCase());
		try{
			int index = inputCommand.indexOf("(");
			inputCommand = inputCommand.substring(index+1, inputCommand.length()-1);		
			String[] elements = inputCommand.split(",");		
			String t_id = elements[0];		
			String x_id = elements[1].trim().substring(1);	
			//check for valid transaction id
			if(!transactions.containsKey(t_id)){
				printOutput("Transaction " + t_id.toUpperCase() + " not found");
				return;
			}
	
			//convert x id into int
			int intX = Integer.parseInt(x_id);

			
			if(!transactions.containsKey(t_id)){
				printOutput("Transaction " + t_id.toUpperCase() + " not found");
				return;
			}
			Transaction t = (Transaction)transactions.get(t_id);
			
			if(t.getAttribute()==Transaction.Attribute.ReadOnly){
				//read from multiversion
				readOnly(t_id,intX);
			}
			else{
				//set read lock
				readWithLock(intX,t_id);
			}
			
		
		}
		catch(Exception e){
			System.out.println("Error in Read-"+ e.getMessage());
		}
	}
	
	/**
	 * @param t_id
	 * @param intX
	 * multiversion read
	 */
	private void readOnly(String t_id,int intX){
		Site s=null;
		int value=0;
		//get target site
		int answer = (intX % 10 ) + 1;
		if( (intX%2) == 1){
			answer = (intX % 10) +1;
			s = (Site)sites.get(answer);
			if(!s.isDown()){
				value = s.readOnly(intX, t_id, transactions.get(t_id).getTimeStamp());
//				value = transactions.get(t_id).getReadOnlyList().get(intX);//this is cheating but works
				printOutput("X" + intX + " is " + value);
			}
			else{//if target site is down, operation insert into waiting list
				printOutput("Site " + answer + " is fail, wait for recovery");
				Operation op = new Operation(Operation.ActionType.read,0,intX,0);
				insertToWaitList(op,t_id);
			}
		}
		else{
			int flag = 0;
			for (int i=1;i<=10;i++){
				s = (Site)sites.get(i);
				if (!s.isDown())
				{
					value = transactions.get(t_id).getReadOnlyList().get(intX);
					printOutput("X" + intX + " is " + value);
					flag = 1;
		        	break;
		        }
			}
			if (flag == 0){//if no site is up
				Operation op = new Operation(Operation.ActionType.read,0,intX,0);
				insertToWaitList(op,t_id);
				printOutput("All sites are fail, wait for recovery");
			}
			
		}
	}

	/**
	 * @param intX
	 * @param t_id
	 * read and set readlock
	 * get target site to read
	 */
	private void readWithLock(int intX,String t_id){
		
		//get target site
		int answer = 0;	
		if( (intX%2) == 1){//odd X index
			answer = (intX % 10) +1;
			Site s = (Site)sites.get(answer);
			if(!s.isDown()){
				readFromData(answer,intX,t_id);
			}
			else{//if fail to read
				printOutput("Site " + answer + " is fail, wait for recovery");
				Operation op = new Operation(Operation.ActionType.read,0,intX,0);
				insertToWaitList(op,t_id);
			}
		}
		else{//even index
			int flag = 0;
			Site s;
			for (int i=1;i<=10;i++){
				s = (Site)sites.get(i);
				if (!s.isDown()){
		        	readFromData(s.getID(),intX,t_id);
		        	flag = 1;
		        	break;
		        }
			}
			if (flag == 0){
				Operation op = new Operation(Operation.ActionType.read,0,intX,0);
				insertToWaitList(op,t_id);
				printOutput("All sites are fail, wait for recovery");
			}	
		}		
	}

	
	/**
	 * @param TargetSite
	 * @param intX
	 * @param t_id
	 * read form site
	 */
	private void readFromData(int TargetSite,int intX,String t_id){
		int value = 0;
		boolean blnSuccess = false;
	
		//at one particular site
		if(!sites.containsKey(TargetSite)){
			printOutput("Site " + TargetSite + " doesn't contained variable x" + intX + ".");
			//impossible will be at backup site, so won't check
			//abort the transaction
			Abort("abort(" + t_id + ")");
			return;
		}
		//target site
		Site s = (Site)sites.get(TargetSite);
		printOutput("target site " + TargetSite);

		value = s.readData(intX, t_id);
			
		//check lock
			if(s.getLockMsg()=="NULL"){ //if no lock on target
				blnSuccess = true;
				printOutput("X" + intX + " is " + value);
				Transaction t = (Transaction)transactions.get(t_id);
				Operation op = new Operation(Operation.ActionType.read,0,intX,currentTimeStamp);
				t.insertOperation(op);
			}
			else if(s.getLockMsg().compareToIgnoreCase(t_id)==0){// if lock by self
				value = s.readData(intX, t_id);
				if (value == -1){
					printOutput("X" + intX + " is locked by " + s.getLockMsg());
				}
				else {
					printOutput("X" + intX + " is " + value);
					blnSuccess = true;
				}
				
			}
			else{
				//if need abort
				if(!isAbort(s.getLockMsg(),t_id)){
					//if not abort then insert into waiting list
					blnSuccess = true;
					printOutput("X" + intX + " is lock by " + s.getLockMsg());
					printOutput("Transaction " + t_id.toUpperCase()  + " waits");
					Operation op = new Operation(Operation.ActionType.read,0,intX,0);
					insertToWaitList(op,t_id);
					//reset message
					s.resetLockMsg();
				}
				else{
					printOutput("Abort because target is locked");
					s.resetLockMsg();
					return;
				}
				
			}
		if(!blnSuccess){
			//save the operation to waiting list
			//time stamp = 0, because hasn't written to site yet
			Operation op = new Operation(Operation.ActionType.read,0,intX,0);
	    	insertToWaitList(op,t_id);
		}
	}

	
	
	/**
	 * @param inputCommand
	 * commit target transaction
	 */
	private void Commit(String inputCommand){
		try{
			printOutput("Processing " + inputCommand);
			
			//get transaction id
			int index = inputCommand.indexOf("(");
			String id = inputCommand.substring(index+1, inputCommand.length()-1);
			if(transactions.containsKey(id)){
				//commit each operation under this transaction
				Transaction t = (Transaction)transactions.get(id); 
				ArrayList<Operation> ops = (ArrayList<Operation>)t.getOperations();
				for(int i = 0 ;i < ops.size(); i++){
					int intX = ops.get(i).getTarget();
					for (int j=1;j<=sites.size();j++){
						if(!sites.get(j).isDown())
							if (sites.get(j).hasX(intX)){
								sites.get(j).commit(intX, currentTimeStamp);
								sites.get(j).setLockMsg("NULL");
								sites.get(j).accessSite("NULL");
							}
					}
				}

				printOutput("Transaction " + id.toUpperCase() + " commited");
				endTransaction("end("+ id + ")");
			}
			else{
				printOutput("Transaction " + id.toUpperCase() + " not found");
			}
			
			
		}
		catch(Exception e){
			printOutput("Error in Commit-"+ e.getMessage());
		}
	}
	
	
	/**
	 * @param _id
	 * end a transaction and process waiting list see if there is available transaction pending
	 */
	private void endTransaction(String _id){
		try{
			
			//get transaction id
			int index = _id.indexOf("(");
			String id = _id.substring(index+1,_id.length()-1);
			if(transactions.containsKey(id)){
				//remove transaction from the map
				transactions.remove(id);
				printOutput("Transaction " + id.toUpperCase() + " removed");
				
				//remove pending operations owned by transaction
				waitList.remove(id);
				//process waiting list
				doWaitList();
			}
			else{
				printOutput("Cannot end transaction " + id.toUpperCase() + " , not found");
			}
		}
		catch(Exception e){
			System.out.println("Error in endTransaction-"+ e.getMessage());
		}
	}
	
	/**
	 * @param inputCommand
	 * process abort command
	 */
	private  void Abort(String inputCommand){
		try{
			printOutput("Processing " + inputCommand);
			//get transaction id
			int index = inputCommand.indexOf("(");
			String id = inputCommand.substring(index+1, inputCommand.length()-1);
			//abort the operation if the transaction not found
			if(!transactions.containsKey(id)){
				printOutput("Transaction " + id.toUpperCase() + " not found");
				return;
			}
			
			Transaction t = (Transaction)transactions.get(id);
			//get a list of operation belongs to this transaction id
			ArrayList<Operation> ops = (ArrayList<Operation>)t.getOperations();
			//loop through every operation
			for(int i = 0 ; i<ops.size();i++){
				Operation op = ops.get(i);
				if((op.getOperationType() == Operation.ActionType.write) ||
						(op.getOperationType() == Operation.ActionType.read)){
					//roll back if is write operation only
					int answer = (op.getTarget() % 10) + 1;
					if(op.getTarget() % 2 == 0){
						//x is at all sites
						Iterator<Site> it = sites.values().iterator();
						while(it.hasNext()){ 
							Site s = (Site)it.next();						
							s.AbortT(id);
						}
						break;
					}
					else{
						Site s = (Site)sites.get(answer);
						s.AbortT(id);
					}
				}
			}
			
			//end transaction
			endTransaction("end("+ id +")");
		}
		catch(Exception e){
			System.out.println("Error in Abort-"+ e.getMessage());
		}
		
	}
	
	/**
	 * @param LockByID
	 * @param RequestID
	 * @return
	 * check if transaction need abort and abort it if needed
	 */
	private boolean isAbort(String LockByID,String RequestID){
		if(transactions.containsKey(LockByID) && transactions.containsKey(RequestID)){
			Transaction t_locked = (Transaction)transactions.get(LockByID);
			Transaction t_request = (Transaction)transactions.get(RequestID);
			if(t_locked.getTimeStamp()<=t_request.getTimeStamp() ){
				Abort("abort(" + RequestID + ")");	//abort
			}
			else{
				return false;//alive
			}
		}
		else{
			printOutput("Cannot make decision because invalid transaction ID");
		}
		return true;
	}
	
	
	/**
	 * @param inputCommand
	 * process recover on a site
	 */
	private void recovery(String inputCommand){
		
		try{
			Site s_backup = null;
			//get site id
			printOutput("Processing " + inputCommand);
			int index = inputCommand.indexOf("(");
			String id = inputCommand.substring(index+1, inputCommand.length()-1);
			int intIndex = Integer.parseInt(id);

			if(!sites.containsKey(intIndex)){
				printOutput("Site " + id + " not found");
				return;
			}

			//get failed site and decide where to backup from
			Site s_down = (Site)sites.get(intIndex);
			//look for backup site
			int backupSite = intIndex+1;
			if (backupSite == 11)
				backupSite = 1;
			int failSiteCount = 1;
			s_backup = (Site)sites.get(backupSite);
			//check whether backup is down also
			while(s_backup.isDown() && failSiteCount<10){
				failSiteCount++;
				backupSite = backupSite+1;
				if (backupSite == 11)
					backupSite = 1;
				s_backup = (Site)sites.get(backupSite);
			}
				
			if(failSiteCount == 10){//if all site are down
				printOutput("Operation failed because all site is down currently");
			}
			else{
				s_down.recovery(s_backup);	
			}
			
		}
		catch(Exception e){
			System.out.println("Error in recovery-"+ e.getMessage());
		}
		
	}
	
	/**
	 * @param inputCommand
	 * fail a site
	 */
	private void fail(String inputCommand){
		try{
			printOutput("Processing " + inputCommand);
			//get site id
			int index = inputCommand.indexOf("(");
			String id = inputCommand.substring(index+1, inputCommand.length()-1);
			int intIndex = Integer.parseInt(id);
			//check for valid site id
			if(!sites.containsKey(intIndex)){
				printOutput("Site " + id + " not found");
				return;
			}
			
			Site s = (Site)sites.get(intIndex);
			s.fail();
			ArrayList<XClass> Xlist = s.getXClass();
			for (int i=0;i<Xlist.size();i++){//abort transactions accessing the fail site
				if (Xlist.get(i).getLockID() != "NULL"){
					printOutput(Xlist.get(i).getLockID()+" aborts due to site "+intIndex+" fail");
					Abort("abort("+Xlist.get(i).getLockID()+")");
				}
			}
				
		}
		catch(Exception e){
			System.out.println("Error in fail-"+ e.getMessage());
		}
	}

	/**
	 * @param inputCommand
	 * print out committed values
	 */
	private void Dump(String inputCommand){
		printOutput("Processing " + inputCommand);
		try{
			if(inputCommand.compareToIgnoreCase("dump()")==0){//dump()
				for (int i=1;i<=sites.size();i++){
					
						printOutput("Site" + i + " " + sites.get(i).ToString());
				}
					
			}
			else if(inputCommand.contains("dump(x")){//dump(xj)
				//dump only a particular variable
				//check for if target site is down
				//get variable id
				int index = inputCommand.indexOf("(x");
				String id = inputCommand.substring(index+2, inputCommand.length()-1);
				
				int intID = Integer.parseInt(id);
				
				for (int i=1;i<=sites.size();i++){
					for (int j=0;j<sites.get(i).getXClass().size();j++){
						if (sites.get(i).getXClass().get(j).getID() == intID)
							if (sites.get(i).isDown())
								printOutput("Site" + i + " is down  X" +intID+  " = " + sites.get(i).getXClass().get(j).getCommittedValue());
							else 
								printOutput("Site" + i + " is up  X" +intID+  " = " + sites.get(i).getXClass().get(j).getCommittedValue());
					}
				}				
			}
			else{//dump(x)
				//dump a site
				int index = inputCommand.indexOf("(");
				String id = inputCommand.substring(index+1, inputCommand.length()-1);
				
				int intID = Integer.parseInt(id);
				Site s = (Site)sites.get(intID);
				if(!s.isDown()){
					printOutput("Site" + id + " " +s.ToString());
				}
				else{
					printOutput("Site" + id + " is down");
				}
			}
		}
		catch(Exception e){
			System.out.println("Error in Dump-"+ e.getMessage());
		}
	}

	/**
	 * @param op
	 * @param t_id
	 * insert pending transaction to waiting list
	 */
	private void insertToWaitList(Operation op,String t_id){
		ArrayList<Operation> ops;
		//check if there is an existing record current transaction
		if(waitList.containsKey(t_id)){
			ops = (ArrayList<Operation>)waitList.get(t_id);
			ops.add(op);
		}
		else{
			//if transaction is not in waiting list
			ops = new ArrayList<Operation>();
			ops.add(op);
			waitList.put(t_id, ops);
		}
	}
	/**
	 * extract transaction form waiting list once a transaction ends
	 */
	private void doWaitList(){
		ArrayList<String> trans = new ArrayList<String>();
		Transaction t;
		int index =0;
		Iterator<String> it = waitList.keySet().iterator();
		while(it.hasNext()){
			trans.add((String)it.next());
		}
		while(trans.size() > 0){
			if(trans.size() == 1){//last transaction in list
				index = 0;
				t = (Transaction)transactions.get(trans.get(index));
				
			}
			else{
				index= getNextFromWaitList(trans);
				t = (Transaction)transactions.get(trans.get(index));
			}
			
			//process the transaction
			doOperationFromWaitList(t.getID());
			
			//remove it from the list after checked
			trans.remove(index);
			
		}
	}

	/**
	 * @param t_id
	 * process the transaction extracted from waiting list
	 */
	private void doOperationFromWaitList(String t_id){
		ArrayList<Operation> ops = (ArrayList<Operation>)waitList.get(t_id);
		ArrayList<Operation> tempOps = new ArrayList<Operation>();
		for(int i = 0;i<ops.size();i++){
			tempOps.add(ops.get(i));
		}

		for(int i=0;i<tempOps.size();i++){
			ops.remove(0);
			if(ops.size()==0){
				waitList.remove(t_id);
			}
			//do command
			Operation op = tempOps.get(i);
			if(op.getOperationType()==Operation.ActionType.commit){
				Do("end(" + t_id + ")");
			}
			else if(op.getOperationType()==Operation.ActionType.write){
				Do("w(" + t_id + ",x" + op.getTarget()+"," +op.getValue()+ ")");
			}
			else{
				Do("r(" + t_id + ",x" + op.getTarget() +")");
			}
		}
	}

	/**
	 * @param trans
	 * @return
	 * get next transaction in waiting list
	 */
	private int getNextFromWaitList(ArrayList<String> trans){
		int index = 0;
		Transaction t_keep = null;
		Transaction t_temp = null;
		String t_id="";
		for(int i = 0;i<trans.size();i++){
			if(i==0){
				t_id = trans.get(0);
				t_keep = (Transaction)transactions.get(t_id);
			}
			else{
				t_id = trans.get(i);
				t_temp = (Transaction)transactions.get(t_id);
				//compare both time stamp
				if(t_keep.getTimeStamp()>t_temp.getTimeStamp()){
					//get the transaction location in array list
					index = i;
					//swap if the holding transaction is younger with current
					t_keep = t_temp;
				}
			}
				
		}
		return index;
	}
	
	
	/**
	 * @param output
	 * write output to file
	 */
	private void printOutput(String output){
		boolean isExist = false;
		try {
	        //check for existing file
			File f = new File(outPutFile);
			isExist = f.exists();
			if(!isExist){
				//create a new file if doesn't exist
				isExist = f.createNewFile();
				f=null;
			}
			if(isExist){
				BufferedWriter bw = new BufferedWriter(new FileWriter(outPutFile,true));
				bw.write(" - "+output);
				bw.newLine();
				bw.flush();
				bw.close();
			}
			else{
				System.out.println("failed to create output file");
			}
	    } 
		catch (IOException e) {
			System.out.println("Error in printOutput-"+ e.getMessage());
	    }
	}
	
	/**
	 * @return current state
	 */
	public String toString(){
		return current_state;
	}
	
}
