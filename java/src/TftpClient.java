import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Scanner;


/**
 * Class TftpClient provides the tftp commands that communicates with
 * tftp server on glados.cs.rit.edu server
 *
 * @author	Dler Ahmad
 * @version	Sep 24, 2014
 */
public class TftpClient {


	static private String mode="netascii";
	static Scanner scan=new Scanner(System.in);
	static private int port=69;
	//final static private String server="glados.cs.rit.edu";
	static InetAddress serverAddress;  
	static DatagramSocket socket;
	static DatagramSocket receiveSocket;

	/**
	 * main program
	 */
	public static void main(String args[])throws Exception{
		while(true){
			System.out.print("tftp>");
			parseCommand(scan.nextLine());
		}
	}

	/**
	 * Creates a read request packet.
	 * 
	 * @param	filename file that is requested to read.
	 * @param	mode mode of the file transfer.
	 *  
	 * @return	read request packet bytes.
	 * 
	 */
	private static byte[] makeRRQ(String filename, String mode){

		byte[] fNameByte=filename.getBytes();
		byte[] modeByte=mode.getBytes();
		byte[] RRQByte=new byte[4+filename.getBytes().length+mode.getBytes().length];
		RRQByte[0]=0;
		RRQByte[1]=1;
		for(int i=2;i<fNameByte.length+2;i++)
			RRQByte[i]=fNameByte[i-2];

		RRQByte[2+fNameByte.length]=0;

		for(int i=3+fNameByte.length;i<modeByte.length+3+fNameByte.length;i++)
			RRQByte[i]=modeByte[i-3-fNameByte.length];

		RRQByte[3+fNameByte.length+modeByte.length]=0;


		return RRQByte;

	}

	/**
	 * Creates a GET acknowledgment packet.
	 * 
	 * @param	packet 	the received packet from server.
	 *  
	 * @return	GET acknowledgment packet bytes.
	 * 
	 */
	private static byte[] makeGetAkc(DatagramPacket packet){
		byte[] pktByte=packet.getData();
		byte[] ack=new byte[4];
		ack[0]=0;
		ack[1]=4;
		ack[2]=pktByte[packet.getOffset()+2];
		ack[3]=pktByte[packet.getOffset()+3];
		return ack;
	}

	/**
	 * Sets the class Socket port value.
	 * 
	 * @param	p 	the port number to number to be set to.
	 * 
	 */
	private static void setPort(int p){
		if (p>0){
			port=p;
		}else{
			port=69; // default tftp port
		}
	}

	/**
	 * Appends the arrived packet to a file.
	 * 
	 * @param	filename	the file that packet is to be appended to.
	 * @param	packet		the arrived packet
	 * 
	 */
	private static void writeToFile(String filename, DatagramPacket packet)throws Exception{

		Path path;
		File file;
		OutputStream outToFile;

		path=Paths.get(filename);
		file=new File(path.getFileName().toString());

		// if this is the block 01, then check for file existence and delete it if exists.
		if(packet.getData()[packet.getOffset()+2]==0 && packet.getData()[packet.getOffset()+3]==1){
			if (file.exists()){
				file.delete();
			}
		}

		byte[] packetData=Arrays.copyOfRange(packet.getData(), packet.getOffset()+4,packet.getOffset()+packet.getLength());

		outToFile=new FileOutputStream(file,true);	//appending to the file
		outToFile.write(packetData);
		outToFile.close();

	}

	/**
	 * Gets a file from the server.
	 * 
	 * @param	filename	the file that is to be received.
	 * 
	 */
	private static void get(String filename)throws Exception{

		System.out.println("inside get");
		//making RRQ pkt
		DatagramPacket RRQpkt=new DatagramPacket(makeRRQ(filename,mode),makeRRQ(filename,mode).length,serverAddress,port);

		//send a RRQ
		socket.send(RRQpkt);

		//making the packet
		byte[] buf = new byte[516];
		DatagramPacket packet=new DatagramPacket(buf,buf.length);

		//starting to get packets one by one
		do{
			socket.receive(packet);


			//check for any possible error
			byte opCode=packet.getData()[1];
			if (opCode==5){
				byte[] packetData=Arrays.copyOfRange(packet.getData(), packet.getOffset()+4,packet.getOffset()+packet.getLength()-1);
				System.err.println(new String(packetData));
				return;
			}

			//appending the packet to the file
			writeToFile(filename, packet);
			
			System.out.println("port= " +packet.getPort());
			//set the port to the received packet port. It seems to be changed time to time!
			setPort(packet.getPort()); 

			// sending an Ack packet for the received packet
			DatagramPacket ack=new DatagramPacket(makeGetAkc(packet),makeGetAkc(packet).length,serverAddress,port);
			socket.send(ack);

		}while(packet.getLength() == 516);

		// end of file transfer
		System.out.println("file transferred.");

		// set the port to default tftp port number
		setPort(69); 

	}

