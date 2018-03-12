package com.zgiot.dataengine.config;

import com.zgiot.common.reloader.ServerReloadManager;
import com.zgiot.dataengine.common.ThreadManager;
import com.zgiot.dataengine.dataplugin.DataPlugin;
import com.zgiot.dataengine.dataplugin.excel.ExcelDataPlugin;
import com.zgiot.dataengine.dataplugin.kepserver.KepServerDataPlugin;
import com.zgiot.dataengine.dataprocessor.DataProcessorManager;
import com.zgiot.dataengine.service.DataEngineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ApplicationEventsHandler implements ApplicationListener<ContextRefreshedEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationEventsHandler.class);

    @Value("${dataengine.plugins}")
    String pluginsStr;

    @Autowired
    private DataProcessorManager dataProcessorManager;
    @Autowired
    private KepServerDataPlugin kepServerDataPlugin;
    @Autowired
    private ExcelDataPlugin excelDataPlugin;
    @Autowired
    private DataEngineService dataEngineService;

    public void onApplicationEvent(ContextRefreshedEvent event) {
        try {

            registerReloaders();
            dataEngineService.initCache();

            // init plugin clazz map
            Map<String, DataPlugin> map = new HashMap<>();
            map.put("NONE", null);
            map.put("KEPSERVER", kepServerDataPlugin);
            map.put("EXCEL", excelDataPlugin);

            String[] pluginNameArr = this.pluginsStr.split(",");
            for (String str : pluginNameArr) {
                DataPlugin dataPlugin = map.get(str.trim());
                if (dataPlugin == null) {
                    continue;
                }

                dataPlugin.init();
                dataPlugin.start();
                LOGGER.info("Data plugin `{}` started. ", dataPlugin.getClass());
            }

            // init thread pool
            ThreadManager.getThreadPool();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                ThreadManager.getThreadPool().shutdown();
                System.out.println("Thread pool shutdown. "); //NOPMD
            }));

        } catch (Throwable e) {
            LOGGER.error("Failed to startup KepServer plugin! ", e);
            System.exit(2);
        }

        dataProcessorManager.start();
        LOGGER.info("Data processor started. ");
    }

    private void registerReloaders() {
        ServerReloadManager.addReloader(this.dataEngineService);
        ServerReloadManager.addReloader(this.kepServerDataPlugin);
    }

}
