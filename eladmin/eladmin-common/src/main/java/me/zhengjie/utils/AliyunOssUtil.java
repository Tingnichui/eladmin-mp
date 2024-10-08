package me.zhengjie.utils;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.CredentialsProvider;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.model.Bucket;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.OSSObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class AliyunOssUtil {

    public static final String bucketNameRe = "^[a-z0-9](?:[a-z0-9]|-(?=[a-z0-9])){1,61}[a-z0-9]$";
    public static final String objectName = "^(?![/\\\\])[^\\p{C}]{1,1023}$\n";

    @Value("${aliyun.oss.bucket:}")
    private String bucket;

    @Value("${aliyun.oss.endpoint:}")
    private String endpoint;

    @Value("${aliyun.oss.accessKey.id:}")
    private String accessKeyId;

    @Value("${aliyun.oss.accessKey.secret:}")
    private String accessKeySecret;

    /**
     * @param path 填写Object完整路径，例如user/exampleobject.txt。Object完整路径中不能包含Bucket名称。
     */
    public String getDownLoadAccess(String path) {

        // 创建OSSClient实例。
        OSS ossClient = getOssClient();

        // 设置请求头。
        // 如果生成签名URL时设置了header参数，例如用户元数据，存储类型等，则调用签名URL下载文件时，也需要将这些参数发送至服务端。如果签名和发送至服务端的不一致，会报签名错误
        Map<String, String> headers = new HashMap<>();
        /*// 指定Object的存储类型。
        headers.put(OSSHeaders.STORAGE_CLASS, StorageClass.Standard.toString());
        // 指定ContentType。
        headers.put(OSSHeaders.CONTENT_TYPE, "text/txt");*/


        // 设置用户自定义元信息。 如果使用userMeta，
        // sdk内部会为userMeta拼接"x-oss-meta-"前缀。当您使用其他方式生成签名URL进行下载时，userMeta也需要拼接"x-oss-meta-"前缀。
        Map<String, String> userMetadata = new HashMap<>();
        /*userMetadata.put("key1","value1");
        userMetadata.put("key2","value2");*/

        URL signedUrl = null;
        // 指定生成的签名URL过期时间，单位为毫秒。本示例以设置过期时间为1分钟为例。
        DateTime expireEndTime = DateUtil.offsetMinute(new Date(), 1);
        // 生成签名URL。
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, path, HttpMethod.GET);
        // 设置过期时间。
        request.setExpiration(expireEndTime);
        // 将请求头加入到request中。
        request.setHeaders(headers);
        // 添加用户自定义元信息。
        request.setUserMetadata(userMetadata);

        // 设置查询参数。
        // Map<String, String> queryParam = new HashMap<String, String>();
        // 指定IP地址或者IP地址段。
        // queryParam.put("x-oss-ac-source-ip","192.0.2.0");
        // 指定子网掩码中1的个数。
        // queryParam.put("x-oss-ac-subnet-mask","32");
        // 指定VPC ID。
        // queryParam.put("x-oss-ac-vpc-id","vpc-12345678");
        // 指定是否允许转发请求。
        // queryParam.put("x-oss-ac-forward-allow","true");
        // request.setQueryParameter(queryParam);

        // 设置单链接限速，单位为bit，例如限速100 KB/s。
        // request.setTrafficLimit(100 * 1024 * 8);

        // 通过HTTP GET请求生成签名URL。
        signedUrl = ossClient.generatePresignedUrl(request);
        // 打印签名URL。
        return signedUrl.toString();
    }

    private OSS getOssClient() {
        CredentialsProvider credentialsProvider = new DefaultCredentialProvider(accessKeyId, accessKeySecret);
        return new OSSClientBuilder().build(endpoint, credentialsProvider);
    }

    /**
     * 创建桶
     */
    public void createBucket(String bucketName) {
        OSS ossClient = getOssClient();
        Bucket bucket = ossClient.createBucket(bucketName);
        ossClient.shutdown();
    }


    /**
     * 删除文件
     */
    public void delete(String bucketName, String objectName) {
        OSS ossClient = getOssClient();
        ossClient.deleteObject(bucketName, objectName);
        ossClient.shutdown();
    }


    /**
     * 上传文件
     *
     * @param bucketName 填写Bucket名称，例如examplebucket。
     * @param objectName 填写Object完整路径，例如exampledir/exampleobject.txt。Object完整路径中不能包含Bucket名称。
     */
    public void upload(String bucketName, String objectName, FileInputStream inputStream) {
        OSS ossClient = getOssClient();
        ossClient.putObject(bucketName, objectName, inputStream);
        ossClient.shutdown();
    }

    public void download(String bucketName, String objectName) throws Exception {
        OSS ossClient = getOssClient();
        // 调用ossClient.getObject返回一个OSSObject实例，该实例包含文件内容及文件元信息。
        OSSObject ossObject = ossClient.getObject(bucketName, objectName);
        // 调用ossObject.getObjectContent获取文件输入流，可读取此输入流获取其内容。
        InputStream content = ossObject.getObjectContent();
        if (content != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(content));
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                System.out.println("\n" + line);
            }
            // 数据读取完成后，获取的流必须关闭，否则会造成连接泄漏，导致请求无连接可用，程序无法正常工作。
            content.close();
        }
        ossClient.shutdown();
    }


    /**
     * @param dir 设置上传到OSS文件的前缀，可置空此项。置空后，文件将上传至Bucket的根目录下。
     * @return
     */
//    public AliyunOssUploadAccess getUploadAccess(String dir, String fileName) {
//        // 填写Host名称，格式为https://bucketname.endpoint。
//        String host = "https://" + bucket + "." + endpoint;
//
//        // 创建OSSClient实例。
//        OSS ossClient = getOssClient();
//
//        PolicyConditions policyConds = new PolicyConditions();
//        // 设置上传大小 此处设置为1G PostObject请求最大可支持的文件大小为5 GB，即CONTENT_LENGTH_RANGE为5*1024*1024*1024。
//        policyConds.addConditionItem(PolicyConditions.COND_CONTENT_LENGTH_RANGE, 0, 1 * 1024 * 1024 * 1024);
//        // 设置上传目录
//        String key = dir + (StringUtils.isBlank(fileName) ? "" : fileName);
//        policyConds.addConditionItem(MatchMode.StartWith, PolicyConditions.COND_KEY, key);
//
//        // 设置过期时间
//        DateTime expireEndTime = DateUtil.offsetMinute(new Date(), 1);
//        String postPolicy = ossClient.generatePostPolicy(expireEndTime, policyConds);
//        byte[] binaryData = postPolicy.getBytes(StandardCharsets.UTF_8);
//        String encodedPolicy = BinaryUtil.toBase64String(binaryData);
//        String postSignature = ossClient.calculatePostSignature(postPolicy);
//
//        AliyunOssUploadAccess access = new AliyunOssUploadAccess();
//        access.setAccessid(accessKeyId);
//        access.setPolicy(encodedPolicy);
//        access.setSignature(postSignature);
//        access.setDir(dir);
//        access.setFilePath(key);
//        access.setHost(host);
//        access.setExpire(String.valueOf(expireEndTime.getTime() / 1000));
//        ossClient.shutdown();
//        return access;
//
//    }

}
