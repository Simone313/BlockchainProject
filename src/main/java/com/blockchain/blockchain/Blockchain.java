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
        //executeWSLCommand("pwd");
        mainDirectory="Prova";
        String yourPin="2003";
        createDirectory(mainDirectory);
        setupCA(yourPin);
        
        //metodo per creare il sistema di cartelle delle organizzazioni
        createDirectoryForOrganizations();
        //insertNewIdentities();
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
    
    
    
    
    
    
    private static void createDirectoryForOrganizations(){
        Scanner in= new Scanner(System.in);
        executeWSLCommand("cd "+mainDirectory+" &&"
                + "mkdir organizations &&"
                + "cd organizations &&"
                + "mkdir fabric-ca ordererOrganizations peerOrganizations");
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
                    for(int i=1;i<=num_orderer;i++){
                        System.out.print("Orderer "+i+" Name: ");
                        String orderer_name=in.next();
                        executeWSLCommand("cd "+mainDirectory+"/organizations/ordererOrganizations/"+organization_name+"/orderers &&"
                                + "mkdir "+orderer_name+" &&"
                                + "cd "+orderer_name+" &&"
                                + "mkdir msp tls");
                    }
                    organizationAdminRegistrationEnroll(organization_name, false);
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
                    for(int i=1;i<=num_peer;i++){
                        System.out.print("Peer "+i+" Name: ");
                        String peer_name=in.next();
                        executeWSLCommand("cd "+mainDirectory+"/organizations/peerOrganizations/"+organization_name+"/peers &&"
                                + "mkdir "+peer_name+" &&"
                                + "cd "+peer_name+" &&"
                                + "mkdir msp tls");
                    }
                    organizationAdminRegistrationEnroll(organization_name, true);
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
    
    
    private static void createConfig_yamlForPeer(){
        
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
