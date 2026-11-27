package com.atlasshield.vpn;

import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ServerHint {
    final String name, country;
    private static final Pattern SERVER = Pattern.compile("(?i)(?<![a-z0-9])([a-z]{2})-(?:FREE[#_-]?|)([0-9]{1,6})(?![a-z0-9])");
    private ServerHint(String name,String country){this.name=name;this.country=country;}
    static ServerHint from(String raw,String filename){
        for(String line:raw.split("\\r?\\n")){
            String l=line.trim();
            if(l.startsWith("#")){
                ServerHint h=match(l.substring(1));
                if(h!=null)return h;
            }
        }
        ServerHint h=match(filename==null?"":filename);
        if(h!=null)return h;
        String n=filename==null?"":filename.replaceFirst("(?i)\\.conf(?:\\.txt)?$","");
        return new ServerHint(n.length()>60?n.substring(0,60):n,"");
    }
    private static ServerHint match(String s){
        Matcher m=SERVER.matcher(s);
        if(!m.find())return null;
        String cc=m.group(1).toUpperCase(Locale.ROOT);
        if(!Arrays.asList(Locale.getISOCountries()).contains(cc))return null;
        return new ServerHint(m.group().toUpperCase(Locale.ROOT),cc);
    }
}
