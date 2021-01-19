# cs457-557-fall2020-pa2-Learner045

Programming language : Java
Tools : apache thrift



Comilation steps:


Please run the commands and create file inside java/ folder

bash

export PATH=$PATH:/home/cs557-inst/local/bin

cp /home/cs557-inst/pa2/chord.thrift ./

thrift -gen java chord.thrift

1)Update nodes.txt with required 'ip:port' 
2)run make
3)run all the servers mentioned on the nodes.txt
4) run  /home/cs557-inst/pa2/init nodes.txt or  /home/cs557-inst/pa2/init2 nodes.txt
on a new remote (different from remote used for servers)
5)run ./client ipaddr(of server) port on a new remote(diff from server)




Completion status:

1)FileStoreHandler contains implementation for -
writeFile() - It writes file to the server. If server is not the file's succ, then it throws a system exception.
readFile() - It reads the file specified, if present on server else throws a system exception.
If server does not own the file's ID, a system exception is thrown
findPred() - finds pred for a nodeID
findSucc() - find's succ by using findPred
getNodeSucc() - returns the node's succ




Testing:

Following has been tested 

1)test for writing file - success

filename = "sample.txt"
testWriteSucc(client) first finds the succ node for file and then writes to it
Hence, it always succeeds


Output
inside client testing
testing write
writing on succ server -- should pass

Inside window for port 9095
Done writing to file


2)test for reading file - success

filename = "sample.txt"
testReadSucc(client) first finds the succ node for file and then reads 
Hence, it always succeeds unless file is not written to server in earlier calls

Output

testing read
test case must pass
sample.txt 0 Hello


3)test for writing file - may fail

testWriteFail(client); writes to the node with port specified in command and
hence, it may or may not succeed depending on whether the port/node is file's succ

Output

writing on existing server port-- could fail
writefile called

Failure message in window of port tried 



4)test for read file- may fail
We read on port specified in cmd line which may not be file's succ

Output

reading failed
SystemException(message:Server does not own the file's ID)



What is not implemented - 

Case : file's hash key == node's key 

This is the case  where key doesn't fall between node1 and node2's range
instead node's hash == key's hash


Please note : file is not being read from the directory, it is just being read from local data structure (Map)
Hence,  reading will only work if previous calls have been made to write it to the data structure and 
next calls are read calls before closing the thread







