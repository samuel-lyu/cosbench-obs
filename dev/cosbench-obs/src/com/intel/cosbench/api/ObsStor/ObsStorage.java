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
import com.intel.cosbench.log.Logger;

import com.obs.services.HttpProxyConfiguration;
import com.obs.services.ObsClient;
import com.obs.services.ObsConfiguration;
import com.obs.services.exception.ObsException;
import com.obs.services.model.CompleteMultipartUploadRequest;
import com.obs.services.model.CompleteMultipartUploadResult;
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
	private int timeout;

	private String accessKey;
	private String secretKey;
	private String endpoint;

	private ObsClient client;

	@Override
	public void init(Config config, Logger logger) {
		// TODO Auto-generated method stub
		super.init(config, logger);
		timeout = config.getInt(CONN_TIMEOUT_KEY, CONN_TIMEOUT_DEFAULT);

    	parms.put(CONN_TIMEOUT_KEY, timeout);
    	
    	endpoint = config.get(ENDPOINT_KEY, ENDPOINT_DEFAULT);
        accessKey = config.get(AUTH_USERNAME_KEY, AUTH_USERNAME_DEFAULT);
        secretKey = config.get(AUTH_PASSWORD_KEY, AUTH_PASSWORD_DEFAULT);

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
        ObsConfiguration obsConf = new ObsConfiguration();
        HttpProxyConfiguration httpConf = new HttpProxyConfiguration();
        
        obsConf.setConnectionTimeout(timeout);
        obsConf.setSocketTimeout(timeout);
        obsConf.setEndPoint(endpoint);
        
//        clientConf.withUseExpectContinue(false);
//        clientConf.withSignerOverride("S3SignerType");
//        clientConf.setProtocol(Protocol.HTTP);
		if((!proxyHost.equals(""))&&(!proxyPort.equals(""))){
			httpConf.setProxyAddr(proxyHost);
			httpConf.setProxyPort(Integer.parseInt(proxyPort));
		}
		
		//create obs client
		client = new ObsClient(accessKey, secretKey, obsConf);
	}

	@Override
	public void setAuthContext(AuthContext info) {
		// TODO Auto-generated method stub
		super.setAuthContext(info);
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		super.dispose();
		try {
			client.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
    public InputStream getObject(String container, String object, Config config) {
        super.getObject(container, object, config);
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

		GetObjectRequest rangeObjectRequest = new GetObjectRequest(container, object);
		rangeObjectRequest.	setRangeStart(startRange);
		rangeObjectRequest.setRangeEnd(endRange);
		S3Object objectPortion = client.getObject(rangeObjectRequest);

		InputStream objectData = objectPortion.getObjectContent();
		// Process the objectData stream.
		return objectData;
	}

	@Override
	public void multiPartUpload(String container, String object, long sizePart, InputStream in) {
		super.multiPartUpload(container, object, sizePart, in);
		
		try {
			System.out.println(in.available());
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		UploadPartResult uploadPart = null; 
		List<PartEtag> partEtags = new ArrayList<PartEtag>();
		PartEtag partEtag = new PartEtag();
		
		//do init
		InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest();
		initRequest.setBucketName(container);
		initRequest.setObjectKey(object);
		InitiateMultipartUploadResult imu = client.initiateMultipartUpload(initRequest);
		System.out.println("bucketName:" + imu.getBucketName()
        + "\tObjectKey:" + imu.getObjectKey()
        + "\tUploadId:" + imu.getUploadId());
		
		//do upload
		String uploadId =imu.getUploadId();
		long uploadedLength = 0;
		int partNumber = 1;
		
		try {
			while(uploadedLength < in.available() ){
				client.uploadPart(imu.getBucketName(), imu.getObjectKey(), uploadId, partNumber, in);
			    //The set of segments to be merged.
			    System.out.println(partNumber + " part is : " + uploadPart.getEtag());
			    partEtag.seteTag(uploadPart.getEtag());
			    partEtag.setPartNumber(uploadPart.getPartNumber());
			    partEtags.add(partEtag);
			    
			    partNumber++;
			    uploadedLength += sizePart;
			}
		} catch (ObsException e) {
			System.out. println("Error message: " + e.getErrorMessage());
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	    
		//complete multiPartUpload
		CompleteMultipartUploadRequest request = new CompleteMultipartUploadRequest();
		request.setPartEtag(partEtags);
		request.setUploadId(imu.getUploadId());
		CompleteMultipartUploadResult result = client.completeMultipartUpload(request);
		System.out.println("ObjectKey: "+result. getObjectKey() + ", Etag: " + result.getEtag());
		
	}

	@Override
	public void deleteObjects(String container, Config config, int amount) {
		super.deleteObjects(container, config, amount);
		int flag = 0;
		ObsObject obsObject;
		DeleteObjectsRequest requst = new DeleteObjectsRequest();
		KeyAndVersion[] keyAndVersions = new KeyAndVersion[amount];
		
		requst.setBucketName(container);
		ObjectListing result = client.listObjects(container);
	    Iterator<ObsObject> itr = result.getObjects().iterator();
	    //get the sets of objects
	    while(itr.hasNext() && flag < amount) {
	    	obsObject = (ObsObject)itr.next();
	    	System.out.println(obsObject.getObjectKey());
	    	keyAndVersions[flag] = new KeyAndVersion(obsObject.getObjectKey());
	    	requst.setKeyAndVersions(keyAndVersions);
	    	flag++;
	    }
	    
	    client.deleteObjects(requst);
	}


	
}
