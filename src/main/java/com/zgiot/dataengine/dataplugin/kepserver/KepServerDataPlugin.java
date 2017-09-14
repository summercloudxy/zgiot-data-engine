package com.zgiot.dataengine.dataplugin.kepserver;

import com.zgiot.common.pojo.DataModel;
import com.zgiot.common.pojo.MetricModel;
import com.zgiot.common.pojo.ThingModel;
import com.zgiot.dataengine.config.OpcUaProperties;
import com.zgiot.dataengine.dataplugin.DataPlugin;
import com.zgiot.dataengine.common.queue.QueueManager;
import com.zgiot.dataengine.repository.ThingMetricLabel;
import com.zgiot.dataengine.service.DataEngineService;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.SessionActivityListener;
import org.eclipse.milo.opcua.sdk.client.api.UaSession;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfig;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.stack.client.UaTcpStackClient;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoredItemCreateRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoringParameters;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

import static com.google.common.collect.Lists.newArrayList;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

@Configuration
@EnableConfigurationProperties(OpcUaProperties.class)
public class KepServerDataPlugin implements DataPlugin {

    private static final Logger logger = LoggerFactory.getLogger(KepServerDataPlugin.class);

    private AtomicLong clientHandles = new AtomicLong(1L);

    @Autowired
    private OpcUaProperties opcUaProperties;

    private static OpcUaClient opcClient;
    private static AtomicBoolean opcClientConnected = new AtomicBoolean(false);
    private double subscriptionInterval = 1000.0; // 1s
    private static final int RETRY_KEP_INTERVAL = 5000; // ms
    private static final int OPC_NAMESPACE_INDEX = 2;

    @Autowired
    private DataEngineService dataEngineService;

    public void init() throws Exception {
        // create milo client
        try {
            dataEngineService.initCache();
            opcClient = createClient();
            clientHandles.set(1l);
            opcClient.addSessionActivityListener(new SessionActivityListener() {
                @Override
                public void onSessionActive(UaSession session) {
                    logger.info("OPC UA Session Active. (id='{}', name='{}')", session.getSessionId(), session.getSessionName());
                    // synchronous connect
                    opcClientConnected.set(true);
                }

                @Override
                public void onSessionInactive(UaSession session) {
                    logger.info("OPC UA Session InActive. (id='{}', name='{}')", session.getSessionId(), session.getSessionName());
                    opcClientConnected.set(false);

                    while (!opcClientConnected.get()) {
                        try {
                            logger.warn("Will retry to start KepServer plugin in {}ms.", RETRY_KEP_INTERVAL);
                            Thread.sleep(RETRY_KEP_INTERVAL);
                            init();
                            start();
                        } catch (Exception e) {
                            logger.error("OPC session failed.", e);
                        }
                    }

                    logger.info("KepServer connection resumed. ");
                }
            });
        } catch (Exception e) {
            throw e;
        }
    }

    private OpcUaClient createClient() throws Exception {
        KeyStoreLoader loader = new KeyStoreLoader();
        SecurityPolicy securityPolicy = SecurityPolicy.None;

        String ep = opcUaProperties.getEndpointUrl();
        EndpointDescription[] endpoints = UaTcpStackClient.getEndpoints(ep).get();

        EndpointDescription endpoint = Arrays.stream(endpoints)
                .filter(e -> e.getSecurityPolicyUri().equals(securityPolicy.getSecurityPolicyUri()))
                .findFirst().orElseThrow(() -> new Exception("no desired endpoints returned"));

        logger.info("Using endpoint: {} [{}]", endpoint.getEndpointUrl(), securityPolicy);

        loader.load();

        OpcUaClientConfig config =
                OpcUaClientConfig.builder()
                        .setApplicationName(LocalizedText.english("zgiot opc-ua client"))
                        .setApplicationUri("urn:zgiot:opcua:client")
                        .setCertificate(loader.getClientCertificate())
                        .setKeyPair(loader.getClientKeyPair())
                        .setEndpoint(endpoint)
                        .setIdentityProvider(new AnonymousProvider())
                        .setRequestTimeout(uint(15000))
                        .build();

        return new OpcUaClient(config);
    }

    public void start() throws Exception {

        /* subscribe all the labels */
        opcClient.connect().get();

        // create a subscription
        UaSubscription subscription = opcClient.getSubscriptionManager()
                .createSubscription(this.subscriptionInterval).get();


        // when creating items in MonitoringMode.Reporting this callback is where each item needs to have its
        // value/event consumer hooked up. The alternative is to create the item in sampling mode, hook up the
        // consumer after the creation call completes, and then change the mode for all items to reporting.
        BiConsumer<UaMonitoredItem, Integer> onItemCreated =
                (item, id) -> item.setValueConsumer(this::onSubscriptionValue);

        List<String> labels = findAvailableLabels();
        ConcurrentHashMap<String, String> failedSubsLabelMap = new ConcurrentHashMap<>();

        for (String label : labels) {
            MonitoredItemCreateRequest request = createMonitorRequest(label);
            /* multi requests mode may suffer from timeout if has large size subscription,
            so use multi items instead */
            List<UaMonitoredItem> items = subscription.createMonitoredItems(
                    TimestampsToReturn.Both,
                    newArrayList(request),
                    onItemCreated
            ).get();

            for (UaMonitoredItem item : items) {
                if (item.getStatusCode().isGood()) {
                    logger.info("item created for nodeId={}", item.getReadValueId().getNodeId());
                } else {
                    failedSubsLabelMap.put(item.getReadValueId().getNodeId().toString()
                            , item.getStatusCode().toString());
                    logger.warn(
                            "failed to create item for nodeId={} (status={})",
                            item.getReadValueId().getNodeId(), item.getStatusCode());
                }
            }
        }

        // check any failed
        if (failedSubsLabelMap.size() > 0) {
            throw new RuntimeException("Failed subscription found, pls check your db/kepserver config and restart again! ");
        } else {
            opcClientConnected.set(true);
            logger.info("Success to subscribe all labels. ");
        }

    }

