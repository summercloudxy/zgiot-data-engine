package com.zgiot.dataengine.dataplugin.excel;

import com.alibaba.fastjson.JSON;
import com.zgiot.common.pojo.CoalAnalysisRecord;
import com.zgiot.common.pojo.DataModel;
import com.zgiot.dataengine.common.queue.QueueManager;
import com.zgiot.dataengine.dataplugin.DataPlugin;
import com.zgiot.dataengine.dataplugin.excel.dao.ExcelMapper;
import com.zgiot.dataengine.dataplugin.excel.pojo.ExcelRange;
import com.zgiot.dataengine.dataplugin.excel.pojo.RecordTimeRange;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.*;

@Component
public class ExcelDataPlugin implements DataPlugin {
    @Autowired
    private ExcelMapper excelMapper;
    private List<ExcelRange> excelRangeList;
    private static final Logger logger = LoggerFactory.getLogger(ExcelDataPlugin.class);
    private static final int READ_MODE_ONE = 1;
    private static final int READ_MODE_TWO = 2;
    private static final int READ_MODE_THREE = 3;
    private static final int DAY_SHIFT_TIME = 8;
    public static final String ZONE_LOCAL = "GMT+08:00";
    @Value("${excel.uri}")
    private String baseUri;// Excel存放的路径
    private ThreadLocal<Date> preTime = new ThreadLocal<>();

    @Override
    public void init() throws Exception {
        excelRangeList = excelMapper.getExcelRange();
    }

    @Override
    public void start() throws Exception {

    }

    @Override
    public int sendCommands(List<DataModel> datalist, List<String> err) throws Exception {
        return 0;
    }

