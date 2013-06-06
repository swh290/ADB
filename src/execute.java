import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Scanner;


public class execute {

	public static void main(String[] args) 
	{	
		Scanner input  = new Scanner(System.in);
	
		try
		{		
			System.out.println("Enter output filename:");
			String outputFile = input.next(); 
			TransactionManager TM = new TransactionManager(outputFile);
			
			 while(true)
			 {
				String line;
				
				System.out.println("Read from file(f) of command(c)");
				line = input.next();
				if(line.compareToIgnoreCase("f")==0)
				{
					System.out.println("Please input the full source path and filename:");
					line = input.next();
					inputFromFile(line,TM);
					
				}
				else if(line.compareToIgnoreCase("c")==0)
				{
					System.out.println("Enter command:");
					line = input.next();
					TM.Do(line);
				}
				else
				{
					System.out.println("Error: Please type 'f' or 'c'");
				}
			 }
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
		}
		
		
		
		System.out.println("done");
	}

	private static void inputFromFile(String inFile,TransactionManager TM)
	{
		try
		{
			FileInputStream fstream = new FileInputStream(inFile);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line;
			while((line = br.readLine())!=null)
			{
				TM.Do(line);
				System.out.println(line);
			}
			System.out.println("input from file done");
		}
		catch(Exception e)
		{
			System.out.println("Error-" + e.getMessage());
		}
	}
}
