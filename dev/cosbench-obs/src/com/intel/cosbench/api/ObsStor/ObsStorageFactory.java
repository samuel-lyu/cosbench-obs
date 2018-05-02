package com.intel.cosbench.api.ObsStor;

import com.intel.cosbench.api.storage.StorageAPI;
import com.intel.cosbench.api.storage.StorageAPIFactory;

public class ObsStorageFactory implements StorageAPIFactory{

	@Override
	public String getStorageName() {
		// TODO Auto-generated method stub
		return "obs";
	}

	@Override
	public StorageAPI getStorageAPI() {
		// TODO Auto-generated method stub
		return new ObsStorage();
	}

}
