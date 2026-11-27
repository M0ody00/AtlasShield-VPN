package com.atlasshield.vpn;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.AtomicFile;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

final class ProfileStore {
    private static final String ALIAS = "atlas_profiles_v1";
    private final AtomicFile file;
    ProfileStore(Context c) { file = new AtomicFile(new File(c.getNoBackupFilesDir(), "profiles.enc")); }
    static final class Profile {
        final String id, name, country, config;
        Profile(String id, String name, String country, String config) { this.id=id; this.name=name; this.country=country; this.config=config; }
    }
    static final class Data {
        final List<Profile> profiles = new ArrayList<>();
        String selected = "";
        Profile selected() { for (Profile p : profiles) if (p.id.equals(selected)) return p; return null; }
    }
    private SecretKey key() throws Exception {
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore"); ks.load(null);
        if (ks.containsAlias(ALIAS)) return (SecretKey)ks.getKey(ALIAS,null);
        KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build());
        return generator.generateKey();
    }
    synchronized Data load() throws Exception {
        Data data=new Data();
        byte[] raw;
        try { raw=file.readFully(); } catch(FileNotFoundException ex) { return data; }
        if(raw.length<30 || raw[0]!=1) throw new IOException("Invalid vault");
        Cipher c=Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,raw,1,12));
        byte[] plain=c.doFinal(raw,13,raw.length-13);
        try {
            JSONObject root=new JSONObject(new String(plain,StandardCharsets.UTF_8));
            data.selected=root.optString("selected",""); JSONArray a=root.getJSONArray("profiles");
            for(int i=0;i<a.length();i++) { JSONObject p=a.getJSONObject(i); data.profiles.add(new Profile(p.getString("id"),p.getString("name"),p.getString("country"),p.getString("config"))); }
            return data;
        } finally { Arrays.fill(plain,(byte)0); }
    }
    synchronized void save(Data data) throws Exception {
        JSONArray a=new JSONArray();
        for(Profile p:data.profiles) a.put(new JSONObject().put("id",p.id).put("name",p.name).put("country",p.country).put("config",p.config));
        byte[] plain=new JSONObject().put("selected",data.selected).put("profiles",a).toString().getBytes(StandardCharsets.UTF_8);
        Cipher c=Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.ENCRYPT_MODE,key());
        byte[] iv=c.getIV(); if(iv.length!=12) throw new IOException("Unexpected IV");
        byte[] enc; try { enc=c.doFinal(plain); } finally { Arrays.fill(plain,(byte)0); }
        FileOutputStream out=file.startWrite();
        try { out.write(1); out.write(iv); out.write(enc); file.finishWrite(out); }
        catch(Exception ex) { file.failWrite(out); throw ex; }
    }
}
