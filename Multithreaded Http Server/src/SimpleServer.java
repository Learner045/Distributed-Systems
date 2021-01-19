import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleServer {


    private static final String SERVER_NAME = "MySimpleHttpServer 1.1\r\n";
    private ConcurrentHashMap<String, Integer> map;

    public class ClientHandler implements Runnable{
        Socket sock;
        BufferedReader reader;
        OutputStream outputStream;

        public ClientHandler(Socket socket){
            //setting up the network tools
            try {
                sock = socket;
                InputStreamReader inputStreamReader = new InputStreamReader(socket.getInputStream());
                reader = new BufferedReader(inputStreamReader);
                outputStream = socket.getOutputStream();

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        String processReq(String line)throws IOException{
            //  GET /cat2.png HTTP/1.1
            String[] subStrings= line.split(" ");
            String image = subStrings[1].substring(1); //image to send to user
            //System.out.println(image);

            File currDir = new File(".");
            List<File> list = new ArrayList<>();
            boolean res = false;

            getDirectories(list, currDir); //get all directories within the current directory
            File requestedResource = getRequestedResource(list, image);

            StringBuilder response = new StringBuilder("");
            //Date Server Last-Modified Content-Type Content-Length

            if(requestedResource !=null){
                //file found
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
                simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                String reqDate = simpleDateFormat.format(Calendar.getInstance().getTime());

                Date date = new Date(requestedResource.lastModified());
                String lastModified = simpleDateFormat.format(date);
                String Content_Type  = "Content-Type: "+ URLConnection.guessContentTypeFromName(requestedResource.getName())+"\r\n";
                String Content_Length = "Content-Length : "+requestedResource.length()+"\r\n";
                String Content_Date = "Date: "+reqDate+"\r\n";
                String Server = "Server: "+SimpleServer.SERVER_NAME+"\r\n";
                String Last_Modified = "Last-Modified: "+lastModified+"\r\n";

                response.append("HTTP/1.1 200 OK\r\n");
                response.append(Server);
                response.append(Content_Type);
                response.append(Content_Date);
                response.append(Last_Modified);
                response.append(Content_Length);
                //   response.append("<html><head><title>Hello</title></head><body></body></html>");
                response.append("\r\n");
                //    response.append("<b>Hola Shreya!</b>");
                //  response.append("\r\n\r\n");

                outputStream.write(response.toString().getBytes());
                Files.copy(Paths.get(requestedResource.toString()), outputStream);
                outputStream.write("\r\n\r\n".getBytes());
                outputStream.flush();
                outputStream.close();
                return image;

            }else{
                response.append("HTTP/1.1 404 Not Found");
                outputStream.write(response.toString().getBytes());
                outputStream.flush();
                outputStream.close();
                return null;
            }

        }

        void getDirectories(List<File> list, File currDir){
            if(currDir == null)return;
            if(currDir.isFile())return;
            if(currDir.listFiles() == null || currDir.listFiles().length <=0)return;
            for(File f: currDir.listFiles()){
                if(f.isDirectory()){
                    list.add(f);
                    getDirectories(list, f);
                }
            }
        }

        File getRequestedResource(List<File> list, String image){
            File resourceFile = null;
            if(list == null || list.size()<=0 )return resourceFile;

            for(int i=0; i<list.size(); i++){
                if(list.get(i).getName().equalsIgnoreCase("www")) {
                    if (list.get(i).listFiles() != null && list.get(i).listFiles().length>0) {
                        for (File f : list.get(i).listFiles()) {
                            if (f.isFile() && f.getName().equalsIgnoreCase(image)) {
                                //  System.out.println("file found");
                                resourceFile = f;
                                break;
                            }//condition
                        }//inner for
                    }//null check
                }//end of www
            }//end of for

            return resourceFile;
        }

        void printClientInfo(String image){
            //Client's IP, Client's port
            ///bar.html|128.226.118.20|4759|1
            String clientIP = sock.getRemoteSocketAddress().toString().split(":")[0].substring(1);
            String clientPort = sock.getPort()+"";
            System.out.println(image+"|"+clientIP+"|"+clientPort+"|"+resourceInfoCount(image));

        }

        int resourceInfoCount(String image){
            if(map.containsKey(image)){
                map.put(image, map.get(image)+1);
            }else{
                map.put(image, 1);
            }
            return map.get(image);
        }

        @Override
        public void run() {

            String message;
            String image = null;
            int itr = 1;
            try{
                message = reader.readLine();
                while(message!=null){

                    if(message.length()<=0)break;

                    System.out.println("Read "+message);
                    if(itr == 1){
                        image = processReq(message);
                        itr++;
                    }
                    message = reader.readLine();
                } //end of loop

                if(image!=null){
                    printClientInfo(image);
                }

                System.out.println("Out of loop");


            }catch (Exception e){
                e.printStackTrace();
            }
            finally {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    sock.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }



    private void solve(){
        ServerSocket serverSocket = null;
        map = new ConcurrentHashMap<>(); //String->image, Integr->count
        try {
            serverSocket = new ServerSocket(4244);

            while(true){

                Socket clientSocket = serverSocket.accept(); //accepting a connection request with a client

                Thread clientThread = new Thread(new ClientHandler(clientSocket));
                clientThread.start();

                System.out.println("-------Got a new connection");

                System.out.println("-Server Hostname:"+ InetAddress.getLocalHost().getHostName());
                System.out.println("-Server Port"+serverSocket.getLocalPort());

            }

        } catch (IOException e) {
            System.out.println("Port inavailable for socket ");
            e.printStackTrace();
        }catch (Exception e){
            e.printStackTrace();
        }finally{
            if(serverSocket!=null){
                try {
                    serverSocket.close();
                    System.out.println("Closing the server socket");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }



    public static void main(String args[]){
        new SimpleServer().solve();
    }
}
