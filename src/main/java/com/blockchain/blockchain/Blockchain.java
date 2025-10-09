/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Project/Maven2/JavaApp/src/main/java/${packagePath}/${mainClassName}.java to edit this template
 */


/*
    Next steps
    Blockchain networks are all about connection, so once you’ve deployed nodes, 
    you’ll obviously want to connect them to other nodes! If you have a peer 
    organization and a peer, you’ll want to join your organization to a consortium 
    and join or Create a channel. If you have an ordering node, you will want to 
    add peer organizations to your consortium. You’ll also want to learn how to 
    develop chaincode, which you can learn about in the topics Smart Contracts 
    and Chaincode and Writing Your First Chaincode.

    Part of the process of connecting nodes and creating channels will involve 
    modifying policies to fit the use cases of business networks. 
    For more information about policies, check out Policies.

    One of the common tasks in a Fabric will be the editing of existing channels. 
    For a tutorial about that process, check out Updating a channel configuration. 
    One popular channel update is to add an org to an existing channel. For a 
    tutorial about that specific process, check out Adding an Org to a Channel. 
    For information about upgrading nodes after they have been deployed, check 
    out Upgrading your components.
*/
package com.blockchain.blockchain;

/**
 *
 * @author simo0
 */
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.yaml.snakeyaml.LoaderOptions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
public class Blockchain {
    
    static LinkedList<ServerCA> server_list= new LinkedList<ServerCA>();
    static String mainDirectory;
    static boolean intermediate=false;
    static String admin_name;
    static String admin_pwd;
    static String icaadmin_name;
    static String icaadmin_psw;
    static String org_name;
    static String org_psw;
    static String fabric_ca_server_name;
    static LinkedList<Integer> ports_used=new LinkedList<Integer>();
    static int inter_port;
    static int server_port;
    static String tls_ca_name;
    static String org_ca_name;
    static String int_ca_name;
    static String first_peer;
    
    public static void main(String[] args) throws IOException, InterruptedException {
        Scanner in= new Scanner(System.in);
        boolean loop=true;
        
        File projects= new File("src\\main\\java\\com\\blockchain\\blockchain\\projects.txt");
        try(BufferedReader reader= new BufferedReader(new FileReader(projects))){
            String line;
            LinkedList<String> prjs= new LinkedList<String>();
            while((line=reader.readLine())!=null){
                prjs.add(line);
            }
            
            do{
                System.out.println(" -------- MAIN MENU -------- ");
                System.out.println("1) Open Project");
                System.out.println("2) New Project");
                System.out.println("3) Delete Project");
                System.out.println("4) Exit");
                switch(in.nextInt()){
                    case 1:{
                        //TODO funzione open project
                        System.out.println(" -------- SELECT PROJECT --------");

                        for(int i=0;i<prjs.size();i++){
                            System.out.println((i+1)+") "+prjs.get(i));
                        }

                        int select= in.nextInt();
                        mainDirectory= prjs.get(select-1);
                        break;
                    }
                    case 2:{
                        System.out.print("Project name: ");
                        mainDirectory=in.next();
                        prjs.add(mainDirectory);
                        String yourPin="simone03";
                        createDirectory(mainDirectory);
                        setupCA(yourPin);

                        executeWSLCommand("cd "+mainDirectory+" &&"
                                + "mkdir peers_bin");
                        download_peer_bin();
                        executeWSLCommand("cd "+mainDirectory+" &&"
                                + "mkdir orderers_bin");
                        download_orderer_bin();
                        String channel_n=createGenesisBlock();
                        //metodo per creare il sistema di cartelle delle organizzazioni
                        createDirectoryForOrganizations(channel_n);
                        break;
                    }
                    case 3:{
                        System.out.println(" -------- SELECT PROJECT --------");

                        for(int i=0;i<prjs.size();i++){
                            System.out.println((i+1)+") "+prjs.get(i));
                        }

                        int select= in.nextInt();
                        mainDirectory= prjs.get(select-1);
                        
                        prjs.remove(mainDirectory);
                        executeWSLCommand("cd "+ mainDirectory+" && "
                                + "docker compose down");
                        executeWSLCommand("rm -rf "+mainDirectory);
                        
                        break;
                    }
                    case 4:{
                        System.out.println("Shutdown in progress...");
                        break;
                    }
                    default:{
                        System.err.println("Input error, try again");
                    }
                }
                FileWriter fw = new FileWriter(projects, false);
                String content="";
                for(int i=0;i<prjs.size();i++){
                    content=content+prjs.get(i)+"\n";
                }
                fw.write(content);
            }while(loop);
            
            
            
        }catch(IOException e){
            System.err.println(e.toString());
        }
        
        
        
        
        
    }
    
    
    
    private static void createDirectory(String name){
        executeWSLCommand("mkdir "+name);
    }
    
