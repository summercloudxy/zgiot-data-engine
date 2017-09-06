package com.zgiot.dataengine.config;

import com.zgiot.dataengine.dataplugin.DataPlugin;
import com.zgiot.dataengine.dataplugin.excel.ExcelDataPlugin;
import com.zgiot.dataengine.dataplugin.kepserver.KepServerDataPlugin;
import com.zgiot.dataengine.common.ThreadManager;
import com.zgiot.dataengine.dataprocessor.DataProcessorManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class ApplicationEventsHandler implements ApplicationListener<ContextRefreshedEvent> {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationEventsHandler.class);

    @Value("${dataengine.plugins}")
    String pluginsStr;

    @Autowired
    private List<DataPlugin> dataPlugins;

    @Autowired
    private DataProcessorManager dataProcessorManager;

    @Autowired
    private KepServerDataPlugin kepServerDataPlugin;
    @Autowired
    private ExcelDataPlugin excelDataPlugin;

    public void onApplicationEvent(ContextRefreshedEvent event) {
        try {
            // init plugin clazz map
            Map<String,DataPlugin> map = new HashMap<>();
            map.put("NONE",null);
            map.put("KEPSERVER", kepServerDataPlugin);
            map.put("EXCEL", excelDataPlugin);

            String[] pluginNameArr = this.pluginsStr.split(",");
            for (String str: pluginNameArr){
                DataPlugin dataPlugin = map.get(str);
                if (dataPlugin == null)
                    continue;

                dataPlugin.init();
                dataPlugin.start();
            }

            // init thread pool
            ThreadManager.getThreadPool();
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    ThreadManager.getThreadPool().shutdown();
                    System.out.println("Thread pool shutdown. ");
                }
            }));

        } catch (Throwable e) {
            logger.error("Failed to startup KepServer plugin! ", e);
            System.exit(2);
        }

        dataProcessorManager.start();
    }

}

