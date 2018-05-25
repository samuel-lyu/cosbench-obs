package com.intel.cosbench.api.ObsStor;

import static com.intel.cosbench.client.ObsStor.ObsConstants.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.http.HttpStatus;




import com.intel.cosbench.api.storage.*;
import com.intel.cosbench.api.context.*;
import com.intel.cosbench.config.Config;
import com.intel.cosbench.log.LogFactory;
import com.intel.cosbench.log.Logger;

import com.obs.services.HttpProxyConfiguration;
import com.obs.services.ObsClient;
import com.obs.services.ObsConfiguration;
import com.obs.services.exception.ObsException;
import com.obs.services.model.CompleteMultipartUploadRequest;
import com.obs.services.model.DeleteObjectsRequest;
import com.obs.services.model.GetObjectRequest;
import com.obs.services.model.InitiateMultipartUploadRequest;
import com.obs.services.model.InitiateMultipartUploadResult;
import com.obs.services.model.KeyAndVersion;
import com.obs.services.model.ObjectListing;
import com.obs.services.model.ObjectMetadata;
import com.obs.services.model.ObsObject;
import com.obs.services.model.PartEtag;
import com.obs.services.model.S3Object;
import com.obs.services.model.UploadPartResult;

public class ObsStorage extends NoneStorage{
	private static final Logger LOGGER = LogFactory.getSystemLogger();
	private int timeout;

	private String accessKey;
	private String secretKey;
	private String endpoint;
	private ObsConfiguration obsConf;
	private String linkWay;
	private String linkTime;
	private ObsClient client;
	
	private List<PartEtag> partEtags;
	private UploadPartResult uploadPartResult; 
	private CompleteMultipartUploadRequest request;
	private InitiateMultipartUploadResult imu;

	@Override
	public void init(Config config, Logger logger) {
		// TODO Auto-generated method stub
		super.init(config, logger);
		timeout = config.getInt(CONN_TIMEOUT_KEY, CONN_TIMEOUT_DEFAULT);

    	parms.put(CONN_TIMEOUT_KEY, timeout);
    	
    	endpoint = config.get(ENDPOINT_KEY, ENDPOINT_DEFAULT);
        accessKey = config.get(AUTH_USERNAME_KEY, AUTH_USERNAME_DEFAULT);
        secretKey = config.get(AUTH_PASSWORD_KEY, AUTH_PASSWORD_DEFAULT);
        
        linkWay = config.get(LINK_WAY_KEY, LINK_WAY_DEFAULT);
        linkTime = config.get(LINK_TIME_KEY, LINK_TIME_DEFAULT);
        System.out.println(linkWay);
        System.out.println(linkTime);

        boolean pathStyleAccess = config.getBoolean(PATH_STYLE_ACCESS_KEY, PATH_STYLE_ACCESS_DEFAULT);
        
		String proxyHost = config.get(PROXY_HOST_KEY, "");
		String proxyPort = config.get(PROXY_PORT_KEY, "");
        
        parms.put(ENDPOINT_KEY, endpoint);
    	parms.put(AUTH_USERNAME_KEY, accessKey);
    	parms.put(AUTH_PASSWORD_KEY, secretKey);
    	parms.put(PATH_STYLE_ACCESS_KEY, pathStyleAccess);
    	parms.put(PROXY_HOST_KEY, proxyHost);
    	parms.put(PROXY_PORT_KEY, proxyPort);

        logger.debug("using storage config: {}", parms);
        
        //config the basic information
        obsConf = new ObsConfiguration();
        HttpProxyConfiguration httpConf = new HttpProxyConfiguration();
        
        obsConf.setConnectionTimeout(timeout);
        obsConf.setSocketTimeout(timeout);
        obsConf.setEndPoint(endpoint);
        if (linkWay.equalsIgnoreCase("https")) {
			obsConf.setHttpsOnly(true);
		} else {
			obsConf.setHttpsOnly(false);
		}
		//        clientConf.withUseExpectContinue(false);
//        clientConf.withSignerOverride("S3SignerType");
//        clientConf.setProtocol(Protocol.HTTP);
		if((!proxyHost.equals(""))&&(!proxyPort.equals(""))){
			httpConf.setProxyAddr(proxyHost);
			httpConf.setProxyPort(Integer.parseInt(proxyPort));
		}
		
		if (linkTime.equalsIgnoreCase("short")) {
			return;
		}
		//create obs client
		client = new ObsClient(accessKey, secretKey, obsConf);
		LOGGER.debug("The client is initialized");
	}