    /**
     * @throws InterruptedException 
     * 
     * 
     */
    private static void setupCA(String pin) throws IOException, InterruptedException{
        Scanner in = new Scanner(System.in);
        if(executeWSLCommandToString("cd "+ mainDirectory +" && which aria2").length()==0){
            System.out.println("Installing aria2...");
            String remoteCmd = "cd '" + mainDirectory + "' && apt update && apt install -y aria2";

            // Chiamiamo wsl.exe e passiamo sudo -S sh -c ... come argomenti: la shell interna gestirà il cd e apt.
            ProcessBuilder pb = new ProcessBuilder(
                "wsl.exe", "--", "sudo", "-S", "sh", "-c", remoteCmd
            );

            // opzionale: eredita env / o setta working dir locale
            pb.redirectErrorStream(true); // unisce stderr a stdout

            Process p = pb.start();

            // SCRIVO la password su stdin del processo (sudo leggerà da stdin)
            try (OutputStream os = p.getOutputStream()) {
                os.write((pin + "\n").getBytes(StandardCharsets.UTF_8));
                os.flush();
                // non chiudere immediatamente se il processo può chiedere altro input; qui va bene chiudere
            }

            // Leggo l'output (stdout + stderr)
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                }
            }
            int exit = p.waitFor();
            System.out.println("Exit code: " + exit);
        }else{
            System.out.println("aria2 already installed. ");
        }
        
        if(executeWSLCommandToString("cd "+ mainDirectory +" && ls").contains("fabric-ca-client") && executeWSLCommandToString("cd "+ mainDirectory +" && ls").contains(""+fabric_ca_server_name+"")){
            System.out.println("Binaries alrady installed");
        }else{
            System.out.println("Installing Fabric CA server and CA client binaries...");
            executeWSLCommand("cd "+ mainDirectory +" && "
                    + "aria2c https://github.com/hyperledger/fabric-ca/releases/download/v1.5.15/hyperledger-fabric-ca-linux-amd64-1.5.15.tar.gz && "
                    + "tar -xvzf hyperledger-fabric-ca-linux-amd64-1.5.15.tar.gz &&"
                    + "rm hyperledger-fabric-ca-linux-amd64-1.5.15.tar.gz &&"
                    + "mkdir fabric-ca-client &&"
                    + "cp bin/fabric-ca-client fabric-ca-client &&"
                    + "cd fabric-ca-client &&"
                    + "mkdir tls-ca int-ca tls-root-cert");
            //System.out.println("How many organizations do you want to distribute?");
            //int num_org=in.nextInt();
            setupCA_TLS(0);
            createDockerComposeYaml();
            new FabricServerThread(0, mainDirectory);
            
            waitForContainer(tls_ca_name);

            enrollRequestToCAServer(0);
            enrollCAAdmin(0);
            System.out.println("Do you want to deploy an Intermediate CA? s/n");
            if(in.next().equals("s")){
                intermediate=true;
                registerIntermediateCAAdmin(0);
                
            }
            String userName=registerNewIdentity(0);
            deployOrganizationCA(0, userName);
            new FabricOrganizationServerThread(0,mainDirectory);
            waitForContainer(org_ca_name);
            
            
            executeWSLCommand("cd "+Blockchain.mainDirectory+"/fabric-ca-client &&"
                + "./fabric-ca-client enroll -d -u https://"+org_name+":"+org_psw+"@localhost:7055 --tls.certfiles $(pwd)/"+mainDirectory+"/fabric-ca-server-org1/tls/cert.pem --csr.hosts '"+server_list.get(0).getCsrHosts().get(0)+","+server_list.get(0).getCsrHosts().get(1)+"' --mspdir $(pwd)/"+mainDirectory+"/fabric-ca-client/org1-ca/rcaadmin/msp");

            
            if(intermediate){
                deployIntermediateCA();
                new FabricIntermediateServerThread(mainDirectory);
                waitForContainer(int_ca_name);
                
                executeWSLCommand("cd "+Blockchain.mainDirectory+"/fabric-ca-client &&"
                + "./fabric-ca-client enroll -d -u https://"+icaadmin_name+":"+icaadmin_psw+"@localhost:7056 --tls.certfiles $(pwd)/"+mainDirectory+"/fabric-ca-server-int-ca/tls/cert.pem --csr.hosts '"+server_list.get(0).getCsrHosts().get(0)+","+server_list.get(0).getCsrHosts().get(1)+"' --mspdir $(pwd)/"+mainDirectory+"/fabric-ca-client/int-ca/icaadmin/msp");

            }
            
            
            
            
        }
        
        
    }
    
    private static void enrollRequestToCAServer(int i){
        executeWSLCommand("cd "+ mainDirectory +"/fabric-ca-client/tls-ca &&"
                + "mkdir tlsadmin &&"
                + "cd tlsadmin &&"
                + "mkdir msp");
        executeWSLCommand("cd "+ mainDirectory +"/fabric-ca-client &&"
                + "./fabric-ca-client enroll -d -u https://"+server_list.get(i).getAdmin()+":"+server_list.get(i).getAdminPwd()+"@127.0.0.1:7054 --tls.certfiles $(pwd)/"+mainDirectory+"/fabric-ca-client/tls-root-cert/tls-ca-cert.pem --enrollment.profile tls --mspdir "+ mainDirectory +"/fabric-ca-client/tls-ca/tlsadmin/msp");
    }
    
    private static void enrollCAAdmin(int i) {
    String caAdmin = server_list.get(i).getAdmin();
    String caAdminPwd = server_list.get(i).getAdminPwd();
    
    String fabricCaClientDir = mainDirectory + "/fabric-ca-client";
    String clientHome = fabricCaClientDir + "/tls-ca/rcaadmin";
    executeWSLCommand("mkdir -p " + fabricCaClientDir + "/tls-ca/rcaadmin/msp");
    String enrollCmd =
    "export FABRIC_CA_CLIENT_HOME=$(pwd)/"+mainDirectory+"/fabric-ca-client/tls-ca/rcaadmin &&" +
    "cd " + mainDirectory + "/fabric-ca-client && " +
    "./fabric-ca-client enroll -d " +
    "-u https://" + caAdmin + ":" + caAdminPwd + "@127.0.0.1:7054 " +
    "--tls.certfiles $(pwd)/"+mainDirectory+"/fabric-ca-client/tls-root-cert/tls-ca-cert.pem";
    executeWSLCommand(enrollCmd);
    
    
    }



    
    private static String registerNewIdentity(int i){
        
        //Scanner in = new Scanner(System.in);
        //System.out.print("Organization CA bootstrap identity name: ");
        org_name= "guest";
        //System.out.print("Organization CA bootstrap identity password: ");
        org_psw= "guestPsw";
        executeWSLCommand("cd "+ mainDirectory +"/fabric-ca-client/tls-ca &&"
                + "mkdir users &&"
                + "cd users &&"
                + "mkdir "+org_name+" &&"
                + "cd "+org_name+" &&"
                + "mkdir msp");
        
        executeWSLCommand("cd "+ mainDirectory +"/fabric-ca-client &&"
                + "mkdir org"+(i+1)+"-ca");
        
        
            
        executeWSLCommand("mkdir "+mainDirectory+"/fabric-ca-server-tls/org1-server-tls");
        executeWSLCommand("cd "+ mainDirectory +"/fabric-ca-client &&"
                + "./fabric-ca-client enroll -d -u https://"+admin_name+":"+admin_pwd+"@127.0.0.1:7054 "
                        + "--tls.certfiles $(pwd)/"+mainDirectory+"/fabric-ca-client/tls-root-cert/tls-ca-cert.pem "
                                + "--enrollment.profile tls "
                                + "--csr.hosts localhost,127.0.0.1,fabric-ca-server-org1 "
                                + "--mspdir $(pwd)/"+mainDirectory+"/fabric-ca-server-tls/org1-server-tls ");
        
        
        
        
        
                          
              
        return icaadmin_name;
    }
    
    private static void registerIntermediateCAAdmin(int i){
        executeWSLCommand("cd "+mainDirectory+"/fabric-ca-client/tls-ca &&"
                + "mkdir icaadmin &&"
                + "cd icaadmin &&"
                + "mkdir msp");
        Scanner in = new Scanner(System.in);
        System.out.print("Intermediate CA admin name: ");
        icaadmin_name= in.next();
        System.out.print("Intermediate CA admin password: ");
        icaadmin_psw= in.next();
        String server_name= "fabric-ca-server-int-ca";
        
        
        executeWSLCommand("mkdir "+mainDirectory+"/febric-ca-client/int-ca/rcaadmin");
        executeWSLCommand("cd "+ mainDirectory +"/fabric-ca-client &&"
                + "export FABRIC_CA_CLIENT_HOME=$(pwd)/int-ca/rcaadmin &&"
                + "./fabric-ca-client register -d --id.name "+icaadmin_name+" --id.secret "+icaadmin_psw+" -u https://127.0.0.1:7054 --tls.certfiles $(pwd)/"+mainDirectory+"/fabric-ca-client/tls-root-cert/tls-ca-cert.pem --mspdir $(pwd)/"+mainDirectory+"/fabric-ca-client/tls-ca/tlsadmin/msp");
        executeWSLCommand("cd "+ mainDirectory +"/fabric-ca-client &&"
                + "export FABRIC_CA_CLIENT_HOME=$(pwd) &&"
                + "./fabric-ca-client enroll -d -u https://"+icaadmin_name+":"+icaadmin_psw+"@127.0.0.1:7054 --tls.certfiles $(pwd)/"+mainDirectory+"/fabric-ca-client/tls-root-cert/tls-ca-cert.pem --csr.hosts '"+server_list.get(i).getCsrHosts().get(0)+","+server_list.get(i).getCsrHosts().get(1)+"' --mspdir "+ mainDirectory +"/fabric-ca-client/tls-ca/icaadmin/msp");
        
        String old_name=executeWSLCommandToString("find $(pwd)/"+mainDirectory+"/fabric-ca-client/tls-ca/icaadmin/msp/keystore -name '*_sk'");
        System.out.println("old_name:"+old_name);
        executeWSLCommand("mv "+old_name+" $(pwd)/"+mainDirectory+"/fabric-ca-client/tls-ca/icaadmin/msp/keystore/CA_PRIVATE_KEY");
        
        executeWSLCommand("cd "+mainDirectory+" &&"
                + "mkdir -p "+server_name+"/tls &&"
                + "cp bin/fabric-ca-server "+server_name+" &&"
                + "cp $(pwd)/"+mainDirectory+"/fabric-ca-client/tls-ca/icaadmin/msp/signcerts/cert.pem $(pwd)/"+mainDirectory+"/"+server_name+"/tls && cp $(pwd)/"+mainDirectory+"/fabric-ca-client/tls-ca/icaadmin/msp/keystore/CA_PRIVATE_KEY $(pwd)/"+mainDirectory+"/"+server_name+"/tls &&"
                + "cp $(pwd)/"+mainDirectory+"/"+fabric_ca_server_name+"/ca-cert.pem $(pwd)/"+mainDirectory+"/"+server_name+"/tls/tls-ca-cert.pem");
    }
    
    
    private static void createDockerComposeYaml(){
        executeWSLCommand("cd "+mainDirectory+" &&"
                + "cat > $(pwd)/"+mainDirectory+"/docker-compose.yaml << 'EOF'\n" +
                "version: '3'\n"+
                "networks:\n" +
                "  fabric_network:\n" +
                "    driver: bridge\n" +
                "services:\n" +
                "  ca_server:\n" +
                "    image: hyperledger/fabric-ca:1.5.15\n" +
                "    container_name: "+tls_ca_name+"\n" +
                "    ports:\n" +
                "      - \"7054:7054\"\n" +
                "    environment:\n" +
                "      - FABRIC_CA_HOME=/etc/hyperledger/fabric-ca-server\n" +
                "      - FABRIC_CA_SERVER_CA_NAME=ca-server\n" +
                "      - FABRIC_CA_SERVER_TLS_ENABLED=true\n" +
                "      - FABRIC_CA_SERVER_TLS_CERTFILE=/etc/hyperledger/fabric-ca-server/ca-cert.pem\n" +
                "      - FABRIC_CA_SERVER_TLS_KEYFILE=/etc/hyperledger/fabric-ca-server/msp/keystore/CA_PRIVATE_KEY\n" +
                "    volumes:\n" +
                "      - $(pwd)/"+mainDirectory+"/"+fabric_ca_server_name+"/:/etc/hyperledger/fabric-ca-server\n"+
                "      - $(pwd)/"+mainDirectory+"/"+fabric_ca_server_name+"/fabric-ca-server-config.yaml:/etc/hyperledger/fabric-ca-server/fabric-ca-server-config.yaml\n" +
                "    command: sh -c 'fabric-ca-server start -b"+ admin_name+":"+admin_pwd+" -d'\n"+
                "EOF");
    }
    
    private static void setupCA_TLS(int i) throws FileNotFoundException, IOException{
        try{
            fabric_ca_server_name="fabric-ca-server-tls"+(i==0? "":"(i+1)");
            executeWSLCommand("cd "+ mainDirectory +" &&"
                + "mkdir "+fabric_ca_server_name+" &&"
                + "cp bin/fabric-ca-server "+fabric_ca_server_name);
            Scanner in = new Scanner(System.in);
            System.out.println("-----------ADMIN REGISTRATION-----------");
            System.out.print("Admin's name: ");
            admin_name=in.next();
            System.out.print("Admin's password: ");
            admin_pwd=in.next();
            executeWSLCommand("cd "+ mainDirectory +"/"+fabric_ca_server_name+" &&"
                    + "./fabric-ca-server init -b "+admin_name+":"+admin_pwd+" --csr.hosts localhost,127.0.0.1");
            executeWSLCommand("cp "+ mainDirectory +"/"+fabric_ca_server_name+"/ca-cert.pem "+ mainDirectory +"/fabric-ca-client/tls-root-cert/tls-ca-cert.pem");
            //executeWSLCommand("cp "+ mainDirectory +"/"+fabric_ca_server_name+"/ca-cert.pem "+ mainDirectory +"/"+fabric_ca_server_name+"/msp/keystore/tls-ca-cert.pem");
            String old_name=executeWSLCommandToString("find $(pwd)/"+mainDirectory+"/"+fabric_ca_server_name+"/msp -name '*_sk'");
            System.out.println("old_name:"+old_name);
            executeWSLCommand("mv "+old_name+" $(pwd)/"+mainDirectory+"/"+fabric_ca_server_name+"/msp/keystore/CA_PRIVATE_KEY");
            System.out.println("------Modify the TLS CA server configuration------");
            
            
            //CA
            System.out.print("Name of this CA: ");
            tls_ca_name=in.next();
            File server_config=new File(""+ mainDirectory +"/"+fabric_ca_server_name+"/fabric-ca-server-config.yaml");
            Yaml yaml= new Yaml();
            Map<String, Object> data= yaml.load(new FileReader(server_config));
            Map<String,Object> ca=(Map<String,Object>) data.get("ca");
            ca.put("name", tls_ca_name);
            
            
            //TLS
            Map<String,Object> tls=(Map<String,Object>) data.get("tls");
            tls.put("enabled",true);
            tls.put("certfile", "/etc/hyperledger/fabric-ca-server-config/ca-cert.pem");
            tls.put("keyfile", "/etc/hyperledger/fabric-ca-server-config/msp/keystore/CA_PRIVATE_KEY");
            //Map<String,Object> tls_clientauth=(Map<String,Object>) tls.get("clientauth");


            System.out.println("Do you want to activate the mutual TLS option? s/n");
            String risp=in.next();
            Map<String,Object> tls_clientauth= (Map<String,Object>) tls.get("clientauth");
            if(risp.equals("n")){
            }else{

                tls_clientauth.put("type", "RequireAndVerifyClientCert");
            }

            tls_clientauth.put("certfiles", "$(pwd)/"+mainDirectory+"/fabric-ca-server-tls/ca-cert.pem");
            
            
            
            
            //INTERMEDIATE CA
            
            
            //PORT
            ports_used=new LinkedList<Integer>();
            
            
            ports_used.add(7054);
            data.put("port", 7054);
            
            //DB
            Map<String,Object> db=(Map<String,Object>) data.get("db");
            db.put("type", "sqlite3");
            
            Map<String, Object> csr = (Map<String, Object>) data.get("csr");

            ArrayList<String> hosts = (ArrayList<String>) csr.get("hosts");
            server_list.add(new ServerCA(admin_name, admin_pwd, server_port, hosts));
            
            //Writing
            DumperOptions options = new DumperOptions();
            options.setIndent(2);
            options.setPrettyFlow(true);
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            Yaml yamlWriter = new Yaml(options);

            try (FileWriter writer = new FileWriter(server_config)) {
                yamlWriter.dump(data, writer);
            }
            System.out.println("Config updated");
        }catch(Exception e){
            System.err.println(e.toString());
        }
        
    }
    
    private static void deployOrganizationCA(int i, String user_name) throws FileNotFoundException, IOException{
        String server_name= "fabric-ca-server-org"+(i+1);
        
        executeWSLCommand("cd "+mainDirectory+" &&"
                + "mkdir "+server_name+" &&"
                + "cp bin/fabric-ca-server "+server_name+" &&"
                + "cd "+server_name+" &&"
                + "mkdir tls");
        
        
        executeWSLCommand("cd "+mainDirectory+"/"+server_name+" &&"
                + "./fabric-ca-server init -b "+org_name+":"+org_psw);
        String old_name=executeWSLCommandToString("find $(pwd)/"+mainDirectory+"/fabric-ca-server-tls/org1-server-tls/keystore -name '*_sk'");
            System.out.println("old_name:"+old_name);
            executeWSLCommand("mv "+old_name+" $(pwd)/"+mainDirectory+"/fabric-ca-server-tls/org1-server-tls/keystore/ORG1_PRIVATE_KEY");
        executeWSLCommand("cp $(pwd)/"+mainDirectory+"/fabric-ca-server-tls/org1-server-tls/signcerts/cert.pem $(pwd)/"+mainDirectory+"/fabric-ca-server-org1/tls && cp $(pwd)/"+mainDirectory+"/fabric-ca-server-tls/org1-server-tls/keystore/ORG1_PRIVATE_KEY $(pwd)/"+mainDirectory+"/fabric-ca-server-org1/tls ");

                System.out.println("------Modify the ORGANIZATION CA Server configuration------");
            
        Scanner in = new Scanner(System.in);
        //CA
        System.out.print("Name of this CA: ");
        org_ca_name=in.next();
        File server_config=new File(""+ mainDirectory +"/"+server_name+"/fabric-ca-server-config.yaml");
        Yaml yaml= new Yaml();
        Map<String, Object> data= yaml.load(new FileReader(server_config));
        Map<String,Object> ca=(Map<String,Object>) data.get("ca");
        ca.put("name", org_ca_name);


        //TLS
        Map<String,Object> tls=(Map<String,Object>) data.get("tls");
        tls.put("enabled",true);

        tls.put("certfile", "fabric-ca-client/tls-ca/rcaadmin/msp/signcerts/cert.pem");
        tls.put("keyfile", "fabric-ca-client/tls-ca/rcaadmin/msp/keystore");

        System.out.println("Do you want to activate the mutual TLS option? s/n");
        String risp=in.next();
        if(risp.equals("n")){
        }else{
            Map<String,Object> tls_clientauth= (Map<String,Object>) tls.get("clientauth");
            tls_clientauth.put("type", "RequireAndVerifyClientCert");
        }
        

        

        

        //PORT
        

        ports_used.add(7055);
        data.put("port", 7055);

        //DB
        Map<String,Object> db=(Map<String,Object>) data.get("db");
        db.put("type", "sqlite3");
        
        Map<String, Object> csr = (Map<String, Object>) data.get("csr");

        ArrayList<String> hosts = (ArrayList<String>) csr.get("hosts");
        server_list.add(new ServerCA(admin_name, admin_pwd, server_port, hosts));

        //Writing
        DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yamlWriter = new Yaml(options);

        try (FileWriter writer = new FileWriter(server_config)) {
            yamlWriter.dump(data, writer);
        }
        System.out.println("Config updated");

        addCAtoDocker(org_ca_name, true, 7055, server_name, false);
            
    }
    
    
    private static void addCAtoDocker(String name, boolean tls, int port, String serverName, boolean inter) throws FileNotFoundException, IOException{
        Yaml yaml= new Yaml();
        File file= new File(""+ mainDirectory +"/docker-compose.yaml");
        Map<String, Object> data = yaml.load(new FileInputStream(file));
        Map<String, Object> services = (Map<String, Object>) data.get("services");
       
        
        //add organization CA service
        Map<String, Object> caOrg1 = new LinkedHashMap<>();
        caOrg1.put("image", "hyperledger/fabric-ca:1.5.15");
        caOrg1.put("container_name",name);
        caOrg1.put("environment", Arrays.asList(
            "FABRIC_CA_HOME=/etc/hyperledger/fabric-ca-server",
            "FABRIC_CA_SERVER_CA_NAME="+name,
            "FABRIC_CA_SERVER_TLS_ENABLED="+tls,
            "FABRIC_CA_SERVER_TLS_CERTFILE=/etc/hyperledger/fabric-ca-server/tls/cert.pem",
            "FABRIC_CA_SERVER_TLS_KEYFILE=/etc/hyperledger/fabric-ca-server/tls/"+((inter)? "CA_PRIVATE_KEY":"ORG1_PRIVATE_KEY")
        ));
        
        caOrg1.put("ports", Collections.singletonList(port+":"+port));
        caOrg1.put("command", "sh -c 'fabric-ca-server start -b "+ admin_name+":"+admin_pwd+" -d'");
        String path=executeWSLCommandToString("echo $(pwd)");
        if(inter){
            
            caOrg1.put("volumes", Arrays.asList(
                path + "/" + mainDirectory + "/fabric-ca-server-int-ca:/etc/hyperledger/fabric-ca-server",
                path + "/" + mainDirectory + "/fabric-ca-server-int-ca/fabric-ca-server-config.yaml:/etc/hyperledger/fabric-ca-server/fabric-ca-server-config.yaml"
            ));

        
        }else{
            caOrg1.put("volumes", Arrays.asList(
                path+"/"+mainDirectory+"/fabric-ca-server-org1:/etc/hyperledger/fabric-ca-server",
                path+"/"+mainDirectory+"/fabric-ca-server-org1/fabric-ca-server-config.yaml:/etc/hyperledger/fabric-ca-server/fabric-ca-server-config.yaml"
            ));

            
        }
        services.put(serverName, caOrg1);
        
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);

        yaml = new Yaml(options);
        FileWriter writer = new FileWriter(file);
        yaml.dump(data, writer);
    }
    
    
    private static void deployIntermediateCA() throws IOException{
        
        
        executeWSLCommand("cd "+mainDirectory+"/fabric-ca-server-int-ca &&"
                + "./fabric-ca-server init -b"+icaadmin_name+":"+icaadmin_psw);
               

        String old_name=executeWSLCommandToString("find $(pwd)/"+mainDirectory+"/fabric-ca-server-int-ca/msp -name '*_sk'");
        System.out.println("old_name:"+old_name);
        executeWSLCommand("mv "+old_name+" $(pwd)/"+mainDirectory+"/fabric-ca-server-int-ca/msp/keystore/CA_PRIVATE_KEY");
        System.out.println("------Modify the INTERMEDIATE CA server configuration------");
        Scanner in = new Scanner(System.in);
            //CA
            System.out.print("Name of this CA: ");
            int_ca_name=in.next();
            File server_config=new File(""+ mainDirectory +"/fabric-ca-server-int-ca/fabric-ca-server-config.yaml");
            Yaml yaml= new Yaml();
            Map<String, Object> data= yaml.load(new FileReader(server_config));
            Map<String,Object> ca=(Map<String,Object>) data.get("ca");
            ca.put("name", int_ca_name);
            
            
            //TLS
            Map<String,Object> tls=(Map<String,Object>) data.get("tls");
            tls.put("enabled",true);

            tls.put("certfile", "fabric-ca-client/tls-ca/rcaadmin/msp/signcerts/cert.pem");
            tls.put("keyfile", "fabric-ca-client/tls-ca/rcaadmin/msp/keystore");

            System.out.println("Do you want to activate the mutual TLS option? s/n");
            String risp=in.next();
            if(risp.equals("n")){
            }else{
                Map<String,Object> tls_clientauth= (Map<String,Object>) tls.get("clientauth");
                tls_clientauth.put("type", "RequireAndVerifyClientCert");
            }
            
            
            
            
            
            
            //PORT
            inter_port=7056;
            while(ports_used.contains(inter_port)){
                inter_port++;
            }
            
            ports_used.add(inter_port);
            data.put("ports", inter_port);
            
            //DB
            Map<String,Object> db=(Map<String,Object>) data.get("db");
            db.put("type", "sqlite3");
            
            Map<String, Object> csr = (Map<String, Object>) data.get("csr");

            ArrayList<String> hosts = (ArrayList<String>) csr.get("hosts");
            server_list.add(new ServerCA(admin_name, admin_pwd, inter_port, hosts));
            
            //Writing
            DumperOptions options = new DumperOptions();
            options.setIndent(2);
            options.setPrettyFlow(true);
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            Yaml yamlWriter = new Yaml(options);

            try (FileWriter writer = new FileWriter(server_config)) {
                yamlWriter.dump(data, writer);
            }
            System.out.println("Config updated");
            String server_name= "fabric-ca-server-int-ca";
            addCAtoDocker(int_ca_name, true, inter_port, server_name, true);
    }
    
    
    
    
    
    
    
    
    
    private static void createDirectoryForOrganizations(String channel_name) throws IOException{
        Scanner in= new Scanner(System.in);
        executeWSLCommand("cd "+mainDirectory+"/organizations &&"
                + "mkdir fabric-ca");
        executeWSLCommand("cd "+mainDirectory+"/organizations &&"
                + "mkdir ordererOrganizations");
        executeWSLCommand("cd "+mainDirectory+"/organizations &&"
                + "mkdir peerOrganizations");
        
        boolean k=true;
        
        do{
            System.out.println("-------- ORGANIZATIONS MENU --------");
            System.out.println("1) Add orderer organization");
            System.out.println("2) Add peer organization");
            System.out.println("3) Exit");
            int risp= in.nextInt();
            
            switch(risp){
                case 1:{
                    System.out.print("Orderer Organization Name: ");
                    String organization_name=in.next();
                    executeWSLCommand("cd "+mainDirectory+"/organizations/ordererOrganizations &&"
                            + "mkdir "+organization_name+" &&"
                            + "cd "+organization_name+" &&"
                            + "mkdir msp orderers");
                    System.out.println("How many orderers do you want to create?");
                    int num_orderer=in.nextInt();
                    LinkedList<String> orderers_names=new LinkedList<String>();
                    for(int i=1;i<=num_orderer;i++){
                        System.out.print("Orderer "+i+" Name: ");
                        String orderer_name=in.next()+"."+organization_name;
                        orderers_names.add(orderer_name);
                        executeWSLCommand("cd "+mainDirectory+"/organizations/ordererOrganizations/"+organization_name+"/orderers &&"
                                + "mkdir "+orderer_name+" &&"
                                + "cd "+orderer_name+" &&"
                                + "mkdir msp tls");
                    }
                    organizationAdminRegistrationEnroll(organization_name, false);
                    createConfig_yaml("organizations/ordererOrganizations/"+organization_name+"/msp");
                    
                    for(int i=0;i<num_orderer;i++){
                        createLocalMsp(organization_name,orderers_names.get(i),false);
                        copy_orderer_bin(orderers_names.get(i));
                        int port= configure_orderer(orderers_names.get(i),organization_name, num_orderer>1);
                        executeWSLCommand("cd "+mainDirectory+" &&"
                                + "mv fabric-ca-client/tls-ca/tlsadmin/msp/keystore/*_sk fabric-ca-client/tls-ca/tlsadmin/msp/keystore/key.pem");
                        
                        //Comando per aggungere l'orderer al canale
                        executeWSLCommand("export OSN_TLS_CA_ROOT_CERT="+mainDirectory+"/organizations/ordererOrganizations/"+organization_name+"/orderers/"+orderers_names.get(i)+"/tls/tls-ca-cert.pem &&"
                                        + "export ADMIN_TLS_SIGN_CERT="+mainDirectory+"/fabric-ca-client/tls-ca/tlsadmin/msp/signcerts/cert.pem &&"
                                        + "export ADMIN_TLS_PRIVATE_KEY="+mainDirectory+"/fabric-ca-client/tls-ca/tlsadmin/msp/key.pem &&"
                                        + " cd "+mainDirectory+"/bin &&"
                                        + "./osnadmin channel join --channelID channel1 --config-block "+mainDirectory+"/bin/genesis_block.pb -o 0.0.0.0:"+port+" --ca-file $OSN_TLS_CA_ROOT_CERT --client-cert $ADMIN_TLS_SIGN_CERT --client-key $ADMIN_TLS_PRIVATE_KEY");
                    }
                    break;
                }
                case 2:{
                    first_peer=null;
                    System.out.print("Peer Organization Name: ");
                    String organization_name=in.next();
                    executeWSLCommand("cd "+mainDirectory+"/organizations/peerOrganizations &&"
                            + "mkdir "+organization_name+" &&"
                            + "cd "+organization_name+" &&"
                            + "mkdir msp peers");
                    
                    System.out.println("How many peers do you want to create?");
                    int num_peer=in.nextInt();
                    LinkedList<String> peers_names=new LinkedList<String>();
                    for(int i=1;i<=num_peer;i++){
                        
                        System.out.print("Peer "+i+" Name: ");
                        String peer_name=in.next()+"."+organization_name;
                        peers_names.add(peer_name);
                        if(i==1) first_peer=peer_name;
                        executeWSLCommand("cd "+mainDirectory+"/organizations/peerOrganizations/"+organization_name+"/peers &&"
                                + "mkdir "+peer_name+" &&"
                                + "cd "+peer_name+" &&"
                                + "mkdir msp tls");
                    }
                    organizationAdminRegistrationEnroll(organization_name, true);
                    createConfig_yaml("organizations/peerOrganizations/"+organization_name+"/msp");
                    
                    for(int i=0;i<num_peer;i++){
                        createLocalMsp(organization_name,peers_names.get(i),true);
                    }
                    
                    
                    
                    for(int i=0;i<num_peer; i++){
                        copy_peer_bin(peers_names.get(i));
                        channelUpdate(channel_name, organization_name,peers_names.get(i));
                        int peer_port=configure_peer_core(peers_names.get(i), organization_name);
                        
                        executeWSLCommand("cd "+mainDirectory+" &&"
                            + "export CORE_PEER_LOCALMSPID=Org3MSP "
                            + "export CORE_PEER_MSPCONFIGPATH=organizations/peerOrganizations/"+organization_name+"/users/Admin@org3.example.com/msp "
                            + "export CORE_PEER_ADDRESS=localhost:"+peer_port+" "
                            + "export CORE_PEER_TLS_ROOTCERT_FILE=organizations/peerOrganizations/"+organization_name+"/peers/"+peers_names.get(i)+"/tls/ca.crt"
                            + "peer channel update -f config_update_in_envelope.pb -c "+channel_name+" -o orderer1.example.com --tls --cafile organizations/ordererOrganizations/Consenters/orderers/orderer1.example.com/tls/ca.crt");
                        
                        executeWSLCommand("export CORE_PEER_LOCALMSPID=Org1MSP &&"
                            + "export CORE_PEER_MSPCONFIGPATH=/mnt/c/Users/simo0/OneDrive/Documenti/NetBeansProjects/blockchain/BlockchianProject/organizations/peerOrganizations/"+organization_name+"/users/Admin@org1.example.com/msp &&"
                            + "export CORE_PEER_ADDRESS="+peers_names.get(i)+":"+peer_port+" &&"
                            + "export CORE_PEER_TLS_ENABLED=true &&"
                            + "export CORE_PEER_TLS_ROOTCERT_FILE=/mnt/c/Users/simo0/OneDrive/Documenti/NetBeansProjects/blockchain/BlockchianProject/organizations/peerOrganizations/"+organization_name+"/peers/"+peers_names.get(i)+"/tls/ca.crt &&"
                            + "cd "+mainDirectory+"/peers_bin/"+peers_names.get(i)+"/bin &&"
                                    + "./peer channel fetch 0 channel1.block "
                                    + "-o orderer1.example.com:7050 "
                                    + "--ordererTLSHostnameOverride orderer1.example.com "
                                    + "--tls --cafile /mnt/c/Users/simo0/OneDrive/Documenti/NetBeansProjects/blockchain/BlockchianProject/organizations/ordererOrganizations/Consenters/orderers/orderer1.example.com/tls/ca.crt" 
                                    + "-c channel1 &&"
                                        + "./peer channel join -b channel1.block");
                    }
                    break;
                }
                case 3:{
                    k=false;
                    break;
                }
                default:{
                    System.out.println("ERROR");
                }
            }
        }while(k);
        
        
    }
    
    private static void channelUpdate(String channel_name, String organization_name, String peer_name) throws IOException{
        executeWSLCommand("export CORE_PEER_LOCALMSPID=Org1MSP && "
                + "export CORE_PEER_MSPCONFIGPATH=$PWD/"+mainDirectory+"/organizations/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp && "
                + "export CORE_PEER_ADDRESS=localhost:7051 && "
                + "export CORE_PEER_TLS_ROOTCERT_FILE=$PWD/"+mainDirectory+"/organizations/peerOrganizations/"+organization_name+"/peers/peer0.org1.example.com/tls/ca.crt && "
                + "export FABRIC_CFG_PATH=peers_bin/"+peer_name+"/config && "
                + "cd "+mainDirectory+" && "
                + "./peer channel fetch config config_block.pb -o orderer1.example.com:7050 "
                + "-c "+channel_name+" "
                + "--tls --cafile $PWD/"+mainDirectory+"/organizations/ordererOrganizations/Consenters/orderers/orderer1.example.com/tls/ca.crt ");
        
        executeWSLCommand("cd "+mainDirectory+" &&"
                + "./configtxlator proto_decode --input config_block.pb --type common.Block --output config_block.json && "
                + "jq .data.data[0].payload.data.config config_block.json > config.json");
        
        executeWSLCommand("cp "+mainDirectory+"/config.json "+mainDirectory+"/modified_config.json");
        
        
        //aggiunta dell'organizzazione nella configurazione
        
        ObjectMapper mapper = new ObjectMapper();
        File file = new File(mainDirectory+"/modified_config.json");
        JsonNode root = mapper.readTree(file);

        // Naviga fino a Application.groups
        JsonNode appGroups = root
                .path("channel_group")
                .path("groups")
                .path("Application")
                .path("groups");
        
        if(appGroups.isObject()){
            ObjectNode appGroupsObj = (ObjectNode) appGroups;

            // Nuova org MSP
            ObjectNode newOrg = mapper.createObjectNode();
            newOrg.put("mod_policy", "Admins");

            // MSP config
            ObjectNode values = mapper.createObjectNode();
            ObjectNode mspValue = mapper.createObjectNode();
            mspValue.put("type", 0);
            mspValue.put("value", mainDirectory+"/organizations/peerOrganizations/"+organization_name+"/msp");
            values.set("MSP", mspValue);

            newOrg.set("values", values);
            newOrg.set("policies", mapper.createObjectNode());

            // Aggiunta Org
            appGroupsObj.set(organization_name, newOrg);
        }
        
        // Salvataggio file
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File("modified_config.json"), root);

        executeWSLCommand("cd "+mainDirectory+" && ./configtxlator proto_encode--input config.json --type common.Config --output config.pb");
        executeWSLCommand("cd "+mainDirectory+" && ./configtxlator proto_encode--input modified_config.json --type common.Config --output modified_config.pb");
        executeWSLCommand("cd "+mainDirectory+" && ./configtxlator compute_update -channel_id "+channel_name+" --original config.pb --updated modified_config.pb --output config_update.pb");
        
        executeWSLCommand("cd "+mainDirectory+" && ./configtxlator proto_decode --input config_update.pb --type common.ConfigUpdate --output config_update && "
                + "echo '{\"payload\":{\"header\":{\"channel_header\":{\"channel_id\":\"'"+organization_name+"'\", \"type\":2}},\"data\":{\"config_update\":'$(cat config_update.json)'}}}' | jq . > config_update_in_envelope.json");
        
        executeWSLCommand("cd "+mainDirectory+" && ./configtxlator proto_encode --input config_update_in_envelope.json --type common.Envelope --output config_update_in_envelope.pb");
        
        
    }
    
    public static void download_orderer_bin(){
        //download dei bin
        executeWSLCommand("cd "+mainDirectory+"/orderers_bin &&"
                + "mkdir original_file");
        executeWSLCommand("cd "+mainDirectory+"/orderers_bin/original_file &&"
                + "aria2c https://github.com/hyperledger/fabric/releases/download/v3.1.1/hyperledger-fabric-linux-amd64-3.1.1.tar.gz &&"
                + "tar -xvzf hyperledger-fabric-linux-amd64-3.1.1.tar.gz &&"
                + "rm hyperledger-fabric-linux-amd64-3.1.1.tar.gz");

    }
    
    private static void copy_orderer_bin(String orderer_name){
        executeWSLCommand("cd "+mainDirectory+"/orderers_bin &&"
                + "mkdir "+orderer_name);
        
        executeWSLCommand("cp -r "+mainDirectory+"/orderers_bin/original_file/* "+mainDirectory+"/orderers_bin/"+orderer_name);
    }
    
    public static void download_peer_bin(){
        //download dei bin
        executeWSLCommand("cd "+mainDirectory+"/peers_bin &&"
                + "mkdir original_file");
        executeWSLCommand("cd "+mainDirectory+"/peers_bin/original_file &&"
                + "aria2c https://github.com/hyperledger/fabric/releases/download/v3.1.1/hyperledger-fabric-linux-amd64-3.1.1.tar.gz &&"
                + "tar -xvzf hyperledger-fabric-linux-amd64-3.1.1.tar.gz &&"
                + "rm hyperledger-fabric-linux-amd64-3.1.1.tar.gz");
    }
    
    private static void copy_peer_bin(String peer_name){
        executeWSLCommand("cd "+mainDirectory+"/peers_bin &&"
                + "mkdir "+peer_name);
        
        executeWSLCommand("cp -r "+mainDirectory+"/peers_bin/original_file/* "+mainDirectory+"/peers_bin/"+peer_name);
    }
    
    public static int configure_peer_core(String peer_name, String org_name) throws FileNotFoundException, IOException{
        Scanner in= new Scanner(System.in);
        System.out.println("------------ PEER "+peer_name+" CONFIGURATION ------------");
        File peer_config=new File(""+ mainDirectory +"/peers_bin/"+peer_name+"/config/core.yaml");
        Yaml yaml= new Yaml();
        Map<String, Object> data= yaml.load(new FileReader(peer_config));
        Map<String, Object> peer= (Map<String, Object>) data.get("peer");
        
        //Cancelliamo l'handler TimeWindowCheck (non supportato)
        Map<String, Object> handlers = (Map<String, Object>) peer.get("handlers");
        List<Map<String, String>> authFilters = (List<Map<String, String>>) handlers.get("authFilters");
        authFilters.removeIf(filter -> "TimeWindowCheck".equals(filter.get("name")));

        //ID
        peer.put("id", peer_name);
        
        //NETWORK ID
        peer.put("networkId", "dev");
        
        //LISTEN ADDRESS   
        String add="0.0.0.0";
        int port=7050;
        do{
            port++;
        }while(ports_used.contains(port));
        
        peer.put("listenAddress",add+":"+port);
        
        //ADDRESS
        peer.put("address",peer_name+":"+port);
        
        //CHAINCODE LISTEN ADDRESS
        int portListenAdd=port;
        do{
            portListenAdd++;
        }while(ports_used.contains(portListenAdd));
        peer.put("chaincodeListenAddress",add+":"+portListenAdd);
        
        //CHAINCODE ADDRESS
        peer.put("chaincodeAddress", peer_name+":"+port);
        
        //MSP CONFIG PATH
        String path=executeWSLCommandToString("echo $(pwd)");
        peer.put("mspConfigPath",path+mainDirectory+"/organizations/peerOrganizations/"+org_name+"/peers"+peer_name+"/msp");
        
        //LOCAL MSP (dell'organizzazione)
        peer.put("localMspId","SampleOrg");
        
        //FILE SYSTEM PATH
        peer.put("fileSystemPath","/var/hyperledger/production/"+peer_name);
        
        //GOSSIP
        Map<String, Object> gossip=(Map<String, Object>) peer.get("gossip");

        System.out.println("Gossip configuration for " + peer_name);

        // Bootstrap
        String bootstrapPeer = (first_peer==peer_name)? "":first_peer;
        gossip.put("bootstrap", bootstrapPeer + "." + org_name + ":7051");

        // Endpoint interno
        String peerEndpoint = peer_name +":7051";
        gossip.put("endpoint", peerEndpoint);

        // Endpoint esterno
        gossip.put("externalEndpoint", peerEndpoint);
        if(peer_name.equals("peer0.org1.example.com")){
            gossip.put("useLeaderElection", true);
            gossip.put("orgLeader", true);
        }else{
           // Leader Election
            System.out.print("Should the peer use automatic leader election? (y/n): ");
            boolean useLeaderElection = (in.next()=="y")? true:false;
            gossip.put("useLeaderElection", useLeaderElection);

            // Org leader
            if (!useLeaderElection) {
                System.out.print("Will the peer be an org leader? (true/false):  ");
                boolean isOrgLeader = in.nextBoolean();
                gossip.put("orgLeader", isOrgLeader);
            } 
        }
        

        // Gossip state transfer
        Map<String, Object> state = new LinkedHashMap<>();
        if(peer_name.equals("peer0.org1.example.com")){
           state.put("enabled", false); 
        }else{
            System.out.print("Enable gossip state transfer? (true/false):  ");
            boolean stateEnabled = in.nextBoolean();
            state.put("enabled", stateEnabled);
            gossip.put("state", state); 
        }
        

        // pvtData.implicitCollectionDisseminationPolicy
        Map<String, Object> pvtData = new LinkedHashMap<>();
        Map<String, Object> implicitPolicy = new LinkedHashMap<>();
        if(peer_name.equals("peer0.org1.example.com")){
            implicitPolicy.put("requiredPeerCount", 1);
            implicitPolicy.put("maxPeerCount", 1);
            pvtData.put("implicitCollectionDisseminationPolicy", implicitPolicy);
            gossip.put("pvtData", pvtData);
        }else{
            System.out.print("requiredPeerCount (Minimum peers for private data dissemination): ");
            int requiredPeerCount = in.nextInt();
            implicitPolicy.put("requiredPeerCount", requiredPeerCount);

            System.out.print("maxPeerCount (Maximum peers for private data dissemination): ");
            int maxPeerCount = in.nextInt();
            implicitPolicy.put("maxPeerCount", maxPeerCount);

            pvtData.put("implicitCollectionDisseminationPolicy", implicitPolicy);
            gossip.put("pvtData", pvtData);
        }
        
        
        //TLS
        Map<String, Object> tls= (Map<String, Object>) peer.get("tls");
        
        // Abilita TLS
        if(peer_name.equals("peer0.org1.example.com")){
            tls.put("enabled", true);
        }else{
            System.out.print("Enable TLS (true/false): ");
            boolean tlsEnabled = in.nextBoolean();
            tls.put("enabled", tlsEnabled);
        }
        

        // Abilita mutual TLS (clientAuthRequired)
        boolean clientAuthRequired = false;
        tls.put("clientAuthRequired", clientAuthRequired);

        // Percorso base per i file TLS
        String tlsBasePath = mainDirectory + "/organizations/peerOrganizations/" + org_name +
            "/peers/" + peer_name + "/tls";

        // File del certificato TLS
        Map<String, Object> cert = new LinkedHashMap<>();
        cert.put("file", "/etc/hyperledger/fabric/tls/cert.pem");
        tls.put("cert", cert);

        // File della chiave privata TLS
        Map<String, Object> key = new LinkedHashMap<>();
        key.put("file", "/etc/hyperledger/fabric/tls/key");
        tls.put("key", key);

        // Root cert (per connessioni in uscita)
        Map<String, Object> rootcert = new LinkedHashMap<>();
        rootcert.put("file", "/etc/hyperledger/fabric/tls/tls-ca-cert.pem");
        tls.put("rootcert", rootcert);

        // clientRootCAs.files (solo se mutual TLS abilitato)
        if (clientAuthRequired) {
            Map<String, Object> clientRootCAs = new LinkedHashMap<>();
            List<String> clientRootFiles = new ArrayList<>();
            clientRootFiles.add(tlsBasePath + "/ca.crt"); // si può estendere a più cert
            clientRootCAs.put("files", clientRootFiles);
            tls.put("clientRootCAs", clientRootCAs);
        }
        
        //BCCSP
        Map<String, Object> bccsp=(Map<String, Object>) peer.get("BCCSP");
        
        // Select BCCSP provider
        String defaultProvider = "SW";
        bccsp.put("Default", defaultProvider);

        // Configuration for SW (Software Crypto)
        if (defaultProvider.equals("SW")) {
            Map<String, Object> sw = new LinkedHashMap<>();
            sw.put("Hash", "SHA2");
            sw.put("Security", 256);

            Map<String, Object> fileKeyStore = new LinkedHashMap<>();

            
            bccsp.put("SW", sw);
        }

        // Configuration for PKCS11 (HSM)
        else if (defaultProvider.equals("PKCS11")) {
            Map<String, Object> pkcs11 = new LinkedHashMap<>();

            System.out.print("Enter PKCS11 library path (e.g., /usr/local/lib/softhsm/libsofthsm2.so): ");
            in.nextLine(); // consume newline
            String library = in.nextLine().trim();
            pkcs11.put("Library", library);

            System.out.print("Enter PKCS11 token label: ");
            String label = in.nextLine().trim();
            pkcs11.put("Label", label);

            System.out.print("Enter PKCS11 user PIN: ");
            String pin = in.nextLine().trim();
            pkcs11.put("Pin", pin);

            pkcs11.put("Hash", "SHA2");
            pkcs11.put("Security", 256);

            bccsp.put("PKCS11", pkcs11);
        }
        
        // EXTERNAL BUILDERS
        Map<String, Object> externalBuilders = (Map<String, Object>) peer.get("externalBuilders");
        if(peer_name.equals("peer0.org1.example.com")){}
        else{
            System.out.print("Do you want to configure external chaincode builders? (yes/no): ");
            String response = in.next().toLowerCase();

            while (response.equals("yes")) {

                in.nextLine(); // consume newline
                System.out.print("Enter path to the external builder (e.g., /opt/builders/mybuilder): ");
                String builderPath = in.nextLine().trim();
                externalBuilders.put("path", builderPath);

                System.out.print("Enter a name for this builder (for logging purposes): ");
                String builderName = in.nextLine().trim();
                externalBuilders.put("name", builderName);

                List<String> envVars = new ArrayList<>();
                System.out.println("Enter environment variables to propagate (press Enter on empty line to finish):");
                while (true) {
                    String env = in.nextLine().trim();
                    if (env.isEmpty()) break;
                    envVars.add(env);
                }
                externalBuilders.put("propagateEnvironment", envVars);


                System.out.print("Do you want to add another builder? (yes/no): ");
                response = in.next().toLowerCase();
            }
        }
        

        // Attach to 'chaincode' section of peer
        Map<String, Object> chaincode = (Map<String, Object>) peer.get("chaincode");
        if (chaincode == null) {
            chaincode = new LinkedHashMap<>();
            peer.put("chaincode", chaincode);
        }
        chaincode.put("externalBuilders", externalBuilders);
        
        //LEDGER
        
        Map<String, Object> ledger= (Map<String, Object>) data.get("ledger");
        Map<String, Object> ledger_state= (Map<String, Object>) ledger.get("state");
        
        // Select state database type
        String stateDb;
        if(peer_name.equals("peer0.org1.example.com")){
            stateDb ="goleveldb";
        }else{
            System.out.print("Choose state database (goleveldb or CouchDB): ");
            stateDb = in.next().trim();
        }
        
        
        
        //Se si sceglie goleveldb non è necessario avviare un container in quanto è un DB integrato
        ledger_state.put("stateDatabase", stateDb);

        // If CouchDB is selected, collect additional config
        if (stateDb.equalsIgnoreCase("CouchDB")) {
            Map<String, Object> couchDBConfig = new LinkedHashMap<>();
            in.nextLine(); // Consume newline

            
            String couchAddress = "couchdb:5984";
            couchDBConfig.put("couchDBAddress", couchAddress);

            
            String couchUser = "admin";
            couchDBConfig.put("username", couchUser);

            
            String couchPass = "adminPsw";
            couchDBConfig.put("password", couchPass);

            ledger_state.put("couchDBConfig", couchDBConfig);
        }
        
        


        // Snapshot configuration
        Map<String, Object> snapshots = new LinkedHashMap<>();
        snapshots.put("rootDir", "/var/hyperledger/production/snapshots");

        ledger.put("snapshots", snapshots);
        
        //OPERATIONS
        Map<String, Object> operations=(Map<String, Object>) data.get("operations");
        
        // Operations server listen address
        String opAddress = "127.0.0.1";
        int opPort=port;
        do {
            opPort++;
        } while (ports_used.contains(opPort));
        operations.put("listenAddress", opAddress + ":" + opPort);

        // TLS configuration
        Map<String, Object> operations_tls = new LinkedHashMap<>();

        boolean operations_tlsEnabled = false;
        operations_tls.put("enabled", operations_tlsEnabled);

        

        operations.put("tls", operations_tls);
        
        Map<String, Object> metrics= (Map<String, Object>)data.get("metrics");
        
        metrics.put("provider", "disabled");

        
        
        //Writing
        DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yamlWriter = new Yaml(options);

        try (FileWriter writer = new FileWriter(peer_config)) {
            yamlWriter.dump(data, writer);
        }
        System.out.println("Config updated");

        System.out.println("Adding peer to docker_compose.yaml");
        String mspPath=path+"/"+mainDirectory+"/organizations/peerOrganizations/"+org_name+"/peers/"+peer_name+"/msp";
        String cfgPath=path+"/"+mainDirectory+"/peers_bin/"+peer_name+"/config";
        String tlsPath=path+"/"+mainDirectory+"/organizations/peerOrganizations/"+org_name+"/peers/"+peer_name+"/tls";
        LinkedList<Integer> ports= new LinkedList<Integer>();
        ports.add(port);
        boolean cDB=false;
        //Aggiungiamo il container per CouchDB se necessario
        if(stateDb.equals("CouchDB")){
            //Controlliamo se CouchDB è già in esecuzione
            if(CouchDB.exists()){
                System.out.println("CouchDB is running");
            }else{
                System.out.println("Starting CouchDB...");
                new CouchDB();
                waitForContainer("couchdb");
                cDB=true;
            }
        }
        
        if(peer_name.equals("peer0.org1.example.com")){
            return port;
        }
        //Aggiunta del peer al file docker-compose.yaml
        add_peer_to_docker(peer_name,cfgPath,mspPath,tlsPath,ports, cDB);
        
        System.out.println("Starting the peer...");
        new peerThread(peer_name, cDB, mainDirectory);
        
        waitForContainer(peer_name);
        
        return port;
        
    }
    
    
    public static int configure_orderer(String orderer_name, String org_name, boolean needClusterConfig) throws FileNotFoundException, IOException{
        Scanner in= new Scanner(System.in);
        System.out.println("------------ ORDERER "+orderer_name+"."+org_name+" CONFIGURATION ------------");
        File orderer_config=new File(""+ mainDirectory +"/orderers_bin/"+orderer_name+"/config/orderer.yaml");
        Yaml yaml= new Yaml();
        Map<String, Object> data= yaml.load(new FileReader(orderer_config));
        Map<String, Object> general= (Map<String, Object>) data.get("General");
        
        //rimozione di Backoff e Throttling (chiavi non valide per la versione di fabric utilizzata)
        general.remove("Backoff");
        general.remove("Throttling");
        // LISTEN ADDRESS & PORT
        general.put("ListenAddress", "0.0.0.0");
        
        int port=7049;
        do{
            port++;
        }while(ports_used.contains(port));
        
        general.put("ListenPort", port);
        
        //TLS
        Map<String, Object> general_tls=(Map<String,Object>)general.get("TLS");
        general_tls.put("Enabled", true);
        general_tls.put("PrivateKey", "/etc/hyperledger/fabric/tls/server.key");

        general_tls.put("Certificate", "/etc/hyperledger/fabric/tls/server.crt"); 
        general_tls.put("RootCAs", "/etc/hyperledger/fabric/tls/ca.crt");
        if(org_name.equals("Consenters")){
            general_tls.put("ClientAuthRequired", true);
            List<String> clientRootCAs = new ArrayList<>();
            //Copiamo il certificato della root nel percorso dell'orderer
            executeWSLCommand("cp "+mainDirectory+"/fabric-ca-client/tls-root-cert/tls-ca-cert.pem "+mainDirectory+"/organizations/ordererOrganizations/"+org_name+"/orderers/"+orderer_name+"/tls/");
            clientRootCAs.add("/etc/hyperledger/fabric/tls/tls-ca-cert.pem");

            general_tls.put("ClientRootCAs", clientRootCAs);
        }else{
            System.out.println("Enable mutual TLS?(y/n)");
            if(in.next().equals("y")){
                general_tls.put("ClientAuthRequired", true);
                List<String> clientRootCAs = new ArrayList<>();
                //Copiamo il certificato della root nel percorso dell'orderer
                executeWSLCommand("cp "+mainDirectory+"/fabric-ca-client/tls-root-cert/tls-ca-cert.pem "+mainDirectory+"/organizations/ordererOrganizations/"+org_name+"/orderers/"+orderer_name+"/tls/");
                clientRootCAs.add("/etc/hyperledger/fabric/tls/tls-ca-cert.pem");

                general_tls.put("ClientRootCAs", clientRootCAs);
            }
        }
        
        
        
        // CLUSTER
        if(needClusterConfig){
            Map<String, Object> general_cluster = (Map<String, Object>) general.get("Cluster");

            general_cluster.put("SendBufferSize", 100);
            
            if(org_name.equals("Consenters")){
                System.out.println("Using default listener and TLS credentials for cluster communication.");
                // If user doesn’t enable separate listener, no need to set extra fields
            }else{
                System.out.println("Use a separate listener for intra-cluster communication? (y/n)");
                if (in.next().equals("y")) {


                    general_cluster.put("ListenAddress", "0.0.0.0");


                    general_cluster.put("ListenPort", 7049);

                    String certPath = "$(pwd)/" + mainDirectory + "/organizations/ordererOrganizations/" + org_name + "/orderers/" + orderer_name + "/tls/server.crt";
                    String keyPath = "$(pwd)/" + mainDirectory + "/organizations/ordererOrganizations/" + org_name + "/orderers/" + orderer_name + "/tls/server.key";

                    general_cluster.put("ServerCertificate", certPath);
                    general_cluster.put("ServerPrivateKey", keyPath);

                    // Set client cert and key (could be reused or different)
                    general_cluster.put("ClientCertificate", certPath);
                    general_cluster.put("ClientPrivateKey", keyPath);

                    System.out.println("Separate cluster listener has been enabled with mutual TLS.");
                } else {
                    System.out.println("Using default listener and TLS credentials for cluster communication.");
                    // If user doesn’t enable separate listener, no need to set extra fields
                }
            }
            
        }else{
            general.remove("Cluster");
        }
        

        
        
        //BOOTSTRAP METHOD
        
        
        //LOCAL MSP DIR
        general.put("LocalMSPDir", "/etc/hyperledger/fabric/msp");
        //LOCAL MSP ID
        general.put("LocalMSPID","SampleOrg");
        // BCCSP
        Map<String, Object> general_bccsp = (Map<String, Object>) general.get("BCCSP");

        //System.out.println("Which crypto provider do you want to use? (SW/PKCS11)");
        //String provider = in.next().toUpperCase();

        general_bccsp.put("Default", "SW");
        Map<String, Object> sw = new HashMap<>();
        sw.put("Hash", "SHA2");
        sw.put("Security", 256);
        
        //creazione di una cartella in cui salvare le chiavi private generate per MSPo TLS
        executeWSLCommand("cd "+mainDirectory+"/organizations/ordererOrganizations/"+ org_name + "/orderers/" + orderer_name+" &&"
                + "mkdir keys");
        Map<String, Object> fileKeyStore = new HashMap<>();
        fileKeyStore.put("KeyStore", "/etc/hyperledger/fabric/keys");
        sw.put("FileKeyStore", fileKeyStore);

        general_bccsp.put("SW", sw);
        

        //FILE LEDGER
        //andiamo a creare una cartella dove verranno salvati i blocchi e lo stato del ledger
        executeWSLCommand("cd "+mainDirectory+"/organizations/ordererOrganizations/"+ org_name + "/orderers/" + orderer_name+"&&"
                + "mkdir ledger"); 
        
        Map<String, Object> fileLedger = (Map<String, Object>) data.get("FileLedger");
        String ledger = "/var/hyperledger/production/orderer";
        fileLedger.put("Location", ledger);
        
        //OPERATIONS
        if(org_name.equals("Consenters")){
            System.out.println("Using default settings");
        }else{
            System.out.println("Do you want to use the operations service?(y/n)");
            if (in.next().equals("y")) {
                Map<String, Object> operations =(Map<String, Object>) data.get("Operations");
                String op_server_add="0.0.0.0";
                port=9443;
                do{
                    port++;
                }while(ports_used.contains(port));
                operations.put("ListenAddress", op_server_add+":"+port);

                operations.put("Certificate","/etc/hyperledger/fabric/tls/cert.pem"); 
                operations.put("PrivateKey","/etc/hyperledger/fabric/tls/key");

                System.out.println("Enable mutal TLS between client and server?(y/n)");
                if (in.next().equals("y")) {
                    operations.put("ClientAuthRequired", true);
                    List<String> clientRootCAs = new ArrayList<>();


                    clientRootCAs.add("/etc/hyperledger/fabric/tls/tls-ca-cert.pem");

                    operations.put("ClientRootCAs", clientRootCAs);
                }
            }
        }
        
        
        
        //METRICS
        Map<String, Object> metrics = (Map<String, Object>) data.get("Metrics");

        System.out.println("Select metrics provider (prometheus / statsd / disabled):");

        

        metrics.put("Provider", "disabled");

        
        
        //ADMIN
        Map<String, Object> admin =(Map<String, Object>) data.get("Admin");
        Map<String, Object> admin_TLS=(Map<String, Object>) admin.get("TLS");
        admin_TLS.put("Enabled", true);
        admin_TLS.put("Certificate", "/etc/hyperledger/fabric/tls/server.crt"); 
        admin_TLS.put("PrivateKey", "/etc/hyperledger/fabric/tls/server.key");
        admin_TLS.put("ClientAuthRequired", true);
        List<String> clientRootCAs = new ArrayList<>();

        
        clientRootCAs.add("/etc/hyperledger/fabric/tls/tls-ca-cert.pem");

        admin_TLS.put("ClientRootCAs", clientRootCAs);
        //CHANNEL PARTECIPATION
        Map<String, Object> channelParticipation = (Map<String, Object>) data.get("ChannelParticipation");
        boolean enabled = false;
        if(org_name.equals("Consenters")){
            enabled=false;
        }else{
           System.out.println("Enable Channel Participation API? (y/n)");
            String enableInput = in.next().toLowerCase();
            enabled = enableInput.equals("y"); 
        }
        
        channelParticipation.put("Enabled", enabled);
        channelParticipation.put("MaxRequestBodySize", "1 MB");
        //CONSENSUS
        Map<String, Object> consensus = (Map<String, Object>) data.get("Consensus");
        consensus.put("WALDir", "/var/hyperledger/production/orderer/etcdraft/wal");
        consensus.put("SnapDir", "/var/hyperledger/production/orderer/etcdraft/snapshot");

        //Writing
        DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yamlWriter = new Yaml(options);

        try (FileWriter writer = new FileWriter(orderer_config)) {
            yamlWriter.dump(data, writer);
        }
        
        if(orderer_name.equals("orderer1.example.com") || orderer_name.equals("orderer2.example.com") || orderer_name.equals("orderer3.example.com")){
            return 0;
        }
        System.out.println("Config updated");
        //Aggiunta dell'orderer al file docker-compose.yaml
        String path=executeWSLCommandToString("echo $(pwd)");
        String mspPath=path+"/"+mainDirectory+"/organizations/ordererOrganizations/"+org_name+"/orderers/"+orderer_name+"/msp";
        String cfgPath=path+"/"+mainDirectory+"/orderers_bin/"+orderer_name+"/config";
        String tlsPath=path+"/"+mainDirectory+"/organizations/ordererOrganizations/"+org_name+"/orderers/"+orderer_name+"/tls";
        String ledgerPath=path+"/"+mainDirectory+"/organizations/ordererOrganizations/"+org_name+"/orderers/"+orderer_name+"/ledger";
        String keysPath=path+"/"+mainDirectory+"/organizations/ordererOrganizations/"+org_name+"/orderers/"+orderer_name+"/keys";
        LinkedList<Integer> ports= new LinkedList<Integer>();
        ports.add(port);
        add_orderer_to_docker(orderer_name,cfgPath,mspPath,tlsPath,ledgerPath,keysPath,ports);
        
        
        //Copia del certificato dell'admin in admincerts
        executeWSLCommand("cp "+mainDirectory+"/organizations/ordererOrganizations/"+org_name+"/orderers/"+orderer_name+"/msp/signcerts/cert.pem "+mainDirectory+"/ordererOrganizations/"+org_name+"/orderers/"+orderer_name+"/msp/admincerts/");
        //Avvio del container
        new ordererThread(mainDirectory);
        
        //Aspetto che il container si avvi
        waitForContainer(orderer_name);
        
        return port;
    }
    
    
    private static void createConfig_yaml(String path){
        executeWSLCommand("cd "+mainDirectory+" &&"
                + "cat > $(pwd)/"+mainDirectory+"/"+path+"/config.yaml << 'EOF'\n" +
                "echo 'NodeOUs:\n" +
                "  Enable: true\n" +
                "  ClientOUIdentifier:\n" +
                "    Certificate: cacerts/localhost-7055.pem\n" +
                "    OrganizationalUnitIdentifier: client\n" +
                "  PeerOUIdentifier:\n" +
                "    Certificate: cacerts/localhost-7055.pem\n" +
                "    OrganizationalUnitIdentifier: peer\n" +
                "  AdminOUIdentifier:\n" +
                "    Certificate: cacerts/localhost-7055.pem\n" +
                "    OrganizationalUnitIdentifier: admin\n" +
                "  OrdererOUIdentifier:\n" +
                "    Certificate: cacerts/localhost-7055.pem\n" +
                "    OrganizationalUnitIdentifier: orderer' > $(pwd)/"+mainDirectory+"/"+path+"/config.yaml\n"+
                "EOF");
    }
    
    private static void organizationAdminRegistrationEnroll(String org_name, boolean peer_org){
        //System.out.println("---------- ORGANIZATION ADMIN REGISTRATION ----------");
        //Scanner in= new Scanner(System.in);
        //System.out.print("Name: ");
        String name=org_name+"Admin";
        //System.out.print("Password: ");
        String psw= org_name+"AdminPsw";
        
        String org_directory= peer_org? "peerOrganizations":"ordererOrganizations";
        
        executeWSLCommand("cd "+ mainDirectory +"/fabric-ca-client &&"
                    + "./fabric-ca-client register -d --id.name "+name+" --id.secret "+psw+" -u https://localhost:7055 --tls.certfiles $(pwd)/"+mainDirectory+"/fabric-ca-server-org1/tls/cert.pem --mspdir $(pwd)/"+mainDirectory+"/fabric-ca-client/org1-ca/rcaadmin/msp --id.type admin");       

        executeWSLCommand("cd "+mainDirectory+"/fabric-ca-client &&"
                + "./fabric-ca-client enroll -u https://"+name+":"+psw+"@localhost:7055 --mspdir $(pwd)/"+mainDirectory+"/organizations/"+org_directory+"/"+org_name+"/users/Admin@"+org_name+"/msp --csr.hosts 'host1' --tls.certfiles $(pwd)/"+mainDirectory+"/fabric-ca-server-org1/tls/cert.pem");
        
        executeWSLCommand("mkdir "+mainDirectory+"/organizations/"+org_directory+"/"+org_name+"/msp &&"
                + "mkdir "+mainDirectory+"/organizations/"+org_directory+"/"+org_name+"/msp/cacerts &&"
                + "cp "+mainDirectory+"/organizations/"+org_directory+"/"+org_name+"/users/Admin@"+org_name+"/msp/localhost-7055.pem "+mainDirectory+"/organizations/"+org_directory+"/"+org_name+"/msp/cacerts &&"
                        + "mkdir "+mainDirectory+"/organizations/"+org_directory+"/"+org_name+"/msp/tlscacerts &&"
                        + "cp "+mainDirectory+"/fabric-ca-server-org1/tls/cert.pem "+mainDirectory+"/organizations/"+org_directory+"/"+org_name+"/msp/tlscacerts");
        
        executeWSLCommand("mkdir -p "+mainDirectory+"/organizations/"+org_directory+"/"+org_name+"/users/Admin@"+org_name+"/msp/admincerts");
        executeWSLCommand("cp  "+mainDirectory+"/organizations/"+org_directory+"/"+org_name+"/users/Admin@"+org_name+"/msp/signcerts/* "+mainDirectory+"/organizations/"+org_directory+"/"+org_name+"/users/Admin@"+org_name+"/msp/admincerts/");
    }
    
    
    
    private static void createLocalMsp(String org_name, String node_name, boolean peer_org) {
        String org_directory = peer_org
                ? "peerOrganizations/" + org_name + "/peers/" + node_name + "/"
                : "ordererOrganizations/" + org_name + "/orderers/" + node_name + "/";
        executeWSLCommand("mkdir "+mainDirectory+"/fabric-ca-client/org1-ca/"+node_name);
        // Registrazione identità (per MSP e TLS)
        executeWSLCommand("cd " + mainDirectory + "/fabric-ca-client &&"
                + "export FABRIC_CA_CLIENT_HOME=$(pwd)/org1-ca/"+node_name+" &&"
                + "./fabric-ca-client register -d --id.name " + node_name + " --id.secret " + node_name + "_PSW "
                + "-u https://localhost:7055 "
                + "--tls.certfiles $(pwd)/" + mainDirectory + "/fabric-ca-server-org1/tls/cert.pem "
                + "--mspdir $(pwd)/" + mainDirectory + "/fabric-ca-client/org1-ca/rcaadmin/msp "
                + "--id.type "+(peer_org? "peer":"orderer") );

        // Enrollment per MSP (identità)
        executeWSLCommand("cd " + mainDirectory + "/fabric-ca-client &&"
                + "./fabric-ca-client enroll -u https://" + node_name + ":" + node_name + "_PSW@localhost:7055 "
                + "--mspdir $(pwd)/" + mainDirectory + "/organizations/" + org_directory + "msp "
                + "--csr.hosts " + node_name + " "
                + "--tls.certfiles $(pwd)/" + mainDirectory + "/fabric-ca-server-org1/tls/cert.pem");

        // Aggiungo admincerts (legacy ma ancora richiesto in alcuni setup)
        executeWSLCommand("mkdir -p " + mainDirectory + "/organizations/" + org_directory + "msp/admincerts &&"
                + "cp " + mainDirectory + "/organizations/" + (peer_org ? "peerOrganizations/" : "ordererOrganizations/")
                + org_name +"/orderers/"+node_name+ "/msp/signcerts/cert.pem "
                + mainDirectory + "/organizations/" + org_directory + "msp/admincerts");
        
        
        executeWSLCommand("cd " + mainDirectory + "/fabric-ca-client &&"
                + "./fabric-ca-client register -d --id.name " + node_name + " --id.secret " + node_name + "_PSW "
                + "-u https://127.0.0.1:7054 "
                + "--tls.certfiles $(pwd)/"+mainDirectory+"/fabric-ca-client/tls-root-cert/tls-ca-cert.pem "
                + "--mspdir $(pwd)/"+mainDirectory+"/fabric-ca-client/tls-ca/rcaadmin/msp "
                + "--id.type "+(peer_org? "peer":"orderer") );
        // Enrollment TLS separato
        executeWSLCommand("cd " + mainDirectory + "/fabric-ca-client &&"
                + "./fabric-ca-client enroll -u https://" + node_name + ":" + node_name + "_PSW@127.0.0.1:7054 "
                + "--enrollment.profile tls "
                + "--csr.hosts " + node_name + " "
                + "--mspdir $(pwd)/" + mainDirectory + "/organizations/" + org_directory + "tls "
                + "--tls.certfiles $(pwd)/"+mainDirectory+"/fabric-ca-client/tls-root-cert/tls-ca-cert.pem");

        // Rinominare i file TLS come richiesto da Fabric
        executeWSLCommand("mv " + mainDirectory + "/organizations/" + org_directory + "tls/signcerts/cert.pem "
                + mainDirectory + "/organizations/" + org_directory + "tls/server.crt");
        executeWSLCommand("mv " + mainDirectory + "/organizations/" + org_directory + "tls/keystore/* "
                + mainDirectory + "/organizations/" + org_directory + "tls/server.key");
        executeWSLCommand("mv " + mainDirectory + "/organizations/" + org_directory + "tls/tlscacerts/* "
                + mainDirectory + "/organizations/" + org_directory + "tls/ca.crt");
        
        //è il ClientTLSCert che da PROBLEMI
        //certificati per clientTLS
        executeWSLCommand("mkdir "+mainDirectory+"/organizations/"+org_directory+"tlsclient");
        executeWSLCommand("cd " + mainDirectory + "/fabric-ca-client &&"
                + "./fabric-ca-client enroll -u https://" + node_name + ":" + node_name + "_PSW@127.0.0.1:7054 "
                + "--enrollment.profile tls "
                + "--csr.hosts " + node_name + " "
                + "--mspdir $(pwd)/" + mainDirectory + "/organizations/" + org_directory + "tlsclient "
                + "--tls.certfiles $(pwd)/"+mainDirectory+"/fabric-ca-client/tls-root-cert/tls-ca-cert.pem");
        executeWSLCommand("mv " + mainDirectory + "/organizations/" + org_directory + "tlsclient/signcerts/cert.pem "
                + mainDirectory + "/organizations/" + org_directory + "tlsclient/client.crt");
        executeWSLCommand("mv " + mainDirectory + "/organizations/" + org_directory + "tlsclient/keystore/* "
                + mainDirectory + "/organizations/" + org_directory + "tlsclient/client.key");
        executeWSLCommand("mv " + mainDirectory + "/organizations/" + org_directory + "tlsclient/tlscacerts/* "
                + mainDirectory + "/organizations/" + org_directory + "tlsclient/ca.crt");
        
    }

    
    /**
     * metodo che aggiunge un peer nel file docjer-compose.yaml per poi poter essere avviato come container
     * @param peerName nome del peer da aggiungere
     * @param cfgPath percorso del file core.yaml
     * @param mspPath ercorso che porta alla cartella msp 
     * @param ports porte utilizzate dal peer
     * @throws IOException 
     */
    private static void add_peer_to_docker(String peerName, String cfgPath, String mspPath, String tlsPath, LinkedList<Integer> ports, boolean couchDB) throws IOException{
        Yaml yaml = new Yaml();
        File file = new File(mainDirectory + "/docker-compose.yaml");

        Map<String, Object> data;
        try (InputStream inputStream = new FileInputStream(file)) {
            data = yaml.load(inputStream);
        }

        // Sezione 'services'
        Map<String, Object> services = (Map<String, Object>) data.get("services");
        if (services == null) {
            services = new LinkedHashMap<>();
            data.put("services", services);
        }
        
        // Configurazione peer
        Map<String, Object> peerConfig = new LinkedHashMap<>();
        peerConfig.put("container_name", peerName);
        peerConfig.put("image", "hyperledger/fabric-peer");
        
        // Environment
        List<String> env = new ArrayList<>();
        env.add("FABRIC_CFG_PATH=/etc/hyperledger/fabric/config");
        env.add("CORE_PEER_MSPCONFIGPATH=/etc/hyperledger/fabric/msp");
        peerConfig.put("environment", env);
        
        // Volumes
        List<String> volumes = new ArrayList<>();
        volumes.add(cfgPath + ":/etc/hyperledger/fabric/config");
        volumes.add(mspPath + ":/etc/hyperledger/fabric/msp");
        volumes.add(tlsPath + ":/etc/hyperledger/fabric/tls");
        peerConfig.put("volumes", volumes);
        
         // Ports
        peerConfig.put("ports", ports);
        // Network
        peerConfig.put("networks", Arrays.asList("fabric-network"));
        // Command
        peerConfig.put("command", "peer node start");
        if(couchDB){
            // Dipendenze, cioè i container che deve aspettare prima di avviarsi
            peerConfig.put("depends_on", Arrays.asList("couchdb"));
        }
        
        services.put(peerName, peerConfig);
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        Yaml outputYaml = new Yaml(options);

        try (FileWriter writer = new FileWriter(file)) {
            outputYaml.dump(data, writer);
        }
    }
    

       
    private static void add_orderer_to_docker(String ordererName, String cfgPath, String mspPath, String tlsPath, String ledgerPath, String keysPath, LinkedList<Integer> ports)throws IOException{
        Yaml yaml = new Yaml();
        File file = new File(mainDirectory + "/docker-compose.yaml");
        Map<String, Object> data;
        try (InputStream inputStream = new FileInputStream(file)) {
            data = yaml.load(inputStream);
        }

        Map<String, Object> services = (Map<String, Object>) data.get("services");
        if (services == null) {
            services = new LinkedHashMap<>();
            data.put("services", services);
        }

        Map<String, Object> ordererConfig = new LinkedHashMap<>();
        ordererConfig.put("container_name", ordererName);
        ordererConfig.put("image", "hyperledger/fabric-orderer");

        List<String> env = new ArrayList<>();
        env.add("FABRIC_CFG_PATH=" + "/etc/hyperledger/fabric/config");
        env.add("ORDERER_GENERAL_LOCALMSPDIR=" + "/etc/hyperledger/fabric/msp");
        env.add("ORDERER_GENERAL_LOCALMSPID=" + "SampleOrg");
        env.add("ORDERER_GENERAL_BOOTSTRAPMETHOD=file");
        env.add("ORDERER_GENERAL_BOOTSTRAPFILE=/etc/hyperledger/fabric/config/genesis_block.pb");
        ordererConfig.put("environment", env);
        
        executeWSLCommand("cp "+mainDirectory+"/bin/genesis_block.pb "+cfgPath);
        List<String> volumes = new ArrayList<>();
        volumes.add(cfgPath + ":/etc/hyperledger/fabric/config");
        volumes.add(mspPath + ":/etc/hyperledger/fabric/msp");
        volumes.add(tlsPath + ":/etc/hyperledger/fabric/tls");
        volumes.add(ledgerPath+":/var/hyperledger/production/orderer");
        volumes.add(keysPath+":/etc/hyperledger/fabric/keys");
        ordererConfig.put("volumes", volumes);
        LinkedList<String> porte= new LinkedList<String>();
        porte.add(ports.get(0)+":"+ports.get(0));
        ordererConfig.put("ports", porte);
        ordererConfig.put("command", "orderer");

        services.put(ordererName, ordererConfig);

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        Yaml outputYaml = new Yaml(options);

        try (FileWriter writer = new FileWriter(file)) {
            outputYaml.dump(data, writer);
        }
    }
    
    private static String createGenesisBlock() throws FileNotFoundException, IOException{
        
        downloadBinForGenesisBlock();
        while(!executeWSLCommandToString("ls "+mainDirectory+"/bin").contains("configtx.yaml")){
            System.out.println("Download of binaries failed, retrying...");
            downloadBinForGenesisBlock();
        }
        
        
        String path=executeWSLCommandToString("echo $(pwd)");
        Map<String, Object> profiles = new HashMap<>();

        // ---------- Profilo OrdererGenesis ----------
        Map<String, Object> ordererGenesis = new HashMap<>();

        // Orderer section
        Map<String, Object> ordererSection = new HashMap<>();
        ordererSection.put("OrdererType", "etcdraft");
        ordererSection.put("Organizations", Arrays.asList("OrdererOrg")); // usa alias *OrdererOrg
        
        
        //Creazione ClientTLSCert e ServerTLSCert per i consenter
        executeWSLCommand("cd "+mainDirectory+"/organizations/ordererOrganizations &&"
                + "mkdir Consenters &&"
                + "cd Consenters &&"
                + "mkdir orderer1.example.com orderer2.example.com orderer3.example.com &&"
                + "cd orderer1.example.com &&"
                + "mkdir msp &&"
                + "mkdir tls &&"
                + "cd .. &&"
                + "cd orderer2.example.com &&"
                + "mkdir msp &&"
                + "mkdir tls &&"
                + "cd .. &&"
                + "cd orderer3.example.com &&"
                + "mkdir msp &&"
                + "mkdir tls");
        executeWSLCommand("cd "+mainDirectory+"/organizations/peerOrganizations &&"
                + "mkdir org1.example.com && "
                + "cd org1.example.com && "
                + "mkdir msp peer0.org1.example.com && "
                + "cd peer0.org1.example.com && "
                + "mkdir msp && "
                + "mkdir tls");
        // Registrazione identità (per MSP e TLS)
        executeWSLCommand("cd " + mainDirectory + "/fabric-ca-client &&"
                + "export FABRIC_CA_CLIENT_HOME=$(pwd)/org1-ca/peer0.org1.example.com &&"
                + "./fabric-ca-client register -d --id.name peer0.org1.example.com --id.secret peer0.org1.example.com_PSW "
                + "-u https://localhost:7055 "
                + "--tls.certfiles $(pwd)/" + mainDirectory + "/fabric-ca-server-org1/tls/cert.pem "
                + "--mspdir $(pwd)/" + mainDirectory + "/fabric-ca-client/org1-ca/rcaadmin/msp "
                + "--id.type peer");
        // Enrollment per MSP (identità)
        executeWSLCommand("cd " + mainDirectory + "/fabric-ca-client &&"
                + "./fabric-ca-client enroll -u https://peer0.org1.example.com:peer0.org1.example.com_PSW@localhost:7055 "
                + "--mspdir $(pwd)/" + mainDirectory + "/organizations/peerOrganizations/org1.example.com/msp "
                + "--csr.hosts peer0.org1.example.com "
                + "--tls.certfiles $(pwd)/" + mainDirectory + "/fabric-ca-server-org1/tls/cert.pem");

        // Aggiungo admincerts (legacy ma ancora richiesto in alcuni setup)
        executeWSLCommand("mkdir -p " + mainDirectory + "/organizations/peerOrganizations/org1.example.com/msp/admincerts &&"
                + "cp " + mainDirectory + "/organizations/peerOrganizations/org1.example.com/msp/signcerts/cert.pem " + mainDirectory + "/organizations/peerOrganizations/org1.example.com/msp/admincerts");
        
        createLocalMsp("Consenters","orderer1.example.com", false);
        createLocalMsp("Consenters","orderer2.example.com", false);
        createLocalMsp("Consenters","orderer3.example.com", false);
        createLocalMsp("org1.example.com","peer0.org1.example.com", true);
        
        //CONFIGURAZIONE CONFIGTX.YAML 
        File file = new File(mainDirectory + "/bin/configtx.yaml");
        
        Yaml yaml = new Yaml();
        LoaderOptions options = new LoaderOptions();
        options.setMaxAliasesForCollections(200); 
        Yaml yamlWithOptions = new Yaml(options);

        Map<String,Object> data;
        try (InputStream inputStream = new FileInputStream(file)) {
            data = yamlWithOptions.load(inputStream);
        }

        
        //Orderers
        int port1=7079;
        int port2=7080;
        int port3=7081;
        do{
            port1++;
            port2++;
            port3++;
        }while(ports_used.contains(port1) || ports_used.contains(port2) || ports_used.contains(port3));
        List<Map<String, Object>> orgs = (List<Map<String, Object>>) data.get("Organizations");
        // prendi la prima org (es. OrdererOrg)
        Map<String, Object> ordererOrg = orgs.get(0);

        // aggiungi o modifica OrdererEndpoints
        List<String> ordererEndpoints = new ArrayList<>();
        ordererEndpoints.add("orderer1.example.com:" + port1);
        ordererEndpoints.add("orderer2.example.com:" + port2);
        ordererEndpoints.add("orderer3.example.com:" + port3);

        ordererOrg.put("OrdererEndpoints", ordererEndpoints);
        ordererOrg.put("MSPDir","/mnt/c/Users/simo0/OneDrive/Documenti/NetBeansProjects/blockchain/ProvaProject/organizations/peerOrganizations/org1.example.com/msp");
        

        
        
        //Consenters
        Map<String,Object> orderer=(Map<String,Object>) data.get("Orderer");
        orderer.put("OrdererType", "etcdraft");
        
        List<Map<String,Object>> consenters = new ArrayList<>();

        Map<String,Object> host1 = new HashMap<>();
        host1.put("Host", "orderer1.example.com");
        host1.put("Port", port1);
        host1.put("ClientTLSCert", path+"/"+mainDirectory+"/organizations/ordererOrganizations/Consenters/orderers/orderer1.example.com/tlsclient/client.crt");
        host1.put("ServerTLSCert", path+"/"+mainDirectory+"/organizations/ordererOrganizations/Consenters/orderers/orderer1.example.com/tls/server.crt");
        

        Map<String,Object> host2 = new HashMap<>();
        host2.put("Host", "orderer2.example.com");
        host2.put("Port", port2);
        host2.put("ClientTLSCert", path+"/"+mainDirectory+"/organizations/ordererOrganizations/Consenters/orderers/orderer2.example.com/tlsclient/client.crt");
        host2.put("ServerTLSCert", path+"/"+mainDirectory+"/organizations/ordererOrganizations/Consenters/orderers/orderer2.example.com/tls/server.crt");
        

        Map<String,Object> host3 = new HashMap<>();
        host3.put("Host", "orderer3.example.com");
        host3.put("Port", port3);
        host3.put("ClientTLSCert", path+"/"+mainDirectory+"/organizations/ordererOrganizations/Consenters/orderers/orderer3.example.com/tlsclient/client.crt");
        host3.put("ServerTLSCert", path+"/"+mainDirectory+"/organizations/ordererOrganizations/Consenters/orderers/orderer3.example.com/tls/server.crt");
        
        consenters.add(host1);
        consenters.add(host2);
        consenters.add(host3);

        
        
        ((Map<String,Object>) orderer.get("EtcdRaft")).put("Consenters", consenters);
        
        
        // Consortiums
        Map<String, Object> consortiums = new HashMap<>();
        Map<String, Object> sampleConsortium = new HashMap<>();
        // Creiamo un'organizzazione orderer "Host1MSP"
        Map<String, Object> host1Org = new HashMap<>();
        host1Org.put("Name", "Host1MSP");
        host1Org.put("ID", "Host1MSP");
        host1Org.put("MSPDir", path+"/"+mainDirectory + "/organizations/ordererOrganizations/Consenters/orderers/orderer1.example.com/msp");

        // policies base
        Map<String, Object> host1Policies = new HashMap<>();
        host1Policies.put("Readers", Map.of("Type", "Signature", "Rule", "OR('Host1MSP.member')"));
        host1Policies.put("Writers", Map.of("Type", "Signature", "Rule", "OR('Host1MSP.member')"));
        host1Policies.put("Admins", Map.of("Type", "Signature", "Rule", "OR('Host1MSP.admin')"));
        host1Org.put("Policies", createOrgPolicies("Host1MSP"));

        // Ripeti per orderer2.example.com e orderer3.example.com
        Map<String, Object> host2Org = new HashMap<>();
        host2Org.put("Name", "Host2MSP");
        host2Org.put("ID", "Host2MSP");
        host2Org.put("MSPDir", path+"/"+mainDirectory + "/organizations/ordererOrganizations/Consenters/orderers/orderer2.example.com/msp");
        host2Org.put("Policies", createOrgPolicies("Host2MSP"));

        Map<String, Object> host3Org = new HashMap<>();
        host3Org.put("Name", "Host3MSP");
        host3Org.put("ID", "Host3MSP");
        host3Org.put("MSPDir", path+"/"+mainDirectory + "/organizations/ordererOrganizations/Consenters/orderers/orderer3.example.com/msp");
        host3Org.put("Policies", createOrgPolicies("Host3MSP"));

        // Lista organizations (solo orderer per ora)
        List<Map<String, Object>> ordererOrgs = new ArrayList<>();
        ordererOrgs.add(host1Org);
        ordererOrgs.add(host2Org);
        ordererOrgs.add(host3Org);

        // Sostituisci direttamente "Organizations" con la lista
        orderer.put("Organizations", ordererOrgs);


        consortiums.put("SampleConsortium", sampleConsortium);

        
        
        
        // ---------- Profilo SampleAppChannelEtcdRaft ----------
        Map<String, Object> SampleAppChannelEtcdRaft = (Map<String,Object>) ((Map<String,Object>) data.get("Profiles")).get("SampleAppChannelEtcdRaft");

        SampleAppChannelEtcdRaft.put("Consortium", "SampleConsortium");
        SampleAppChannelEtcdRaft.put("Consortiums","Consortiums:\r\n" + 
                        "      SampleConsortium:\r\n" + 
                        "        Organizations: [\r\n" + 
                        "          ]");
        Map<String, Object> SectionOrderer = (Map<String,Object>) SampleAppChannelEtcdRaft.get("Orderer");
        host1 = new HashMap<>();
        host1.put("Name", "Host1MSP");
        host1.put("ID", "Host1MSP");
        host1.put("MSPDir", path+"/"+mainDirectory + "/organizations/ordererOrganizations/Consenters/orderers/orderer1.example.com/msp");
        host1.put("Policies", createOrgPolicies("Host1MSP")); // solo Policies

        host2 = new HashMap<>();
        host2.put("Name", "Host2MSP");
        host2.put("ID", "Host2MSP");
        host2.put("MSPDir", path+"/"+mainDirectory + "/organizations/ordererOrganizations/Consenters/orderers/orderer2.example.com/msp");
        host2.put("Policies", createOrgPolicies("Host2MSP"));

        host3 = new HashMap<>();
        host3.put("Name", "Host3MSP");
        host3.put("ID", "Host3MSP");
        host3.put("MSPDir", path+"/"+mainDirectory + "/organizations/ordererOrganizations/Consenters/orderers/orderer3.example.com/msp");
        host3.put("Policies", createOrgPolicies("Host3MSP"));

        //List<Map<String,Object>> appOrgs = Arrays.asList(host1, host2, host3);
        //applicationSection.put("Organizations", appOrgs);

        SectionOrderer.put("Organizations", Arrays.asList(host1,host2,host3)); 
        
        //SampleAppChannelEtcdRaft.put("Organizations", Arrays.asList(host1,host2,host3));
        
        
        Map<String, Object> SectionApplication=(Map<String, Object>) SampleAppChannelEtcdRaft.get("Application");
        HashMap<String,Object> Org = new HashMap<>();
        Org.put("Name", "Org1");
        Org.put("ID", "Org1");
        Org.put("MSPDir", path+"/"+mainDirectory + "/organizations/peerOrganizations/org1.example.com/msp");
        Org.put("Policies", createOrgPolicies("Org1"));
        SectionApplication.put("Organizations", Arrays.asList(Org));

        ordererSection.put("Organizations", ordererOrgs);
        
        sampleConsortium = new HashMap<>();

        // Inserisci le organizzazioni (per ora vuote)
        sampleConsortium.put("Organizations", new ArrayList<>());


        Map<String, Object> ordererGenesisConsortiums= new HashMap<>();

        // Inserisci nel blocco Consortiums di OrdererGenesis
        ordererGenesisConsortiums.put("SampleConsortium", sampleConsortium);

        // Ora inserisci il tutto in OrdererGenesis
        //ordererGenesis.put("Consortiums", ordererGenesisConsortiums);

        // Assicurati di aver già popolato OrdererGenesis.Orderer con ordererSection
        //ordererGenesis.put("Orderer", ordererSection);

        // Infine inserisci OrdererGenesis nei profili
        profiles.put("OrdererGenesis", ordererGenesis);
        
        // ---------- Inserisci nei profili ----------
        Map<String, Object> etcdRaft = new HashMap<>();
        etcdRaft.put("Consenters", consenters);
        List<String> addresses = new ArrayList<>();
        addresses.add("orderer1.example.com:"+port1);
        addresses.add("orderer2.example.com:"+port2);
        addresses.add("orderer3.example.com:"+port3);
        Map<String, Object> ordererGenesisOrderer = new HashMap<>();
        
        // Inserisci nelle Policies dell'Orderer
        ordererGenesisOrderer.put("Policies", createOrdererPolicies(true)); 
        ordererGenesisOrderer.put("OrdererType", "etcdraft");
        ordererGenesisOrderer.put("Addresses", addresses);
        ordererGenesisOrderer.put("EtcdRaft", etcdRaft);
        ordererGenesisOrderer.put("Organizations", data.get("Orderer") != null ? ((Map<String,Object>) data.get("Orderer")).get("Organizations") : new ArrayList<>());
        
        Map<String, Object> appPolicies = new HashMap<>();
        appPolicies.put("Readers", createOrdererPolicies(false).get("Readers"));
        appPolicies.put("Writers", createOrdererPolicies(false).get("Writers"));
        appPolicies.put("Admins", createOrdererPolicies(false).get("Admins"));

        //applicationSection.put("Policies", appPolicies);
        //applicationChannel.put("Application", applicationSection);

        
        //profiles.put("ApplicationChannel", applicationChannel);
        // Assembla OrdererGenesis
        ordererGenesis.put("Policies", createOrdererPolicies(false));
        ordererGenesis.put("Consortiums", ordererGenesisConsortiums);
        ordererGenesis.put("Orderer", ordererGenesisOrderer);
        
        
        // Infine: inserisci "Profiles" nella root del tuo data
        //data.put("Profiles", profiles);
        
        //Aggiunta Consortiums al profilo SampleAppChannelEtcdRaft
        Map<String,Object> SampleAppChannelEtcdRaft_profile=(Map<String,Object>)((Map<String,Object>) data.get("Profiles")).get("SampleAppChannelEtcdRaft");
        sampleConsortium = new LinkedHashMap<>();
        sampleConsortium.put("Organizations", Arrays.asList(host1,host2,host3));

        consortiums = new LinkedHashMap<>();
        consortiums.put("SampleConsortium", sampleConsortium);

        //data.put("Consortiums", consortiums);

        
        //Writing
        DumperOptions op = new DumperOptions();
        op.setIndent(2);
        op.setPrettyFlow(true);
        op.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        Yaml yamlWriter = new Yaml(op);

        try (FileWriter writer = new FileWriter(file)) {
            yamlWriter.dump(data, writer);
        }
        System.out.println("Config updated");
        Scanner in= new Scanner(System.in);
            
        //avvio degli orderer orderer1.example.com, orderer2.example.com e orderer3.example.com
        
        //------------------HOST1------------------
        organizationAdminRegistrationEnroll("Consenters", false);
        createConfig_yaml("organizations/ordererOrganizations/"+"Consenters"+"/msp");
        copy_orderer_bin("orderer1.example.com");
        configure_orderer("orderer1.example.com","Consenters", true);
        //Copia del certificato dell'admin in admincerts
        executeWSLCommand("cp "+mainDirectory+"/organizations/ordererOrganizations/Consenters/orderers/orderer1.example.com/msp/signcerts/cert.pem "+mainDirectory+"/organizations/ordererOrganizations/Consenters/orderers/orderer1.example.com/msp/admincerts/");
        //Avvio del container
        
        
        //------------------HOST2------------------
        copy_orderer_bin("orderer2.example.com");
        configure_orderer("orderer2.example.com","Consenters", true);
        //Copia del certificato dell'admin in admincerts
        executeWSLCommand("cp "+mainDirectory+"/organizations/ordererOrganizations/Consenters/orderers/orderer2.example.com/msp/signcerts/cert.pem "+mainDirectory+"/organizations/ordererOrganizations/Consenters/orderers/orderer2.example.com/msp/admincerts/");
        //Avvio del container
        
        
        //------------------HOST3------------------
        copy_orderer_bin("orderer3.example.com");
        configure_orderer("orderer3.example.com","Consenters", true);
        //Copia del certificato dell'admin in admincerts
        executeWSLCommand("cp "+mainDirectory+"/organizations/ordererOrganizations/Consenters/orderers/orderer3.example.com/msp/signcerts/cert.pem "+mainDirectory+"/organizations/ordererOrganizations/Consenters/orderers/orderer3.example.com/msp/admincerts/");
        
        //------------------peer------------------
        copy_peer_bin("peer0.org1.example.com");
        configure_peer_core("peer0.org1.example.com","org1.example.com");
        executeWSLCommand("mkdir "+mainDirectory+"/organizations/peerOrganizations/org1.example.com/users/Admin@org1.example.com"
                + "mkdir "+mainDirectory+"/organizations/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp");
        organizationAdminRegistrationEnroll("org1.example.com", true);
        
        
        //Avvio del container
        addConsentersDocker();
        new ordererThread(mainDirectory);
        waitForContainer("orderer1.example.com");
        waitForContainer("orderer2.example.com");
        waitForContainer("orderer3.example.com");
        
        //CREAZIONE DEL GENESIS BLOCK
        System.out.println("Channel name: ");
        String channel_name=in.next();
        
        /*executeWSLCommand("cd "+mainDirectory+"/bin &&"
                + "./configtxgen -profile SampleAppChannelEtcdRaft "
                + "-outputCreateChannelTx "+channel_name+".tx "
                + "-channelID "+channel_name);
        executeWSLCommand("cp "+mainDirectory+"/bin/peer "+mainDirectory);
        executeWSLCommand("export FABRIC_CFG_PATH=$(pwd)/"+mainDirectory+"/bin &&"
                + "export CORE_PEER_MSPCONFIGPATH=$(pwd)/Prova/organizations/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp &&"                            
                +"cd "+mainDirectory+" &&"
                + "./peer channel create "
                + "-c "+channel_name+" "
                + "-f bin/"+channel_name+".tx "
                + "-o orderer1.example.com:7050 "
                + "--outputBlock bin/"+channel_name+"_block.pb "
                + "--tls "
                + "--cafile $(pwd)/"+mainDirectory+"/organizations/ordererOrganizations/Consenters/orderers/orderer1.example.com/tls/ca.crt");*/
        executeWSLCommand("cp -r $(pwd)/"+mainDirectory+"/organizations/peerOrganizations/org1.example.com/msp $(pwd)/"+mainDirectory+"/bin");
        executeWSLCommand("cd $(pwd)/"+mainDirectory+"/bin &&"
                + "./configtxgen -configPath $(pwd)/"+mainDirectory+"/bin  -profile SampleAppChannelEtcdRaft -channelID "+channel_name+" -outputBlock ./"+channel_name+"_block.pb");
    
        
        executeWSLCommand("cd "+mainDirectory+" &&"
                                + "mv fabric-ca-client/tls-ca/tlsadmin/msp/keystore/*_sk fabric-ca-client/tls-ca/tlsadmin/msp/keystore/key.pem");
        executeWSLCommand("cp "+mainDirectory+"/bin/osnadmin "+mainDirectory);
                        
        //Comando per aggungere l'orderer orderer1.example.com al canale
        executeWSLCommand("cd "+mainDirectory+" &&"
                        + "./osnadmin channel join "
                        + "--channelID "+channel_name+" "
                        + "--config-block bin/"+channel_name+"_block.pb "
                        + "--orderer-address orderer1.example.com:9443 "
                        + "--ca-file organizations/ordererOrganizations/Consenters/orderers/orderer1.example.com/tls/ca.crt "
                        + "--client-cert organizations/ordererOrganizations/Consenters/orderers/orderer1.example.com/tlsclient/client.crt "
                        + "--client-key organizations/ordererOrganizations/Consenters/orderers/orderer1.example.com/tlsclient/client.key");
        //Comando per aggungere l'orderer orderer2.example.com al canale
        executeWSLCommand("cd "+mainDirectory+" &&"
                        + "./osnadmin channel join "
                        + "--channelID "+channel_name+" "
                        + "--config-block bin/"+channel_name+"_block.pb "
                        + "--orderer-address orderer2.example.com:9444 "
                        + "--ca-file organizations/ordererOrganizations/Consenters/orderers/orderer2.example.com/tls/ca.crt "
                        + "--client-cert organizations/ordererOrganizations/Consenters/orderers/orderer2.example.com/tlsclient/client.crt "
                        + "--client-key organizations/ordererOrganizations/Consenters/orderers/orderer2.example.com/tlsclient/client.key");
             
        //Comando per aggungere l'orderer orderer3.example.com al canale
        executeWSLCommand("cd "+mainDirectory+" &&"
                        + "./osnadmin channel join "
                        + "--channelID "+channel_name+" "
                        + "--config-block bin/"+channel_name+"_block.pb "
                        + "--orderer-address orderer3.example.com:9445 "
                        + "--ca-file organizations/ordererOrganizations/Consenters/orderers/orderer3.example.com/tls/ca.crt "
                        + "--client-cert organizations/ordererOrganizations/Consenters/orderers/orderer3.example.com/tlsclient/client.crt "
                        + "--client-key organizations/ordererOrganizations/Consenters/orderers/orderer3.example.com/tlsclient/client.key");
        //Aggiunta peer
        executeWSLCommand("export CORE_PEER_LOCALMSPID=SampleOrg && "
                        + "export CORE_PEER_MSPCONFIGPATH=$(pwd)/"+mainDirectory+"/organizations/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/msp && "
                        + "export CORE_PEER_ADDRESS=peer0.org1.example.com:7051 && "
                        + "cd "+mainDirectory+" && "
                        + "./peer channel join -b bin/"+channel_name+"_block.pb");
        //Riavvio dei container
        
        /*executeWSLCommand("cd "+mainDirectory+" && "
                + "docker restart orderer1.example.com");
        
        executeWSLCommand("cd "+mainDirectory+" && "
                + "docker restart orderer2.example.com");
        
        executeWSLCommand("cd "+mainDirectory+" && "
                + "docker restart orderer3.example.com");
        
        executeWSLCommand("cp "+mainDirectory+"/bin/peer "+mainDirectory);
        executeWSLCommand("cp "+mainDirectory+"/bin/configtxlator "+mainDirectory);
        channelUpdate(channel_name,"org1.example.com","peer0.org1.example.com");
        
        
        executeWSLCommand("cd "+mainDirectory+" &&"
                            + "export CORE_PEER_LOCALMSPID=Org3MSP &&"
                            + "export CORE_PEER_MSPCONFIGPATH=organizations/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp &&"
                            + "export CORE_PEER_ADDRESS=localhost:7051 &&"
                            + "export CORE_PEER_TLS_ROOTCERT_FILE=organizations/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls/ca.crt &&"
                            + "./peer channel update -f config_update_in_envelope.pb -c "+channel_name+" -o orderer1.example.com --tls --cafile organizations/ordererOrganizations/Consenters/orderers/orderer1.example.com/tls/ca.crt");
                        
        
        executeWSLCommand("export CORE_PEER_LOCALMSPID=Org1MSP &&"
                + "export CORE_PEER_MSPCONFIGPATH=/mnt/c/Users/simo0/OneDrive/Documenti/NetBeansProjects/blockchain/BlockchianProject/organizations/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp &&"
                + "export CORE_PEER_ADDRESS=peer0.org1.example.com:7051 &&"
                + "export CORE_PEER_TLS_ENABLED=true &&"
                + "export CORE_PEER_TLS_ROOTCERT_FILE=/mnt/c/Users/simo0/OneDrive/Documenti/NetBeansProjects/blockchain/BlockchianProject/organizations/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls/ca.crt &&"
                + "cd "+mainDirectory+" &&"
                        + "./peer channel fetch 0 channel1.block "
                        + "-o orderer1.example.com:7050 "
                        + "--ordererTLSHostnameOverride orderer1.example.com "
                        + "--tls --cafile /mnt/c/Users/simo0/OneDrive/Documenti/NetBeansProjects/blockchain/BlockchianProject/organizations/ordererOrganizations/example.com/orderers/orderer1.example.com/tls/ca.crt "
                        + "-c channel1 &&"
                            + "./peer channel join -b channel1.block");*/
        
        return channel_name;
    }
    
    private static Map<String,Object> createOrdererPolicies(boolean blockValidation){
        Map<String,Object> policies = new HashMap<>();

        Map<String,Object> readers = new HashMap<>();
        readers.put("Type", "ImplicitMeta");
        readers.put("Rule", "ANY Readers");

        Map<String,Object> writers = new HashMap<>();
        writers.put("Type", "ImplicitMeta");
        writers.put("Rule", "ANY Writers");

        Map<String,Object> admins = new HashMap<>();
        admins.put("Type", "ImplicitMeta");
        admins.put("Rule", "MAJORITY Admins");

        policies.put("Readers", readers);
        policies.put("Writers", writers);
        policies.put("Admins", admins);
        
        if(blockValidation){
            Map<String,Object> blkVal= new HashMap<>();
            blkVal.put("Type", "ImplicitMeta");
            blkVal.put("Rule", "ANY Writers");
            
            policies.put("BlockValidation", blkVal);
        }
        
        return policies;
    }
    private static Map<String,Object> createOrgPolicies(String mspId) {
        Map<String,Object> policies = new HashMap<>();

        Map<String,Object> readers = new HashMap<>();
        readers.put("Type", "Signature");
        readers.put("Rule", "OR('" + mspId + ".member')");

        Map<String,Object> writers = new HashMap<>();
        writers.put("Type", "Signature");
        writers.put("Rule", "OR('" + mspId + ".member')");

        Map<String,Object> admins = new HashMap<>();
        admins.put("Type", "Signature");
        admins.put("Rule", "OR('" + mspId + ".admin')");

        policies.put("Readers", readers);
        policies.put("Writers", writers);
        policies.put("Admins", admins);

        return policies;
    }
    
    /**
     * This method is used to install all the binaries needed for the creation of the genesis block
     */
    private static void downloadBinForGenesisBlock(){
        executeWSLCommand("cd $(pwd)/"+mainDirectory+"/bin &&"
                + "aria2c https://github.com/hyperledger/fabric/releases/download/v2.5.0/hyperledger-fabric-linux-amd64-2.5.0.tar.gz -o fabric-bin.tar.gz &&"
                + "tar -xvzf fabric-bin.tar.gz --strip-components=1");
    }
    
    /**
     * This method is used to add orderer1.example.com, orderer2.example.com and orderer3.example.com to the docker-compose.yaml file
     */
    private static void addConsentersDocker() throws IOException{
        String file_content="  orderer1.example.com:\n" +
                "    image: hyperledger/fabric-orderer:2.5\n" +
                "    container_name: orderer1.example.com\n" +
                "    environment:\n" +
                "      - FABRIC_LOGGING_SPEC=INFO\n" +
                "\n" +
                "      # Network\n" +
                "      - ORDERER_GENERAL_LISTENADDRESS=0.0.0.0\n" +
                "      - ORDERER_GENERAL_LISTENPORT=7050\n" +
                "\n" +
                "      # Cluster (Raft)\n" +
                "      - ORDERER_GENERAL_CLUSTER_LISTENADDRESS=0.0.0.0\n" +
                "      - ORDERER_GENERAL_CLUSTER_LISTENPORT=7080\n" +
                "      - ORDERER_GENERAL_CLUSTER_SERVERCERTIFICATE=/var/hyperledger/orderer/tls/server.crt\n" +
                "      - ORDERER_GENERAL_CLUSTER_SERVERPRIVATEKEY=/var/hyperledger/orderer/tls/server.key\n" +
                "      - ORDERER_GENERAL_CLUSTER_CLIENTCERTIFICATE=/var/hyperledger/orderer/tlsclient/client.crt\n" +
                "      - ORDERER_GENERAL_CLUSTER_CLIENTPRIVATEKEY=/var/hyperledger/orderer/tlsclient/client.key\n" +
                "      - ORDERER_GENERAL_CLUSTER_ROOTCAS=[/var/hyperledger/orderer/tls/ca.crt]\n" +
                "      # MSP\n" +
                "      - ORDERER_GENERAL_LOCALMSPID=OrdererOrg\n" +
                "      - ORDERER_GENERAL_LOCALMSPDIR=/var/hyperledger/orderer/msp\n" +
                "\n" +
                "      # TLS\n" +
                "      - ORDERER_GENERAL_TLS_ENABLED=true\n" +
                "      - ORDERER_GENERAL_TLS_PRIVATEKEY=/var/hyperledger/orderer/tls/server.key\n" +
                "      - ORDERER_GENERAL_TLS_CERTIFICATE=/var/hyperledger/orderer/tls/server.crt\n" +
                "      - ORDERER_GENERAL_TLS_ROOTCAS=[/var/hyperledger/orderer/tls/ca.crt]\n" +
                "\n" +
                "      # Bootstrap\n" +
                "      - ORDERER_GENERAL_BOOTSTRAPMETHOD=none\n" +
                "\n" +
                "      # Admin service\n" +
                "      - ORDERER_ADMIN_LISTENADDRESS=0.0.0.0:9443\n" +
                "      - ORDERER_ADMIN_TLS_ENABLED=true\n" +
                "      - ORDERER_ADMIN_TLS_PRIVATEKEY=/var/hyperledger/orderer/tls/server.key\n" +
                "      - ORDERER_ADMIN_TLS_CERTIFICATE=/var/hyperledger/orderer/tls/server.crt\n" +
                "      - ORDERER_ADMIN_TLS_CLIENTAUTHREQUIRED=true\n" +
                "      - ORDERER_ADMIN_TLS_CLIENTROOTCAS=[/var/hyperledger/orderer/tls/ca.crt]\n" +
                "\n" +
                "      # Channel participation\n" +
                "      - ORDERER_CHANNELPARTICIPATION_ENABLED=true\n" +
                "    working_dir: /opt/gopath/src/github.com/hyperledger/fabric\n" +
                "    command: orderer\n" +
                "    ports:\n" +
                "      - 9443:9443\n" +
                "      - 7050:7050\n" +
                "      - 7080:7080\n" +
                "    volumes:\n" +
                "      - /mnt/c/Users/simo0/OneDrive/Documenti/NetBeansProjects/blockchain/"+mainDirectory+"/organizations/ordererOrganizations/Consenters/orderers/orderer1.example.com/msp:/var/hyperledger/orderer/msp\n" +
                "      - /mnt/c/Users/simo0/OneDrive/Documenti/NetBeansProjects/blockchain/"+mainDirectory+"/organizations/ordererOrganizations/Consenters/orderers/orderer1.example.com/tls:/var/hyperledger/orderer/tls\n" +
                "      - /mnt/c/Users/simo0/OneDrive/Documenti/NetBeansProjects/blockchain/"+mainDirectory+"/organizations/ordererOrganizations/Consenters/orderers/orderer1.example.com/tlsclient:/var/hyperledger/orderer/tlsclient\n" +
                "      - /mnt/c/Users/simo0/OneDrive/Documenti/NetBeansProjects/blockchain/"+mainDirectory+"/bin/genesis_block.pb:/var/hyperledger/orderer/orderer.genesis.block\n" +
                "    networks:\n" +
                "      - fabric_network\n" +
                "\n" +
                "  orderer2.example.com:\n" +
                "    image: hyperledger/fabric-orderer:2.5\n" +
                "    container_name: orderer2.example.com\n" +
                "    environment:\n" +
                "      - FABRIC_LOGGING_SPEC=INFO\n" +
                "\n" +
                "      # Network\n" +
                "      - ORDERER_GENERAL_LISTENADDRESS=0.0.0.0\n" +
                "      - ORDERER_GENERAL_LISTENPORT=7050\n" +
                "\n" +
                "      # Cluster (Raft)\n" +
                "      - ORDERER_GENERAL_CLUSTER_LISTENADDRESS=0.0.0.0\n" +
                "      - ORDERER_GENERAL_CLUSTER_LISTENPORT=7081\n" +
                "      - ORDERER_GENERAL_CLUSTER_SERVERCERTIFICATE=/var/hyperledger/orderer/tls/server.crt\n" +
                "      - ORDERER_GENERAL_CLUSTER_SERVERPRIVATEKEY=/var/hyperledger/orderer/tls/server.key\n" +
                "      - ORDERER_GENERAL_CLUSTER_CLIENTCERTIFICATE=/var/hyperledger/orderer/tlsclient/client.crt\n" +
                "      - ORDERER_GENERAL_CLUSTER_CLIENTPRIVATEKEY=/var/hyperledger/orderer/tlsclient/client.key\n" +
                "      - ORDERER_GENERAL_CLUSTER_ROOTCAS=[/var/hyperledger/orderer/tls/ca.crt]\n" +
                "      # MSP\n" +
                "      - ORDERER_GENERAL_LOCALMSPID=OrdererOrg\n" +
                "      - ORDERER_GENERAL_LOCALMSPDIR=/var/hyperledger/orderer/msp\n" +
                "\n" +
                "      # TLS\n" +
                "      - ORDERER_GENERAL_TLS_ENABLED=true\n" +
                "      - ORDERER_GENERAL_TLS_PRIVATEKEY=/var/hyperledger/orderer/tls/server.key\n" +
                "      - ORDERER_GENERAL_TLS_CERTIFICATE=/var/hyperledger/orderer/tls/server.crt\n" +
                "      - ORDERER_GENERAL_TLS_ROOTCAS=[/var/hyperledger/orderer/tls/ca.crt]\n" +
                "\n" +
                "      # Bootstrap\n" +
                "      - ORDERER_GENERAL_BOOTSTRAPMETHOD=none\n" +
                "\n" +
                "      # Admin service\n" +
                "      - ORDERER_ADMIN_LISTENADDRESS=0.0.0.0:9444\n" +
                "      - ORDERER_ADMIN_TLS_ENABLED=true\n" +
                "      - ORDERER_ADMIN_TLS_PRIVATEKEY=/var/hyperledger/orderer/tls/server.key\n" +
                "      - ORDERER_ADMIN_TLS_CERTIFICATE=/var/hyperledger/orderer/tls/server.crt\n" +
                "      - ORDERER_ADMIN_TLS_CLIENTAUTHREQUIRED=true\n" +
                "      - ORDERER_ADMIN_TLS_CLIENTROOTCAS=[/var/hyperledger/orderer/tls/ca.crt]\n" +
                "\n" +
                "      # Channel participation\n" +
                "      - ORDERER_CHANNELPARTICIPATION_ENABLED=true\n" +
                "    working_dir: /opt/gopath/src/github.com/hyperledger/fabric\n" +
                "    command: orderer\n" +
                "    ports:\n" +
                "      - 9444:9444\n" +
                "      - 8050:7050\n" +
                "      - 7081:7081\n" +
                "    volumes:\n" +
                "      - /mnt/c/Users/simo0/OneDrive/Documenti/NetBeansProjects/blockchain/"+mainDirectory+"/organizations/ordererOrganizations/Consenters/orderers/orderer2.example.com/msp:/var/hyperledger/orderer/msp\n" +
                "      - /mnt/c/Users/simo0/OneDrive/Documenti/NetBeansProjects/blockchain/"+mainDirectory+"/organizations/ordererOrganizations/Consenters/orderers/orderer2.example.com/tls:/var/hyperledger/orderer/tls\n" +
                "      - /mnt/c/Users/simo0/OneDrive/Documenti/NetBeansProjects/blockchain/"+mainDirectory+"/organizations/ordererOrganizations/Consenters/orderers/orderer2.example.com/tlsclient:/var/hyperledger/orderer/tlsclient\n" +
                "      - /mnt/c/Users/simo0/OneDrive/Documenti/NetBeansProjects/blockchain/"+mainDirectory+"/bin/genesis_block.pb:/var/hyperledger/orderer/orderer.genesis.block\n" +
                "    networks:\n" +
                "      - fabric_network\n" +
                "\n" +
                "  orderer3.example.com:\n" +
                "    image: hyperledger/fabric-orderer:2.5\n" +
                "    container_name: orderer3.example.com\n" +
                "    environment:\n" +
                "      - FABRIC_LOGGING_SPEC=INFO\n" +
                "\n" +
                "      # Network\n" +
                "      - ORDERER_GENERAL_LISTENADDRESS=0.0.0.0\n" +
                "      - ORDERER_GENERAL_LISTENPORT=7050\n" +
                "\n" +
                "      # Cluster (Raft)\n" +
                "      - ORDERER_GENERAL_CLUSTER_LISTENADDRESS=0.0.0.0\n" +
                "      - ORDERER_GENERAL_CLUSTER_LISTENPORT=7082\n" +
                "      - ORDERER_GENERAL_CLUSTER_SERVERCERTIFICATE=/var/hyperledger/orderer/tls/server.crt\n" +
                "      - ORDERER_GENERAL_CLUSTER_SERVERPRIVATEKEY=/var/hyperledger/orderer/tls/server.key\n" +
                "      - ORDERER_GENERAL_CLUSTER_CLIENTCERTIFICATE=/var/hyperledger/orderer/tlsclient/client.crt\n" +
                "      - ORDERER_GENERAL_CLUSTER_CLIENTPRIVATEKEY=/var/hyperledger/orderer/tlsclient/client.key\n" +
                "      - ORDERER_GENERAL_CLUSTER_ROOTCAS=[/var/hyperledger/orderer/tls/ca.crt]\n" +
                "      # MSP\n" +
                "      - ORDERER_GENERAL_LOCALMSPID=OrdererOrg\n" +
                "      - ORDERER_GENERAL_LOCALMSPDIR=/var/hyperledger/orderer/msp\n" +
                "\n" +
                "      # TLS\n" +
                "      - ORDERER_GENERAL_TLS_ENABLED=true\n" +
                "      - ORDERER_GENERAL_TLS_PRIVATEKEY=/var/hyperledger/orderer/tls/server.key\n" +
                "      - ORDERER_GENERAL_TLS_CERTIFICATE=/var/hyperledger/orderer/tls/server.crt\n" +
                "      - ORDERER_GENERAL_TLS_ROOTCAS=[/var/hyperledger/orderer/tls/ca.crt]\n" +
                "\n" +
                "      # Bootstrap\n" +
                "      - ORDERER_GENERAL_BOOTSTRAPMETHOD=none\n" +
                "\n" +
                "      # Admin service\n" +
                "      - ORDERER_ADMIN_LISTENADDRESS=0.0.0.0:9445\n" +
                "      - ORDERER_ADMIN_TLS_ENABLED=true\n" +
                "      - ORDERER_ADMIN_TLS_PRIVATEKEY=/var/hyperledger/orderer/tls/server.key\n" +
                "      - ORDERER_ADMIN_TLS_CERTIFICATE=/var/hyperledger/orderer/tls/server.crt\n" +
                "      - ORDERER_ADMIN_TLS_CLIENTAUTHREQUIRED=true\n" +
                "      - ORDERER_ADMIN_TLS_CLIENTROOTCAS=[/var/hyperledger/orderer/tls/ca.crt]\n" +
                "\n" +
                "      # Channel participation\n" +
                "      - ORDERER_CHANNELPARTICIPATION_ENABLED=true\n" +
                "    working_dir: /opt/gopath/src/github.com/hyperledger/fabric\n" +
                "    command: orderer\n" +
                "    ports:\n" +
                "      - 9445:9445\n" +
                "      - 9050:7050\n" +
                "      - 7082:7082\n" +
                "    volumes:\n" +
                "      - /mnt/c/Users/simo0/OneDrive/Documenti/NetBeansProjects/blockchain/"+mainDirectory+"/organizations/ordererOrganizations/Consenters/orderers/orderer3.example.com/msp:/var/hyperledger/orderer/msp\n" +
                "      - /mnt/c/Users/simo0/OneDrive/Documenti/NetBeansProjects/blockchain/"+mainDirectory+"/organizations/ordererOrganizations/Consenters/orderers/orderer3.example.com/tls:/var/hyperledger/orderer/tls\n" +
                "      - /mnt/c/Users/simo0/OneDrive/Documenti/NetBeansProjects/blockchain/"+mainDirectory+"/organizations/ordererOrganizations/Consenters/orderers/orderer3.example.com/tlsclient:/var/hyperledger/orderer/tlsclient\n" +
                "      - /mnt/c/Users/simo0/OneDrive/Documenti/NetBeansProjects/blockchain/"+mainDirectory+"/bin/genesis_block.pb:/var/hyperledger/orderer/orderer.genesis.block\n" +
                "    networks:\n" +
                "      - fabric_network\n" +
                "\n" +
                "  peer0.org1.example.com:\n" +
                "    container_name: peer0.org1.example.com\n" +
                "    image: hyperledger/fabric-peer:2.5\n" +
                "    environment:\n" +
                "      - CORE_PEER_ID=peer0.org1.example.com\n" +
                "      - CORE_PEER_ADDRESS=peer0.org1.example.com:7051\n" +
                "      - CORE_PEER_LISTENADDRESS=0.0.0.0:7051\n" +
                "      - CORE_PEER_CHAINCODEADDRESS=peer0.org1.example.com:7052\n" +
                "      - CORE_PEER_CHAINCODELISTENADDRESS=0.0.0.0:7052\n" +
                "      - CORE_PEER_GOSSIP_BOOTSTRAP=peer0.org1.example.com:7051\n" +
                "      - CORE_PEER_GOSSIP_EXTERNALENDPOINT=peer0.org1.example.com:7051\n" +
                "      - CORE_PEER_LOCALMSPID=Org1MSP\n" +
                "      - CORE_OPERATIONS_LISTENADDRESS=0.0.0.0:9446\n" +
                "\n" +
                "      # TLS\n" +
                "      - CORE_PEER_TLS_ENABLED=true\n" +
                "      - CORE_PEER_TLS_CERT_FILE=/etc/hyperledger/fabric/tls/server.crt\n" +
                "      - CORE_PEER_TLS_KEY_FILE=/etc/hyperledger/fabric/tls/server.key\n" +
                "      - CORE_PEER_TLS_ROOTCERT_FILE=/etc/hyperledger/fabric/tls/ca.crt\n" +
                "\n" +
                "    working_dir: /opt/gopath/src/github.com/hyperledger/fabric/peer\n" +
                "    command: peer node start\n" +
                "    ports:\n" +
                "      - 7051:7051\n" +
                "      - 9446:9446\n" +
                "    volumes:\n" +
                "      - /mnt/c/Users/simo0/OneDrive/Documenti/NetBeansProjects/blockchain/"+mainDirectory+"/organizations/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/msp:/etc/hyperledger/fabric/msp\n" +
                "      - /mnt/c/Users/simo0/OneDrive/Documenti/NetBeansProjects/blockchain/"+mainDirectory+"/organizations/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls:/etc/hyperledger/fabric/tls\n" +
                "    networks:\n" +
                "      - fabric_network\n";
        
        Files.write(Paths.get(mainDirectory, "docker-compose.yaml"),
            file_content.getBytes(StandardCharsets.UTF_8),
            StandardOpenOption.APPEND);
    }
    
    
    
    /**
     * executeWSLCommand is a method used to execute a command in WSL 
     * @param bashCommand is the command to exec
     */
    protected static void executeWSLCommand(String bashCommand) {
        try {
            // Esegui comando in WSL
            Process process = Runtime.getRuntime().exec(new String[]{"wsl", "bash", "-c", bashCommand});

            // Leggi output standard
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            System.out.println(">>> Comando: " + bashCommand);
            /*while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }*/

            // Leggi eventuali errori
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = errorReader.readLine()) != null) {
                System.err.println("ERR: " + line);
            }

            process.waitFor();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    public static String executeWSLCommandToString(String bashCommand){
        String ris="";
        try {
            // Esegui comando in WSL
            Process process = Runtime.getRuntime().exec(new String[]{"wsl", "bash", "-c", bashCommand});

            // Leggi output standard
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            System.out.println(">>> Comando: " + bashCommand);
            while ((line = reader.readLine()) != null) {
                ris=ris+line;
            }

            // Leggi eventuali errori
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = errorReader.readLine()) != null) {
                System.err.println("ERR: " + line);
            }

            process.waitFor();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return ris;
    }
    
    /**
     * It is used to ensure that the program waits for the specified container to start.
     * @param containerName Name of the container whose startup must be awaited.
     */
    public static void waitForContainer(String containerName){
        boolean ready= false;
        while(!ready){
          String output= executeWSLCommandToString("cd "+mainDirectory+" && docker ps"); 
           if(output !=null && output.contains(containerName)){
               ready=true;
           }else{
              try {
                  Thread.sleep(2000);
              } catch (InterruptedException ex) {
                  Logger.getLogger(FabricOrganizationServerThread.class.getName()).log(Level.SEVERE, null, ex);
              }
           }
        }
    }
    
}
