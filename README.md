# Telecommuncations
This project is sending and receiving signals in java using multithreading. 
Each frame has a max size of 1252 bytes. 1250 bytes is the data content being sent 
while the first 2 bytes define the header. The header consists of a an address which in the
project are either 0 or 1, and the frame number the sender is on and the receiver is on
for stop and wait but for selective Repeat it has code which tells the sender to either
send the next frame or resend a frame along with a frame number. The second byte of the header also
contains the P/F bit which tells the receiver if this is the final frame.
It has Stop and wait implemented and Selective Repeat wiht a max of 16 frames implemented.
tcdlib.jar is authored by Stefan Weber. 
