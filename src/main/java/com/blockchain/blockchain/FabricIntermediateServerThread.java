/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.blockchain.blockchain;

/**
 *
 * @author simo0
 */
public class FabricIntermediateServerThread extends Thread{
    String name;
    String psw;
    public FabricIntermediateServerThread(String name, String psw){
        this.name=name;
        this.psw=psw;
        this.start();
    }
    
    public void run(){
        Blockchain.executeWSLCommand("cd Prova &&"
                + "docker compose down");
        Blockchain.executeWSLCommand("cd Prova &&"
                + "docker compose up");
        
        Blockchain.executeWSLCommand("cd "+Blockchain.mainDirectory+"/fabric-ca-client &&"
                + "./fabric-ca-client enroll -d -u https://"+name+":"+psw+"@127.0.0.1:7055 --tls.certfiles tls-root-cert/tls-ca-cert.pem --mspdir int-ca/icaadmin/msp");

    }
}
