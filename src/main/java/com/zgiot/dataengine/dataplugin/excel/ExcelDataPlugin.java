package com.zgiot.dataengine.dataplugin.excel;

import com.zgiot.common.constants.MetricCodes;
import com.zgiot.common.pojo.DataModel;
import com.zgiot.common.pojo.MetricModel;
import com.zgiot.common.pojo.ThingModel;
import com.zgiot.dataengine.common.queue.QueueManager;
import com.zgiot.dataengine.dataplugin.DataPlugin;
import com.zgiot.dataengine.dataplugin.excel.dao.ExcelMapper;
import com.zgiot.dataengine.dataplugin.excel.pojo.CoalTestRecord;
import com.zgiot.dataengine.dataplugin.excel.pojo.ExcelRange;
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
import java.util.*;

@Component
public class ExcelDataPlugin implements DataPlugin {
    @Autowired
    private ExcelMapper excelMapper;
    private List<ExcelRange> excelRangeList;
    private static final Logger logger = LoggerFactory.getLogger(ExcelDataPlugin.class);
    private static final int READ_MODE_ONE=1;
    private static final int READ_MODE_TWO=2;
    private static final int READ_MODE_THREE=3;
    private static final int DAY_SHIFT_TIME=8;
    @Value("${excel.uri}")
    private String baseUri;// Excel存放的路径

    @Override
    public void init() throws Exception {
        excelRangeList= excelMapper.getExcelRange();
    }

    @Override
    public void start() throws Exception {

    }

    @Override
    public int sendCommands(List<DataModel> datalist, List<String> err) throws Exception {
        return 0;
    }

