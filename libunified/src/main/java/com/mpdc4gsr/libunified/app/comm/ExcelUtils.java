package com.mpdc4gsr.libunified.app.comm;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import com.mpdc4gsr.libunified.R;
import com.mpdc4gsr.libunified.app.common.SharedManager;
import com.mpdc4gsr.libunified.app.config.FileConfig;
import com.mpdc4gsr.libunified.app.db.entity.ThermalEntity;
import com.mpdc4gsr.libunified.app.tools.TimeTools;
import com.mpdc4gsr.libunified.app.tools.UnitTools;
import com.mpdc4gsr.libunified.compat.ContextProvider;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class ExcelUtils {

    @NonNull
    private static String getTemperature(int index, @NonNull byte[] norTempData, boolean isShowC) {
        int tempValue = (norTempData[2 * index + 1] << 8 & 0xff00) | (norTempData[2 * index] & 0xff);
        float value = tempValue / 64f - 273.15f;
        return UnitTools.showC(value, isShowC);
    }

    @Nullable
    public static String exportExcel(@NonNull String name, int width, int height, @NonNull byte[] norTempData, @Nullable Callback callback) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet();
        boolean isShowC = SharedManager.INSTANCE.getTemperature() == 1;
        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        long time = System.currentTimeMillis();
        for (int i = 0; i < height; i++) {
            Row row = sheet.createRow(i);
            for (int j = 0; j < width; j++) {
                int index = i * width + j;
                sheet.setColumnWidth(j, 9 * width);
                Cell cell = row.createCell(j);
                cell.setCellStyle(cellStyle);
                cell.setCellValue(getTemperature(index, norTempData, isShowC));
                if (index % 100 == 0 && callback != null) {
                    //11，1001
                    callback.onOneCell(index / 100, width * height / 100);
                }
            }
        }
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                File excel = new File(FileConfig.getExcelDir(), name + ".xlsx");
                FileOutputStream fos = new FileOutputStream(excel);
                workbook.write(fos);
                fos.flush();
                fos.close();
                return excel.getAbsolutePath();
            } else {
                String fileName = name + ".xlsx";
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, FileConfig.getExcelDir());
                Uri contentUri = MediaStore.Files.getContentUri("external");
                Uri uri = ContextProvider.getContext().getContentResolver().insert(contentUri, values);
                if (uri != null) {
                    OutputStream outputStream = ContextProvider.getContext().getContentResolver().openOutputStream(uri);
                    if (outputStream != null) {
                        BufferedOutputStream bos = new BufferedOutputStream(outputStream);
                        workbook.write(bos);
                        bos.flush();
                        bos.close();
                    }
                    DocumentFile documentFile = DocumentFile.fromSingleUri(ContextProvider.getContext(), uri);
                    String filePath = uri.toString();
                    Log.w("", filePath);
                    return filePath;
                } else {
                    return null;
                }
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static String exportExcel(ArrayList<ThermalEntity> listData, boolean isPoint) {
        boolean isShowC = SharedManager.INSTANCE.getTemperature() == 1;
        try {
            // excel xlsx
            Workbook wb = new XSSFWorkbook();
            // 
            Sheet sheet = wb.createSheet();
            String[] title = {ContextProvider.getContext().getString(R.string.detail_date), ContextProvider.getContext().getString(R.string.chart_temperature_low), ContextProvider.getContext().getString(R.string.chart_temperature_high)};
            if (isPoint) {
                title = new String[]{ContextProvider.getContext().getString(R.string.detail_date), ContextProvider.getContext().getString(R.string.chart_temperature)};
            }
            //
            Row row = sheet.createRow(0);
            // 
            int colNum = title.length;
            CellStyle titleStyle = wb.createCellStyle();
            titleStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            titleStyle.setAlignment(HorizontalAlignment.CENTER); // 
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            Font font = wb.createFont();
            font.setBold(true);//
            titleStyle.setFont(font);
            CellStyle contentStyle = wb.createCellStyle();
            contentStyle.setAlignment(HorizontalAlignment.CENTER); // 
            contentStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            for (int i = 0; i < colNum; i++) {
                sheet.setColumnWidth(i, 20 * 256);  // 20
                Cell cell1 = row.createCell(i);
                cell1.setCellStyle(titleStyle);
                //
                cell1.setCellValue(title[i]);
            }
            // 
            for (int rowNum = 0; rowNum < listData.size(); rowNum++) {

                // rowNum + 1 
                row = sheet.createRow(rowNum + 1);
                // 
                row.setHeightInPoints(28f);

                ThermalEntity bean = listData.get(rowNum);

                for (int j = 0; j < title.length; j++) {
                    Cell cell = row.createCell(j);
                    //title[]
                    if (isPoint) {
                        switch (j) {
                            case 0:
                                //
                                cell.setCellValue(bean.getTime());
                                break;
                            case 1:
                                //
                                cell.setCellStyle(contentStyle);
                                cell.setCellValue(UnitTools.showC(bean.getMinTemp()));
                                break;
                        }
                    } else {
                        switch (j) {
                            case 0:
                                //
                                cell.setCellValue(bean.getTime());
                                break;
                            case 1:
                                //
                                cell.setCellStyle(contentStyle);
                                cell.setCellValue(UnitTools.showC(bean.getMinTemp()));
                                break;
                            case 2:
                                //
                                cell.setCellStyle(contentStyle);
                                cell.setCellValue(UnitTools.showC(bean.getMaxTemp(), isShowC));
                                break;
                        }
                    }
                }
            }
            String timeStr;
            if (listData.isEmpty()) {
                timeStr = TimeTools.INSTANCE.showDateSecond();
            } else {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
                timeStr = Instant.ofEpochMilli(listData.get(0).getStartTime())
                        .atZone(ZoneId.systemDefault())
                        .format(formatter);
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                File excel = new File(FileConfig.getExcelDir(), "TCView_" + timeStr + ".xlsx");
                FileOutputStream fos = new FileOutputStream(excel);
                wb.write(fos);
                fos.flush();
                fos.close();
                return excel.getAbsolutePath();
            } else {
                String fileName = "TCView_" + timeStr + ".xlsx";
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
//                values.put(MediaStore.MediaColumns.MIME_TYPE, "application/xlsx");
//                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM);
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, FileConfig.getExcelDir());
                Uri contentUri = MediaStore.Files.getContentUri("external");
                Uri uri = ContextProvider.getContext().getContentResolver().insert(contentUri, values);
                if (uri != null) {
                    OutputStream outputStream = ContextProvider.getContext().getContentResolver().openOutputStream(uri);
                    if (outputStream != null) {
                        BufferedOutputStream bos = new BufferedOutputStream(outputStream);
                        wb.write(bos);
                        bos.flush();
                        bos.close();
                    }
                    DocumentFile documentFile = DocumentFile.fromSingleUri(ContextProvider.getContext(), uri);
                    String filePath = documentFile != null ? documentFile.getName() : uri.toString();
                    Log.w("", filePath);
                    return filePath;
                } else {
                    return null;
                }
            }
        } catch (IOException e) {
            Log.e("ExpressExcle", "exportExcel", e);
            return null;
        }

    }

    @FunctionalInterface
    public interface Callback {
        void onOneCell(int current, int total);
    }
}
