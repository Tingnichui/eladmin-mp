package me.zhengjie.iou.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.security.MessageDigest;
import java.util.Base64;

/** Immutable private files; never serves an arbitrary client path. */
@Service
@RequiredArgsConstructor
public class IouAgreementStore {
    private final IouNoteOptions options;
    public static String hash(byte[] bytes) {
        try {byte[] digest=MessageDigest.getInstance("SHA-256").digest(bytes);return hex(digest);}
        catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    public static String hash(String text){return hash(text.getBytes(StandardCharsets.UTF_8));}
    private static String hex(byte[] bytes){StringBuilder s=new StringBuilder();for(byte b:bytes)s.append(String.format("%02x",b&255));return s.toString();}
    public String seal(String value) {
        if(options.getSealKey()==null||options.getSealKey().length()<32)throw new IouBusinessException(503,"CONFIG_NOT_READY","签署配置尚未就绪");
        try{Mac m=Mac.getInstance("HmacSHA256");m.init(new SecretKeySpec(options.getSealKey().getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return hex(m.doFinal(value.getBytes(StandardCharsets.UTF_8)));}
        catch(java.security.GeneralSecurityException e){throw new IllegalStateException(e);}
    }
    public static byte[] signature(String data) {
        if(data==null||!data.startsWith("data:image/png;base64,")||data.length()>200000)invalid();
        try {
            byte[] bytes=Base64.getDecoder().decode(data.substring(22));
            if(bytes.length>140000)invalid();
            // Read dimensions before decompression to reject oversized/decompression-bomb images.
            try(javax.imageio.stream.ImageInputStream in=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))){
                java.util.Iterator<javax.imageio.ImageReader> readers=ImageIO.getImageReaders(in);
                if(!readers.hasNext())invalid();javax.imageio.ImageReader reader=readers.next();
                try{reader.setInput(in);int w=reader.getWidth(0),h=reader.getHeight(0);if(!"png".equalsIgnoreCase(reader.getFormatName())||w<100||w>1200||h<60||h>600)invalid();}
                finally{reader.dispose();}
            }
            BufferedImage image=ImageIO.read(new ByteArrayInputStream(bytes));if(image==null)invalid();
            int ink=0;
            for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++) {
                int pixel=image.getRGB(x,y);int alpha=pixel>>>24;
                if(alpha>128&&((pixel>>16)&255)<180&&((pixel>>8)&255)<180&&(pixel&255)<180)ink++;
            }
            if(ink<30||ink>image.getWidth()*image.getHeight()/2)invalid();
            ByteArrayOutputStream sanitized=new ByteArrayOutputStream();ImageIO.write(image,"png",sanitized);return sanitized.toByteArray();
        } catch(IOException|IllegalArgumentException e){invalid();return null;}
    }
    private static void invalid(){throw new IouBusinessException(400,"INVALID_SIGNATURE","请在签名区域手写签名，图片格式或内容无效");}
    private Path resolve(String key){
        Path root=Paths.get(options.getStoragePath()).toAbsolutePath().normalize(),path=root.resolve(key).normalize();
        if(!path.startsWith(root)||key.contains("..")||key.startsWith("/")||key.contains("\\"))throw new IouBusinessException(409,"AGREEMENT_NOT_READY","文件暂不可用");
        return path;
    }
    public String write(String key,byte[] value){
        Path path=resolve(key);
        try {
            Files.createDirectories(path.getParent());Files.write(path,value,StandardOpenOption.CREATE_NEW);
            if(TransactionSynchronizationManager.isSynchronizationActive())TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
                @Override public void afterCompletion(int status){if(status!=STATUS_COMMITTED)try{Files.deleteIfExists(path);}catch(IOException ignored){}}
            });
            return key;
        }catch(IOException e){throw new IouBusinessException(503,"FILE_STORAGE_UNAVAILABLE","签署文件保存失败，请稍后重试");}
    }
    public byte[] read(String key,String expectedHash){
        try{byte[] bytes=Files.readAllBytes(resolve(key));if(expectedHash==null||!hash(bytes).equals(expectedHash))throw new IouBusinessException(409,"FILE_INTEGRITY_ERROR","文件校验失败，请联系管理员");return bytes;}
        catch(IOException e){throw new IouBusinessException(409,"AGREEMENT_NOT_READY","文件尚未就绪");}
    }
    public static String escape(String text){return text==null?"":text.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
}