    @Scheduled(cron = "0 0/3 * * * ?")
    public void doUpdate() throws Exception {
        logger.debug("煤质化验指标更新启动 ...");
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
                            List<CoalTestRecord> tlst = new ArrayList<>();
                            for(ExcelRange excelRange:excelRangeList){
                                tlst.addAll(getTextInfoInArea(excelRange,sheet));
                            }
                            if (!tlst.isEmpty()) {
                                logger.info("找到新表格[" + sheet.getSheetName() + "],有[" + tlst.size() + "]条数据需要转换！");
                                List<DataModel> dataModels = parseToDataModel(tlst);
                                Queue q = QueueManager.getQueueCollected();
                                q.addAll(dataModels);
                                logger.info("表格[" + sheet.getSheetName() + "]数据更新完毕,[" + dataModels.size() + "]条数据添加到队列！");
                                total[0] += 1;
                                total[1] += dataModels.size();
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

    private List<CoalTestRecord> getTextInfoInArea(ExcelRange excelRange, HSSFSheet sheet) throws ParseException {

        CoalTestRecord test;
        String target = null;
        HSSFRow row;
        String deviceNum = null;
        Date time = null;
        // 存放化验数据列表
        List<CoalTestRecord> coalTestRecords = new ArrayList<>();
        // 获取当前sheet日期
        Date day = getDateString(sheet);
        // 获取单元格
        for (int i = excelRange.getStartY(); i < excelRange.getStartY() + excelRange.getRow(); i++) {
            test = new CoalTestRecord();
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
            test.setSample(deviceNum);
            // 二、时间
            time = setTime(excelRange, test, row, time, day);
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
                coalTestRecords.add(test);
            }
        }
        return coalTestRecords;
    }

    /**
     * 设置化验项目
     * @param excelRange
     * @param test
     * @param target
     * @param row
     * @return
     */
    private String setTarget(ExcelRange excelRange, CoalTestRecord test, String target, HSSFRow row) {
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
     * @param time
     * @param day
     * @return
     */
    private Date setTime(ExcelRange excelRange, CoalTestRecord test, HSSFRow row, Date time, Date day) {
        HSSFCell cell = row.getCell(excelRange.getStartX() + excelRange.getTimeGap());
        if (!isEmptyCell(cell)) {
            if (cell.getCellType() == HSSFCell.CELL_TYPE_NUMERIC) {
                Calendar ca = Calendar.getInstance();
                ca.setTime(cell.getDateCellValue());
                if (ca.get(Calendar.HOUR_OF_DAY) < DAY_SHIFT_TIME) {
                    ca.setTime(day);
                    ca.add(Calendar.DAY_OF_MONTH, 1);
                } else {
                    ca.setTime(day);
                }
                Date tempDay = ca.getTime();
                Date tempTime = cell.getDateCellValue();
                if (tempTime != null) {
                    ca.setTime(tempTime);
                    int hour = ca.get(Calendar.HOUR_OF_DAY);
                    int minute = ca.get(Calendar.MINUTE);
                    int second = ca.get(Calendar.SECOND);
                    ca.setTime(tempDay);
                    ca.add(Calendar.HOUR_OF_DAY, hour);
                    ca.add(Calendar.MINUTE, minute);
                    ca.add(Calendar.SECOND, second);
                    time = ca.getTime();
                }
            }
        }
        test.setTime(time);
        return time;
    }

    /**
     * 设置硫分
     * 
     * @param excelRange
     * @param test
     * @param row
     */
    private void setStad(ExcelRange excelRange, CoalTestRecord test, HSSFRow row) {
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
    private void setMt(ExcelRange excelRange, CoalTestRecord test, HSSFRow row) {
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
    private void setAad(ExcelRange excelRange, CoalTestRecord test, HSSFRow row) {
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
    private void setQnetar(ExcelRange excelRange, CoalTestRecord test, HSSFRow row) {
        HSSFCell cell = row.getCell(excelRange.getStartX() + excelRange.getQarGap());

        if (!isEmptyCell(cell)) {
            if (cell.getCellType() == HSSFCell.CELL_TYPE_NUMERIC) {
                test.setQnetar(cell.getNumericCellValue());
            }
            if (cell.getCellType() == HSSFCell.CELL_TYPE_FORMULA) {
                try {
                    String qnetarStr = cell.getStringCellValue();
                    if (!("".equals(qnetarStr) || qnetarStr == null)) {
                        test.setQnetar(Double.parseDouble(qnetarStr));
                    }
                } catch (IllegalStateException e) {
                    test.setQnetar(cell.getNumericCellValue());
                }
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
    private Date getDateString(HSSFSheet sheet) throws ParseException {
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

    private List<DataModel> parseToDataModel(List<CoalTestRecord> records) {
        List<DataModel> dataModels = new ArrayList<>();
        for (CoalTestRecord coalTestRecord : records) {
            if (coalTestRecord.getAad() != null) {
                getParamModel(dataModels, coalTestRecord, MetricCodes.ASSAY_AAD, coalTestRecord.getAad());
            }
            if (coalTestRecord.getMt() != null) {
                getParamModel(dataModels, coalTestRecord, MetricCodes.ASSAY_MT, coalTestRecord.getMt());
            }
            if (coalTestRecord.getStad() != null) {
                getParamModel(dataModels, coalTestRecord, MetricCodes.ASSAY_STAD, coalTestRecord.getStad());
            }
            if (coalTestRecord.getQnetar() != null) {
                getParamModel(dataModels, coalTestRecord, MetricCodes.ASSAY_QNETAR, coalTestRecord.getQnetar());
            }
        }
        return dataModels;
    }

    private void getParamModel(List<DataModel> dataModels, CoalTestRecord coalTestRecord, String metricCode,
            Double value) {
        DataModel dataModel = new DataModel();
        dataModel.setThingCategoryCode(ThingModel.CATEGORY_DEVICE);
        dataModel.setThingCode(coalTestRecord.getSample());
        dataModel.setMetricCategoryCode(MetricModel.CATEGORY_ASSAY);
        dataModel.setMetricCode(metricCode);
        dataModel.setDataTimeStamp(coalTestRecord.getTime());
        dataModel.setValue(String.valueOf(value));
        dataModels.add(dataModel);
    }

}
