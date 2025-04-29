/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.blockchain.blockchain;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 *
 * @author simo0
 */
public class ServerCA {
    String admin;
    String admin_pwd;
    int port;
    ArrayList<String> csr_hosts;
    public ServerCA(String ad, String ad_pwd, int p, ArrayList<String> hosts){
        admin=ad;
        admin_pwd=ad_pwd;
        port=p;
        csr_hosts=hosts;
    }
    
    public String getAdmin(){
        return admin;
    }
    public String getAdminPwd(){
        return admin_pwd;
    }
    public int getPort(){
        return port;
    }
    
    public ArrayList<String> getCsrHosts(){
        return csr_hosts;
    }
    
    
}
