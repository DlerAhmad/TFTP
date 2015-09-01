#!/usr/bin/perl -w
#Yogesh Jagadeesan, Dler Ahmad		Nov 23, 2014
#A simple file transfer program developed based on TFTP (RFC 1350) 

use strict;

use Encode;
use Socket;
use File::Basename;

#use constant REMOTE_PORT=>69;
use constant MAX_RECV_LEN=>516;
use constant REMOTE_HOST=>'glados.cs.rit.edu';

our $mode="octet";
our $service;
#prints a usage message
sub usage {
	print	"\nconnect\t\tconnect to remote tftp\nmode\t\tdisplay file transfer mode\nget\t\trecieve file\nbinary\t\tset mode to octet\nascii\t\tset mode to netascii\nquit\t\texit\n";
}

#makes UDP socket connection
sub makeConnection{
	$service = getprotobyname('udp');
	socket(UDP_SOCK, PF_INET, SOCK_DGRAM, $service)		|| die "socket:  $!";
	print "connected to ", REMOTE_HOST,"\n";
        }

#puts file to server
sub put{
	
	if (!$service){
		print "please connect first.\n";
		return;
	}
	
	print "local file name:";
	my $localfile=<STDIN>;
	chomp $localfile;
	
	#validating the localfile
	if (!$localfile){
                print "no local file name provided\n";
                return;
        }
	
	if (! -e $localfile){
                print "file not exist\n";
                return;
        }
	
	print "remote file name:";
	my $filename=<STDIN>;
	chomp $filename;
	if (!$filename){
                print "no remote file name provided\n";
                return;
        }


	#making a write request packet
	my $op=02;
        my $WRQ=pack("n a* c a* c",$op,$filename,0,$mode,0);

	#sending the write request to tftp sever
        my $remote_host = gethostbyname(REMOTE_HOST);
        my $remote_port=69;
        my $destination= sockaddr_in($remote_port, $remote_host);
	send (UDP_SOCK,$WRQ,0,$destination) or die "cant send the write request $!\n";

	#initializing the variables to receive data
	my $pkt;
	my $from;
	my $opcodedata=03;
	my $blocknum=0;
	my $filecontents;

	#receiving response for write request
	$from=recv (UDP_SOCK, $pkt, MAX_RECV_LEN,0) or die "couldnt receive the file $!\n";
	my @rec=unpack("nna*",$pkt);
	my $opcode=$rec[0];
	$blocknum=$rec[1];
	
	#check for err pkt
        if ($opcode==05){
        	print "$pkt\n";
                return;
        }

	#Setting the port to the new port assigned by server
	my ( $the_port, $the_ip ) = sockaddr_in( $from );
	$destination= sockaddr_in($the_port, $remote_host);
	
	#a response with block number 0 means write request has been granted.
	if($opcode==4 && $blocknum == 0){
		
		#sending file in block of 512 bytes
		$blocknum=01;
		open(FILE, $localfile);
		while(read(FILE,$filecontents,512)){
			my $succpkt = pack("n n a*",$opcodedata,$blocknum,$filecontents);
			send (UDP_SOCK,$succpkt,0,$destination);
			$from=recv(UDP_SOCK, $pkt, MAX_RECV_LEN,0);
			@rec=unpack("nna*",$pkt);
			my $receivedopcode=$rec[0];
			my $receivedblock=$rec[1];
		
			#check for err pkt
                        if ($receivedopcode==05){
                                print "$pkt\n";
                                return
                        }
			
			if($receivedopcode!=04 || $receivedblock!=$blocknum){
				last;
			}
			++$blocknum;
		}
	print "$localfile has been successfully sent!\n";
	}
}


#gets a file from server	
sub get{
	if (!$service){
		print "please connect first.\n";
		return;
	}
	
	#receiving the file name
        print "remote file name: ";
        my $filename=<STDIN>;
        chomp $filename;
	if (!$filename){ 
		print "no file name provided\n";
		return;
	}
	#making a read request packet
	my $op=01;
        my $RRQ=pack("n a* c a* c",$op,$filename,0,$mode,0);

	#sending the read request to tftp sever
        my $remote_host = gethostbyname(REMOTE_HOST);
        my $remote_port=69;
        my $destination= sockaddr_in($remote_port, $remote_host);
	send (UDP_SOCK,$RRQ,0,$destination) or die "cant send the read request $!\n";
	
	#initializing the variables to receive data
	my $pkt;
	my $from;
	my $opcode=0;
	my $blocknum=0;
	my($outfile, $dirs, $suffix) = fileparse($filename);

	#receiving data
	do{
		$from=recv (UDP_SOCK, $pkt, MAX_RECV_LEN,0) or die "couldnt receive the file $!\n";
		my @rec=unpack("nna*",$pkt);
		$opcode=$rec[0];

		#check for error pkt
		if ($opcode==05){
			print "$pkt\n";
		}

		#check for data pkt
		if ($opcode==03){
			$blocknum=$rec[1];
			my $data=$rec[2];
	
			#cheking if the file exist after receving the first pkt
			if ($blocknum==1 && -e $outfile){
               			unlink $outfile or die "file cant be deleted! $!\n";
        		}
	
			#opening file when first pkt is received
			if($blocknum==1){
				open OUT, ">>" ,"$outfile" or die "cant open the file to wire $! \n";
			}
			print OUT "$data";
	
			#Setting the port to the new port assigned by server
			my ( $the_port, $the_ip ) = sockaddr_in( $from );
			$destination= sockaddr_in($the_port, $remote_host);
			
			#sending ack pkt for the received pkt
			my $ackop=04;
			my $ack=pack("n n",$ackop,$blocknum);
			send (UDP_SOCK,$ack,0,$destination);
		}
	}while(bytes::length($pkt) == 516);
	
	if ($opcode==03){
		print "file $outfile has been successfully transferred.\n"; 
	}
	close(OUT);
}

#sets the mode of file transfer
sub setMode{
	my $m=shift @_;
	$mode=$m;
}

#main program
our $cmd;
do{

	print "enter a command:";
	$cmd=<STDIN>;
	chomp $cmd;
	if ($cmd eq "?"){
		usage();
	}elsif($cmd eq "put"){
		put();

	}elsif($cmd eq "get"){
		get();	

	}elsif($cmd eq "connect"){
		makeConnection();	
	
	}elsif($cmd eq "mode"){
		print "Using $mode mode to transfer files.\n";

	}elsif($cmd eq "binary"){
		setMode("octet");

	}elsif($cmd eq "ascii"){
		setMode("netascii");

	}elsif($cmd ne "quit"){
		print "?Invalid command\n";
	}
	
}while ( $cmd ne "quit");
close(UDP_SOCK);

