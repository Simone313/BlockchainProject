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
    public FabricServerThread(int i){
        if(i==0){
            directory="fabric-ca-server";
        }else{
            directory="fabric-ca-server"+i;
        }
        this.start();
    }
    
    public void run(){
        Blockchain.executeWSLCommand("cd Prova/"+directory+" &&"
                + "docker compose up");
    }
    
    public void kill(){
        this.kill();
        
    }
}
