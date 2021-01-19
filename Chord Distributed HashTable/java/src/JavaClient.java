/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

// Generated code
//import chord.*;

import org.apache.thrift.TException;
import org.apache.thrift.transport.TSSLTransportFactory;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TSSLTransportFactory.TSSLTransportParameters;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;

public class JavaClient {
  public static void main(String [] args) {

    if (args.length != 2) {
    
      System.out.println("Incorrect number of arguments");
  
      System.exit(0);
    }
    
     String hostName = args[0];
     int port = Integer.parseInt(args[1]);

    try {
      TTransport transport;
  
      transport = new TSocket(args[0], Integer.valueOf(args[1]));
      transport.open();
      
      TProtocol protocol = new  TBinaryProtocol(transport);
      FileStore.Client client = new FileStore.Client(protocol);

      perform(client);

      transport.close();
    } catch (TException x) {
      
      x.printStackTrace();
    } 
  }
  
  private static void testReadFail(FileStore.Client client) throws TException
  {
     //server does not own the file
      try{
        System.out.println("test case may fail as trying on given server");
        
        RFile r= client.readFile("sample.txt");
        if(r != null){
          System.out.println(r.meta.filename+" "+r.meta.version+" "+r.content);
        }
        
      }catch(Exception e){
      System.out.println("reading failed"); 
      e.printStackTrace();
      }
  }
  
    private static void testReadSucc(FileStore.Client client) throws TException
    {
      System.out.println("testing read");
      
      try{
        
       System.out.println("test case must pass");
       
       String key = "9496eec54d06963f3666d7719cd27073898a3ee588453b934627bb504cf19fbd"; //sample.txt
      // String key ="2b37659f12dce2432fc70f8760eee6ecb7e3764fb28ef2713196a5298d42de46";
       NodeID succ = client.findSucc(key);
      // System.out.println("got the succ" +succ.port);
       TTransport transport;
  
       transport = new TSocket(succ.ip, succ.port);
       transport.open();
      
       TProtocol protocol = new  TBinaryProtocol(transport);
       FileStore.Client clientR = new FileStore.Client(protocol);
       
       RFile r = clientR.readFile("sample.txt");
         if(r != null){
          System.out.println(r.meta.filename+" "+r.meta.version+" "+r.content);
        }
       transport.close(); 
      // clientW.writeFile(r);
      }catch(Exception e){
      System.out.println("reading failed"); 
      e.printStackTrace();
      }
      
      
    }
  
   private static void testWriteFail(FileStore.Client client) throws TException
   {
     try {
    
       System.out.println("writing on existing server port-- could fail");
       
       String fname = "sample.txt";
       
       RFile r = new RFile();
       RFileMetadata meta = new RFileMetadata();
       
       meta.filename = fname;
       meta.version = 0;
       
       r.content = "Hello";
       r.meta = meta;
       
       System.out.println("writefile called");
       client.writeFile(r);
       
    } catch (TException x) {
      
    } catch(Exception x){
      System.out.println("Writing failed ");
      x.printStackTrace();
    }
    
   }
  
  private static void testWriteSucc(FileStore.Client client) throws TException
  {
   System.out.println("testing write");
    
     try {
    
       System.out.println("writing on succ server -- should pass");
       
       String fname = "sample.txt";
       
       RFile r = new RFile();
       RFileMetadata meta = new RFileMetadata();
       
       meta.filename = fname;
       meta.version = 0;
       
       r.content = "Hello";
       r.meta = meta;
       
       String key = "9496eec54d06963f3666d7719cd27073898a3ee588453b934627bb504cf19fbd";
      // String key = "2b37659f12dce2432fc70f8760eee6ecb7e3764fb28ef2713196a5298d42de46";
       
       NodeID succ = client.findSucc(key);
       System.out.println("got the succ" +succ.port);
       TTransport transport;
  
       transport = new TSocket(succ.ip, succ.port);
       transport.open();
      
       TProtocol protocol = new  TBinaryProtocol(transport);
       FileStore.Client clientW = new FileStore.Client(protocol);
       
       clientW.writeFile(r);
      // clientW.writeFile(r);
       
    } catch (TException x) {
      System.out.println("Writing failed ");
    } catch(Exception x){
      x.printStackTrace();
    }
    
  
  }

  private static void perform(FileStore.Client client) throws TException
  {
   
    System.out.println("inside client testing");
     
    //Testing the write operation
    testWriteSucc(client);
    testReadSucc(client);
    
    testWriteFail(client);
    testReadFail(client);
   
   
  
  }//end of perform
  
  
  
}
