package com.sherlocky.back2oss.service;

import com.alibaba.fastjson.JSON;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.BatchStatus;
import com.qiniu.storage.model.FileInfo;
import com.qiniu.util.Auth;
import com.sherlocky.utils.CompressUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BackupService {
	@Value("${backup.zip.password}")
	private String BACKUP_ZIP_PASSWORD;
    //设置好账号的ACCESS_KEY和SECRET_KEY
	@Value("${backup.qiniu.accesskey}")
    private String ACCESS_KEY;
	@Value("${backup.qiniu.secretkey}")
    private String SECRET_KEY;
	@Value("${backup.qiniu.bucket}")
    private String BUCKET;
	// 在七牛存储时的文件前缀
    @Value("${backup.qiniu.file.prefix}")
    private String QINIU_FILE_PREFIX;
	@Value("${backup.filename.filter.prefix}") 
    private String FILENAME_FILTER_PREFIX; // 文件前缀
	@Value("${backup.filename.filter.delimiter}")
	private String FILENAME_FILTER_DELIMITER; // 文件目录分隔符
	@Value("${backup.file.limit}")
	private Integer FILE_LIMIT; // 查询时文件个数
	@Value("${backup.foldername}") // 文件夹 
    private String FOLDER_PATH;

    private Auth auth;
    private UploadManager uploadManager;
    private Configuration configuration;
    private BucketManager bucketManager;
    
    private void initUploadConfig() {
        if (auth != null) {
            return;
        }
        synchronized (BackupService.class) {
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
    	    configuration = new Configuration(Zone.autoZone());
    	    //创建上传对象
    	    uploadManager = new UploadManager(configuration);
            bucketManager = new BucketManager(auth, configuration);
        }
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
        String filePath = null;
        File folder = new File(FOLDER_PATH);
        if (!folder.exists()) {
        	log.error("$$$ 要备份的文件目录不存在！");
        	return;
        }
        if (!folder.isDirectory()) {
        	log.error("$$$ 不是有效的目录");
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
        	log.error("## 没有需要备份的文件，" + nameFilter);
        	return;
        }
        filePath = backupFiles[0].getAbsolutePath();
        // 加密压缩文件
        String encyptFilePath = this.zipEncypt(filePath);
        if (StringUtils.isEmpty(encyptFilePath)) {
        	 log.info("### 加密压缩文件失败！！");
        	 return;
        }
        // 删除压缩前文件
        FileUtils.deleteQuietly(backupFiles[0]);
       	String result = this.upload(BUCKET, encyptFilePath, this.getFileKey(encyptFilePath));
       	if (StringUtils.isNotBlank(result)) {
       		log.info("## 上传成功！");
       	} else {
       		log.error("$$$ 上传失败！");
       	}
    }
    
    // db_ghost-blog-new-20170815_030001.sql.gz --> db_ghost-blog-new-20170815_030001.sql.gz.zip
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
            log.info(" ### 上传请求返回结果：" + result);
        } catch (QiniuException e) {
            Response r = e.response;
            // 请求失败时打印的异常的信息
            log.error("$$$ 上传请求失败 ： " + r.toString(), e);
            try {
                //响应的文本信息
            	log.error("$$$ 上传请求失败 ： " + r.bodyString());
            } catch (QiniuException e1) {
            }
        } finally {
            return result;
        }
    }

    /**
     * 获取上一个月的日期字符串，例如：201904
     * @return
     */
    private String getLastMonthStr() {
        return LocalDate.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyyMM"));
    }

    /**
     * 清理上一个月的过期备份文件
     * <p>历史月份，每月只保留一份最早日期的数据</p>
     * @return
     */
    public String[] deleteLastMonthOldFiles() {
        // 获取上个月的所有备份文件
        List<FileInfo> lastMonthFiles = this.listBackupFiles(this.getLastMonthStr());
        if (lastMonthFiles == null || lastMonthFiles.size() <= 1) {
            log.error("### 没有需要删除的文件！");
            return null;
        }
        // 先对上个月备份文件，按文件名（日期）正序排序
        lastMonthFiles.sort((a, b) -> a.key.compareTo(b.key));
        // 每个月只保留最早的一个备份（故删除时跳过第1个）
        String[] needDeleteKeyList = lastMonthFiles.stream().skip(1).map((f) -> {
            return f.key;
        }).collect(Collectors.toList()).toArray(new String[0]);
        try {
            BucketManager.BatchOperations batchOperations = new BucketManager.BatchOperations();
            batchOperations.addDeleteOp(BUCKET, needDeleteKeyList);
            Response response = bucketManager.batch(batchOperations);
            BatchStatus[] batchStatusList = response.jsonToObject(BatchStatus[].class);
            for (int i = 0; i < needDeleteKeyList.length; i++) {
                BatchStatus status = batchStatusList[i];
                String key = needDeleteKeyList[i];
                if (status.code == 200) {
                    log.info(key + "\t delete success");
                } else {
                    log.info(key + "\t " + status.data.error);
                }
            }
        } catch (QiniuException ex) {
           log.error(ex.response.toString(), ex);
        }
        return needDeleteKeyList;
    }

    public List<FileInfo> listBackupFiles() {
        return this.listBackupFiles(StringUtils.EMPTY);
    }

    /**
     * 获取空间文件列表
     * @param dateStr 指定日期前缀
     * @author zhangcx
     * @date 2017-09-05
     */
    public List<FileInfo> listBackupFiles(String dateStr) {
        initUploadConfig();
        BucketManager.FileListIterator fileListIterator = bucketManager.createFileListIterator(BUCKET, QINIU_FILE_PREFIX + FILENAME_FILTER_PREFIX + dateStr, FILE_LIMIT, FILENAME_FILTER_DELIMITER);
        List<FileInfo> files = new ArrayList<FileInfo>();
        while (fileListIterator.hasNext()) {
            //处理获取的file list结果
            CollectionUtils.addAll(files, fileListIterator.next());
/*            FileInfo[] items = fileListIterator.next();
            for (FileInfo item : items) {
                System.out.println(item.key);
                System.out.println(item.hash);
                System.out.println(item.fsize);
                System.out.println(item.mimeType);
                System.out.println(item.putTime);
                System.out.println(item.endUser);
            }*/
        }
        log.info(JSON.toJSONString(files));
        return files;
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
        // String prefix = "backup/db/";
        return QINIU_FILE_PREFIX + FilenameUtils.getName(encyptFilePath);
	}
}