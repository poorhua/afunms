package com.afunms.config.model;

import com.afunms.common.base.BaseVo;

public class SlaNodeProp extends BaseVo {
	private int id;
	private int telnetconfigid;
	private int entrynumber;// H3C:暂无；cisco:实体数字
	private String slatype;
	private String bak;
	private String createtime;
	private int operatorid;
	private String adminsign;// 备注1:( H3C:存放管理名；cisco:暂无）
	private String operatesign;// 备注2:( H3C:存放操作标识；cisco:暂无）

	public String getAdminsign() {
		return adminsign;
	}

	public String getBak() {
		return bak;
	}

	public String getCreatetime() {
		return createtime;
	}

	public int getEntrynumber() {
		return entrynumber;
	}

	public int getId() {
		return id;
	}

	public String getOperatesign() {
		return operatesign;
	}

	public int getOperatorid() {
		return operatorid;
	}

	public String getSlatype() {
		return slatype;
	}

	public int getTelnetconfigid() {
		return telnetconfigid;
	}

	public void setAdminsign(String adminsign) {
		this.adminsign = adminsign;
	}

	public void setBak(String bak) {
		this.bak = bak;
	}

	public void setCreatetime(String createtime) {
		this.createtime = createtime;
	}

	public void setEntrynumber(int entrynumber) {
		this.entrynumber = entrynumber;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setOperatesign(String operatesign) {
		this.operatesign = operatesign;
	}

	public void setOperatorid(int operatorid) {
		this.operatorid = operatorid;
	}

	public void setSlatype(String slatype) {
		this.slatype = slatype;
	}

	public void setTelnetconfigid(int telnetconfigid) {
		this.telnetconfigid = telnetconfigid;
	}

}