	@Override
	public void setAuthContext(AuthContext info) {
		// TODO Auto-generated method stub
		super.setAuthContext(info);
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		if (client == null) {
			return;
		}
		super.dispose();
		try {
			client.close();
			LOGGER.debug("The client has been destroyed.");
		} catch (IOException e) {
			LOGGER.debug("Failed to destroy the client.");
			e.printStackTrace();
		}
	}

	@Override
    public InputStream getObject(String container, String object, Config config) {
		super.getObject(container, object, config);
        InputStream stream;
        if (linkTime.equalsIgnoreCase("short")) {
			ObsClient client = new ObsClient(accessKey, secretKey, obsConf);
			LOGGER.debug("The client is initialized by method self");
			try {
				stream = getObject(container, object, config, client);
			} finally {
				try {
					client.close();
					LOGGER.debug("The client created by method self has been destroyed.");
				} catch (IOException e) {
					LOGGER.debug("Failed to destroy the client created by method self.");
					e.printStackTrace();
				}
			}
		} else {
			stream = getObject(container, object, config, this.client);
		}
        return stream;
    }
	
	public InputStream getObject(String container, String object, Config config, ObsClient client) {
		InputStream stream;
		try {
			
			S3Object s3Obj = client.getObject(container, object);
			stream = s3Obj.getObjectContent();
			
		} catch (Exception e) {
			throw new StorageException(e);
		}
		return stream;
	}

	@Override
	public void createContainer(String container, Config config) {
		// TODO Auto-generated method stub
		super.createContainer(container, config);
		if (linkTime.equalsIgnoreCase("short")) {
			ObsClient client = new ObsClient(accessKey, secretKey, obsConf);
			LOGGER.debug("The client is initialized by method self");
			try {
				createContainer(container, config, client);
			} finally {
				try {
					client.close();
					LOGGER.debug("The client created by method self has been destroyed.");
				} catch (IOException e) {
					LOGGER.debug("Failed to destroy the client created by method self.");
					e.printStackTrace();
				}
			}
		} else {
			createContainer(container, config, this.client);
		}
	}
	
	public void createContainer(String container, Config config, ObsClient client) {
		try {
			if(!client.headBucket(container)) {
				
				client.createBucket(container);
			}
		} catch (Exception e) {
			throw new StorageException(e);
		}
	}

	@Override
	public void createObject(String container, String object, InputStream data,
            long length, Config config) {
		// TODO Auto-generated method stub
		super.createObject(container, object, data, length, config);
		if (linkTime.equalsIgnoreCase("short")) {
			ObsClient client = new ObsClient(accessKey, secretKey, obsConf);
			LOGGER.debug("The client is initialized by method self");
			try {
				createObject(container, object, data, length, config, client);
			} finally {
				try {
					client.close();
					LOGGER.debug("The client created by method self has been destroyed.");
				} catch (IOException e) {
					LOGGER.debug("Failed to destroy the client created by method self.");
					e.printStackTrace();
				}
			}
		} else {
			createObject(container, object, data, length, config, this.client);
		}
	}
	
	public void createObject(String container, String object, InputStream data,
			long length, Config config, ObsClient client) {
		// TODO Auto-generated method stub
		super.createObject(container, object, data, length, config);
		try {
			ObjectMetadata metadata = new ObjectMetadata();
			metadata.setContentLength(length);
			metadata.setContentType("application/octet-stream");
			
			client.putObject(container, object, data, metadata);
		} catch (Exception e) {
			throw new StorageException(e);
		}
	}

