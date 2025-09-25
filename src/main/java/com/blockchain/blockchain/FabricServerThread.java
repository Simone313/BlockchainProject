/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.blockchain.blockchain;


/**
 *
 * @author simo0
 */
public class FabricServerThread extends Thread{
    String directory;
    String mainDir;
    public FabricServerThread(int i, String mainDir){
        directory=Blockchain.fabric_ca_server_name;
        this.mainDir=mainDir;
        this.start();
    }
    
    public void run(){
        Blockchain.executeWSLCommand("cd "+mainDir+"/"+directory+" &&"
                + "docker compose up -d");
    }
    
    
}
