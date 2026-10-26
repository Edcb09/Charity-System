package com.example.charity.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DonationExportExcel {
    @ExcelProperty("订单号")
    @ColumnWidth(25)
    private String orderNo;

    @ExcelProperty("捐款人")
    @ColumnWidth(15)
    private String username;

    @ExcelProperty("捐款项目")
    @ColumnWidth(25)
    private String projectTitle;

    @ExcelProperty("项目状态")
    @ColumnWidth(12)
    private String projectStatus;

    @ExcelProperty("捐款金额(元)")
    @ColumnWidth(15)
    private BigDecimal amount;

    @ExcelProperty("是否匿名")
    @ColumnWidth(10)
    private String isAnonymous;

    @ExcelProperty("捐款时间")
    @ColumnWidth(25)
    private LocalDateTime donateTime;
}