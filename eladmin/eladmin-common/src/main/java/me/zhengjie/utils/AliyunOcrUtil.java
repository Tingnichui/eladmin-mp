package me.zhengjie.utils;

import com.aliyun.ocr_api20210707.Client;
import com.aliyun.ocr_api20210707.models.*;
import com.aliyun.teaopenapi.models.Config;
import me.zhengjie.exception.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

@Component
public class AliyunOcrUtil {

    @Value("${aliyun.ocr.accessKey.id:}")
    private String accessKeyId;

    @Value("${aliyun.ocr.accessKey.secret:}")
    private String accessKeySecret;

    @Value("${aliyun.ocr.endpoint:}")
    private String endpoint;

    public String recognizeGeneral(File file) {
        if (file == null || !file.exists() || !file.isFile() || file.length() == 0) {
            throw new BizException("OCR file is required");
        }
        try (InputStream inputStream = new FileInputStream(file)) {
            Client client = createClient();
            RecognizeGeneralRequest request = new RecognizeGeneralRequest()
                    .setBody(inputStream);
            RecognizeGeneralResponse response = client.recognizeGeneral(request);
            RecognizeGeneralResponseBody body = response.getBody();
            return body == null ? null : body.getData();
        } catch (Exception e) {
            throw new BizException("阿里云OCR失败: " + e.getMessage());
        }
    }

    public String recognizeDocumentStructure(File file) {
        if (file == null || !file.exists() || !file.isFile() || file.length() == 0) {
            throw new BizException("OCR file is required");
        }
        try (InputStream inputStream = new FileInputStream(file)) {
            Client client = createClient();
            RecognizeDocumentStructureRequest request = new RecognizeDocumentStructureRequest()
                    .setBody(inputStream);
            RecognizeDocumentStructureResponse response = client.recognizeDocumentStructure(request);
            RecognizeDocumentStructureResponseBody body = response.getBody();
            return body == null ? null : body.getData();
        } catch (Exception e) {
            throw new BizException("阿里云OCR失败: " + e.getMessage());
        }
    }

    private Client createClient() throws Exception {
        Config config = new Config()
                .setAccessKeyId(accessKeyId)
                .setAccessKeySecret(accessKeySecret)
                .setEndpoint(endpoint);
        return new Client(config);
    }
}
