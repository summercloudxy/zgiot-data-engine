package com.zgiot.dataengine.config;

import com.zgiot.dataengine.dataplugin.DataPlugin;
import com.zgiot.dataengine.dataplugin.excel.ExcelDataPlugin;
import com.zgiot.dataengine.dataplugin.kepserver.KepServerDataPlugin;
import com.zgiot.dataengine.common.ThreadManager;
import com.zgiot.dataengine.dataprocessor.DataProcessorManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.List;

@Configuration
public class ApplicationEventsHandler implements ApplicationListener<ContextRefreshedEvent> {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationEventsHandler.class);

    @Autowired
    private List<DataPlugin> dataPlugins;

    @Autowired
    private DataProcessorManager dataProcessorManager;

    @Autowired
    private KepServerDataPlugin kepServerDataCollecter;
    @Autowired
    private ExcelDataPlugin excelDataCollecter;

    public void onApplicationEvent(ContextRefreshedEvent event) {
        try {
            // TODO 配置需要加载的插件
            this.dataPlugins.clear();
//            this.dataPlugins.add(this.kepServerDataCollecter);
            this.dataPlugins.add(this.excelDataCollecter);
            for (DataPlugin dataPlugin : dataPlugins){
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