	@Override
	public void deleteContainer(String container, Config config) {
		// TODO Auto-generated method stub
		super.deleteContainer(container, config);
		if (linkTime.equalsIgnoreCase("short")) {
			ObsClient client = new ObsClient(accessKey, secretKey, obsConf);
			LOGGER.debug("The client is initialized by method self");
			try {
				deleteContainer(container, config, client);
			} finally {
				try {
					client.close();
					LOGGER.debug("The client created by method self has been destroyed.");
				} catch (IOException e) {
					LOGGER.debug("Failed to destroy the client created by method self.");
					e.printStackTrace();
				}
			}
		} else {
			deleteContainer(container, config, this.client);
		}
	}
	
	public void deleteContainer(String container, Config config, ObsClient client) {
		// TODO Auto-generated method stub
		super.deleteContainer(container, config);
		try {
			if(client.headBucket(container)) {
				client.deleteBucket(container);
			}
		} catch(ObsException awse) {
			if(Integer.parseInt(awse.getErrorCode()) != HttpStatus.SC_NOT_FOUND) {
				throw new StorageException(awse);
			}
		} catch (Exception e) {
			throw new StorageException(e);
		}
	}

	@Override
	public void deleteObject(String container, String object, Config config) {
		// TODO Auto-generated method stub
		super.deleteObject(container, object, config);
		if (linkTime.equalsIgnoreCase("short")) {
			ObsClient client = new ObsClient(accessKey, secretKey, obsConf);
			LOGGER.debug("The client is initialized by method self");
			try {
				deleteObject(container, object, config, client);
			} finally {
				try {
					client.close();
					LOGGER.debug("The client created by method self has been destroyed.");
				} catch (IOException e) {
					LOGGER.debug("Failed to destroy the client created by method self.");
					e.printStackTrace();
				}
			}
		} else {
			deleteObject(container, object, config, this.client);
		}
	}
	
	public void deleteObject(String container, String object, Config config, ObsClient client) {
		// TODO Auto-generated method stub
		super.deleteObject(container, object, config);
		try {
			client.deleteObject(container, object);
		} catch(ObsException awse) {
			if(Integer.parseInt(awse.getErrorCode()) != HttpStatus.SC_NOT_FOUND) {
				throw new StorageException(awse);
			}
		} catch (Exception e) {
			throw new StorageException(e);
		}
	}

	@Override
	public InputStream getObjectByRange(String container, String object, Config config, long startRange, long endRange) {
		// TODO Auto-generated method stub
		System.out.println(startRange+"-"+endRange);
		super.getObjectByRange(container, object, config, startRange, endRange);     

		InputStream objectData;
		if (linkTime.equalsIgnoreCase("short")) {
			ObsClient client = new ObsClient(accessKey, secretKey, obsConf);
			LOGGER.debug("The client is initialized by method self");
			try {
				objectData = getObjectByRange(container, object, config, startRange, endRange, client);
			} finally {
				try {
					client.close();
					LOGGER.debug("The client created by method self has been destroyed.");
				} catch (IOException e) {
					LOGGER.debug("Failed to destroy the client created by method self.");
					e.printStackTrace();
				}
			}
		} else {
			objectData = getObjectByRange(container, object, config, startRange, endRange, this.client);
		}
		return objectData;
	}
	
	public InputStream getObjectByRange(String container, String object, Config config, long startRange, long endRange, ObsClient client) {
		// TODO Auto-generated method stub
		System.out.println(startRange+"-"+endRange);
		super.getObjectByRange(container, object, config, startRange, endRange);     
		
		GetObjectRequest rangeObjectRequest = new GetObjectRequest(container, object);
		rangeObjectRequest.	setRangeStart(startRange);
		rangeObjectRequest.setRangeEnd(endRange);
		S3Object objectPortion = client.getObject(rangeObjectRequest);
		
		InputStream objectData = objectPortion.getObjectContent();
		// Process the objectData stream.
		return objectData;
	}

	@Override
	public long multiPartUpload(String container, String object, long sizePart, InputStream in, boolean isFinish, int partNum) {
		super.multiPartUpload(container, object, sizePart, in ,isFinish, partNum);
		long  resTime;
		if (linkTime.equalsIgnoreCase("short")) {
			ObsClient client = new ObsClient(accessKey, secretKey, obsConf);
			LOGGER.debug("The client is initialized by method self");
			try {
				resTime = multiPartUpload(container, object, sizePart, in ,isFinish, partNum, client);
			} finally {
				try {
					client.close();
					LOGGER.debug("The client created by method self has been destroyed.");
				} catch (IOException e) {
					LOGGER.debug("Failed to destroy the client created by method self.");
					e.printStackTrace();
				}
			}
		} else {
			resTime = multiPartUpload(container, object, sizePart, in ,isFinish, partNum, this.client);
		}
		return resTime;
	}
	
