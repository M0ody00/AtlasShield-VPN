package com.atlasshield.vpn;

import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

final class ProtonDiscovery {
    private final AtlasApp app;
    private final JSONArray rows;
    private static final Pattern FREE=Pattern.compile("[A-Z]{2}-FREE[#_-]?[0-9]+",Pattern.CASE_INSENSITIVE);
    static final class Outcome { int tested,added; SmartEngine.Result connected; }
    ProtonDiscovery(AtlasApp app) throws Exception {
        this.app=app;
        try(InputStream in=app.getAssets().open("proton_free_catalog.json");ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){if(out.size()+n>262144)throw new IOException();out.write(b,0,n);}
            rows=new JSONObject(new String(out.toByteArray(),StandardCharsets.UTF_8)).getJSONArray("servers");
        }
    }
    String[] countries() throws Exception {
        TreeSet<String> set=new TreeSet<>();for(int i=0;i<rows.length();i++)set.add(rows.getJSONObject(i).getString("country"));return set.toArray(new String[0]);
    }
    private static boolean isProton(ProfileStore.Profile p){
        if(p==null)return false;
        String raw=p.config;
        return FREE.matcher(raw+" "+p.name).find()&&Pattern.compile("(?im)^\\s*DNS\\s*=\\s*10\\.2\\.0\\.1\\s*$").matcher(raw).find()
            &&Pattern.compile("(?im)^\\s*Address\\s*=\\s*10\\.2\\.0\\.2/32(?:\\s*,[^\\r\\n]+)?\\s*$").matcher(raw).find()
            &&!Pattern.compile("(?im)^\\s*PresharedKey\\s*=").matcher(raw).find();
    }
    private static String candidate(ProfileStore.Profile seed,JSONObject row) throws Exception {
        String[] parts=seed.config.split("(?im)^\\s*\\[Peer\\]\\s*$",2);
        if(parts.length!=2)throw new IOException();
        String name=row.getString("name"),key=row.getString("publicKey"),ip=row.getString("ip");
        if(!FREE.matcher(name).matches()||!key.matches("[A-Za-z0-9+/]{43}=")||!ip.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}"))throw new IOException();
        String result=parts[0].trim()+"\n\n[Peer]\n# "+name+"\nPublicKey = "+key+"\nAllowedIPs = 0.0.0.0/0, ::/0\nEndpoint = "+ip+":51820\nPersistentKeepalive = 25\n";
        AtlasApp.validate(result);return result;
    }
    Outcome discover(ProfileStore.Data data,String country) throws Exception {
        ProfileStore.Profile seed=data.selected();if(!isProton(seed)){seed=null;for(ProfileStore.Profile p:data.profiles)if(isProton(p)){seed=p;break;}}
        if(seed==null)throw new IOException("Proton FREE profile required");
        AtlasApp.validate(seed.config);
        ArrayList<JSONObject> candidates=new ArrayList<>();
        for(int i=0;i<rows.length();i++){JSONObject row=rows.getJSONObject(i);if(country.isEmpty()||country.equals(row.getString("country")))candidates.add(row);}
        Collections.shuffle(candidates);
        if(country.isEmpty()){
            LinkedHashMap<String,JSONObject> distinct=new LinkedHashMap<>();for(JSONObject row:candidates)distinct.putIfAbsent(row.getString("country"),row);
            ArrayList<JSONObject> ordered=new ArrayList<>(distinct.values());for(JSONObject row:candidates)if(!ordered.contains(row))ordered.add(row);candidates=ordered;
        }
        SmartEngine test=new SmartEngine(app);ArrayList<SmartEngine.Result> passed=new ArrayList<>();Outcome outcome=new Outcome();boolean keep=false;
        try{
            for(JSONObject row:candidates){
                if(outcome.tested>=6)break;
                String raw=candidate(seed,row);
                String name=row.getString("name");
                ProfileStore.Profile p=null;for(ProfileStore.Profile old:data.profiles)if(old.config.equals(raw)){p=old;break;}
                if(p==null)p=new ProfileStore.Profile(UUID.randomUUID().toString(),name,row.getString("country"),raw);
                outcome.tested++;app.progress="Discovering "+outcome.tested+"/6 · "+name;
                try{app.connect(p);long ms=test.probe();if(ms>=0){
                    passed.add(new SmartEngine.Result(p,ms));
                    boolean known=false;for(ProfileStore.Profile old:data.profiles)if(old.id.equals(p.id)){known=true;break;}
                    if(!known&&data.profiles.size()<200){data.profiles.add(p);outcome.added++;}
                }}catch(Exception|LinkageError ignored){}finally{app.disconnect();}
            }
            app.store.save(data);
            passed.sort(Comparator.comparingLong(r->r.ms));
            for(SmartEngine.Result r:passed){
                boolean saved=false;for(ProfileStore.Profile old:data.profiles)if(old.id.equals(r.profile.id)){saved=true;break;}if(!saved)continue;
                try{app.progress="Connecting · "+r.profile.name;app.connect(r.profile);long ms=test.probe();if(ms>=0){
                    data.selected=r.profile.id;app.store.save(data);outcome.connected=new SmartEngine.Result(r.profile,ms);keep=true;break;
                }}catch(Exception|LinkageError ignored){}app.disconnect();
            }
            if(!keep)app.error="No catalog server accepted the key and passed verification";
            return outcome;
        }finally{if(!keep)app.disconnect();}
    }
}
