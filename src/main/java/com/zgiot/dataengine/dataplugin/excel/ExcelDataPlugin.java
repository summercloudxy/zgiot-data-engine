package com.zgiot.dataengine.dataplugin.excel;

import com.alibaba.fastjson.JSON;
import com.zgiot.common.constants.CoalAnalysisConstants;
import com.zgiot.common.pojo.CoalAnalysisRecord;
import com.zgiot.common.pojo.DataModel;
import com.zgiot.common.pojo.ProductionInspectRecord;
import com.zgiot.common.pojo.ReportFormsRecord;
import com.zgiot.dataengine.common.queue.QueueManager;
import com.zgiot.dataengine.dataplugin.DataPlugin;
import com.zgiot.dataengine.dataplugin.excel.dao.ExcelMapper;
import com.zgiot.dataengine.dataplugin.excel.pojo.ExcelRange;
import com.zgiot.dataengine.dataplugin.excel.pojo.RecordTimeRange;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.DateUtils;
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
import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.*;

@Component
public class ExcelDataPlugin implements DataPlugin {
    @Autowired
    private ExcelMapper excelMapper;
    private List<ExcelRange> coalAnalysisExcelRangeList = new ArrayList<>();
    private List<ExcelRange> productionExcelRangeList = new ArrayList<>();
    private static final Logger logger = LoggerFactory.getLogger(ExcelDataPlugin.class);
    /**
     * target：从excelrange读取
     * sample：从excel读取
     * time：记录的时间
     * eg：末原煤
     */
    private static final int READ_MODE_ONE = 1;
    /**
     * target：从excel读取
     * sample：从excel读取
     * time：记录的时间
     * eg：块原至末精区域
     */
    private static final int READ_MODE_TWO = 2;
    /**
     * target：从excelrange读取
     * sample：从excel读取后截取
     * time：记录的时间
     * eg：551生产精煤
     */
    private static final int READ_MODE_THREE = 3;
    /**
     * target：从excelrange读取
     * sample：从excel读取后截取
     * time：当前班次时间
     * eg：551生产精煤平均
     */
    private static final int READ_MODE_FOUR = 4;
    private static final int DAY_SHIFT_TIME = 8;
    public static final String ZONE_LOCAL = "GMT+08:00";
    private static final String COAL_ANALYSIS_EXCEL = "化验班报";
    private static final String PRODUCTION_EXCEL = "生产检查班报";
    @Value("${excel.uri}")
    private String baseUri;// Excel存放的路径
    private ThreadLocal<Date> preTime = new ThreadLocal<>();

    @Override
    public void init()  {
        coalAnalysisExcelRangeList = excelMapper.getExcelRange(COAL_ANALYSIS_EXCEL);
        productionExcelRangeList = excelMapper.getExcelRange(PRODUCTION_EXCEL);
    }

    @Override
    public void start() throws Exception {
        logger.info("excel插件开启，读取相关报表数据");
    }

    @Override
    public int sendCommands(List<DataModel> datalist, List<String> err) throws Exception {
        return 0;
    }

    /**
     * thingcode:CoalAnalysisConstants.COAL_ANALYSIS/CoalAnalysisConstants.PRODUCTION_INSPECT
     * metriccode:target
     * value:record
     * @throws Exception
     */
    @Scheduled(cron = "0 0/1 * * * ?")
    public void doUpdate() throws IOException, ParseException {
        logger.debug("煤质化验指标更新启动 ...");
        preTime.remove();
        // 读取班报常规流程
        if (baseUri != null) {
            File file = new File(baseUri);
            logger.debug("获取数据存放路径[\"{}\"], 是否连接：{}", file.getAbsolutePath(), file.exists());
            if (file.exists()) {
                if (file.isDirectory()) {
                    disposeFileInDir(file);


                }
            } else {
                logger.debug("无法连接到指定目录，未做任何操作直接退出！");
            }
        } else {
            logger.debug("未配置数据存放路径，未做任何操作直接退出！");
        }
    }