	public long multiPartUpload(String container, String object, long sizePart, InputStream in, boolean isFinish, int partNum, ObsClient client) {
		super.multiPartUpload(container, object, sizePart, in ,isFinish, partNum);
		long end;
		if (partNum == 1) {
			// do init
			partEtags = new ArrayList<PartEtag>();
			request = new CompleteMultipartUploadRequest();
			imu = null;
			InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest();
			
			initRequest.setBucketName(container);
			initRequest.setObjectKey(object);
			imu = client.initiateMultipartUpload(initRequest);
			LOGGER.debug("bucketName:" + imu.getBucketName() + "\tObjectKey:" + imu.getObjectKey() + "\tUploadId:"
					+ imu.getUploadId());
			// do upload
		}
		PartEtag partEtag = new PartEtag();
		System.out.println("第:" + partNum + "次uploadId是 " + imu.getUploadId());
		long start = System.nanoTime();
		try 
		{
			uploadPartResult = client.uploadPart(imu.getBucketName(), imu.getObjectKey(), imu.getUploadId(), partNum,
					in);
			end = System.nanoTime();
			System.out.println("完成时间" + (end - start) / 1000000);
			LOGGER.debug(partNum + " part is : " + uploadPartResult.getEtag());
			partEtag.seteTag(uploadPartResult.getEtag());
			partEtag.setPartNumber(uploadPartResult.getPartNumber());
			partEtags.add(partEtag);
			
			// complete multiPartUpload
			if (isFinish) 
			{
				request.setBucketName(imu.getBucketName());
				request.setObjectKey(imu.getObjectKey());
				request.setPartEtag(partEtags);
				request.setUploadId(imu.getUploadId());
				client.completeMultipartUpload(request);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(client.headBucket(imu.getBucketName()));
			return -1;
		} 
		return (end - start) / 1000000;
	}

	@Override
	public void deleteObjects(String container, Config config, int amount) {
		super.deleteObjects(container, config, amount);
		if (linkTime.equalsIgnoreCase("short")) {
			ObsClient client = new ObsClient(accessKey, secretKey, obsConf);
			LOGGER.debug("The client is initialized by method self");
			try {
				deleteObjects(container, config, amount, client);
			} finally {
				try {
					client.close();
					LOGGER.debug("The client created by method self has been destroyed.");
				} catch (IOException e) {
					LOGGER.debug("Failed to destroy the client created by method self.");
					e.printStackTrace();
				}
			}
		} else {
			deleteObjects(container, config, amount, this.client);
		}
	}
	
	public void deleteObjects(String container, Config config, int amount, ObsClient client) {
		super.deleteObjects(container, config, amount);
		int flag = 0;
		ObsObject obsObject = null;
		ObjectListing result = null;
		DeleteObjectsRequest requst = new DeleteObjectsRequest();
		KeyAndVersion[] keyAndVersions = new KeyAndVersion[amount];
		
		requst.setBucketName(container);
		try {
			result = client.listObjects(container);
		}catch(Exception e) {
			e.printStackTrace();
			System.out.println(client.headBucket(container));
		}
		Iterator<ObsObject> itr = result.getObjects().iterator();
		//get the sets of objects
		while(itr.hasNext() && flag < amount) {
			obsObject = (ObsObject)itr.next();
			LOGGER.debug( "Deleted object name:"+obsObject.getObjectKey());
			keyAndVersions[flag] = new KeyAndVersion(obsObject.getObjectKey());
			flag++;
		}
		requst.setKeyAndVersions(keyAndVersions);
		try {
			client.deleteObjects(requst);
		}catch(ObsException e){
			e.printStackTrace();
			System.out. println("Error message: " + e.getErrorMessage()+ ". ResponseCode: " + e.getResponseCode());
		}
	}

}
