package me.zhengjie.config.logging;

import ch.qos.logback.core.PropertyDefinerBase;
import me.zhengjie.AppRun;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.util.StringUtils;

import java.io.File;

/**
 * Resolves the default log directory to the folder that contains the application jar.
 */
public class ApplicationHomeLogPropertyDefiner extends PropertyDefinerBase {

    @Override
    public String getPropertyValue() {
        String configured = System.getProperty("LOG_PATH");
        if (!StringUtils.hasText(configured)) {
            configured = System.getenv("LOG_PATH");
        }
        if (StringUtils.hasText(configured)) {
            return configured;
        }
        File dir = new ApplicationHome(AppRun.class).getDir();
        return new File(dir, "logs").getAbsolutePath();
    }
}
