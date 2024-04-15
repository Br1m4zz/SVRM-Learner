package nl.cypherpunk.SVCSLearner.SVCS;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.HashMap;





public class SVCSHarness {
    Socket socket;
    boolean processdebug = false;
    boolean socketdebug = false;
    boolean socketdebug2 =false;
    boolean receivedebug = false; 
    OutputStream Socket_output = null;
    OutputStreamWriter outwriter = null ;
    BufferedWriter NetBw = null;
    InputStream Socket_input = null;
    InputStreamReader inreader = null; 
    BufferedReader NetBr = null;

    String host = "127.0.0.1";
    int port = 12345;

    // Timeout in ms
    int RECEIVE_MSG_TIMEOUT = 200;
    
    // Restart server after every session
    boolean REQUIRE_RESTART = true;

    // Send output from target to console
    boolean CONSOLE_OUTPUT = false;
    boolean RECEIVE_FIRST = false;
    Boolean NEEDCLEANUP = false;


    String cmd;
    String cleanupcmd;

    public Process targetProcess;
    boolean server_ready = false;
    ShareMemory shm = null;
    HashMap<String,InputSymbol>SVCS_Symbols = new  HashMap<String,InputSymbol>();
    int tryconnection = 0;
    int trykill = 0;


    public SVCSHarness() throws Exception {
    }

    public void setHost(String hostip) {
        this.host = hostip;
        System.out.println("[SVCS]HostIP: "+ hostip);
    }

    public void setPort(int port){
        this.port = port;
        System.out.println("[SVCS]Port: "+ port);
    }

    public void setCommand(String cmd){
        this.cmd = cmd;
        System.out.println("[SVCS]cmd: "+ cmd);
    }

    public void setNeedRestartTarget(Boolean restart){
        this.REQUIRE_RESTART = restart;
        System.out.println("[SVCS]restart: "+ restart);
    }

    public void setReceiveMessagesTimeout(int timeout){
        RECEIVE_MSG_TIMEOUT = timeout;
        System.out.println("[SVCS]timeout: "+ timeout);
    }

    public void setConsoleOutput(boolean enable) {
        CONSOLE_OUTPUT = enable;
        System.out.println("[SVCS]CONSOLE_OUTPUT: "+ CONSOLE_OUTPUT);
    }

    public void setRequireRestart(boolean enable) {
        REQUIRE_RESTART = enable;
        System.out.println("[SVCS]REQUIRE_RESTART: "+ REQUIRE_RESTART);
	}

    public void setShareMemory(String path, String fname){
        shm = new ShareMemory(path, fname);
        System.out.println("[SVCS]sharememory: "+ path + fname);
    }
    
    public void setreceivefirst(boolean enable){
        RECEIVE_FIRST = enable;
        System.out.println("[SVCS]RECEIVE_FIRST: "+ RECEIVE_FIRST);
    }

    public void setcleanupcmd(String cleanup){
        if(cleanup == null)
            NEEDCLEANUP=false;
        else
        {
            this.cleanupcmd = cleanup;
            NEEDCLEANUP = true;
        }
        System.out.println("[SVCS]NEEDCLEANUP: "+ NEEDCLEANUP);
        
    }

    public void setInputfiles(String path){
        int inputs_num = fetch_input(path, SVCS_Symbols);
        if(inputs_num == -1){
            System.out.println("[SVCS]Invalid Input dir");
            System.exit(-1);
        }
    }

    public void enable_sdebug(Boolean enable){
        this.socketdebug2 = enable;
        this.receivedebug = enable;
    }

    public void enable_pdebug(Boolean enable){
        this.processdebug = enable;
    }

    static class rawfilter implements FileFilter
    {
        @Override
        public boolean accept(File pathname){
            if (pathname.getName().toLowerCase().endsWith(".raw"))
            {
                return true;
            }
            else return false;
        }
    } 