	/**
	 * Establish the connection between client and given server. 
	 * 
	 * @param	server	server to connect to.
	 */
	private static void stablishTheConnection(String server)throws UnknownHostException{
		try{
			serverAddress=InetAddress.getByName(server);
			socket=new DatagramSocket();
		}catch(UnknownHostException ex){
			System.out.println("tftp: nodename nor servname provided, or not known");
			throw new UnknownHostException();
		}catch(SocketException ex){
			System.err.println("tftp error: socket error");
		}

	}

	/**
	 * Prints out the tftp usage message.
	 */
	private static void usage(){
		System.out.println("Commands may be abbreviated.  Commands are:\n\nconnect\t\tconnect to remote tftp\n"
				+ "mode\t\tset file transfer mode\nput\t\tsend file\nget\t\treceive file\nquit\t\texit tftp\n?\t\t"
				+ "print help information");
	}

	/**
	 * Sets the mode of the transfer
	 * 
	 *  @param	m		mode to set to.
	 */
	private static void setMode(String m){
		mode=m;
	}

	/**
	 * Parse the command given by user and react accordingly
	 * 
	 *  @param	commandline		command line given by user
	 */
	private static void parseCommand(String commandline)throws Exception{

		//Splitting the line of command 
		String[] command=commandline.split("\\s+");

		//if command is "connect"
		if(command[0].equals("connect")){
			
			//check if connected to the server already
			if(socket==null){
				
				//check the # of arguments
				if (command.length==1){
					
					System.out.print("(to) ");
					
					String toLine=scan.nextLine();
					String[] to=toLine.split("\\s+");
					if(to.length==1){
						try{stablishTheConnection(to[0]);}catch(Exception ex){};
					}else{
						System.out.println("tftp: nodename nor servname provided, or not known");
					}	
				}else if (command.length==2){
					try{stablishTheConnection(command[1]);}catch(Exception ex){};
				}else{
					System.out.println("tftp: nodename nor servname provided, or not known");
				}
			}else{
				System.err.println("Already connected.");
			}

			//if command is "mode"
		}else if(command[0].equals("mode")){
			if (command.length==1){
				System.out.println("Using "+mode+" mode to transfer files.");
			}else if(command.length==2){
				if(command[1].equals("ascii") || command[1].equals("netascii")){
					setMode("netascii");
				}else if(command[1].equals("octet") || command[1].equals("binary") || command[1].equals("image")){
					setMode("octet");
				}else{
					System.out.println(command[1]+": unknown mode");
					System.out.println("usage: mode [ ascii | netascii | binary | image | octet ]");	
				}
			}else{
				System.out.println("usage: mode [ ascii | netascii | binary | image | octet ]");

			}

			//if command is "get"
		}else if(command[0].equals("get")){

			//check the # of argument
			if (command.length>1){
				
				//check if already connected to server
				if(socket!=null){
					for(int i=1; i<command.length;i++){
						get(command[i]);
					}
				}else{
					// if any of the args is not following the pattern of host:filename, method will return;
					//o.w each args will be processed. 
					for(int i=1; i<command.length;i++){
						if (!command[i].contains(":")){
							System.out.println("usage: get host:file host:file ... file, or\n       get file file ... file if connected");
							return;
						}
					}
					for(int i=1; i<command.length;i++){

						String host=command[i].substring(0, command[i].indexOf(":"));
						String filename= command[i].substring(command[i].indexOf(":")+1,command[i].length());
						try {stablishTheConnection(host);}catch(UnknownHostException ex){break;}

						get(filename);
						socket=null;
					}
				}
			}else{

				System.out.print("(files) ");
				String filesline=scan.nextLine();
				if (filesline.equals("")){
					System.out.println("usage: get host:file host:file ... file, or\n       get file file ... file if connected");
				}else{
					String[] files=filesline.split("\\s+");
					if(socket!=null){

						for(int i=0; i<files.length;i++){
							get(files[i]);
						}
					}else{

						// if any of the args is not following the pattern of host:filename, method will return;
						//o.w each args will be processed. 
						for(int i=0; i<files.length;i++){
							if (!files[i].contains(":")){
								System.out.println("usage: get host:file host:file ... file, or\n\tget file file ... file if connected");
								return;
							}
						}
						for(int i=0; i<files.length;i++){

							String host=files[i].substring(0, files[i].indexOf(":"));
							String filename= files[i].substring(files[i].indexOf(":")+1,files[i].length());
							try{stablishTheConnection(host);}catch(UnknownHostException ex){break;}
							get(filename);
							socket=null;
						}
					}

				}
			}

			//if command is "?"
		}else if(command[0].equals("?")){
			usage();

			//if command is "quit"
		}else if(command[0].equals("quit")){
			System.exit(0);

			//if command is "put"
		}else if(command[0].equals("put")){
			System.out.println("command not available");

			//if "Enter" is pressed
		}else if(command[0].equals("")){
			//if Enter pressed nothing to do. line will be skipped

			//if anything else than above provided
		}else{
			System.out.println("?Invalid command");
		}
	}
}
