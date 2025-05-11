/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.blockchain.blockchain;

/**
 *
 * @author simo0
 */
public class FabricOrganizationServerThread extends Thread{
    String directory;
    String name;
    String psw;
    public FabricOrganizationServerThread(int i, String name, String psw){
        directory="fabric-ca-server-org"+(i+1);
        this.name=name;
        this.psw=psw;
        this.start();
        
    }
    
    public void run(){
        Blockchain.executeWSLCommand("cd Prova/"+directory+" &&"
                + "docker compose down");
        Blockchain.executeWSLCommand("cd Prova/"+directory+" &&"
                + "docker compose up");
        
        Blockchain.executeWSLCommand("cd "+Blockchain.mainDirectory+"/fabric-ca-client &&"
                + "./fabric-ca-client enroll -d -u https://"+name+":"+psw+"@127.0.0.1:7055 --tls.certfiles tls-root-cert/tls-ca-cert.pem --mspdir org1-ca/rcaadmin/msp");
    }
}
