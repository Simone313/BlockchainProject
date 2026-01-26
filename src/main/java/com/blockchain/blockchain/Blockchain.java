/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Project/Maven2/JavaApp/src/main/java/${packagePath}/${mainClassName}.java to edit this template
 */



package com.blockchain.blockchain;

/**
 *
 * @author simo0
 */
import java.sql.*;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions;
import java.io.*;
import java.nio.Buffer;
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
import java.util.Optional;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.yaml.snakeyaml.LoaderOptions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
public class Blockchain {
    

    static String mainDirectory;
    static boolean intermediate=true;
    static String admin_name;
    static String admin_pwd;
    static String icaadmin_name;
    static String icaadmin_psw;
    static String org_name;
    static String org_psw;
    static LinkedList<Integer> ports_used=new LinkedList<Integer>();
    static int inter_port;
    static int server_port;
    static String tls_ca_name;
    static String org_ca_name;
    static String int_ca_name;
    static String first_peer;
    static String orderer_name;
    static int orderer_port;

    public static final String GREEN = "\u001B[32m";
    public static final String RESET = "\u001B[0m";
    public static final String RED = "\u001B[31m";
    /**
     * Application entry point.
     * Starts the main menu and handles project creation/opening.
     * @param args command line arguments (not used)
     * @throws IOException in case of I/O errors
     * @throws InterruptedException if execution is interrupted
     */
    public static void main(String[] args) throws IOException, InterruptedException {
       



       
        Scanner in= new Scanner(System.in);
        boolean loop=true;
        
        File projects= new File("src\\main\\java\\com\\blockchain\\blockchain\\projects.txt");
        
        try(BufferedReader reader= new BufferedReader(new FileReader(projects))){
            String line;
            LinkedList<String> prjs= new LinkedList<String>();
            while((line=reader.readLine())!=null){
                prjs.add(line.split(" ")[0]);
            }
            
            do{
                System.out.println(GREEN+" -------- MAIN MENU --------");
                System.out.println("1) Open Project");
                System.out.println("2) New Project");
                System.out.println("3) Delete Project");
                System.out.println("4) Exit");
                System.out.print("--> "+RESET);
                switch(in.nextInt()){
                    case 1:{
                        //TODO funzione open project
                        System.out.println(GREEN+" -------- SELECT PROJECT --------");

                        for(int i=0;i<prjs.size();i++){
                            System.out.println((i+1)+") "+((prjs.get(i)).split(" ")[0]));
                        }
                        System.out.println((prjs.size()+1)+") Exit");
                        System.out.print("--> ");
                        System.out.print(RESET);
                        int select= in.nextInt();
                        if(select==prjs.size()+1){
                            break;
                        }
                        mainDirectory= prjs.get(select-1);
                        if(prjs.get(select-1).split(" ")[0].equals("intermediate")){
                            intermediate=true;
                        }
                        if(!checkContainersRunning()){
                            executeWSLCommand("cd "+ mainDirectory+" && "
                                + "docker compose start");
                        }

                        

                        mainMenu();
                        break;
                    }
                    case 2:{
                        System.out.print(GREEN+"Project name: "+RESET);
                        mainDirectory=in.next().replace(" ", "_");
                        
                        prjs.add(mainDirectory+" "+(intermediate ? "intermediate":"root"));
                        FileWriter fw = new FileWriter(projects, true);
                        fw.write(mainDirectory+" "+(intermediate ? "intermediate":"root")+"\n");
                        fw.close();
                        Console console = System.console();
                        System.out.println("Insert sudo password for WSL:");
                        String yourPin;
                        if (console != null) {
                            yourPin = new String(console.readPassword("Password: "));
                        } else {
                            // Fallback per IDE: la password sarà purtroppo visibile
                            System.out.print("Password (visibile nell'IDE): ");
                            yourPin = in.nextLine();
                        }
                        
                        
                        createDirectory(mainDirectory);
                        setupCA(yourPin);
                        
                        if(!check_go()){
                            System.err.println("Go is not installed.\nInstalling Go...");
                            try {
                                installGo(yourPin);
                            } catch (IOException | InterruptedException e) {
                                e.printStackTrace();
                            }
                            System.out.println("Go installed successfully.");
                        }
                        installjq(yourPin);
                        executeWSLCommand("cd "+mainDirectory+" &&"
                                + "mkdir peers_bin");
                        download_peer_bin();
                        executeWSLCommand("cd "+mainDirectory+" &&"
                                + "mkdir orderers_bin");
                        download_orderer_bin();
                        
                        mainMenu();
                        break;
                    }
                    case 3:{
                        System.out.println(GREEN+" -------- SELECT PROJECT --------");

                        for(int i=0;i<prjs.size();i++){
                            System.out.println((i+1)+") "+prjs.get(i).split(" ")[0]);
                        }
                        System.out.println((prjs.size()+1)+") Exit"+RESET);

                        int select= in.nextInt();
                        in.nextLine();
                        if(select==prjs.size()+1){
                            break;
                        }
                        mainDirectory= prjs.get(select-1).split(" ")[0];

                        //Rimuovo il progetto dal file peerOrgs.txt
                        File peerOrgs= new File("src\\main\\java\\com\\blockchain\\blockchain\\peerOrgs.txt");
                        FileReader fr= new FileReader(peerOrgs);
                        BufferedReader br= new BufferedReader(fr);
                        line="";
                        LinkedList<String> peer_orgs= new LinkedList<String>();
                        while((line=br.readLine())!=null){
                            if(!line.contains(mainDirectory)){
                                peer_orgs.add(line);
                            }
                        }

                        br.close();

                        FileWriter fw2= new FileWriter(peerOrgs, false);
                        for(int i=0;i<peer_orgs.size();i++){
                            fw2.write(peer_orgs.get(i)+"\n");
                            fw2.flush();
                        }
                        fw2.close();
                        //Rimuovere il canale dal file channels.txt
                        File channels= new File("src\\main\\java\\com\\blockchain\\blockchain\\channels.txt");
                        FileReader fr2= new FileReader(channels);
                        BufferedReader br2= new BufferedReader(fr2);
                        line="";
                        LinkedList<String> chls= new LinkedList<String>();
                        while((line=br2.readLine())!=null){
                            if(!line.contains(mainDirectory)){
                                chls.add(line);
                            }
                        }
                        br2.close();
                        FileWriter fw3= new FileWriter(channels, false);
                        for(int i=0;i<chls.size();i++){
                            fw3.write(chls.get(i)+"\n");
                            fw3.flush();
                        }
                        fw3.close();
                        //Rimuovere il progetto dal file projects.txt
                        File prs= new File("src\\main\\java\\com\\blockchain\\blockchain\\projects.txt");
                        FileReader fr3= new FileReader(prs);
                        BufferedReader br3= new BufferedReader(fr3);
                        line="";
                        LinkedList<String> prjs2= new LinkedList<String>();
                        while((line=br3.readLine())!=null){
                            if(!line.contains(mainDirectory)){
                                prjs2.add(line);
                            }
                        }
                        br3.close();
                        FileWriter fw4= new FileWriter(projects, false);
                        for(int i=0;i<prjs2.size();i++){
                            fw4.write(prjs2.get(i)+"\n");
                            fw4.flush();
                        }
                        fw4.close();

                        //Elimino tutte le organizzazioni associate al progetto
                        File org_file= new File("src\\main\\java\\com\\blockchain\\blockchain\\organizations.txt");
                        FileReader fr_org= new FileReader(org_file);
                        BufferedReader br_org= new BufferedReader(fr_org);
                        line="";
                        LinkedList<String> orgs= new LinkedList<String>();
                        while((line=br_org.readLine())!=null){
                            if(!line.contains("\"projectName\":\""+mainDirectory+"\"")){
                                orgs.add(line);
                            }
                        }
                        br_org.close();
                        FileWriter fw_org= new FileWriter(org_file, false);
                        for(int i=0;i<orgs.size();i++){
                            fw_org.write(orgs.get(i)+"\n");
                            fw_org.flush();
                        }
                        fw_org.close();

                        LinkedList<String> ords= new LinkedList<>();
                        try(FileReader fr4= new FileReader("src\\main\\java\\com\\blockchain\\blockchain\\orderers.txt");
                            BufferedReader br4= new BufferedReader(fr4)){
                            String l;
                            while((l=br4.readLine())!=null){
                                if(!l.contains(mainDirectory)){
                                    ords.add(l);
                                }
                            }
                        }

                        for(int i=0;i<ords.size();i++){
                            if(ords.get(i).contains(mainDirectory)){
                                ords.remove(i);
                            }
                        }

                        try(FileWriter filew= new FileWriter("src\\main\\java\\com\\blockchain\\blockchain\\orderers.txt", false);
                            BufferedWriter bw= new BufferedWriter(filew)){
                            for(int j=0;j<ords.size();j++){
                                bw.write(ords.get(j));
                                bw.newLine();
                            }
                        }

                        //Elimino la cartella del progetto
                        prjs.remove(mainDirectory);
                        executeWSLCommand("cd "+ mainDirectory+" && "
                                + "docker compose down");
                        String check="";
                        do{
                            executeWSLCommand("rm -rf "+mainDirectory);
                            if(check.length()>0){
                                Thread.sleep(1000);
                            }                                                       
                            check=executeWSLCommandLS("");
                        }while(check.contains(mainDirectory+" "));
                        
                        
                        
                        break;
                    }
                    case 4:{
                        System.out.println("Shutdown in progress...");
                        loop=false;
                        break;
                    }
                    default:{
                        System.err.println(RED+"Input error, try again"+RESET);
                    }
                }
                FileWriter fw = new FileWriter(projects, false);
                String content="";
                for(int i=0;i<prjs.size();i++){
                    content=content+prjs.get(i)+"\n";
                }
                fw.write(content);
                fw.close();
            }while(loop);
            
            
            
        }catch(IOException e){
            System.err.println(e.toString());
        }
        
        
        
        
        
    }

    

    
    /**
     * Checks if Docker is running by querying the server version.
     * @return true if Docker is active, false otherwise
     * @throws IOException in case of I/O errors
     * @throws InterruptedException if execution is interrupted
     */
    private static boolean isDockerRunning() throws IOException, InterruptedException {
        String output = executeWSLCommandToString("docker info --format '{{.ServerVersion}}'");
        return !output.isEmpty();
    }

    /**
     * Checks if the Docker containers for the current project are running.
     * @return true if at least one container is active, false otherwise
     */
    private static boolean checkContainersRunning(){
        String output = executeWSLCommandToString("cd "+ mainDirectory +" && docker compose ps");
        return output.contains("Up");
    }
    
    
    /**
     * Creates a directory in the filesystem using a WSL command.
     * @param name the name of the directory to create
     */
    private static void createDirectory(String name){
        executeWSLCommand("mkdir "+name);
    }
    
    
    /**
     * Prepares and configures the Certificate Authority (CA) environment for the blockchain project.
     * Installs necessary binaries if missing, configures TLS and organization CA servers,
     * and starts the CA services using WSL.
     * @param pin the password used for sudo operations in WSL
     * @throws IOException in case of I/O errors
     * @throws InterruptedException if the process is interrupted
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

        if(executeWSLCommandToString("cd "+ mainDirectory +" && which sqlite3").length()==0){
            String remoteCmd = "cd '" + mainDirectory + "' && apt update && apt install -y sqlite3";

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
            System.out.println("sqlite3 already installed. ");
        }

        
        if(executeWSLCommandToString("cd "+ mainDirectory +" && ls").contains("fabric-ca-client") && executeWSLCommandToString("cd "+ mainDirectory +" && ls").contains("fabric-ca-server-tls")){
            System.out.println("Binaries alrady installed");
        }else{
            System.out.println("Installing Fabric CA server and CA client binaries...");
            executeWSLCommandWithProgress("cd "+ mainDirectory +" && "
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
            restartCAserverWithTLS();
            enrollCAAdmin(0);
            String userName=registerNewIdentity(0);
            deployOrganizationCA(0, userName);
            new FabricOrganizationServerThread(0,mainDirectory);
            waitForContainer(org_ca_name);

            executeWSLCommand("cd "+Blockchain.mainDirectory+"/fabric-ca-client &&"
                + "./fabric-ca-client enroll -d -u https://"+org_name+":"+org_psw+"@localhost:7055 "
                +"--tls.certfiles $(pwd)/"+mainDirectory+"/fabric-ca-client/tls-root-cert/tls-ca-cert.pem "
                +"--csr.hosts 'localhost,tls-ca,127.0.0.1' "
                +"--mspdir $(pwd)/"+mainDirectory+"/fabric-ca-client/org1-ca/rcaadmin/msp");
            
            
            
            

            
            if(intermediate){
                registerIntermediateCAAdmin(0);
                executeWSLCommand("cp "+mainDirectory+"/fabric-ca-client/tls-root-cert/tls-ca-cert.pem "+mainDirectory+"/fabric-ca-server-int-ca/");
                deployIntermediateCA();
                new FabricIntermediateServerThread(mainDirectory);
                waitForContainer(int_ca_name);
                String enrollCmd =
                "cd " + mainDirectory + "/fabric-ca-client && " +
                "./fabric-ca-client enroll " +
                "-u https://icaadmin:icaadminPsw@127.0.0.1:7056 " +
                "--tls.certfiles $(pwd)/"+mainDirectory+"/fabric-ca-client/tls-root-cert/tls-ca-cert.pem "+
                "--csr.hosts localhost,127.0.0.1 "+
                "--mspdir $(pwd)/"+mainDirectory+"/fabric-ca-client/int-ca/icaadmin/msp";
                executeWSLCommand(enrollCmd);

                

                // 3. COPIA E RIAVVIA IL SERVER (Oppure avvialo se era fermo)
                executeWSLCommand("docker cp " + mainDirectory + "/fabric-ca-server-int-ca/ca-chain.pem int-ca:/etc/hyperledger/fabric-ca-server/ca-chain.pem");
                executeWSLCommand("docker restart int-ca");

            }
            
            
            
            
        }
        
        
    }
    
    /**
     * Performs the enrollment request to the TLS CA server to obtain TLS certificates.
     * Creates necessary directories and registers the TLS admin.
     * @param i index used to identify the CA server (currently not used in the method)
     */
    private static void enrollRequestToCAServer(int i){
        executeWSLCommand("cd "+ mainDirectory +"/fabric-ca-client/tls-ca &&"
                + "mkdir tlsadmin &&"
                + "cd tlsadmin &&"
                + "mkdir msp");
        executeWSLCommand("cd "+ mainDirectory +"/fabric-ca-client &&"
                + "./fabric-ca-client enroll -d -u http://admin:adminPsw@127.0.0.1:7054 "
                + "--enrollment.profile tls "
                + "--csr.hosts 'localhost,127.0.0.1,tls-ca' "
                + "--tls.certfiles $(pwd)/"+mainDirectory+"/fabric-ca-server-tls/ca-cert.pem "
                + "--mspdir $(pwd)/"+mainDirectory+"/fabric-ca-client/tls-ca/tlsadmin/msp");
        executeWSLCommand("mv $(pwd)/"+mainDirectory+"/fabric-ca-client/tls-ca/tlsadmin/msp/keystore/*_sk "
                + "$(pwd)/"+mainDirectory+"/fabric-ca-client/tls-ca/tlsadmin/msp/keystore/CA_PRIVATE_KEY");
    }