    private MonitoredItemCreateRequest createMonitorRequest(String label) {

        // subscribe to the Value attribute of the server's CurrentTime node
        ReadValueId readValueId = new ReadValueId(
                new NodeId(OPC_NAMESPACE_INDEX, label)
                , AttributeId.Value.uid()
                , null
                , QualifiedName.NULL_VALUE);

        // important: client handle must be unique per item
        UInteger clientHandle = uint(clientHandles.getAndIncrement());

        MonitoringParameters parameters = new MonitoringParameters(clientHandle
                , 500.0, // 采样间隔
                null, // filter, null means use default
                uint(10), // 队列长度
                true // 是否抛弃旧数据
        );

        MonitoredItemCreateRequest req = new MonitoredItemCreateRequest(
                readValueId, MonitoringMode.Reporting, parameters);

        return req;
    }

    private List<String> findAvailableLabels() {
        List<ThingMetricLabel> srcList = this.dataEngineService.findAllTML();
        List<String> list = new ArrayList<>(srcList.size());
        for (ThingMetricLabel item : srcList) {
            if (item.getEnabled() == 1) {
                list.add(item.getLabelPath()); // e.g. "XG.XG.1303/PR/CURRENT/0"
            }
        }
        return list;
    }

    private void onSubscriptionValue(UaMonitoredItem item, DataValue value) {
        logger.trace(
                "subscription value received: item={}, value={}",
                item.getReadValueId().getNodeId(), value.getValue());
        // parse
        DataModel data = parseToDataModel(item, value);

        // send to Q
        Queue q = QueueManager.getQueueCollected();
        q.add(data);

    }

    private DataModel parseToDataModel(UaMonitoredItem item, DataValue value) {
        DataModel data = new DataModel();
        // e.g.  "XG.XG.1303/PR/CURRENT/0"
        try {
            String dataLabel = item.getReadValueId().getNodeId().getIdentifier().toString();
            ThingMetricLabel tml = this.dataEngineService.getTMLByLabel(dataLabel);
            if (tml == null) {
                throw new RuntimeException("Cannot match label to std model, pls check db config. (label=`" + dataLabel + "`)");
            }

            if (value.getStatusCode().isGood()) {
                data.setDataTimeStamp(value.getSourceTime().getJavaDate());
                data.setThingCategoryCode(ThingModel.CATEGORY_DEVICE);
                data.setMetricCategoryCode(MetricModel.CATEGORY_SIGNAL);
                data.setThingCode(tml.getThingCode());
                data.setMetricCode(tml.getMetricCode());
                data.setValue(parseOpcValueToString(value));

            } else {
                logger.warn("Not good data responsed, nodeId is '{}', status is: '{}' "
                        , item.getReadValueId().getNodeId(), value.getStatusCode().toString());
                data.setThingCategoryCode(ThingModel.CATEGORY_ERROR);
                data.setMetricCategoryCode(MetricModel.CATEGORY_SIGNAL);
                data.setThingCode(tml.getThingCode());
                data.setMetricCode(tml.getMetricCode());
                data.setDataTimeStamp(new Date());
            }

        } catch (Exception e) {
            logger.warn("Unexpected data responsed, nodeId is '{}', error msg is: '{}' "
                    , item.getReadValueId().getNodeId(), e.getMessage());
            data.setThingCategoryCode(ThingModel.CATEGORY_ERROR);
            data.setMetricCategoryCode(MetricModel.CATEGORY_SIGNAL);
            data.setDataTimeStamp(new Date());
        }

        return data;
    }

    private String parseOpcValueToString(DataValue value){
        // todo DINT for filterpress
        NodeId node = value.getValue().getDataType().get();
        return value.getValue().getValue().toString();
    }

    public int sendCommands(List<DataModel> datalist) throws Exception {

        if (datalist == null || datalist.size() == 0) {
            logger.warn("Send command list is empty. ({})", datalist);
            return 0;
        }

        List<NodeId> nodeIds = new ArrayList<>(datalist.size());
        List<DataValue> values = new ArrayList<>(datalist.size());

        for (DataModel data : datalist) {
            // parse data to label and value
            String labelPath = this.dataEngineService.getLabelByTM(data.getThingCode()
                    , data.getMetricCode());

            NodeId nodeId = new NodeId(OPC_NAMESPACE_INDEX, labelPath);
            nodeIds.add(nodeId);

            DataValue v = new DataValue(new Variant(data.getValueObj())
                    , null, null);
            values.add(v);

            logger.debug("Pre-send cmd label='{}'  value='{}' ", labelPath, data.getValue());

        }

        // send

        CompletableFuture<List<StatusCode>> future = opcClient
                .writeValues(nodeIds, values);
        List<StatusCode> statuses = future.get();

        int goodCount = 0;
        for (StatusCode status : statuses) {
            if (status.isGood()) {
                goodCount++;
            }
        }

        return goodCount;
    }

}
