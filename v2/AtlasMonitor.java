package com.protonvpn.android.redesign.home_screen.ui;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.*;
import android.os.*;
import java.util.*;
import java.text.DateFormat;

public final class AtlasMonitor {
    private static AtlasMonitor instance;
    private final Context context;
    private final ConnectivityManager cm;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ArrayDeque<String> events = new ArrayDeque<>();
    private boolean wasVpn, expectedStop;
    private long pendingLoss;
    private AtlasMonitor(Context c) {
        context=c.getApplicationContext();
        cm=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        wasVpn=snapshot().vpn;
        if(cm!=null) {
            try { cm.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback(){
                @Override public void onAvailable(Network n){schedule();}
                @Override public void onLost(Network n){schedule();}
                @Override public void onCapabilitiesChanged(Network n,NetworkCapabilities caps){schedule();}
                @Override public void onLinkPropertiesChanged(Network n,LinkProperties props){schedule();}
            }); } catch(RuntimeException ignored){}
        }
    }
    public static synchronized AtlasMonitor install(Context c) {
        if(instance==null)instance=new AtlasMonitor(c);
        return instance;
    }
    public static final class State {
        public boolean vpn,network,validated,captive,dns,v4,v6;
        public String transport="Unknown";
    }
    public State snapshot(){
        State out=new State();
        if(cm==null)return out;
        try {
            Network n=cm.getActiveNetwork();
            if(n==null)return out;
            NetworkCapabilities caps=cm.getNetworkCapabilities(n);
            if(caps==null)return out;
            out.network=true;out.vpn=caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN);
            out.validated=caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            out.captive=caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL);
            out.transport=out.vpn?"VPN":caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)?"Wi-Fi":caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)?"Mobile data":"Other";
            LinkProperties lp=cm.getLinkProperties(n);
            if(lp!=null){
                out.dns=!lp.getDnsServers().isEmpty();
                for(RouteInfo route:lp.getRoutes())if(route.isDefaultRoute()){
                    if(route.getDestination().getAddress() instanceof java.net.Inet4Address)out.v4=true;
                    if(route.getDestination().getAddress() instanceof java.net.Inet6Address)out.v6=true;
                }
            }
        }catch(RuntimeException ignored){}
        return out;
    }
    private final Runnable evaluate=()->{
        State s=snapshot();
        if(wasVpn&&!s.vpn){
            if(expectedStop){expectedStop=false;add("VPN disconnected by user");}
            else{pendingLoss=SystemClock.elapsedRealtime();add("VPN no longer the active network");notifyLoss();}
        }
        if(!wasVpn&&s.vpn){pendingLoss=0;expectedStop=false;add("VPN network detected");}
        wasVpn=s.vpn;
    };
    private void schedule(){handler.removeCallbacks(evaluate);handler.postDelayed(evaluate,1600);}
    public void expectDisconnect(){expectedStop=true;handler.postDelayed(()->expectedStop=false,10000);}
    private synchronized void add(String event){
        events.addFirst(DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.MEDIUM).format(new Date())+"  •  "+event);
        while(events.size()>30)events.removeLast();
    }
    public synchronized String history(){return events.isEmpty()?"No network events in this app session.":String.join("\n",events);}
    private void notifyLoss(){
        if(Build.VERSION.SDK_INT>=33&&context.checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=PackageManager.PERMISSION_GRANTED)return;
        try{
            NotificationManager nm=context.getSystemService(NotificationManager.class);
            if(nm==null)return;
            nm.createNotificationChannel(new NotificationChannel("atlas_protection","AtlasShield protection alerts",NotificationManager.IMPORTANCE_HIGH));
            Intent settings=new Intent(android.provider.Settings.ACTION_VPN_SETTINGS);
            PendingIntent pi=PendingIntent.getActivity(context,21,settings,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
            Notification note=new Notification.Builder(context,"atlas_protection")
              .setSmallIcon(com.protonvpn.android.R.drawable.atlas_globe)
              .setContentTitle("AtlasShield • VPN interrupted")
              .setContentText("VPN no longer active. Check Android VPN lockdown.")
              .setStyle(new Notification.BigTextStyle().bigText("VPN is no longer the active network. This is not proof of spying. Enable Android's block connections without VPN to prevent unprotected traffic."))
              .setAutoCancel(true).setContentIntent(pi).build();
            nm.notify(9102,note);
        }catch(RuntimeException ignored){}
    }
}