    private void disposeFileInDir(File file) throws IOException, ParseException {
        logger.debug("成功访问煤质化验指标存放目录[\"{}\"]" , baseUri);
        logger.debug("开始更新任务...");
        File[] coalAnalysisFileLst = file.listFiles((dir, name) ->
            // 文件定位
             name.contains("煤质化验班报.xls")
        );
        File[] productionCheckFileLst = file.listFiles((dir, name) ->
            // 文件定位
//            name.contains("生产检查班报.xls")
                //todo
                false
        );
        if (coalAnalysisFileLst == null || coalAnalysisFileLst.length == 0) {
            logger.debug("找不到煤质化验班报");
        }else {
            parseCoalAnalysisFile(coalAnalysisFileLst);
        }
        if (productionCheckFileLst == null || productionCheckFileLst.length == 0) {
            logger.debug("找不到生产检查班报");
        }else {
            parseProductionFile(productionCheckFileLst);
        }
    }

    private void parseCoalAnalysisFile(File[] coalAnalysisFileLst) throws IOException, ParseException {
        int[] total = {0, 0};
        for (File testIndexFile : coalAnalysisFileLst) {
            logger.debug("搜索到指标文件：[" + testIndexFile.getName() + "]");
            logger.debug("初始化指标文件：[" + testIndexFile.getName() + "]");
            try(HSSFWorkbook workbook =
                    new HSSFWorkbook(new BufferedInputStream(new FileInputStream(testIndexFile)))) {
                HSSFSheet sheet;
                logger.debug("开始检索新的数据表格...");
                // 开始解析
                for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                    sheet = workbook.getSheetAt(i);
                    logger.debug("开始读取文件第{}个sheet，名为{}",i,sheet.getSheetName());
                    if (!(sheet.getSheetName().endsWith("白") || sheet.getSheetName().endsWith("夜"))) {
                        logger.debug("该sheet不以白/夜结尾，不符合班报格式，跳过该sheet读取");
                        continue;

                    }
                    List<CoalAnalysisRecord> tlst = new ArrayList<>();
                    logger.debug("开始按区域读取sheet：{},当前共{}个区域需要读取",sheet.getSheetName(),coalAnalysisExcelRangeList.size());
                    for (ExcelRange excelRange : coalAnalysisExcelRangeList) {
                        tlst.addAll(getCoalAnalysisInfoInArea(excelRange, sheet));
                    }
                    if (!tlst.isEmpty()) {
                        disposeNewRecords(total, sheet, tlst);
                    }
                }
            }
        }
        logger.info("本次任务统计：更新煤质化验班报[{}]个表格，[{}]条记录！" ,total[0] , total[1] );
    }

    private void disposeNewRecords(int[] total, HSSFSheet sheet, List<CoalAnalysisRecord> tlst) {
        RecordTimeRange timeRange = getTimeRange(tlst);
        List<CoalAnalysisRecord> existRecord = excelMapper.getExistCoalAnalysisRecord(timeRange);
        Collection<CoalAnalysisRecord> newRecord = CollectionUtils.subtract(tlst, existRecord);
        if (!newRecord.isEmpty()) {
            addToCacheQueue(total, sheet, newRecord, COAL_ANALYSIS_EXCEL);
        }
    }


    private void parseProductionFile(File[] coalAnalysisFileLst) throws IOException, ParseException {
        int[] total = {0, 0};
        for (File testIndexFile : coalAnalysisFileLst) {
            logger.debug("搜索到生产检查文件：[" + testIndexFile.getName() + "]");
            logger.debug("初始化生产检查文件：[" + testIndexFile.getName() + "]");
            try(HSSFWorkbook workbook =
                    new HSSFWorkbook(new BufferedInputStream(new FileInputStream(testIndexFile)))) {
                HSSFSheet sheet;
                logger.debug("开始检索生产检查班报新的数据表格...");
                // 开始解析
                for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                    sheet = workbook.getSheetAt(i);
                    if (!(sheet.getSheetName().endsWith("白") || sheet.getSheetName().endsWith("夜"))) {
                        continue;
                    }
                    List<ProductionInspectRecord> productionCheckRecords = new ArrayList<>();
                    for (ExcelRange excelRange : productionExcelRangeList) {
                        productionCheckRecords.addAll(getProductionInfoInArea(excelRange, sheet));
                    }
                    if (!productionCheckRecords.isEmpty()) {
                        RecordTimeRange timeRange = getTimeRange(productionCheckRecords);
                        List<ProductionInspectRecord> existRecord = excelMapper.getExistProductionCheckRecord(timeRange);
                        Collection<ProductionInspectRecord> newRecord = CollectionUtils.subtract(productionCheckRecords, existRecord);
                        addToCacheQueue(total, sheet, newRecord, PRODUCTION_EXCEL);
                    }
                }
            }
        }
        logger.info("本次任务统计：更新生产检查班报[{}]个表格，[{}]条记录！" ,total[0] , total[1]);
    }

    private void addToCacheQueue(int[] total, HSSFSheet sheet, Collection<? extends ReportFormsRecord> newRecord, String type) {
        logger.info("找到新表格[{}],有[{}]条数据需要转换！" ,sheet.getSheetName() , newRecord.size() );
        if (!newRecord.isEmpty()) {
            List<DataModel> dataModels = parseToDataModel(new ArrayList<>(newRecord),type);
            Queue q = QueueManager.getQueueCollected();
            q.addAll(dataModels);
            logger.info("表格[{}]数据更新完毕,[{}]条数据添加到队列！" , sheet.getSheetName(), dataModels.size() );
            total[0] += 1;
            total[1] += dataModels.size();
        }
    }

    private RecordTimeRange getTimeRange(List<? extends ReportFormsRecord> records) {
        Date startTime = null;
        Date endTime = null;
        for (ReportFormsRecord record : records) {
            Date time = record.getTime();
            if (time == null) {
                continue;
            }
            if (startTime == null || startTime.after(time)) {
                startTime = time;
            }
            if (endTime == null || endTime.before(time)) {
                endTime = time;
            }
        }
        return new RecordTimeRange(startTime, endTime);
    }


    private List<CoalAnalysisRecord> getCoalAnalysisInfoInArea(ExcelRange excelRange, HSSFSheet sheet) throws ParseException {
        logger.debug("开始读取sheet:{}的{}区域的记录，excelRange为：{}",sheet.getSheetName(),excelRange.getName(),excelRange);
        CoalAnalysisRecord test;
        String target = null;
        HSSFRow row;
        String deviceNum = null;
        // 存放化验数据列表
        List<CoalAnalysisRecord> coalAnalysisRecords = new ArrayList<>();
        // 获取当前sheet日期
        // 白班为日期+8:00，夜班为日期+20:00
        Date sheetDate = getSheetDate(sheet);
        // 获取单元格
        for (int i = excelRange.getStartY(); i < excelRange.getStartY() + excelRange.getRow(); i++) {
            logger.debug("开始读取第{}行数据",i);
            test = new CoalAnalysisRecord();
            row = sheet.getRow(i);
            if (row == null) {

                logger.debug("第{}行没有数据，返回",i);
                continue;
            }
            // 零、化验类型
            target = getTarget(excelRange,target, row);
            test.setTarget(target);
            // 一、设备编号
            deviceNum = getDeviceCode(excelRange, row, deviceNum);
            if (deviceNum == null) {
                logger.debug("读取不到该行记录的设备号，返回",i);
                continue;
            }
            test.setSystem(excelRange.getSystem());
            test.setSample(deviceNum);
            // 二、时间
            Date time = getTime(excelRange, row, sheetDate);
            test.setTime(time);
            setCoalAnalysisParamValue(excelRange, test, row);

            if (!(test.getAad() == null && test.getMt() == null && test.getStad() == null
                    && test.getQnetar() == null)) {
                coalAnalysisRecords.add(test);
            }
        }
        return coalAnalysisRecords;
    }

    private void setCoalAnalysisParamValue(ExcelRange excelRange, CoalAnalysisRecord test, HSSFRow row) {
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
    }

    private List<ProductionInspectRecord> getProductionInfoInArea(ExcelRange excelRange, HSSFSheet sheet) throws ParseException {
        ProductionInspectRecord record;
        String target = null;
        HSSFRow row;
        String deviceNum = null;
        // 存放化验数据列表
        List<ProductionInspectRecord> productionCheckRecords = new ArrayList<>();
        // 获取当前sheet日期
        Date day = getSheetDate(sheet);
        // 获取单元格
        for (int i = excelRange.getStartY(); i < excelRange.getStartY() + excelRange.getRow(); i++) {
            record = new ProductionInspectRecord();
            row = sheet.getRow(i);
            if (row == null) {
                continue;
            }
            // 零、化验类型
            target = getTarget(excelRange, target, row);
            record.setTarget(target);
            record.setSystem(excelRange.getSystem());
            // 一、设备编号
            deviceNum = getDeviceCode(excelRange, row, deviceNum);
            if (deviceNum == null) {
                continue;
            }
            record.setSystem(excelRange.getSystem());
            record.setSample(deviceNum);
            // 二、时间
            Date time = getTime(excelRange, row, day);
            record.setTime(time);
            setProductionParamValue(excelRange, record, row);

            if (!record.isEmpty()) {
                productionCheckRecords.add(record);
            }
        }
        return productionCheckRecords;
    }

    private void setProductionParamValue(ExcelRange excelRange, ProductionInspectRecord record, HSSFRow row) {
        // 三、-1.8
        if (excelRange.getNegative1Point8Gap() != null) {
            setNegative1Point8(excelRange, record, row);
        }
        // 四、+1.8
        if (excelRange.getPositive1Point8Gap() != null) {
            setPositive1Point8(excelRange, record, row);
        }
        // 五、-1.45
        if (excelRange.getNegative1Point45Gap() != null) {
            setNegative1Point45(excelRange, record, row);
        }
        // 六、+1.45
        if (excelRange.getPositive1Point45Gap() != null) {
            setPositive1Point45(excelRange, record, row);
        }
        // 七、1.45-1.8
        if (excelRange.getOnePoint45To1Point8Gap() != null) {
            setOnePoint45To1Point8(excelRange, record, row);
        }
        // 五、-50mm
        if (excelRange.getNegative50mmGap() != null) {
            setNegative50mm(excelRange, record, row);
        }
        // 六、+50mm
        if (excelRange.getPositive50mmGap() != null) {
            setPositive50mm(excelRange, record, row);
        }
    }

    /**
     * 设置化验项目
     *
     * @param excelRange
     * @param target
     * @param row
     * @return
     */
    private String getTarget(ExcelRange excelRange, String target, HSSFRow row) {
        HSSFCell cell;
        int readMode = excelRange.getReadMode();
        if (readMode == READ_MODE_ONE || readMode == READ_MODE_THREE || readMode ==READ_MODE_FOUR) {
            target = excelRange.getName();
        } else if (readMode == READ_MODE_TWO) {
            cell = row.getCell(excelRange.getStartX() + excelRange.getTargetGap());
            if (!isEmptyCell(cell)) {
                target = cell.getStringCellValue();
            }
        }
        return target;
    }


    private String getDeviceCode(ExcelRange excelRange, HSSFRow row, String deviceNum) {
        if (excelRange.getReadMode() == READ_MODE_THREE || excelRange.getReadMode() == READ_MODE_FOUR) {
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
     *
     * @param excelRange
     * @param row
     * @param sheetDate
     */
    private Date getTime(ExcelRange excelRange,HSSFRow row, Date sheetDate) {
        int readMode = excelRange.getReadMode();
        Date currentTime = null;
        if (readMode == READ_MODE_ONE || readMode == READ_MODE_THREE || readMode ==READ_MODE_TWO) {
            HSSFCell cell = row.getCell(excelRange.getStartX() + excelRange.getTimeGap());
            LocalDateTime dateTime = null;
            LocalDate date = LocalDateTime.ofInstant(Instant.ofEpochMilli(sheetDate.getTime()), ZoneId.of(ZONE_LOCAL)).toLocalDate();
            if (!isEmptyCell(cell)) {
                dateTime = getTimeFromCell(cell, dateTime, date);
            } else {
                if (preTime.get() != null) {
                    dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(preTime.get().getTime()), ZoneId.of(ZONE_LOCAL));
                }
            }

            if (dateTime != null) {
                currentTime = new Date(dateTime.atZone(ZoneId.of(ZONE_LOCAL)).toInstant().toEpochMilli());
                preTime.set(currentTime);
            }

        }
        //平均记录以当前班次开始时间作为时间
        else if (readMode == READ_MODE_FOUR){
            currentTime = sheetDate;
        }
        return currentTime;
    }

    private LocalDateTime getTimeFromCell(HSSFCell cell, LocalDateTime dateTime, LocalDate date) {
        if (cell.getCellType() == HSSFCell.CELL_TYPE_NUMERIC) {
            Date cellValue = cell.getDateCellValue();
            LocalTime time = LocalDateTime.ofInstant(Instant.ofEpochMilli(cellValue.getTime()), ZoneId.of(ZONE_LOCAL)).toLocalTime();
            dateTime = time.atDate(date);
            if (time.getHour() < DAY_SHIFT_TIME) {
                dateTime = dateTime.plusDays(1);
            }
        }
        return dateTime;
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
        Double valueInCell = getValueInCell(cell);
        test.setStad(valueInCell);
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
        Double valueInCell = getValueInCell(cell);
        test.setMt(valueInCell);
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
        Double value = getValueInCell(cell);
        test.setAad(value);
    }

    private Double getValueInCell(HSSFCell cell) {
        Double value = null;
        if (!isEmptyCell(cell)) {
            if (cell.getCellType() == HSSFCell.CELL_TYPE_NUMERIC) {
               value = cell.getNumericCellValue();
            }
            if (cell.getCellType() == HSSFCell.CELL_TYPE_FORMULA) {
                if (cell.getCachedFormulaResultType() == HSSFCell.CELL_TYPE_STRING) {
                    String valueStr = cell.getStringCellValue();
                    if (!("".equals(valueStr) || valueStr == null)) {
                        value = Double.parseDouble(valueStr);
                    }
                } else if (cell.getCachedFormulaResultType() == HSSFCell.CELL_TYPE_NUMERIC) {
                    value = cell.getNumericCellValue();
                }
            }
        }
        return value;
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
        Double valueInCell = getValueInCell(cell);
        test.setQnetar(valueInCell);
    }

    /**
     * 设置-1.8
     *
     * @param excelRange
     * @param record
     * @param row
     */
    private void setNegative1Point8(ExcelRange excelRange, ProductionInspectRecord record, HSSFRow row) {
        HSSFCell cell;
        cell = row.getCell(excelRange.getStartX() + excelRange.getNegative1Point8Gap());
        if (!isEmptyCell(cell) && cell.getCellType() == HSSFCell.CELL_TYPE_NUMERIC) {
            double numericCellValue = cell.getNumericCellValue();
            BigDecimal bigDecimal = BigDecimal.valueOf(numericCellValue).setScale(2,BigDecimal.ROUND_HALF_UP);
            record.setNegative1Point8(bigDecimal.doubleValue());
        }
    }

    /**
     * 设置+1.8
     *
     * @param excelRange
     * @param record
     * @param row
     */
    private void setPositive1Point8(ExcelRange excelRange, ProductionInspectRecord record, HSSFRow row) {
        HSSFCell cell;
        cell = row.getCell(excelRange.getStartX() + excelRange.getPositive1Point8Gap());
        if (!isEmptyCell(cell) && cell.getCellType() == HSSFCell.CELL_TYPE_NUMERIC) {
            double numericCellValue = cell.getNumericCellValue();
            BigDecimal bigDecimal = BigDecimal.valueOf(numericCellValue).setScale(2,BigDecimal.ROUND_HALF_UP);
            record.setPositive1Point8(bigDecimal.doubleValue());
        }
    }


    /**
     * 设置-1.45
     *
     * @param excelRange
     * @param record
     * @param row
     */
    private void setNegative1Point45(ExcelRange excelRange, ProductionInspectRecord record, HSSFRow row) {
        HSSFCell cell;
        cell = row.getCell(excelRange.getStartX() + excelRange.getNegative1Point45Gap());
        if (!isEmptyCell(cell) && cell.getCellType() == HSSFCell.CELL_TYPE_NUMERIC) {
            double numericCellValue = cell.getNumericCellValue();
            BigDecimal bigDecimal = BigDecimal.valueOf(numericCellValue).setScale(2,BigDecimal.ROUND_HALF_UP);
            record.setNegative1Point45(bigDecimal.doubleValue());
        }
    }

    /**
     * 设置+1.45
     *
     * @param excelRange
     * @param record
     * @param row
     */
    private void setPositive1Point45(ExcelRange excelRange, ProductionInspectRecord record, HSSFRow row) {
        HSSFCell cell;
        cell = row.getCell(excelRange.getStartX() + excelRange.getPositive1Point45Gap());
        if (!isEmptyCell(cell) && cell.getCellType() == HSSFCell.CELL_TYPE_NUMERIC) {
            double numericCellValue = cell.getNumericCellValue();
            BigDecimal bigDecimal = BigDecimal.valueOf(numericCellValue).setScale(2,BigDecimal.ROUND_HALF_UP);
            record.setPositive1Point45(bigDecimal.doubleValue());
        }
    }

    /**
     * 设置1.45-1.8
     *
     * @param excelRange
     * @param record
     * @param row
     */
    private void setOnePoint45To1Point8(ExcelRange excelRange, ProductionInspectRecord record, HSSFRow row) {
        HSSFCell cell;
        cell = row.getCell(excelRange.getStartX() + excelRange.getOnePoint45To1Point8Gap());
        if (!isEmptyCell(cell) && cell.getCellType() == HSSFCell.CELL_TYPE_NUMERIC) {
            double numericCellValue = cell.getNumericCellValue();
            BigDecimal bigDecimal = BigDecimal.valueOf(numericCellValue).setScale(2,BigDecimal.ROUND_HALF_UP);
            record.setOnePoint45To1Point8(bigDecimal.doubleValue());
        }
    }

    /**
     * 设置-50mm
     *
     * @param excelRange
     * @param record
     * @param row
     */
    private void setNegative50mm(ExcelRange excelRange, ProductionInspectRecord record, HSSFRow row) {
        HSSFCell cell;
        cell = row.getCell(excelRange.getStartX() + excelRange.getNegative50mmGap());
        if (!isEmptyCell(cell) && cell.getCellType() == HSSFCell.CELL_TYPE_NUMERIC) {
            double numericCellValue = cell.getNumericCellValue();
            BigDecimal bigDecimal = BigDecimal.valueOf(numericCellValue).setScale(2,BigDecimal.ROUND_HALF_UP);
            record.setNegative50mm(bigDecimal.doubleValue());
        }
    }

    /**
     * 设置+50mm
     *
     * @param excelRange
     * @param record
     * @param row
     */
    private void setPositive50mm(ExcelRange excelRange, ProductionInspectRecord record, HSSFRow row) {
        HSSFCell cell;
        cell = row.getCell(excelRange.getStartX() + excelRange.getPositive50mmGap());
        if (!isEmptyCell(cell) && cell.getCellType() == HSSFCell.CELL_TYPE_NUMERIC) {
            double numericCellValue = cell.getNumericCellValue();
            BigDecimal bigDecimal = BigDecimal.valueOf(numericCellValue).setScale(2,BigDecimal.ROUND_HALF_UP);
            record.setPositive50mm(bigDecimal.doubleValue());
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
                if (tempCell.getStringCellValue().contains("年")) {
                    if (tempCell.getCellType() != HSSFCell.CELL_TYPE_BLANK) {
                        tmp = simpleDateFormat.parse(tempCell.getStringCellValue().trim());
                    }
                    break;
                }
            }
        }
        String sheetName = sheet.getSheetName();
        String mm = sheetName.split("\\.")[0];
        String dd = sheetName.split("\\.")[1];
        dd = dd.substring(0, dd.length() - 1);
        if (tmp == null) {

            Integer yy = Calendar.getInstance().get(Calendar.YEAR);
            tmp = simpleDateFormat.parse(yy + "年" + mm + "月" + dd + "日");
        }
        String shift = sheetName.substring(sheetName.length()-1,sheetName.length());
        tmp = getDutyHour(tmp, shift);
        return tmp;
    }

    private Date getDutyHour(Date tmp, String shift) {
        if ("白".equals(shift)){
            tmp= DateUtils.addHours(tmp,8);
        }else {
            tmp=DateUtils.addHours(tmp,20);
        }
        return tmp;
    }

    private List<DataModel> parseToDataModel(List<? extends ReportFormsRecord> records,String type) {
        List<DataModel> dataModels = new ArrayList<>();
        for (ReportFormsRecord record : records) {
            DataModel dataModel = new DataModel();
            if (COAL_ANALYSIS_EXCEL.equals(type)) {
                dataModel.setThingCode(CoalAnalysisConstants.COAL_ANALYSIS);
            }else if(PRODUCTION_EXCEL.equals(type)){
                dataModel.setThingCode(CoalAnalysisConstants.PRODUCTION_INSPECT);
            }
            dataModel.setMetricCode(record.getTarget());
            dataModel.setDataTimeStamp(record.getTime());
            dataModel.setValue(JSON.toJSONString(record));
            dataModels.add(dataModel);
        }
        return dataModels;
    }

}
