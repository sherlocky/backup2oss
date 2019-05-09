package com.sherlocky.back2oss.service;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.sherlocky.utils.CompressUtil;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.Date;

@Service
public class BackupService {
	private Log logger = LogFactory.getLog(getClass());
	
	@Value("${backup.zip.password}")
	private String BACKUP_ZIP_PASSWORD;
    //设置好账号的ACCESS_KEY和SECRET_KEY
	@Value("${backup.qiniu.accesskey}")
    private String ACCESS_KEY;
	@Value("${backup.qiniu.secretkey}")
    private String SECRET_KEY;
	@Value("${backup.qiniu.bucket}")
    private String BUCKET;
	@Value("${backup.filename.filter.prefix}") 
    private String FILENAME_FILTER_PREFIX; // 文件前缀
	@Value("${backup.foldername}") // 文件夹 
    private String FOLDER_PATH;
	
    private static Auth auth;
    private static UploadManager uploadManager;
    
    private void initUploadConfig() {
	    //密钥配置
	    auth = Auth.create(ACCESS_KEY, SECRET_KEY);
	    ///////////////////////指定上传的Zone的信息//////////////////
	    //第一种方式: 指定具体的要上传的zone
	    //注：该具体指定的方式和以下自动识别的方式选择其一即可
	    //要上传的空间(bucket)的存储区域为华东时
	    // Zone z = Zone.zone0();
	    //要上传的空间(bucket)的存储区域为华北时
	    // Zone z = Zone.zone1();
	    //要上传的空间(bucket)的存储区域为华南时
	    // Zone z = Zone.zone2();
	    //第二种方式: 自动识别要上传的空间(bucket)的存储区域是华东、华北、华南。
	    Zone z = Zone.autoZone();
	    Configuration c = new Configuration(z);
	    //创建上传对象
	    uploadManager = new UploadManager(c);
    }
    
    private String getUpToken(String bucketname, String key) {
    	// <bucket>:<key>，表示只允许用户上传指定key的文件。在这种格式下文件默认允许“修改”，已存在同名资源则会被本次覆盖。
        return auth.uploadToken(bucketname, key);
        /**
         * 如果希望只能上传指定key的文件，并且不允许修改，那么可以将下面的 insertOnly 属性值设为 1。
         * 第三个参数是token的过期时间
         * 	return auth.uploadToken(bucketname, key, 3600, new StringMap().put("insertOnly", 1));
         */
    }

    public void backup() {
        //上传文件的路径
    	//TODO
        String filePath = null;
        File folder = new File(FOLDER_PATH);
        if (!folder.exists()) {
        	logger.error("$$$ 要备份的文件目录不存在！");
        	return;
        }
        if (!folder.isDirectory()) {
        	logger.error("$$$ 不是有效的目录");
        	return;
        }
        // 
        final String nameFilter = FILENAME_FILTER_PREFIX + new SimpleDateFormat("yyyyMMdd").format(new Date());
        File[] backupFiles = folder.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				if (StringUtils.startsWith(name, nameFilter)) {
					return true;
				}
				return false;
			}
		});
        if (ArrayUtils.isEmpty(backupFiles)) {
        	logger.error("## 没有需要备份的文件，" + nameFilter);
        	return;
        }
        filePath = backupFiles[0].getAbsolutePath();
        // 加密压缩文件
        String encyptFilePath = this.zipEncypt(filePath);
        if (StringUtils.isEmpty(encyptFilePath)) {
        	 logger.info("### 加密压缩文件失败！！");
        	 return;
        }
       	String result = this.upload(BUCKET, encyptFilePath, this.getFileKey(encyptFilePath));
       	if (StringUtils.isNotBlank(result)) {
       		logger.info("## 上传成功！");
       	} else {
       		logger.error("$$$ 上传失败！");
       	}
    }
    
    private String zipEncypt(String filePath) {
    	String destPath = filePath + ".zip";
    	CompressUtil.zip(filePath, destPath, BACKUP_ZIP_PASSWORD);
    	File dest = new File(destPath);
    	if (dest.exists() && dest.length() > 0) {
    		return destPath;
    	}
    	return null;
    }
    
    private String upload(String bucketname, String filePath, String key) {
    	initUploadConfig();
        String result = null;
        try {
            //调用put方法上传
            Response res = uploadManager.put(filePath, key, getUpToken(bucketname, key));
            result = res.bodyString();
            //打印返回的信息
            logger.info(" ### 上传请求返回结果：" + result);
        } catch (QiniuException e) {
            Response r = e.response;
            // 请求失败时打印的异常的信息
            logger.error("$$$ 上传请求失败 ： " + r.toString(), e);
            try {
                //响应的文本信息
            	logger.error("$$$ 上传请求失败 ： " + r.bodyString());
            } catch (QiniuException e1) {
            }
        } finally {
            return result;
        }
    }
    
/*    public static void main(String args[]) throws IOException {
        //要上传的空间
        String bucketname = "ghost";
        //上传到七牛后保存的文件名
        // db_ghost-blog-new-$(date +%Y%m%d_%H%M%S).sql.gz
        String key = "backup/db/db_ghost-blog-new-20170116_094911.sql.gz";
        //上传文件的路径
        String filePath = "F:\\VPS\\香港VPS\\db_ghost-blog-new-20170116_094911.sql.gz";//"/backup/db";
        new BackupService().upload(bucketname, filePath, key);
    }*/

    private String getFileKey(String encyptFilePath) {
        //上传到七牛后保存的文件名
        // db_ghost-blog-new-$(date +%Y%m%d_%H%M%S).sql.gz
        // String key = "backup/db/db_ghost-blog-new-20170116_094911.sql.gz.zip";
        String prefix = "backup/db/";
        return prefix + FilenameUtils.getName(encyptFilePath);
	}
}