package com.excellence.basetoolslibrary.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.excellence.basetoolslibrary.utils.ConvertUtils.bytes2HexString;
import static com.excellence.basetoolslibrary.utils.EmptyUtils.isEmpty;

/**
 * <pre>
 *     author : VeiZhang
 *     blog   : https://veizhang.github.io/
 *     time   : 2017/1/24
 *     desc   : 网络相关工具类
 *     			权限：{@link android.Manifest.permission#INTERNET              }
 *                     {@link android.Manifest.permission#ACCESS_NETWORK_STATE }
 *                     {@link android.Manifest.permission#ACCESS_WIFI_STATE    }
 *                     {@link android.Manifest.permission#CHANGE_WIFI_STATE    }
 *
 *     			isAvailable、isConnected：1，显示连接已保存，但标题栏没有，即没有实质连接上   not connect， available
 *										 2，显示连接已保存，标题栏也有已连接上的图标，           connect， available
 * 										 3，选择不保存后 								     not connect， available
 *										 4，选择连接，在正在获取IP地址时					 not connect， not available
 * </pre>
 */

public class NetworkUtils {

    public static final String DEFAULT_WIRELESS_MAC = "02:00:00:00:00:00";

    public enum NetworkType {
        NETWORK_ETH,
        NETWORK_WIFI,
        NETWORK_4G,
        NETWORK_3G,
        NETWORK_2G,
        NETWORK_UNKNOWN,
        NETWORK_NO
    }

    /**
     * 获取活动的网络信息
     *
     * @param context 上下文
     * @return NetworkInfo
     */
    private static NetworkInfo getActiveNetworkInfo(Context context) {
        return ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
    }

    /**
     * 检查网络是否连接
     *
     * @param context 上下文
     * @return {@code true}：是<br>{@code false}：否
     */
    public static boolean isConnected(Context context) {
        NetworkInfo info = getActiveNetworkInfo(context);
        return info != null && info.isConnected();
    }

    /**
     * 判断网络是否可用
     * {@code ping -c 1 -w 1}：{@code -c 1}：执行次数 1次；{@code -w 1}：等待每个响应的最长时间 1s
     * 223.5.5.5：阿里云
     * 206.190.36.45：yahoo.com
     *
     * @return {@code true}：可用<br>{@code false}：不可用
     */
    public static boolean isAvailableByPing() {
        return isAvailableByPing("223.5.5.5");
    }

    /**
     * 使用ping命令，判断网络是否可用
     *
     * @param address
     * @return
     */
    public static boolean isAvailableByPing(String address) {
        String cmd = String.format("ping -c 1 -w 1 %s", address);
        ShellUtils.CommandResult result = ShellUtils.execRuntimeCommand(cmd);
        return result.resultCode == 0;
    }

