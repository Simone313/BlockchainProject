package com.blockchain.blockchain;

import java.util.LinkedList;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class organization {
    @JsonProperty("organization_name") String organization_name;
    @JsonProperty("peers") LinkedList<String> peers;
    @JsonProperty("peer_ports") LinkedList<Integer> peer_ports;
    @JsonProperty("projectName") String projectName;
    @JsonProperty("peers_channels") LinkedList<String> peers_channels;
    // Usiamo @JsonCreator per dire a Jackson: "Usa questo costruttore specifico"
    @JsonCreator
    public organization(
        @JsonProperty("organization_name") String organization_name, 
        @JsonProperty("peers") LinkedList<String> peers, 
        @JsonProperty("peer_ports") LinkedList<Integer> peer_ports, 
        @JsonProperty("projectName") String projectName, 
        @JsonProperty("peers_channels") LinkedList<String> peers_channels
    ) {
        this.organization_name = organization_name;
        // Se nel file JSON i campi sono nulli o mancanti, evitiamo il NullPointerException
        this.peers = (peers != null) ? peers : new LinkedList<>();
        this.peer_ports = (peer_ports != null) ? peer_ports : new LinkedList<>();
        this.projectName = projectName;
        this.peers_channels = (peers_channels != null) ? peers_channels : new LinkedList<>();
    }

    public LinkedList<String> getPeers_channels() {
        return peers_channels;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getOrganization_name() {
        return organization_name;
    }
    public LinkedList<String> getPeers() {
        return peers;
    }

    public void setPeers(LinkedList<String> peers) {
        this.peers = peers;
    }

    public void setOrganization_name(String organization_name) {
        this.organization_name = organization_name;
    }

    public void setPeer_ports(LinkedList<Integer> peer_ports) {
        this.peer_ports = peer_ports;
    }
    public LinkedList<Integer> getPeer_ports() {
        return peer_ports;
    }

    

    public void removePeer(String peer) {
        int index = this.peers.indexOf(peer);
        if (index != -1) {
            this.peers.remove(index);
            this.peer_ports.remove(index);
        }
    }

    public void addPeer(String peer, String channel){
        this.peers.add(peer);
        this.peers_channels.add(channel);
    }
}