    @Scheduled(cron = "0/10 * * * * ?")
    public void doUpdate() throws Exception {
        logger.debug("煤质化验指标更新启动 ...");
        preTime.remove();
        // 读取班报常规流程
        if (baseUri != null) {
            File file = new File(baseUri);
            logger.debug("获取数据存放路径[\"{}\"], 是否连接：{}", file.getAbsolutePath(), file.exists());
            if (file.exists()) {
                if (file.isDirectory()) {
                    logger.debug("成功访问煤质化验指标存放目录[\"" + baseUri + "\"]");
                    logger.debug("开始更新任务...");
                    File[] testIndexFileLst = file.listFiles((dir, name) -> {
                        // 文件定位
                        return name.indexOf("煤质化验班报.xls") > 0;
                    });
                    if(testIndexFileLst == null||testIndexFileLst.length ==0){
                        logger.debug("找不到煤质化验班报");
                        return;
                    }
                    int[] total = {0, 0};
                    for (File testIndexFile : testIndexFileLst) {
                        logger.debug("搜索到指标文件：[" + testIndexFile.getName() + "]");
                        logger.debug("初始化指标文件：[" + testIndexFile.getName() + "]");
                        HSSFWorkbook workbook =
                                new HSSFWorkbook(new BufferedInputStream(new FileInputStream(testIndexFile)));
                        HSSFSheet sheet;
                        logger.debug("开始检索新的数据表格...");
                        // 开始解析
                        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                            sheet = workbook.getSheetAt(i);
                            if (!(sheet.getSheetName().endsWith("白") || sheet.getSheetName().endsWith("夜"))) {
                                continue;
                            }
                            List<CoalAnalysisRecord> tlst = new ArrayList<>();
                            for (ExcelRange excelRange : excelRangeList) {
                                tlst.addAll(getTextInfoInArea(excelRange, sheet));
                            }
                            if (!tlst.isEmpty()) {
                                RecordTimeRange timeRange = getTimeRange(tlst);
                                List<CoalAnalysisRecord> existRecord = excelMapper.getExistRecord(timeRange);
                                Collection<CoalAnalysisRecord> newRecord = CollectionUtils.subtract(tlst, existRecord);
                                logger.info("找到新表格[" + sheet.getSheetName() + "],有[" + newRecord.size() + "]条数据需要转换！");
                                if(newRecord.size()!= 0) {
                                    List<DataModel> dataModels = parseToDataModel(new ArrayList<>(newRecord));
                                    Queue q = QueueManager.getQueueCollected();
                                    q.addAll(dataModels);
                                    logger.info("表格[" + sheet.getSheetName() + "]数据更新完毕,[" + dataModels.size() + "]条数据添加到队列！");
                                    total[0] += 1;
                                    total[1] += dataModels.size();
                                }
                            }
                        }
                        workbook.close();
                    }
                    logger.info("本次任务统计：更新[" + total[0] + "]个表格，[" + total[1] + "]条记录！");
                }
            } else {
                logger.debug("无法连接到指定目录，未做任何操作直接退出！");
            }
        } else {
            logger.debug("未配置数据存放路径，未做任何操作直接退出！");
        }
    }

    private RecordTimeRange getTimeRange(List<CoalAnalysisRecord> records){
        Date startTime = null;
        Date endTime = null;
        for (CoalAnalysisRecord record:records){
            Date time = record.getTime();
            if(time == null){
                continue;
            }
            if(startTime == null || startTime.after(time)){
                startTime = time;
            }
            if(endTime == null || endTime.before(time)){
                endTime = time;
            }
        }
        return new RecordTimeRange(startTime,endTime);
    }

    private List<CoalAnalysisRecord> getTextInfoInArea(ExcelRange excelRange, HSSFSheet sheet) throws ParseException {
        CoalAnalysisRecord test;
        String target = null;
        HSSFRow row;
        String deviceNum = null;
        // 存放化验数据列表
        List<CoalAnalysisRecord> coalAnalysisRecords = new ArrayList<>();
        // 获取当前sheet日期
        Date day = getSheetDate(sheet);
        // 获取单元格
        for (int i = excelRange.getStartY(); i < excelRange.getStartY() + excelRange.getRow(); i++) {
            test = new CoalAnalysisRecord();
            row = sheet.getRow(i);
            if (row == null) {
                continue;
            }
            // 零、化验类型
            target = setTarget(excelRange, test, target, row);
            // 一、设备编号
            deviceNum = getDeviceCode(excelRange, row, deviceNum);
            if (deviceNum == null) {
                continue;
            }
            test.setSystem(excelRange.getSystem());
            test.setSample(deviceNum);
            // 二、时间
            setTime(excelRange, test, row, day);
            // 三、灰分
            if (excelRange.getAadGap() != null) {
                setAad(excelRange, test, row);
            }
            // 四、水分
            if (excelRange.getMtGap() != null) {
                setMt(excelRange, test, row);
            }
            // 五、硫分
            if (excelRange.getStadGap() != null) {
                setStad(excelRange, test, row);
            }
            // 六、发热量
            if (excelRange.getQarGap() != null) {
                setQnetar(excelRange, test, row);
            }
            if (!(test.getAad() == null && test.getMt() == null && test.getStad() == null
                    && test.getQnetar() == null)) {
                coalAnalysisRecords.add(test);
            }
        }
        return coalAnalysisRecords;
    }

    /**
     * 设置化验项目
     *
     * @param excelRange
     * @param test
     * @param target
     * @param row
     * @return
     */
    private String setTarget(ExcelRange excelRange, CoalAnalysisRecord test, String target, HSSFRow row) {
        HSSFCell cell;
        int readMode = excelRange.getReadMode();
        if (readMode == READ_MODE_ONE || readMode == READ_MODE_THREE) {
            target = excelRange.getName();
        } else if (readMode == READ_MODE_TWO) {
            cell = row.getCell(excelRange.getStartX() + excelRange.getTargetGap());
            if (!isEmptyCell(cell)) {
                target = cell.getStringCellValue();
            }
        }
        test.setTarget(target);
        return target;
    }


    private String getDeviceCode(ExcelRange excelRange, HSSFRow row, String deviceNum) {
        if (excelRange.getReadMode() == READ_MODE_THREE) {
            deviceNum = excelRange.getSystem() + excelRange.getName().substring(0, 3);
        } else if (excelRange.getReadMode() == READ_MODE_ONE || excelRange.getReadMode() == READ_MODE_TWO) {
            HSSFCell cell = row.getCell(excelRange.getStartX() + excelRange.getDeviceIdGap());
            if (!isEmptyCell(cell)) {
                if (cell.getCellType() == HSSFCell.CELL_TYPE_NUMERIC) {
                    deviceNum = String.valueOf((int) cell.getNumericCellValue());
                } else if (cell.getCellType() == HSSFCell.CELL_TYPE_STRING) {
                    deviceNum = cell.getStringCellValue();
                }
            }
        }
        return deviceNum;
    }


    /**
     * 设置时间
     * @param excelRange
     * @param test
     * @param row
     * @param day
     */
    private void setTime(ExcelRange excelRange, CoalAnalysisRecord test, HSSFRow row, Date day) {
        HSSFCell cell = row.getCell(excelRange.getStartX() + excelRange.getTimeGap());
        LocalDateTime dateTime = null;
        LocalDate date = LocalDateTime.ofInstant(Instant.ofEpochMilli(day.getTime()), ZoneId.of(ZONE_LOCAL)).toLocalDate();
        if (!isEmptyCell(cell)) {
            if (cell.getCellType() == HSSFCell.CELL_TYPE_NUMERIC) {
                Date cellValue = cell.getDateCellValue();
                LocalTime time = LocalDateTime.ofInstant(Instant.ofEpochMilli(cellValue.getTime()), ZoneId.of(ZONE_LOCAL)).toLocalTime();
                dateTime = time.atDate(date);
                if (time.getHour() < DAY_SHIFT_TIME) {
                    dateTime = dateTime.plusDays(1);
                }
            }
        }else {
            if(preTime.get() != null) {
                dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(preTime.get().getTime()), ZoneId.of(ZONE_LOCAL));
            }
        }
        Date currentTime = null;
        if(dateTime!= null) {
            currentTime = new Date(dateTime.atZone(ZoneId.of(ZONE_LOCAL)).toInstant().toEpochMilli());
            preTime.set(currentTime);
        }
        test.setTime(currentTime);
    }

    /**
     * 设置硫分
     *
     * @param excelRange
     * @param test
     * @param row
     */
    private void setStad(ExcelRange excelRange, CoalAnalysisRecord test, HSSFRow row) {
        HSSFCell cell;
        cell = row.getCell(excelRange.getStartX() + excelRange.getStadGap());
        if (!isEmptyCell(cell)) {
            if (cell.getCellType() == HSSFCell.CELL_TYPE_NUMERIC) {
                test.setStad(cell.getNumericCellValue());
            }
        }
    }

    /**
     * 设置水分
     *
     * @param excelRange
     * @param test
     * @param row
     */
    private void setMt(ExcelRange excelRange, CoalAnalysisRecord test, HSSFRow row) {
        HSSFCell cell;
        cell = row.getCell(excelRange.getStartX() + excelRange.getMtGap());
        if (!isEmptyCell(cell)) {
            if (cell.getCellType() == HSSFCell.CELL_TYPE_NUMERIC) {
                test.setMt(cell.getNumericCellValue());
            }
        }
    }

    /**
     * 设置灰分
     *
     * @param excelRange
     * @param test
     * @param row
     */
    private void setAad(ExcelRange excelRange, CoalAnalysisRecord test, HSSFRow row) {
        HSSFCell cell;
        cell = row.getCell(excelRange.getStartX() + excelRange.getAadGap());
        if (!isEmptyCell(cell)) {
            if (cell.getCellType() == HSSFCell.CELL_TYPE_NUMERIC) {
                test.setAad(cell.getNumericCellValue());
            }
        }
    }

    /**
     * 设置发热量
     *
     * @param excelRange
     * @param test
     * @param row
     */
    private void setQnetar(ExcelRange excelRange, CoalAnalysisRecord test, HSSFRow row) {
        HSSFCell cell = row.getCell(excelRange.getStartX() + excelRange.getQarGap());

        if (!isEmptyCell(cell)) {
            if (cell.getCellType() == HSSFCell.CELL_TYPE_NUMERIC) {
                test.setQnetar(cell.getNumericCellValue());
            }
            if (cell.getCellType() == HSSFCell.CELL_TYPE_FORMULA) {
                if (cell.getCachedFormulaResultType() == HSSFCell.CELL_TYPE_STRING) {
//                try {
                    String qnetarStr = cell.getStringCellValue();
                    if (!("".equals(qnetarStr) || qnetarStr == null)) {
                        test.setQnetar(Double.parseDouble(qnetarStr));
                    }
                } else if (cell.getCachedFormulaResultType() == HSSFCell.CELL_TYPE_NUMERIC) {
                    test.setQnetar(cell.getNumericCellValue());
                }
//                } catch (IllegalStateException e) {
//                    test.setQnetar(cell.getNumericCellValue());
//                }
            }
        }
    }

    /**
     * 判断是否为空单元格
     *
     * @param cell
     * @return
     */
    private boolean isEmptyCell(HSSFCell cell) {
        boolean ret = true;
        if (cell != null && cell.getCellType() != HSSFCell.CELL_TYPE_BLANK) {
            ret = false;
        }
        return ret;
    }

    /**
     * 获取sheet日期
     *
     * @param sheet
     * @return
     */
    private Date getSheetDate(HSSFSheet sheet) throws ParseException {
        Date tmp = null;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日");
        if (sheet.getRow(1) != null) {
            for (int i = 0; i < sheet.getRow(1).getLastCellNum(); i++) {
                HSSFCell tempCell = sheet.getRow(1).getCell(i);
                if (tempCell == null) {
                    continue;
                }
                if (tempCell.getStringCellValue().indexOf("年") > 0) {
                    if (tempCell.getCellType() != HSSFCell.CELL_TYPE_BLANK) {
                        tmp = simpleDateFormat.parse(tempCell.getStringCellValue().trim());
                    }
                    break;
                }
            }
        }
        if (tmp == null) {
            String sheetName = sheet.getSheetName();
            String mm = sheetName.split("\\.")[0];
            String dd = sheetName.split("\\.")[1];
            dd = dd.substring(0, dd.length() - 1);
            Integer yy = Calendar.getInstance().get(Calendar.YEAR);
            tmp = simpleDateFormat.parse(yy + "年" + mm + "月" + dd + "日");
        }
        return tmp;
    }

    private List<DataModel> parseToDataModel(List<CoalAnalysisRecord> records) {
        List<DataModel> dataModels = new ArrayList<>();
        for (CoalAnalysisRecord record : records) {
            DataModel dataModel = new DataModel();
            dataModel.setThingCode("coalanalysis");
            dataModel.setMetricCode(record.getTarget());
            dataModel.setDataTimeStamp(record.getTime());
            dataModel.setValue(JSON.toJSONString(record));
            dataModels.add(dataModel);
        }
        return dataModels;
    }

//    private void getParamModel(List<DataModel> dataModels, CoalAnalysisRecord coalAnalysisRecord, String metricCode,
//                               Double value) {
//        DataModel dataModel = new DataModel();
//        dataModel.setMetricDataType(MetricDataTypeEnum.METRIC_DATA_TYPE_OK.getName());
//        dataModel.setThingCode(coalAnalysisRecord.getSample());
//        dataModel.setMetricCategoryCode(MetricModel.CATEGORY_ASSAY);
//        dataModel.setMetricCode(metricCode);
//        dataModel.setDataTimeStamp(coalAnalysisRecord.getTime());
//        dataModel.setValue(String.valueOf(value));
//        dataModels.add(dataModel);
//    }

}
