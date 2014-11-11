package org.apache.hadoop.hdfs.server.namenode;

import java.io.IOException;

import org.apache.hadoop.hdfs.protocol.HdfsConstants;
import org.apache.hadoop.hdfs.server.blockmanagement.BlockStoragePolicySuite;
import org.apache.hadoop.hdfs.server.namenode.snapshot.Snapshot;
import org.apache.hadoop.hdfs.util.ReadOnlyList;

public class StorageEngine {
	public static final int HOT_THRESHOLD = 10000;
	
	private FSNamesystem namesystem;
	
	StorageEngine(FSNamesystem namesystem) {
		this.namesystem = namesystem;
		Thread monitor = new Thread(new INodeStateMonitor());
		monitor.start();
	}
	
	void visitCountSetNotify(String src, int clickCount) throws IOException {
		if (clickCount > HOT_THRESHOLD) {
	  		  namesystem.setStoragePolicy(src, HdfsConstants.ALLSSD_STORAGE_POLICY_NAME);
	  	  }
	}
	
	
	class INodeStateMonitor implements Runnable {

		@Override
		public void run() {
			INode rootDir = namesystem.dir.rootDir;
			try {
				traversalInodes(rootDir);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			
			try {
				Thread.sleep(60/* * 1000 * 5*/);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		private void traversalInodes(INode iNode) throws IOException {
		    final boolean isDir = iNode.isDirectory();
		    if (isDir) {
		    	final INodeDirectory dir = iNode.asDirectory();  
		    	
				ReadOnlyList<INode> children = dir.getChildrenList(Snapshot.CURRENT_STATE_ID);
				for (INode child : children) {
					traversalInodes(child);
				}
			} else {
				correctState(iNode);
			}
		}

		private void correctState(INode iNode) throws IOException {
			int clickCount = namesystem.getClickCount(iNode);
			byte storagePolicyId = iNode.getStoragePolicyID();
			if (clickCount > HOT_THRESHOLD && 
					(storagePolicyId == HdfsConstants.COLD_STORAGE_POLICY_ID || storagePolicyId == BlockStoragePolicySuite.ID_UNSPECIFIED)) {
				namesystem.setStoragePolicy(iNode.getFullPathName(), HdfsConstants.HOT_STORAGE_POLICY_NAME);
			}
		}
	}
}
