import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.io.File;  
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;
import java.util.HashSet;
//import chord.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TSSLTransportFactory;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TSSLTransportFactory.TSSLTransportParameters;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;


public class FileStoreHandler implements FileStore.Iface {

    public String ip;
    public int port;
    public NodeID currNode;
    public List<NodeID> fingerTable;
    
    
    class FileInfo{
       public String version;
       public String content;
       
       public FileInfo(String v, String con){
         version = v;
         content = con;
       }   
    }
    
    public Map<String, FileInfo> hm;

    public FileStoreHandler(String ip, int port)throws Exception{
         //   System.out.println("inside the handler constructor");
            this.ip = ip;
            this.port = port;
            String nodeId = getHash(ip+":"+port);
            
            currNode = new NodeID(nodeId, ip, port);
            
            hm = new HashMap<>();
            
          //  System.out.println("outside the handler constructor");
    }
    
    public String getHash(String key){
    
      try{
    
          MessageDigest digest = MessageDigest.getInstance("SHA-256");
          //get hash in bytes
          byte[] encodedhash = digest.digest(key.getBytes(StandardCharsets.UTF_8));
         
          return bytesToHex(encodedhash);
         
      }catch(NoSuchAlgorithmException ex){
          ex.printStackTrace();
          System.out.println("An error occured while calculating Hash");
      }catch(Exception ex){
          ex.printStackTrace();
          System.out.println("An exception occured while calculating hash");
      }
      
      return "";
         
   }
     
    public String bytesToHex(byte[] hash) throws NoSuchAlgorithmException, Exception{
         
	    StringBuilder hexString = new StringBuilder(2 * hash.length);
	    
	    for (int i = 0; i < hash.length; i++) {
	        String hex = Integer.toHexString(0xff & hash[i]);
	        if(hex.length() == 1) {
	            hexString.append('0');
	        }
	        hexString.append(hex);
	    }
	    return hexString.toString();
         
   }

    public void writeFile(RFile rFile) throws SystemException{
    
    try{
    
      if(rFile == null) return;
      
      String filename = rFile.meta.filename;
      String key = getHash(filename);
      
      NodeID node = findSucc(key);
     
      
      if(!node.equals(currNode)){
         SystemException s = new SystemException();
         s.message = "Server does not own the file's ID";
         throw s;    
      }
      
      //check if file exists with server
      File file = new File(filename);
      
      if(hm.containsKey(filename)){
        //file is present with server
        FileInfo fileInfo = hm.get(filename);
        fileInfo.content = rFile.content;
        fileInfo.version = Integer.toString(Integer.parseInt(fileInfo.version)+1);
        System.out.println(fileInfo.version);
        hm.put(filename, fileInfo);
      
      }else{
        //file is not present
        
        String version = Integer.toString(rFile.meta.version);
        String content = rFile.content;
        FileInfo fileI = new FileInfo(version, content);
        
        hm.put(filename, fileI);
      
      }
      
      //write to the file 
      writeToFile(file, rFile.content);
      
    }catch(TException ex){ex.printStackTrace();}

    }
    
    public void writeToFile(File file, String content){
        FileWriter fw  = null;
        BufferedWriter bw = null;
        
        try{
        
          if(!file.exists()){
            file.createNewFile();
          }
          
          fw = new FileWriter(file.getAbsoluteFile());
          bw = new BufferedWriter(fw);
          
          bw.write(content);
         
          System.out.println("Done writing to file");
        
        }catch(IOException ex){
          ex.printStackTrace();
          System.out.println("An error occured while writing to file "+ ex.getMessage());
        }finally{
          try{
          
          if(bw!=null)bw.close();
          
          }catch(Exception ex){
            ex.printStackTrace();
            System.out.println(ex.getMessage());
          }
        }
    
    }

    public RFile readFile(String filename) throws SystemException, TException{
    
         String key = getHash(filename);
      
         NodeID node = findSucc(key);
      
         if(!node.equals(currNode)){
          SystemException s = new SystemException();
          s.message = "Server does not own the file's ID";
          throw s;    
         }
         
         RFile rFile = null;
         if(hm.containsKey(filename)){
           //server contains file
           
           rFile = new RFile();
           RFileMetadata meta = new RFileMetadata();
           
           FileInfo fileInfo = hm.get(filename);
           meta.version = Integer.parseInt(fileInfo.version);
           meta.filename = filename;
           
           rFile.meta = meta;
           rFile.content = fileInfo.content;
           
           return rFile;
           
         }else{
           SystemException s = new SystemException();
           s.message = "Server does not contain the file "+filename;
           throw s; 
         }     
        
    }

    public void setFingertable(List<NodeID> node_list){
        System.out.println("Setting the finger table ");
        this.fingerTable = node_list;     
        
    }

    public NodeID findSucc(String key) throws SystemException, TException {
    
       
        NodeID predNode = findPred(key);
        System.out.println(predNode.ip+" "+predNode.port);
        
        FileStore.Client client = null;
        try{        
            client = connectToNode(predNode);
            if(client != null){
              return client.getNodeSucc();
            }
          
         }catch(Exception e)
         { 
           System.out.println("An exception occured while finding Succ "+e.getMessage()); 
          // System.exit(0);
         }
        
        return currNode;
        
    }


    public NodeID findPred(String key) throws SystemException{
    
     NodeID node1 = this.currNode;
     try{
           
        if(fingerTable == null){
          SystemException s = new SystemException();
          s.message = "FingerTable is not initialzed, please run the init command";
          throw s;      
        }
        
        NodeID node2 = fingerTable.get(0);
        
        while(!checkIfInBetween(key, node1.id, node2.id) ){
          return closestPrecedingFinger(key, node1);
        }
        
      }catch(TException ex){
          ex.printStackTrace();
      }
        
        return node1;
    }
    
    
    
    public NodeID closestPrecedingFinger(String key, NodeID node1)throws SystemException, TException{
    
       
        for( int i = fingerTable.size()-1; i>0; i--){
            NodeID currSucc = fingerTable.get(i);
             
            if(checkIfInBetween(currSucc.id, node1.id, key) ){
                //recursive call on findPred for node currSucc
                //return rpc  
              
               FileStore.Client client =  connectToNode(currSucc);
               if(client!=null){
                return client.findPred(key);
               }
            }
        }
        
        return node1;
    }
    
    
    public boolean checkIfInBetween(String key, String id1, String id2){
    
    
        if( id1.compareTo(key)<0 && id2.compareTo(key)>0){
         return true;
        }
        
        else if( id1.compareTo(id2)>0 ){
       
          if(id1.compareTo(key) >0 && id2.compareTo(key) >0){ 
          return true;
          }
        }
         return false;
    }
    
    
    public FileStore.Client connectToNode(NodeID node){
    
      FileStore.Client client = null;
      try{
          TTransport transport = null;
          transport = new TSocket(node.ip, node.port);
          transport.open();
          TProtocol protocol = new  TBinaryProtocol(transport);
          client = new FileStore.Client(protocol);
          return client;
          
      }catch(Exception ex){
          System.out.println(" Error occured while making a remote call "+ex.getMessage());
          System.exit(0);
      }
      
      return client;
      
    }//end of method
    
    
    
    //gets the current node's succ node
    public NodeID getNodeSucc() throws SystemException{
     
        if(fingerTable == null){
          SystemException s = new SystemException();
          s.message = "FingerTable is not initialzed, please run the init command";
          throw s;      
        }
        
        return fingerTable.get(0);
    }


}