    static int fetch_input(String path, HashMap<String,InputSymbol> Symbols)
    {
        File dirfile = new File(path);
        if (dirfile.isDirectory() == false) return -1;
        File[] files = dirfile.listFiles(new rawfilter());
        int syms_number = 0;
        for(File file : files){
            try{
                String fname = file.getName().toUpperCase();
                String symname = fname.substring(0, fname.length()-4);
                byte[] data = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
                InputSymbol tmp = new InputSymbol(symname, data);
                Symbols.put(symname,tmp);
                syms_number++;
            }catch(IOException e){
                e.printStackTrace();
                System.exit(-1);
            }
        }
        return syms_number;
    }

    public void start()throws Exception
    {
         if(processdebug)System.out.println("\033[32;4m"+"[processdebug]STARITING SUT \n"+ "\033[0m");
        if(cmd != null && !cmd.equals("")){
            ProcessBuilder pb = new ProcessBuilder(cmd.split(" "));

            if(CONSOLE_OUTPUT)pb.inheritIO();
            else{
                pb.redirectErrorStream(true);
                pb.redirectOutput(new File("server_output.log"));
            }
            targetProcess = pb.start();
            Thread.sleep(5);                       
        }else{
            System.out.println("cmd is not set!");
            System.exit(0);
        }
        tryconnection = 0;
        connectSocket();
        
        if(processdebug)System.out.println("\033[32;4m"+"[processdebug]: SUT is connectable \n"+ "\033[0m");
        
        reset();
    }

	public void reset() throws Exception 
    {
        if(processdebug)System.out.println("\033[32;4m"+"[processdebug]RESETTING SUT: "+targetProcess.pid() +"\033[0m");
        server_ready = false;
        int trystart = 0;
        tryconnection = 0;
        trykill = 0;
		socket.close();
        if(REQUIRE_RESTART && cmd != null && !cmd.equals("")) 
        {
            try{
                do{
                    targetProcess.destroyForcibly();
                    Thread.sleep(5);
                }while(targetProcess.isAlive()==true);
            }catch(IllegalThreadStateException e)
            {
                System.out.println("Try killing ingraceful...\n");
                kill_ingraceful();
                Thread.sleep(5);
            }
            
            if(NEEDCLEANUP){
                if(processdebug)System.out.println("\033[32;4m"+"[processdebug]cleaning SUT\n"+ "\033[0m");
                do_cleanup();
                if(processdebug)System.out.println("\033[32;4m"+"[processdebug]cleaning SUT finish\n"+ "\033[0m");
            }

            ProcessBuilder pb = new ProcessBuilder(cmd.split(" "));
            if(CONSOLE_OUTPUT) {
                pb.inheritIO();
            }
            else {
                pb.redirectErrorStream(true);
                pb.redirectOutput(new File("server_output.log"));
            }
            while(trystart<11){
                try{
                    targetProcess = pb.start();
                    trystart++;
                    if(targetProcess.isAlive() == false)
                    {
                        targetProcess = pb.start();
                        Thread.sleep(10);
                        trystart++;
                        
                    }
                    else
                        break;
                    if(trystart == 10){
                        System.out.println("[processdebug]building process 10 times failed, Abort!");
                        System.exit(-1);
                    }
                }   
                catch(Exception e){
                    trystart++;
                }   
            }

            if(processdebug)System.out.println("\033[33;4m"+"[SVCS]GENERATIN SUT FINISH: "+targetProcess.pid()+ "\033[0m");
        }
        Thread.sleep(150);
       

        if(socketdebug)System.out.println("\033[33;4m"+"[socketdebug]Connecting SUT\n"+ "\033[0m");
        connectSocket();
        
        if(RECEIVE_FIRST){
            if(socketdebug)System.out.println("\033[33;4m"+"[socketdebug]:try receive hello\n"+ "\033[0m");
            int recv_isblank = 0;
            try{
                while(true){
                    int recvs = receiveMessage();
                    if(recvs==0){
                        recv_isblank++;
                    }
                    else
                    {
                        break;
                    }
                        

                    if(recv_isblank > 10)
                    {
                        if(Socket_input.available() == 0 && socketdebug2){
                            System.out.println("[socketdebug]:we receive no hello msg!");
                        }
                    }
                }
            }catch(Exception e){
                e.printStackTrace();
                System.exit(-1); 
            }

        }
        
        
    }
    
