package com.daquexian.chaoli.forum.model;

/**
 * Created by jianhao on 16-10-2.
 */

public interface IExpanded {
    /**
     * 比较两个IExpanded元素，A大于B时返回大于0的整数，等于时返回0
     * @param B 比较的对象
     * @return  表示大小关系的整数
     */
    int compareTo(IExpanded B);
}
