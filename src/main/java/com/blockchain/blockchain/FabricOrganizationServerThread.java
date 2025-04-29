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
    public FabricOrganizationServerThread(int i){
        directory="fabric-ca-server-org"+(i+1);
        this.start();
    }
    
    public void run(){
        Blockchain.executeWSLCommand("cd Prova/"+directory+" &&"
                + "./fabric-ca-server start");
    }
}
