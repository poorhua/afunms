package com.afunms.realtime;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.afunms.indicators.dao.NodeGatherIndicatorsDao;
import com.afunms.indicators.model.NodeDTO;
import com.afunms.indicators.model.NodeGatherIndicators;
import com.afunms.indicators.util.NodeUtil;
import com.afunms.monitor.executor.base.SnmpMonitor;
import com.afunms.polling.PollingEngine;
import com.afunms.polling.node.Host;
import com.afunms.polling.om.InPkts;
import com.afunms.polling.om.OutPkts;
import com.afunms.polling.snmp.interfaces.PackageSnmp;

public class DataPacketControler extends SnmpMonitor {
	private Logger logger = Logger.getLogger(PortControler.class);
	private DoubleDataQueue doubleDataQueue = null;

	public static void main(String args[]) {
	}

	/**
	 * 生成数据
	 * 
	 * @param fileName
	 *            文件名
	 * @param blackFlag
	 *            是否是要生成初始化数据
	 * @param nodeID
	 * @param ifindex
	 * @param rq
	 * @param cx
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public String generateData(String fileName, boolean blackFlag, int nodeID, int ifindex, HttpServletRequest rq, ServletContext cx) {
		String webAppPath = cx.getRealPath("/");
		String path = webAppPath + "amcharts_data/" + fileName;
		HttpSession session = rq.getSession();
		this.doubleDataQueue = (DoubleDataQueue) session.getAttribute("portqueue");
		if (null == this.doubleDataQueue) {
			this.doubleDataQueue = new DoubleDataQueue();
		}
		if (true == blackFlag) {// 生成空文件 前n-1个数据为null 最后一个数据为0.0
			this.doubleDataQueue.initWithLastData(0.0);// 最后一个数字为0
			this.doubleDataQueue.setDataList(false);// 不是真实数据
			session.setAttribute("portqueue", this.doubleDataQueue);
		} else {
			DoubleDataModel doubleDM = null;
			doubleDM = getPortData(nodeID, ifindex);// 采集数据
			if (null == doubleDM) {// 采集数据失败
				return "failed:采集数据失败";
			} else {
				if (false == this.doubleDataQueue.isDataList()) {
					this.doubleDataQueue.getList().removeLast();// 将初始化的0值去掉
					this.doubleDataQueue.setDataList(true);
				}
				this.doubleDataQueue.enqueue(doubleDM);
				session.setAttribute("portqueue", this.doubleDataQueue);
			}
		}
		int size = this.doubleDataQueue.getLENGTH();
		SimpleDateFormat smft = new SimpleDateFormat("ss");// 设置日期格式
		String date;
		String data;
		StringBuffer dataXML = new StringBuffer("");
		dataXML.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		dataXML.append("<chart><series>");
		for (int i = 0; i < size; i++) {
			date = smft.format(this.doubleDataQueue.getList().get(i).getDate());
			dataXML.append("<value xid=\"").append(i).append("\">").append(date).append("</value>");
		}

		dataXML.append("</series><graphs><graph gid=\"1\">");
		for (int i = 0; i < size; i++) {
			data = this.doubleDataQueue.getList().get(i).getFirstData() + "";
			dataXML.append("<value xid=\"").append(i).append("\">").append(data).append("</value>");
		}
		dataXML.append("</graph><graph gid=\"2\">");
		for (int i = 0; i < size; i++) {
			data = this.doubleDataQueue.getList().get(i).getSecondData() + "";
			dataXML.append("<value xid=\"").append(i).append("\">").append(data).append("</value>");
		}
		dataXML.append("</graph></graphs></chart>");
		write(path, dataXML.toString());
		return "success";
	}

	/**
	 * 写文件
	 * 
	 * @param path
	 * @param content
	 *            void
	 */
	public void write(String path, String content) {
		try {
			File f = new File(path);
			if (f.exists()) {
				f.delete();
				f.createNewFile();
			} else {
				if (f.createNewFile()) {
				} else {
					logger.error("文件创建失败！");
				}
			}
			BufferedWriter output = new BufferedWriter(new FileWriter(f));
			output.write(content);
			output.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 采集数据
	 * 
	 * @param nodeID
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public DoubleDataModel getPortData(int nodeID, int ifindex) {
		DoubleDataModel doubleDataModel = new DoubleDataModel();
		Host hostNode = (Host) PollingEngine.getInstance().getNodeByID(nodeID);
		if (null == hostNode) {
			return null;
		} else if (!hostNode.isManaged()) {
			return null;
		}
		List<NodeGatherIndicators> gatherlist = new ArrayList<NodeGatherIndicators>();
		NodeDTO nodeDTO = null; 
		NodeUtil nodeutil = new NodeUtil(); 
		nodeDTO = nodeutil.creatNodeDTOByNode(hostNode);
		NodeGatherIndicatorsDao indicatorsdao = new NodeGatherIndicatorsDao(); 
		try {
			gatherlist = indicatorsdao.getByNodeidAndType(hostNode.getId() + "", 1, nodeDTO.getType());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			indicatorsdao.close();
		}
		if (gatherlist != null && gatherlist.size() > 0) {
			for (int i = 0; i < gatherlist.size(); i++) {
				NodeGatherIndicators nodeGatherIndicators = (NodeGatherIndicators) gatherlist.get(i);
				if ("interface".equalsIgnoreCase(nodeGatherIndicators.getName())) {
					// 进行接口信息的采集
					PackageSnmp packageSnmp = null;
					try {
						packageSnmp = (PackageSnmp) Class.forName("com.afunms.polling.snmp.interfaces.PackageSnmp").newInstance();
						Hashtable returnHash = packageSnmp.collect_Data(nodeGatherIndicators);
						Vector inpacksVector = (Vector) returnHash.get("inpacks");
						if (inpacksVector != null && inpacksVector.size() > 0) {
							for (int ii = 0; ii < inpacksVector.size(); ii++) {
								InPkts inpkts = (InPkts) inpacksVector.get(ii);
								if (ifindex == Integer.valueOf(inpkts.getSubentity())) {
									if ("ifInBroadcastPkts".equalsIgnoreCase(inpkts.getEntity())) {
										doubleDataModel.setFirstData(Double.valueOf(inpkts.getThevalue()));
										doubleDataModel.setDate(inpkts.getCollecttime().getTime());
									}
								}
							}
						} else {
							Date date = Calendar.getInstance().getTime();
							doubleDataModel.setFirstData(0.0);
							doubleDataModel.setDate(date);
						}

						Vector outpacksVector = (Vector) returnHash.get("outpacks");
						if (outpacksVector != null && outpacksVector.size() > 0) {
							for (int ii = 0; ii < inpacksVector.size(); ii++) {
								OutPkts outpkts = (OutPkts) outpacksVector.get(ii);
								if (ifindex == Integer.valueOf(outpkts.getSubentity())) {
									if ("ifOutBroadcastPkts".equalsIgnoreCase(outpkts.getEntity())) {
										doubleDataModel.setSecondData(Double.valueOf(outpkts.getThevalue()));
										doubleDataModel.setDate(outpkts.getCollecttime().getTime());
									}
								}
							}
						} else {
							Date date = Calendar.getInstance().getTime();
							doubleDataModel.setSecondData(0.0);
							doubleDataModel.setDate(date);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		return doubleDataModel;
	}
}
