What does the program do?
	
	This program will act as an tftp client and connects to the tftp server
	running on glados.cs.rit.edu. It provide following commands:
		
		
		connect		connect to remote tftp
		mode		display file transfer mode
		put		send file
		get		recieve file
		binary		set mode to octet
		ascii		set mode to netascii
		quit		exit
	

How to run the program? 
	
	1. Run the program using command "./tftp.pl" 

	2. To connect to the tftp server running on glados.cs.rit.edu, 
	use command "connect"

	3. To send a file, use command "put". Then give the local file name and
	remote file name. You may include path for local file name. You must not
	include any path for the remote file name, since it will be automatically
	placed in the path /local/sandbox on glados.cs.rit.edu.

	4. To receive a file, use command "get". Then give the remote file name.
	You must not include any path for the remote file name, since the default
	path is /local/sandbox on glados.cs.rit.edu.
		
	5. To set the file transfer mode to octet, use command "binary". This is
	the proper mode to transfer binary files.
	
	6. To set the fila transfer mode to netascii, use command "ascii". This is
	the proper mode to transfer text files.
	
	7. To display the current file transfer mode, use command "mode". 
	
	8. To display list of the commands, use command "?".
	
	9. To exit the program, use command "quit".
	

Dler Ahamd


