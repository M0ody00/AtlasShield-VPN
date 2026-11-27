package com.atlasshield.vpn;

import android.app.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.net.VpnService;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class MainActivity extends Activity {
    private static final int BG=0xff091321, CARD=0xff142437, MINT=0xff58e2bb, WHITE=0xfff3f7fb, MUTED=0xffa8b9cc;
    private final Handler h=new Handler(Looper.getMainLooper());
    private AtlasApp app;
    private ProfileStore.Data data;
    private TextView status, detail, traffic, profile, count;
    private Button connect, choose, add, proton, smart, discover;
    private ProfileStore.Profile pending;
    private boolean resumed=false, refreshing=false, loading=true;
    private final Runnable tick=new Runnable(){ public void run(){ if(!resumed)return; refresh(); h.postDelayed(this,2000); }};
    private int dp(int x){return Math.round(x*getResources().getDisplayMetrics().density);}
    private boolean ar(){return Locale.getDefault().getLanguage().equals("ar");}
    private String tr(String en,String ar){return ar()?ar:en;}
    @Override public void onCreate(Bundle state){
        super.onCreate(state); app=(AtlasApp)getApplication();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(BG);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(22),dp(24),dp(22),dp(24));
        if(Build.VERSION.SDK_INT>=35) root.setOnApplyWindowInsetsListener((v,i)->{ android.graphics.Insets bars=i.getInsets(WindowInsets.Type.systemBars()); v.setPadding(dp(22),dp(24)+bars.top,dp(22),dp(24)+bars.bottom);return i; });
        scroll.addView(root); setContentView(scroll);
        TextView brand=text("ATLAS / SHIELD",25,WHITE,true); root.addView(brand); root.addView(text("PRIVATE NETWORK  /  WIREGUARD",11,MINT,true));
        LinearLayout hero=panel(root); hero.setGravity(Gravity.CENTER_HORIZONTAL); hero.setPadding(dp(20),dp(24),dp(20),dp(24));
        ImageView shield=new ImageView(this); shield.setImageResource(com.atlasshield.vpn.R.drawable.ic_shield); hero.addView(shield,new LinearLayout.LayoutParams(dp(96),dp(96)));
        status=text(tr("Starting…","جارٍ التحضير…"),25,WHITE,true); status.setGravity(Gravity.CENTER); hero.addView(status);
        detail=text("",13,MUTED,false); detail.setGravity(Gravity.CENTER); hero.addView(detail); gap(hero,18);
        connect=button(tr("Connect","اتصال"),true); hero.addView(connect,new LinearLayout.LayoutParams(-1,dp(56))); connect.setEnabled(false); connect.setOnClickListener(v->toggle());
        smart=button(tr("Smart Connect · test servers","اتصال ذكي · اختبار السيرفرات"),false);hero.addView(smart);smart.setEnabled(false);smart.setOnClickListener(v->startSmart());
        discover=button(tr("Discover Proton FREE servers","اكتشاف سيرفرات Proton المجانية"),true);hero.addView(discover);discover.setEnabled(false);discover.setOnClickListener(v->discoverDialog());
        hero.addView(text(tr("53 catalog entries · verify on your network","٥٣ سيرفرًا بالقائمة · يتم اختبار الاتصال على شبكتك"),12,MUTED,false));
        LinearLayout location=panel(root); location.addView(text(tr("YOUR LOCATION","موقع السيرفر"),11,MINT,true));
        profile=text(tr("No server imported","لا يوجد سيرفر مضاف"),20,WHITE,true); location.addView(profile); count=text("",13,MUTED,false); location.addView(count); gap(location,12);
        choose=button(tr("Select server","اختيار السيرفر"),false); location.addView(choose); choose.setOnClickListener(v->pick());
        add=button(tr("Import WireGuard profiles","استيراد ملفات WireGuard"),false); location.addView(add); add.setOnClickListener(v->importFile());
        proton=button(tr("Get servers from Proton","إضافة سيرفرات من Proton"),false); location.addView(proton); proton.setOnClickListener(v->openProton());
        LinearLayout metrics=panel(root); metrics.addView(text(tr("SESSION / LIVE TRAFFIC","الجلسة / استهلاك البيانات"),11,MINT,true));
        traffic=text("↓ 0 B     ↑ 0 B",22,WHITE,true); metrics.addView(traffic);
        LinearLayout security=panel(root); security.addView(text(tr("PRIVACY & SECURITY","الخصوصية والأمان"),11,MINT,true));
        security.addView(text(tr("Encrypted local profile storage. No advertising SDK or analytics. For a kill switch, enable Android's Always-on VPN and Block connections without VPN.","حفظ ملفات الاتصال مشفرة محليًا، دون مكتبات إعلانات أو تحليلات. لتفعيل Kill Switch استخدم VPN دائم التشغيل وحظر الاتصال دون VPN من إعدادات أندرويد."),14,MUTED,false));
        Button settings=button(tr("Android VPN settings","إعدادات VPN في أندرويد"),false); security.addView(settings); settings.setOnClickListener(v->{try{startActivity(new Intent(Settings.ACTION_VPN_SETTINGS));}catch(Exception e){toast(tr("Open VPN settings manually","افتح إعدادات VPN يدويًا"));}});
        Button remove=button(tr("Remove selected profile","حذف السيرفر المحدد"),false); security.addView(remove); remove.setOnClickListener(v->remove());
        gap(root,12); root.addView(text("ATLAS SHIELD  /  1.1.0  /  DISCOVERY BETA",11,MUTED,true));
        app.worker.execute(()->{try{ProfileStore.Data d=app.store.load();runOnUiThread(()->{data=d;loading=false;updateProfile();});}catch(Exception e){runOnUiThread(()->{loading=false;toast(tr("Cannot read encrypted profiles","تعذر قراءة الملفات المشفرة"));});}});
    }
    @Override protected void onResume(){super.onResume();resumed=true;h.removeCallbacks(tick);h.post(tick);}
    @Override protected void onPause(){resumed=false;h.removeCallbacks(tick);super.onPause();}
    private TextView text(String s,int size,int color,boolean bold){ TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setLineSpacing(dp(3),1f);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t; }
    private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private LinearLayout panel(LinearLayout parent){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setBackground(shape(CARD,20));l.setPadding(dp(16),dp(16),dp(16),dp(16));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(16);parent.addView(l,p);return l;}
    private Button button(String s,boolean primary){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(15);b.setTextColor(primary?BG:WHITE);b.setBackgroundTintList(ColorStateList.valueOf(primary?MINT:0xff263d52));b.setMinHeight(dp(46));return b;}
    private void gap(LinearLayout l,int n){View v=new View(this);l.addView(v,new LinearLayout.LayoutParams(1,dp(n)));}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    private String country(String code){return code==null||code.isEmpty()?tr("Unknown country","دولة غير محددة"):new Locale("",code).getDisplayCountry(Locale.getDefault());}
    private void updateProfile(){if(data==null)return;ProfileStore.Profile p=data.selected();profile.setText(p==null?tr("No server imported","لا يوجد سيرفر مضاف"):country(p.country)+" / "+p.name);count.setText(data.profiles.size()+tr(" imported profiles · locations are user-labelled"," ملف اتصال مضاف · المواقع يحددها المستخدم"));}
    private void refresh(){if(refreshing)return;if(app.busy){status.setText(tr("Working…","جارٍ التنفيذ…"));connect.setEnabled(false);choose.setEnabled(false);add.setEnabled(false);smart.setEnabled(false);discover.setEnabled(false);if(!app.progress.isEmpty())detail.setText(app.progress);return;}refreshing=true;
        app.worker.execute(()->{AtlasApp.Snapshot s=null;try{s=app.stats();}catch(Exception|LinkageError e){app.error="Cannot read VPN state";}final AtlasApp.Snapshot x=s;runOnUiThread(()->{refreshing=false;if(isFinishing()||isDestroyed())return;
            boolean up=x!=null&&x.up, known=x!=null;long age=up&&x.handshake>0?System.currentTimeMillis()-x.handshake:Long.MAX_VALUE;boolean recent=age>=0&&age<180000;
            status.setText(!known?tr("State unknown","حالة غير معروفة"):up?(recent?tr("Handshake verified","تم التحقق من الاتصال"):tr("Tunnel active","النفق نشط")):tr("Disconnected","غير متصل")); status.setTextColor(up&&recent?MINT:WHITE);
            detail.setText(!app.error.isEmpty()?tr("VPN error. Check profile and network.","خطأ في VPN؛ تحقق من الملف والشبكة."):up?(recent?tr("Recent authenticated handshake · internet not tested","مصافحة مشفرة حديثة · لم يتم اختبار الإنترنت"):tr("Waiting for a recent server handshake","في انتظار مصافحة حديثة مع السيرفر")):tr("Import a trusted server to get started","أضف سيرفرًا موثوقًا للبدء"));
            traffic.setText(up?"↓ "+bytes(x.rx)+"     ↑ "+bytes(x.tx)+"\n"+duration(x.elapsed):"↓ 0 B     ↑ 0 B");
            connect.setText(up?tr("Disconnect","قطع الاتصال"):tr("Connect","اتصال"));connect.setEnabled(!app.busy&&(up||!loading&&data!=null&&data.selected()!=null));choose.setEnabled(!loading&&!app.busy&&!up&&data!=null);add.setEnabled(!loading&&!app.busy&&!up&&data!=null);smart.setEnabled(known&&!loading&&!app.busy&&!up&&data!=null&&!data.profiles.isEmpty());discover.setEnabled(known&&!loading&&!app.busy&&!up&&data!=null);
        });});
    }
    private static String bytes(long n){if(n<1024)return n+" B";if(n<1048576)return String.format(Locale.US,"%.1f KB",n/1024.0);return String.format(Locale.US,"%.1f MB",n/1048576.0);}
    private static String duration(long ms){long s=ms/1000;return String.format(Locale.US,"%02d:%02d:%02d",s/3600,s/60%60,s%60);}
    private void toggle(){if(app.busy)return;if(!app.activeId.isEmpty()){runVpn(null);return;}if(data==null)return;pending=data.selected();if(pending==null)return;Intent i=VpnService.prepare(this);if(i!=null)startActivityForResult(i,11);else{runVpn(pending);pending=null;}}
    private void runVpn(ProfileStore.Profile p){app.busy=true;connect.setEnabled(false);app.worker.execute(()->{try{if(p==null)app.disconnect();else app.connect(p);}catch(Exception|LinkageError e){app.error="VPN operation failed";}finally{app.busy=false;runOnUiThread(()->{if(resumed)refresh();});}});}
    private void openProton(){
        try { startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://account.protonvpn.com/downloads"))); }
        catch(ActivityNotFoundException e){ toast(tr("Open account.protonvpn.com/downloads in your browser","افتح account.protonvpn.com/downloads في متصفحك")); }
    }
    private void importFile(){if(app.busy||data==null||!app.activeId.isEmpty())return;Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);startActivityForResult(i,12);}
    @Override protected void onActivityResult(int req,int result,Intent intent){super.onActivityResult(req,result,intent);if(req==11){ProfileStore.Profile p=pending!=null?pending:data==null?null:data.selected();pending=null;if(result==RESULT_OK&&p!=null)runVpn(p);return;}if(req==14){if(result==RESULT_OK)runDiscovery();return;}if(req==13&&result==RESULT_OK){runSmart();return;}if(req==12&&result==RESULT_OK&&intent!=null){ArrayList<Uri> uris=new ArrayList<>();ClipData clips=intent.getClipData();if(clips!=null){for(int j=0;j<clips.getItemCount()&&j<50;j++){Uri u=clips.getItemAt(j).getUri();if(u!=null&&!uris.contains(u))uris.add(u);}}else if(intent.getData()!=null)uris.add(intent.getData());if(uris.size()==1)readFile(uris.get(0));else if(!uris.isEmpty())confirmBulk(uris);}}
    private String discoveryCountry="";
    private void discoverDialog(){
        if(app.busy||data==null||!app.activeId.isEmpty())return;
        if(data.profiles.isEmpty()){
            new AlertDialog.Builder(this).setTitle(tr("One Proton key needed","مطلوب ملف Proton واحد"))
              .setMessage(tr("Import one official Proton FREE WireGuard file once. Discovery then tries other catalog servers locally, without sending your key to any website.","استورد ملف WireGuard مجانيًا رسميًا من Proton مرة واحدة. بعدها يحاول التطبيق الاتصال بسيرفرات القائمة محليًا دون إرسال مفتاحك لأي موقع."))
              .setNegativeButton(android.R.string.cancel,null).setPositiveButton(tr("Import","استيراد"),(d,w)->importFile()).setNeutralButton("Proton",(d,w)->openProton()).show();return;
        }
        try{
            ProtonDiscovery catalog=new ProtonDiscovery(app);
            String[] codes=catalog.countries();
            String[] labels=new String[codes.length+1];labels[0]=tr("Automatic · test different countries","تلقائي · اختبار دول مختلفة");
            for(int i=0;i<codes.length;i++)labels[i+1]=country(codes[i]);
            new AlertDialog.Builder(this).setTitle(tr("Discover free servers","اكتشاف السيرفرات المجانية"))
                .setItems(labels,(d,w)->{discoveryCountry=w==0?"":codes[w-1];
                    new AlertDialog.Builder(this).setTitle(tr("Test catalog servers","اختبار سيرفرات القائمة"))
                      .setMessage(tr("Community-maintained Gluetun catalog, not an official Proton API. Up to 6 candidates are tested with your existing local Proton key. Only a verified WireGuard + HTTPS connection is saved. Proton may reject the key. Enable Android VPN lockdown to prevent traffic escaping between tests.","قائمة Gluetun مفتوحة المصدر وليست واجهة Proton رسمية. يتم اختبار حتى ٦ سيرفرات بمفتاح Proton الموجود على هاتفك. لا يحفظ التطبيق إلا اتصالًا نجح في WireGuard وHTTPS. قد يرفض Proton المفتاح. فعّل حظر الاتصال بدون VPN للحماية بين الاختبارات."))
                      .setNegativeButton(android.R.string.cancel,null).setPositiveButton(tr("Discover and connect","اكتشاف واتصال"),(x,y)->{Intent i=VpnService.prepare(this);if(i!=null)startActivityForResult(i,14);else runDiscovery();}).show();
                }).setNegativeButton(android.R.string.cancel,null).show();
        }catch(Exception e){toast(tr("Server catalog unavailable","قائمة السيرفرات غير متاحة"));}
    }
    private void runDiscovery(){
        if(app.busy||!app.activeId.isEmpty())return;app.busy=true;discover.setEnabled(false);final String filter=discoveryCountry;
        app.worker.execute(()->{try{
            ProfileStore.Data latest=app.store.load();
            ProtonDiscovery.Outcome outcome=new ProtonDiscovery(app).discover(latest,filter);
            runOnUiThread(()->{data=latest;updateProfile();new AlertDialog.Builder(this).setTitle(tr("Discovery result","نتيجة الاكتشاف"))
                .setMessage(tr("Tested: ","تم اختبار: ")+outcome.tested+tr("\nNew verified servers: ","\nسيرفرات جديدة مؤكدة: ")+outcome.added+tr("\nConnected: ","\nمتصل بـ: ")+(outcome.connected==null?tr("None — key or network may not support these servers","لا يوجد — قد لا يدعم المفتاح أو الشبكة هذه السيرفرات"):outcome.connected.profile.name+" · "+outcome.connected.ms+" ms")).setPositiveButton(android.R.string.ok,null).show();});
        }catch(Exception|LinkageError e){app.error="Discovery failed";runOnUiThread(()->toast(tr("Discovery requires a valid Proton FREE profile and working network","الاكتشاف يتطلب ملف Proton مجاني صالح وشبكة تعمل")));}
        finally{app.progress="";app.busy=false;runOnUiThread(()->{if(resumed)refresh();});}});
    }
    private void startSmart(){
        if(app.busy||data==null||data.profiles.isEmpty()||!app.activeId.isEmpty())return;
        new AlertDialog.Builder(this).setTitle(tr("Smart Connect","اتصال ذكي"))
            .setMessage(tr("Tests up to 4 imported servers using an HTTPS connectivity check through the VPN. Enable Android's block-connections-without-VPN setting for protection while switching. This measures response time, not download speed.","يختبر حتى ٤ سيرفرات مستوردة عبر طلب HTTPS داخل النفق. فعّل حظر الاتصال بدون VPN من إعدادات أندرويد للحماية أثناء التبديل. الاختبار يقيس زمن الاستجابة وليس سرعة التنزيل."))
            .setNegativeButton(android.R.string.cancel,null).setPositiveButton(tr("Test and connect","اختبار واتصال"),(d,w)->{Intent i=VpnService.prepare(this);if(i!=null)startActivityForResult(i,13);else runSmart();}).show();
    }
    private void runSmart(){
        if(app.busy||!app.activeId.isEmpty())return;app.busy=true;smart.setEnabled(false);
        app.worker.execute(()->{try{
            ProfileStore.Data latest=app.store.load();
            SmartEngine.Result result=new SmartEngine(app).select(latest);
            if(result!=null){latest.selected=result.profile.id;app.store.save(latest);runOnUiThread(()->{data=latest;updateProfile();toast(tr("Selected tested server: ","تم اختيار سيرفر مختبر: ")+result.profile.name+" · "+result.ms+" ms");});}
            else runOnUiThread(()->toast(tr("No imported server passed the connection test","لم ينجح أي سيرفر مستورد في اختبار الاتصال")));
        }catch(Exception|LinkageError e){app.error="Smart connection failed";runOnUiThread(()->toast(tr("Smart connection failed","تعذر الاتصال الذكي")));}
        finally{app.progress="";app.busy=false;runOnUiThread(()->{if(resumed)refresh();});}});
    }
    private void confirmBulk(ArrayList<Uri> uris){
        if(app.busy||data==null||!app.activeId.isEmpty())return;
        new AlertDialog.Builder(this).setTitle(tr("Import trusted profiles?","استيراد ملفات موثوقة؟"))
            .setMessage(tr("Only import configurations from your own account or a trusted administrator. Unrecognized locations are marked unknown.","استورد فقط إعدادات حسابك أو جهة موثوقة. المواقع غير المعروفة ستظهر بدون دولة محددة."))
            .setNegativeButton(android.R.string.cancel,null).setPositiveButton(tr("Import","استيراد"),(d,w)->bulk(uris)).show();
    }
    private String readConfig(Uri uri) throws Exception {
        try(InputStream in=getContentResolver().openInputStream(uri);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            if(in==null)throw new IOException();byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){if(out.size()+n>65536)throw new IOException();out.write(b,0,n);}
            String raw=new String(out.toByteArray(),StandardCharsets.UTF_8);if(raw.startsWith("\uFEFF"))raw=raw.substring(1);AtlasApp.validate(raw);return raw;
        }
    }
    private void bulk(ArrayList<Uri> uris){
        if(app.busy||!app.activeId.isEmpty())return;app.busy=true;
        app.worker.execute(()->{try{
            ProfileStore.Data latest=app.store.load();int added=0, skipped=0;
            for(Uri uri:uris){
                try{String raw=readConfig(uri);boolean duplicate=false;for(ProfileStore.Profile old:latest.profiles){if(old.config.equals(raw)){duplicate=true;break;}}
                    if(duplicate||latest.profiles.size()>=200){skipped++;continue;}
                    ServerHint hint=ServerHint.from(raw,displayName(uri));String name=hint.name.isEmpty()?"WireGuard "+(latest.profiles.size()+1):hint.name;
                    ProfileStore.Profile p=new ProfileStore.Profile(UUID.randomUUID().toString(),name,hint.country,raw);latest.profiles.add(p);if(latest.selected.isEmpty())latest.selected=p.id;added++;
                }catch(Exception|LinkageError e){skipped++;}
            }
            app.store.save(latest);final int a=added,s=skipped;runOnUiThread(()->{data=latest;updateProfile();toast(tr("Imported: ","تم استيراد: ")+a+tr(" · skipped: "," · تم تخطي: ")+s);});
        }catch(Exception e){runOnUiThread(()->toast(tr("Import failed","تعذر الاستيراد")));}
        finally{app.busy=false;runOnUiThread(()->{if(resumed)refresh();});}});
    }
    private void readFile(Uri uri){app.busy=true;app.worker.execute(()->{try(InputStream in=getContentResolver().openInputStream(uri);ByteArrayOutputStream out=new ByteArrayOutputStream()){if(in==null)throw new IOException();byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){if(out.size()+n>65536)throw new IOException();out.write(b,0,n);}String raw=new String(out.toByteArray(),StandardCharsets.UTF_8);if(raw.startsWith("\uFEFF"))raw=raw.substring(1);AtlasApp.validate(raw);final String config=raw;final String fileName=displayName(uri);runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())label(config,fileName);});}catch(Exception|LinkageError e){runOnUiThread(()->toast(tr("Invalid profile. Requires one peer, DNS, and full IPv4/IPv6 routes without app exclusions.","ملف غير مقبول: مطلوب DNS وسيرفر واحد ومسارات IPv4 وIPv6 كاملة دون استثناء تطبيقات.")));}finally{app.busy=false;}});}
    private String displayName(Uri uri){try(android.database.Cursor c=getContentResolver().query(uri,new String[]{android.provider.OpenableColumns.DISPLAY_NAME},null,null,null)){if(c!=null&&c.moveToFirst())return c.getString(0);}catch(Exception ignored){}return "";}
    private void label(String raw,String fileName){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(20),dp(12),dp(20),0);EditText name=new EditText(this);name.setSingleLine(true);name.setHint(tr("Server name","اسم السيرفر"));ServerHint hint=ServerHint.from(raw,fileName);name.setText(hint.name);box.addView(name);
        String[] codes=Locale.getISOCountries();Arrays.sort(codes,Comparator.comparing(this::country));String[] labels=new String[codes.length];for(int j=0;j<codes.length;j++)labels[j]=country(codes[j]);Spinner spin=new Spinner(this);ArrayAdapter<String> a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels);spin.setAdapter(a);for(int j=0;j<codes.length;j++)if(codes[j].equals(hint.country)){spin.setSelection(j);break;}box.addView(spin);box.addView(text(tr("Choose the actual server country. The app does not verify this label.","اختر الدولة الحقيقية للسيرفر؛ التطبيق لا يتحقق من الموقع."),12,MUTED,false));
        new AlertDialog.Builder(this).setTitle(tr("Add trusted server","إضافة سيرفر موثوق")).setView(box).setNegativeButton(android.R.string.cancel,null).setPositiveButton(tr("Save","حفظ"),(d,w)->{String label=name.getText().toString().trim();if(data==null||app.busy||!app.activeId.isEmpty())return;if(label.isEmpty())label=labels[spin.getSelectedItemPosition()];if(label.length()>60)label=label.substring(0,60);ProfileStore.Data next=copy();for(ProfileStore.Profile old:next.profiles){if(old.config.equals(raw)){next.selected=old.id;save(next);toast(tr("Already imported. Selected existing profile.","السيرفر موجود بالفعل وتم اختياره"));return;}}ProfileStore.Profile p=new ProfileStore.Profile(UUID.randomUUID().toString(),label,codes[spin.getSelectedItemPosition()],raw);next.profiles.add(p);next.selected=p.id;save(next);}).show();}
    private ProfileStore.Data copy(){ProfileStore.Data d=new ProfileStore.Data();d.profiles.addAll(data.profiles);d.selected=data.selected;return d;}
    private void save(ProfileStore.Data next){app.busy=true;app.worker.execute(()->{try{app.store.save(next);runOnUiThread(()->{data=next;updateProfile();});}catch(Exception e){runOnUiThread(()->toast(tr("Profile could not be saved","تعذر حفظ السيرفر")));}finally{app.busy=false;}});}
    private void pick(){if(data==null||app.busy||!app.activeId.isEmpty())return;if(data.profiles.isEmpty()){importFile();return;}String[] labels=new String[data.profiles.size()];for(int i=0;i<labels.length;i++){ProfileStore.Profile p=data.profiles.get(i);labels[i]=country(p.country)+" / "+p.name;}new AlertDialog.Builder(this).setTitle(tr("Available imported servers","السيرفرات المضافة")).setItems(labels,(d,w)->{ProfileStore.Data next=copy();next.selected=next.profiles.get(w).id;save(next);}).setNegativeButton(android.R.string.cancel,null).show();}
    private void remove(){if(data==null||app.busy||!app.activeId.isEmpty()||data.selected()==null)return;new AlertDialog.Builder(this).setTitle(tr("Delete selected profile?","حذف السيرفر المحدد؟")).setNegativeButton(android.R.string.cancel,null).setPositiveButton(android.R.string.ok,(d,w)->{ProfileStore.Data next=copy();next.profiles.removeIf(p->p.id.equals(next.selected));next.selected=next.profiles.isEmpty()?"":next.profiles.get(0).id;save(next);}).show();}
}
