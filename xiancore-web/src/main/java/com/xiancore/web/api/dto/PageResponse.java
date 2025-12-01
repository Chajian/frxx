package com.xiancore.web.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分页响应包装类
 * 用于包装分页数据
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PageResponse<T> {

    /** 当前页码 (从0开始) */
    private int pageNumber;

    /** 每页大小 */
    private int pageSize;

    /** 总页数 */
    private int totalPages;

    /** 总元素数 */
    private long totalElements;

    /** 当前页的内容 */
    private T content;

    /** 是否最后一页 */
    private boolean last;

    /** 是否第一页 */
    private boolean first;

    /** 是否有下一页 */
    private boolean hasNext;

    /** 是否有上一页 */
    private boolean hasPrevious;
}