    /**
     * 判断移动数据是否打开
     *
     * @param context 上下文
     * @return {@code true}：打开<br>{@code false}：关闭
     */
    public static boolean isMobileDataEnabled(Context context) {
        try {
            TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            Method getMobileDataEnabledMethod = manager.getClass().getDeclaredMethod("getDataEnabled");
            if (getMobileDataEnabledMethod != null) {
                return (boolean) getMobileDataEnabledMethod.invoke(manager);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * @deprecated 反射不能打开或关闭移动数据，故舍弃
     * 打开或关闭移动数据
     *
     * @param context 上下文
     * @param enabled {@code true}：打开<br>{@code false}：关闭
     */
    public static void setMobileDataEnabled(Context context, boolean enabled) {
        try {
            TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            Method setMobileDataEnabledMethod = manager.getClass().getDeclaredMethod("setDataEnabled", boolean.class);
            if (setMobileDataEnabledMethod != null) {
                setMobileDataEnabledMethod.invoke(manager, enabled);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 判断是否是4G网络
     *
     * @param context 上下文
     * @return {@code true}：是<br>{@code false}：否
     */
    public static boolean is4G(Context context) {
        NetworkInfo info = getActiveNetworkInfo(context);
        return info != null && info.isAvailable() && info.getSubtype() == TelephonyManager.NETWORK_TYPE_LTE;
    }

    /**
     * 判断是否打开WiFi
     *
     * @param context 上下文
     * @return {@code true}：是<br>{@code false}：否
     */
    public static boolean isWiFiEnabled(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        return wifiManager.isWifiEnabled();
    }

    /**
     * 打开或关闭WiFi
     *
     * @param context 上下文
     * @param enabled {@code true}：打开<br>{@code false}：关闭
     */
    public static void setWiFiEnabled(Context context, boolean enabled) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (enabled) {
            if (!wifiManager.isWifiEnabled()) {
                wifiManager.setWifiEnabled(true);
            }
        } else {
            if (wifiManager.isWifiEnabled()) {
                wifiManager.setWifiEnabled(false);
            }
        }
    }

    /**
     * 判断以太网是否连接
     *
     * @param context
     * @return
     */
    public static boolean isEthConnected(Context context) {
        NetworkInfo info = getActiveNetworkInfo(context);
        return info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_ETHERNET;
    }

    /**
     * 判断以太网是否可用
     *
     * @param context
     * @return
     */
    public static boolean isEthAvailable(Context context) {
        NetworkInfo info = getActiveNetworkInfo(context);
        return info != null && info.isAvailable() && info.getType() == ConnectivityManager.TYPE_ETHERNET;
    }

    /**
     * 判断WiFi是否连接
     *
     * @param context 上下文
     * @return {@code true}：是<br>{@code false}：否
     */
    public static boolean isWiFiConnected(Context context) {
        NetworkInfo info = getActiveNetworkInfo(context);
        return info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_WIFI;
    }

    /**
     * 判断WiFi是否可用
     *
     * @param context 上下文
     * @return {@code true}：是<br>{@code false}：否
     */
    public static boolean isWiFiAvailable(Context context) {
        NetworkInfo info = getActiveNetworkInfo(context);
        return info != null && info.isAvailable() && info.getType() == ConnectivityManager.TYPE_WIFI;
    }

    /**
     * 通过ping的方式判断WiFi是否可用
     *
     * @param context 上下文
     * @return {@code true}：是<br>{@code false}：否
     */
    public static boolean isWiFiAvailableByPing(Context context) {
        return isWiFiEnabled(context) && isAvailableByPing();
    }

    /**
     * 获取网络运营商名称
     * <p>中国移动、中国联通、中国电信</p>
     *
     * @param context 上下文
     * @return 网络运营商名称
     */
    public static String getNetworkOperatorName(Context context) {
        TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return manager == null ? null : manager.getNetworkOperatorName();
    }

    private static final int NETWORK_TYPE_GSM = 16;
    private static final int NETWORK_TYPE_TD_SCDMA = 17;
    private static final int NETWORK_TYPE_IWLAN = 18;

    /**
     * 获取当前网络类型
     *
     * @param context 上下文
     * @return NetworkType网络类型
     *         <ul>
     *           <li>{@link NetworkType#NETWORK_ETH    } </li>
     *           <li>{@link NetworkType#NETWORK_WIFI   } </li>
     *           <li>{@link NetworkType#NETWORK_4G     } </li>
     *           <li>{@link NetworkType#NETWORK_3G     } </li>
     *           <li>{@link NetworkType#NETWORK_2G     } </li>
     *           <li>{@link NetworkType#NETWORK_UNKNOWN} </li>
     *           <li>{@link NetworkType#NETWORK_NO     } </li>
     *         </ul>
     */
    public static NetworkType getNetworkType(Context context) {
        NetworkType networkType = NetworkType.NETWORK_NO;
        NetworkInfo info = getActiveNetworkInfo(context);
        if (info != null && info.isAvailable()) {
            switch (info.getType()) {
                case ConnectivityManager.TYPE_ETHERNET:
                    networkType = NetworkType.NETWORK_ETH;
                    break;

                case ConnectivityManager.TYPE_WIFI:
                    networkType = NetworkType.NETWORK_WIFI;
                    break;
                case ConnectivityManager.TYPE_MOBILE:
                    switch (info.getSubtype()) {
                        case NETWORK_TYPE_GSM:
                        case TelephonyManager.NETWORK_TYPE_GPRS:
                        case TelephonyManager.NETWORK_TYPE_CDMA:
                        case TelephonyManager.NETWORK_TYPE_EDGE:
                        case TelephonyManager.NETWORK_TYPE_1xRTT:
                        case TelephonyManager.NETWORK_TYPE_IDEN:
                            networkType = NetworkType.NETWORK_2G;
                            break;

                        case NETWORK_TYPE_TD_SCDMA:
                        case TelephonyManager.NETWORK_TYPE_EVDO_A:
                        case TelephonyManager.NETWORK_TYPE_UMTS:
                        case TelephonyManager.NETWORK_TYPE_EVDO_0:
                        case TelephonyManager.NETWORK_TYPE_HSDPA:
                        case TelephonyManager.NETWORK_TYPE_HSUPA:
                        case TelephonyManager.NETWORK_TYPE_HSPA:
                        case TelephonyManager.NETWORK_TYPE_EVDO_B:
                        case TelephonyManager.NETWORK_TYPE_EHRPD:
                        case TelephonyManager.NETWORK_TYPE_HSPAP:
                            networkType = NetworkType.NETWORK_3G;
                            break;

                        case NETWORK_TYPE_IWLAN:
                        case TelephonyManager.NETWORK_TYPE_LTE:
                            networkType = NetworkType.NETWORK_4G;
                            break;

                        default:
                            String subTypeName = info.getSubtypeName();
                            if (subTypeName.equalsIgnoreCase("TD-SCDMA") || subTypeName.equalsIgnoreCase("WCDMA") || subTypeName.equalsIgnoreCase("CDMA2000")) {
                                networkType = NetworkType.NETWORK_3G;
                            } else {
                                networkType = NetworkType.NETWORK_UNKNOWN;
                            }
                            break;
                    }
                    break;

                default:
                    networkType = NetworkType.NETWORK_UNKNOWN;
                    break;
            }
        }
        return networkType;
    }

    /**
     * 获取内网网络IP地址
     *
     * @return IP地址
     */
    public static String getIPAddress(boolean useIPv4) {
        try {
            for (Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces(); enumeration.hasMoreElements(); ) {
                NetworkInterface networkInterface = enumeration.nextElement();
                // 防止小米手机返回10.0.2.15
                if (!networkInterface.isUp()) {
                    continue;
                }
                for (Enumeration<InetAddress> addresses = networkInterface.getInetAddresses(); addresses.hasMoreElements(); ) {
                    InetAddress inetAddress = addresses.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        String hostAddress = inetAddress.getHostAddress();
                        boolean isIPv4 = hostAddress.indexOf(':') < 0;
                        if (useIPv4) {
                            if (isIPv4) {
                                return hostAddress;
                            }
                        } else {
                            if (!isIPv4) {
                                int index = hostAddress.indexOf('%');
                                return index < 0 ? hostAddress.toUpperCase() : hostAddress.substring(0, index).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 根据域名获取ip
     *
     * @param domain 域名
     * @return IP地址
     */
    public static String getDomainAddress(final String domain) {
        try {
            ExecutorService executorService = Executors.newCachedThreadPool();
            Future<String> future = executorService.submit(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    InetAddress inetAddress;
                    inetAddress = InetAddress.getByName(domain);
                    return inetAddress.getHostAddress();
                }
            });
            return future.get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 读取Mac地址：优先获取Eth的MAC，当Eth为空，接着获取WiFi的MAC
     *
     * @param context
     * @return
     */
    public static String readMac(Context context) {
        String mac = getWiredMac(context);
        if (isEmpty(mac)) {
            mac = getWiredMac("eth0");
        }
        if (isEmpty(mac)) {
            mac = getWirelessMac(context);
        }
        return mac;
    }

    /**
     * 获取Mac地址：使用Eth时读取Eth的MAC，否则读取WiFi的MAC
     *
     * @param context
     * @return
     */
    public static String getMac(Context context) {
        if (isEthConnected(context)) {
            return getWiredMac(context);
        } else {
            return getWirelessMac(context);
        }
    }

    /**
     * 获取有线Mac地址
     *
     * @param context
     * @return
     */
    public static String getWiredMac(Context context) {
        String macAddress = "";
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
                if (activeNetwork != null) {
                    if (activeNetwork.getType() == ConnectivityManager.TYPE_ETHERNET) {
                        macAddress = activeNetwork.getExtraInfo();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return macAddress;
    }

    /**
     * 读取eth里面的Mac地址：如eth0、eth1
     *
     * @param ethName eth0
     * @return
     */
    public static String getWiredMac(String ethName) {
        String macAddress = "";
        if (isEmpty(ethName)) {
            ethName = "eth0";
        }
        NetworkInterface nicInterface;
        try {
            nicInterface = NetworkInterface.getByName(ethName);
            if (nicInterface != null) {
                byte[] buf = nicInterface.getHardwareAddress();
                StringBuilder sbBuffer = new StringBuilder();
                if (buf != null && buf.length > 1) {
                    sbBuffer.append(bytes2HexString(buf[0])).append(":")
                            .append(bytes2HexString(buf[1])).append(":")
                            .append(bytes2HexString(buf[2])).append(":")
                            .append(bytes2HexString(buf[3])).append(":")
                            .append(bytes2HexString(buf[4])).append(":")
                            .append(bytes2HexString(buf[5]));
                    macAddress = sbBuffer.toString();
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return macAddress;
    }

    /**
     * 获取无线Mac地址，有可能获取{@link #DEFAULT_WIRELESS_MAC}，再进一步读取WiFi接口wlan0的MAC
     *
     * @param context
     * @return
     */
    public static String getWirelessMac(Context context) {
        String macAddress = "";
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = (null == wifiManager ? null : wifiManager.getConnectionInfo());
            if (wifiInfo != null) {
                macAddress = wifiInfo.getMacAddress();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (DEFAULT_WIRELESS_MAC.equalsIgnoreCase(macAddress)) {
            macAddress = getWiredMac("wlan0");
        }
        return macAddress;
    }
}
