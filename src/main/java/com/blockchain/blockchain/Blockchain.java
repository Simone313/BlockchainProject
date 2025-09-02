/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Project/Maven2/JavaApp/src/main/java/${packagePath}/${mainClassName}.java to edit this template
 */

package com.blockchain.blockchain;

/**
 *
 * @author simo0
 */
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions;
import java.io.*;
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
    static LinkedList<Integer> ports_used;
    static int inter_port;
    static int server_port;
    static String tls_ca_name;
    static String org_ca_name;
    static String int_ca_name;
    
    public static void main(String[] args) throws IOException {
        mainDirectory="Prova";
        String yourPin="2003";
        createDirectory(mainDirectory);
        setupCA(yourPin);
        
        //metodo per creare il sistema di cartelle delle organizzazioni
        createDirectoryForOrganizations();
        
    }
    
    private static void createDirectory(String name){
        executeWSLCommand("mkdir "+name);
    }
    
    /**
     * 
     * 
     */
    private static void setupCA(String pin) throws IOException{
        Scanner in = new Scanner(System.in);
        if(executeWSLCommandToString("cd "+ mainDirectory +" && which curl").length()==0){
            System.out.println("Installing curl...");
            executeWSLCommand("cd "+ mainDirectory +" && echo "+pin+" | sudo -S apt update && sudo apt install curl");
        }else{
            System.out.println("curl already installed. ");
        }
        
        if(executeWSLCommandToString("cd "+ mainDirectory +" && ls").contains("fabric-ca-client") && executeWSLCommandToString("cd "+ mainDirectory +" && ls").contains(""+fabric_ca_server_name+"")){
            System.out.println("Binaries alrady installed");
        }else{
            System.out.println("Installing Fabric CA server and CA client binaries...");
            executeWSLCommand("cd "+ mainDirectory +" && "
                    + "curl -L -O https://github.com/hyperledger/fabric-ca/releases/download/v1.5.15/hyperledger-fabric-ca-linux-amd64-1.5.15.tar.gz && "
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
            new FabricServerThread(0);
            
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
            new FabricOrganizationServerThread(0);
            waitForContainer(org_ca_name);
            
            
            executeWSLCommand("cd "+Blockchain.mainDirectory+"/fabric-ca-client &&"
                + "./fabric-ca-client enroll -d -u https://"+org_name+":"+org_psw+"@localhost:7055 --tls.certfiles $(pwd)/"+mainDirectory+"/fabric-ca-server-org1/tls/cert.pem --csr.hosts '"+server_list.get(0).getCsrHosts().get(0)+","+server_list.get(0).getCsrHosts().get(1)+"' --mspdir $(pwd)/"+mainDirectory+"/fabric-ca-client/org1-ca/rcaadmin/msp");

            
            if(intermediate){
                deployIntermediateCA();
                new FabricIntermediateServerThread();
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
                + "./fabric-ca-client enroll -d -u https://"+server_list.get(i).getAdmin()+":"+server_list.get(i).getAdminPwd()+"@127.0.0.1:7054 --tls.certfiles $(pwd)/Prova/fabric-ca-client/tls-root-cert/tls-ca-cert.pem --enrollment.profile tls --mspdir "+ mainDirectory +"/fabric-ca-client/tls-ca/tlsadmin/msp");
    }
    
    private static void enrollCAAdmin(int i) {
    String caAdmin = server_list.get(i).getAdmin();
    String caAdminPwd = server_list.get(i).getAdminPwd();
    
    String fabricCaClientDir = mainDirectory + "/fabric-ca-client";
    String clientHome = fabricCaClientDir + "/tls-ca/rcaadmin";
    executeWSLCommand("mkdir -p " + fabricCaClientDir + "/tls-ca/rcaadmin/msp");
    String enrollCmd =
    "cd " + mainDirectory + "/fabric-ca-client && " +
    "export FABRIC_CA_CLIENT_HOME=$(pwd)/tls-ca/rcaadmin && " +
    "./fabric-ca-client enroll -d " +
    "-u https://" + caAdmin + ":" + caAdminPwd + "@127.0.0.1:7054 " +
    "--tls.certfiles $(pwd)/Prova/fabric-ca-client/tls-root-cert/tls-ca-cert.pem"
            + " --mspdir $(pwd)/Prova/fabric-ca-client/tls-ca/tlsadmin/msp";

    //executeWSLCommand("rm -rf $(pwd)/tls-ca/rcaadmin/Prova/fabric-ca-client/tls-ca/rcaadmin/*");
    executeWSLCommand(enrollCmd);
    
    //executeWSLCommand("cp -r $(pwd)/tls-ca/rcaadmin/Prova/fabric-ca-client/tls-ca/rcaadmin/* $(pwd)/Prova/fabric-ca-client/tls-ca/tlsadmin");
    
    }



    
    private static String registerNewIdentity(int i){
        
        Scanner in = new Scanner(System.in);
        System.out.print("Organization CA bootstrap identity name: ");
        org_name= in.next();
        System.out.print("Organization CA bootstrap identity password: ");
        org_psw= in.next();
        executeWSLCommand("cd "+ mainDirectory +"/fabric-ca-client/tls-ca &&"
                + "mkdir users &&"
                + "cd users &&"
                + "mkdir "+org_name+" &&"
                + "cd "+org_name+" &&"
                + "mkdir msp");
        
        executeWSLCommand("cd "+ mainDirectory +"/fabric-ca-client &&"
                + "mkdir org"+(i+1)+"-ca");
        
        executeWSLCommand("cd "+ mainDirectory +"/fabric-ca-client &&"
                    + "export FABRIC_CA_CLIENT_HOME=$(pwd)/tls-ca/rcaadmin &&"
                    + "./fabric-ca-client register -d --id.name "+org_name+" --id.secret "+org_psw+" -u https://127.0.0.1:7054 --tls.certfiles $(pwd)/"+mainDirectory+"/fabric-ca-client/tls-root-cert/tls-ca-cert.pem --mspdir $(pwd)/"+mainDirectory+"/fabric-ca-client/tls-ca/tlsadmin/msp");       

            executeWSLCommand("cd "+ mainDirectory +"/fabric-ca-client &&"
                + "export FABRIC_CA_CLIENT_HOME=$(pwd) &&"
                + "./fabric-ca-client enroll -d -u https://"+org_name+":"+org_psw+"@127.0.0.1:7054 --tls.certfiles $(pwd)/"+mainDirectory+"/fabric-ca-client/tls-root-cert/tls-ca-cert.pem --csr.hosts '"+server_list.get(i).getCsrHosts().get(0)+","+server_list.get(i).getCsrHosts().get(1)+"' --mspdir "+ mainDirectory +"/fabric-ca-client/tls-ca/rcaadmin/msp");
        
        /*if(intermediate){
            
            executeWSLCommand("cd "+ mainDirectory +"/fabric-ca-client &&"
                + "export FABRIC_CA_CLIENT_HOME=$(pwd)/tls-ca/icaadmin &&"
                + "./fabric-ca-client register -d --id.name "+org_name+" --id.secret "+org_psw+" -u https://127.0.0.1:"+inter_port+" --tls.certfiles $(pwd)/"+mainDirectory+"/fabric-ca-server-int-ca/tls/cert.pem --mspdir $(pwd)/"+mainDirectory+"/fabric-ca-client/tls-ca/icaadmin/msp");       

            executeWSLCommand("cd "+ mainDirectory +"/fabric-ca-client &&"
                + "export FABRIC_CA_CLIENT_HOME=$(pwd) &&"
                + "./fabric-ca-client enroll -d -u https://"+org_name+":"+org_psw+"@127.0.0.1:"+inter_port+" --tls.certfiles $(pwd)/"+mainDirectory+"/fabric-ca-server-int-ca/tls/cert.pem --csr.hosts '"+server_list.get(i).getCsrHosts().get(0)+","+server_list.get(i).getCsrHosts().get(1)+"' --mspdir "+ mainDirectory +"/fabric-ca-client/org"+(i+1)+"-ca/rcaadmin/msp");

        }else{
            executeWSLCommand("cd "+ mainDirectory +"/fabric-ca-client &&"
                    + "export FABRIC_CA_CLIENT_HOME=$(pwd)/tls-ca/rcaadmin &&"
                    + "./fabric-ca-client register -d --id.name "+org_name+" --id.secret "+org_psw+" -u https://127.0.0.1:7054 --tls.certfiles $(pwd)/"+mainDirectory+"/fabric-ca-client/tls-root-cert/tls-ca-cert.pem --mspdir $(pwd)/"+mainDirectory+"/fabric-ca-client/tls-ca/tlsadmin/msp");       

            executeWSLCommand("cd "+ mainDirectory +"/fabric-ca-client &&"
                + "export FABRIC_CA_CLIENT_HOME=$(pwd) &&"
                + "./fabric-ca-client enroll -d -u https://"+org_name+":"+org_psw+"@127.0.0.1:7054 --tls.certfiles $(pwd)/"+mainDirectory+"/fabric-ca-client/tls-root-cert/tls-ca-cert.pem --csr.hosts '"+server_list.get(i).getCsrHosts().get(0)+","+server_list.get(i).getCsrHosts().get(1)+"' --mspdir "+ mainDirectory +"/fabric-ca-client/tls-ca/rcaadmin/msp");

        }*/
        
                          
              
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
        
        
        
        executeWSLCommand("cd "+ mainDirectory +"/fabric-ca-client &&"
                + "export FABRIC_CA_CLIENT_HOME=$(pwd)/tls-ca/rcaadmin &&"
                + "./fabric-ca-client register -d --id.name "+icaadmin_name+" --id.secret "+icaadmin_psw+" -u https://127.0.0.1:7054 --tls.certfiles $(pwd)/"+mainDirectory+"/fabric-ca-client/tls-root-cert/tls-ca-cert.pem --mspdir $(pwd)/"+mainDirectory+"/fabric-ca-client/tls-ca/tlsadmin/msp");
        executeWSLCommand("cd "+ mainDirectory +"/fabric-ca-client &&"
                + "export FABRIC_CA_CLIENT_HOME=$(pwd) &&"
                + "./fabric-ca-client enroll -d -u https://"+icaadmin_name+":"+icaadmin_psw+"@127.0.0.1:7054 --tls.certfiles $(pwd)/"+mainDirectory+"/fabric-ca-client/tls-root-cert/tls-ca-cert.pem --csr.hosts '"+server_list.get(i).getCsrHosts().get(0)+","+server_list.get(i).getCsrHosts().get(1)+"' --mspdir "+ mainDirectory +"/fabric-ca-client/tls-ca/icaadmin/msp");
        
        String old_name=executeWSLCommandToString("find $(pwd)/Prova/fabric-ca-client/tls-ca/icaadmin/msp/keystore -name '*_sk'");
        System.out.println("old_name:"+old_name);
        executeWSLCommand("mv "+old_name+" $(pwd)/Prova/fabric-ca-client/tls-ca/icaadmin/msp/keystore/CA_PRIVATE_KEY");
        
        executeWSLCommand("cd "+mainDirectory+" &&"
                + "mkdir -p "+server_name+"/tls &&"
                + "cp bin/fabric-ca-server "+server_name+" &&"
                + "cp $(pwd)/"+mainDirectory+"/fabric-ca-client/tls-ca/icaadmin/msp/signcerts/cert.pem $(pwd)/"+mainDirectory+"/"+server_name+"/tls && cp $(pwd)/"+mainDirectory+"/fabric-ca-client/tls-ca/icaadmin/msp/keystore/CA_PRIVATE_KEY $(pwd)/"+mainDirectory+"/"+server_name+"/tls &&"
                + "cp $(pwd)/"+mainDirectory+"/"+fabric_ca_server_name+"/ca-cert.pem $(pwd)/"+mainDirectory+"/"+server_name+"/tls/tls-ca-cert.pem");
    }
    
    
    private static void createDockerComposeYaml(){
        executeWSLCommand("cd "+mainDirectory+" &&"
                + "cat > $(pwd)/Prova/docker-compose.yaml << 'EOF'\n" +
                "version: '3'\n" +
                "services:\n" +
                "  ca_server:\n" +
                "    image: hyperledger/fabric-ca:1.5\n" +
                "    container_name: "+tls_ca_name+"\n" +
                "    ports:\n" +
                "      - \"7054:7054\"\n" +
                "    environment:\n" +
                "      - FABRIC_CA_HOME=/etc/hyperledger/fabric-ca-server\n" +
                "      - FABRIC_CA_SERVER_CA_NAME=ca-server\n" +
                "      - FABRIC_CA_SERVER_TLS_ENABLED=true\n" +
                "      - FABRIC_CA_SERVER_TLS_CERTFILE=/etc/hyperledger/fabric-ca-server-config/tls-ca-cert.pem\n" +
                "      - FABRIC_CA_SERVER_TLS_KEYFILE=/etc/hyperledger/fabric-ca-server-config/CA_PRIVATE_KEY\n" +
                "    volumes:\n" +
                "      - $(pwd)/"+mainDirectory+"/"+fabric_ca_server_name+"/msp/keystore:/etc/hyperledger/fabric-ca-server-config\n"+
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
            executeWSLCommand("cp "+ mainDirectory +"/"+fabric_ca_server_name+"/ca-cert.pem "+ mainDirectory +"/"+fabric_ca_server_name+"/msp/keystore/tls-ca-cert.pem");
            String old_name=executeWSLCommandToString("find $(pwd)/Prova/"+fabric_ca_server_name+"/msp -name '*_sk'");
            System.out.println("old_name:"+old_name);
            executeWSLCommand("mv "+old_name+" $(pwd)/Prova/"+fabric_ca_server_name+"/msp/keystore/CA_PRIVATE_KEY");
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
            System.out.println("Do you want to enable TLS? s/n");
            String risp=in.next();
            if(risp.equals("n")){
                
            }else{
                Map<String,Object> tls=(Map<String,Object>) data.get("tls");
                tls.put("enabled",true);
                tls.put("certfile", "/etc/hyperledger/fabric-ca-server/tls/cert.pem");
                tls.put("keyfile", "/etc/hyperledger/fabric-ca-server/tls/CA_PRIVATE_KEY");
                //Map<String,Object> tls_clientauth=(Map<String,Object>) tls.get("clientauth");
                
                
                System.out.println("Do you want to activate the mutual TLS option? s/n");
                risp=in.next();
                Map<String,Object> tls_clientauth= (Map<String,Object>) tls.get("clientauth");
                if(risp.equals("n")){
                }else{
                    
                    tls_clientauth.put("type", "RequireAndVerifyClientCert");
                }
                
                tls_clientauth.put("certfiles", "$(pwd)/Prova/fabric-ca-server-tls/msp/keystore/tls-ca-cert.pem");
            }
            
            //CAFILES  
            System.out.println("Do you want to configure a dual-headed CA? s/n");
            risp=in.next();
            if(risp.equals("n")){
            }else{
                //  TODO creare un organization ca ed inserire il percorso che porta al suo fabric-caserver-config.yaml
            }
            
            //INTERMEDIATE CA
            System.out.println("Is this an intermediate CA? s/n");
            risp=in.next();
            if(risp.equals("n")){
            }else{
                intermediate=true;
                Map<String,Object> intermediate=(Map<String,Object>) data.get("intermediate");
                Map<String,Object> intermediate_parentserver=(Map<String,Object>) intermediate.get("parentserver");
                System.out.print("Parentserver url: ");
                String url=in.next();
                System.out.print("Parentserver caname: ");
                String caname=in.next();
                intermediate_parentserver.put("url", url);
                intermediate_parentserver.put("caname", caname);
                
                Map<String,Object> intermediate_enrollment=(Map<String,Object>) intermediate.get("enrollment");
                intermediate_enrollment.put("hosts",tls_ca_name);
                intermediate_enrollment.put("profile", "tls");
                intermediate_enrollment.put("label", "tls-ca-intermediate");
                
                //TODO una volta generati i certificati del TLS riportarli nella sezione intermediate
            }
            
            //PORT
            ports_used=new LinkedList<Integer>();
            boolean k=true;
            while(k){
                System.out.print("Enter the server port (es. 7054): ");
                server_port=in.nextInt();
                if(!ports_used.contains(server_port)){
                    k=false;
                }else{
                    System.out.println("Port already in use, try another one.");
                }
            }
            
            ports_used.add(server_port);
            data.put("port", server_port);
            
            //DB
            System.out.println("Which type of database do you want to use?");
            System.out.println("1- SQLite Version 3");
            System.out.println("2- PostgresSQL");
            System.out.println("3- MySQL");
            int risDB=in.nextInt();
            Map<String,Object> db=(Map<String,Object>) data.get("db");
            switch(risDB){
                case 1 -> {
                    db.put("type", "sqlite3");
                }
                case 2 -> {
                    db.put("type", "postgres");
                }
                case 3 -> {
                    db.put("type", "mysql");
                }
            }
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
        
                /*+ "cp "+mainDirectory+"/fabric-ca-client/tls-ca/users/"+user_name+"/msp/signcerts/cert.pem tls && cp "+mainDirectory+"/fabric-ca-client/tls-ca/rcaadmin/msp/keystore/key.pem tls"*/
        
        executeWSLCommand("cd "+mainDirectory+"/"+server_name+" &&"
                + "./fabric-ca-server init -b "+org_name+":"+org_psw);
        String old_name=executeWSLCommandToString("find $(pwd)/Prova/fabric-ca-client/tls-ca/rcaadmin/msp -name '*_sk'");
            System.out.println("old_name:"+old_name);
            executeWSLCommand("mv "+old_name+" $(pwd)/Prova/fabric-ca-client/tls-ca/rcaadmin/msp/keystore/ORG1_PRIVATE_KEY");
        executeWSLCommand("cp $(pwd)/"+mainDirectory+"/fabric-ca-client/tls-ca/rcaadmin/msp/signcerts/cert.pem $(pwd)/"+mainDirectory+"/fabric-ca-server-org1/tls && cp $(pwd)/"+mainDirectory+"/fabric-ca-client/tls-ca/rcaadmin/msp/keystore/ORG1_PRIVATE_KEY $(pwd)/"+mainDirectory+"/fabric-ca-server-org1/tls ");
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
            System.out.println("Do you want to enable TLS? s/n");
            String risp=in.next();
            boolean tlsEn=false;
            if(risp.equals("n")){
                
            }else{
                tlsEn=true;
                Map<String,Object> tls=(Map<String,Object>) data.get("tls");
                tls.put("enabled",true);
                
                tls.put("certfile", "fabric-ca-client/tls-ca/rcaadmin/msp/signcerts/cert.pem");
                tls.put("keyfile", "fabric-ca-client/tls-ca/rcaadmin/msp/keystore");
                
                System.out.println("Do you want to activate the mutual TLS option? s/n");
                risp=in.next();
                if(risp.equals("n")){
                }else{
                    Map<String,Object> tls_clientauth= (Map<String,Object>) tls.get("clientauth");
                    tls_clientauth.put("type", "RequireAndVerifyClientCert");
                }
            }
            
            //CAFILES  
            System.out.println("Do you want to configure a dual-headed CA? s/n");
            risp=in.next();
            if(risp.equals("n")){
            }else{
                //  TODO creare un organization ca ed inserire il percorso che porta al suo fabric-caserver-config.yaml
            }
            
            //INTERMEDIATE CA
            System.out.println("Is this an intermediate CA? s/n");
            risp=in.next();
            if(risp.equals("n")){
            }else{
                intermediate=true;
                Map<String,Object> intermediate=(Map<String,Object>) data.get("intermediate");
                Map<String,Object> intermediate_parentserver=(Map<String,Object>) intermediate.get("parentserver");
                System.out.print("Parentserver url: ");
                String url=in.next();
                System.out.print("Parentserver caname: ");
                String caname=in.next();
                intermediate_parentserver.put("url", url);
                intermediate_parentserver.put("caname", caname);
                
                Map<String,Object> intermediate_enrollment=(Map<String,Object>) intermediate.get("enrollment");
                intermediate_enrollment.put("hosts",org_ca_name);
                intermediate_enrollment.put("profile", "tls");
                intermediate_enrollment.put("label", "tls-ca-intermediate");
                
                //TODO una volta generati i certificati del TLS riportarli nella sezione intermediate
            }
            
            //PORT
            int server_port = 0;
            boolean k=true;
            while(k){
                System.out.print("Enter the server port (es. 7055): ");
                server_port=in.nextInt();
                if(!ports_used.contains(server_port)){
                    k=false;
                }else{
                    System.out.println("Port already in use, try another one.");
                }
            }
            
            ports_used.add(server_port);
            data.put("port", server_port);
            
            //DB
            System.out.println("Which type of database do you want to use?");
            System.out.println("1- SQLite Version 3");
            System.out.println("2- PostgresSQL");
            System.out.println("3- MySQL");
            int risDB=in.nextInt();
            Map<String,Object> db=(Map<String,Object>) data.get("db");
            switch(risDB){
                case 1 -> {
                    db.put("type", "sqlite3");
                }
                case 2 -> {
                    db.put("type", "postgres");
                }
                case 3 -> {
                    db.put("type", "mysql");
                }
            }
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
            
            addCAtoDocker(org_ca_name, tlsEn, server_port, server_name, false);
            
    }
    
    
    private static void addCAtoDocker(String name, boolean tls, int port, String serverName, boolean inter) throws FileNotFoundException, IOException{
        Yaml yaml= new Yaml();
        File file= new File(""+ mainDirectory +"/docker-compose.yaml");
        Map<String, Object> data = yaml.load(new FileInputStream(file));
        Map<String, Object> services = (Map<String, Object>) data.get("services");
       
        
        //add organization CA service
        Map<String, Object> caOrg1 = new LinkedHashMap<>();
        caOrg1.put("image", "hyperledger/fabric-ca");
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
               

        String old_name=executeWSLCommandToString("find $(pwd)/Prova/fabric-ca-server-int-ca/msp -name '*_sk'");
        System.out.println("old_name:"+old_name);
        executeWSLCommand("mv "+old_name+" $(pwd)/Prova/fabric-ca-server-int-ca/msp/keystore/CA_PRIVATE_KEY");
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
            System.out.println("Do you want to enable TLS? s/n");
            String risp=in.next();
            boolean tlsEn=false;
            if(risp.equals("n")){
                
            }else{
                tlsEn=true;
                Map<String,Object> tls=(Map<String,Object>) data.get("tls");
                tls.put("enabled",true);
                
                tls.put("certfile", "fabric-ca-client/tls-ca/rcaadmin/msp/signcerts/cert.pem");
                tls.put("keyfile", "fabric-ca-client/tls-ca/rcaadmin/msp/keystore");
                
                System.out.println("Do you want to activate the mutual TLS option? s/n");
                risp=in.next();
                if(risp.equals("n")){
                }else{
                    Map<String,Object> tls_clientauth= (Map<String,Object>) tls.get("clientauth");
                    tls_clientauth.put("type", "RequireAndVerifyClientCert");
                }
                
                
            }
            
            //CAFILES  
            System.out.println("Do you want to configure a dual-headed CA? s/n");
            risp=in.next();
            if(risp.equals("n")){
            }else{
                //  TODO creare un organization ca ed inserire il percorso che porta al suo fabric-caserver-config.yaml
            }
            
            //INTERMEDIATE CA
            System.out.println("Is this an intermediate CA? s/n");
            risp=in.next();
            if(risp.equals("n")){
            }else{
                intermediate=true;
                Map<String,Object> intermediate=(Map<String,Object>) data.get("intermediate");
                Map<String,Object> intermediate_parentserver=(Map<String,Object>) intermediate.get("parentserver");
                System.out.print("Parentserver url: ");
                String url=in.next();
                System.out.print("Parentserver caname: ");
                String caname=in.next();
                intermediate_parentserver.put("url", url);
                intermediate_parentserver.put("caname", caname);
                
                Map<String,Object> intermediate_enrollment=(Map<String,Object>) intermediate.get("enrollment");
                intermediate_enrollment.put("hosts",int_ca_name);
                intermediate_enrollment.put("profile", "tls");
                intermediate_enrollment.put("label", "tls-ca-intermediate");
                
                //TODO una volta generati i certificati del TLS riportarli nella sezione intermediate
            }
            
            //PORT
            boolean k=true;
            while(k){
                System.out.print("Enter the server port (es. 7056): ");
                inter_port=in.nextInt();
                if(!ports_used.contains(inter_port)){
                    k=false;
                }else{
                    System.out.println("Port already in use, try another one.");
                }
            }
            
            ports_used.add(inter_port);
            data.put("port", inter_port);
            
            //DB
            System.out.println("Which type of database do you want to use?");
            System.out.println("1- SQLite Version 3");
            System.out.println("2- PostgresSQL");
            System.out.println("3- MySQL");
            int risDB=in.nextInt();
            Map<String,Object> db=(Map<String,Object>) data.get("db");
            switch(risDB){
                case 1 -> {
                    db.put("type", "sqlite3");
                }
                case 2 -> {
                    db.put("type", "postgres");
                }
                case 3 -> {
                    db.put("type", "mysql");
                }
            }
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
            addCAtoDocker(int_ca_name, tlsEn, inter_port, server_name, true);
    }
    
    
    private static void insertNewIdentities() throws IOException{
        Scanner in= new Scanner(System.in);
        System.out.println("How many organizations?");
        int numOrgs=in.nextInt();
        
        System.out.println("How many peers per organization?");
        int peersPerOrg=in.nextInt();
        
        System.out.println("How many orderers?");
        int numOrderer=in.nextInt();
        
        Map<String, Object> dockerCompose = new LinkedHashMap<>();

        // Setup iniziale
        dockerCompose.put("version", "2");
        Map<String, Object> services = new LinkedHashMap<>();
        
        // Aggiungi i peer
        int peerPort = 7051;
        for (int org = 1; org <= numOrgs; org++) {
            for (int peer = 0; peer < peersPerOrg; peer++) {
                String peerName = "peer" + peer + ".org" + org + ".example.com";
                Map<String, Object> peerConfig = new LinkedHashMap<>();
                peerConfig.put("image", "hyperledger/fabric-peer:latest");
                peerConfig.put("ports", List.of(peerPort + ":7051"));
                peerConfig.put("networks", List.of("fabric_network"));
                services.put(peerName, peerConfig);
                boolean k=true;
                while(k){
                    peerPort += 1000;
                    if(!ports_used.contains(peerPort)){
                        k=false;
                    }
                }
                peerPort += 1000; // es: 7051, 8051, 9051, ecc.
            }
        }

        // Aggiungi orderer
        for (int i = 0; i < numOrderer; i++) {
            String ordererName = "orderer" + i + ".example.com";
            Map<String, Object> ordererConfig = new LinkedHashMap<>();
            ordererConfig.put("image", "hyperledger/fabric-orderer:latest");
            ordererConfig.put("ports", List.of((7050 + i) + ":7050"));
            ordererConfig.put("networks", List.of("fabric_network"));
            services.put(ordererName, ordererConfig);
        }

        // Metti tutti i servizi nella sezione "services"
        dockerCompose.put("services", services);

        // Definisci la rete
        dockerCompose.put("networks", Map.of("fabric_network", new LinkedHashMap<>()));

        // Scrivi il file YAML
        Yaml yaml = new Yaml();
        FileWriter writer = new FileWriter("docker-compose.yaml");
        yaml.dump(dockerCompose, writer);
        writer.close();

        System.out.println("docker-compose.yaml created!");
    }
    
    
    
    
    
    
    private static void createDirectoryForOrganizations() throws IOException{
        Scanner in= new Scanner(System.in);
        executeWSLCommand("cd "+mainDirectory+" &&"
                + "mkdir organizations &&"
                + "cd organizations &&"
                + "mkdir fabric-ca ordererOrganizations peerOrganizations");
        boolean k=true;
        executeWSLCommand("cd "+mainDirectory+" &&"
                + "mkdir peers_bin");
        executeWSLCommand("cd "+mainDirectory+" &&"
                + "mkdir orderers_bin");
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
                        download_orderer_bin(orderers_names.get(i));
                    }
                    break;
                }
                case 2:{
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
                        executeWSLCommand("cd "+mainDirectory+"/organizations/peerOrganizations/"+organization_name+"/peers &&"
                                + "mkdir "+peer_name+" &&"
                                + "cd "+peer_name+" &&"
                                + "mkdir msp tls");
                    }
                    organizationAdminRegistrationEnroll(organization_name, true);
                    createConfig_yaml("organizations/peerOrganizations/"+organization_name+"/msp");
                    
                    for(int i=0;i<num_peer;i++){
                        createLocalMsp(organization_name,peers_names.get(i),true);
                        download_peer_bin(peers_names.get(i));
                        configure_peer_core(peers_names.get(i), organization_name);
                        
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
    
    public static void download_orderer_bin(String orderer_name){
        //download dei bin
        executeWSLCommand("cd "+mainDirectory+"/orderers_bin &&"
                + "mkdir "+orderer_name);
        executeWSLCommand("cd "+mainDirectory+"/ordererss_bin/"+orderer_name+" &&"
                + "curl -L -O https://github.com/hyperledger/fabric/releases/download/v3.1.1/hyperledger-fabric-linux-amd64-3.1.1.tar.gz &&"
                + "tar -xvzf hyperledger-fabric-linux-amd64-3.1.1.tar.gz &&"
                + "rm hyperledger-fabric-linux-amd64-3.1.1.tar.gz");

    }
    
    public static void download_peer_bin(String peer_name){
        //download dei bin
        executeWSLCommand("cd "+mainDirectory+"/peers_bin &&"
                + "mkdir "+peer_name);
        executeWSLCommand("cd "+mainDirectory+"/peers_bin/"+peer_name+" &&"
                + "curl -L -O https://github.com/hyperledger/fabric/releases/download/v3.1.1/hyperledger-fabric-linux-amd64-3.1.1.tar.gz &&"
                + "tar -xvzf hyperledger-fabric-linux-amd64-3.1.1.tar.gz &&"
                + "rm hyperledger-fabric-linux-amd64-3.1.1.tar.gz");
    }
    
    public static void configure_peer_core(String peer_name, String org_name) throws FileNotFoundException, IOException{
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
        System.out.print("networkId (es. dev, test, production ...): ");
        String risp= in.next();
        peer.put("networkId", "dev");
        
        //LISTEN ADDRESS   
        System.out.print("Listen address (es. 0.0.0.0)");
        String add=in.next();
        int port;
        do{
            System.out.print("Port (es. 7051): ");
            port=in.nextInt();
            System.out.println((ports_used.contains(port))? "Port already in use, try another one":"");
        }while(ports_used.contains(port));
        
        peer.put("listenAddress",add+":"+port);
        
        //ADDRESS
        peer.put("address",peer_name+":"+port);
        
        //CHAINCODE LISTEN ADDRESS
        System.out.println("Chaincode listen address (es. 0.0.0.0)");
        add=in.next();
        int portC;
        do{
            System.out.print("Port (es. 7052): ");
            portC=in.nextInt();
            System.out.println((ports_used.contains(portC))? "Port already in use, try another one":"");
        }while(ports_used.contains(portC));
        peer.put("chaincodeListenAddress",add+":"+portC);
        
        //CHAINCODE ADDRESS
        peer.put("chaincodeAddress", peer_name+":"+portC);
        
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
        System.out.print("Name of a bootstrap peer (different from " + peer_name + ") in the same organization (e.g., p0): ");
        String bootstrapPeer = in.next();
        gossip.put("bootstrap", bootstrapPeer + "." + org_name + ":7051");

        // Endpoint interno
        String peerEndpoint = peer_name +":7051";
        gossip.put("endpoint", peerEndpoint);

        // Endpoint esterno
        gossip.put("externalEndpoint", peerEndpoint);

        // Leader Election
        System.out.print("Should the peer use automatic leader election? (true/false): ");
        boolean useLeaderElection = in.nextBoolean();
        gossip.put("useLeaderElection", useLeaderElection);

        // Org leader
        if (!useLeaderElection) {
            System.out.print("Will the peer be an org leader? (true/false):  ");
            boolean isOrgLeader = in.nextBoolean();
            gossip.put("orgLeader", isOrgLeader);
        }

        // Gossip state transfer
        Map<String, Object> state = new LinkedHashMap<>();
        System.out.print("Enable gossip state transfer? (true/false):  ");
        boolean stateEnabled = in.nextBoolean();
        state.put("enabled", stateEnabled);
        gossip.put("state", state);

        // pvtData.implicitCollectionDisseminationPolicy
        Map<String, Object> pvtData = new LinkedHashMap<>();
        Map<String, Object> implicitPolicy = new LinkedHashMap<>();

        System.out.print("requiredPeerCount (Minimum peers for private data dissemination): ");
        int requiredPeerCount = in.nextInt();
        implicitPolicy.put("requiredPeerCount", requiredPeerCount);

        System.out.print("maxPeerCount (Maximum peers for private data dissemination): ");
        int maxPeerCount = in.nextInt();
        implicitPolicy.put("maxPeerCount", maxPeerCount);

        pvtData.put("implicitCollectionDisseminationPolicy", implicitPolicy);
        gossip.put("pvtData", pvtData);
        
        //TLS
        Map<String, Object> tls= (Map<String, Object>) peer.get("tls");
        
        // Abilita TLS
        System.out.print("Enable TLS (true/false): ");
        boolean tlsEnabled = in.nextBoolean();
        tls.put("enabled", tlsEnabled);

        // Abilita mutual TLS (clientAuthRequired)
        System.out.print("Mutual TLS required (clientAuthRequired)? (true/false): ");
        boolean clientAuthRequired = in.nextBoolean();
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
        System.out.print("Select BCCSP provider (SW or PKCS11): ");
        String defaultProvider = in.next().toUpperCase();
        bccsp.put("Default", defaultProvider);

        // Configuration for SW (Software Crypto)
        if (defaultProvider.equals("SW")) {
            Map<String, Object> sw = new LinkedHashMap<>();
            sw.put("Hash", "SHA2");
            sw.put("Security", 256);

            Map<String, Object> fileKeyStore = new LinkedHashMap<>();

            // Optional FileKeyStore path
            System.out.print("Enter FileKeyStore path (leave empty to use default): ");
            in.nextLine(); // consume newline
            String fileKeyStorePath = in.nextLine().trim();
            fileKeyStore.put("KeyStore", fileKeyStorePath);

            sw.put("FileKeyStore", fileKeyStore);
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
        System.out.print("Choose state database (goleveldb or CouchDB): ");
        String stateDb = in.next().trim();
        
        
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
        System.out.print("Enter the path to store ledger snapshots (e.g., /var/hyperledger/production/snapshots): ");
        String snapshotDir = in.nextLine().trim();
        snapshots.put("rootDir", snapshotDir);

        ledger.put("snapshots", snapshots);
        
        //OPERATIONS
        Map<String, Object> operations=(Map<String, Object>) data.get("operations");
        
        // Operations server listen address
        System.out.print("Enter operations server address (e.g., 127.0.0.1): ");
        String opAddress = in.nextLine().trim();
        int opPort;
        do {
            System.out.print("Enter operations server port (e.g., 9443): ");
            opPort = in.nextInt();
            in.nextLine(); // consume newline
            if (ports_used.contains(opPort)) {
                System.out.println("Port already in use, please choose another one.");
            }
        } while (ports_used.contains(opPort));
        operations.put("listenAddress", opAddress + ":" + opPort);

        // TLS configuration
        Map<String, Object> operations_tls = new LinkedHashMap<>();

        System.out.print("Enable TLS for operations endpoint? (true/false): ");
        boolean operations_tlsEnabled = in.nextBoolean();
        operations_tls.put("enabled", operations_tlsEnabled);
        in.nextLine(); // consume newline

        if (operations_tlsEnabled) {
            Map<String, Object> operations_cert = new LinkedHashMap<>();
            Map<String, Object> operations_key = new LinkedHashMap<>();

            System.out.print("Enter path to TLS cert file (e.g., tls/server.crt): ");
            operations_cert.put("file", in.nextLine().trim());
            operations_tls.put("cert", operations_cert);

            System.out.print("Enter path to TLS key file (e.g., tls/server.key): ");
            operations_key.put("file", in.nextLine().trim());
            operations_tls.put("key", operations_key);

            System.out.print("Require client authentication? (true/false): ");
            boolean operations_clientAuthRequired = in.nextBoolean();
            operations_tls.put("clientAuthRequired", operations_clientAuthRequired);
            in.nextLine(); // consume newline

            List<String> caFiles = new ArrayList<>();
            if (operations_clientAuthRequired) {
                System.out.print("Enter path to client root CA file (e.g., tls/ca.crt): ");
                String caPath = in.nextLine().trim();
                caFiles.add(caPath);
            }

            Map<String, Object> clientRootCAs = new LinkedHashMap<>();
            clientRootCAs.put("files", caFiles);
            operations_tls.put("clientRootCAs", clientRootCAs);
        }

        operations.put("tls", operations_tls);
        
        Map<String, Object> metrics= (Map<String, Object>)data.get("metrics");
        System.out.print("Enter metrics provider (disabled, prometheus, statsd): ");
        String provider;
        while (true) {
            provider = in.nextLine().trim().toLowerCase();
            if (provider.equals("disabled") || provider.equals("prometheus") || provider.equals("statsd")) {
                break;
            }
            System.out.print("Invalid provider. Please enter one of: disabled, prometheus, statsd: ");
        }
        metrics.put("provider", provider);

        if (provider.equals("statsd")) {
            Map<String, Object> statsd = new LinkedHashMap<>();

            System.out.print("Enter network type (tcp/udp): ");
            String network;
            while (true) {
                network = in.nextLine().trim().toLowerCase();
                if (network.equals("tcp") || network.equals("udp")) break;
                System.out.print("Invalid network type. Enter tcp or udp: ");
            }
            statsd.put("network", network);

            System.out.print("Enter statsd server address (e.g., 127.0.0.1:8125): ");
            statsd.put("address", in.nextLine().trim());

            metrics.put("statsd", statsd);
        }
        
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
        //Aggiunta del peer al file docker-compose.yaml
        add_peer_to_docker(peer_name,cfgPath,mspPath,tlsPath,ports, cDB);
        
        System.out.println("Starting the peer...");
        new peerThread(peer_name);
        
        waitForContainer(peer_name);
        
    }
    
    
    public static void configure_orderer(String orderer_name, String org_name) throws FileNotFoundException, IOException{
        Scanner in= new Scanner(System.in);
        System.out.println("------------ ORDERER "+orderer_name+"."+org_name+" CONFIGURATION ------------");
        File orderer_config=new File(""+ mainDirectory +"/orderers_bin/"+orderer_name+"/config/orderer.yaml");
        Yaml yaml= new Yaml();
        Map<String, Object> data= yaml.load(new FileReader(orderer_config));
        Map<String, Object> general= (Map<String, Object>) data.get("general");
        
        // LISTEN ADDRESS & PORT
        System.out.println("Listen address (The IP on which to bind to listen)");
        String add=in.next();
        general.put("ListenAddress", add);
        
        System.out.println("Listen port (The port on which to bind to listen, es 7050)");
        int port;
        do{
            port=in.nextInt();
            System.out.println((ports_used.contains(port))? "Port already in use, try another one":"");
        }while(ports_used.contains(port));
        
        general.put("ListenPort", port);
        
        //TLS
        Map<String, Object> general_tls=(Map<String,Object>)general.get("tls");
        System.out.println("Enable TLS? (y/n)");
        if(in.next().equals("y")){
            general_tls.put("Enabled", true);
            general_tls.put("PrivateKey", "/etc/hyperledger/fabric/tls/key");
            
            general_tls.put("Certificate", "/etc/hyperledger/fabric/tls/cert.pem"); 
            
            System.out.println("Enable mutual TLS?(y/n)");
            if(in.next().equals("y")){
                general_tls.put("ClientAuthRequired", true);
                List<String> clientRootCAs = new ArrayList<>();
                
                
                clientRootCAs.add("/etc/hyperledger/fabric/tls/tls-ca-cert.pem");

                general_tls.put("ClientRootCAs", clientRootCAs);
            }
        }
        
        
        // CLUSTER
        Map<String, Object> general_cluster = (Map<String, Object>) general.get("Cluster");

        general_cluster.put("SendBufferSize", 100);

        System.out.println("Use a separate listener for intra-cluster communication? (y/n)");
        if (in.next().equals("y")) {

            System.out.println("Insert the IP address to listen for intra-cluster communication (e.g., 127.0.0.1):");
            String clusterAddress = in.next();
            general_cluster.put("ListenAddress", clusterAddress);

            System.out.println("Insert the port to listen for intra-cluster communication (e.g., 9444):");
            int clusterPort = in.nextInt();
            general_cluster.put("ListenPort", clusterPort);

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

        
        
        //BOOTSTRAP METHOD
        System.out.println("Do you want to create this node on a network that is not using a system channel?(y/n)");
        boolean k=in.next().equals("y");
         if (k) {
             general.put("BoostrapMethod","none");
             //BoostrapFile
             System.out.println("insert the location and name fo the system channel genesis block");
             general.put("BoostrapFile",in.next());
         }
        
        //LOCAL MSP DIR
        general.put("LocalMSPDir", "$(pwd)/"+mainDirectory+"/organizations/ordererOrganizations/"+org_name+"/orderers/"+orderer_name+"/msp");
        //LOCAL MSP ID
        general.put("LocalMSPID","SampleOrg");
        // BCCSP
        Map<String, Object> general_bccsp = (Map<String, Object>) general.get("BCCSP");

        System.out.println("Which crypto provider do you want to use? (SW/PKCS11)");
        String provider = in.next().toUpperCase();

        general_bccsp.put("Default", provider);

        if (provider.equals("SW")) {
            Map<String, Object> sw = new HashMap<>();
            sw.put("Hash", "SHA2");
            sw.put("Security", 256);

            Map<String, Object> fileKeyStore = new HashMap<>();
            fileKeyStore.put("KeyStore", "$(pwd)/" + mainDirectory + "/organizations/ordererOrganizations/" + org_name + "/orderers/" + orderer_name + "/msp/keystore");
            sw.put("FileKeyStore", fileKeyStore);

            general_bccsp.put("SW", sw);

        } else if (provider.equals("PKCS11")) {
            Map<String, Object> pkcs11 = new HashMap<>();

            System.out.println("Enter PKCS11 library path:");
            pkcs11.put("Library", in.next());

            System.out.println("Enter PKCS11 token label:");
            pkcs11.put("Label", in.next());

            System.out.println("Enter PKCS11 user PIN:");
            pkcs11.put("Pin", in.next());

            pkcs11.put("Hash", "SHA2");
            pkcs11.put("Security", 256);

            Map<String, Object> fileKeyStore = new HashMap<>();
            fileKeyStore.put("KeyStore", "$(pwd)/" + mainDirectory + "/organizations/ordererOrganizations/" + org_name + "/orderers/" + orderer_name + "/msp/keystore");
            pkcs11.put("FileKeyStore", fileKeyStore);

            general_bccsp.put("PKCS11", pkcs11);
        } else {
            System.out.println("Invalid provider. Defaulting to SW.");
            general_bccsp.put("Default", "SW");
        }

        //FILE LEDGER
        Map<String, Object> fileLedger = (Map<String, Object>) data.get("FileLedger");
        String ledgerPath = "$(pwd)/" + mainDirectory + "/organizations/ordererOrganizations/" + org_name + "/orderers/" + orderer_name + "/fileLedger";
        fileLedger.put("Location", ledgerPath);
        
        //OPERATIONS
        System.out.println("Do you want to use the operations service?(y/n)");
        if (in.next().equals("y")) {
            Map<String, Object> operations =(Map<String, Object>) data.get("Operations");
            System.out.print("Address of the operations server: ");
            String op_server_add=in.next();
            System.out.print("Port of the operations server: ");
            int op_server_port=in.nextInt();
            operations.put("ListenAddress", op_server_add+":"+op_server_port);
            
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
        
        
        //METRICS
        Map<String, Object> metrics = (Map<String, Object>) data.get("Metrics");

        System.out.println("Select metrics provider (prometheus / statsd / disabled):");
        provider = in.next().toLowerCase();

        if (!provider.equals("prometheus") && !provider.equals("statsd") && !provider.equals("disabled")) {
            System.out.println("Invalid provider. Defaulting to 'disabled'.");
            provider = "disabled";
        }

        metrics.put("Provider", provider);

        if (provider.equals("statsd")) {
            Map<String, Object> statsd = new LinkedHashMap<>();

            System.out.println("Enter StatsD network type (tcp/udp):");
            statsd.put("Network", in.next());

            System.out.println("Enter StatsD server address (e.g. 127.0.0.1:8125):");
            statsd.put("Address", in.next());

            System.out.println("Enter write interval (e.g. 30s):");
            statsd.put("WriteInterval", in.next());

            System.out.println("Enter metrics prefix:");
            statsd.put("Prefix", in.next());

            metrics.put("Statsd", statsd);
        }
        
        //ADMIN
        Map<String, Object> admin =(Map<String, Object>) data.get("Admin");
        Map<String, Object> admin_TLS=(Map<String, Object>) admin.get("TLS");
        admin_TLS.put("Enabled", true);
        admin_TLS.put("Certificate", "$(pwd)/"+mainDirectory+"/organizations/ordererOrganizations/"+org_name+"/orderers/"+orderer_name+"/msp/keystore/"); //TODO Scrivere nome del certificato
        admin_TLS.put("PrivateKey", "$(pwd)/"+mainDirectory+"/organizations/ordererOrganizations/"+org_name+"/orderers/"+orderer_name+"/msp/keystore/server.key");
        admin_TLS.put("ClientAuthRequired", true);
        List<String> clientRootCAs = new ArrayList<>();

        
        clientRootCAs.add("/etc/hyperledger/fabric/tls/tls-ca-cert.pem");

        admin_TLS.put("ClientRootCAs", clientRootCAs);
        //CHANNEL PARTECIPATION
        Map<String, Object> channelParticipation = (Map<String, Object>) data.get("ChannelParticipation");
        System.out.println("Enable Channel Participation API? (y/n)");
        String enableInput = in.next().toLowerCase();
        boolean enabled = enableInput.equals("y");
        channelParticipation.put("Enabled", enabled);
        channelParticipation.put("MaxRequestBodySize", "1 MB");
        //CONSENSUS
        Map<String, Object> consensus = (Map<String, Object>) data.get("Consensus");

        System.out.println("Set custom WALDir path? (default is /var/hyperledger/production/orderer/etcdraft/wal) (y/n)");
        if (in.next().toLowerCase().equals("y")) {
            System.out.print("Enter WALDir path: ");
            String walDir = in.next();
            consensus.put("WALDir", walDir);
        } else {
            consensus.put("WALDir", "/var/hyperledger/production/orderer/etcdraft/wal");
        }

        System.out.println("Set custom SnapDir path? (default is /var/hyperledger/production/orderer/etcdraft/snapshot) (y/n)");
        if (in.next().toLowerCase().equals("y")) {
            System.out.print("Enter SnapDir path: ");
            String snapDir = in.next();
            consensus.put("SnapDir", snapDir);
        } else {
            consensus.put("SnapDir", "/var/hyperledger/production/orderer/etcdraft/snapshot");
        }
        //Aggiunta dell'orderer al file docker-compose.yaml
        String path=executeWSLCommandToString("echo $(pwd)");
        String mspPath=path+"/"+mainDirectory+"/organizations/ordererOrganizations/"+org_name+"/orderers/"+orderer_name+"/msp";
        String cfgPath=path+"/"+mainDirectory+"/orderers_bin/"+orderer_name+"/config";
        String tlsPath=path+"/"+mainDirectory+"/organizations/ordererOrganizations/"+org_name+"/orderers/"+orderer_name+"/tls";
        LinkedList<Integer> ports= new LinkedList<Integer>();
        ports.add(port);
        add_orderer_to_docker(orderer_name,cfgPath,mspPath,tlsPath,"SampleOrg",ports);
        
        //Avvio del container
        new ordererThread();
        
        //Aspetto che il container si avvi
        waitForContainer(orderer_name);
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
        System.out.println("---------- ORGANIZATION ADMIN REGISTRATION ----------");
        Scanner in= new Scanner(System.in);
        System.out.print("Name: ");
        String name=in.next();
        System.out.print("Password: ");
        String psw= in.next();
        
        String org_directory= peer_org? "peerOrganizations":"ordererOrganizations";
        
        executeWSLCommand("cd "+ mainDirectory +"/fabric-ca-client &&"
                    + "export FABRIC_CA_CLIENT_HOME=$(pwd)/tls-ca/rcaadmin &&"
                    + "./fabric-ca-client register -d --id.name "+name+" --id.secret "+psw+" -u https://localhost:7055 --tls.certfiles $(pwd)/"+mainDirectory+"/fabric-ca-server-org1/tls/cert.pem --mspdir $(pwd)/"+mainDirectory+"/fabric-ca-client/org1-ca/rcaadmin/msp --id.type admin");       

        executeWSLCommand("cd "+mainDirectory+"/fabric-ca-client &&"
                + "./fabric-ca-client enroll -u https://"+name+":"+psw+"@localhost:7055 --mspdir $(pwd)/"+mainDirectory+"/organizations/"+org_directory+"/"+org_name+"/msp --csr.hosts 'host1' --tls.certfiles $(pwd)/"+mainDirectory+"/fabric-ca-server-org1/tls/cert.pem");
    }
    
    private static void createLocalMsp(String org_name, String node_name, boolean peer_org){
        String org_directory= peer_org? "peerOrganizations/"+org_name+"/peers/"+node_name+"/":"ordererOrganizations/"+org_name+"/orderers/"+node_name+"/";
         executeWSLCommand("cd "+ mainDirectory +"/fabric-ca-client &&"
                    + "export FABRIC_CA_CLIENT_HOME=$(pwd)/tls-ca/rcaadmin &&"
                    + "./fabric-ca-client register -d --id.name "+node_name+" --id.secret "+node_name+"_PSW -u https://localhost:7055 --tls.certfiles $(pwd)/"+mainDirectory+"/fabric-ca-server-org1/tls/cert.pem --mspdir $(pwd)/"+mainDirectory+"/fabric-ca-client/org1-ca/rcaadmin/msp --id.type admin");       

        executeWSLCommand("cd "+mainDirectory+"/fabric-ca-client &&"
                + "./fabric-ca-client enroll -u https://"+node_name+":"+node_name+"_PSW@localhost:7055 --mspdir $(pwd)/"+mainDirectory+"/organizations/"+org_directory+"msp --csr.hosts 'host1' --tls.certfiles $(pwd)/"+mainDirectory+"/fabric-ca-server-org1/tls/cert.pem");
        executeWSLCommand("mkdir "+mainDirectory+"/organizations/"+org_directory+"msp/admincerts &&"
                + "cp "+mainDirectory+"/organizations/"+(peer_org? "peerOrganizations/":"ordererOrganizations/")+org_name+"/msp/signcerts/cert.pem "+mainDirectory+"/organizations/"+org_directory+"msp/admincerts");
        
        //copio il certificato in tls
        executeWSLCommand("cp $(pwd)/"+mainDirectory+"/organizations/"+org_directory+"msp/signcerts/cert.pem $(pwd)/"+mainDirectory+"/organizations/"+org_directory+"tls");
        //rinomino la chiave e ls sposto in tls
        String old_name=executeWSLCommandToString("find $(pwd)/"+mainDirectory+"/organizations/"+org_directory+"msp/keystore -name '*_sk'");
        executeWSLCommand("mv "+old_name+" $(pwd)/"+mainDirectory+"/organizations/"+org_directory+"msp/keystore/key");
        executeWSLCommand("cp $(pwd)/"+mainDirectory+"/organizations/"+org_directory+"msp/keystore/key $(pwd)/"+mainDirectory+"/organizations/"+org_directory+"tls");
        //copia del tls-root-cert
        executeWSLCommand("cp $(pwd)/"+mainDirectory+"/fabric-ca-client/tls-root-cert/tls-ca-cert.pem $(pwd)/"+mainDirectory+"/organizations/"+org_directory+"tls");
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
            peerConfig.put("depends_on", "couchdb");
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
    

       
    private static void add_orderer_to_docker(String ordererName, String cfgPath, String mspPath, String tlsPath, String mspId, LinkedList<Integer> ports)throws IOException{
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
        env.add("FABRIC_CFG_PATH=" + cfgPath);
        env.add("ORDERER_GENERAL_LOCALMSPDIR=" + mspPath);
        env.add("ORDERER_GENERAL_LOCALMSPID=" + mspId);
        ordererConfig.put("environment", env);

        List<String> volumes = new ArrayList<>();
        volumes.add(cfgPath + ":/etc/hyperledger/fabric/config");
        volumes.add(mspPath + ":/etc/hyperledger/fabric/msp");
        volumes.add(tlsPath + ":/etc/hyperledger/fabric/tls");
        ordererConfig.put("volumes", volumes);

        ordererConfig.put("ports", ports);
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
    private static void waitForContainer(String containerName){
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