    /**
     * Restarts the TLS CA server after enabling TLS in the configuration.
     * Updates the YAML config file to enable TLS and restarts the Docker container.
     */
    private static void restartCAserverWithTLS(){
        File server_config=new File(""+ mainDirectory +"/fabric-ca-server-tls/fabric-ca-server-config.yaml");
        Yaml yaml= new Yaml();
        try {
            Map<String, Object> data= yaml.load(new FileReader(server_config));
            Map<String,Object> tls=(Map<String,Object>) data.get("tls");
            tls.put("enabled",true);
            tls.put("certfile", "/etc/hyperledger/fabric-ca-client/tls-ca/tlsadmin/msp/signcerts/cert.pem");
            tls.put("keyfile", "/etc/hyperledger/fabric-ca-client/tls-ca/tlsadmin/msp/keystore/CA_PRIVATE_KEY");

            Map<String,Object> ca= (Map<String,Object>) data.get("ca");
            ca.put("certfile","/etc/hyperledger/fabric-ca-server/ca-cert.pem");
            ca.put("keyfile", "/etc/hyperledger/fabric-ca-server/msp/keystore/CA_PRIVATE_KEY");
            //Writing
            DumperOptions options = new DumperOptions();
            options.setIndent(2);
            options.setPrettyFlow(true);
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            Yaml yamlWriter = new Yaml(options);

            try (FileWriter writer = new FileWriter(server_config)) {
                yamlWriter.dump(data, writer);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Config updated");
        } catch (FileNotFoundException e) {
            
        }

        executeWSLCommand("cd "+ mainDirectory +" &&"
                + "docker compose restart tls-ca");
    }
    
    /**
     * Registers and enrolls the root CA admin (rcaadmin).
     * Creates necessary MSP directories and registers the user with admin privileges.
     * @param i index used to identify the CA (currently not used in the method)
     */
    private static void enrollCAAdmin(int i) {
        String caAdmin = "admin";
        String caAdminPwd = "adminPsw";
        
        String fabricCaClientDir = mainDirectory + "/fabric-ca-client";
        String clientHome = fabricCaClientDir + "/tls-ca/rcaadmin";
        executeWSLCommand("mkdir -p " + fabricCaClientDir + "/tls-ca/rcaadmin/msp");
        executeWSLCommand("cd "+mainDirectory+"/fabric-ca-client && "
            +"export FABRIC_CA_CLIENT_HOME=$(pwd)/"+mainDirectory+"/fabric-ca-client/tls-ca/tlsadmin && "
            +"./fabric-ca-client register "
                + "-u https://127.0.0.1:7054 "
                + "--tls.certfiles $(pwd)/"+mainDirectory+"/fabric-ca-client/tls-root-cert/tls-ca-cert.pem "
                + "--id.name rcaadmin "
                + "--id.secret rcaadminPsw "
                + "--id.type admin "
                + "--id.attrs \"hf.Registrar.Roles=client\" "
                + "--id.attrs \"hf.Registrar.Roles=peer\" "
                + "--id.attrs \"hf.Registrar.Roles=orderer\" "
                + "--id.attrs \"hf.Registrar.Roles=admin\" "
                + "--id.attrs \"hf.Registrar.Roles=ca\" "
                + "--id.attrs \"hf.Registrar.Attributes=*\" "
                + "--id.attrs \"hf.IntermediateCA=true\" "
                + "--id.attrs \"hf.Revoker=true\""
            );
        String enrollCmd =
        "cd " + mainDirectory + "/fabric-ca-client && " +
        "./fabric-ca-client enroll " +
        "-u https://rcaadmin:rcaadminPsw@127.0.0.1:7054 " +
        "--tls.certfiles $(pwd)/"+mainDirectory+"/fabric-ca-client/tls-root-cert/tls-ca-cert.pem "+
        "--csr.hosts localhost,127.0.0.1 "+
        "--mspdir $(pwd)/"+mainDirectory+"/fabric-ca-client/tls-ca/rcaadmin/msp";
        executeWSLCommand(enrollCmd);
    
    
    }



    
    /**
     * Registers a new identity in the CA and prepares the folder structure for the org.
     * @param i index used to build paths
     * @return the name of the registered identity (icaadmin_name in the current flow)
     */
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
    
    /**
     * Registers and configures the admin for an intermediate CA (icaadmin).
     * Creates MSP directories, registers the identity, and performs enrollment with CA profile.
     * @param i index of the intermediate CA server
     */
    private static void registerIntermediateCAAdmin(int i){
        executeWSLCommand("cd "+mainDirectory+"/fabric-ca-client/tls-ca &&"
                + "mkdir icaadmin &&"
                + "cd icaadmin &&"
                + "mkdir msp");
        icaadmin_name= "icaadmin";
        icaadmin_psw= "icaadminPsw";
        
        executeWSLCommand("mkdir "+mainDirectory+"/fabric-ca-client/int-ca/icaadmin");
        executeWSLCommand("cd " + mainDirectory + "/fabric-ca-client && "
            +"export FABRIC_CA_CLIENT_HOME=$(pwd)/"+mainDirectory+"/fabric-ca-client/tls-ca/tlsadmin && "
            + "./fabric-ca-client register -d "
            + "-u https://127.0.0.1:7054 "
            + "--tls.certfiles $(pwd)/"+mainDirectory+"/fabric-ca-client/tls-root-cert/tls-ca-cert.pem "
            + "--id.name icaadmin --id.secret icaadminPsw "
            + "--id.type admin " 
            + "--id.attrs \"hf.IntermediateCA=true:ecert\"");
        
        executeWSLCommand("cd " + mainDirectory + "/fabric-ca-client && "
            + "./fabric-ca-client enroll -d "
            + "-u https://icaadmin:icaadminPsw@127.0.0.1:7054 "
            + "--tls.certfiles $(pwd)/"+mainDirectory+"/fabric-ca-client/tls-root-cert/tls-ca-cert.pem "
            + "--csr.hosts '127.0.0.1,localhost' "
            + "--mspdir $(pwd)/"+mainDirectory+"/fabric-ca-server-int-ca/icaadmin/msp "
            + "--enrollment.profile ca"
        );     
        String old_name=executeWSLCommandToString("find $(pwd)/"+mainDirectory+"/fabric-ca-server-int-ca/icaadmin/msp/keystore -name '*_sk'");
        System.out.println("old_name:"+old_name);
        executeWSLCommand("mv "+old_name+" $(pwd)/"+mainDirectory+"/fabric-ca-server-int-ca/icaadmin/msp/keystore/CA_PRIVATE_KEY");
        


        
        
    }

    /**
     * Copies the intermediate CA admin certificate (icaadmin) from the root CA database
     * to the intermediate CA database to ensure certificate chain continuity.
     */
    private static void addIcaadminToDB() {
        String selectSQL = "SELECT * FROM certificates WHERE id = 'icaadmin'";

        // Nota i nomi file: fabric-ca-server.db (con trattino) vs fabric_ca_server.db (con underscore)
        // Assicurati che siano quelli corretti nel tuo filesystem
        try (Connection rootConn = DriverManager.getConnection("jdbc:sqlite:" + mainDirectory + "\\fabric-ca-server-tls\\fabric-ca-server.db");
            Connection intConn = DriverManager.getConnection("jdbc:sqlite:" + mainDirectory + "\\fabric-ca-server-int-ca\\fabric-ca-server.db");
            Statement stmt = rootConn.createStatement();
            ResultSet rs = stmt.executeQuery(selectSQL)) {

            if (rs.next()) {
                // MATCHING DELLE COLONNE CON LA TUA IMMAGINE:
                // Vedo: id, serial_number, authority_key_identifier, ca_label, status, reason, expiry, revoked_at, pem, level
                String insertSQL = "INSERT OR REPLACE INTO certificates " +
                                "(id, serial_number, authority_key_identifier, ca_label, status, " +
                                "reason, expiry, revoked_at, pem, level) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                try (PreparedStatement pstmt = intConn.prepareStatement(insertSQL)) {
                    pstmt.setString(1, rs.getString("id"));
                    pstmt.setString(2, rs.getString("serial_number"));
                    pstmt.setString(3, rs.getString("authority_key_identifier"));
                    pstmt.setString(4, rs.getString("ca_label"));
                    pstmt.setString(5, rs.getString("status"));
                    pstmt.setInt(6, rs.getInt("reason"));
                    
                    // Usiamo getString o getTimestamp a seconda di come SQLite ha memorizzato la data
                    pstmt.setString(7, rs.getString("expiry")); 
                    pstmt.setString(8, rs.getString("revoked_at"));
                    
                    pstmt.setString(9, rs.getString("pem"));
                    pstmt.setInt(10, rs.getInt("level"));

                    pstmt.executeUpdate();
                    System.out.println("Certificato icaadmin copiato con successo!");
                }
            } else {
                System.err.println("Errore: icaadmin non trovato nel database sorgente (Root CA).");
            }
        } catch (SQLException e) {
            System.err.println("Errore SQL: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    
    /**
     * Generates and writes the initial docker-compose.yaml file for the blockchain environment,
     * configuring the network and TLS CA service.
     */
    private static void createDockerComposeYaml(){
        executeWSLCommand("cd "+mainDirectory+" &&"
                + "cat > $(pwd)/"+mainDirectory+"/docker-compose.yaml << 'EOF'\n" +
                "networks:\n" +
                "  fabric_network:\n" +
                "    driver: bridge\n" +
                "services:\n" +
                "  tls-ca:\n" +
                "    image: hyperledger/fabric-ca:1.5.15\n" +
                "    container_name: "+tls_ca_name+"\n" +
                "    ports:\n" +
                "      - \"7054:7054\"\n" +
                "    environment:\n" +
                "      - FABRIC_CA_HOME=/etc/hyperledger/fabric-ca-server\n" +
                "    volumes:\n" +
                "      - $(pwd)/"+mainDirectory+"/fabric-ca-server-tls:/etc/hyperledger/fabric-ca-server\n"+
                "      - $(pwd)/"+mainDirectory+"/fabric-ca-client:/etc/hyperledger/fabric-ca-client\n"+
                "      - $(pwd)/"+mainDirectory+"/fabric-ca-server-tls/fabric-ca-server-config.yaml:/etc/hyperledger/fabric-ca-server/fabric-ca-server-config.yaml\n" +
                "    command: sh -c 'fabric-ca-server start -b"+ admin_name+":"+admin_pwd+" -d'\n"+
                "EOF");
    }
    
    /**
     * Sets up the TLS part for the CA, creating necessary files and directories.
     * @param i index of the server/CA
     * @throws FileNotFoundException if a required file is not found
     * @throws IOException in case of other I/O errors
     */
    private static void setupCA_TLS(int i) throws FileNotFoundException, IOException{
        try{
            executeWSLCommand("cd "+ mainDirectory +" &&"
                + "mkdir fabric-ca-server-tls &&"
                + "cp bin/fabric-ca-server fabric-ca-server-tls");
            //System.out.println("-----------ADMIN REGISTRATION-----------");
            //System.out.print("Admin's name: ");
            admin_name="admin";
            //System.out.print("Admin's password: ");
            admin_pwd="adminPsw";
            
            //System.out.println(GREEN+"------Modify the TLS CA server configuration------"+RESET);
            executeWSLCommand("cp fabric-ca-server-config.yaml "+mainDirectory+"/fabric-ca-server-tls/");
            

            executeWSLCommand("cp "+mainDirectory+"/fabric-ca-server-tls/fabric-ca-server-config.yaml "+mainDirectory);


            
            
            //CA
            //System.out.print(GREEN+"Name of this CA: "+RESET);
            tls_ca_name="tls-ca";
            File server_config=new File(""+ mainDirectory +"/fabric-ca-server-tls/fabric-ca-server-config.yaml");
            Yaml yaml= new Yaml();
            Map<String, Object> data= yaml.load(new FileReader(server_config));
            Map<String,Object> ca=(Map<String,Object>) data.get("ca");
            ca.put("name", tls_ca_name);
            
            
            data.remove("intermediate");


            //registry
            

            //TLS
            Map<String,Object> tls=(Map<String,Object>) data.get("tls");
            tls.put("enabled",false);
            //tls.put("certfile", "/etc/hyperledger/fabric-ca-server-config/ca-cert.pem");
            //tls.put("keyfile", "/etc/hyperledger/fabric-ca-server-config/msp/keystore/CA_PRIVATE_KEY");
            //Map<String,Object> tls_clientauth=(Map<String,Object>) tls.get("clientauth");


            //System.out.println(GREEN+"Do you want to activate the mutual TLS option? y/n"+RESET);
            String risp="n";
            Map<String,Object> tls_clientauth= (Map<String,Object>) tls.get("clientauth");
            if(risp.equals("n")){
            }else{

                tls_clientauth.put("type", "RequireAndVerifyClientCert");
            }

            //tls_clientauth.put("certfiles", "ca-cert.pem");
            
            //Profile CA
            Map<String,Object> caconstraint= (Map<String,Object>) ((Map<String,Object>) ((Map<String,Object>) ((Map<String,Object>) data.get("signing")).get("profiles")).get("ca")).get("caconstraint");
            caconstraint.put("maxpathlen",0);
            
            //csr
            Map<String,Object> csr_ca=(Map<String,Object>)((Map<String,Object>)data.get("csr")).get("ca");
            csr_ca.put("pathlength", 1);
            
            
            
            
            //PORT
            ports_used=new LinkedList<Integer>();
            
            
            ports_used.add(7054);
            data.put("port", 7054);
            
            //DB
            Map<String,Object> db=(Map<String,Object>) data.get("db");
            db.put("type", "sqlite3");
            
            Map<String, Object> csr = (Map<String, Object>) data.get("csr");
            csr.put("cn", "tls-ca");
            csr.put("hosts", Arrays.asList("localhost","tls-ca","127.0.0.1"));
            
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
        executeWSLCommand(
            "cd "+ mainDirectory +"/fabric-ca-server-tls && " +
            "./fabric-ca-server init " +
            "-b "+admin_name+":"+admin_pwd
        );

        executeWSLCommand("cp "+ mainDirectory +"/fabric-ca-server-tls/ca-cert.pem "+ mainDirectory +"/fabric-ca-client/tls-root-cert/tls-ca-cert.pem");
        //executeWSLCommand("cp "+ mainDirectory +"/fabric-ca-server-tls/ca-cert.pem "+ mainDirectory +"/fabric-ca-server-tls/msp/keystore/tls-ca-cert.pem");
        String old_name=executeWSLCommandToString("find $(pwd)/"+mainDirectory+"/fabric-ca-server-tls/msp -name '*_sk'");
        executeWSLCommand("mv "+old_name+" $(pwd)/"+mainDirectory+"/fabric-ca-server-tls/msp/keystore/CA_PRIVATE_KEY");
        
        
    }
    
    /**
     * Deploys an organization CA, copying certificates and configurations.
     * @param i index of the CA
     * @param user_name bootstrap user name used for registration
     * @throws FileNotFoundException if a required file is not found
     * @throws IOException in case of other I/O errors
     */
    private static void deployOrganizationCA(int i, String user_name) throws FileNotFoundException, IOException{
        String server_name= "fabric-ca-server-org"+(i+1);
        
        executeWSLCommand("cd "+mainDirectory+" &&"
                + "mkdir "+server_name+" &&"
                + "cp bin/fabric-ca-server "+server_name+" &&"
                + "cd "+server_name+" &&"
                + "mkdir tls");
        
        
        executeWSLCommand("cd "+mainDirectory+"/"+server_name+" &&"
                + "./fabric-ca-server init -b "+org_name+":"+org_psw
                +" --csr.hosts localhost,127.0.0.1"
            );
        String old_name=executeWSLCommandToString("find $(pwd)/"+mainDirectory+"/fabric-ca-server-tls/org1-server-tls/keystore -name '*_sk'");
            executeWSLCommand("mv "+old_name+" $(pwd)/"+mainDirectory+"/fabric-ca-server-tls/org1-server-tls/keystore/ORG1_PRIVATE_KEY");
        executeWSLCommand("cp $(pwd)/"+mainDirectory+"/fabric-ca-server-tls/org1-server-tls/signcerts/cert.pem $(pwd)/"+mainDirectory+"/fabric-ca-server-org1/tls && cp $(pwd)/"+mainDirectory+"/fabric-ca-server-tls/org1-server-tls/keystore/ORG1_PRIVATE_KEY $(pwd)/"+mainDirectory+"/fabric-ca-server-org1/tls ");

        //System.out.println(GREEN+"------Modify the ORGANIZATION CA Server configuration------"+RESET);
            
        Scanner in = new Scanner(System.in);
        //CA
        //System.out.print(GREEN+"Name of this CA: "+RESET);
        org_ca_name="org-ca";
        File server_config=new File(""+ mainDirectory +"/"+server_name+"/fabric-ca-server-config.yaml");
        Yaml yaml= new Yaml();
        Map<String, Object> data= yaml.load(new FileReader(server_config));
        Map<String,Object> ca=(Map<String,Object>) data.get("ca");
        ca.put("name", org_ca_name);


        //TLS
        Map<String,Object> tls=(Map<String,Object>) data.get("tls");
        tls.put("enabled",true);

        tls.put("certfile", "/etc/hyperldger/fabric-ca-client/tls-ca/rcaadmin/msp/signcerts/cert.pem");
        tls.put("keyfile", "/etc/hyperldger/fabric-ca-client/tls-ca/rcaadmin/msp/keystore/"+executeWSLCommandToString("ls "+mainDirectory+"/fabric-ca-client/tls-ca/rcaadmin/msp/keystore/ | grep '_sk'").trim());

        //System.out.println(GREEN+"Do you want to activate the mutual TLS option? y/n"+RESET);
        String risp="n";
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
        csr.put("cn", "org-ca");
        ArrayList<String> hosts = (ArrayList<String>) csr.get("hosts");

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
    
    
    /**
     * Adds the CA service definition to the docker-compose file (append).
     * @param name service name
     * @param tls true if TLS is enabled
     * @param port exposed port
     * @param serverName CA server name
     * @param inter true if it is an intermediate CA
     * @throws FileNotFoundException if a file is not found
     * @throws IOException in case of I/O errors
     */
    private static void addCAtoDocker(String name, boolean tls, int port, String serverName, boolean inter) throws FileNotFoundException, IOException{
        Yaml yaml= new Yaml();
        File file= new File(""+ mainDirectory +"/docker-compose.yaml");
        Map<String, Object> data = yaml.load(new FileInputStream(file));
        Map<String, Object> services = (Map<String, Object>) data.get("services");
       
        
        //add organization CA service
        Map<String, Object> caOrg1 = new LinkedHashMap<>();
        caOrg1.put("image", "hyperledger/fabric-ca:1.5.15");
        caOrg1.put("container_name",name);
        
        
        caOrg1.put("ports", Collections.singletonList(port+":"+port));
        
        
        String path=executeWSLCommandToString("echo $(pwd)");
        if(inter){
            caOrg1.put("environment", Arrays.asList(
                "FABRIC_CA_SERVER_HOME=/etc/hyperledger/fabric-ca-server",
                "FABRIC_CA_SERVER_CONFIG=/etc/hyperledger/fabric-ca-server/fabric-ca-server-config.yaml"
            ));
            caOrg1.put("command", "sh -c 'fabric-ca-server start -b icaadmin:icaadminPsw -d'");
            caOrg1.put("volumes", Arrays.asList(
                path + "/" + mainDirectory + "/fabric-ca-client/tls-ca/icaadmin:/etc/hyperledger/fabric-ca-client/icaadmin",
                path + "/" + mainDirectory + "/fabric-ca-server-int-ca:/etc/hyperledger/fabric-ca-server",
                path + "/" + mainDirectory + "/fabric-ca-server-int-ca/fabric-ca-server-config.yaml:/etc/hyperledger/fabric-ca-server/fabric-ca-server-config.yaml"
            ));

        
        }else{
            caOrg1.put("environment", Arrays.asList(
                "FABRIC_CA_SERVER_HOME=/etc/hyperledger/fabric-ca-server",
                "FABRIC_CA_SERVER_CONFIG=/etc/hyperledger/fabric-ca-server/fabric-ca-server-config.yaml"
            ));
            caOrg1.put("command", "sh -c 'fabric-ca-server start -b admin:adminPsw -d'");
            caOrg1.put("volumes", Arrays.asList(
                path+"/"+mainDirectory+"/fabric-ca-server-org1:/etc/hyperledger/fabric-ca-server",
                path+"/"+mainDirectory+"/fabric-ca-client:/etc/hyperldger/fabric-ca-client/",
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
    
    
    /**
     * Deploys and configures an intermediate CA, copying binaries and configuring the YAML file
     * to link it to the root TLS CA.
     * @throws IOException in case of I/O errors during configuration
     */
    private static void deployIntermediateCA() throws IOException{
        
        
        executeWSLCommand("cp "+mainDirectory+"/fabric-ca-server-config.yaml "+mainDirectory+"/fabric-ca-server-int-ca/");
        File server_config=new File(""+ mainDirectory +"/fabric-ca-server-int-ca/fabric-ca-server-config.yaml");
        Yaml yaml= new Yaml();
        Map<String, Object> data= yaml.load(new FileReader(server_config));
        //identiites
        ArrayList<Map<String, Object>> identities= (ArrayList<Map<String, Object>>)((Map<String, Object>) data.get("registry")).get("identities");
        Map<String, Object> admin_identity= identities.get(0);
        admin_identity.put("name", "icaadmin");
        admin_identity.put("pass", "icaadminPsw");
        admin_identity.put("type", "admin");
        Map<String, Object> csr = (Map<String, Object>) data.get("csr");

        csr.remove("cn");

        
        Map<String,Object> ca=(Map<String,Object>) data.get("ca");
        ca.put("name", int_ca_name);
        ca.put("chainfile", "ca-chain.pem");
        ca.put("trustedroots", Arrays.asList("/etc/hyperledger/fabric-ca-server/tls-ca-cert.pem"));
        //intermediate
        Map<String,Object> intermediate=(Map<String,Object>) data.get("intermediate");
        intermediate.put("parentserver", Map.of(
            "url", "https://icaadmin:icaadminPsw@tls-ca:7054",
            "caname", "tls-ca"
        ));

        intermediate.put("tls",Map.of(
            "certfiles", Arrays.asList("tls-ca-cert.pem")
        ));

        Map<String,Object> csr_ca=(Map<String,Object>)((Map<String,Object>)data.get("csr")).get("ca");
            csr_ca.put("pathlength", 0);
        //Writing
        DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yamlWriter = new Yaml(options);

        try (FileWriter writer = new FileWriter(server_config)) {
            yamlWriter.dump(data, writer);
        }
        executeWSLCommand("cp "+mainDirectory+"/bin/fabric-ca-server "+ mainDirectory +"/fabric-ca-server-int-ca");
        executeWSLCommand(
            "cd "+ mainDirectory +"/fabric-ca-server-int-ca && " +
            "./fabric-ca-server init " +
            "-b icaadmin:icaadminPsw"
        );


        
            //CA
            int_ca_name="int-ca";
            yaml= new Yaml();
            data= yaml.load(new FileReader(server_config));
            data.put("port", 7056);
            
            //TLS
            Map<String,Object> tls=(Map<String,Object>) data.get("tls");
            tls.put("enabled",true);

            tls.put("certfile", "/etc/hyperledger/fabric-ca-server/icaadmin/msp/signcerts/cert.pem");
            tls.put("keyfile", "/etc/hyperledger/fabric-ca-server/icaadmin/msp/keystore/CA_PRIVATE_KEY");

            String risp="n";
            if(risp.equals("n")){
            }else{
                Map<String,Object> tls_clientauth= (Map<String,Object>) tls.get("clientauth");
                tls_clientauth.put("type", "RequireAndVerifyClientCert");
            }
            
            
           intermediate= (Map<String,Object>) data.get("intermediate");
            
            Map<String,Object> enrollment=(Map<String,Object>) intermediate.get("enrollment");
            enrollment.put("hosts", Arrays.asList("localhost","int-ca"));
            enrollment.put("profile", "ca");

            Map<String,Object> inter_tls=(Map<String,Object>) intermediate.get("tls");
            inter_tls.put("certfiles", "/etc/hyperledger/fabric-ca-server/tls-ca-cert.pem");
            
            data.put("no_verify", true);
            Map<String, Object> tlsMap = (Map<String, Object>) data.get("tls");
            Map<String, Object> clientMap = new LinkedHashMap<>();
            clientMap.put("skiphostverify", true);
            tlsMap.put("client", clientMap);
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
            
            
            //Writing
            options = new DumperOptions();
            options.setIndent(2);
            options.setPrettyFlow(true);
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            yamlWriter = new Yaml(options);

            try (FileWriter writer = new FileWriter(server_config)) {
                yamlWriter.dump(data, writer);
            }
            System.out.println("Config updated");
            String server_name= "fabric-ca-server-int-ca";
            
            
            addCAtoDocker(int_ca_name, true, inter_port, server_name, true);
    }
    
    
    
    
    /**
     * Counts the number of peer organizations registered in the peerOrgs.txt file.
     * @return the total number of peer organizations
     */
    private static int get_num_org(){
        File peerOrgs= new File("src\\main\\java\\com\\blockchain\\blockchain\\peerOrgs.txt");
        int count=0;
        try{
            FileReader fr = new FileReader(peerOrgs);
            BufferedReader br = new BufferedReader(fr);
            while(br.readLine()!=null){
                count++;
            }
            fr.close();
            br.close();
        }catch(Exception e){
            System.err.println(e.toString());
        }
        return count;
    }
    
    
    
    
    /**
     * Creates the directory structure for organizations and their artifacts.
     * @param channel_name name of the associated channel/genesis
     * @throws IOException in case of I/O errors
     * @throws InterruptedException
     */
    private static void mainMenu() throws IOException, InterruptedException{
        Scanner in= new Scanner(System.in);
        executeWSLCommand("cd "+mainDirectory+" &&"
                + "mkdir organizations");
        executeWSLCommand("cd "+mainDirectory+" && cd organizations &&"
                + "mkdir fabric-ca");
        executeWSLCommand("cd "+mainDirectory+" && cd organizations &&"
                + "mkdir ordererOrganizations");
        executeWSLCommand("cd "+mainDirectory+" && cd organizations &&"
                + "mkdir peerOrganizations");
        
        boolean k=true;
        
        do{
            System.out.println(GREEN+"-------- MENU --------");
            //System.out.println("1) Add orderer organization");
            System.out.println("1) Add peer organization");
            System.out.println("2) Remove peer organization");
            System.out.println("3) Modify organization");
            System.out.println("4) Exit");
            System.out.print("--> "+RESET);
            int risp= in.nextInt();
            
            switch(risp){
                case 1:{
                    File peerOrgs= new File("src\\main\\java\\com\\blockchain\\blockchain\\peerOrgs.txt");
                    FileWriter fw = new FileWriter(peerOrgs, true);
                    FileReader fr = new FileReader(peerOrgs);
                    BufferedReader br = new BufferedReader(fr);
                    
                    boolean exists=false;
                    String organization_name;
                    do{
                        String line;
                        System.out.print(GREEN+"Peer Organization Name: "+RESET);
                        organization_name=in.next();
                        while((line=br.readLine())!=null){
                            if(line.equals(organization_name)){
                                exists=true;
                                System.out.println(RED+"Organization already exists! Choose another name."+RESET);
                                break;
                            }
                        }
                    }while(exists);
                    fr.close();
                    br.close();
                    first_peer=null;
                    fw.write(mainDirectory+"/"+organization_name+"\n");
                    fw.close();
                    //executeWSLCommand("mkdir "+mainDirectory+"/organizations");
                    //executeWSLCommand("mkdir "+mainDirectory+"/organizations/peerOrganizations");
                    executeWSLCommand("cd "+mainDirectory+"/organizations/peerOrganizations &&"
                            + "mkdir "+organization_name+" &&"
                            + "cd "+organization_name+" &&"
                            + "mkdir msp peers");
                    
                    executeWSLCommand("cd "+mainDirectory+"/organizations/peerOrganizations/"+organization_name+"/msp &&"
                            + "mkdir cacerts tlscacerts signcerts keystore");
                    
                    executeWSLCommand("cp "+mainDirectory+"/fabric-ca-client/tls-root-cert/tls-ca-cert.pem "+mainDirectory+"/organizations/peerOrganizations/"+organization_name+"/msp/tlscacerts");
                    organizationAdminRegistrationEnroll(organization_name, true);
                    createConfig_yaml("organizations/peerOrganizations/"+organization_name+"/users/Admin@"+organization_name+"/msp", organization_name);
                    
                    System.out.println(GREEN+"How many peers do you want to create?"+RESET);
                    int num_peer=in.nextInt();
                    LinkedList<String> peers_names=new LinkedList<String>();
                    for(int i=1;i<=num_peer;i++){
                        
                        System.out.print(GREEN+"Peer "+i+" Name: "+RESET);
                        String peer_name=in.next()+"."+organization_name;
                        if(!peer_name.startsWith("peer")){
                            peer_name="peer."+peer_name;
                        }
                        peers_names.add(peer_name);
                        if(i==1) first_peer=peer_name;
                        executeWSLCommand("cd "+mainDirectory+"/organizations/peerOrganizations/"+organization_name+"/peers &&"
                                + "mkdir "+peer_name+" &&"
                                + "cd "+peer_name+" &&"
                                + "mkdir msp tls");
                    }
                    String channel_name;
                    System.out.println(GREEN+"1) Use an existing channel");
                    System.out.println("2) Create a new channel"+RESET);
                    int choice=in.nextInt();
                    String existing_channel=null;
                    String channel_peer=null;
                    switch(choice){
                        case 1:{
                            File projects= new File("src\\main\\java\\com\\blockchain\\blockchain\\channels.txt");
                            BufferedReader reader= new BufferedReader(new FileReader(projects));
                            String line;
                            LinkedList<String> chs= new LinkedList<String>();
                            while((line=reader.readLine())!=null){
                                chs.add(line);
                            }
                            System.out.println(GREEN+"Select the channel to join:");
                            for(int i=0;i<chs.size();i++){
                                System.out.println((i+1)+") "+chs.get(i).split("/")[1]);
                            }
                            System.out.print("--> "+RESET);
                            int ch_choice=in.nextInt();
                            channel_name=chs.get(ch_choice-1).split("/")[1];
                            channel_peer=chs.get(ch_choice-1).split("/")[2];
                            existing_channel=chs.get(ch_choice-1);
                            reader.close();
                            break;
                        }
                        case 2:{
                            System.out.print(GREEN+"Channel name: "+RESET);
                            channel_name=in.next().toLowerCase();

                            createGenesisBlock(organization_name, peers_names, channel_name);
                    
                            createConfig_yaml("organizations/peerOrganizations/"+organization_name+"/msp", organization_name);
                            break;
                        }
                        default:{
                            System.out.println(RED+"Invalid choice! Exiting..."+RESET);
                            return;
                        }
                    }
                    
                    LinkedList<Integer> peer_ports= new LinkedList<Integer>();
                    int port=7051+((get_num_org()+get_num_other_peers(organization_name)-1)*1000);
                    for(int i=0;i<num_peer;i++){
                        while(ports_used.contains(port)){
                            port++;
                        }
                        peer_ports.add(port);
                        ports_used.add(port);
                        port++;
                    }
                    for(int i=0;i<num_peer;i++){
                        create_msp_tls_certificate(organization_name,peers_names.get(i),true);
                        executeWSLCommand("mkdir "+mainDirectory+"/organizations/peerOrganizations/"+organization_name+"/users");
                        executeWSLCommand("mkdir "+mainDirectory+"/organizations/peerOrganizations/"+organization_name+"/users/Admin@"+organization_name+" "
                                + "mkdir "+mainDirectory+"/organizations/peerOrganizations/"+organization_name+"/users/Admin@"+organization_name+"/msp");
                        createConfig_yaml("organizations/peerOrganizations/"+organization_name+"/msp", organization_name);
                        createConfig_yaml("organizations/peerOrganizations/"+organization_name+"/peers/"+peers_names.get(i)+"/msp", organization_name);
                        copy_peer_bin(peers_names.get(i), organization_name);
                        if(num_peer>1){
                            if(i==0){
                                configure_peer_core(peers_names.get(i), peers_names.get(i+1),peer_ports.get(i+1),organization_name,i, existing_channel);
                            }else{
                                configure_peer_core(peers_names.get(i), first_peer, peer_ports.get(0),organization_name,i, existing_channel);
                            }
                            
                        }else{
                            configure_peer_core(peers_names.get(i), first_peer, 7051+((get_num_org()+get_num_other_peers(organization_name)-1)*1000),organization_name,i, existing_channel);
                        }
                        
                        
                        
                    }
                    LinkedList<String> peers_channels= new LinkedList<String>();
                    

                    switch(choice){
                        case 1:{
                            addOrgtoChannel(organization_name, channel_peer,peers_names.get(0), channel_name);
                            executeWSLCommand("docker cp "+mainDirectory+"/bin/"+channel_name+"_block.pb "+peers_names.get(0)+":/etc/hyperledger/fabric/");
                                
                            String dockerCmd =
                                "docker exec " + peers_names.get(0) + " bash -c '"
                                + "export CORE_PEER_MSPCONFIGPATH=/etc/hyperledger/fabric/mspAdmin && "
                                + "export CORE_PEER_TLS_ENABLED=true && "
                                + "export CORE_PEER_ADDRESS="+peers_names.get(0)+":7051 &&"
                                + "export CORE_PEER_TLS_ROOTCERT_FILE="+(intermediate ?"/etc/hyperledger/fabric/tls/ca-chain.pem": "/etc/hyperledger/fabric/msp/cacerts/127-0-0-1-7054.pem")+" && "
                                + "export CORE_PEER_TLS_CERT_FILE=/etc/hyperledger/fabric/msp/signcerts/cert.pem && "
                                + "export CORE_PEER_TLS_KEY_FILE=/etc/hyperledger/fabric/msp/keystore/" + executeWSLCommandToString("ls "+mainDirectory+"/organizations/peerOrganizations/"+organization_name+"/peers/"+peers_names.get(0)+"/msp/keystore/ | grep _sk").trim() + " && "
                                + "peer channel join -b /etc/hyperledger/fabric/" + channel_name + "_block.pb'";
                            executeWSLCommand(dockerCmd);
                            create_chaincode_for_one_peer(channel_name, organization_name, peers_names.get(0), peer_ports.get(0));
                            if (num_peer>1){
                                for(int i=1;i<num_peer;i++){
                                    executeWSLCommand("docker exec " + peers_names.get(i) + " bash -c '"
                                    + "peer channel fetch 0 "+channel_name+"_block.pb -o " + orderer_name + ":" + orderer_port + " -c "+channel_name+" --tls --cafile "+(intermediate ? "/etc/hyperledger/fabric/tls/ca-chain.pem" : "/etc/hyperledger/fabric/tls/tls-ca-cert.pem")+" '");
                                    executeWSLCommand("docker exec " + peers_names.get(i) + " bash -c '"
                                    + "export CORE_PEER_MSPCONFIGPATH=/etc/hyperledger/fabric/mspAdmin && "
                                    + "peer channel join -b "+channel_name+"_block.pb'");
                                    //peers_channels.add(channel_name);
                                    executeWSLCommand(
                                        "cd "+mainDirectory+" && " +
                                            "export CORE_PEER_LOCALMSPID="+org_name+" && " +
                                            "export CORE_PEER_MSPCONFIGPATH=$PWD/"+mainDirectory+"/organizations/peerOrganizations/"+org_name+"/msp && " +
                                            "export CORE_PEER_ADDRESS="+ peers_names.get(i) +":"+port+" && " +
                                            "export CORE_PEER_TLS_ENABLED=true && "+
                                            "export FABRIC_CFG_PATH=$PWD/"+mainDirectory+"/peers_bin/"+ peers_names.get(i) +"/config/ && " +      
                                            "./peer lifecycle chaincode package mycc.tar.gz " +
                                            "--path $PWD/"+mainDirectory+"/atcc " +
                                            "--lang golang " +
                                            "--label mycc_1.0" 
                                    );

                                    
                                    //Install chaincode

                                    executeWSLCommand("docker cp $PWD/"+mainDirectory+"/mycc.tar.gz "+peers_names.get(i)+":/etc/hyperledger/fabric");
                                    executeWSLCommand("cd "+mainDirectory+" &&"
                                        + "docker exec " + peers_names.get(i) + " bash -c '"+
                                        "export CORE_PEER_MSPCONFIGPATH=/etc/hyperledger/fabric/mspAdmin && " +
                                        "peer lifecycle chaincode install /etc/hyperledger/fabric/mycc.tar.gz'");
                                }
                            }
                            organization peerOrg= new organization(organization_name, peers_names, peer_ports, mainDirectory, peers_channels);
                            try(FileWriter filew= new FileWriter("src\\main\\java\\com\\blockchain\\blockchain\\organizations.txt", true);
                                BufferedWriter bw= new BufferedWriter(filew)){
                                ObjectMapper mapper= new ObjectMapper();
                                String jsonString= mapper.writeValueAsString(peerOrg);
                                bw.write(jsonString);
                                bw.newLine();
                            }
                            break;
                        }
                        case 2:{
                            
                            createChannel(organization_name, peers_names.get(0), channel_name);
                            //Se abbiamo un solo peer, dobbiamo prima farlo joinare e poi aggiornare l'anchor peer 
                            executeWSLCommand("docker cp "+mainDirectory+"/bin/"+channel_name+"_block.pb "+peers_names.get(0)+":/etc/hyperledger/fabric/");
                                String dockerCmd =
                                    "docker exec " + peers_names.get(0) + " bash -c '"
                                    + "export CORE_PEER_MSPCONFIGPATH=/etc/hyperledger/fabric/mspAdmin && "
                                    + "export CORE_PEER_TLS_ENABLED=true && "
                                    + "export CORE_PEER_ADDRESS="+peers_names.get(0)+":"+(7051+((get_num_org()+get_num_other_peers(organization_name)-1)*1000))+" &&"
                                    + "export CORE_PEER_TLS_ROOTCERT_FILE="+(intermediate ?"/etc/hyperledger/fabric/tls/ca-chain.pem": "/etc/hyperledger/fabric/msp/cacerts/127-0-0-1-7054.pem")+" && "
                                    + "export CORE_PEER_TLS_CERT_FILE=/etc/hyperledger/fabric/msp/signcerts/cert.pem && "
                                    + "export CORE_PEER_TLS_KEY_FILE=/etc/hyperledger/fabric/msp/keystore/" + executeWSLCommandToString("ls "+mainDirectory+"/organizations/peerOrganizations/"+organization_name+"/peers/"+peers_names.get(0)+"/msp/keystore/ | grep _sk").trim() + " && "
                                    + "peer channel join -b /etc/hyperledger/fabric/" + channel_name + "_block.pb'";
                                executeWSLCommand(dockerCmd);
                                peers_channels.add(channel_name);
                                //Aggiorniamo l'anchor peer
                                //AnchorPeerUpdate(channel_name, organization_name, peers_names.get(0));
                                create_chaincode_for_one_peer(channel_name, organization_name, peers_names.get(0), peer_ports.get(0));
                            if (num_peer>1){
                                for(int i=1;i<num_peer;i++){
                                    executeWSLCommand("docker exec " + peers_names.get(i) + " bash -c '"
                                    + "peer channel fetch 0 "+channel_name+"_block.pb -o " + orderer_name + ":" + orderer_port + " -c "+channel_name+" --tls --cafile "+(intermediate ? "/etc/hyperledger/fabric/tls/ca-chain.pem" : "/etc/hyperledger/fabric/tls/tls-ca-cert.pem")+" '");
                                    executeWSLCommand("docker exec " + peers_names.get(i) + " bash -c '"
                                    + "export CORE_PEER_MSPCONFIGPATH=/etc/hyperledger/fabric/mspAdmin && "
                                    + "peer channel join -b "+channel_name+"_block.pb'");
                                    //peers_channels.add(channel_name);
                                    executeWSLCommand(
                                        "cd "+mainDirectory+" && " +
                                            "export CORE_PEER_LOCALMSPID="+org_name+" && " +
                                            "export CORE_PEER_MSPCONFIGPATH=$PWD/"+mainDirectory+"/organizations/peerOrganizations/"+org_name+"/msp && " +
                                            "export CORE_PEER_ADDRESS="+ peers_names.get(i) +":"+port+" && " +
                                            "export CORE_PEER_TLS_ENABLED=true && "+
                                            "export FABRIC_CFG_PATH=$PWD/"+mainDirectory+"/peers_bin/"+ peers_names.get(i) +"/config/ && " +      
                                            "./peer lifecycle chaincode package mycc.tar.gz " +
                                            "--path $PWD/"+mainDirectory+"/atcc " +
                                            "--lang golang " +
                                            "--label mycc_1.0" 
                                    );

                                    
                                    //Install chaincode

                                    executeWSLCommand("docker cp $PWD/"+mainDirectory+"/mycc.tar.gz "+peers_names.get(i)+":/etc/hyperledger/fabric");
                                    executeWSLCommand("cd "+mainDirectory+" &&"
                                        + "docker exec " + peers_names.get(i) + " bash -c '"+
                                        "export CORE_PEER_MSPCONFIGPATH=/etc/hyperledger/fabric/mspAdmin && " +
                                        "peer lifecycle chaincode install /etc/hyperledger/fabric/mycc.tar.gz'");
                                }
                                
                            }
                            organization peerOrg= new organization(organization_name, peers_names, peer_ports, mainDirectory, peers_channels);
                            try(FileWriter filew= new FileWriter("src\\main\\java\\com\\blockchain\\blockchain\\organizations.txt", true);
                                BufferedWriter bw= new BufferedWriter(filew)){
                                ObjectMapper mapper= new ObjectMapper();
                                String jsonString= mapper.writeValueAsString(peerOrg);
                                bw.write(jsonString);
                                bw.newLine();
                            }

                            
                            break;
                        }
                        default:{
                            System.out.println(RED+"ERROR"+RESET);
                        }


                        
                        
                    }
                    
                    
                    break;
                }
                case 2:{
                    File peerOrgs= new File("src\\main\\java\\com\\blockchain\\blockchain\\peerOrgs.txt");
                    BufferedReader reader= new BufferedReader(new FileReader(peerOrgs));
                    System.out.print(GREEN+"Select the organization to remove: "+RESET);
                    String line;
                    int i=1;
                    System.out.println(GREEN);
                    while((line=reader.readLine())!=null){
                        System.out.println(i+")"+line.split("/")[1]);
                        i++;
                    }
                    System.out.println(i+") "+"Exit");
                    System.out.print("--> "+RESET);
                    int org_choice=in.nextInt();

                    if(org_choice==i){
                        break;
                    }
                    String organization_name="";
                    LinkedList<String> pOr= new LinkedList<String>();
                    reader= new BufferedReader(new FileReader(peerOrgs));
                    i=1;
                    while((line=reader.readLine())!=null){
                        pOr.add(line);
                        if(i==org_choice){
                            organization_name=line.split("/")[1];
                        }
                        i++;
                    }
                    reader.close();
                    FileWriter fw = new FileWriter(peerOrgs);
                    for(int j=0;j<pOr.size();j++){
                        if((pOr.get(j).split("/")[1]).equals(organization_name)==false){
                            fw.write(pOr.get(j)+"\n");
                        }
                    }
                    fw.close();
                    String check="";
                    do{
                        executeWSLCommand("rm -rf "+mainDirectory+"/organizations/peerOrganizations/"+organization_name);
                        if(check.length()>0){
                            Thread.sleep(1000);
                        }                                                       
                        check=executeWSLCommandLS(mainDirectory+"/organizations/peerOrganizations/");
                    }while(check.contains(organization_name+" "));

                    LinkedList<organization> orgs= getOrganizationsCreated();
                    for(int j=0;j<orgs.size();j++){
                        if(orgs.get(j).getOrganization_name().equals(organization_name)){
                            for(int p=0;p<orgs.get(j).getPeers().size();p++){
                                executeWSLCommand("rm -rf "+mainDirectory+"/peers_bin/"+orgs.get(j).getPeers().get(p));
                                executeWSLCommand("docker rm -f "+orgs.get(j).getPeers().get(p));
                            }
                        }
                    }

                    for(int j=0;j<orgs.size();j++){
                        if(orgs.get(j).getOrganization_name().equals(organization_name)){
                            orgs.remove(j);
                            break;
                        }
                    }
                    File orgFile= new File("src\\main\\java\\com\\blockchain\\blockchain\\organizations.txt");
                    FileWriter orgFW= new FileWriter(orgFile);
                    BufferedWriter orgBW= new BufferedWriter(orgFW);
                    ObjectMapper mapper= new ObjectMapper();
                    for(int j=0;j<orgs.size();j++){
                        String jsonString= mapper.writeValueAsString(orgs.get(j));
                        orgBW.write(jsonString);
                        orgBW.newLine();
                    }

                    orgBW.close();
                    orgFW.close();

                    System.out.println("Organization "+organization_name+" removed.");
                    break;
                }
                case 3:{
                    File peerOrgs= new File("src\\main\\java\\com\\blockchain\\blockchain\\peerOrgs.txt");
                    BufferedReader reader= new BufferedReader(new FileReader(peerOrgs));
                    String line;
                    LinkedList<String> pOr= new LinkedList<String>();
                    while((line=reader.readLine())!=null){
                        pOr.add(line);
                    }
                    System.out.println(GREEN+"Select the organization:");
                    for(int i=0;i<pOr.size();i++){
                        System.out.println((i+1)+") "+pOr.get(i).split("/")[1]);  
                    }
                    System.out.println(pOr.size()+") Exit");
                    System.out.print("--> "+RESET);
                    int org_choice=in.nextInt();
                    if(org_choice==pOr.size()){
                        break;
                    }
                    String organization_name=pOr.get(org_choice-1);
                    int peer_number=get_num_peers(organization_name.split("/")[1]);
                    organizationMenu(organization_name.split("/")[1],peer_number+1);
                    break;
                }
                
                case 4:{
                    k=false;
                    break;
                }
                default:{
                    System.out.println("ERROR");
                }
            }
        }while(k);
        
        
    }

    /**
     * Adds an organization to an existing channel by modifying configtx.yaml and updating the configuration.
     * @param organization_name the name of the organization to add
     * @param channel_peer the channel peer used for the update
     * @param org_peer the organization's peer
     * @param channelName the channel name
     * @throws IOException in case of I/O errors
     * @throws InterruptedException if execution is interrupted
     */
    private static void addOrgtoChannel(String organization_name, String channel_peer, String org_peer, String channelName) throws IOException, InterruptedException{
        File file = new File(mainDirectory + "/bin/configtx.yaml");
        
        Yaml yaml = new Yaml();
        LoaderOptions options = new LoaderOptions();
        options.setMaxAliasesForCollections(200); 
        Yaml yamlWithOptions = new Yaml(options);

        Map<String,Object> data;
        try (InputStream inputStream = new FileInputStream(file)) {
            data = yamlWithOptions.load(inputStream);
        }

        
        List<Map<String, Object>> orgs = (List<Map<String, Object>>) data.get("Organizations");
        // prendi la prima org (es. OrdererOrg)
        Map<String, Object> peerOrg = orgs.get(0);

        // aggiungi o modifica OrdererEndpoints
        List<String> ordererEndpoints = new ArrayList<>();
        ordererEndpoints.add(orderer_name + ":" + orderer_port);
        //ordererEndpoints.add("orderer2.example.com:" + port2);
        //ordererEndpoints.add("orderer3.example.com:" + port3);
        String path= executeWSLCommandToString("pwd").trim();  
        peerOrg.put("Name", organization_name);
        peerOrg.put("ID", organization_name);
        peerOrg.put("Policies", createOrdererPolicies(false)); //createOrgPolicies("org1"));
        peerOrg.put("MSPDir",path+"/"+mainDirectory+"/organizations/peerOrganizations/"+organization_name+"/msp");
        peerOrg.put("OrdererEndpoints", ordererEndpoints);
        peerOrg.put("AnchorPeers", Arrays.asList(new HashMap<String,Object>() {
        {
            put("Host", org_peer);
            put("Port", 7051+((get_num_org()+get_num_other_peers(organization_name)-1)*1000));
        }
        }
        ));

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
        
        executeWSLCommand(
            "docker exec " + channel_peer + " bash -c '" +
            "export CORE_PEER_MSPCONFIGPATH=/etc/hyperledger/fabric/mspAdmin && " +
            "peer channel fetch config " + channelName + "_block.pb " +
            "-o " + orderer_name + ":" + orderer_port + " " +
            "--channelID " + channelName + " " +
            "--tls --cafile /etc/hyperledger/fabric/tls/ca-chain.pem'"
        );
        executeWSLCommand(
            "docker cp " + channel_peer +
            ":/etc/hyperledger/fabric/" + channelName + "_block.pb " + mainDirectory+"/bin/"
        );
        
        executeWSLCommand(
            "cd " + mainDirectory + "/bin && " +
            "./configtxlator proto_decode " +
            "--input " + channelName + "_block.pb --type common.Block | " +
            "jq '.data.data[0].payload.data.config' > config.json"
        );

        executeWSLCommand(
            "cd " + mainDirectory + "/bin && cp config.json config_original.json"
        );

        executeWSLCommand("cd "+mainDirectory+"/bin &&"+
            "./configtxgen -printOrg "+organization_name+" > "+organization_name+".json"
        );

        executeWSLCommand("cd "+mainDirectory+"/bin && "+
            "jq 'del(.. | .Endpoints?, .Endpoint?)' "+organization_name+".json > "+organization_name+"_no_endpoints.json "
        );

        // 4️⃣ Aggiungere nuova org
        executeWSLCommand(
            "cd " + mainDirectory + "/bin && jq '.channel_group.groups.Application.groups += " +
            "{\"" + organization_name + "\": " +
            "input}' config.json " + organization_name + "_no_endpoints.json > modified_config.json"
        );  

        // 5️⃣ Encode proto
        executeWSLCommand(
            "cd " + mainDirectory + "/bin && " +
            "./configtxlator proto_encode --input config_original.json " +
            "--type common.Config --output original_config.pb"
        );

        executeWSLCommand(
            "cd " + mainDirectory + "/bin && " +
            "./configtxlator proto_encode --input modified_config.json " +
            "--type common.Config --output modified_config.pb"
        );

        // 6️⃣ Compute update
        executeWSLCommand(
            "cd " + mainDirectory + "/bin && " +
            "./configtxlator compute_update " +
            "--channel_id " + channelName + " " +
            "--original original_config.pb " +
            "--updated modified_config.pb " +
            "--output org_update.pb"
        );

        // 7️⃣ Decode update
        executeWSLCommand(
            "cd " + mainDirectory + "/bin && " +
            "./configtxlator proto_decode --input org_update.pb " +
            "--type common.ConfigUpdate > org_update.json"
        );

        // 8️⃣ Wrap in envelope
        executeWSLCommand(
            "cd " + mainDirectory + "/bin && " +
            "bash -c 'cat <<\"EOF\" > org_update_in_envelope.json\n" +
            "{\n" +
            "  \\\"payload\\\": {\n" +
            "    \\\"header\\\": {\n" +
            "      \\\"channel_header\\\": {\n" +
            "        \\\"channel_id\\\": \\\"" + channelName + "\\\",\n" +
            "        \\\"type\\\": 2\n" +
            "      }\n" +
            "    },\n" +
            "    \\\"data\\\": {\n" +
            "      \\\"config_update\\\":\n" +
            "$(cat "+mainDirectory+"/bin/org_update.json)\n" +
            "    }\n" +
            "  }\n" +
            "}\n" +
            "EOF'"
        );

        // 9️⃣ Encode envelope
        executeWSLCommand(
            "cd " + mainDirectory + "/bin && " +
            "./configtxlator proto_encode " +
            "--input org_update_in_envelope.json " +
            "--type common.Envelope --output org_update_in_envelope.pb"
        );

        executeWSLCommand("docker cp " + mainDirectory + "/bin/org_update_in_envelope.pb " +
            channel_peer + ":/etc/hyperledger/fabric/"
        );

        // 🔟 Channel update
        executeWSLCommand("cd " + mainDirectory + " && " +
            "docker exec " + channel_peer + " bash -c '" +
            "export CORE_PEER_MSPCONFIGPATH=/etc/hyperledger/fabric/mspAdmin && " +
            "peer channel update " +
            "-f /etc/hyperledger/fabric/org_update_in_envelope.pb " +
            "-c " + channelName + " " +
            "-o " + orderer_name + ":" + orderer_port + " " +
            "--tls --cafile /etc/hyperledger/fabric/tls/ca-chain.pem'"
        );

    }

    /**
     * Counts the number of peers for a given organization.
     * @param organization_name the name of the organization
     * @return the number of peers in the organization
     * @throws IOException in case of I/O errors
     */
    private static int get_num_peers(String organization_name) throws IOException{
        LinkedList<organization> organizations_creted= getOrganizationsCreated();
        int num_peers=0;
        for(int j=0;j<organizations_creted.size();j++){
            if(organizations_creted.get(j).getOrganization_name().equals(organization_name)){
                num_peers=organizations_creted.get(j).getPeers().size();
                break;
            }   
        }
        return num_peers;
    }

    /**
     * Removes a channel from the channels.txt file.
     * @param channel_name the name of the channel to remove
     * @throws IOException in case of I/O errors
     */
    private static void removeChannelFromFile(String channel_name) throws IOException{
        File channels= new File("src\\main\\java\\com\\blockchain\\blockchain\\channels.txt");
        BufferedReader reader= new BufferedReader(new FileReader(channels));
        String line;
        LinkedList<String> chs= new LinkedList<String>();
        while((line=reader.readLine())!=null){
            chs.add(line);
        }
        reader.close();
        FileWriter fw = new FileWriter(channels);
        for(int i=0;i<chs.size();i++){
            if(chs.get(i).equals(channel_name)==false){
                fw.write(mainDirectory+"/"+chs.get(i)+"\n");
            }
        }
        fw.close();
    }

    /**
     * Displays the organization menu for managing peers and channels.
     * @param organization_name the name of the organization
     * @param peer_number the number of peers in the organization
     * @throws IOException in case of I/O errors
     * @throws InterruptedException if execution is interrupted
     */
    private static void organizationMenu(String organization_name, int peer_number) throws IOException, InterruptedException{
        Scanner in= new Scanner(System.in);
        boolean k=true;
        do{
            System.out.println(GREEN+"-------- ORGANIZATION "+organization_name+" MENU --------");
            System.out.println("1) Add a new peer to an existing channel");
            System.out.println("2) Remove peer");
            System.out.println("3) Back to main menu");
            System.out.print("--> "+RESET);
            int risp= in.nextInt();
            switch (risp) {
                case 1:{
                    File channels= new File("src\\main\\java\\com\\blockchain\\blockchain\\channels.txt");
                    BufferedReader reader= new BufferedReader(new FileReader(channels));
                    System.out.print(GREEN+"Peer name to add: "+RESET);
                    String peer_name=in.next()+"."+organization_name;

                    System.out.print(GREEN+"Select a channel: "+RESET);
                    String line;
                    int i=1;
                    System.out.println(GREEN);
                    LinkedList<String> chs= new LinkedList<String>();
                    while((line=reader.readLine())!=null){
                        System.out.println(i+")"+line.split("/")[1]);
                        chs.add(line.split("/")[1]);
                        i++;
                    }
                    System.out.print("--> "+RESET);
                    int channel_risp=in.nextInt();
                    String channel_name=chs.get(channel_risp-1);
                    reader.close();
                    executeWSLCommand("cd "+mainDirectory+"/organizations/peerOrganizations/"+organization_name+"/peers &&"
                                + "mkdir "+peer_name+" &&"
                                + "cd "+peer_name+" &&"
                                + "mkdir msp tls");
                    
                    create_msp_tls_certificate(organization_name,peer_name,true);
                    executeWSLCommand("mkdir "+mainDirectory+"/organizations/peerOrganizations/"+organization_name+"/users");
                    executeWSLCommand("mkdir "+mainDirectory+"/organizations/peerOrganizations/"+organization_name+"/users/Admin@"+organization_name+" "
                            + "mkdir "+mainDirectory+"/organizations/peerOrganizations/"+organization_name+"/users/Admin@"+organization_name+"/msp");
                    createConfig_yaml("organizations/peerOrganizations/"+organization_name+"/msp", organization_name);
                    createConfig_yaml("organizations/peerOrganizations/"+organization_name+"/peers/"+peer_name+"/msp", organization_name);
                    copy_peer_bin(peer_name, organization_name);
                    LinkedList<organization> organizations_creted= getOrganizationsCreated();
                    int peer_num=0;
                    for(int j=0;j<organizations_creted.size();j++){
                        if(organizations_creted.get(j).getOrganization_name().equals(organization_name)){
                            organizations_creted.get(j).addPeer(peer_name, channel_name);
                            peer_num=organizations_creted.get(j).getPeers().size();
                            break;
                        }   
                    }


                    configure_peer_core(peer_name, first_peer, 7051+((get_num_org()+get_num_other_peers(organization_name)-1)*1000),organization_name,peer_num, channel_name);
                    executeWSLCommand("docker exec " + peer_name + " bash -c '"
                                    + "peer channel fetch 0 "+channel_name+"_block.pb -o " + get_orderer_name(channel_name) + ":" + get_orderer_port(channel_name) + " -c "+channel_name+" --tls --cafile "+(intermediate ? "/etc/hyperledger/fabric/tls/ca-chain.pem" : "/etc/hyperledger/fabric/tls/tls-ca-cert.pem")+"'");
                    executeWSLCommand("docker exec " + peer_name + " bash -c '"
                                    + "export CORE_PEER_MSPCONFIGPATH=/etc/hyperledger/fabric/mspAdmin && "
                                    + "peer channel join -b "+channel_name+"_block.pb'");
                                    
                    executeWSLCommand(
                        "cd "+mainDirectory+" && " +
                            "export CORE_PEER_LOCALMSPID="+org_name+" && " +
                            "export CORE_PEER_MSPCONFIGPATH=$PWD/"+mainDirectory+"/organizations/peerOrganizations/"+org_name+"/msp && " +
                            "export CORE_PEER_ADDRESS="+ peer_name +":"+(7051+((get_num_org()+get_num_other_peers(org_name)-1)*1000)+(peer_number*1000))+" && " +
                            "export CORE_PEER_TLS_ENABLED=true && "+
                            "export FABRIC_CFG_PATH=$PWD/"+mainDirectory+"/peers_bin/"+ peer_name +"/config/ && " +      
                            "./peer lifecycle chaincode package mycc.tar.gz " +
                            "--path $PWD/"+mainDirectory+"/atcc " +
                            "--lang golang " +
                            "--label mycc_1.0" );
                    //Install chaincode

                    executeWSLCommand("docker cp $PWD/"+mainDirectory+"/mycc.tar.gz "+peer_name+":/etc/hyperledger/fabric");
                    executeWSLCommand("cd "+mainDirectory+" &&"
                        + "docker exec " + peer_name + " bash -c '"+
                        "export CORE_PEER_MSPCONFIGPATH=/etc/hyperledger/fabric/mspAdmin && " +
                        "peer lifecycle chaincode install /etc/hyperledger/fabric/mycc.tar.gz'");

                    //update del file organizations.txt
                    try(FileWriter filew= new FileWriter("src\\main\\java\\com\\blockchain\\blockchain\\organizations.txt", false);
                        BufferedWriter bw= new BufferedWriter(filew)){
                        ObjectMapper mapper= new ObjectMapper();
                        for(int j=0;j<organizations_creted.size();j++){
                            String jsonString= mapper.writeValueAsString(organizations_creted.get(j));
                            bw.write(jsonString);
                            bw.newLine();
                        }
                    }
                    break;
                }
                case 2:{
                    LinkedList<organization> organizations_creted= getOrganizationsCreated();
                    LinkedList<String> peers_names=new LinkedList<String>();
                    System.out.print(GREEN+"Select the peer to remove: \n");
                    for(int i=0;i<organizations_creted.size();i++){
                        if(organizations_creted.get(i).getOrganization_name().equals(organization_name)){
                            if(organizations_creted.get(i).getPeers().size()==0){
                                System.out.println(RED+"No peers to remove."+RESET);
                                return;
                            }else{
                                peers_names= organizations_creted.get(i).getPeers();
                            }
                        }
                    }

                    for(int i=0;i<peers_names.size();i++){
                        System.out.println((i+1)+") "+peers_names.get(i));
                    }
                    System.out.print("--> "+RESET);
                    int peer_choice=in.nextInt();
                    String peer_name=peers_names.get(peer_choice-1);
                    String check="";
                    do{
                        executeWSLCommand("rm -rf "+mainDirectory+"/organizations/peerOrganizations/"+organization_name+"/peers/"+peer_name);
                        if(check.length()>0){
                            Thread.sleep(1000);
                        }                                                       
                        check=executeWSLCommandLS(mainDirectory+"/organizations/peerOrganizations/"+organization_name+"/peers");
                    }while(check.contains(peer_name+" "));
                    

                    do{
                        executeWSLCommand("rm -rf "+mainDirectory+"/peers_bin/"+peer_name);
                        if(check.length()>0){
                            Thread.sleep(1000);
                        }                                                       
                        check=executeWSLCommandLS(mainDirectory+"/peers_bin/");
                    }while(check.contains(peer_name+" "));
                    
                    executeWSLCommand("docker rm -f "+peer_name);
                    System.out.println("Peer "+peer_name+" removed.");

                    removeServiceFromDockerCompose(peer_name);
                    LinkedList<String> updated_peers= new LinkedList<String>();
                    for(int i=0;i<peers_names.size();i++){
                        if(peers_names.get(i).equals(peer_name)==false){
                            updated_peers.add(peers_names.get(i));
                        }
                    }
                    for(int j=0;j<organizations_creted.size();j++){
                        if(organizations_creted.get(j).getOrganization_name().equals(organization_name)){
                            organizations_creted.get(j).setPeers(updated_peers);
                            break;
                        }   
                    }
                    //update del file organizations.txt
                    try(FileWriter filew= new FileWriter("src\\main\\java\\com\\blockchain\\blockchain\\organizations.txt", false);
                        BufferedWriter bw= new BufferedWriter(filew)){
                        ObjectMapper mapper= new ObjectMapper();
                        for(int j=0;j<organizations_creted.size();j++){
                            String jsonString= mapper.writeValueAsString(organizations_creted.get(j));
                            bw.write(jsonString);
                            bw.newLine();
                        }
                    }
                    break;
                }
                case 3:{
                    k=false;
                    break;
                }
            
                default:
                    System.out.println(RED+"ERROR"+RESET);
                    break;
            }
        }while(k);
    }

    /**
     * Retrieves the orderer name associated with a given channel.
     * @param channel_name the name of the channel
     * @return the orderer name for the channel
     */
    private static String get_orderer_name(String channel_name){
        File orderers= new File("src\\main\\java\\com\\blockchain\\blockchain\\orderers.txt");
        try{
            BufferedReader reader= new BufferedReader(new FileReader(orderers));
            String line;
            while((line=reader.readLine())!=null){
                if(line.contains(channel_name)){
                    return line.split("/")[1];
                }
            }
            reader.close();
        }catch(Exception e){
            System.err.println(e.toString());
        }
        return null;
    }

    /**
     * Retrieves the orderer port associated with a given channel.
     * @param channel_name the name of the channel
     * @return the orderer port for the channel
     */
    private static int get_orderer_port(String channel_name){
        File orderers= new File("src\\main\\java\\com\\blockchain\\blockchain\\orderers.txt");
        try{
            BufferedReader reader= new BufferedReader(new FileReader(orderers));
            String line;
            while((line=reader.readLine())!=null){
                if(line.contains(channel_name)){
                    return Integer.parseInt(line.split("/")[2]);
                }
            }
            reader.close();
        }catch(Exception e){
            System.err.println(e.toString());
        }
        return -1;
    }

    /**
     * Removes a service from the docker-compose.yaml file.
     * @param name the name of the service to remove
     * @throws FileNotFoundException if the file is not found
     * @throws IOException in case of I/O errors
     */
    private static void removeServiceFromDockerCompose(String name) throws FileNotFoundException, IOException{
        String path=executeWSLCommandToString("echo $(pwd)");
        executeWSLCommand("cp "+path+"/"+mainDirectory+"/docker-compose.yaml "+path+"/src/main/java/com/blockchain/blockchain/docker-compose.yaml");
        executeWSLCommand("rm "+path+"/"+mainDirectory+"/docker-compose.yaml");
        File inputFile = new File("src\\main\\java\\com\\blockchain\\blockchain\\docker-compose.yaml");
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);

        Yaml yaml= new Yaml(options);

        try(InputStream inputStream= new FileInputStream(inputFile)){
            Map<String, Object> data = yaml.load(inputStream);
            Map<String, Object> services = (Map<String, Object>) data.get("services");
            if(services.containsKey(name)){
                services.remove(name);
            }
            try(FileWriter writer= new FileWriter(inputFile)){
                yaml.dump(data, writer);
            }catch(Exception e){
                System.err.println("Error updating docker-compose.yaml");
            }
        }
        executeWSLCommand("mv "+path+"/src/main/java/com/blockchain/blockchain/docker-compose.yaml "+path+"/"+mainDirectory+"/docker-compose.yaml");
    }

    /**
     * Counts the number of orderers in the system.
     * @return the total number of orderers
     */
    private static int get_num_orderers(){
        int num_orderer=0;
        try{
            File orgs_file= new File("src\\main\\java\\com\\blockchain\\blockchain\\orderers.txt");
            BufferedReader reader= new BufferedReader(new FileReader(orgs_file));
            String line;
            while((line=reader.readLine())!=null){
                num_orderer++;
            }
            reader.close();
        }catch(Exception e){
            System.err.println(e.toString());
        }
        return num_orderer;
    }

    

    private static LinkedList<organization> getOrganizationsCreated() throws IOException{
        LinkedList<organization> organizations_creted= new LinkedList<organization>();
        File orgs_file= new File("src\\main\\java\\com\\blockchain\\blockchain\\organizations.txt");
        BufferedReader reader= new BufferedReader(new FileReader(orgs_file));
        String line;
        ObjectMapper mapper= new ObjectMapper();
        while((line=reader.readLine())!=null){
            organization org= mapper.readValue(line, organization.class);
            organizations_creted.add(org);
        }
        reader.close();
        return organizations_creted;
    }

    /**
     * Updates the configtx.yaml file with the current organizations and orderers.
     */
    private static void updateConfigtx_yaml(){
        File configtx_file= new File("src\\main\\java\\com\\blockchain\\blockchain\\configtx.yaml");
        Yaml yaml= new Yaml();
        try{
            Map<String, Object> data= yaml.load(new FileReader(configtx_file));
            Map<String, Object> profiles= (Map<String, Object>) data.get("Profiles");
            Map<String, Object> sampleAppChannelEtcdRaft= (Map<String, Object>) profiles.get("SampleAppChannelEtcdRaft");
            Map<String, Object> orderer= (Map<String, Object>) sampleAppChannelEtcdRaft.get("Orderer");
            Map<String, Object> etcdRaft= (Map<String, Object>) orderer.get("EtcdRaft");
            ArrayList<Map<String, Object>> consenters= (ArrayList<Map<String, Object>>) etcdRaft.get("Consenters");
            consenters.clear();
            String path=executeWSLCommandToString("echo $(pwd)");
            Map<String,Object> host = new HashMap<>();
            host.put("Host", orderer_name);
            host.put("port", orderer_port);
            host.put("ServerTLSCert", path+"/"+mainDirectory + "/organizations/ordererOrganizations/Consenters/orderers/"+orderer_name+"/tls/signcerts/cert.pem");
            consenters.add(host);

            DumperOptions options = new DumperOptions();
            options.setIndent(2);
            options.setPrettyFlow(true);
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            Yaml yamlWriter = new Yaml(options);

            try (FileWriter writer = new FileWriter(configtx_file)) {
                yamlWriter.dump(data, writer);
            }catch(Exception e){
                System.err.println("Error updating configtx.yaml");
            }
        }catch(Exception e){
            System.err.println(e.toString());
        }
    }

    /**
     * Creates a new channel with the specified organization and peer.
     * @param organization_name the name of the organization
     * @param peer_name the name of the peer
     * @param channel_name the name of the channel to create
     * @throws IOException in case of I/O errors
     */
    private static void createChannel(String organization_name, String peer_name, String channel_name) throws IOException{
        //Salviamo il nome del canale su File
        File channels= new File("src\\main\\java\\com\\blockchain\\blockchain\\channels.txt");
        FileWriter fw = new FileWriter(channels, true);
        fw.write(mainDirectory+"/"+channel_name+"/"+peer_name+"\n");
        fw.flush();
        
        //copia della cartella intermediateca nel consenter org MSP
        
        //updateConfigtx_yaml();
        executeWSLCommand("cd "+mainDirectory+"/bin && "+
        "./configtxgen -configPath $(pwd)/"+mainDirectory+"/bin  -profile SampleAppChannelEtcdRaft -channelID "+channel_name+" -outputCreateChannelTx ./"+channel_name+".tx");
        executeWSLCommand("docker cp $PWD/"+mainDirectory+"/bin/" + channel_name + ".tx  "+peer_name+":/etc/hyperledger/fabric/");
        String dockerCmd =
            "docker exec " + peer_name + " bash -c '"
            + "export CORE_PEER_MSPCONFIGPATH=/etc/hyperledger/fabric/mspAdmin && "
            + "export CORE_PEER_TLS_ENABLED=true && "
            + "export CORE_PEER_ADDRESS="+peer_name+":"+(7051+((get_num_org()+get_num_other_peers(organization_name)-1)*1000))+" &&"
            + "export CORE_PEER_TLS_CERT_FILE=/etc/hyperledger/fabric/tls/signcerts/cert.pem && "
            + "export CORE_PEER_TLS_KEY_FILE=/etc/hyperledger/fabric/tls/keystore/" + executeWSLCommandToString("ls "+mainDirectory+"/organizations/peerOrganizations/"+organization_name+"/peers/"+peer_name+"/tls/keystore/ | grep _sk").trim() + " && "
            + "peer channel create -c " + channel_name 
            + " -f /etc/hyperledger/fabric/" + channel_name 
            + ".tx -o " + orderer_name + ":" + orderer_port + " "
            + "--tls --cafile"+(intermediate? " /etc/hyperledger/fabric/tls/ca-chain.pem ":" /etc/hyperledger/fabric/tls/tls-ca-cert.pem ")
            + "--outputBlock /etc/hyperledger/fabric/" + channel_name + "_block.pb'";
        executeWSLCommand(dockerCmd);
        executeWSLCommand("docker cp "+peer_name+":/etc/hyperledger/fabric/"+channel_name+"_block.pb "+mainDirectory+"/bin/");
        

        

        
            
        
    }
    
    /**
     * Performs a channel update to add/update the organization or peer.
     * @param channel_name the name of the channel
     * @param organization_name the name of the organization to update
     * @param peer_name the name of the peer involved
     * @throws IOException in case of I/O errors
     */
    private static void channelUpdate(String channel_name, String organization_name, String peer_name) throws IOException{
        executeWSLCommand("export CORE_PEER_LOCALMSPID=Org1MSP && "
                + "export CORE_PEER_MSPCONFIGPATH=$PWD/"+mainDirectory+"/organizations/peerOrganizations/"+organization_name+"/users/Admin@"+organization_name+"/msp && "
                + "export CORE_PEER_ADDRESS=localhost:"+(7051+((get_num_org()+get_num_other_peers(organization_name)-1)*1000))+" && "
                + "export CORE_PEER_TLS_ROOTCERT_FILE=$PWD/"+mainDirectory+"/organizations/peerOrganizations/"+organization_name+"/peers/"+peer_name+"/tls/tlscacerts/tls-127-0-0-1-"+(intermediate?"7056":"7054")+".pem && "
                + "export FABRIC_CFG_PATH=peers_bin/"+peer_name+"/config && "
                + "cd "+mainDirectory+" && "
                + "./peer channel fetch config config_block.pb -o " + orderer_name + ":" + orderer_port + " "
                + "-c "+channel_name+" "
                + "--tls --cafile $PWD/"+mainDirectory+"/organizations/ordererOrganizations/Consenters/orderers/" + orderer_name + "/tls/tlscacerts/tls-127-0-0-1-"+(intermediate?"7056":"7054")+".pem ");
        
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
    
    /**
     * Downloads the orderer binaries (if not present) and prepares the local structure.
     */
    public static void download_orderer_bin(){
        if(Files.exists(Paths.get(mainDirectory+"/peers_bin/original_file"))){
            executeWSLCommand("cp -r "+mainDirectory+"/peers_bin/original_file "+mainDirectory+"/orderers_bin");
        }else{
            //download dei bin
            executeWSLCommand("cd "+mainDirectory+"/orderers_bin &&"
                    + "mkdir original_file");
            executeWSLCommandWithProgress("cd "+mainDirectory+"/orderers_bin/original_file &&"
                    + "aria2c https://github.com/hyperledger/fabric/releases/download/v3.1.1/hyperledger-fabric-linux-amd64-3.1.1.tar.gz &&"
                    + "tar -xvzf hyperledger-fabric-linux-amd64-3.1.1.tar.gz &&"
                    + "rm hyperledger-fabric-linux-amd64-3.1.1.tar.gz");
        }
        
    }
    
    /**
     * Copies the orderer binary files to the specified orderer folder.
     * @param orderer_name the name of the orderer (e.g., orderer1.example.com)
     */
    private static void copy_orderer_bin(String orderer_name){
        executeWSLCommand("cd "+mainDirectory+"/orderers_bin &&"
                + "mkdir "+orderer_name);
        
        executeWSLCommand("cp -r "+mainDirectory+"/orderers_bin/original_file/* "+mainDirectory+"/orderers_bin/"+orderer_name);
    }
    
    /**
     * Scarica i binari del peer e li prepara per l'uso locale.
     */
    /**
     * Downloads the peer binaries if not already present, or copies from an existing original file directory.
     * Downloads Hyperledger Fabric peer binaries from GitHub and extracts them.
     */
    public static void download_peer_bin(){
        if(Files.exists(Paths.get(mainDirectory+"/orderers_bin/original_file"))){
            executeWSLCommand("cp -r "+mainDirectory+"/orderers_bin/original_file "+mainDirectory+"/peers_bin");
        }else{
            //download dei bin
            executeWSLCommand("cd "+mainDirectory+"/peers_bin &&"
                    + "mkdir original_file");
            executeWSLCommandWithProgress("cd "+mainDirectory+"/peers_bin/original_file &&"
                    + "aria2c https://github.com/hyperledger/fabric/releases/download/v3.1.1/hyperledger-fabric-linux-amd64-3.1.1.tar.gz &&"
                    + "tar -xvzf hyperledger-fabric-linux-amd64-3.1.1.tar.gz &&"
                    + "rm hyperledger-fabric-linux-amd64-3.1.1.tar.gz");
        }
        
    }
    
    /**
     * Copies the peer binaries to the specified peer's directory for the given organization.
     * @param peer_name the name of the peer (e.g., peer0.org1.example.com)
     * @param org_name the name of the organization
     */
    private static void copy_peer_bin(String peer_name, String org_name){
        executeWSLCommand("cd "+mainDirectory+"/peers_bin &&"
                + "mkdir "+peer_name);
        
        executeWSLCommand("cp -r "+mainDirectory+"/peers_bin/original_file/* "+mainDirectory+"/peers_bin/"+peer_name);
    }
    
    
    /**
     * Configures the core.yaml file for the specified peer.
     * Sets up peer identity, network settings, gossip, TLS, BCCSP, ledger, and operations configurations.
     * @param peer_name the name of the peer
     * @param second_peer the name of the second peer for bootstrap (can be null)
     * @param second_peer_port the port of the second peer
     * @param org_name the name of the organization
     * @param peer_number the peer number for port calculation
     * @param existing_channel the name of the existing channel (can be null)
     * @throws FileNotFoundException if the core.yaml file is not found
     * @throws IOException in case of I/O errors
     */
    public static void configure_peer_core(String peer_name, String second_peer, int second_peer_port, String org_name, int peer_number,String existing_channel) throws FileNotFoundException, IOException{
        Scanner in= new Scanner(System.in);
        System.out.println(GREEN+"------------ PEER "+peer_name+" CONFIGURATION ------------"+RESET);
        File peer_config=new File(""+ mainDirectory +"/peers_bin/"+peer_name+"/config/core.yaml");
        Yaml yaml= new Yaml();
        Map<String, Object> data= yaml.load(new FileReader(peer_config));
        Map<String, Object> peer= (Map<String, Object>) data.get("peer");
        
        //Cancelliamo l'handler TimeWindowCheck (non supportato)
        Map<String, Object> handlers = (Map<String, Object>) peer.get("handlers");
        List<Map<String, String>> authFilters = (List<Map<String, String>>) handlers.get("authFilters");
        authFilters.removeIf(filter -> "TimeWindowCheck".equals(filter.get("name")));

        //ID
        peer.put("id", peer_name+"MSP");
        
        //LOCAL MSP ID
        peer.put("localMspId", org_name);
        //NETWORK ID
        peer.put("networkId", "dev");
        
        //LISTEN ADDRESS   
        String add="0.0.0.0";
        peer.put("listenAddress",add+":"+(7051+((get_num_org()+get_num_other_peers(org_name)-1)*1000)+(peer_number*1000)));
        
        //ADDRESS
        peer.put("address",peer_name+":"+(7051+((get_num_org()+get_num_other_peers(org_name)-1)*1000)+(peer_number*1000)));
        
        //CHAINCODE LISTEN ADDRESS
        peer.put("chaincodeListenAddress",add+":7052");
        
        //CHAINCODE ADDRESS
        peer.put("chaincodeAddress", peer_name+":7052");
        
        //MSP CONFIG PATH
        String path=executeWSLCommandToString("echo $(pwd)");
        peer.put("mspConfigPath","/etc/hyperledger/fabric/msp");
        
        //LOCAL MSP (dell'organizzazione)
        peer.put("localMspId",org_name);
        
        //FILE SYSTEM PATH
        peer.put("fileSystemPath","/var/hyperledger/production/"+peer_name);
        
        //KEEPALIVE
        Map<String, Object> keepalive = (Map<String, Object>) peer.get("keepalive");
        keepalive.put("interval", "300s");
        keepalive.put("timeout", "20s");
        keepalive.put("minInterval", "60s");

        //GOSSIP
        Map<String, Object> gossip=(Map<String, Object>) peer.get("gossip");

        //System.out.println(GREEN+"Gossip configuration for " + peer_name+RESET);

        // Bootstrap
        String bootstrap;
        if(existing_channel!=null){
            bootstrap="" ;
        }else{
            if(second_peer!=null){
                bootstrap= second_peer +":7051";
            }else{
                bootstrap= "";
            }
        }
        
        
        
        
        
        
        
        gossip.put("bootstrap", bootstrap);
        gossip.put("useLeaderElection", true);
        gossip.put("orgLeader", false);
        // Endpoint interno
        String peerEndpoint = peer_name +":"+(7051+((get_num_org()+get_num_other_peers(org_name)-1)*1000)+(peer_number*1000));
        gossip.put("endpoint", peerEndpoint);

        // Endpoint esterno
        gossip.put("externalEndpoint", peerEndpoint);
        
        
        

        // Gossip state transfer
        Map<String, Object> state = new LinkedHashMap<>();
        //System.out.print(GREEN+"Enable gossip state transfer? (y/n):  "+RESET);
        boolean stateEnabled = true;//in.next().equals("y");
        state.put("enabled", stateEnabled);
        gossip.put("state", state); 
        
        

        // pvtData.implicitCollectionDisseminationPolicy
        Map<String, Object> pvtData = new LinkedHashMap<>();
        Map<String, Object> implicitPolicy = new LinkedHashMap<>();
        //System.out.print(GREEN+"requiredPeerCount (Minimum peers for private data dissemination): "+RESET);
        //int requiredPeerCount = in.nextInt();
        implicitPolicy.put("requiredPeerCount", 0);

        //System.out.print(GREEN+"maxPeerCount (Maximum peers for private data dissemination): "+RESET);
        //int maxPeerCount = in.nextInt();
        implicitPolicy.put("maxPeerCount", 1);

        pvtData.put("implicitCollectionDisseminationPolicy", implicitPolicy);
        gossip.put("pvtData", pvtData);
        
        
        
        //TLS
        Map<String, Object> tls= (Map<String, Object>) peer.get("tls");
        
        // Abilita TLS
        //System.out.print(GREEN+"Enable TLS (y/n): "+RESET);
        boolean tlsEnabled = true;//in.next().equals("n");
        tls.put("enabled", tlsEnabled);
        
        

        // Abilita mutual TLS (clientAuthRequired)
        boolean clientAuthRequired = false;
        tls.put("clientAuthRequired", clientAuthRequired);

        // Percorso base per i file TLS
        String tlsBasePath = mainDirectory + "/organizations/peerOrganizations/" + org_name +
            "/peers/" + peer_name + "/tls";

        Map<String, Object> clientRootCAs = new LinkedHashMap<>();
        clientRootCAs.put("files", List.of("/etc/hyperledger/fabric/tls/tlscacerts/tls-127-0-0-1-"+(intermediate?"7056":"7054")+".pem"));
        tls.put("clientRootCAs", clientRootCAs);
        // File del certificato TLS
        Map<String, Object> cert = new LinkedHashMap<>();
        cert.put("file", "/etc/hyperledger/fabric/tls/signcerts/cert.pem");
        tls.put("cert", cert);
        Map<String, Object> clientCert = new LinkedHashMap<>();
        clientCert.put("file", "/etc/hyperledger/fabric/tls/signcerts/cert.pem");
        tls.put("clientCert", clientCert); 
        String serverKey=executeWSLCommandToString("ls "+tlsBasePath+"/keystore/ | grep '_sk'");
        // File della chiave privata TLS
        Map<String, Object> key = new LinkedHashMap<>();
        key.put("file", "/etc/hyperledger/fabric/tls/keystore/"+serverKey);
        tls.put("key", key);
        Map<String, Object> clientKey = new LinkedHashMap<>();
        clientKey.put("file", "/etc/hyperledger/fabric/tls/keystore/"+serverKey);
        tls.put("clientKey", clientKey);
        // Root cert (per connessioni in uscita)
        Map<String, Object> rootcert = new LinkedHashMap<>();
        rootcert.put("file", "/etc/hyperledger/fabric/tls/tlscacerts/tls-127-0-0-1-"+(intermediate?"7056":"7054")+".pem");
        tls.put("rootcert", rootcert);
        Map<String, Object> clientRootcert = new LinkedHashMap<>();
        clientRootcert.put("file", "/etc/hyperledger/fabric/tls/tlscacerts/tls-127-0-0-1-"+(intermediate?"7056":"7054")+".pem");
        tls.put("clientRootcert", clientRootcert);

        // clientRootCAs.files (solo se mutual TLS abilitato)
        if (clientAuthRequired) {
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

            System.out.print(GREEN+"Enter PKCS11 library path (e.g., /usr/local/lib/softhsm/libsofthsm2.so): "+RESET);
            in.nextLine(); // consume newline
            String library = in.nextLine().trim();
            pkcs11.put("Library", library);

            System.out.print(GREEN+"Enter PKCS11 token label: "+RESET);
            String label = in.nextLine().trim();
            pkcs11.put("Label", label);

            System.out.print(GREEN+"Enter PKCS11 user PIN: "+RESET);
            String pin = in.nextLine().trim();
            pkcs11.put("Pin", pin);

            pkcs11.put("Hash", "SHA2");
            pkcs11.put("Security", 256);

            bccsp.put("PKCS11", pkcs11);
        }
        
        
        
        

        // Attach to 'chaincode' section of peer
        Map<String, Object> chaincode = (Map<String, Object>) peer.get("chaincode");
        if (chaincode == null) {
            chaincode = new LinkedHashMap<>();
            peer.put("chaincode", chaincode);
        }
        
        //LEDGER
        
        Map<String, Object> ledger= (Map<String, Object>) data.get("ledger");
        Map<String, Object> ledger_state= (Map<String, Object>) ledger.get("state");
        
        // Select state database type
        String stateDb;
       stateDb="goleveldb";
        
        
        
        
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
        operations.put("listenAddress", opAddress + ":"+(7051+((get_num_org()+get_num_other_peers(org_name)-1)*1000)+(peer_number*1000)));

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
        String mspAdminPath=path+"/"+mainDirectory+"/organizations/peerOrganizations/"+org_name+"/users/Admin@"+org_name+"/msp";
        String mspPath=path+"/"+mainDirectory+"/organizations/peerOrganizations/"+org_name+"/peers/"+peer_name+"/msp";
        String cfgPath=path+"/"+mainDirectory+"/peers_bin/"+peer_name+"/config";
        String tlsPath=path+"/"+mainDirectory+"/organizations/peerOrganizations/"+org_name+"/peers/"+peer_name+"/tls";
        String rootPath=path+"/"+mainDirectory+"/fabric-ca-client/tls-root-cert/tls-ca-cert.pem";
        String chainPath=path+"/"+mainDirectory+"/fabric-ca-server-int-ca/ca-chain.pem";
        LinkedList<Integer> ports= new LinkedList<Integer>();
        boolean cDB=false;
        //Aggiunta del peer al file docker-compose.yaml
        add_peer_to_docker(peer_name,org_name,cfgPath,mspAdminPath, mspPath ,tlsPath,rootPath,chainPath,ports, cDB, tlsEnabled, bootstrap, peer_number);
        
        System.out.println("Starting the peer...");
        new peerThread(peer_name, cDB, mainDirectory);
        
        waitForContainer(peer_name);
        
        
    }

    


    
    
    /**
     * Configures the orderer (MSP, TLS, and cluster) for the given organization.
     * @param orderer_name the name of the orderer
     * @param port the port for the orderer
     * @param org_name the name of the organization
     * @param needClusterConfig true if cluster configuration is needed
     * @param channel_name the name of the channel
     * @return the port used by the orderer
     * @throws FileNotFoundException if required files are not found
     * @throws IOException in case of I/O errors
     */
    public static int configure_orderer(String orderer_name, int port, String org_name, boolean needClusterConfig, String channel_name) throws FileNotFoundException, IOException{
        File orderers= new File("src\\main\\java\\com\\blockchain\\blockchain\\orderers.txt");
        FileWriter fw = new FileWriter(orderers, true);
        fw.write(mainDirectory+"/"+orderer_name+"/"+port+"/"+channel_name+"\n");
        fw.flush();
        fw.close();
        Scanner in= new Scanner(System.in);
        System.out.println(GREEN+"------------ ORDERER "+orderer_name+"."+org_name+" CONFIGURATION ------------"+RESET);
        File orderer_config=new File(""+ mainDirectory +"/orderers_bin/"+orderer_name+"/config/orderer.yaml");
        Yaml yaml= new Yaml();
        Map<String, Object> data= yaml.load(new FileReader(orderer_config));
        Map<String, Object> general= (Map<String, Object>) data.get("General");
        
        //rimozione di Backoff e Throttling (chiavi non valide per la versione di fabric utilizzata)
        general.remove("Backoff");
        general.remove("Throttling");
        // LISTEN ADDRESS & PORT
        general.put("ListenAddress", "0.0.0.0");
        
        
        general.put("ListenPort", port);
        
        //TLS
        Map<String, Object> general_tls=(Map<String,Object>)general.get("TLS");
        general_tls.put("Enabled", false);
        general_tls.put("PrivateKey", "/var/hyperledger/orderer/tls/keystore/"+executeWSLCommandToString("ls "+mainDirectory+"/organizations/ordererOrganizations/"+ org_name + "/orderers/" + orderer_name+"/tls/keystore/ | grep '_sk'").trim());

        general_tls.put("Certificate", "/var/hyperledger/orderer/tls/signcerts/cert.pem"); 
        general_tls.put("RootCAs", "/var/hyperledger/orderer/tls/tlscacerts/tls-127-0-0-1-"+(intermediate?"7056":"7054")+".pem");
        if(org_name.equals("Consenters")){
            general_tls.put("ClientAuthRequired", false);
            List<String> clientRootCAs = new ArrayList<>();
            //Copiamo il certificato della root nel percorso dell'orderer
            executeWSLCommand("cp "+mainDirectory+"/fabric-ca-client/tls-root-cert/tls-ca-cert.pem "+mainDirectory+"/organizations/ordererOrganizations/"+org_name+"/orderers/"+orderer_name+"/tls/");
            clientRootCAs.add("/var/hyperledger/orderer/tls/tlscacerts/tls-127-0-0-1-"+(intermediate?"7056":"7054")+".pem");

            general_tls.put("ClientRootCAs", clientRootCAs);
        }else{
            System.out.println(GREEN+"Enable mutual TLS?(y/n)"+RESET);
            if(in.next().equals("y")){
                general_tls.put("ClientAuthRequired", false);
                List<String> clientRootCAs = new ArrayList<>();
                //Copiamo il certificato della root nel percorso dell'orderer
                executeWSLCommand("cp "+mainDirectory+"/fabric-ca-client/tls-root-cert/tls-ca-cert.pem "+mainDirectory+"/organizations/ordererOrganizations/"+org_name+"/orderers/"+orderer_name+"/tls/");
                clientRootCAs.add("/var/hyperledger/orderer/tls/tlscacerts/tls-127-0-0-1-"+(intermediate?"7056":"7054")+".pem");

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
                System.out.println(GREEN+"Use a separate listener for intra-cluster communication? (y/n)"+RESET);
                if (in.next().equals("y")) {


                    general_cluster.put("ListenAddress", "0.0.0.0");


                    general_cluster.put("ListenPort", 7049);

                    String certPath = "$(pwd)/" + mainDirectory + "/organizations/ordererOrganizations/" + org_name + "/orderers/" + orderer_name + "/tls/signcerts/cert.pem";
                    String keyPath = "$(pwd)/" + mainDirectory + "/organizations/ordererOrganizations/" + org_name + "/orderers/" + orderer_name + "/tls/keystore/" + executeWSLCommandToString("ls " + mainDirectory + "/organizations/ordererOrganizations/" + org_name + "/orderers/" + orderer_name + "/tls/keystore/ | grep '_sk'").trim();

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
        general.put("LocalMSPDir", "/var/hyperledger/orderer/msp");
        //LOCAL MSP ID
        general.put("LocalMSPID",org_name);
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
        fileKeyStore.put("KeyStore", "/var/hyperledger/orderer/keys");
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
            Map<String, Object> operations =(Map<String, Object>) data.get("Operations");
            ((Map<String,Object>) operations.get("TLS")).put("Enabled", false);
            ((Map<String,Object>) operations.get("TLS")).put("ClientAuthRequired", false);
        }else{
            System.out.println(GREEN+"Do you want to use the operations service?(y/n)"+RESET);
            if (in.next().equals("y")) {
                Map<String, Object> operations =(Map<String, Object>) data.get("Operations");
                String op_server_add="0.0.0.0";
                port=(9442+(get_num_orderers()));
                do{
                    port++;
                }while(ports_used.contains(port));
                operations.put("ListenAddress", op_server_add+":"+port);

                operations.put("Certificate","/var/hyperledger/orderer/tls/signcerts/cert.pem"); 
                operations.put("PrivateKey","/var/hyperledger/orderer/tls/keystore/"+executeWSLCommandToString("ls "+mainDirectory+"/organizations/ordererOrganizations/"+ org_name + "/orderers/" + orderer_name+"/tls/keystore/ | grep '_sk'").trim());

                System.out.println(GREEN+"Enable mutal TLS between client and server?(y/n)"+RESET);
                if (in.next().equals("y")) {
                    operations.put("ClientAuthRequired", false);
                    List<String> clientRootCAs = new ArrayList<>();


                    clientRootCAs.add("/var/hyperledger/orderer/tls/tlscacerts/tls-127-0-0-1-"+(intermediate?"7056":"7054")+".pem");

                    operations.put("ClientRootCAs", clientRootCAs);
                }
            }
        }
        
        
        
        //METRICS
        Map<String, Object> metrics = (Map<String, Object>) data.get("Metrics");

        //System.out.println("Select metrics provider (prometheus / statsd / disabled):");

        

        metrics.put("Provider", "disabled");

        
        
        //ADMIN
        Map<String, Object> admin =(Map<String, Object>) data.get("Admin");
        Map<String, Object> admin_TLS=(Map<String, Object>) admin.get("TLS");
        admin_TLS.put("Enabled", true);
        admin_TLS.put("Certificate", "/var/hyperledger/orderer/tls/signcerts/cert.pem"); 
        admin_TLS.put("PrivateKey", "/var/hyperledger/orderer/tls/keystore/"+executeWSLCommandToString("ls "+mainDirectory+"/organizations/ordererOrganizations/"+ org_name + "/orderers/" + orderer_name+"/tls/keystore/ | grep '_sk'").trim());
        admin_TLS.put("ClientAuthRequired", false);
        List<String> clientRootCAs = new ArrayList<>();


        clientRootCAs.add("/var/hyperledger/orderer/tls/tlscacerts/tls-127-0-0-1-"+(intermediate?"7056":"7054")+".pem");

        admin_TLS.put("ClientRootCAs", clientRootCAs);
        //CHANNEL PARTECIPATION
        Map<String, Object> channelParticipation = (Map<String, Object>) data.get("ChannelParticipation");
        boolean enabled = false;
        if(org_name.equals("Consenters")){
            enabled=false;
        }else{
           System.out.println(GREEN+"Enable Channel Participation API? (y/n)"+RESET);
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
        
        
        System.out.println("Config updated");
        //Aggiunta dell'orderer al file docker-compose.yaml
        
        if(org_name.equals("Consenters")){
            
            addConsentersDocker();
        }else{
            String path=executeWSLCommandToString("echo $(pwd)");
            String mspPath=path+"/"+mainDirectory+"/organizations/ordererOrganizations/"+org_name+"/orderers/"+orderer_name+"/msp";
            String cfgPath=path+"/"+mainDirectory+"/orderers_bin/"+orderer_name+"/config";
            String tlsPath=path+"/"+mainDirectory+"/organizations/ordererOrganizations/"+org_name+"/orderers/"+orderer_name+"/tls";
            String ledgerPath=path+"/"+mainDirectory+"/organizations/ordererOrganizations/"+org_name+"/orderers/"+orderer_name+"/ledger";
            String keysPath=path+"/"+mainDirectory+"/organizations/ordererOrganizations/"+org_name+"/orderers/"+orderer_name+"/keys";
            LinkedList<Integer> ports= new LinkedList<Integer>();
            ports.add(port);
            add_orderer_to_docker(orderer_name, org_name,cfgPath,mspPath,tlsPath,ledgerPath,keysPath,ports);
        }
        createConfig_yaml("organizations/ordererOrganizations/"+org_name+"/orderers/"+orderer_name+"/msp", org_name);
        
        
        
        //Copia del certificato dell'admin in admincerts
        //executeWSLCommand("cp "+mainDirectory+"/organizations/ordererOrganizations/"+org_name+"/orderers/"+orderer_name+"/msp/signcerts/cert.pem "+mainDirectory+"/ordererOrganizations/"+org_name+"/orderers/"+orderer_name+"/msp/admincerts/");
        //Avvio del container
        new ordererThread(mainDirectory, orderer_name);
        
        //Aspetto che il container si avvi
        waitForContainer(orderer_name);
        
        return port;
    }
    
    
    /**
     * Writes or updates the config.yaml file required by the orderer/peer.
     * @param path the path where the file should be written
     * @param org_name the name of the organization
     */
    private static void createConfig_yaml(String path, String org_name){
        if(intermediate){
            executeWSLCommand("cd "+mainDirectory+" &&"
                + "cat > $(pwd)/"+mainDirectory+"/"+path+"/config.yaml << 'EOF'\n" +
                "NodeOUs:\n" +
                "  Enable: true\n" +
                "  ClientOUIdentifier:\n" +
                "    Certificate: intermediatecerts/ca-cert.pem\n" +
                "    OrganizationalUnitIdentifier: client\n" +
                "  PeerOUIdentifier:\n" +
                "    Certificate: intermediatecerts/ca-cert.pem\n" +
                "    OrganizationalUnitIdentifier: peer\n" +
                "  AdminOUIdentifier:\n" +
                "    Certificate: intermediatecerts/ca-cert.pem\n" +
                "    OrganizationalUnitIdentifier: admin\n" +
                "  OrdererOUIdentifier:\n" +
                "    Certificate: intermediatecerts/ca-cert.pem\n" +
                "    OrganizationalUnitIdentifier: orderer\n"+
                "Name: "+org_name+"\n"+
                "ID: "+org_name+"\n"+
                "EOF");
        }else{
            executeWSLCommand("cd "+mainDirectory+" &&"
                + "cat > $(pwd)/"+mainDirectory+"/"+path+"/config.yaml << 'EOF'\n" +
                "NodeOUs:\n" +
                "  Enable: true\n" +
                "  ClientOUIdentifier:\n" +
                "    Certificate: cacerts/127-0-0-1-7054.pem\n" +
                "    OrganizationalUnitIdentifier: client\n" +
                "  PeerOUIdentifier:\n" +
                "    Certificate: cacerts/127-0-0-1-7054.pem\n" +
                "    OrganizationalUnitIdentifier: peer\n" +
                "  AdminOUIdentifier:\n" +
                "    Certificate: cacerts/127-0-0-1-7054.pem\n" +
                "    OrganizationalUnitIdentifier: admin\n" +
                "  OrdererOUIdentifier:\n" +
                "    Certificate: cacerts/127-0-0-1-7054.pem\n" +
                "    OrganizationalUnitIdentifier: orderer\n"+
                "Name: "+org_name+"\n"+
                "ID: "+org_name+"\n"+
                "EOF");
        }
        
    }
    
    /**
     * Registers and enrolls the organization admin.
     * @param org_name the name of the organization
     * @param peer_org true if the organization is a peer organization
     * @throws IOException in case of I/O errors
     */
    private static void organizationAdminRegistrationEnroll(String org_name, boolean peer_org) throws IOException{
        String name="Admin@"+org_name;
        String psw= "Admin@"+org_name+"Psw";
        
        String org_directory= peer_org? "peerOrganizations":"ordererOrganizations";

        if(intermediate){
            executeWSLCommand("cd " + mainDirectory + "/fabric-ca-client && "
                +"export FABRIC_CA_CLIENT_TLS_SKIPHOSTVERIFY=true && "
                + "./fabric-ca-client register -d "
                + "--id.name " + name + " "
                + "--id.secret " + psw + " "
                + "-u https://127.0.0.1:7056 "
                + "--tls.certfiles $(pwd)/" + mainDirectory + "/fabric-ca-client/tls-root-cert/tls-ca-cert.pem "
                + "--mspdir $(pwd)/"+mainDirectory+"/fabric-ca-client/int-ca/icaadmin/msp " 
                + "--id.type admin");

            executeWSLCommand("cd " + mainDirectory + "/fabric-ca-client && "
                + "./fabric-ca-client enroll "
                + "-u https://" + name + ":" + psw + "@127.0.0.1:7056 "
                + "--mspdir $(pwd)/" + mainDirectory + "/organizations/" + org_directory + "/" + org_name + "/users/" + name + "/msp "
                + "--csr.hosts 'host1' "
                + "--tls.certfiles $(pwd)/" + mainDirectory + "/fabric-ca-client/tls-root-cert/tls-ca-cert.pem");

            if(intermediate){
                executeWSLCommand("cp "+mainDirectory+"/fabric-ca-client/tls-root-cert/tls-ca-cert.pem "+mainDirectory+"/organizations/"+org_directory+"/"+org_name+"/msp/cacerts &&"
                    + "cp "+mainDirectory+"/fabric-ca-server-int-ca/ca-chain.pem "+mainDirectory+"/organizations/"+org_directory+"/"+org_name+"/msp/tlscacerts");
            
            }else{
                executeWSLCommand("cp "+mainDirectory+"/fabric-ca-client/tls-root-cert/tls-ca-cert.pem "+mainDirectory+"/organizations/"+org_directory+"/"+org_name+"/msp/cacerts &&"
                    + "cp "+mainDirectory+"/fabric-ca-client/tls-root-cert/tls-ca-cert.pem "+mainDirectory+"/organizations/"+org_directory+"/"+org_name+"/msp/tlscacerts");
            
            }
            
            
            
            executeWSLCommand("mkdir -p "+mainDirectory+"/organizations/"+org_directory+"/"+org_name+"/msp/intermediatecerts");
            executeWSLCommand("cp -r "+mainDirectory+"/fabric-ca-server-int-ca/ca-cert.pem "+mainDirectory+"/organizations/"+org_directory+"/"+org_name+"/msp/intermediatecerts/");
                        
            executeWSLCommand("mkdir -p "+mainDirectory+"/organizations/"+org_directory+"/"+org_name+"/users/Admin@"+org_name+"/msp/admincerts && "
                    + "cp  "+mainDirectory+"/organizations/"+org_directory+"/"+org_name+"/users/Admin@"+org_name+"/msp/signcerts/* "+mainDirectory+"/organizations/"+org_directory+"/"+org_name+"/users/Admin@"+org_name+"/msp/admincerts/");
            //TLS
            executeWSLCommand("cd "+mainDirectory+"/fabric-ca-client &&"+
                    "./fabric-ca-client enroll "
                    + "-u https://"+name+":"+psw+"@127.0.0.1:7056 "
                    + "--enrollment.profile tls "
                    + "--mspdir $(pwd)/"+mainDirectory+"/organizations/"+org_directory+"/"+org_name+"/users/Admin@"+org_name+"/tls "
                    + "--csr.hosts 'admin'  "
                    + "--tls.certfiles $(pwd)/"+mainDirectory+"/fabric-ca-client/tls-root-cert/tls-ca-cert.pem"
            );

            executeWSLCommand("cp "+mainDirectory+"/organizations/"+org_directory+"/"+org_name+"/users/Admin@"+org_name+"/msp/intermediatecerts/* "+mainDirectory+"/organizations/"+org_directory+"/"+org_name+"/users/Admin@"+org_name+"/msp/intermediatecerts/ca-cert.pem");
        }else{
            executeWSLCommand("cd " + mainDirectory + "/fabric-ca-client &&"
                + "./fabric-ca-client register -d --id.name " + name + " --id.secret " + psw + " "
                + "-u https://localhost:7054 "
                + "--tls.certfiles $(pwd)/"+mainDirectory+"/fabric-ca-client/tls-root-cert/tls-ca-cert.pem "
                + "--mspdir $(pwd)/"+mainDirectory+"/fabric-ca-client/tls-ca/rcaadmin/msp "
                + "--id.type admin ");
            executeWSLCommand("cd "+mainDirectory+"/fabric-ca-client &&"
                    + "./fabric-ca-client enroll -u https://"+name+":"+psw+"@localhost:7054 --mspdir $(pwd)/"+mainDirectory+"/organizations/"+org_directory+"/"+org_name+"/users/Admin@"+org_name+"/msp --csr.hosts 'host1' --csr.names 'C=US,ST=North Carolina,O="+org_name+",OU=admin' --csr.cn " + name + " --tls.certfiles $(pwd)/"+mainDirectory+"/fabric-ca-client/tls-root-cert/tls-ca-cert.pem");
            
            executeWSLCommand("cp "+mainDirectory+"/fabric-ca-client/tls-root-cert/tls-ca-cert.pem "+mainDirectory+"/organizations/"+org_directory+"/"+org_name+"/msp/cacerts &&"
                       + "cp "+mainDirectory+"/fabric-ca-server-org1/tls/cert.pem "+mainDirectory+"/organizations/"+org_directory+"/"+org_name+"/msp/tlscacerts");
            
                        
            executeWSLCommand("mkdir -p "+mainDirectory+"/organizations/"+org_directory+"/"+org_name+"/users/Admin@"+org_name+"/msp/admincerts && "
                    + "cp  "+mainDirectory+"/organizations/"+org_directory+"/"+org_name+"/users/Admin@"+org_name+"/msp/signcerts/* "+mainDirectory+"/organizations/"+org_directory+"/"+org_name+"/users/Admin@"+org_name+"/msp/admincerts/");
            executeWSLCommand("cp  "+mainDirectory+"/organizations/"+org_directory+"/"+org_name+"/users/Admin@"+org_name+"/msp/signcerts/* "+mainDirectory+"/organizations/"+org_directory+"/"+org_name+"/msp/signcerts/");
            executeWSLCommand("cp  "+mainDirectory+"/organizations/"+org_directory+"/"+org_name+"/users/Admin@"+org_name+"/msp/cacerts/* "+mainDirectory+"/organizations/"+org_directory+"/"+org_name+"/msp/cacerts/");
            executeWSLCommand("cp  "+mainDirectory+"/organizations/"+org_directory+"/"+org_name+"/users/Admin@"+org_name+"/msp/keystore/* "+mainDirectory+"/organizations/"+org_directory+"/"+org_name+"/msp/keystore/");
            //TLS
            executeWSLCommand("cd "+mainDirectory+"/fabric-ca-client &&"+
                    "./fabric-ca-client enroll -u https://"+name+":"+psw+"@127.0.0.1:7054 --enrollment.profile tls --mspdir $(pwd)/"+mainDirectory+"/organizations/"+org_directory+"/"+org_name+"/users/Admin@"+org_name+"/tls --csr.hosts 'admin'  --tls.certfiles $(pwd)/"+mainDirectory+"/fabric-ca-client/tls-root-cert/tls-ca-cert.pem"
            );

            
        }

        //Inserimento dell'admin in fabric-ca-server-config.yaml
        File server_config=new File(""+ mainDirectory +"/fabric-ca-server-tls/fabric-ca-server-config.yaml");
        Yaml yaml= new Yaml();
        Map<String, Object> data= yaml.load(new FileReader(server_config));
        ArrayList<Map<String, Object>> identities= (ArrayList<Map<String, Object>>)((Map<String, Object>) data.get("registry")).get("identities");
        Map<String, Object> admin_identity= identities.get(0);
        admin_identity.put("name", name);
        admin_identity.put("pass", psw);
        admin_identity.put("type", "admin");

        DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yamlWriter = new Yaml(options);

        try (FileWriter writer = new FileWriter(server_config)) {
            yamlWriter.dump(data, writer);
        }

        //riavvio del container di fabric-ca-server per applicare le modifiche
        executeWSLCommand("docker restart "+tls_ca_name);
        
    }
    
    
    
    /**
     * Creates MSP/TLS certificates for a node (peer or orderer) within the organization.
     * @param org_name the name of the organization
     * @param node_name the name of the node
     * @param peer_org true if the node is a peer, false if it is an orderer
     * @throws IOException in case of I/O errors
     */
    private static void create_msp_tls_certificate(String org_name, String node_name, boolean peer_org) throws IOException {
        String org_directory = peer_org
                ? "peerOrganizations/" + org_name + "/peers/" + node_name + "/"
                : "ordererOrganizations/" + org_name + "/orderers/" + node_name + "/";
        executeWSLCommand("mkdir "+mainDirectory+"/fabric-ca-client/org1-ca/"+node_name);
        if(intermediate){
            executeWSLCommand("cd " + mainDirectory + "/fabric-ca-client && "
                +"export FABRIC_CA_CLIENT_TLS_SKIPHOSTVERIFY=true && "
                + "./fabric-ca-client register -d "
                + "--id.name " + node_name + " "
                + "--id.secret " + node_name + "_PSW " 
                + "-u https://"+node_name+":" + node_name + "_PSW@127.0.0.1:7056 "
                + "--tls.certfiles $(pwd)/" + mainDirectory + "/fabric-ca-client/tls-root-cert/tls-ca-cert.pem "
                + "--mspdir $(pwd)/"+mainDirectory+"/fabric-ca-client/int-ca/icaadmin/msp "
                + "--id.type "+(peer_org? "peer ":"orderer "));

            // Enrollment per MSP (identità)

            executeWSLCommand("cd " + mainDirectory + "/fabric-ca-client && "
                + "./fabric-ca-client enroll "
                + "-u https://" + node_name + ":" + node_name + "_PSW@127.0.0.1:7056 "
                + "--mspdir $(pwd)/" + mainDirectory + "/organizations/" + org_directory + "msp "
                + "--csr.hosts " + node_name + " "
                + "--tls.certfiles $(pwd)/" + mainDirectory + "/fabric-ca-server-int-ca/ca-chain.pem");

            // Aggiungo admincerts (legacy ma ancora richiesto in alcuni setup)
            executeWSLCommand("mkdir -p " + mainDirectory + "/organizations/" + org_directory + "msp/admincerts ");
                   // + "cp fabric-ca-server-int-ca/icaadmin/msp/signcerts/cert.pem "
                    //+ mainDirectory + "/organizations/" + org_directory + "msp/admincerts");
            
            //sistemazione cartelle intermediatecerts e cacerts
            executeWSLCommand("mkdir -p "+mainDirectory+"/organizations/"+org_directory+"msp/intermediatecerts && "
                    + "cp -r "+mainDirectory+"/fabric-ca-server-int-ca/ca-cert.pem "+mainDirectory+"/organizations/"+org_directory+"msp/intermediatecerts/ && "
                    + "rm "+mainDirectory+"/organizations/"+org_directory+"msp/cacerts/* && "
                    + "cp -r "+mainDirectory+"/fabric-ca-client/tls-root-cert/tls-ca-cert.pem "+mainDirectory+"/organizations/"+org_directory+"msp/cacerts/");
            //TLS
            executeWSLCommand("cd "+mainDirectory+"/fabric-ca-client &&"+
                    "./fabric-ca-client enroll "
                    + "-u https://"+node_name+":"+node_name+"_PSW@127.0.0.1:7056 "
                    + "--enrollment.profile tls "
                    + "--mspdir $(pwd)/" + mainDirectory + "/organizations/" + org_directory + "tls "
                    + "--csr.hosts " + node_name + " "
                    + "--tls.certfiles $(pwd)/"+mainDirectory+"/fabric-ca-server-int-ca/ca-chain.pem"
            );
        }else{
            // Registrazione identità presso 7054
            executeWSLCommand("cd " + mainDirectory + "/fabric-ca-client &&"
                    + "./fabric-ca-client register -d --id.name " + node_name + " --id.secret " + node_name + "_PSW "
                    + "-u https://127.0.0.1:7054 "
                    + "--tls.certfiles $(pwd)/"+mainDirectory+"/fabric-ca-client/tls-root-cert/tls-ca-cert.pem "
                    + "--mspdir $(pwd)/"+mainDirectory+"/fabric-ca-client/tls-ca/rcaadmin/msp "
                    + "--id.type "+(peer_org? "peer ":"orderer "));

            // Enrollment per MSP (identità)
            executeWSLCommand("cd " + mainDirectory + "/fabric-ca-client &&"
                    + "./fabric-ca-client enroll -u https://" + node_name + ":" + node_name + "_PSW@127.0.0.1:7054 "
                    + "--mspdir $(pwd)/" + mainDirectory + "/organizations/" + org_directory + "msp "
                    + "--csr.hosts " + node_name + " "
                    + "--tls.certfiles $(pwd)/"+mainDirectory+"/fabric-ca-client/tls-root-cert/tls-ca-cert.pem");

            // Aggiungo admincerts (legacy ma ancora richiesto in alcuni setup)
            executeWSLCommand("mkdir -p " + mainDirectory + "/organizations/" + org_directory + "msp/admincerts &&"
                    + "cp " + mainDirectory + "/organizations/" + (peer_org ? "peerOrganizations/" : "ordererOrganizations/")
                    + org_name +"/orderers/"+node_name+ "/msp/signcerts/cert.pem "
                    + mainDirectory + "/organizations/" + org_directory + "msp/admincerts");
            
            
        

            
            // Enrollment TLS 
            executeWSLCommand("cd " + mainDirectory + "/fabric-ca-client &&"
                    + "./fabric-ca-client enroll -u https://" + node_name + ":" + node_name + "_PSW@127.0.0.1:7054 "
                    + "--enrollment.profile tls "
                    + "--csr.hosts " + node_name + " "
                    + "--mspdir $(pwd)/" + mainDirectory + "/organizations/" + org_directory + "tls "
                    + "--tls.certfiles $(pwd)/"+mainDirectory+"/fabric-ca-client/tls-root-cert/tls-ca-cert.pem");
            
        }
        
    }

    

    /**
     * Sets the TLS profile for the server (modifies temporary configuration files).
     * @throws IOException in case of I/O errors
     */
    private static void setProfileTLSForServer() throws IOException{
        

        File server_config=new File(""+ mainDirectory +"/fabric-ca-server-tls/fabric-ca-server-config.yaml");
        Yaml yaml= new Yaml();
        Map<String, Object> data= yaml.load(new FileReader(server_config));
        Map<String, Object> profiles= (Map<String, Object>) ((Map<String, Object>)data.get("signing")).get("profiles");
        //rimozione del profilo tls di default
        profiles.remove("tls");
        profiles.remove("tls-client");
        profiles.remove("tls-server");
        profiles.put("tls", new HashMap<String,Object>());
        //tls-server profile
        Map<String,Object> tls_server_profile=new HashMap<String,Object>() {
            {
                put("usage", Arrays.asList("signing","key encipherment","server auth", "key agreement"));
                put("expiry", "8760h");
                
            }
        };
        profiles.put("tls", tls_server_profile);

        //Writing
        DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yamlWriter = new Yaml(options);

        try (FileWriter writer = new FileWriter(server_config)) {
            yamlWriter.dump(data, writer);
        }

        //riavvio del container di fabric-ca-server per applicare le modifiche
        executeWSLCommand("docker restart "+tls_ca_name);
    }

    /**
     * Sets the TLS profile for the client.
     * @throws IOException in case of I/O errors
     */
    private static void setProfileTLSForClient() throws IOException{
        

        File server_config=new File(""+ mainDirectory +"/fabric-ca-server-tls/fabric-ca-server-config.yaml");
        Yaml yaml= new Yaml();
        Map<String, Object> data= yaml.load(new FileReader(server_config));
        Map<String, Object> profiles= (Map<String, Object>) ((Map<String, Object>)data.get("signing")).get("profiles");
        //rimozione del profilo tls di default
        profiles.remove("tls");
        profiles.remove("tls-client");
        profiles.remove("tls-server");
        profiles.put("tls", new HashMap<String,Object>());
        //tls-client profile
        Map<String,Object> tls_client_profile=new HashMap<String,Object>() {
            {
                put("usage", Arrays.asList("signing","key encipherment","client auth", "key agreement"));
                put("expiry", "8760h");
                
            }
        };
        profiles.put("tls", tls_client_profile);
        //Writing
        DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yamlWriter = new Yaml(options);

        try (FileWriter writer = new FileWriter(server_config)) {
            yamlWriter.dump(data, writer);
        }

        //riavvio del container di fabric-ca-server per applicare le modifiche
        executeWSLCommand("docker restart "+tls_ca_name);
    }

    /**
     * Restores the original TLS profile by removing temporary modifications.
     * @throws IOException in case of I/O errors
     */
    private static void resetProfileTLS() throws IOException{

        File server_config=new File(""+ mainDirectory +"/fabric-ca-server-tls/fabric-ca-server-config.yaml");
        Yaml yaml= new Yaml();
        Map<String, Object> data= yaml.load(new FileReader(server_config));
        Map<String, Object> profiles= (Map<String, Object>) ((Map<String, Object>)data.get("signing")).get("profiles");
        //rimozione del profilo tls di default
        profiles.remove("tls");
        profiles.remove("tls-client");
        profiles.remove("tls-server");
        profiles.put("tls", new HashMap<String,Object>());
        //tls profile
        Map<String,Object> tls_profile=new HashMap<String,Object>() {
            {
                put("usage", Arrays.asList("signing","key encipherment","client auth","server auth", "key agreement"));
                put("expiry", "8760h");
                
            }
        };
        profiles.put("tls", tls_profile);
        //Writing
        DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yamlWriter = new Yaml(options);

        try (FileWriter writer = new FileWriter(server_config)) {
            yamlWriter.dump(data, writer);
        }

        //riavvio del container di fabric-ca-server per applicare le modifiche
        executeWSLCommand("docker restart "+tls_ca_name);
    }



    
    
    /**
     * Adds the peer definition to the docker-compose file (append) with specified volumes and ports.
     * @param peerName the name of the peer
     * @param org_name the name of the organization
     * @param cfgPath the path to the configuration
     * @param mspAdminPath the path to the MSP admin
     * @param mspPath the path to the MSP
     * @param tlsPath the path to the TLS
     * @param ports the list of ports used
     * @param couchDB true if the peer should have associated CouchDB
     * @param tls_enabled true if TLS is enabled
     * @param bootstrap the bootstrap peer
     * @param peer_number the peer number
     * @throws IOException in case of I/O errors
     */
    private static void add_peer_to_docker(String peerName, String org_name, String cfgPath, String mspAdminPath ,String mspPath, String tlsPath, String rootPath, String chainPath, LinkedList<Integer> ports, boolean couchDB, boolean tls_enabled, String bootstrap, int peer_number) throws IOException{
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
        env.add("CORE_PEER_ID=" + peerName);

        env.add("CORE_PEER_ADDRESS=" + peerName + ":7051");
        env.add("CORE_PEER_LISTENADDRESS=0.0.0.0:7051");

        env.add("CORE_PEER_CHAINCODEADDRESS=" + peerName + ":7052");
        env.add("CORE_PEER_CHAINCODELISTENADDRESS=0.0.0.0:7052");
        env.add("CORE_PEER_GOSSIP_EXTERNALENDPOINT=" + peerName + ":7051");
        env.add("CORE_PEER_GOSSIP_INTERNALENDPOINT=" + peerName + ":7051");
        env.add("CORE_PEER_GOSSIP_BOOTSTRAP=" + bootstrap);

        env.add("CORE_PEER_LOCALMSPID="+org_name);
        env.add("CORE_OPERATIONS_LISTENADDRESS=0.0.0.0:7053");

        env.add("CORE_PEER_MSPCONFIGPATH=/etc/hyperledger/fabric/msp");
        env.add("CORE_PEER_TLS_ROOTCERT_FILE="+(intermediate ?"/etc/hyperledger/fabric/tls/ca-chain.pem": "/etc/hyperledger/fabric/tls/tls-ca-cert.pem"));
        env.add("CORE_PEER_TLS_CERT_FILE=/etc/hyperledger/fabric/tls/signcerts/cert.pem");
        env.add("CORE_PEER_TLS_KEY_FILE=/etc/hyperledger/fabric/tls/keystore/"+executeWSLCommandToString("ls "+mainDirectory+"/organizations/peerOrganizations/"+ org_name + "/peers/" + peerName+"/tls/keystore/ | grep '_sk'").trim());
        env.add("CORE_PEER_TLS_ENABLED="+tls_enabled);
        env.add("CORE_PEER_GOSSIP_USELEADERELECTION=true");
        env.add("CORE_PEER_GOSSIP_ORGLEADER=false");
        env.add("CORE_PEER_TLS_CLIENTKEY_FILE=/etc/hyperledger/fabric/tls/keystore/"+executeWSLCommandToString("ls "+mainDirectory+"/organizations/peerOrganizations/"+ org_name + "/peers/" + peerName+"/tls/keystore/ | grep '_sk'").trim());
        env.add("CORE_PEER_TLS_CLIENTCERT_FILE=/etc/hyperledger/fabric/tls/signcerts/cert.pem");
        env.add("CORE_PEER_TLS_CLIENTROOTCAS_FILES=/etc/hyperledger/fabric/tls/tlscacerts/tls-127-0-0-1-"+(intermediate?"7056":"7054")+".pem");

        env.add("CORE_PEER_GOSSIP_ALIVEEXPIRATIONTIMEOUT=300s");

        env.add("CORE_VM_DOCKER_HOSTCONFIG_NETWORKMODE="+mainDirectory.toLowerCase()+"_fabric_network");
        peerConfig.put("environment", env);
        
        // Volumes
        List<String> volumes = new ArrayList<>();
        volumes.add(cfgPath + ":/etc/hyperledger/fabric/config");
        volumes.add(mspAdminPath + ":/etc/hyperledger/fabric/mspAdmin");
        volumes.add(mspPath + ":/etc/hyperledger/fabric/msp");
        volumes.add(tlsPath + ":/etc/hyperledger/fabric/tls");
        volumes.add("/var/run/docker.sock:/var/run/docker.sock");
        volumes.add(rootPath + ":/etc/hyperledger/fabric/tls/tls-ca-cert.pem");
        if(intermediate){
            volumes.add(chainPath + ":/etc/hyperledger/fabric/tls/ca-chain.pem");
        }
        peerConfig.put("volumes", volumes);
        
         // Ports
        peerConfig.put("ports", Arrays.asList((7051+((get_num_org()+get_num_other_peers(org_name)-1)*1000)+(peer_number*1000))+":7051",(7052+((get_num_org()+get_num_other_peers(org_name)-1)*1000)+(peer_number*1000))+":7052",(7053+((get_num_org()+get_num_other_peers(org_name)-1)*1000)+(peer_number*1000))+":7053"));
        // Network
        Map<String, Object> networks = new LinkedHashMap<>();
        Map<String, Object> fabric_network= new LinkedHashMap<>();
        fabric_network.put("aliases", Arrays.asList(peerName));
        networks.put("fabric_network", fabric_network);
        peerConfig.put("networks", networks);
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
    
    /**
     * Counts the number of other peers in the docker-compose that are not in the specified organization.
     * @param org_name the name of the organization to exclude
     * @return the count of other peers
     * @throws FileNotFoundException if the docker-compose.yaml file is not found
     * @throws IOException in case of I/O errors
     */
    private static int get_num_other_peers(String org_name) throws FileNotFoundException, IOException{
        int count=0;
        Yaml yaml = new Yaml();
        File file = new File(mainDirectory + "/docker-compose.yaml");

        Map<String, Object> data;
        try (InputStream inputStream = new FileInputStream(file)) {
            data = yaml.load(inputStream);
        }

        // Sezione 'services'
        Map<String, Object> services = (Map<String, Object>) data.get("services");
        if (services == null) {
            return 0;
        }
        
        for(String key: services.keySet()){
            if(key.startsWith("peer") && key.contains(org_name)){
                
            }else{
                if(key.startsWith("peer")){
                    count++;
                }
            }
        }
        return count;
    }
       
    /**
     * Adds the orderer definition to the docker-compose with volumes, ports, and specified paths.
     * @param ordererName the name of the orderer
     * @param orgName the name of the organization
     * @param cfgPath the path to the configuration
     * @param mspPath the path to the MSP
     * @param tlsPath the path to the TLS
     * @param ledgerPath the path to the ledger
     * @param keysPath the path to the keys
     * @param ports the list of ports to expose
     * @throws IOException in case of I/O errors
     */
    private static void add_orderer_to_docker(String ordererName, String orgName, String cfgPath, String mspPath, String tlsPath, String ledgerPath, String keysPath, LinkedList<Integer> ports)throws IOException{
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
        env.add("FABRIC_CFG_PATH=" + "/var/hyperledger/orderer/config");
        env.add("ORDERER_GENERAL_LOCALMSPDIR=" + "/var/hyperledger/orderer/msp");
        env.add("ORDERER_GENERAL_LOCALMSPID=" + orgName);
        env.add("ORDERER_GENERAL_BOOTSTRAPMETHOD=file");
        env.add("ORDERER_GENERAL_BOOTSTRAPFILE=/var/hyperledger/orderer/config/genesis_block.pb");
        ordererConfig.put("environment", env);
        
        executeWSLCommand("cp "+mainDirectory+"/bin/genesis_block.pb "+cfgPath);
        List<String> volumes = new ArrayList<>();
        volumes.add(cfgPath + ":/var/hyperledger/orderer/config");
        volumes.add(mspPath + ":/var/hyperledger/orderer/msp");
        volumes.add(tlsPath + ":/var/hyperledger/orderer/tls");
        volumes.add(ledgerPath+":/var/hyperledger/production/orderer");
        volumes.add(keysPath+":/var/hyperledger/orderer/keys");
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
    
    /**
     * Crea il genesis block usando gli strumenti locali (configtxgen/configtxlator) e lo salva nella cartella bin.
     * @return il nome del canale generato
     * @throws FileNotFoundException se qualche file richiesto non viene trovato
     * @throws IOException in caso di errori I/O
     */
    /**
     * Creates and writes the initial genesis block using local tools (configtxgen/configtxlator) and saves it in the bin folder.
     * @param org_name the name of the organization
     * @param peers the list of peers
     * @param channel_name the name of the channel
     * @throws FileNotFoundException if required files are not found
     * @throws IOException in case of I/O errors
     */
    private static void createGenesisBlock(String org_name, LinkedList<String> peers, String channel_name) throws FileNotFoundException, IOException{
        
        downloadBinForGenesisBlock();
        while(!executeWSLCommandToString("ls "+mainDirectory+"/bin").contains("configtx.yaml")){
            System.out.println("Download of binaries failed, retrying...");
            downloadBinForGenesisBlock();
        }
        
        
        String path=executeWSLCommandToString("echo $(pwd)");
        Map<String, Object> profiles = new HashMap<>();

        

        
        orderer_name="orderer"+(get_num_orderers()+1)+".example.com";
        orderer_port=5050+get_num_orderers();
        
        //Creazione ClientTLSCert e ServerTLSCert per i consenter
        executeWSLCommand("cd "+mainDirectory+"/organizations/ordererOrganizations &&"
                + "mkdir Consenters &&"
                + "cd Consenters &&"
                + "mkdir " + orderer_name + " &&"
                //"orderer2.example.com orderer3.example.com &&"
                + "cd " + orderer_name + " &&"
                + "mkdir msp &&"
                + "mkdir tls ");
        executeWSLCommand("mkdir -p " + mainDirectory + "/organizations/peerOrganizations/"+org_name+"/msp/admincerts &&"
                + "cp " + mainDirectory + "/organizations/peerOrganizations/"+org_name+"/users/Admin@"+org_name+"/msp/signcerts/cert.pem " + mainDirectory + "/organizations/peerOrganizations/"+org_name+"/msp/admincerts");
        
        create_msp_tls_certificate("Consenters",orderer_name, false);

        
        //create_msp_tls_certificate("Consenters","orderer2.example.com", false);
        //create_msp_tls_certificate("Consenters","orderer3.example.com", false);
        //create_msp_tls_certificate("org1","peer0.org1", true);
        
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

        
        List<Map<String, Object>> orgs = (List<Map<String, Object>>) data.get("Organizations");
        // prendi la prima org (es. OrdererOrg)
        Map<String, Object> peerOrg = orgs.get(0);

        // aggiungi o modifica OrdererEndpoints
        List<String> ordererEndpoints = new ArrayList<>();
        ordererEndpoints.add(orderer_name + ":" + orderer_port);

        peerOrg.put("Name", org_name);
        peerOrg.put("ID", org_name);
        peerOrg.put("Policies", createOrdererPolicies(false)); //createOrgPolicies("org1"));
        peerOrg.put("MSPDir",path+"/"+mainDirectory+"/organizations/peerOrganizations/"+org_name+"/msp");
        peerOrg.put("OrdererEndpoints", ordererEndpoints);
        peerOrg.put("AnchorPeers", Arrays.asList(new HashMap<String,Object>() {
        {
            put("Host", peers.get(0));
            put("Port", 7051+((get_num_org()+get_num_other_peers(org_name)-1)*1000));
        }
        }
        ));
        
        
        
        Map<String,Object> consenterOrg= new HashMap<>();
        consenterOrg.put("Name", "Consenters");
        consenterOrg.put("ID", "Consenters");
        consenterOrg.put("Policies", createOrgPolicies("Consenters"));
        consenterOrg.put("MSPDir",path+"/"+mainDirectory+"/organizations/ordererOrganizations/Consenters/msp");
        consenterOrg.put("OrdererEndpoints", ordererEndpoints);
        orgs.add(consenterOrg);
        
        //Sezione Application
        Map<String, Object> application = (Map<String, Object>) data.get("Application");
        application.put("Organizations", Arrays.asList(peerOrg, consenterOrg));
        //Consenters
        Map<String,Object> orderer=(Map<String,Object>) data.get("Orderer");
        orderer.put("OrdererType", "etcdraft");
        
        List<Map<String,Object>> consenters = new ArrayList<>();
        

        
        
        ((Map<String,Object>) orderer.get("EtcdRaft")).put("Consenters", consenters);
        //orderer.remove("EtcdRaft");
        
        // Consortiums
        Map<String, Object> consortiums = new HashMap<>();
        Map<String, Object> sampleConsortium = new HashMap<>();
        // Creiamo un'organizzazione orderer "Host1MSP"
        
        List<Map<String, Object>> ordererOrgs = new ArrayList<>();
        ordererOrgs.add(peerOrg);


        consortiums.put("SampleConsortium", sampleConsortium);

        
        
        
        // ---------- Profilo SampleAppChannelEtcdRaft ----------
        Map<String, Object> SampleAppChannelEtcdRaft = (Map<String,Object>) ((Map<String,Object>) data.get("Profiles")).get("SampleAppChannelEtcdRaft");

        //SampleAppChannelEtcdRaft.put("Consortium", "SampleConsortium");
        consortiums = new LinkedHashMap<>();

        sampleConsortium = new LinkedHashMap<>();
        List<Object> orgsList = new ArrayList<>();


        sampleConsortium.put("Organizations", orgsList);

        // mappa con la singola entry "SampleConsortium"
        //consortiums.put("SampleConsortium", sampleConsortium);
        //SampleAppChannelEtcdRaft.put("Consortiums",consortiums);
        Map<String, Object> SectionOrderer = (Map<String,Object>) data.get("Orderer");

        SectionOrderer.put("OrdererType", "etcdraft");
        //SectionOrderer.remove("EtcdRaft");
        Map<String,Object> host = new HashMap<>();
        host.put("Host", orderer_name);
        host.put("port", orderer_port);
        host.put("ClientTLSCert", path+"/"+mainDirectory + "/organizations/ordererOrganizations/Consenters/orderers/" + orderer_name + "/tls/signcerts/cert.pem");
        host.put("ServerTLSCert", path+"/"+mainDirectory + "/organizations/ordererOrganizations/Consenters/orderers/" + orderer_name + "/tls/signcerts/cert.pem");
        SectionOrderer.put("EtcdRaft", Map.of("Consenters", Arrays.asList(host)));
        

        //Addresses nell'orderer
        List<String> addresses = new ArrayList<>();
        addresses.add(orderer_name + ":" + orderer_port);

        SectionOrderer.put("Organizations", Arrays.asList(consenterOrg)); 
        SectionOrderer.put("Addresses", addresses);

        SampleAppChannelEtcdRaft.remove("Orderer");
        SampleAppChannelEtcdRaft.put("Orderer", SectionOrderer);

        //SampleAppChannelEtcdRaft.put("Organizations", Arrays.asList(host1,host2,host3));
        
        

        
        Map<String, Object> SectionApplication=(Map<String, Object>) SampleAppChannelEtcdRaft.get("Application");
        SectionApplication.put("Policies", createOrdererPolicies(false));
        SectionApplication.remove("ACLs");
        SectionApplication.put("Organizations", Arrays.asList(peerOrg));
        
        sampleConsortium = new HashMap<>();

        // Inserisci le organizzazioni (per ora vuote)
        sampleConsortium.put("Organizations", new ArrayList<>());



        // Inserisci nel blocco Consortiums di OrdererGenesis

        // Ora inserisci il tutto in OrdererGenesis
        //ordererGenesis.put("Consortiums", ordererGenesisConsortiums);

        // Assicurati di aver già popolato OrdererGenesis.Orderer con ordererSection
        //ordererGenesis.put("Orderer", ordererSection);

        // Infine inserisci OrdererGenesis nei profili
        
        // ---------- Inserisci nei profili ----------
        Map<String, Object> etcdRaft = new HashMap<>();
        etcdRaft.put("Consenters", consenters);
        
         
        Map<String, Object> appPolicies = new HashMap<>();
        appPolicies.put("Readers", createOrdererPolicies(false).get("Readers"));
        appPolicies.put("Writers", createOrdererPolicies(false).get("Writers"));
        appPolicies.put("Admins", createOrdererPolicies(false).get("Admins"));

        

        // ---------- Profilo OrdererGenesis ----------
        Map<String, Object> ordererGenesis = new HashMap<>();
        ordererGenesis.putAll(SampleAppChannelEtcdRaft);
        sampleConsortium = new LinkedHashMap<>();
        sampleConsortium.put("Organizations", Arrays.asList(peerOrg, consenterOrg));

        consortiums = new LinkedHashMap<>();
        consortiums.put("SampleConsortium", sampleConsortium);
        ordererGenesis.put("Consortiums", consortiums);
        data.put("Profiles", Map.of(
            "OrdererGenesis", ordererGenesis,
            "SampleAppChannelEtcdRaft", SampleAppChannelEtcdRaft
        ));
        SampleAppChannelEtcdRaft.put("Consortium", "SampleConsortium");

        
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
        
        // Correzzione della versione da 2.5 a 2.0
        fixVersionConfigtx();
        
        //------------------ORDERER------------------
        executeWSLCommand("mkdir "+mainDirectory+"/organizations/ordererOrganizations/Consenters/msp");
        executeWSLCommand("mkdir "+mainDirectory+"/organizations/ordererOrganizations/Consenters/msp/cacerts");
        executeWSLCommand("mkdir "+mainDirectory+"/organizations/ordererOrganizations/Consenters/msp/tlscacerts");

        organizationAdminRegistrationEnroll("Consenters", false);
        createConfig_yaml("organizations/ordererOrganizations/"+"Consenters"+"/msp","Consenters");
        copy_orderer_bin(orderer_name);
        executeWSLCommand("cp -r $(pwd)/"+mainDirectory+"/organizations/peerOrganizations/"+org_name+"/msp $(pwd)/"+mainDirectory+"/bin");
        executeWSLCommand("cd "+mainDirectory+"/bin &&"
                + "./configtxgen -configPath $(pwd)/"+mainDirectory+"/bin  -profile OrdererGenesis -channelID sys-channel -outputBlock ./sys-channel_block.pb");
        configure_orderer(orderer_name, orderer_port, "Consenters", true, channel_name);
        //Copia del certificato dell'admin in admincerts
        //executeWSLCommand("cp "+mainDirectory+"/organizations/ordererOrganizations/Consenters/orderers/"+orderer_name+"/msp/signcerts/cert.pem "+mainDirectory+"/organizations/ordererOrganizations/Consenters/orderers/"+orderer_name+"/msp/admincerts/");
        executeWSLCommand("cp "+mainDirectory+"/organizations/peerOrganizations/"+org_name+"/users/Admin@"+org_name+"/msp/signcerts/cert.pem "
                +mainDirectory+"/organizations/ordererOrganizations/Consenters/msp/admincerts/");
        
        
        

        //Copia del binario peer nella cartella principale per poter essere usato nel container
        executeWSLCommand("cp "+mainDirectory+"/bin/peer "+mainDirectory);

        
        
        
        executeWSLCommand("cp "+mainDirectory+"/bin/osnadmin "+mainDirectory);
        
        
    }

    /**
     * Adds a volume to a running Docker container by copying the host path to the container path.
     * @param containerName the name of the container
     * @param hostPath the path on the host
     * @param containerPath the path in the container
     */
    private static void addVolumeToContainer(String containerName, String hostPath, String containerPath) {
        executeWSLCommand("docker cp " + hostPath + " " + containerName + ":" + containerPath);
    }

    
    
    /**
     * Creates the orderer policies map.
     * @param blockValidation true if block validation policy is needed
     * @return the map of policies
     */
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

        Map<String,Object> Endorsement = new HashMap<>();
        Endorsement.put("Type", "ImplicitMeta");
        Endorsement.put("Rule", "ANY Endorsers");

        policies.put("Readers", readers);
        policies.put("Writers", writers);
        policies.put("Admins", admins);
        policies.put("Endorsement", Endorsement);
        
        if(blockValidation){
            Map<String,Object> blkVal= new HashMap<>();
            blkVal.put("Type", "Signature");
            blkVal.put("Rule", "ANY Writers");
            
            policies.put("BlockValidation", blkVal);
        }
        
        return policies;
    }
    /**
     * Creates the organization policies map.
     * @param mspId the MSP ID
     * @return the map of policies
     */
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

        Map<String, Object> endorsement = new HashMap<>();
        endorsement.put("Type", "Signature");
        endorsement.put("Rule", "OR('" + mspId + ".member')");

        policies.put("Readers", readers);
        policies.put("Writers", writers);
        policies.put("Admins", admins);
        policies.put("Endorsement", endorsement);

        return policies;
    }


    /**
     * Apporta correzioni al file configtx.yaml per risolvere incompatibilità di versione.
     */
    private static void fixVersionConfigtx(){
        executeWSLCommand("cd $(pwd)/"+mainDirectory+"/bin &&"
                + "sed -i 's/2.0/2_0/g' configtx.yaml &&"
                + "sed -i 's/2_5/2_0/g' configtx.yaml");
    }
    
    
    /**
     * Scarica i binari necessari per la generazione del genesis block (configtxgen, configtxlator, ecc.).
     */
    private static void downloadBinForGenesisBlock(){
        executeWSLCommandWithProgress("cd $(pwd)/"+mainDirectory+"/bin &&"
                + "aria2c https://github.com/hyperledger/fabric/releases/download/v2.5.0/hyperledger-fabric-linux-amd64-2.5.0.tar.gz -o fabric-bin.tar.gz &&"
                + "tar -xvzf fabric-bin.tar.gz --strip-components=1");
    }
    /**
     * Adds the consenters (orderers) to the docker-compose for the specified channel.
     * @param channel_name the name of the channel
     * @throws IOException in case of I/O errors
     */
    private static void addConsentersDocker() throws IOException{
        if(intermediate){
            executeWSLCommand("cp "+mainDirectory+"/fabric-ca-server-int-ca/ca-chain.pem "+mainDirectory+"/organizations/ordererOrganizations/Consenters/orderers/" + orderer_name + "/tls");
        }
        String path=executeWSLCommandToString("echo $(pwd)");
        String file_content="  " + orderer_name + ":\n" +
                "    image: hyperledger/fabric-orderer:2.5\n" +
                "    container_name: " + orderer_name + "\n" +
                "    environment:\n" +
                "      - FABRIC_CFG_PATH=/etc/hyperledger/fabric/config\n" +
                "      - FABRIC_LOGGING_SPEC=INFO\n" +
                "\n" +
                "      # Network\n" +
                "      - ORDERER_GENERAL_LISTENADDRESS=0.0.0.0\n" +
                "      - ORDERER_GENERAL_LISTENPORT=" + orderer_port + "\n" +
                "\n" +
                "      # Cluster (Raft)\n" +
                "      - ORDERER_GENERAL_CLUSTER_LISTENADDRESS=0.0.0.0\n" +
                "      - ORDERER_GENERAL_CLUSTER_LISTENPORT="+(7079+(get_num_orderers()))+"\n" +
                "      - ORDERER_GENERAL_CLUSTER_TLS_ENABLED=true\n" +
                "      - ORDERER_GENERAL_CLUSTER_TLS_CLIENTAUTHREQUIRED=false\n" +
                "      - ORDERER_GENERAL_CLUSTER_SERVERCERTIFICATE=/var/hyperledger/orderer/tls/signcerts/cert.pem\n" +
                "      - ORDERER_GENERAL_CLUSTER_SERVERPRIVATEKEY=/var/hyperledger/orderer/tls/keystore/"+executeWSLCommandToString("ls "+mainDirectory+"/organizations/ordererOrganizations/Consenters/orderers/" + orderer_name + "/tls/keystore/ | grep '_sk'").trim()+"\n" +
                "      - ORDERER_GENERAL_CLUSTER_CLIENTCERTIFICATE=/var/hyperledger/orderer/tls/signcerts/cert.pem\n" +
                "      - ORDERER_GENERAL_CLUSTER_CLIENTPRIVATEKEY=/var/hyperledger/orderer/tls/keystore/"+executeWSLCommandToString("ls "+mainDirectory+"/organizations/ordererOrganizations/Consenters/orderers/" + orderer_name + "/tls/keystore/ | grep '_sk'").trim()+"\n" +
                "      - ORDERER_GENERAL_CLUSTER_ROOTCAS="+(intermediate ? "[/var/hyperledger/orderer/tls/tls-ca-cert.pem, /var/hyperledger/orderer/tls/ca-chain.pem]" : "[/var/hyperledger/orderer/tls/tls-ca-cert.pem]")+"\n" +
                "      # MSP\n" +
                "      - ORDERER_GENERAL_LOCALMSPID=Consenters\n" +
                "      - ORDERER_GENERAL_LOCALMSPDIR=/var/hyperledger/orderer/msp\n" +
                "\n" +
                "      # TLS\n" +
                "      - ORDERER_GENERAL_TLS_ENABLED=true \n" +
                "      - ORDERER_GENERAL_TLS_CLIENTAUTHREQUIRED=false\n" +
                "      - ORDERER_GENERAL_TLS_CLIENTROOTCAS="+(intermediate ? "[/var/hyperledger/orderer/tls/tls-ca-cert.pem, /var/hyperledger/orderer/tls/ca-chain.pem]" : "[/var/hyperledger/orderer/tls/tls-ca-cert.pem]")+"\n" +
                "      - ORDERER_GENERAL_TLS_PRIVATEKEY=/var/hyperledger/orderer/tls/keystore/"+executeWSLCommandToString("ls "+mainDirectory+"/organizations/ordererOrganizations/Consenters/orderers/" + orderer_name + "/tls/keystore/ | grep '_sk'").trim()+"\n" +
                "      - ORDERER_GENERAL_TLS_CERTIFICATE=/var/hyperledger/orderer/tls/signcerts/cert.pem\n" +
                "      - ORDERER_GENERAL_TLS_ROOTCAS="+(intermediate ? "[/var/hyperledger/orderer/tls/tls-ca-cert.pem,/var/hyperledger/orderer/tls/ca-chain.pem]" : "[/var/hyperledger/orderer/tls/tls-ca-cert.pem]")+"\n" +
                "\n" +
                "      # Bootstrap\n" +
                "      #- ORDERER_GENERAL_BOOTSTRAPMETHOD=none\n" +
                "\n" +
                "      # Admin service\n" +
                "      - ORDERER_ADMIN_LISTENADDRESS=" + orderer_name + ":"+(9442+(get_num_orderers()))+"\n" +
                "      - ORDERER_ADMIN_TLS_ENABLED=true\n" +
                "      - ORDERER_ADMIN_TLS_ROOTCAS="+(intermediate ? "[/var/hyperledger/orderer/tls/tls-ca-cert.pem, /var/hyperledger/orderer/tls/ca-chain.pem]" : "[/var/hyperledger/orderer/tls/tls-ca-cert.pem]")+"\n" +
                "      - ORDERER_ADMIN_TLS_PRIVATEKEY=/var/hyperledger/orderer/tls/keystore/"+executeWSLCommandToString("ls "+mainDirectory+"/organizations/ordererOrganizations/Consenters/orderers/" + orderer_name + "/tls/keystore/ | grep '_sk'").trim()+"\n" +
                "      - ORDERER_ADMIN_TLS_CERTIFICATE=/var/hyperledger/orderer/tls/signcerts/cert.pem\n" +
                "      - ORDERER_ADMIN_TLS_CLIENTAUTHREQUIRED=true\n" +
                "      - ORDERER_ADMIN_TLS_CLIENTROOTCAS="+(intermediate ? "[/var/hyperledger/orderer/tls/tls-ca-cert.pem, /var/hyperledger/orderer/tls/ca-chain.pem]" : "[/var/hyperledger/orderer/tls/tls-ca-cert.pem]")+"\n" +
                    "\n" +
                "      # Channel participation\n" +
                "      - ORDERER_CHANNELPARTICIPATION_ENABLED=true\n" +
                "\n" +
                "      # Genesis file\n" +
                "      - ORDERER_GENERAL_GENESISFILE=/var/hyperledger/orderer/genesis_block/sys-channel_block.pb\n" +
                //"      #- ORDERER_GENERAL_SYSTEMCHANNELID=system-channel\n" + 
                "      - ORDERER_GENERAL_BOOTSTRAPMETHOD=file\n" +  //file
                "      - ORDERER_GENERAL_BOOTSTRAPFILE=/var/hyperledger/orderer/genesis_block/sys-channel_block.pb\n"+
                "    working_dir: /opt/gopath/src/github.com/hyperledger/fabric\n" +
                "    command: orderer\n" +
                "    ports:\n" +
                "      - "+(9442+(get_num_orderers()))+":"+(9442+(get_num_orderers()))+"\n" +
                "      - "+orderer_port+":"+orderer_port+"\n" +
                "      - "+(7079+(get_num_orderers()))+":"+(7079+(get_num_orderers()))+"\n" +
                "    volumes:\n" +
                "      - "+path+"/"+mainDirectory+"/organizations/ordererOrganizations/Consenters/msp:/etc/hyperledger/fabric/msp\n" +
                "      - "+path+"/"+mainDirectory+"/organizations/ordererOrganizations/Consenters/orderers/" + orderer_name + "/msp:/var/hyperledger/orderer/msp\n" +
                "      - "+path+"/"+mainDirectory+"/organizations/ordererOrganizations/Consenters/orderers/" + orderer_name + "/tls:/var/hyperledger/orderer/tls\n" +
                "      - "+path+"/"+mainDirectory+"/bin/:/var/hyperledger/orderer/genesis_block\n" +
                "      - "+path+"/"+mainDirectory+"/orderers_bin/" + orderer_name + "/config:/etc/hyperledger/fabric/config\n"+
                (intermediate ? "      - "+path+"/"+mainDirectory+"/fabric-ca-server-int-ca/ca-cert.pem:/var/hyperledger/orderer/tls/ca-cert.pem\n" : "") +
                "    networks:\n" +
                "      - fabric_network\n" +
                "\n";
                
        
        Files.write(Paths.get(mainDirectory, "docker-compose.yaml"),
            file_content.getBytes(StandardCharsets.UTF_8),
            StandardOpenOption.APPEND);
    }
    
    
    
    /**
     * Esegue un comando all'interno di WSL (Windows Subsystem for Linux).
     * Questa variante stampa direttamente l'output su stdout/stderr.
     * @param bashCommand il comando bash da eseguire in WSL
     */
    protected static void executeWSLCommand(String bashCommand) {
        try {
            // Esegui comando in WSL
            Process process = Runtime.getRuntime().exec(new String[]{"wsl", "bash", "-c", bashCommand});

            // Leggi output standard (se necessario, decommentare il ciclo di lettura)
            String line;
            System.out.println(">>> Comando: " + bashCommand);
            

            // Leggi eventuali errori
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = errorReader.readLine()) != null) {
                System.err.println("OUT: " + line);
            }

            process.waitFor();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    /**
     * Esegue un comando in WSL e restituisce l'output standard come String.
     * @param bashCommand il comando bash da eseguire
     * @return l'output prodotto dal comando (stdout) concatenato
     */
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
                ris=ris+line+"";
                if(ris.contains("Package ID")){
                    break;
                }
            }

            // Leggi eventuali errori
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = errorReader.readLine()) != null) {
                System.err.println("OUT: " + line);
            }

            process.waitFor();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return ris;
    }

    /**
     * Esegue un comando in WSL e restituisce l'output (o l'ultima riga letta) in caso di errore.
     * Nota: attualmente restituisce la variabile 'line' letta dopo il loop (potrebbe essere null).
     * @param bashCommand il comando bash da eseguire
     * @return l'ultima riga letta dall'output o "0" in caso di eccezione
     */
    public static String executeWSLCommandErrorToString(String bashCommand){
        String ris="";
        try {
            // Esegui comando in WSL
            Process process = Runtime.getRuntime().exec(new String[]{"wsl", "bash", "-c", bashCommand});

            // Leggi output standard
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            System.out.println(">>> Comando: " + bashCommand);
            while ((line = reader.readLine()) != null) {
                ris=ris+line+"";
            }

            // Leggi eventuali errori
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = errorReader.readLine()) != null) {
                System.err.println("OUT: " + line);
            }

            process.waitFor();
            return line;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "0";
        
    }

    /**
     * Esegue 'ls' in WSL e restituisce l'elenco dei file come String.
     * @return la lista di file/nomi concatenati separati da spazi
     */
    public static String executeWSLCommandLS(String path){
        String ris=" ";
        try {
            // Esegui comando in WSL
            Process process = Runtime.getRuntime().exec(new String[]{"wsl", "bash", "-c", "ls " + path});
            // Leggi output standard
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            System.out.println(">>> Comando: " + "ls " + path);
            while ((line = reader.readLine()) != null) {
                ris=ris+line+" ";
            }

            // Leggi eventuali errori
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = errorReader.readLine()) != null) {
                System.err.println("OUT: " + line);
            }

            process.waitFor();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return ris;
    }

    /**
     * Esegue un comando in WSL mostrando l'output in tempo reale e mantenendo stdout+stderr uniti.
     * Utile per comandi lunghi che devono mostrare progressi.
     * @param command il comando da eseguire in WSL
     */
    public static void executeWSLCommandWithProgress(String command) {
    try {
        ProcessBuilder pb = new ProcessBuilder("wsl", "-e", "bash", "-c", command);
        pb.redirectErrorStream(true); // unisce stdout e stderr
        Process process = pb.start();

        // Legge l'output in tempo reale
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            System.err.println("⚠️ Comando terminato con codice: " + exitCode);
        } else {
            System.out.println("✅ Comando completato con successo!");
        }

    } catch (Exception e) {
        e.printStackTrace();
    }
}

    
    /**
     * Attende che un container Docker sia avviato controllando `docker ps` in loop.
     * @param containerName nome del container da verificare
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

    /**
     * Creates chaincode for one peer.
     * @param channelName the name of the channel
     * @param org_name the name of the organization
     * @param peer the peer name
     * @param port the port number
     * @throws IOException in case of I/O errors
     */
    private static void create_chaincode_for_one_peer(String channelName, String org_name, String peer, int port) throws IOException {
        executeWSLCommand("cd "+mainDirectory+" &&"
                + "mkdir atcc && cd atcc &&"
                +" go mod init atcc &&"
                +" touch atcc.go");
        
        System.out.println("Writing go code...");
        writeChaincode();
        System.out.println("Go code written successfully.");
        File go_mod= new File(mainDirectory+"/atcc/go.mod");
        FileWriter go_mod_writer= new FileWriter(go_mod, true);
        go_mod_writer.write("\nrequire (\n" +
                " github.com/hyperledger/fabric-contract-api-go v1.1.0\n" +
                ")\n");
        go_mod_writer.close();
        executeWSLCommand("cd "+mainDirectory+"/atcc &&"+
                "go mod tidy");
        //Package chaincode
        executeWSLCommand(
            "cd "+mainDirectory+" && " +
                "export CORE_PEER_LOCALMSPID="+org_name+" && " +
                "export CORE_PEER_MSPCONFIGPATH=$PWD/"+mainDirectory+"/organizations/peerOrganizations/"+org_name+"/msp && " +
                "export CORE_PEER_ADDRESS="+ peer +":"+port+" && " +
                "export CORE_PEER_TLS_ENABLED=true && "+
                "export FABRIC_CFG_PATH=$PWD/"+mainDirectory+"/peers_bin/"+ peer +"/config/ && " +      
                "./peer lifecycle chaincode package mycc.tar.gz " +
                "--path $PWD/"+mainDirectory+"/atcc " +
                "--lang golang " +
                "--label mycc_1.0" 
        );

        
        //Install chaincode

        executeWSLCommand("docker cp $PWD/"+mainDirectory+"/mycc.tar.gz "+peer+":/etc/hyperledger/fabric");
        executeWSLCommand("cd "+mainDirectory+" &&"
            + "docker exec " + peer + " bash -c '"+
            "export CORE_PEER_MSPCONFIGPATH=/etc/hyperledger/fabric/mspAdmin && " +
            "export CORE_PEER_TLS_ROOTCERT_FILE="+(intermediate ? "/etc/hyperledger/fabric/tls/ca-chain.pem" : "/etc/hyperledger/fabric/tls/tls-ca-cert.pem")+" && " +
            "peer lifecycle chaincode install /etc/hyperledger/fabric/mycc.tar.gz'");
        

        String policy="OR('" + org_name + ".member')";
        //Approve and commit chaincode
        String packageId = getPackageId(peer);
        executeWSLCommand(
            "cd " + mainDirectory + " && " +
            "docker exec " + peer + " bash -c '" +
            "export CORE_PEER_MSPCONFIGPATH=/etc/hyperledger/fabric/mspAdmin && " +
            "peer lifecycle chaincode approveformyorg " +
            "-o " + orderer_name + ":"+orderer_port+" " +
            "--tls " +
            "--cafile "+(intermediate ? "/etc/hyperledger/fabric/tls/ca-chain.pem" : "/etc/hyperledger/fabric/tls/tls-ca-cert.pem")+" " +
            "--channelID " + channelName + " " +
            "--name mycc " +
            "--version 1.0 " +
            "--sequence 1 " +
            "--init-required " +
            "--package-id " + packageId + " " + "'"
        );

        

        

        executeWSLCommand("cd "+mainDirectory+" &&"
                + "docker exec " + peer + " bash -c '"
                +"export CORE_PEER_MSPCONFIGPATH=/etc/hyperledger/fabric/mspAdmin && "
                + "peer lifecycle chaincode commit "
                +"-o " + orderer_name + ":"+orderer_port+" "
                +" --tls --cafile "+(intermediate ? "/etc/hyperledger/fabric/tls/ca-chain.pem" : "/etc/hyperledger/fabric/tls/tls-ca-cert.pem")+" "
                +"--channelID "+channelName+" "
                +"--name mycc "
                +"--version 1.0 " 
                +"--sequence 1 "
                +"--init-required "
                +"--peerAddresses "+peer+":"+port+" "
                +"--tlsRootCertFiles "+(intermediate ? "/etc/hyperledger/fabric/tls/ca-chain.pem" : "/etc/hyperledger/fabric/tls/tls-ca-cert.pem")+" '");

        //Invoke chaincode
    }

    /**
     * Updates the anchor peer for the organization.
     * @param channel_name the name of the channel
     * @param org_name the name of the organization
     * @param peer the peer name
     */
    private static void AnchorPeerUpdate(String channel_name, String org_name, String peer){
        executeWSLCommand("cd "+mainDirectory+"/bin &&"
            +" ./configtxgen -profile SampleAppChannelEtcdRaft -outputAnchorPeersUpdate "+org_name+"Anchors.tx "
            + "-channelID "+channel_name+" -asOrg "+org_name
        );

        executeWSLCommand("docker cp $PWD/"+mainDirectory+"/bin/"+org_name+"Anchors.tx "+peer+":/etc/hyperledger/fabric");

        executeWSLCommand("cd "+mainDirectory+" &&"
            +" docker exec "+peer+" bash -c '"
            +"export CORE_PEER_MSPCONFIGPATH=/etc/hyperledger/fabric/mspAdmin && "
            +"peer channel update -o "+orderer_name+":"+orderer_port+" "
            +"--tls --cafile "+(intermediate ? "/etc/hyperledger/fabric/tls/ca-chain.pem" : "/etc/hyperledger/fabric/tls/tls-ca-cert.pem")+" "
            +"-c "+channel_name+" "
            +"-f /etc/hyperledger/fabric/"+org_name+"Anchors.tx'"
        );

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks if Go is installed by running 'go version'.
     * @return true if Go is installed, false otherwise
     */
    private static boolean check_go(){
        String output= executeWSLCommandToString("go version");
        if(output !=null && output.contains("go version")){
            return true;
        }
        return false;
    }

    /**
     * Installs Go using apt in WSL.
     * @param pin the sudo password
     * @throws IOException in case of I/O errors
     * @throws InterruptedException if the process is interrupted
     */
    private static void installGo(String pin) throws IOException, InterruptedException{
        ProcessBuilder pb = new ProcessBuilder(
                "wsl.exe", "--", "sudo", "-S", "sh", "-c", "sudo apt install golang-go"
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
    }

    private static void installjq(String pin) throws IOException, InterruptedException{
        ProcessBuilder pb = new ProcessBuilder(
                "wsl.exe", "--", "sudo", "-S", "sh", "-c", "sudo apt install jq"
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
    }

    private static void writeChaincode(){
        executeWSLCommand("cp $PWD/src/main/java/com/blockchain/blockchain/atcc.go "+mainDirectory+"/atcc/atcc.go");
    }

    public static String getPackageId(String containerName) {
        // Esegue il comando per interrogare i chaincode installati
        String command = "docker exec " + containerName + " bash -c 'export CORE_PEER_MSPCONFIGPATH=/etc/hyperledger/fabric/mspAdmin && peer lifecycle chaincode queryinstalled'";
        String output = executeWSLCommandToString(command);

        
        if (output.contains("Package ID: ")) {
            int start = output.indexOf("Package ID: ") + "Package ID: ".length();
            int end = output.indexOf(",", start);
            return output.substring(start, end).trim();
        }
        
        return null; // O gestisci l'errore se non trovato
    }
    
}






