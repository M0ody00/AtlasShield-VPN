package com.atlasshield.vpn;

import android.app.Application;
import android.net.VpnService;
import android.os.SystemClock;
import com.wireguard.android.backend.GoBackend;
import com.wireguard.android.backend.Statistics;
import com.wireguard.android.backend.Tunnel;
import com.wireguard.config.Config;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public final class AtlasApp extends Application {
    final ExecutorService worker=Executors.newSingleThreadExecutor();
    ProfileStore store;
    private GoBackend backend;
    volatile String activeId="", error="";
    volatile boolean busy=false;
    volatile long started=0;
    private final Tunnel tunnel=new Tunnel() {
        @Override public String getName(){ return "AtlasShield"; }
        @Override public void onStateChange(State state){ if(state==State.DOWN){ activeId=""; started=0; } }
    };
    @Override public void onCreate(){
        super.onCreate(); store=new ProfileStore(this);
        GoBackend.setAlwaysOnCallback(() -> worker.execute(() -> {
            if(activeId.isEmpty()) try { ProfileStore.Profile p=store.load().selected(); if(p!=null && VpnService.prepare(this)==null) connect(p); }
            catch(Exception | LinkageError e){ error="Always-on could not start. Check your profile."; }
        }));
    }
    private GoBackend backend(){ if(backend==null) backend=new GoBackend(this); return backend; }
    static Config parse(String raw) throws Exception {
        return Config.parse(new ByteArrayInputStream(raw.getBytes(StandardCharsets.UTF_8)));
    }
    static Config validate(String raw) throws Exception {
        Config c=parse(raw);
        if(c.getPeers().size()!=1) throw new IllegalArgumentException("Use one gateway peer per profile");
        if(c.getInterface().getAddresses().isEmpty() || c.getInterface().getDnsServers().isEmpty()) throw new IllegalArgumentException("Address and DNS required");
        if(!c.getInterface().getExcludedApplications().isEmpty() || !c.getInterface().getIncludedApplications().isEmpty()) throw new IllegalArgumentException("App bypass is not supported");
        if(!c.getPeers().get(0).getEndpoint().isPresent()) throw new IllegalArgumentException("Endpoint required");
        boolean v4=false,v6=false;
        for(com.wireguard.config.InetNetwork n:c.getPeers().get(0).getAllowedIps()) {
            if(n.getMask()==0) { if(n.getAddress() instanceof java.net.Inet4Address) v4=true; else if(n.getAddress() instanceof java.net.Inet6Address) v6=true; }
        }
        if(!v4 || !v6) throw new IllegalArgumentException("Full IPv4 and IPv6 routes required");
        return c;
    }
    void connect(ProfileStore.Profile p) throws Exception {
        Config c=validate(p.config);
        if(backend().getState(tunnel)==Tunnel.State.UP) backend().setState(tunnel,Tunnel.State.DOWN,null);
        if(backend().setState(tunnel,Tunnel.State.UP,c)!=Tunnel.State.UP) throw new IllegalStateException("Tunnel not up");
        activeId=p.id; started=SystemClock.elapsedRealtime(); error="";
    }
    void disconnect() throws Exception { if(backend!=null) backend.setState(tunnel,Tunnel.State.DOWN,null); activeId=""; started=0; error=""; }
    static final class Snapshot { boolean up; long rx,tx,handshake,elapsed; }
    Snapshot stats() throws Exception {
        Snapshot s=new Snapshot(); if(backend==null || backend.getState(tunnel)!=Tunnel.State.UP) return s;
        s.up=true; s.elapsed=Math.max(0,SystemClock.elapsedRealtime()-started);
        Statistics st=backend.getStatistics(tunnel); s.rx=st.totalRx(); s.tx=st.totalTx();
        for(com.wireguard.crypto.Key k:st.peers()) { Statistics.PeerStats p=st.peer(k); if(p!=null) s.handshake=Math.max(s.handshake,p.latestHandshakeEpochMillis()); }
        return s;
    }
}
