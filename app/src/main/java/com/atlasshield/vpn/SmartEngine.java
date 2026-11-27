package com.atlasshield.vpn;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.SystemClock;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class SmartEngine {
    static final class Result {
        final ProfileStore.Profile profile; final long ms;
        Result(ProfileStore.Profile p,long ms){this.profile=p;this.ms=ms;}
    }
    private final AtlasApp app;
    SmartEngine(AtlasApp app){this.app=app;}
    Result select(ProfileStore.Data data) throws Exception {
        List<ProfileStore.Profile> candidates=new ArrayList<>();
        ProfileStore.Profile selected=data.selected();
        if(selected!=null)candidates.add(selected);
        for(ProfileStore.Profile p:data.profiles)if(p!=selected&&candidates.size()<4)candidates.add(p);
        List<Result> passed=new ArrayList<>();
        boolean keep=false;
        try{
            for(int i=0;i<candidates.size();i++){
                ProfileStore.Profile p=candidates.get(i);
                app.progress="Testing "+(i+1)+"/"+candidates.size()+" · "+p.name;
                try{app.connect(p);long ms=probe();if(ms>=0)passed.add(new Result(p,ms));}
                catch(Exception|LinkageError ignored){}
                finally{app.disconnect();}
            }
            passed.sort(Comparator.comparingLong(r->r.ms));
            for(Result r:passed){
                app.progress="Connecting · "+r.profile.name;
                try{app.connect(r.profile);long ms=probe();if(ms>=0){keep=true;return new Result(r.profile,ms);}}
                catch(Exception|LinkageError ignored){}
                app.disconnect();
            }
            app.error="No server passed verification";return null;
        }finally{if(!keep)app.disconnect();}
    }
    long probe() throws Exception {
        long deadline=SystemClock.elapsedRealtime()+3500;
        ConnectivityManager cm=(ConnectivityManager)app.getSystemService(Context.CONNECTIVITY_SERVICE);
        if(cm==null)return -1;
        Network n=null;
        while(SystemClock.elapsedRealtime()<deadline){
            Network current=cm.getActiveNetwork();NetworkCapabilities caps=current==null?null:cm.getNetworkCapabilities(current);
            if(caps!=null&&caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)&&app.stats().up){n=current;break;}
            SystemClock.sleep(150);
        }
        if(n==null)return -1;
        HttpsURLConnection c=(HttpsURLConnection)n.openConnection(new URL("https://connectivitycheck.gstatic.com/generate_204"));
        try{
            c.setConnectTimeout(5000);c.setReadTimeout(5000);c.setInstanceFollowRedirects(false);c.setUseCaches(false);
            long start=SystemClock.elapsedRealtime();int code=c.getResponseCode();long ms=SystemClock.elapsedRealtime()-start;
            AtlasApp.Snapshot s=app.stats();long age=System.currentTimeMillis()-s.handshake;
            return code==204&&s.up&&s.handshake>0&&age>=0&&age<30000&&n.equals(cm.getActiveNetwork())?ms:-1;
        }finally{c.disconnect();}
    }
}
