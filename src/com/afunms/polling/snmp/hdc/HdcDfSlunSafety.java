package com.afunms.polling.snmp.hdc;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import com.afunms.alarm.model.AlarmIndicatorsNode;
import com.afunms.alarm.util.AlarmConstant;
import com.afunms.alarm.util.AlarmIndicatorsUtil;
import com.afunms.common.util.AlarmHelper;
import com.afunms.common.util.CheckEventUtil;
import com.afunms.common.util.ShareData;
import com.afunms.common.util.SnmpUtils;
import com.afunms.common.util.SysUtil;
import com.afunms.common.util.SystemConstant;
import com.afunms.config.model.EnvConfig;
import com.afunms.indicators.model.NodeGatherIndicators;
import com.afunms.polling.PollingEngine;
import com.afunms.polling.node.Host;
import com.afunms.polling.om.Interfacecollectdata;
import com.gatherdb.GathersqlListManager;

@SuppressWarnings("unchecked")
public class HdcDfSlunSafety {

	java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public Hashtable collect_Data(NodeGatherIndicators alarmIndicatorsNode) {
		Hashtable returnHash = new Hashtable();
		Vector fanVector = new Vector();
		Host node = (Host) PollingEngine.getInstance().getNodeByID(Integer.parseInt(alarmIndicatorsNode.getNodeid()));
		if (node == null)
			return returnHash;
		try {
			Interfacecollectdata interfacedata = new Interfacecollectdata();
			Calendar date = Calendar.getInstance();
			Hashtable ipAllData = (Hashtable) ShareData.getSharedata().get(node.getIpAddress());
			if (ipAllData == null)
				ipAllData = new Hashtable();
			try {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				com.afunms.polling.base.Node snmpnode = (com.afunms.polling.base.Node) PollingEngine.getInstance().getNodeByIP(node.getIpAddress());
				Date cc = date.getTime();
				String time = sdf.format(cc);
				snmpnode.setLastTime(time);
			} catch (Exception e) {
			}
			try {
				String[][] valueArray = null;
				String[] oids = new String[] { "1.3.6.1.4.1.116.5.11.4.1.1.6.1.1",// dkcRaidListIndexSerialNumber
						".1.3.6.1.4.1.116.5.11.1.2.5.4.1.4"// dfLUNWWNSecurity
				// wwn安全
				};
				valueArray = SnmpUtils.getTemperatureTableData(node.getIpAddress(), node.getCommunity(), oids, node.getSnmpversion(), node.getSecuritylevel(), node
						.getSecurityName(), node.getV3_ap(), node.getAuthpassphrase(), node.getV3_privacy(), node.getPrivacyPassphrase(), 3, 1000 * 30);
				if (valueArray != null) {
					for (int i = 0; i < valueArray.length; i++) {
						String _value = valueArray[i][1];
						if (_value != null) {
							if (_value.equalsIgnoreCase("00:00:00:00:00:00:00:00")) {
								_value = "0";
							} else {
								_value = "1";
							}
						}
						String index = valueArray[i][2];
						String num = valueArray[i][0];
						interfacedata = new Interfacecollectdata();
						interfacedata.setIpaddress(node.getIpAddress());
						interfacedata.setCollecttime(date);
						interfacedata.setCategory("rsafety");
						interfacedata.setEntity(index);
						interfacedata.setSubentity(num);
						interfacedata.setRestype("dynamic");
						interfacedata.setUnit("");
						interfacedata.setThevalue(_value);
						fanVector.addElement(interfacedata);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (!(ShareData.getSharedata().containsKey(node.getIpAddress()))) {
			Hashtable ipAllData = new Hashtable();
			if (ipAllData == null)
				ipAllData = new Hashtable();
			if (fanVector != null && fanVector.size() > 0)
				ipAllData.put("rsafety", fanVector);
			ShareData.getSharedata().put(node.getIpAddress(), ipAllData);
		} else {
			if (fanVector != null && fanVector.size() > 0)
				((Hashtable) ShareData.getSharedata().get(node.getIpAddress())).put("rsafety", fanVector);

		}
		returnHash.put("rsafety", fanVector);

		try {
			AlarmIndicatorsUtil alarmIndicatorsUtil = new AlarmIndicatorsUtil();
			List list = alarmIndicatorsUtil.getAlarmInicatorsThresholdForNode(String.valueOf(node.getId()), AlarmConstant.TYPE_STORAGE, "hds", "rsafety");

			AlarmHelper helper = new AlarmHelper();
			Hashtable<String, EnvConfig> envHashtable = helper.getAlarmConfig(node.getIpAddress(), "rsafety");
			for (int i = 0; i < list.size(); i++) {
				AlarmIndicatorsNode alarmIndicatorsnode = (AlarmIndicatorsNode) list.get(i);
				CheckEventUtil checkutil = new CheckEventUtil();
				if (fanVector.size() > 0) {
					for (int j = 0; j < fanVector.size(); j++) {
						Interfacecollectdata data = (Interfacecollectdata) fanVector.get(j);
						if (data != null) {
							EnvConfig config = envHashtable.get(data.getEntity());
							if (config != null && config.getEnabled() == 1) {
								alarmIndicatorsnode.setAlarm_level(config.getAlarmlevel());
								alarmIndicatorsnode.setAlarm_times(config.getAlarmtimes() + "");
								alarmIndicatorsnode.setLimenvalue0(config.getAlarmvalue() + "");
								checkutil.checkEvent(node, alarmIndicatorsnode, data.getThevalue(), data.getSubentity());
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		// 把采集结果生成sql
		this.CreateResultTosql(returnHash, node.getIpAddress());

		return returnHash;
	}

	public void CreateResultTosql(Hashtable ipdata, String ip) {

		if (ipdata.containsKey("rsafety")) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String allipstr = SysUtil.doip(ip);
			Vector fanVector = (Vector) ipdata.get("rsafety");
			if (fanVector != null && fanVector.size() > 0) {
				Interfacecollectdata fandata = (Interfacecollectdata) fanVector.elementAt(0);
				if (fandata.getRestype().equals("dynamic")) {

					Calendar tempCal = (Calendar) fandata.getCollecttime();
					Date cc = tempCal.getTime();
					String time = sdf.format(cc);
					String tablename = "rsafety" + allipstr;
					long count = 0;
					if (fandata.getCount() != null) {
						count = fandata.getCount();
					}
					StringBuffer sBuffer = new StringBuffer();
					sBuffer.append("insert into ");
					sBuffer.append(tablename);
					sBuffer.append("(ipaddress,restype,category,entity,subentity,unit,chname,bak,count,thevalue,collecttime) ");
					sBuffer.append("values('");
					sBuffer.append(ip);
					sBuffer.append("','");
					sBuffer.append(fandata.getRestype());
					sBuffer.append("','");
					sBuffer.append(fandata.getCategory());
					sBuffer.append("','");
					sBuffer.append(fandata.getEntity());
					sBuffer.append("','");
					sBuffer.append(fandata.getSubentity());
					sBuffer.append("','");
					sBuffer.append(fandata.getUnit());
					sBuffer.append("','");
					sBuffer.append(fandata.getChname());
					sBuffer.append("','");
					sBuffer.append(fandata.getBak());
					sBuffer.append("','");
					sBuffer.append(count);
					sBuffer.append("','");
					sBuffer.append(fandata.getThevalue());
					if ("mysql".equalsIgnoreCase(SystemConstant.DBType)) {
						sBuffer.append("','");
						sBuffer.append(time);
						sBuffer.append("')");
					} else if ("oracle".equalsIgnoreCase(SystemConstant.DBType)) {
						sBuffer.append("',");
						sBuffer.append("to_date('" + time + "','YYYY-MM-DD HH24:MI:SS')");
						sBuffer.append(")");
					}

					GathersqlListManager.Addsql(sBuffer.toString());
					sBuffer = null;
				}
				fandata = null;
			}
			fanVector = null;
		}

	}
}
