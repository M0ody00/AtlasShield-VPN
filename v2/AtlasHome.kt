package com.protonvpn.android.redesign.home_screen.ui

import android.content.Intent
import android.provider.Settings
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.protonvpn.android.R
import kotlinx.coroutines.delay

private val ink = Color(0xff08110d)
private val panel = Color(0xff15231b)
private val mint = Color(0xff35e59a)
private val pale = Color(0xffadc5b6)

@Composable
internal fun AtlasHome(onConnect: () -> Unit, onDisconnect: () -> Unit, onDetails: () -> Unit, onAdvanced: () -> Unit) {
    val context=LocalContext.current
    val monitor=remember(context){ AtlasMonitor.install(context) }
    var state by remember { mutableStateOf(monitor.snapshot()) }
    var history by remember { mutableStateOf(monitor.history()) }
    var tab by remember { mutableIntStateOf(0) }
    LaunchedEffect(monitor) {
        while(true) { state=monitor.snapshot(); history=monitor.history(); delay(2000) }
    }
    val colors=ButtonDefaults.buttonColors(containerColor=mint,contentColor=ink)
    Column(Modifier.fillMaxSize().background(ink).statusBarsPadding().verticalScroll(rememberScrollState()).padding(18.dp),
        horizontalAlignment=Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
            Image(painterResource(R.drawable.atlas_globe),contentDescription="AtlasShield",modifier=Modifier.size(48.dp))
            Column(Modifier.weight(1f).padding(start=12.dp)){
                Text("ATLAS / SHIELD",color=Color.White,fontWeight=FontWeight.Bold,fontSize=23.sp)
                Text("GLOBAL PRIVACY NETWORK",color=mint,fontSize=11.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){
            listOf("Home","Privacy","Alerts").forEachIndexed { i,label ->
                TextButton(onClick={tab=i}){Text(label,color=if(tab==i)mint else pale)}
            }
        }
        Spacer(Modifier.height(12.dp))
        when(tab){
            0 -> {
                AtlasPanel {
                    Image(painterResource(R.drawable.atlas_globe),contentDescription=null,modifier=Modifier.size(156.dp).align(Alignment.CenterHorizontally))
                    Text(if(state.vpn) "VPN NETWORK ACTIVE" else "NOT PROTECTED",color=if(state.vpn)mint else Color(0xffffb47d),fontSize=21.sp,fontWeight=FontWeight.Bold)
                    Text(if(state.validated)"Internet validated by Android" else "Internet not validated",color=pale)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick={if(state.vpn){monitor.expectDisconnect();onDisconnect()}else onConnect()},modifier=Modifier.fillMaxWidth().height(54.dp),colors=colors){
                        Text(if(state.vpn)"Disconnect" else "Quick Connect",fontSize=17.sp,fontWeight=FontWeight.Bold)
                    }
                    OutlinedButton(onClick=onDetails,modifier=Modifier.fillMaxWidth()){Text("Connection / server details",color=mint)}
                    OutlinedButton(onClick=onAdvanced,modifier=Modifier.fillMaxWidth()){Text("Advanced connection controls",color=mint)}
                    Text("Free client • network access follows provider's free-plan limits.",color=pale,fontSize=12.sp)
                }
            }
            1 -> {
                AtlasPanel {
                    Text("Shield Check",color=mint,fontSize=20.sp,fontWeight=FontWeight.Bold)
                    AtlasLine("VPN network",if(state.vpn)"Detected" else "Not active")
                    AtlasLine("Internet reachability",if(state.validated)"Validated" else "Unverified")
                    AtlasLine("DNS configured",if(state.dns)"Yes • not a leak test" else "Not observed")
                    AtlasLine("IPv4 default route",if(state.v4)"Observed" else "Not observed")
                    AtlasLine("IPv6 default route",if(state.v6)"Observed" else "Not observed")
                    AtlasLine("Captive portal",if(state.captive)"Android reports portal" else "Not reported")
                    AtlasLine("Network type",state.transport)
                    Text("Route checks are indicators only. They cannot certify zero DNS/IPv6 leaks, detect all spyware, or prove a VPN belongs to AtlasShield.",color=pale,fontSize=12.sp)
                    Button(onClick={state=monitor.snapshot();history=monitor.history()},modifier=Modifier.fillMaxWidth(),colors=colors){Text("Refresh checks")}
                    OutlinedButton(onClick={runCatching {context.startActivity(Intent(Settings.ACTION_VPN_SETTINGS))}},modifier=Modifier.fillMaxWidth()){Text("Android Kill Switch settings",color=mint)}
                    Text("Enable Always-on VPN and Block connections without VPN in Android to prevent traffic escaping when disconnected.",color=pale,fontSize=12.sp)
                }
            }
            2 -> {
                AtlasPanel {
                    Text("AtlasGuard Alerts",color=mint,fontSize=20.sp,fontWeight=FontWeight.Bold)
                    Text("Local connection events • no traffic content recorded by this monitor.",color=pale,fontSize=13.sp)
                    Text(history,color=Color.White,fontSize=13.sp)
                    if(Build.VERSION.SDK_INT>=33){
                        OutlinedButton(onClick={(context as? ComponentActivity)?.requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"),911)},modifier=Modifier.fillMaxWidth()){Text("Allow security notifications",color=mint)}
                    }
                    Text("VPN interruptions are monitored while the application process is alive. An interruption does not prove tracking or espionage.",color=pale,fontSize=12.sp)
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("Founder & Owner • Ahmed Nasr Labib",color=mint,fontSize=12.sp,fontWeight=FontWeight.SemiBold)
        Text("Independent GPLv3 client • based on Proton VPN open source",color=pale,fontSize=11.sp)
        Text("AtlasShield 2.1 • ARM64 preview",color=pale,fontSize=11.sp)
    }
}
@Composable private fun AtlasPanel(content:@Composable ColumnScope.()->Unit){
    Column(Modifier.fillMaxWidth().background(panel,RoundedCornerShape(24.dp)).padding(18.dp),
        verticalArrangement=Arrangement.spacedBy(10.dp),content=content)
}
@Composable private fun AtlasLine(label:String,value:String){
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
        Text(label,color=pale,modifier=Modifier.weight(1f),fontSize=13.sp)
        Text(value,color=Color.White,fontSize=13.sp)
    }
}
