package com.afunms.polling.base;

import com.afunms.common.base.BaseVo;
import com.afunms.topology.dao.NodeMonitorDao;

public abstract class NodeLoader {

	public NodeLoader() {
	}

	/**
	 * 加载所有节点
	 */
	public abstract void loading();

	/**
	 * 加载一个节点
	 */
	public abstract void loadOne(BaseVo vo);

	public NodeMonitorDao getNmDao() {
		NodeMonitorDao nmDao = new NodeMonitorDao();
		return nmDao;
	}

}