    public void kill_ingraceful() throws Exception
    {
        if(targetProcess != null){
            try{
                long pid = targetProcess.pid();
                 Runtime.getRuntime().exec("killall -9 "+pid);
            }catch(Exception e){
                System.out.println("[SVCS]: kill_ingraceful fail!:");
                e.printStackTrace();
            }
        }
    }
    
    public void do_cleanup() throws Exception 
    {
        try{
            Runtime.getRuntime().exec(cleanupcmd).waitFor();
        }catch(Exception e){
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public void connectSocket() throws UnknownHostException, IOException, InterruptedException
    {
        while(true)
        {
            try
            {
                Thread.sleep(0,25*1000);
                if(socketdebug2)System.out.println("[socketdebug] try " + tryconnection + "time...");
                socket = new Socket(host, port);
                break;
            }
            catch(IOException e){
            tryconnection++;
            }
            if(tryconnection>1800)
            {
                System.out.println("[socketdebug] Connection failed, Abort!");
                System.exit(-1);
            }

        }


		socket.setTcpNoDelay(true);
		socket.setSoTimeout(RECEIVE_MSG_TIMEOUT);
		Socket_output = socket.getOutputStream();
		Socket_input = socket.getInputStream();
    }

    public void closeSocket() throws IOException 
    {
        try{
            socket.close();
        }catch(Exception e){
            System.out.println("[SV-CS]:socket close failed:");
            e.printStackTrace();
            System.exit(-1);
        }
        
	}
    
    public Boolean sendMessage(byte[] msg) throws Exception
    {
        if(receivedebug)System.out.println("\033[33;4m"+"[receivedebug]SENDING:\n"+new String(msg)+ "\n\033[0m");
        try{
            Socket_output.write(msg);
            Socket_output.flush();
            // NetBw.flush();
        }catch(Exception e){
            if(socketdebug)e.printStackTrace();
            System.out.println("sending fail:"+msg.toString());
            return false;
        }
        return true;
    }

    public int receiveMessage() throws Exception
    {
        int receivetimeout = 0;
        byte[] buffer = new byte[16384];
        int receive_bytes = 0;
        
        try{         
            while(Socket_input.available()==0)
            {
                Thread.sleep(1);
                receivetimeout++;
                if(receivetimeout>50)return 0;
            }
            while(Socket_input.available()>0)
            {
                Thread.sleep(5);
                receive_bytes += Socket_input.read(buffer,0,Socket_input.available());
            }

        }catch(IOException e)
        {
            e.printStackTrace();
            System.exit(-1);        
        }
        if(receivedebug)System.out.println("[receivedebug]RECEIVING:\n"+new String(buffer)+ "\n\033[0m");            
        return receive_bytes;    
    }

    public String processSymbol(String in_symbol) throws Exception
    {        

        int try_send =0;
        int recv_isblank =0;
        byte[] bytessendtoSUT = input_instantiation(in_symbol);
        String out_symbol = null; 
        if(socket.isClosed() || !socket.isConnected() || !targetProcess.isAlive())
        {
            if(socketdebug2 || processdebug){
                if(socket.isClosed() || !socket.isConnected())
                    System.out.println("[SVCS]:socket cloesed! ");
                if(!targetProcess.isAlive())
                    System.out.println("[processdebug]:target down! ");
            }
            return "ConnectionClosed";
        }else{
            // try{
            //     socket.sendUrgentData(0);
            // }
            // catch (IOException e){
            //     return "ConnectionClosed";
            // }
        }

        while(true){
            if(sendMessage(bytessendtoSUT)==true)
                break;                        
            else{
                try_send++;
                Thread.sleep(50);
                if(socketdebug2) System.out.println("[socketdebug]:try send:"+in_symbol+"  again! ");
            }
                
            if(try_send>5){
                if(socketdebug2 || processdebug){
                     System.out.println("[socketdebug]: failed! ");
                }
                return "ConnectionClosed";
            }
        }
        // if(socket.isClosed() || !socket.isConnected() || !targetProcess.isAlive())
        // {
        //     if(socketdebug2 || processdebug){
        //         if(socket.isClosed() || !socket.isConnected())
        //             System.out.println("[socketdebug]:socket cloesed! ");
        //         if(!targetProcess.isAlive())
        //             System.out.println("[SVCS]:target down! ");
        //     }
        //     return "ConnectionClosed";
        // }
        try{
            while(true)
            {
                int recvs = receiveMessage();
                if(recvs==0)
                {
                    recv_isblank++;
                }
                else{
                    break;
                }
                if(recv_isblank > 10)
                {
                    if(Socket_input.available() == 0 && socketdebug2){
                        System.out.println("[SVCS]:send:"+in_symbol+"but we receive nothing! sengding next! ");
                    }
                    break;
                }

            }
        }catch(Exception e){
            e.printStackTrace();
            System.exit(-1);
        }

        out_symbol = getOutSymbol();
        if(out_symbol == null){
            System.out.println("[SVCS]: InputSymbol missing! check Shm");
            System.exit(0);
        }
        if(processdebug)System.out.println("[processdebug]: current_hash:"+out_symbol);
        return out_symbol;
    }

    byte[] input_instantiation(String in_symbol) throws Exception
    {
        byte[] instance = null;
        InputSymbol in_symbol_obj = SVCS_Symbols.get(in_symbol);
        if(in_symbol_obj != null){
            instance = in_symbol_obj.data;
        }else{
            System.out.print("[SVCS]: Symbol not found:"+ in_symbol+"\n");
            System.exit(-1);
        }
        return instance;
    }

    public String getOutSymbol() throws Exception 
    {
        int stable = 0;    
        String out_symbol = "";    
        String numberchange = "";  
        String lastnumberstr = "";  
        int mayfail = 0;
        int readlen = 0;
        byte[] b = new byte[100]; 

        while(stable<80)
        {
            Thread.sleep(0,25);
            
            readlen = shm.read(0, 32, b);
            if(readlen > 5)
            {
                ShareStruct shareStruct = new ShareStruct(b);
                shareStruct.ParseStruct();
                numberchange = shareStruct.counterstr;
                if(numberchange.equals(lastnumberstr))
                {
                    stable ++;
                    out_symbol = shareStruct.hashstr;
                }else{
                    lastnumberstr = numberchange;
                    stable = 0;
                    mayfail++;
                    
                }
            }
            if(mayfail>1000)
            {
                System.out.println("[SVCS] memhash unstable! check your instrument first!");
                System.exit(-1);
            }
        }
        return out_symbol;
    }


}

class ShareStruct
{
    boolean svdebug = false;
    byte[] recv = new byte[255];
    byte[] hashstate = new byte[8];
    byte[] counter = new byte[8];
    String hashstr = null;
    String counterstr;

    public ShareStruct(byte[] recv)
    {
        this.recv = recv;
    }

    public void ParseStruct()
    {
        InputStream in_withcode;
        try {
            in_withcode = new ByteArrayInputStream(recv);
            DataInputStream inputStream = new DataInputStream(in_withcode);
            inputStream.read(hashstate, 0, 8);
            if(svdebug)System.out.print("\033[31;4m"+"[SVCS-Learner]current hashstate:" + toHexString(hashstate)+"\n"+"\033[0m");
            inputStream.read(counter, 0, 8);
            hashstr = toHexString(hashstate);
            counterstr = toHexString(counter);
        } catch (IOException e){
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private static String toHexString(byte[] b) {
        String res = "";
        for (int i = 0; i < b.length; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            res += hex;
        }
        return res;
    }
}


class ShareMemory
{
    int flen = 8+4;                   
    int fsize = 0;                   
    String shareFileName;          
    String sharePath;                
    MappedByteBuffer mapBuf = null;   
    FileChannel file_channel = null;           
    FileLock fl = null;              
    RandomAccessFile RAFile = null;  

    public ShareMemory(String shm_path, String shm_name)
    {
        this.shareFileName = shm_name;
        this.sharePath = shm_path;

        try
        {
            RAFile = new RandomAccessFile(this.sharePath + this.shareFileName, "rw");
            file_channel = RAFile.getChannel();
            fsize = (int)file_channel.size();
            if(fsize < flen)
            {
                byte bb[] = new byte[flen - fsize];
                ByteBuffer bf = ByteBuffer.wrap(bb);
                bf.clear();
                file_channel.position(fsize);
                file_channel.write(bf);
                file_channel.force(false);

                fsize = flen;
            }

            mapBuf = file_channel.map(FileChannel.MapMode.READ_WRITE, 0, fsize);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param ps    
     * @param len  
     * @param buff  
     * @return
     */
    public synchronized int write(int ps, int len, byte[] buff)
    {
        if(ps >= fsize || ps + len >= fsize)
            return 0;

        FileLock fl = null;
        try
        {

            fl = file_channel.lock(ps, len, false);
            if(fl != null)
            {
                mapBuf.position(ps);
                ByteBuffer bf1 = ByteBuffer.wrap(buff);
                mapBuf.put(bf1);

                fl.release();
            }
        }
        catch (Exception e)
        {
            if(fl != null)
            {
                try
                {
                    fl.release();
                }
                catch (IOException el)
                {
                    System.out.println(el.toString());
                }
            }
            return 0;
        }

        return 0;
    }

    /**
     *
     * @param ps    
     * @param len   
     * @param buff  
     * @return
     */
    public synchronized int read(int ps, int len, byte[] buff)
    {
        if(ps >= fsize)
            return 0;

        FileLock fl = null;
        try
        {
            fl = file_channel.lock(ps, len, false);
            if(fl != null)
            {
                mapBuf.position(ps);
                if(mapBuf.remaining() < len)
                    len = mapBuf.remaining();

                if(len > 0)
                    mapBuf.get(buff, 0, len);

                fl.release();

                return len;
            }
        }
        catch (Exception e)
        {
            if(fl != null)
            {
                try
                {
                    fl.release();
                }
                catch (IOException el)
                {
                    System.out.println(el.toString());
                }
            }
            return 0;
        }
        return 0;
    }

    /**
     * 
     * @throws Throwable
     */
    protected void finalize() throws Throwable
    {
        if(file_channel != null)
        {
            try {
                file_channel.close();
            } catch (IOException e) {
                System.out.println(e.toString());
            }
            file_channel = null;
        }

        if(RAFile != null)
        {
            try {
                RAFile.close();
            } catch (IOException e) {
                System.out.println(e.toString());
            }
            RAFile = null;
        }
        mapBuf = null;
    }

    /**
     * 
     */
    public synchronized void closeSMFile()
    {
        if(file_channel != null)
        {
            try {
                file_channel.close();
            } catch (IOException e) {
                System.out.println(e.toString());
            }
            file_channel = null;
        }

        if(RAFile != null)
        {
            try {
                RAFile.close();
            } catch (IOException e) {
                System.out.println(e.toString());
            }
            RAFile = null;
        }
        mapBuf = null;
    }

    /**
     * 
     * @return 
     */
    public synchronized boolean checkToExit()
    {
        byte bb[] = new byte[1];
        if(read(1,1, bb) > 0)
        {
            if(bb[0] == 1)
                return true;
        }
        return false;
    }

    /**
     * 
     */
    public synchronized void resetExit()
    {
        byte bb[] = new byte[1];
        bb[0] = 0;
        write(1, 1, bb);
    }

    /**
     * 
     */
    public synchronized void toExit()
    {
        byte bb[] = new byte[1];
        bb[0] = 1;
        write(1, 1, bb);
    }

    // public static void main(String args[]) throws Exception
    // {
    //     ShareMemory shm = new ShareMemory("/dev/shm/", "SVshm");
    //     byte[] b = new byte[100];
    //     int readlen = shm.read(0, 28, b);
    //     System.out.println("readbuf len:" + readlen);
    //     ShareStruct shareStruct = new ShareStruct(b);
    //     //shareStruct.ParseStruct();
    // }
}

class InputSymbol
{
    byte[] data;
    String symName;
    public InputSymbol(String synmbolname, byte[] msg){
        symName = synmbolname;
        data = msg;
    }
}
