package com.daquexian.chaoli.forum.binding;

/**
 * Created by jianhao on 16-9-19.
 */
public interface DiffItem {
    boolean areContentsTheSame(DiffItem anotherItem);
    boolean areItemsTheSame(DiffItem anotherItem);
}
