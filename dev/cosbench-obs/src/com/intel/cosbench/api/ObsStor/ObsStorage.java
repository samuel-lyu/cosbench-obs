package com.intel.cosbench.api.ObsStor;

import static com.intel.cosbench.client.ObsStor.ObsConstants.*;

import java.io.*;

import org.apache.http.HttpStatus;

import com.amazonaws.*;
import com.amazonaws.auth.*;
import com.amazonaws.services.s3.*;
import com.amazonaws.services.s3.model.*;

import com.intel.cosbench.api.storage.*;
import com.intel.cosbench.api.context.*;
import com.intel.cosbench.config.Config;
import com.intel.cosbench.log.Logger;

public class ObsStorage extends NoneStorage{
	private int timeout;

	private String accessKey;
	private String secretKey;
	private String endpoint;

	private AmazonS3 client;

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
        
        ClientConfiguration clientConf = new ClientConfiguration();
        clientConf.setConnectionTimeout(timeout);
        clientConf.setSocketTimeout(timeout);
        clientConf.withUseExpectContinue(false);
        clientConf.withSignerOverride("S3SignerType");
//        clientConf.setProtocol(Protocol.HTTP);
		if((!proxyHost.equals(""))&&(!proxyPort.equals(""))){
			clientConf.setProxyHost(proxyHost);
			clientConf.setProxyPort(Integer.parseInt(proxyPort));
		}
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
		client = null;
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
        	if(!client.doesBucketExist(container)) {
	        	
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
        	if(client.doesBucketExist(container)) {
        		client.deleteBucket(container);
        	}
        } catch(AmazonS3Exception awse) {
        	if(awse.getStatusCode() != HttpStatus.SC_NOT_FOUND) {
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
        } catch(AmazonS3Exception awse) {
        	if(awse.getStatusCode() != HttpStatus.SC_NOT_FOUND) {
        		throw new StorageException(awse);
        	}
        } catch (Exception e) {
            throw new StorageException(e);
        }
	}

	@Override
	public InputStream downloadByRange(String container, String object, Config config, String scope) {
		// TODO Auto-generated method stub
		//return super.downloadByRange(container, object, config);
		System.out.println("ªÒ»°∑∂Œß"+scope);       
//		GetObjectRequest rangeObjectRequest = new GetObjectRequest(
//				"bucketName", "key");
//		rangeObjectRequest.setRange(1,2); // retrieve 1st 11 bytes.
//		S3Object objectPortion = client.getObject(rangeObjectRequest);
//
//		InputStream objectData = objectPortion.getObjectContent();
//		// Process the objectData stream.
//		return objectData;
		return null;
	}
	
}
